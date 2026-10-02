package com.createmeow.currency_plugin.client;

import com.createmeow.currency_plugin.machine.RefinerBlockEntity;
import com.createmeow.currency_plugin.machine.RefinerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 提炼机界面：2 个槽位（原料输入 / 产物输出）+ 提炼量进度条。
 * 背景使用程序化绘制（fill），无需 GUI 贴图。
 * 进度条与数值文本使用逐帧指数插值平滑过渡。
 */
public class RefinerScreen extends AbstractContainerScreen<RefinerMenu> {

    private static final int COL_BG = 0xF0101018;
    private static final int COL_SLOT = 0xFF3A3A44;
    private static final int COL_SLOT_BORDER = 0xFF15151C;
    private static final int COL_LABEL = 0xFFAAAAAA;
    private static final int COL_TEXT = 0xFFFFFFFF;
    private static final int COL_BAR_BG = 0xFF2A2A32;
    private static final int COL_BAR_FILL = 0xFF55FF55;
    private static final int COL_BAR_BORDER = 0xFF888888;

    /** 平滑系数（每 tick 收敛比例，值越大越快）。 */
    private static final float SMOOTH_FACTOR = 0.25f;

    /** 平滑显示中的提炼量（浮点）。 */
    private float smoothPoints;
    /** 上一帧的平滑值，用于与 partialTick 插值。 */
    private float prevSmoothPoints;

    public RefinerScreen(RefinerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelY = 6;
        // 打开界面时直接对齐当前值，避免从 0 动画
        this.smoothPoints = this.menu.getRefinePoints();
        this.prevSmoothPoints = this.smoothPoints;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // ── 平滑推进 ──
        // 先用上一帧的插值渲染，再按本帧推进（指数趋近，帧率无关）
        float prev = this.prevSmoothPoints;
        float curr = this.smoothPoints;

        // 主面板（覆盖机器区 + 玩家背包区）
        g.fill(x, y, x + this.imageWidth, y + this.imageHeight, COL_BG);
        drawBorder(g, x, y, this.imageWidth, this.imageHeight, COL_BAR_BORDER);

        // 机器槽位（与 Menu 中 Slot 坐标一致：物品渲染在 slot.x/slot.y 的 16×16 区域）
        drawSlotBg(g, x + 55, y + 34);
        drawSlotBg(g, x + 115, y + 34);

        // 槽位标签（与槽位左对齐）
        var font = this.font;
        g.drawString(font, Component.translatable("gui.currency_plugin.refiner.input"), x + 56, y + 24, COL_LABEL);
        g.drawString(font, Component.translatable("gui.currency_plugin.refiner.output"), x + 116, y + 24, COL_LABEL);

        // 提炼量进度条：精确对齐两个槽位背景的外缘（55 → 133）
        int barX = x + 55;
        int barY = y + 56;
        int barW = 78;
        int barH = 8;
        g.fill(barX, barY, barX + barW, barY + barH, COL_BAR_BG);
        drawBorder(g, barX, barY, barW, barH, COL_BAR_BORDER);
        int fillW = (int) ((prev / (float) RefinerBlockEntity.POINTS_PER_COIN) * (barW - 2));
        if (fillW > 0) {
            g.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + barH - 1, COL_BAR_FILL);
        }
        // 提炼量数值（平滑取整显示）
        String text = Math.round(prev) + "/" + RefinerBlockEntity.POINTS_PER_COIN;
        g.drawCenteredString(font, text, barX + barW / 2, y + 67, COL_TEXT);

        // ── 推进到下一帧 ──
        int target = this.menu.getRefinePoints();
        // 产出钱币后数值回退时不做反向动画，直接跳到目标
        if (target < curr) {
            this.smoothPoints = target;
            this.prevSmoothPoints = target;
        } else {
            this.prevSmoothPoints = curr;
            float next = curr + (target - curr) * SMOOTH_FACTOR;
            if (Math.abs(target - next) < 0.01f) next = target;
            this.smoothPoints = next;
        }
    }

    private void drawSlotBg(GuiGraphics g, int x, int y) {
        // 18×18 槽位背景：物品 16×16 区域内缩 1px（与原版槽位绘制一致）
        g.fill(x, y, x + 18, y + 18, COL_SLOT);
        drawBorder(g, x, y, 18, 18, COL_SLOT_BORDER);
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
