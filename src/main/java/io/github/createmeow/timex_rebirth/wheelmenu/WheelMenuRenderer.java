package io.github.createmeow.timex_rebirth.wheelmenu;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.client.ModShaders;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Radial wheel menu renderer with GPU-based shader rendering.
 * <p>
 * Performance: each ring/arc is drawn as a single quad via a custom GLSL
 * shader — the GPU computes distance, angle, anti-aliasing, and colour
 * gradient per fragment.  This replaces the old CPU pixel-iteration
 * approach that caused severe lag on low-end devices (~648 000 point-in-
 * triangle tests per frame → 4 vertices + 1 draw call).
 */
@OnlyIn(Dist.CLIENT)
public class WheelMenuRenderer {
    /** Kept for API compatibility; rendering is handled by {@link WheelMenuScreen}. */
    public static final LayeredDraw.Layer OVERLAY = WheelMenuRenderer::renderOverlay;

    private static final Map<ResourceLocation, WheelSelection> selections = new LinkedHashMap<>();
    private static final List<WheelSelection> openOrder = new ArrayList<>();
    private static List<WheelSelection> visibleSelections = new ArrayList<>();
    private static boolean isOpen = false;
    private static boolean isClosing = false;
    private static float openingStatus = 0;
    private static int hoveredIndex = -1;

    // ── Geometry ───────────────────────────────────────────────────────────
    private static final float OUTER_R = 62;
    private static final float RING_W = 30;
    private static final float INNER_R = OUTER_R - RING_W;
    private static final float MID_R = (OUTER_R + INNER_R) / 2;
    private static final int MAX_ITEMS = 8;

    // ── Animation ──────────────────────────────────────────────────────────
    private static final float OPEN_DURATION = 3.0f;

    // ── Theme colours (ARGB) ───────────────────────────────────────────────
    private static final int THEME = 0x00E5FF;       // bright cyan
    private static final int THEME_DARK = 0x0088AA;   // muted cyan
    private static final int WHITE = 0xFFFFFF;

    // ════════════════════════════════════════════════════════════════════════
    //  Public API
    // ════════════════════════════════════════════════════════════════════════

    public static void registerSelection(WheelSelection selection) {
        registerSelection(TimeX.rl("selection_" + selections.size()), selection);
    }

    public static void registerSelection(ResourceLocation id, WheelSelection selection) {
        selections.put(id, selection);
    }

    /**
     * 通过 WheelMenuSelectionRegisterEvent 初始化轮盘选项（清空后重新注册）。
     * 各选项在事件订阅中通过 SelectionBuilder 注册。
     */
    @SuppressWarnings("removal")
    public static void initSelections() {
        selections.clear();
        NeoForge.EVENT_BUS.post(new WheelMenuSelectionRegisterEvent(selections));
    }

    public static boolean isOpen() {
        return isOpen;
    }

    public static List<WheelSelection> getSelections() {
        return new ArrayList<>(selections.values());
    }

    public static void open() {
        if (!selections.isEmpty() && Minecraft.getInstance().screen == null) {
            // 触发打开事件，允许其他模组调整显隐与顺序或取消打开
            List<ResourceLocation> toShow = new ArrayList<>(selections.keySet());
            WheelMenuOpenEvent openEvent = new WheelMenuOpenEvent(toShow);
            if (NeoForge.EVENT_BUS.post(openEvent).isCanceled()) {
                return;
            }
            openOrder.clear();
            for (ResourceLocation id : toShow) {
                WheelSelection sel = selections.get(id);
                if (sel != null) {
                    openOrder.add(sel);
                }
            }
            isOpen = true;
            isClosing = false;
            openingStatus = 0;
            hoveredIndex = -1;
            Minecraft.getInstance().setScreen(new WheelMenuScreen());
        }
    }

