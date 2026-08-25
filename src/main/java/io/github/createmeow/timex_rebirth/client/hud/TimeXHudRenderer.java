package io.github.createmeow.timex_rebirth.client.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * 原生 HUD 渲染器：直接硬编码渲染 TimeX:ReBirth 自定义 HUD（替代 FancyMenu spiffy_overlay 布局）。
 *
 * <p>坐标语义对照 FancyMenu ElementAnchorPoint：
 * <ul>
 *   <li>bottom-centered：左上角 = (sw/2 + x, sh + y)；bottom-right：= (sw + x, sh + y)；等</li>
 *   <li>进度条：value_mode=percentage 时 progress/100，float 直用；smooth 时 0.95/0.05 平滑</li>
 *   <li>所有数据经 {@link HudValues} 读取，跨模组（RealityValue/ThirstWasTaken/ColdSweat/
 *       NumismaticOverhaul/ImmersiveAircraft）数据缺失时优雅降级</li>
 * </ul>
 */
public final class TimeXHudRenderer {

    public static final TimeXHudRenderer INSTANCE = new TimeXHudRenderer();

    /** 进度条平滑缓存（进度条标识 -> smoothed 0~1）。 */
    private final Map<String, Float> smoothedProgress = new HashMap<>();

    private TimeXHudRenderer() {
    }

