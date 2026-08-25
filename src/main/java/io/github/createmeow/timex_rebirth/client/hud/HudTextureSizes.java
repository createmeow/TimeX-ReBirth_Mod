package io.github.createmeow.timex_rebirth.client.hud;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * PNG 纹理尺寸缓存：从资源中读取 PNG 头部的宽高（IHDR chunk），避免把不同尺寸的
 * HUD 图标（15x15、4x12、16x16、68x10 等）硬编码为 16x16 导致 UV 拉伸错误。
 */
public final class HudTextureSizes {

    private static final Map<ResourceLocation, int[]> cache = new HashMap<>();

    private HudTextureSizes() {
    }

    /** 返回 {width, height}；未知纹理返回 {16,16}。 */
    public static int[] size(ResourceLocation rl) {
        int[] cached = cache.get(rl);
        if (cached != null) return cached;
        int[] size = readPngSize(rl);
        if (size == null) size = new int[]{16, 16};
        cache.put(rl, size);
        return size;
    }

    private static int[] readPngSize(ResourceLocation rl) {
        try {
            var res = Minecraft.getInstance().getResourceManager().getResource(rl);
            if (res.isEmpty()) return null;
            try (InputStream is = res.get().open();
                 DataInputStream dis = new DataInputStream(is)) {
                byte[] sig = new byte[8];
                dis.readFully(sig);
                // PNG 签名校验
                if (sig[0] != (byte) 0x89 || sig[1] != 0x50) return null;
                dis.readInt(); // chunk 长度
                byte[] type = new byte[4];
                dis.readFully(type);
                if (type[0] != 'I' || type[1] != 'H') return null;
                int width = dis.readInt();
                int height = dis.readInt();
                if (width > 0 && height > 0) {
                    return new int[]{width, height};
                }
            }
        } catch (IOException | RuntimeException e) {
            TimeX.LOGGER.debug("[Hud] 读取纹理尺寸失败 {}: {}", rl, e.toString());
        }
        return null;
    }

    /** 清空缓存（资源重载时调用）。 */
    public static void invalidate() {
        cache.clear();
    }
}
