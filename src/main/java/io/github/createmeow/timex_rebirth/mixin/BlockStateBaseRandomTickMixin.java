package io.github.createmeow.timex_rebirth.mixin;

import io.github.createmeow.timex_rebirth.features.PlantFrostHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 植物随机刻挂钩（1.21.1 随机刻入口为 BlockStateBase.randomTick）：
 * - 植物枯萎（抗冻/抗热）并阻止生长。
 * 仅对植物方块生效，非植物方块快速放行；冻土转换改由
 * ServerLevelMixin_PlaceExtraSnow（tickChunk）统一处理，避免强制
 * 大量泥土参与随机刻造成性能问题。
 */
@Mixin(BlockStateBase.class)
public abstract class BlockStateBaseRandomTickMixin {

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void timex_rebirth$onRandomTick(ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        BlockState state = (BlockState) (Object) this;
        if (PlantFrostHandler.handleRandomTick(state, level, pos, random)) {
            ci.cancel();
        }
    }
}
