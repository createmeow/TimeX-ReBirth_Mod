package io.github.createmeow.timex_rebirth.heat.station;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.HitResult;

/**
 * 热源供应站站位方块（结构件）抽象基类：
 * - 由主方块（加热底座 / 热源发生器）在放置时自动填充周围空间（仿机械动力大水车），
 *   无物品注册、无掉落，仅随整体结构存在。
 * - FACING 指向主方块方向：沿 FACING 链可回溯到主方块（getMaster / stillValid）。
 * - 破坏站位方块 → 连带摧毁主方块 → 主方块 onRemove 触发整体解体。
 * - 结构失效（主方块消失）时经 updateShape 调度自检 tick 自行移除，清理孤儿站位。
 */
public abstract class HeatCasingBlock extends DirectionalBlock {

    protected HeatCasingBlock(Properties properties) {
        super(properties);
    }

    /** 该站位方块对应的主方块类型判定。 */
    protected abstract boolean isMaster(BlockState state);

    /** 创造模式中键拾取返回主方块物品。 */
    protected abstract ItemStack masterStack();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** 沿 FACING 链回溯主方块位置（链上仅允许同结构站位方块，深度限制防环）。 */
    public BlockPos getMaster(BlockGetter level, BlockPos pos, BlockState state) {
        BlockPos current = pos;
        Direction facing = state.getValue(FACING);
        for (int i = 0; i < 8; i++) {
            BlockPos next = current.relative(facing);
            BlockState nextState = level.getBlockState(next);
            if (isMaster(nextState)) return next;
            if (!(nextState.getBlock() instanceof HeatCasingBlock casing)) return next;
            current = next;
            facing = nextState.getValue(FACING);
        }
        return current;
    }

    /** 结构仍完整：沿 FACING 链能回到主方块。 */
    public boolean stillValid(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.getBlock() != this) return false;
        return isMaster(level.getBlockState(getMaster(level, pos, state)));
    }

    /**
     * 站位方块不可被任何流体替换（水/岩浆桶、流体管道等）。
     * 实心方块默认已返回 false，此处显式声明防止碰撞箱/形状变动后出现漏洞。
     */
    @Override
    protected boolean canBeReplaced(BlockState state, Fluid fluid) {
        return false;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()
                && !HeatStructureHelper.disassembling && stillValid(level, pos, state)) {
            // 站位方块被破坏：整体解体（主方块掉落，其余站位/部件由其 onRemove 清理）
            level.destroyBlock(getMaster(level, pos, state), true);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!level.isClientSide()) {
            if (stillValid(level, pos, state)) {
                // 结构仍有效：调度主方块重新组装校验（站位被异物占用时整体拆除）
                BlockPos master = getMaster(level, pos, state);
                Block block = level.getBlockState(master).getBlock();
                if (!level.getBlockTicks().hasScheduledTick(master, block)) {
                    level.scheduleTick(master, block, 1);
                }
            } else if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
                level.scheduleTick(pos, this, 1);
            }
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 孤儿站位方块（主方块已消失）：自毁清理
        if (!stillValid(level, pos, state)) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return masterStack();
    }
}