    public void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(TimeX.rl("timex_hud"), (graphics, delta) -> render(graphics));
    }

    /** 布局/数据重载时清缓存。 */
    public void invalidate() {
        smoothedProgress.clear();
    }

    // ─────────────────────────── 主渲染入口 ───────────────────────────

    public void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (mc.level == null || p == null) return;
        if (mc.screen != null) return; // 打开界面时不渲染

        HudVariables.load();
        int sw = g.guiWidth();
        int sh = g.guiHeight();

        // 左下角盔甲槽背景 + 槽位
        renderArmorSlots(g, p, sw, sh);

        // 底部中央：血量文本 / 血条 / 吸收条
        boolean survival = isSurvival(p);
        if (survival) {
            renderHealthText(g, p, sw, sh);
            renderHealthBar(g, p, sw, sh);
            renderAbsorptionBar(g, p, sw, sh);
        }

        // 底部图标竖条：饥饿/口渴/健康/理智/盔甲（bottom-centered：X = sw/2 + x）
        if (survival) {
            // 农夫乐事"滋养"效果：饥饿条填充改用金色 hungry_nourish 材质（对照原版食物条变金）
            boolean nourished = hasEffect(p, FD_NOURISHMENT);
            renderIconBar(g, sw / 2 - 91, sh - 39, 12, 12, hungerPct(p), "hungry",
                    nourished ? "hungry_nourish" : null);
            renderIconBar(g, sw / 2 - 77, sh - 39, 12, 12, thirstPct(p), "thirsty");
            renderIconBar(g, sw / 2 - 63, sh - 39, 12, 12, rvHealthPct(p), "healthy");
            renderIconBar(g, sw / 2 - 49, sh - 39, 12, 12, rvSanityPct(p), "sanity");
            renderIconBar(g, sw / 2 - 35, sh - 39, 12, 12, armorPct(p), "armor");
        }
        // AppleSkin 兼容：安装 AppleSkin 时隐藏其原生覆盖条（AppleSkinOverlayMixin），
        // 改为在饥饿条/水分条上叠加 hungry_none / thirsty_none 显示饱和度与解渴度
        if (survival && isAppleSkinLoaded()) {
            renderSaturationOverlay(g, sw / 2 - 91, sh - 39, 12, 12, satPct(p), "hungry_none");
            renderSaturationOverlay(g, sw / 2 - 77, sh - 39, 12, 12, quenchedPct(p), "thirsty_none");
        }

        // 坐骑条（乘坐有生命实体时）
        if (p.getVehicle() instanceof LivingEntity mount && mount.getMaxHealth() > 0) {
            renderIconBar(g, sw / 2 - 21, sh - 39, 12, 12, mount.getHealth() / mount.getMaxHealth(), "mount");
            renderMountHealthText(g, p, mount, sw, sh);
            // 跳跃条 + 文本：布局条件 jump>1 是百分比（getJumpRidingScale()*100 > 1），不平滑
            if (p.getVehicle() instanceof PlayerRideableJumping) {
                float jumpPct = p.getJumpRidingScale() * 100f;
                if (jumpPct > 1f) {
                    renderVBar(g, sw / 2 + 13, sh - 139, 2, 24, p.getJumpRidingScale(), 0xFF825A8C, 0x5E000000, false);
                    String jt = fmt0(jumpPct) + "%";
                    int jw = Minecraft.getInstance().font.width(jt);
                    renderText(g, sw / 2 - jw / 2 + 26, sh - 130, jt, false);
                }
            }
        }

        // 飞机（沉浸式飞机）耐久/引擎
        if (HudValues.inAircraft()) {
            float dur = HudValues.aircraftDurability();
            if (dur > 0f) {
                renderIconBar(g, sw / 2 - 21, sh - 39, 12, 12, Mth.clamp(dur / 100f, 0f, 1f), "durability");
                String dt = fmt0(dur) + "%";
                int dw = Minecraft.getInstance().font.width(dt);
                renderText(g, sw / 2 - dw / 2 + 1, sh - 37, dt, false);
            }
            // 引擎条：布局 if_not(engine*1.0014 > 1) → 功率未满（engine*1.0014 <= 1）时才显示，满功率隐藏
            float engine = HudValues.aircraftEngine() * 1.00146253588741f;
            if (engine <= 0.9986f) {
                renderVBar(g, sw / 2 + 13, sh - 139, 2, 24, Mth.clamp(engine, 0f, 1f), 0xFFFF8000, 0x5E000000, true);
            }
        }

        // 氧气条（潜水，布局 smooth=false 不平滑）
        if (survival && p.getAirSupply() < p.getMaxAirSupply()) {
            float oxy = p.getAirSupply() / (float) p.getMaxAirSupply();
            renderVBar(g, sw / 2 + 11, sh / 2 - 12, 2, 24, oxy, 0xFF18FFED, 0x5E000000, false);
            String ot = fmt0(oxy * 100f) + "%";
            int ow = Minecraft.getInstance().font.width(ot);
            renderText(g, sw / 2 - ow / 2 + 26, sh - 130, ot, false);
        }

        // 经验条 + 等级（右下，平滑）
        if (survival) {
            renderVBar(g, sw - 32, sh - 22, 18, 18, p.experienceProgress, 0xFF89d848, 0x5E000000, true);
            // 等级文本：向左偏移 9 像素（经验条右边缘在 sw-14）
            String lt = Integer.toString(p.experienceLevel);
            int jw = Minecraft.getInstance().font.width(lt);
            int x = (sw - 14) - jw / 2 - 9;
            renderText(g, x, sh - 17, lt, false);
        }

        // 温度计区（底部中央偏右）
        renderThermometerArea(g, p, sw, sh);

        // 警示层（值<=6 时闪烁，bottom-centered 锚点，覆盖对应进度条）
        if (survival) {
            if (p.getFoodData().getFoodLevel() <= 6) renderWarning(g, sw / 2 - 91, sh - 39, 12, 12);
            if (HudValues.thirst() <= 6) renderWarning(g, sw / 2 - 77, sh - 39, 12, 12);
            if (HudValues.realityValueHealth() <= 6) renderWarning(g, sw / 2 - 63, sh - 39, 12, 12);
            if (HudValues.realityValueSanity() <= 6) renderWarning(g, sw / 2 - 49, sh - 39, 12, 12);
            if (p.getHealth() <= 6) renderWarning(g, sw / 2 - 90, sh - 25, 180, 3);
        }

        // 玩家实体（play=开 时，右侧中部）
        if (HudVariables.is("play", "开")) {
            renderPlayerEntity(g, p, sw - 43, sh / 2 - 70, 50, 50);
        }

        // Boss 条
        renderBossOverlay(g, sh / 2 - 75);

        // 顶部文本：金币 / 坐标 / FPS
        if (HudVariables.is("xyz", "开")) {
            renderText(g, 1, 11, "X: " + (int) Math.floor(p.getX()) + " Y: " + (int) Math.floor(p.getY())
                    + " Z: " + (int) Math.floor(p.getZ()) + " D:" + viewDir(p), false);
        }
        // 金币文本：使用 TimeX-Fontpark 的货币图标字体 U+EC00(铜)/U+EC02(银)/U+EC01(金)
        renderText(g, -1, 2, "\uEC00: " + fmt0(HudValues.numismaticBronze()) + " \uEC02: "
                + fmt0(HudValues.numismaticSilver()) + " \uEC01: " + fmt0(HudValues.numismaticGold()), false);
        renderText(g, 1, sh - 35, "FPS: " + HudValues.fps(), false);
    }

    // ─────────────────────────── 盔甲槽 ───────────────────────────

    private void renderArmorSlots(GuiGraphics g, LocalPlayer p, int sw, int sh) {
        // 4 个黑底方块（布局 shape：x=45/66/3/24, y=-20, 18x18；非 sticky bottom-left：Y = sh + y）
        int[] bgX = {45, 66, 3, 24};
        for (int bx : bgX) {
            g.fill(bx, sh - 20, bx + 18, sh - 2, 0x5E000000);
        }
        // 4 个盔甲槽（36=头盔 37=胸甲 38=护腿 39=靴子），20x20
        // sticky bottom-left：Y = sh - h + y = sh - 20 + (-1) = sh - 21
        int[] slotX = {44, 65, 2, 23};
        int[] slots = {38, 39, 36, 37};
        for (int i = 0; i < 4; i++) {
            ItemStack stack = p.getInventory().getItem(slots[i]);
            int sx = slotX[i];
            int sy = sh - 21;
            g.fill(sx, sy, sx + 20, sy + 20, 0x22000000);
            if (!stack.isEmpty()) {
                g.renderItem(stack, sx + 2, sy + 2);
                if (stack.isDamageableItem()) {
                    // 耐久条：宽度 = 物品图标 16px - 2px = 14px，居中于物品图标上方（z 提升保证在物品之上）
                    int max = stack.getMaxDamage();
                    int dmg = stack.getDamageValue();
                    if (max > 0) {
                        float pct = 1f - (float) dmg / max;
                        int barX = sx + 3; // 物品图标 sx+2~sx+18，14px 居中 → sx+3~sx+17
                        g.pose().pushPose();
                        g.pose().translate(0.0F, 0.0F, 200.0F);
                        g.fill(barX, sy + 15, barX + 14, sy + 16, 0xFF000000);
                        if (pct > 0f) {
                            g.fill(barX, sy + 15, barX + (int) (14 * pct), sy + 16,
                                    pct > 0.3f ? 0xFF55FF55 : 0xFFFF5555);
                        }
                        g.pose().popPose();
                    }
                }
            }
        }
    }

    // ─────────────────────────── 血量区 ───────────────────────────

    private void renderHealthText(GuiGraphics g, LocalPlayer p, int sw, int sh) {
        String text = fmt1(p.getMaxHealth()) + "/" + fmt1(p.getHealth() + p.getAbsorptionAmount());
        int tw = Minecraft.getInstance().font.width(text);
        renderText(g, sw / 2 - tw / 2 - 1, sh - 28, text, false);
    }

    private void renderHealthBar(GuiGraphics g, LocalPlayer p, int sw, int sh) {
        float prog = Mth.clamp(p.getHealth() / Math.max(1f, p.getMaxHealth()), 0f, 1f);
        // 背景
        g.fill(sw / 2 - 90, sh - 25, sw / 2 + 90, sh - 22, 0x5E000000);
        // 前景：农夫乐事"舒适"效果时改为蓝灰色（对照原版 comfort 的暖色氛围 → 冷色血条）
        int color = hasEffect(p, FD_COMFORT) ? 0xFF8fb3c7 : 0xFFcc1111;
        int pw = (int) (180 * smooth("health", prog));
        if (pw > 0) {
            g.fill(sw / 2 - 90, sh - 25, sw / 2 - 90 + pw, sh - 22, color);
        }
    }

    private void renderAbsorptionBar(GuiGraphics g, LocalPlayer p, int sw, int sh) {
        float abs = p.getAbsorptionAmount();
        if (abs <= 0f) return;
        float prog = Mth.clamp(abs / 20f, 0f, 1f);
        int pw = (int) (180 * smooth("absorption", prog));
        if (pw > 0) {
            g.fill(sw / 2 - 90, sh - 26, sw / 2 - 90 + pw, sh - 24, 0xFFf7bd02);
        }
    }

    // ─────────────────────────── 图标竖条 ───────────────────────────

    /** 渲染 12x12 向上填充图标条（如 hungry/armor/mount 等）。纹理在 gui/hud 目录。 */
    private void renderIconBar(GuiGraphics g, int x, int y, int w, int h, float progress, String name) {
        renderIconBar(g, x, y, w, h, progress, name, null);
    }

    /** 渲染 12x12 向上填充图标条；fillOverride 非空时前景改用该纹理（如滋养状态的 hungry_nourish）。 */
    private void renderIconBar(GuiGraphics g, int x, int y, int w, int h, float progress, String name,
                               @Nullable String fillOverride) {
        // 背景
        ResourceLocation bg = rl("textures/gui/hud/" + name + "_dark.png");
        int[] bgSize = HudTextureSizes.size(bg);
        blitWithAlpha(g, bg, x, y, w, h, 0, 0, bgSize[0], bgSize[1], bgSize[0], bgSize[1], 1f);
        // 前景（从底部向上）；平滑动画仍按基础名缓存，避免滋养效果切换时进度跳动
        float sm = smooth(name, Mth.clamp(progress, 0f, 1f));
        int ph = (int) (h * sm);
        if (ph <= 0) return;
        String fillName = fillOverride != null ? fillOverride : name;
        ResourceLocation fg = rl("textures/gui/hud/" + fillName + ".png");
        int[] fgSize = HudTextureSizes.size(fg);
        g.enableScissor(x, y + h - ph, x + w, y + h);
        blitWithAlpha(g, fg, x, y, w, h, 0, 0, fgSize[0], fgSize[1], fgSize[0], fgSize[1], 1f);
        g.disableScissor();
    }

    /** 在条上叠加饱和度/解渴度覆盖（AppleSkin 风格）：按比例从底部向上裁剪绘制。 */
    private void renderSaturationOverlay(GuiGraphics g, int x, int y, int w, int h, float pct, String name) {
        int ph = (int) (h * Mth.clamp(pct, 0f, 1f));
        if (ph <= 0) return;
        ResourceLocation tex = rl("textures/gui/hud/" + name + ".png");
        int[] size = HudTextureSizes.size(tex);
        g.enableScissor(x, y + h - ph, x + w, y + h);
        blitWithAlpha(g, tex, x, y, w, h, 0, 0, size[0], size[1], size[0], size[1], 1f);
        g.disableScissor();
    }

    /** 渲染细长竖直条（氧气/经验/跳跃/引擎）。smooth=true 应用平滑动画。 */
    private void renderVBar(GuiGraphics g, int x, int y, int w, int h, float progress, int fg, Integer bg, boolean smooth) {
        float sm;
        if (smooth) {
            sm = smooth("vbar-" + x + "-" + y, Mth.clamp(progress, 0f, 1f));
        } else {
            sm = Mth.clamp(progress, 0f, 1f);
        }
        if (bg != null) {
            g.fill(x, y, x + w, y + h, bg);
        }
        int ph = (int) (h * sm);
        if (ph > 0) {
            g.fill(x, y + h - ph, x + w, y + h, fg);
        }
    }

    // ─────────────────────────── 坐骑文本 ───────────────────────────

    private void renderMountHealthText(GuiGraphics g, LocalPlayer p, LivingEntity mount, int sw, int sh) {
        String text = fmt1(mount.getMaxHealth()) + "/" + fmt1(mount.getHealth());
        int tw = Minecraft.getInstance().font.width(text);
        renderText(g, sw / 2 - tw / 2 + 1, sh - 37, text, false);
    }

    // ─────────────────────────── 温度计区 ───────────────────────────

    private void renderThermometerArea(GuiGraphics g, LocalPlayer p, int sw, int sh) {
        // 温度计 item（cold_sweat:thermometer）32x16；布局 x=114，用户反馈右移 5 匹配文本 → 119
        // FancyMenu ItemElement 从元素左上角渲染物品（renderScaledItem: translate(x,y)），非居中
        int tx = sw / 2 + 119;
        int ty = sh - 21;
        // 背景框（布局 shape b4677df1：element 锚点相对 item，x=8 y=2 w=32 h=12）
        g.fill(tx + 8, ty + 2, tx + 40, ty + 14, 0x5E000000);
        // 温度计图标（16x16，从元素左上角渲染）
        ItemStack thermo = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM
                .get(ResourceLocation.fromNamespaceAndPath("cold_sweat", "thermometer")));
        if (!thermo.isEmpty()) {
            g.renderItem(thermo, tx, ty);
        }
        // 温度条（布局 61c160a4：bottom-centered x=23 y=-38 w=68 h=10，direction=left）
        int bx = sw / 2 + 23;
        int by = sh - 38;
        float prog = coldSweatPct(p);
        // 背景 cold.png（覆盖全条）
        ResourceLocation cold = rl("textures/gui/hud/cold.png");
        int[] cs = HudTextureSizes.size(cold);
        blitWithAlpha(g, cold, bx, by, 68, 10, 0, 0, cs[0], cs[1], cs[0], cs[1], 1f);
        // 前景 hot.png（从左往右填充 —— direction=left 时从右往左）
        float sm = smooth("coldsweat", Mth.clamp(prog, 0f, 1f));
        int pw = (int) (68 * sm);
        if (pw > 0) {
            ResourceLocation hot = rl("textures/gui/hud/hot.png");
            int[] hs = HudTextureSizes.size(hot);
            g.enableScissor(bx + 68 - pw, by, bx + 68, by + 10);
            blitWithAlpha(g, hot, bx, by, 68, 10, 0, 0, hs[0], hs[1], hs[0], hs[1], 1f);
            g.disableScissor();
        }
        // 指示器指针（布局 indicator 52f48a95：element 锚点相对温度条，随进度移动）
        int ix = bx + 68 - pw - 1; // 温度条当前进度边界
        ResourceLocation ind = rl("textures/gui/hud/indicator.png");
        int[] isz = HudTextureSizes.size(ind);
        blitWithAlpha(g, ind, ix, by - 1, 2, 12, 0, 0, isz[0], isz[1], isz[0], isz[1], 1f);
        // 温度文本（bottom-centered sticky，x=139+5 右移 5 对齐物品；Y 减文本高 9）
        String t = fmt1((float) HudValues.coldSweatWorldCelsius());
        int tw = Minecraft.getInstance().font.width(t);
        renderText(g, sw / 2 - tw / 2 + 144, sh - 17, t, false);
    }

    // ─────────────────────────── 警示层 ───────────────────────────

    /**
     * 警示闪烁：1s 内透明度 100%→0%，循环。对应值≤6 时调用。
     */
    private void renderWarning(GuiGraphics g, int x, int y, int w, int h) {
        long t = System.currentTimeMillis() % 1000;
        float alpha = 1f - (t / 1000f);
        ResourceLocation warn = rl("textures/gui/warning/image_00.png");
        int[] size = HudTextureSizes.size(warn);
        blitWithAlpha(g, warn, x, y, w, h, 0, 0, size[0], size[1], size[0], size[1], alpha);
    }

    // ─────────────────────────── 玩家实体 ───────────────────────────

    private void renderPlayerEntity(GuiGraphics g, LocalPlayer p, int x, int y, int w, int h) {
        int cx = x + w / 2;
        int cy = y + h / 2;
        float scale = Math.max(w, h) / 50.0f;
        InventoryScreen.renderEntityInInventoryFollowsAngle(g, cx, cy, (int) (scale * 30f),
                0, 0, 0, -30, 0, p);
    }

    // ─────────────────────────── Boss 条 ───────────────────────────

    private void renderBossOverlay(GuiGraphics g, int yPos) {
        Minecraft mc = Minecraft.getInstance();
        try {
            Object bossOverlay = null;
            try {
                java.lang.reflect.Field f = net.minecraft.client.gui.Gui.class.getDeclaredField("bossOverlay");
                f.setAccessible(true);
                bossOverlay = f.get(mc.gui);
            } catch (NoSuchFieldException nsfe) {
                return;
            }
            if (bossOverlay == null) return;
            java.lang.reflect.Field eventsField = bossOverlay.getClass().getDeclaredField("events");
            eventsField.setAccessible(true);
            Map<?, ?> events = (Map<?, ?>) eventsField.get(bossOverlay);
            if (events == null || events.isEmpty()) return;
            int sw = g.guiWidth();
            int xPos = sw / 2 - 91;
            int[] idx = {0};
            events.values().forEach(ev -> {
                if (ev instanceof LerpingBossEvent event) {
                    ResourceLocation bg = bossBarTexture(event.getColor(), false);
                    ResourceLocation fg = bossBarTexture(event.getColor(), true);
                    int my = yPos + idx[0] * 19;
                    g.blitSprite(bg, xPos, my, 182, 5);
                    float prog = event.getProgress();
                    if (prog > 0) {
                        g.blitSprite(fg, xPos, my, (int) (182 * prog), 5);
                    }
                    Component name = event.getName();
                    int nw = mc.font.width(name);
                    g.drawString(mc.font, name, sw / 2 - nw / 2, my - 9, 0xFFFFFFFF);
                    idx[0]++;
                }
            });
        } catch (Exception ex) {
            // 忽略
        }
    }

    private static ResourceLocation bossBarTexture(net.minecraft.world.BossEvent.BossBarColor color, boolean progress) {
        String name = color.getName();
        String dir = progress ? "boss_bar/" + name + "_progress" : "boss_bar/" + name + "_background";
        return ResourceLocation.withDefaultNamespace(dir);
    }

    // ─────────────────────────── 文本 ───────────────────────────

    private void renderText(GuiGraphics g, int x, int y, String text, boolean center) {
        if (text == null || text.isEmpty()) return;
        Font font = Minecraft.getInstance().font;
        if (center) {
            x -= font.width(text) / 2;
        }
        g.drawString(font, text, x, y, 0xFFFFFFFF, true);
    }

    // ─────────────────────────── 工具 ───────────────────────────

    private static boolean isSurvival(LocalPlayer p) {
        return p.isAlive() && !p.isCreative() && !p.isSpectator();
    }

    /** 农夫乐事"滋养"效果（该效果 1.21.1 无原版对应，仅 FD 提供）。 */
    private static final ResourceLocation FD_NOURISHMENT = ResourceLocation.parse("farmersdelight:nourishment");
    /** 农夫乐事"舒适"效果。 */
    private static final ResourceLocation FD_COMFORT = ResourceLocation.parse("farmersdelight:comfort");

    /** 玩家是否持有指定效果（按注册表 id 查找，模组未安装时返回 false）。 */
    private static boolean hasEffect(LocalPlayer p, ResourceLocation effectId) {
        if (p == null) return false;
        return BuiltInRegistries.MOB_EFFECT.getHolder(effectId)
                .map(p::hasEffect).orElse(false);
    }

    /** 是否安装 AppleSkin（决定饱和度/解渴度覆盖条显示）。 */
    private static boolean isAppleSkinLoaded() {
        return ModList.get() != null && ModList.get().isLoaded("appleskin");
    }

    private static float satPct(LocalPlayer p) {
        return Mth.clamp(p.getFoodData().getSaturationLevel() / 20f, 0f, 1f);
    }

    private static float quenchedPct(LocalPlayer p) {
        return Mth.clamp(HudValues.thirstQuenched() / 20f, 0f, 1f);
    }

    private static float hungerPct(LocalPlayer p) {
        return p.getFoodData().getFoodLevel() / 20f;
    }

    private static float thirstPct(LocalPlayer p) {
        return Mth.clamp(HudValues.thirst() / 20f, 0f, 1f);
    }

    private static float rvHealthPct(LocalPlayer p) {
        float max = HudValues.realityValueMaxHealth();
        return max <= 0 ? 0 : Mth.clamp(HudValues.realityValueHealth() / max, 0f, 1f);
    }

    private static float rvSanityPct(LocalPlayer p) {
        float max = HudValues.realityValueMaxHealth();
        return max <= 0 ? 0 : Mth.clamp(HudValues.realityValueSanity() / max, 0f, 1f);
    }

    private static float armorPct(LocalPlayer p) {
        return Mth.clamp(p.getArmorValue() / 20f, 0f, 1f);
    }

    private static float coldSweatPct(LocalPlayer p) {
        double core = HudValues.coldSweatCore();
        return (float) Mth.clamp((core + 100) / 200, 0f, 1f);
    }

    private static String viewDir(LocalPlayer p) {
        return switch (p.getDirection()) {
            case NORTH -> "北";
            case SOUTH -> "南";
            case WEST -> "西";
            case EAST -> "东";
            default -> "?";
        };
    }

    private float smooth(String id, float progress) {
        Float cached = smoothedProgress.get(id);
        float sm = cached == null ? progress : cached;
        sm = Mth.clamp(sm * 0.95f + progress * 0.05f, 0f, 1f);
        if (Math.abs(sm - progress) < 0.001f) sm = progress;
        smoothedProgress.put(id, sm);
        return sm;
    }

    /** 四舍五入去尾零（20.0 → "20"，12.3 → "12.3"）。 */
    private static String fmt1(float v) {
        float r = Math.round(v * 10f) / 10f;
        if (r == (int) r) return Integer.toString((int) r);
        return String.format(java.util.Locale.ROOT, "%.1f", r);
    }

    private static String fmt0(float v) {
        return Long.toString(Math.round(v));
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(TimeX.MODID, path);
    }

    /** 带 alpha 的纹理绘制（半透明 PNG 需启用 blend）。 */
    private static void blitWithAlpha(GuiGraphics g, ResourceLocation tex, int x, int y,
                                      int w, int h, float u, float v, int uW, int uH,
                                      int texW, int texH, float alpha) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        g.setColor(1f, 1f, 1f, alpha);
        g.blit(tex, x, y, w, h, u, v, uW, uH, texW, texH);
        g.setColor(1f, 1f, 1f, 1f);
    }
}
