package io.github.createmeow.timex_rebirth.heat.station;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * 加热底座站位方块：3×1×3 底座结构件，围绕加热底座方块自动填充。
 * 无物品、无掉落，破坏后整座底座（含上方发生器）随之解体。
 */
public class HeatBaseCasingBlock extends HeatCasingBlock {
    private static final MapCodec<HeatBaseCasingBlock> CODEC = simpleCodec(p -> new HeatBaseCasingBlock());

    public HeatBaseCasingBlock() {
        super(Block.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(3.5F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.HEAVY_CORE));
    }

    @Override
    protected boolean isMaster(BlockState state) {
        return state.getBlock() instanceof HeatBaseBlock;
    }

    @Override
    protected ItemStack masterStack() {
        return new ItemStack(HeatStationRegistry.HEAT_BASE_ITEM.get());
    }

    @Override
    protected MapCodec<? extends HeatCasingBlock> codec() {
        return CODEC;
    }
}
