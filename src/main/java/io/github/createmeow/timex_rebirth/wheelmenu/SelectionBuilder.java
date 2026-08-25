package io.github.createmeow.timex_rebirth.wheelmenu;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Predicate;

/**
 * 轮盘选项流式构造器（参考 FrostedHeart SelectionBuilder）：
 * <pre>
 * SelectionBuilder.create()
 *     .message(Component.translatable("..."))
 *     .icon(Items.COMPASS)
 *     .visibleWhen(s -> XaeroCompat.isLoaded())
 *     .selected(() -> XaeroCompat::createTemporaryWaypoint)
 *     .build();
 * </pre>
 * 亦可通过 {@link #fromKey} 直接绑定一个按键映射。
 */
@OnlyIn(Dist.CLIENT)
public class SelectionBuilder {

    private Predicate<WheelSelection> visibility = WheelSelection.ALWAYS_VISIBLE;
    private Action selectAction = Action.NO_ACTION;
    private Action hoverAction = Action.NO_ACTION;
    private ItemStack icon = ItemStack.EMPTY;
    private Component message = Component.empty();
    private int color = 0x00E5FF;

    protected SelectionBuilder() {
    }

    public static SelectionBuilder create() {
        return new SelectionBuilder();
    }

    /**
     * 绑定按键映射：选中时触发指定按键。
     *
     * @param key  按键映射名称（KeyMapping.getName()）
     * @param icon 显示图标
     */
    public static SelectionBuilder fromKey(String key, ItemStack icon) {
        return create().message(Component.translatable(key)).icon(icon).selected(new KeyMappingTriggerAction(key));
    }

    public SelectionBuilder defaultHidden() {
        visibility = s -> false;
        return this;
    }

    public SelectionBuilder visibleWhen(Predicate<WheelSelection> condition) {
        visibility = condition;
        return this;
    }

    public SelectionBuilder hovered(Action action) {
        hoverAction = action;
        return this;
    }

    public SelectionBuilder hovered(Runnable action) {
        return hovered(Action.of(action));
    }

    public SelectionBuilder selected(Action action) {
        selectAction = action;
        return this;
    }

    public SelectionBuilder selected(Runnable action) {
        return selected(Action.of(action));
    }

    public SelectionBuilder message(Component message) {
        this.message = message;
        return this;
    }

    public SelectionBuilder color(int color) {
        this.color = color;
        return this;
    }

    public SelectionBuilder icon(ItemStack icon) {
        this.icon = icon;
        return this;
    }

    public SelectionBuilder icon(ItemLike item) {
        this.icon = new ItemStack(item);
        return this;
    }

    public WheelSelection build() {
        return new WheelSelection(message, icon, selectAction, hoverAction, visibility, color);
    }

    public void register(WheelMenuSelectionRegisterEvent event, ResourceLocation name) {
        event.register(name, build());
    }
}
