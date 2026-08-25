package io.github.createmeow.timex_rebirth.wheelmenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Predicate;

/**
 * 轮盘选项（参考 FrostedHeart wheelmenu Selection）。
 * 包含选中行为、悬停行为、可见性谓词与自定义颜色。
 */
@OnlyIn(Dist.CLIENT)
public class WheelSelection {
    public static final Predicate<WheelSelection> ALWAYS_VISIBLE = s -> true;

    private final Component message;
    private final ItemStack icon;
    private final Action selectAction;
    private final Action hoverAction;
    private final Predicate<WheelSelection> visibility;
    private final int color;
    private boolean visible = true;
    private boolean hovered = false;

    public WheelSelection(Component message, ItemStack icon, Runnable action) {
        this(message, icon, Action.of(action), Action.NO_ACTION, ALWAYS_VISIBLE, 0x00E5FF);
    }

    WheelSelection(Component message, ItemStack icon, Action selectAction, Action hoverAction,
                   Predicate<WheelSelection> visibility, int color) {
        this.message = message;
        this.icon = icon;
        this.selectAction = selectAction;
        this.hoverAction = hoverAction;
        this.visibility = visibility;
        this.color = color;
    }

    public Component getMessage() {
        return message;
    }

    public ItemStack getIcon() {
        return icon;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isHovered() {
        return hovered;
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    public int getColor() {
        return color;
    }

    /**
     * 每帧/每刻更新可见性（由渲染器调用）。
     */
    public void tick() {
        visible = visibility.test(this);
    }

    /**
     * 执行选中行为。
     */
    public void execute() {
        selectAction.execute(this);
    }

    /**
     * 执行悬停行为（悬停到该选项时）。
     */
    public void onHover() {
        hoverAction.execute(this);
    }

    /**
     * Render the selection item at the given position.
     *
     * @param graphics the GuiGraphics context
     * @param x        centre x
     * @param y        centre y
     * @param hovered  whether this item is currently hovered
     * @param alpha    global menu alpha (0-1, from open/close animation)
     */
    public void render(GuiGraphics graphics, int x, int y, boolean hovered, float alpha) {
        if (!visible) return;

        var pose = graphics.pose();
        pose.pushPose();

        if (hovered) {
            // Scale up slightly when hovered for a tactile feel
            pose.translate(x, y, 0);
            pose.scale(1.25f, 1.25f, 1);
            pose.translate(-x, -y, 0);
        }

        // Render item icon (8x8 offset to centre the 16x16 item)
        graphics.renderItem(icon, x - 8, y - 8);

        pose.popPose();
    }
}
