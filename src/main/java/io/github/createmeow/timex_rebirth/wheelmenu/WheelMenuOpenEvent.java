package io.github.createmeow.timex_rebirth.wheelmenu;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

import java.util.List;

/**
 * 轮盘打开事件（参考 FrostedHeart WheelMenuOpenEvent）：
 * 在尝试打开轮盘菜单时于游戏总线触发。取消该事件可阻止轮盘打开；
 * 也可通过 hide/showFirst/showLast/showBefore/showAfter 调整选项顺序与显隐。
 */
public class WheelMenuOpenEvent extends Event implements ICancellableEvent {
    private final List<ResourceLocation> toShow;

    public WheelMenuOpenEvent(List<ResourceLocation> toShow) {
        this.toShow = toShow;
    }

    public void showFirst(ResourceLocation id) {
        toShow.add(0, id);
    }

    public void showLast(ResourceLocation id) {
        toShow.add(id);
    }

    public void showBefore(ResourceLocation id, ResourceLocation beforeWhich) {
        hide(id);
        int idx = toShow.indexOf(beforeWhich);
        if (idx >= 0) {
            toShow.add(idx, id);
        } else {
            toShow.add(0, id);
        }
    }

    public void showAfter(ResourceLocation id, ResourceLocation afterWhich) {
        hide(id);
        int idx = toShow.indexOf(afterWhich);
        if (idx >= 0) {
            toShow.add(idx + 1, id);
        } else {
            toShow.add(id);
        }
    }

    public boolean hide(ResourceLocation id) {
        return toShow.remove(id);
    }

    public boolean contains(ResourceLocation id) {
        return toShow.contains(id);
    }
}
