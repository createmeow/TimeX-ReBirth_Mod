package io.github.createmeow.timex_rebirth.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ModKeys {
    public static final KeyMapping KEY_OPEN_WHEEL_MENU = new KeyMapping(
            "key." + TimeX.MODID + ".open_wheel_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            "key.categories." + TimeX.MODID
    );

    /** 悬停植物方块物品时按住查看环境信息（默认 S 键，可在按键设置中修改）。 */
    public static final KeyMapping KEY_CROP_INFO = new KeyMapping(
            "key." + TimeX.MODID + ".crop_info",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_S,
            "key.categories." + TimeX.MODID
    );

    public static void init() {
        // no-op, just ensures static initializer runs
    }
}