package com.createmeow.currency_plugin.client;

import com.createmeow.currency_plugin.cap.CurrencyHolder;
import com.createmeow.currency_plugin.currency.Currency;
import com.createmeow.currency_plugin.init.CurrencyItems;
import com.createmeow.currency_plugin.network.PurseActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 背包界面右上角的钱币按钮 + 取出面板。
 * <p>点击按钮后，面板覆盖背包的 2×2 合成槽区域（不向背包 UI 外突出），
 * 避免与右侧的 JEI 收藏夹/物品列表冲突。
 * <p>面板布局（2 行）：
 * <pre>
 * 钱包  取出状态        [§a√]
 * [coin_icon] [输入框]
 * </pre>
 * <p>取出状态：输入金额超过余额时显示 §c余额不足；点击取出后显示 §a取出 N（2 秒）。
 */
public class PurseOverlay {

    private static boolean panelOpen = false;
    private static Currency selectedType = Currency.COMMON;
    private static EditBox amountField;

    // ── 布局 ──
    private static final int BTN_SIZE = 20;
    private static final int PANEL_X_OFF = 75;
    private static final int PANEL_Y_OFF = 4;
    private static final int PANEL_W = 81;
    private static final int PANEL_H = 38;

    // 第 1 行：标题 + √ 按钮
    private static final int ROW1_Y = 3;
    private static final int CHECK_BTN_W = 14;
    private static final int CHECK_BTN_H = 14;
    private static final int CHECK_BTN_X = PANEL_W - 2 - CHECK_BTN_W; // 65

    // 第 2 行：类型图标 + 输入框
    private static final int ROW2_Y = 19;
    private static final int ICON_X = 3;
    private static final int ICON_Y = ROW2_Y - 1;
    private static final int FIELD_X = 21;
    private static final int FIELD_Y = ROW2_Y;
    private static final int FIELD_W = PANEL_W - FIELD_X - 2; // 58

    // ── 缓存坐标 ──
    private static int buttonX, buttonY;
    private static int panelX, panelY;
    private static boolean active = false;

    // ── 按压状态：0=无 1=主按钮 2=√按钮 3=类型图标 ──
    private static int pressTarget = 0;

    // ── 取出状态：点击取出后的提示（2 秒） ──
    private static String extractedStatus = "";
    private static long extractedStatusUntil = 0;

    // ── 颜色 ──
    private static final int COL_BG = 0xE0222222;
    private static final int COL_BORDER = 0xFF888888;
    private static final int COL_BTN = 0xFF333333;
    private static final int COL_BTN_HOVER = 0xFF666666;
    private static final int COL_BTN_PRESSED = 0xFF191919;
    private static final int COL_TITLE = 0xFFFFAA00;

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (screen instanceof InventoryScreen) {
            int leftPos = (screen.width - 176) / 2;
            int topPos = (screen.height - 166) / 2;
            buttonX = leftPos + 152;
            buttonY = topPos + 4;
            panelX = leftPos + PANEL_X_OFF;
            panelY = topPos + PANEL_Y_OFF;
            active = true;
        } else if (screen instanceof CreativeModeInventoryScreen) {
            int leftPos = (screen.width - 195) / 2;
            int topPos = (screen.height - 136) / 2;
            buttonX = leftPos + 172;
            buttonY = topPos + 4;
            panelX = -1;
            active = true;
        } else {
            active = false;
            return;
        }

        GuiGraphics g = event.getGuiGraphics();
        var mc = Minecraft.getInstance();
        var win = mc.getWindow();
        int mx = (int) (mc.mouseHandler.xpos() * (double) win.getGuiScaledWidth() / (double) win.getScreenWidth());
        int my = (int) (mc.mouseHandler.ypos() * (double) win.getGuiScaledHeight() / (double) win.getScreenHeight());

