package io.github.createmeow.timex_rebirth.wheelmenu;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;

import java.util.Map;

/**
 * 轮盘选项注册事件（参考 FrostedHeart WheelMenuSelectionRegisterEvent）：
 * 在客户端初始化轮盘选项时于游戏总线触发，各模组通过 register(id, selection) 注册选项。
 */
public class WheelMenuSelectionRegisterEvent extends Event {
    private final Map<ResourceLocation, WheelSelection> toAdd;

    public WheelMenuSelectionRegisterEvent(Map<ResourceLocation, WheelSelection> toAdd) {
        this.toAdd = toAdd;
    }

    public void register(ResourceLocation id, WheelSelection selection) {
        toAdd.put(id, selection);
    }

    public int getRegisteredCount() {
        return toAdd.size();
    }
}
