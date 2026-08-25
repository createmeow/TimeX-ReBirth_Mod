package io.github.createmeow.timex_rebirth.antifreeze;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.CommonHooks;

import javax.annotation.Nullable;

/**
 * 抗冻耕地：受防冻剂处理过的耕地，低温下不会冻结（天然不在 FrozenSoilHandler 转换目标中）；
 * 被踩踏或干涸时退化为抗冻土壤（而非普通泥土）。
 */
public class AntiFreezeFarmlandBlock extends FarmBlock {

    public AntiFreezeFarmlandBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int i = state.getValue(MOISTURE);
        if (!isNearWater(level, pos) && !level.isRainingAt(pos.above())) {
            if (i > 0) {
                level.setBlock(pos, state.setValue(MOISTURE, i - 1), 2);
            } else if (!shouldMaintainFarmland(level, pos)) {
                turnToAntiFreezeDirt(null, state, level, pos);
            }
        } else if (i < 7) {
            level.setBlock(pos, state.setValue(MOISTURE, 7), 2);
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (!level.isClientSide && CommonHooks.onFarmlandTrample(level, pos,
                AntiFreezeRegistry.ANTI_FREEZE_DIRT.get().defaultBlockState(), fallDistance, entity)) {
            turnToAntiFreezeDirt(entity, state, level, pos);
        }
        // 不调用 super.fallOn（FarmBlock 会把耕地踩成普通泥土），只保留摔落伤害
        entity.causeFallDamage(fallDistance, 1.0F, level.damageSources().fall());
    }

    private static void turnToAntiFreezeDirt(@Nullable Entity entity, BlockState state, Level level, BlockPos pos) {
        BlockState target = pushEntitiesUp(state,
                AntiFreezeRegistry.ANTI_FREEZE_DIRT.get().defaultBlockState(), level, pos);
        level.setBlockAndUpdate(pos, target);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(entity, target));
    }

    private static boolean shouldMaintainFarmland(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos.above()).is(BlockTags.MAINTAINS_FARMLAND);
    }

    private static boolean isNearWater(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        for (BlockPos blockpos : BlockPos.betweenClosed(pos.offset(-4, 0, -4), pos.offset(4, 1, 4))) {
            if (state.canBeHydrated(level, pos, level.getFluidState(blockpos), blockpos)) {
                return true;
            }
        }
        return net.neoforged.neoforge.common.FarmlandWaterManager.hasBlockWaterTicket(level, pos);
    }
}