        renderButton(g, mx, my);
        if (panelOpen && panelX >= 0) {
            ensureEditBox();
            amountField.setX(panelX + FIELD_X);
            amountField.setY(panelY + FIELD_Y);
            renderPanel(g, mx, my, event.getPartialTick());
        }
    }

    @SubscribeEvent
    public static void onMouseRelease(ScreenEvent.MouseButtonReleased.Post event) {
        if (event.getButton() == 0) pressTarget = 0;
    }

    @SubscribeEvent
    public static void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!active || event.getButton() != 0) return;
        int mx = (int) event.getMouseX();
        int my = (int) event.getMouseY();

        // 主按钮
        if (inRect(mx, my, buttonX, buttonY, BTN_SIZE, BTN_SIZE)) {
            pressTarget = 1;
            playClick();
            if (Screen.hasShiftDown()) {
                PacketDistributor.sendToServer(PurseActionPayload.storeAll());
            } else {
                panelOpen = !panelOpen;
                if (amountField != null) {
                    amountField.setValue("");
                    amountField.setFocused(false);
                }
            }
            event.setCanceled(true);
            return;
        }

        // 面板内
        if (panelOpen && panelX >= 0 && inRect(mx, my, panelX, panelY, PANEL_W, PANEL_H)) {
            handlePanelClick(mx, my);
            event.setCanceled(true);
            return;
        }

        // 面板外：关闭
        if (panelOpen) {
            panelOpen = false;
            if (amountField != null) {
                amountField.setValue("");
                amountField.setFocused(false);
            }
        }
    }

    @SubscribeEvent
    public static void onCharTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (!panelOpen || !active || amountField == null || !amountField.isFocused()) return;
        amountField.charTyped(event.getCodePoint(), event.getModifiers());
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!panelOpen || !active || amountField == null || !amountField.isFocused()) return;
        int keyCode = event.getKeyCode();

        // Enter：取出
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            doExtract();
            event.setCanceled(true);
            return;
        }
        // Escape：仅取消输入框焦点，不关闭背包
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            amountField.setFocused(false);
            event.setCanceled(true);
            return;
        }
        // 其余按键转发给输入框并拦截：
        // 阻止数字快捷键 1-9 交换物品、E 关闭背包等界面行为
        amountField.keyPressed(keyCode, event.getScanCode(), event.getModifiers());
        event.setCanceled(true);
    }

    // ─────────────────────────── 渲染 ───────────────────────────

    private static void renderButton(GuiGraphics g, int mx, int my) {
        boolean hover = inRect(mx, my, buttonX, buttonY, BTN_SIZE, BTN_SIZE);
        boolean pressed = pressTarget == 1 && hover;
        int bg = pressed ? COL_BTN_PRESSED : (hover ? COL_BTN_HOVER : COL_BTN);
        g.fill(buttonX, buttonY, buttonX + BTN_SIZE, buttonY + BTN_SIZE, bg);
        drawBorder(g, buttonX, buttonY, BTN_SIZE, BTN_SIZE, COL_BORDER);
        int off = pressed ? 1 : 0;
        g.renderItem(new ItemStack(CurrencyItems.RARE_COIN.get()), buttonX + 2 + off, buttonY + 2 + off);
    }

    private static void renderPanel(GuiGraphics g, int mx, int my, float partialTick) {
        var font = Minecraft.getInstance().font;

        // 背景 + 边框
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, COL_BG);
        drawBorder(g, panelX, panelY, PANEL_W, PANEL_H, COL_BORDER);

        // ── 第 1 行：标题（左）+ 取出状态（中）+ √ 按钮（右）──
        g.drawString(font, Component.translatable("gui.currency_plugin.purse_title"),
                panelX + 4, panelY + ROW1_Y + 3, COL_TITLE);

        int cbX = panelX + CHECK_BTN_X;
        int cbY = panelY + ROW1_Y;
        String status = getStatusText();
        if (!status.isEmpty()) {
            int statusX = cbX - 2 - font.width(status);
            if (statusX < panelX + 26) statusX = panelX + 26;
            g.drawString(font, status, statusX, panelY + ROW1_Y + 3, 0xFFFFFFFF);
        }

        boolean cbHover = inRect(mx, my, cbX, cbY, CHECK_BTN_W, CHECK_BTN_H);
        boolean cbPressed = pressTarget == 2 && cbHover;
        g.fill(cbX, cbY, cbX + CHECK_BTN_W, cbY + CHECK_BTN_H, cbPressed ? COL_BTN_PRESSED : (cbHover ? COL_BTN_HOVER : COL_BTN));
        drawBorder(g, cbX, cbY, CHECK_BTN_W, CHECK_BTN_H, COL_BORDER);
        int cbOff = cbPressed ? 1 : 0;
        g.drawCenteredString(font, "\u00A7a\u221A",
                cbX + CHECK_BTN_W / 2 + cbOff, cbY + 3 + cbOff, 0xFFFFFFFF);

        // ── 第 2 行：类型图标（左）+ 输入框（右）──
        boolean iconPressed = pressTarget == 3 && inRect(mx, my, panelX + ICON_X, panelY + ICON_Y, 16, 16);
        int iconOff = iconPressed ? 1 : 0;
        g.renderItem(new ItemStack(selectedType.asItem()), panelX + ICON_X + iconOff, panelY + ICON_Y + iconOff);
        amountField.render(g, mx, my, partialTick);
    }

    // ─────────────────────────── 点击处理 ───────────────────────────

    private static void handlePanelClick(int mx, int my) {
        // √ 按钮（第 1 行右侧）
        int cbX = panelX + CHECK_BTN_X;
        int cbY = panelY + ROW1_Y;
        if (inRect(mx, my, cbX, cbY, CHECK_BTN_W, CHECK_BTN_H)) {
            pressTarget = 2;
            playClick();
            doExtract();
            return;
        }

        // 类型图标（第 2 行左侧）— 点击切换类型
        if (inRect(mx, my, panelX + ICON_X, panelY + ICON_Y, 16, 16)) {
            pressTarget = 3;
            playClick();
            selectedType = (selectedType == Currency.COMMON) ? Currency.RARE : Currency.COMMON;
            return;
        }

        // 输入框
        if (inRect(mx, my, panelX + FIELD_X, panelY + FIELD_Y, FIELD_W, 14)) {
            amountField.mouseClicked(mx, my, 0);
            amountField.setFocused(true);
            return;
        }

        // 其他区域：取消焦点
        amountField.setFocused(false);
    }

    private static void doExtract() {
        if (amountField == null) return;
        String text = amountField.getValue();
        if (text.isEmpty()) return;
        try {
            long amount = Long.parseLong(text);
            if (amount > 0) {
                long balance = CurrencyHolder.getBalance(Minecraft.getInstance().player, selectedType);
                long actual = Math.min(amount, balance);
                // 服务端会钳制到全部余额；状态提示显示实际取出量（枚数，不换算）
                extractedStatus = actual > 0
                        ? "\u00A7a" + Component.translatable("gui.currency_plugin.extracted", actual).getString()
                        : "\u00A7c" + Component.translatable("gui.currency_plugin.no_balance").getString();
                extractedStatusUntil = System.currentTimeMillis() + 2000;
                PacketDistributor.sendToServer(PurseActionPayload.extract(selectedType, amount));
                amountField.setValue("");
            }
        } catch (NumberFormatException ignored) {
        }
    }

    /**
     * 取出状态文本：
     * <ul>
     *   <li>点击取出后 2 秒内 → 显示实际取出量（§a取出 N / §c余额不足）</li>
     *   <li>输入金额超过当前类型的客户端余额 → 显示 §c余额不足</li>
     *   <li>其他（空闲）→ 显示当前类型余额（§7余额 N）</li>
     * </ul>
     */
    private static String getStatusText() {
        if (System.currentTimeMillis() < extractedStatusUntil) return extractedStatus;
        long balance = CurrencyHolder.getBalance(Minecraft.getInstance().player, selectedType);
        if (amountField != null && !amountField.getValue().isEmpty()) {
            try {
                long amount = Long.parseLong(amountField.getValue());
                if (amount > balance) {
                    return "\u00A7c" + Component.translatable("gui.currency_plugin.no_balance").getString();
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return "\u00A77" + Component.translatable("gui.currency_plugin.balance", balance).getString();
    }

    // ─────────────────────────── 工具 ───────────────────────────

    private static void ensureEditBox() {
        if (amountField == null) {
            var font = Minecraft.getInstance().font;
            amountField = new EditBox(font, 0, 0, FIELD_W, 14, Component.literal(""));
            amountField.setHint(Component.translatable("gui.currency_plugin.amount_hint"));
            amountField.setMaxLength(9);
            amountField.setFilter(s -> s.isEmpty() || s.matches("\\d{1,9}"));
        }
    }

    private static boolean inRect(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static void playClick() {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
