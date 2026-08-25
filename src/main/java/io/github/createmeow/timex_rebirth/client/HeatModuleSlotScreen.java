package io.github.createmeow.timex_rebirth.client;

import io.github.createmeow.timex_rebirth.heat.station.HeatModuleSlotMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 模块插槽界面：展示 6 格模块容器与说明。
 */
public class HeatModuleSlotScreen extends AbstractContainerScreen<HeatModuleSlotMenu> {

    public HeatModuleSlotScreen(HeatModuleSlotMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    /**
     * 槽位背景框：以槽位坐标 (slotX, slotY) 为准，实际绘制位置 (slotX-1, slotY-1) 的 18×18 框，
     * 与物品图标（slotX, slotY 的 16×16）及悬停高亮覆盖层（slotX-1, slotY-1 的 16×16）对齐。
     */
    private void drawSlot(GuiGraphics graphics, int slotX, int slotY) {
        graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF8B8B8B);
        graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF303236);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        graphics.fill(x + 3, y + 3, x + this.imageWidth - 3, y + this.imageHeight - 3, 0xC6151515);
        graphics.drawString(this.font, this.title, x + 8, y + 6, 0xFFFFFF);
        graphics.drawString(this.font,
                Component.translatable("gui.timex_rebirth.heat_module_slot.hint"),
                x + 8, y + 50, 0xAAAAAA);

        // 槽位背景框（无 GUI 纹理，需手动绘制，否则空槽位不可见、物品悬浮无底）
        for (int i = 0; i < 6; i++) {
            drawSlot(graphics, x + 62 + (i % 3) * 18, y + 20 + (i / 3) * 18); // 6 格模块槽
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(graphics, x + 8 + col * 18, y + 84 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(graphics, x + 8 + col * 18, y + 142);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
