package io.github.createmeow.timex_rebirth.heat;

import com.momosoftworks.coldsweat.core.init.ModEffects;
import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.advancement.AdvancementTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.UUID;

/**
 * 热流接触效果：玩家脚部处于热流流体中时获得温暖 II 效果（持续供热）。
 * 热流为真流体，放置于地面会扩散/蒸发，站上去即可取暖。
 */
@EventBusSubscriber(modid = TimeX.MODID)
public class HeatFluxEvents {

    /** 记录已触发过"真正的温泉"成就的玩家，避免重复触发。 */
    private static final java.util.Set<UUID> HOT_SPRING_AWARDED = new java.util.HashSet<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide) return;

        // 实体包围盒级流体检测：覆盖源与流动热流（流动层很薄，单格判定易漏）
        boolean inFlux = player.isInFluidType(HeatRegistry.HEAT_FLUX_TYPE.get());
        if (!inFlux) {
            BlockPos pos = player.blockPosition();
            inFlux = level.getFluidState(pos).is(HeatRegistry.HEAT_FLUX.get())
                    || level.getFluidState(pos.below()).is(HeatRegistry.HEAT_FLUX.get())
                    || level.getFluidState(pos).is(HeatRegistry.HEAT_FLUX_FLOWING.get())
                    || level.getFluidState(pos.below()).is(HeatRegistry.HEAT_FLUX_FLOWING.get());
        }
        if (!inFlux) return;

        // 触发"真正的温泉"成就（服务端、首次泡入热流）
        if (player instanceof ServerPlayer serverPlayer) {
            UUID id = serverPlayer.getUUID();
            if (HOT_SPRING_AWARDED.add(id)) {
                AdvancementTriggers.triggerHotSpring(serverPlayer);
            }
        }

        // 每 20 tick 刷新一次温暖 II 效果（level 1 = 2 级）
        if (player.tickCount % 20 == 0) {
            player.addEffect(new MobEffectInstance(ModEffects.WARMTH, 40, 1, false, false, true));
        }
    }
}
