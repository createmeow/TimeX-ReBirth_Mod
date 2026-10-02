package com.createmeow.cm_plugins.client;

import net.minecraft.Util;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

import com.createmeow.cm_plugins.createmeowsplugins;

/**
 * 爆炸音效节流（仅客户端）：TNT 连锁爆炸时同一 tick 内海量爆炸音效
 * 会灌满客户端音效池（Maximum sound pool size 247 reached + Failed to create
 * new sound handle 刷屏），造成卡顿与日志爆炸。
 * <p>每个游戏刻最多播放 {@link #MAX_PER_TICK} 个爆炸音效，超出直接丢弃。</p>
 */
@EventBusSubscriber(modid = createmeowsplugins.MODID, value = Dist.CLIENT)
public class ExplosionSoundLimiter {

    /** 每个游戏刻允许播放的爆炸音效上限 */
    private static final int MAX_PER_TICK = 6;

    /** 当前 tick 窗口（Util.getMillis() / 50）与已播放计数 */
    private static long window = -1;
    private static int count;

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null) return;
        ResourceLocation location = sound.getLocation();
        if (location == null || !location.getPath().contains("explode")) return;

        long now = Util.getMillis() / 50;
        if (now != window) {
            window = now;
            count = 0;
        }
        if (count >= MAX_PER_TICK) {
            // 丢弃多余爆炸音效（setSound(null) = 不播放）
            event.setSound(null);
        } else {
            count++;
        }
    }
}
