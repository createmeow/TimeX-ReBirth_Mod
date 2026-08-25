package io.github.createmeow.timex_rebirth.wheelmenu;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 按键触发行为（参考 FrostedHeart KeyMappingTriggerAction）：
 * 模拟一次按键按下/释放，从而触发某个已注册按键映射的行为。
 * 可用于轮盘选项联动其他模组的按键（如地图快捷键）。
 */
@OnlyIn(Dist.CLIENT)
public class KeyMappingTriggerAction implements Action {
    private final String name;

    public KeyMappingTriggerAction(String name) {
        this.name = name;
    }

    public String getKey() {
        return name;
    }

    @Override
    @SuppressWarnings("removal")
    public void execute(WheelSelection selection) {
        KeyMapping km = findKeyMapping(name);
        if (km == null) return;
        km.setDown(true);
        // 模拟按下/释放，触发 NeoForge 按键处理
        NeoForge.EVENT_BUS.post(new InputEvent.Key(0, 0, InputConstants.PRESS, 0));
        NeoForge.EVENT_BUS.post(new InputEvent.Key(0, 0, InputConstants.RELEASE, 0));
        km.setDown(false);
    }

    private static KeyMapping findKeyMapping(String name) {
        for (KeyMapping km : Minecraft.getInstance().options.keyMappings) {
            if (km.getName().equals(name)) {
                return km;
            }
        }
        return null;
    }
}