    public static void close() {
        if (isOpen && !isClosing) {
            isClosing = true;
            openingStatus = OPEN_DURATION;
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            if (isOpen) forceClose();
            return;
        }

        // 更新所有选项的可见性
        for (WheelSelection sel : selections.values()) {
            sel.tick();
        }

        if (isOpen) {
            long window = mc.getWindow().getWindow();
            boolean altDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                            || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT);

            if (altDown) {
                isClosing = false;
                if (openingStatus < OPEN_DURATION) openingStatus++;
            } else {
                if (!isClosing) executeSelection();
                isClosing = true;
                openingStatus--;
                if (openingStatus <= 0) forceClose();
            }
        }
    }

    private static void executeSelection() {
        if (hoveredIndex >= 0 && hoveredIndex < visibleSelections.size()) {
            WheelSelection sel = visibleSelections.get(hoveredIndex);
            if (sel.isVisible()) sel.execute();
        }
    }

    private static void forceClose() {
        isOpen = false;
        isClosing = false;
        openingStatus = 0;
        hoveredIndex = -1;
        if (Minecraft.getInstance().screen instanceof WheelMenuScreen) {
            Minecraft.getInstance().setScreen(null);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Rendering
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Render the wheel menu. Called from {@link WheelMenuScreen#render}.
     */
    private static void renderWheelMenu(GuiGraphics graphics, float partialTick,
                                         int mouseX, int mouseY, int screenWidth, int screenHeight) {
        if (!isOpen || selections.isEmpty()) return;
        if (isClosing && openingStatus <= 0) return;

        // ── Progress with smooth easing ────────────────────────────────────
        float progress = isClosing
                ? Mth.clamp((openingStatus - partialTick) / OPEN_DURATION, 0, 1)
                : Mth.clamp((openingStatus + partialTick) / OPEN_DURATION, 0, 1);
        if (progress <= 0) return;
        // Smoothstep easing for buttery animation
        float eased = progress * progress * (3 - 2 * progress);
        float scale = Mth.lerp(0.4f, 1.0f, eased);       // start at 40% size
        float alpha = eased;                              // fade in/out

        // ── Filter visible selections ──────────────────────────────────────
        visibleSelections = new ArrayList<>();
        for (WheelSelection s : openOrder) if (s.isVisible()) visibleSelections.add(s);
        int size = Math.min(visibleSelections.size(), MAX_ITEMS);
        if (size == 0) return;

        // ── Mouse position relative to centre ──────────────────────────────
        double mx = mouseX - (screenWidth / 2.0);
        double my = mouseY - (screenHeight / 2.0);
        double dist = Math.sqrt(mx * mx + my * my);

        float sliceAngle = 360f / size;
        float halfSlice = sliceAngle / 2;

        // ── Hover detection ────────────────────────────────────────────────
        boolean inRing = dist >= INNER_R * 0.4 && dist <= OUTER_R * 1.3;
        if (inRing) {
            double rawAngle = Math.toDegrees(Math.atan2(mx, -my));
            if (rawAngle < 0) rawAngle += 360;
            // Shift by half a slice so boundaries fall BETWEEN items
            hoveredIndex = ((int) ((rawAngle + halfSlice) / sliceAngle)) % size;
        } else {
            hoveredIndex = -1;
        }

        // ── Hover state & hover action ─────────────────────────────────────
        for (int i = 0; i < size; i++) {
            WheelSelection sel = visibleSelections.get(i);
            boolean hovered = (i == hoveredIndex);
            if (hovered != sel.isHovered()) {
                sel.setHovered(hovered);
                if (hovered) sel.onHover();
            }
        }
        int hoverColor = (hoveredIndex >= 0 && hoveredIndex < visibleSelections.size())
                ? visibleSelections.get(hoveredIndex).getColor()
                : THEME;

        Font font = Minecraft.getInstance().font;
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(screenWidth / 2.0, screenHeight / 2.0, 0);
        pose.scale(scale, scale, 1);

        // ── Layer 1: Background ring (dark, semi-transparent) ──────────────
        drawRing(graphics, INNER_R, OUTER_R, 0, 360,
                withAlpha(0x1A1A2E, 0.85f * alpha),
                withAlpha(0x0D0D1A, 0.70f * alpha));

        // ── Layer 2: Hover segment highlight ───────────────────────────────
        if (hoveredIndex >= 0) {
            float segStart = hoveredIndex * sliceAngle - halfSlice;
            float segEnd = hoveredIndex * sliceAngle + halfSlice;

            // Soft fill
            drawRing(graphics, INNER_R, OUTER_R, segStart, segEnd,
                    withAlpha(hoverColor, 0.35f * alpha),
                    withAlpha(hoverColor, 0.15f * alpha));

            // Bright inner-edge accent
            drawRing(graphics, INNER_R, INNER_R + 3, segStart, segEnd,
                    withAlpha(hoverColor, 0.90f * alpha),
                    withAlpha(hoverColor, 0.40f * alpha));

            // Bright outer-edge accent
            drawRing(graphics, OUTER_R - 2, OUTER_R, segStart, segEnd,
                    withAlpha(WHITE, 0.50f * alpha),
                    withAlpha(hoverColor, 0.70f * alpha));
        }

        // ── Layer 3: Inner accent ring (thin bright circle at inner edge) ──
        drawRing(graphics, INNER_R - 1, INNER_R + 0.5f, 0, 360,
                withAlpha(WHITE, 0.15f * alpha),
                withAlpha(THEME, 0.08f * alpha));

        // ── Layer 4: Outer accent ring (thin bright circle at outer edge) ──
        drawRing(graphics, OUTER_R - 0.5f, OUTER_R + 1, 0, 360,
                withAlpha(THEME, 0.20f * alpha),
                withAlpha(WHITE, 0.10f * alpha));

        // ── Layer 5: Divider lines between segments ───────────────────────
        if (size > 1) {
            for (int i = 0; i < size; i++) {
                float divAngle = i * sliceAngle - halfSlice;
                drawRing(graphics, INNER_R, OUTER_R, divAngle - 0.6f, divAngle + 0.6f,
                        withAlpha(0x000000, 0.50f * alpha),
                        withAlpha(0x000000, 0.30f * alpha));
            }
        }

        // ── Layer 6: Centre dot (always the same visual, brighter when hovering) ─
        float dotAlpha = (hoveredIndex >= 0 ? 1.0f : 0.80f) * alpha;
        drawRing(graphics, 0, 4, 0, 360,
                withAlpha(THEME, dotAlpha),
                withAlpha(THEME_DARK, dotAlpha * 0.5f));

        // ── Layer 7: Item icons ────────────────────────────────────────────
        for (int i = 0; i < size; i++) {
            double theta = Math.toRadians(i * sliceAngle - 90);
            int ix = (int) (MID_R * Math.cos(theta));
            int iy = (int) (MID_R * Math.sin(theta));
            boolean hovered = (i == hoveredIndex);
            visibleSelections.get(i).render(graphics, ix, iy, hovered, alpha);
        }

        // ── Layer 8: Text — item name below ring ───────────────────────────
        if (hoveredIndex >= 0 && hoveredIndex < visibleSelections.size()) {
            Component text = visibleSelections.get(hoveredIndex).getMessage();
            int textAlpha = (int) (255 * alpha);
            graphics.drawCenteredString(font, text, 0, (int) OUTER_R + 12,
                    (textAlpha << 24) | hoverColor);
        }

        // ── Layer 9: Hint text above ring ──────────────────────────────────
        if (hoveredIndex < 0) {
            Component hint = Component.translatable("key." + TimeX.MODID + ".open_wheel_menu");
            int hintAlpha = (int) (140 * alpha);
            graphics.drawCenteredString(font, hint, 0, (int) -(OUTER_R + 18),
                    (hintAlpha << 24) | 0xFFFFFF);
        }

        pose.popPose();
    }

    /**
     * No-op overlay — rendering is handled by {@link WheelMenuScreen}.
     * Kept for API compatibility with the overlay registration in {@code TimeXClient}.
     */
    private static void renderOverlay(GuiGraphics graphics, DeltaTracker deltaTracker) {
        // intentionally empty
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GPU Shader Ring Drawing
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Draw a ring (or arc) using a custom GPU shader.
     * <p>
     * The entire shape is rendered as a single quad — the fragment shader
     * computes distance, angle, anti-aliasing, and an inner→outer colour
     * gradient per pixel.  This is O(1) on the CPU side regardless of ring
     * size or arc resolution.
     *
     * @param graphics    the GuiGraphics context
     * @param innerR      inner radius (pixels)
     * @param outerR      outer radius (pixels)
     * @param startAngle  arc start angle (degrees, clockwise from 12 o'clock)
     * @param endAngle    arc end angle (degrees, clockwise from 12 o'clock)
     * @param innerColor  ARGB colour at the inner edge
     * @param outerColor  ARGB colour at the outer edge
     */
    private static void drawRing(GuiGraphics graphics, float innerR, float outerR,
                                  float startAngle, float endAngle,
                                  int innerColor, int outerColor) {
        if (outerR <= 0) return;
        if (endAngle - startAngle <= 0.01f) return;

        // Clamp inner radius to 0 (filled circle when innerR == 0)
        float ir = Math.max(0, innerR);
        float or = Math.max(ir + 0.1f, outerR);

        RenderSystem.setShader(ModShaders::getRingShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        ShaderInstance shader = ModShaders.getRingShader();

        // Convert pixel radii to UV space: quad spans [-or, +or], so UV 0..1
        // maps to that range, centre = (0.5, 0.5), outerRadius = 0.5
        shader.safeGetUniform("innerRadius").set(ir / or * 0.5f);
        shader.safeGetUniform("outerRadius").set(0.5f);
        shader.safeGetUniform("innerColor").set(intToVec4(innerColor));
        shader.safeGetUniform("outerColor").set(intToVec4(outerColor));
        shader.safeGetUniform("startAngle").set(startAngle);
        shader.safeGetUniform("endAngle").set(endAngle);
        shader.safeGetUniform("Smooth").set(0.6f / or);  // ~1 px anti-aliasing

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(matrix, -or, -or, 0).setUv(0, 0);
        builder.addVertex(matrix, -or,  or, 0).setUv(0, 1);
        builder.addVertex(matrix,  or,  or, 0).setUv(1, 1);
        builder.addVertex(matrix,  or, -or, 0).setUv(1, 0);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Colour helpers
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Set the alpha channel of an RGB colour.
     *
     * @param rgb   colour without alpha (0xRRGGBB)
     * @param alpha alpha 0-1
     * @return ARGB int
     */
    private static int withAlpha(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    /**
     * Convert an ARGB int to a {@link Vector4f} (RGBA, 0-1).
     */
    private static Vector4f intToVec4(int color) {
        return new Vector4f(
                (color >> 16 & 255) / 255.0f,
                (color >> 8 & 255) / 255.0f,
                (color & 255) / 255.0f,
                (color >> 24 & 255) / 255.0f
        );
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Screen
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Minimal Screen that manages cursor visibility automatically.
     * {@code setScreen(new WheelMenuScreen())} shows the cursor;
     * {@code setScreen(null)} hides it and returns to FPS mode.
     */
    private static class WheelMenuScreen extends Screen {

        protected WheelMenuScreen() {
            super(Component.literal("Wheel Menu"));
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderWheelMenu(graphics, partialTick, mouseX, mouseY, this.width, this.height);
        }

        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // Transparent — don't render the default dark background
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return true; // Absorb clicks so they don't reach the game
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            return true; // Absorb scroll
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                forceClose();
                return true;
            }
            // Return false so KeyMapping / InputConstants can still track key state
            return false;
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return false; // Handle ESC manually in keyPressed()
        }

        @Override
        public void removed() {
            // Clean up wheel menu state when the Screen is closed or replaced
            // (e.g. when the Corpse death-history screen opens)
            isOpen = false;
            isClosing = false;
            openingStatus = 0;
            hoveredIndex = -1;
        }
    }
}
