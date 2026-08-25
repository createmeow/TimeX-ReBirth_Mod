package io.github.createmeow.timex_rebirth.crops;

import io.github.createmeow.timex_rebirth.heat.HeatFieldState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.neoforged.neoforge.common.CommonHooks;

/**
 * 冬季作物基类（黑麦 / 芜菁）：
 * - 耐寒生长：PlantTempData 配置 frost=high，低温/积雪/暴风雪中不枯萎；
 * - 热场加速：置于基地热场（HeatFieldState）内生长速度显著提升。
 * MAX_AGE=7，与普通作物一致的成长阶段。
 */
public abstract class WinterCropBlock extends CropBlock {
    public static final int MAX_AGE = 7;
    /** 基地热场中的生长速度倍数（显著加速）。 */
    public static final float HEAT_FIELD_BOOST = 4.0F;

    public WinterCropBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxAge() {
        return MAX_AGE;
    }

    @Override
    public BlockState getStateForAge(int age) {
        return this.defaultBlockState().setValue(this.getAgeProperty(), age);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) return;
        if (level.getRawBrightness(pos, 0) >= 9) {
            int age = this.getAge(state);
            if (age < this.getMaxAge()) {
                float f = getGrowthSpeed(state, level, pos);
                if (HeatFieldState.isHeated(level, pos)) {
                    f *= HEAT_FIELD_BOOST;
                }
                boolean canGrow = random.nextInt((int) (25.0F / f) + 1) == 0;
                if (CommonHooks.canCropGrow(level, pos, state, canGrow)) {
                    level.setBlock(pos, this.getStateForAge(age + 1), 2);
                    CommonHooks.fireCropGrowPost(level, pos, state);
                }
            }
        }
    }
}
