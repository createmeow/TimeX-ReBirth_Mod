package io.github.createmeow.timex_rebirth.mixin;

import com.ordana.immersive_weathering.data.block_growths.TickSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Immersive Weathering 联动：禁用薄冰生成/蔓延。
 * 薄冰的两条生成路径：
 * 1. thin_ice_from_ice.json：owner = minecraft:ice，天光/降雪随机刻中把水替换为薄冰
 * 2. thin_ice_from_thin_ice.json：owner = immersive_weathering:thin_ice，薄冰蔓延
 * 拦截 BlockGrowthHandler.tickBlock，当 owner 为冰或薄冰时跳过，阻止薄冰出现。
 * @Pseudo + optional：该模组未安装时此 mixin 不生效。
 */
@Pseudo
@Mixin(targets = "com.ordana.immersive_weathering.data.block_growths.BlockGrowthHandler", remap = false)
public abstract class BlockGrowthHandlerMixin {

    private static Block thinIce;

    @Inject(method = "tickBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private static void timex_rebirth$disableThinIce(TickSource source, BlockState state, ServerLevel level,
                                                     BlockPos pos, CallbackInfo ci) {
        // 拦截所有冰/薄冰相关的生长，防止薄冰生成
        if (state.is(Blocks.ICE) || (getThinIce() != null && state.is(getThinIce()))) {
            ci.cancel();
        }
    }

    private static Block getThinIce() {
        if (thinIce == null) {
            thinIce = BuiltInRegistries.BLOCK.get(
                    ResourceLocation.fromNamespaceAndPath("immersive_weathering", "thin_ice"));
        }
        return thinIce;
    }
}
