package io.github.createmeow.timex_rebirth.mixin;

import io.github.createmeow.timex_rebirth.compat.ImmersiveWeatheringCompat;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 世界生成修改：地形生成的泥土（dirt）替换为冻土（immersive_weathering:permafrost），
 * 草方块（grass_block）替换为冻草块（immersive_weathering:grassy_permafrost）。
 * 拦截 SurfaceRules$StateRule.tryApply 的返回值。
 */
@Mixin(targets = "net.minecraft.world.level.levelgen.SurfaceRules$StateRule")
public abstract class SurfaceRulesStateRuleMixin {

    private static BlockState permafrostState;
    private static BlockState grassyPermafrostState;

    @Inject(method = "tryApply", at = @At("RETURN"), cancellable = true)
    private void timex_rebirth$replaceSurface(int x, int y, int z, CallbackInfoReturnable<BlockState> cir) {
        BlockState result = cir.getReturnValue();
        if (result == null) return;
        if (result.is(Blocks.DIRT)) {
            cir.setReturnValue(getPermafrostState());
        } else if (result.is(Blocks.GRASS_BLOCK)) {
            cir.setReturnValue(getGrassyPermafrostState());
        }
    }

    private static BlockState getPermafrostState() {
        if (permafrostState == null) {
            Block block = ImmersiveWeatheringCompat.getPermafrost();
            // 未安装 Immersive Weathering 时保持泥土原样
            permafrostState = block != null ? block.defaultBlockState() : Blocks.DIRT.defaultBlockState();
        }
        return permafrostState;
    }

    private static BlockState getGrassyPermafrostState() {
        if (grassyPermafrostState == null) {
            Block block = ImmersiveWeatheringCompat.getGrassyPermafrost();
            // 未安装 Immersive Weathering 时保持草方块原样
            grassyPermafrostState = block != null ? block.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
        }
        return grassyPermafrostState;
    }
}
