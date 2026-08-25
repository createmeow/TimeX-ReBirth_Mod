package io.github.createmeow.timex_rebirth.heat.station;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * 热源发生器站位方块：3×2×3 发生器结构件，围绕热源发生器自动填充
 * （第一层四角 + 第二层满 3×3，第一层边中点预留缺口安装部件）。
 * 无物品、无掉落，破坏后整座发生器（含缺口部件）随之解体。
 */
public class HeatStationCasingBlock extends HeatCasingBlock {
    private static final MapCodec<HeatStationCasingBlock> CODEC = simpleCodec(p -> new HeatStationCasingBlock());

    public HeatStationCasingBlock() {
        super(Block.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(5.0F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.HEAVY_CORE));
    }

    @Override
    protected boolean isMaster(BlockState state) {
        return state.getBlock() instanceof HeatStationBlock;
    }

    @Override
    protected ItemStack masterStack() {
        return new ItemStack(HeatStationRegistry.HEAT_STATION_ITEM.get());
    }

    @Override
    protected MapCodec<? extends HeatCasingBlock> codec() {
        return CODEC;
    }
}
