package io.github.createmeow.timex_rebirth.client;

import io.github.createmeow.timex_rebirth.heat.station.HeatStationMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

/**
 * 热源发生器界面（200×200 面板，纯矩形绘制，不依赖 GUI 纹理）：
 * - 标题行右侧附加"增产/节能"模块等级
 * - 左侧信息列：结构血量、热流液位条、熔岩液位条、燃烧进度条、燃料槽说明
 * - 燃料槽位于右侧中部，避免与文本/物品栏重叠；不绘制原版"物品栏"标题
 */
public class HeatStationScreen extends AbstractContainerScreen<HeatStationMenu> {

    public HeatStationScreen(HeatStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 200;
        this.imageHeight = 200;
    }

    private ContainerData data() {
        return this.menu.getData();
    }

    /** 信息条（x 起点、y 起点、当前量、上限、颜色），固定 140px 宽，未满部分深色背景。 */
    private void drawBar(GuiGraphics graphics, int x, int y, int amount, int capacity, int color) {
        int filled = Math.min(140, (int) (140L * amount / Math.max(1, capacity)));
        graphics.fill(x, y, x + filled, y + 6, color);
        graphics.fill(x + filled, y, x + 140, y + 6, 0xFF303236);
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
        int x = this.leftPos;
        int y = this.topPos;

        // 面板
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF101010);
        graphics.fill(x + 2, y + 2, x + this.imageWidth - 2, y + this.imageHeight - 2, 0xFF26282C);

        ContainerData data = this.data();
        // 热流液位条（琥珀色）
        drawBar(graphics, x + 10, y + 38, data.get(0), data.get(1), 0xFFFFB347);
        // 熔岩液位条（红色）
        drawBar(graphics, x + 10, y + 58, data.get(2), data.get(3), 0xFFE25822);
        // 固体燃料燃烧进度条
        int burnTotal = Math.max(1, data.get(5));
        drawBar(graphics, x + 10, y + 72, Math.min(burnTotal, data.get(4)), burnTotal, 0xFFFF8A3D);

        // 槽位背景框（无 GUI 纹理，需手动绘制，否则空槽位不可见、物品悬浮无底）
        drawSlot(graphics, x + 80, y + 78); // 燃料槽
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(graphics, x + 19 + col * 18, y + 118 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(graphics, x + 19 + col * 18, y + 172);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        ContainerData data = this.data();

        // 标题行：热源发生器 + 模块等级（灰色，追加在标题右侧）
        Component title = Component.literal(
                        Component.translatable("container.timex_rebirth.heat_station").getString())
                .append(Component.translatable("gui.timex_rebirth.heat_station.modules",
                        data.get(6), data.get(7)).withStyle(ChatFormatting.GRAY));
        graphics.drawString(this.font, title, 8, 6, 0xFFFFFF);

        // 结构血量
        graphics.drawString(this.font,
                Component.translatable("gui.timex_rebirth.heat_station.health", data.get(8), data.get(9)),
                8, 20, 0xFFAA55);
        // 热流 / 熔岩
        graphics.drawString(this.font,
                Component.translatable("gui.timex_rebirth.heat_station.heat", data.get(0), data.get(1)),
                8, 30, 0xFFAA00);
        graphics.drawString(this.font,
                Component.translatable("gui.timex_rebirth.heat_station.lava", data.get(2), data.get(3)),
                8, 50, 0xFFE25822);
        // 固体燃料槽说明（紧邻燃料槽下方）
        graphics.drawString(this.font,
                Component.translatable("gui.timex_rebirth.heat_station.fuel_slot"),
                8, 98, 0x777777);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
