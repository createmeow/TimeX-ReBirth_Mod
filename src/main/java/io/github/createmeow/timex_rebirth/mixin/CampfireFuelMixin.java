package io.github.createmeow.timex_rebirth.mixin;

import io.github.createmeow.timex_rebirth.features.FireManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 篝火燃料化：
 * <ul>
 *   <li>cookTick（燃烧时每 tick）：扣减燃料，归零自动熄灭。</li>
 * </ul>
 * 点燃限制与右键填充燃料在 {@link FireInteractHandler} 事件层处理。
 */
@Mixin(CampfireBlockEntity.class)
public abstract class CampfireFuelMixin {

    /** 每 tick 扣减燃料，归零熄灭。注入 cookTick 头部（仅燃烧状态执行，与原版 ticker 一致）。 */
    @Inject(method = "cookTick", at = @At("HEAD"))
    private static void timex_rebirth$consumeFuel(Level level, BlockPos pos,
                                                  BlockState state, CampfireBlockEntity campfire,
                                                  CallbackInfo ci) {
        FireManager.consumeTick(level, pos, state, campfire);
    }
}
