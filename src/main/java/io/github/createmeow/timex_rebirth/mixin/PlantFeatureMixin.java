package io.github.createmeow.timex_rebirth.mixin;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.SimpleBlockFeature;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 禁止草、蕨及其大型变种在世界中生成。
 * 草（short_grass）、蕨（fern）、大型草（tall_grass）、大型蕨（large_fern）
 * 都由 SimpleBlockFeature 放置（patch_grass / single_piece_of_grass 等），
 * 在放置前拦截，阻止这些方块出现在世界中。
 */
@Mixin(SimpleBlockFeature.class)
public abstract class PlantFeatureMixin {

    private static boolean timex_rebirth$isBannedPlant(BlockState state) {
        return state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.LARGE_FERN);
    }

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void timex_rebirth$blockGrass(FeaturePlaceContext<SimpleBlockConfiguration> context,
                                          CallbackInfoReturnable<Boolean> cir) {
        BlockStateProvider provider = context.config().toPlace();
        BlockState candidate = provider.getState(context.random(), context.origin());
        if (timex_rebirth$isBannedPlant(candidate)) {
            cir.setReturnValue(false);
        }
    }
}
