package io.github.createmeow.timex_rebirth.heat;

import io.github.createmeow.timex_rebirth.heat.station.HeatStationRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * 热流流体（真流体，BaseFlowingFluid 标准双状态实现：源 + 流动）：
 * - 源状态（LEVEL=8）可被空桶回收；流动状态（LEVEL 1~7）不可回收（同原版水）
 * - 与水相似的流动性质（slopeFind=4、dropOff=1、tickRate=5），不自生
 * - 与岩浆反应：接触岩浆源 → 黑曜石；接触流动岩浆 → 圆石（水遇岩浆行为）
 * - 蒸发：放置于世界一段时间后自然消失（模拟热流消耗）
 * - 与普通水接触不发生任何反应
 * 经热源发生器/管道/热流桶输送，接触玩家提供温暖效果。
 */
public abstract class HeatFluxFluid extends BaseFlowingFluid {
    /** 每 tickRate 检查一次蒸发概率，约 10~15 秒内单格热流自然消失。 */
    private static final float EVAPORATE_CHANCE = 0.02F;

    protected HeatFluxFluid(Properties properties) {
        super(properties);
    }

    public static Properties createProperties() {
        return new Properties(
                HeatRegistry.HEAT_FLUX_TYPE,
                HeatRegistry.HEAT_FLUX,
                HeatRegistry.HEAT_FLUX_FLOWING)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .explosionResistance(100.0F)
                .tickRate(5)
                .bucket(HeatStationRegistry.HEAT_FLUX_BUCKET)
                .block(HeatRegistry.HEAT_FLUX_BLOCK);
    }

    @Override
    public Item getBucket() {
        return HeatStationRegistry.HEAT_FLUX_BUCKET.get();
    }

    /**
     * 热流方块 tick：与岩浆反应 → 蒸发 → 标准扩散。
     */
    @Override
    public void tick(Level level, BlockPos pos, FluidState state) {
        if (!level.isClientSide) {
            // 与岩浆反应：接触岩浆源 → 黑曜石，接触流动岩浆 → 圆石（水遇岩浆行为）
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);
                FluidState neighbor = level.getFluidState(neighborPos);
                if (neighbor.is(Fluids.LAVA)) {
                    level.setBlockAndUpdate(neighborPos,
                            neighbor.isSource() ? Blocks.OBSIDIAN.defaultBlockState()
                                    : Blocks.COBBLESTONE.defaultBlockState());
                    level.levelEvent(LevelEvent.LAVA_FIZZ, neighborPos, 0);
                    return; // 本次 tick 消耗在与岩浆的反应上
                }
            }

            // 蒸发：热流在地面一段时间后自然消失（模拟热流消耗）
            if (level.random.nextFloat() < EVAPORATE_CHANCE) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                return;
            }
        }

        super.tick(level, pos, state);
    }

    /** 热流源（可被桶回收、可扩散生成流动态）。 */
    public static class Source extends HeatFluxFluid {
        public Source(Properties properties) {
            super(properties);
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    /** 热流流动态（不可回收，LEVEL 1~7，默认下落）。 */
    public static class Flowing extends HeatFluxFluid {
        public Flowing(Properties properties) {
            super(properties);
            registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7));
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}
