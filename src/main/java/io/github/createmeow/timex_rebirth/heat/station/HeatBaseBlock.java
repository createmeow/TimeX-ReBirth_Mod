package io.github.createmeow.timex_rebirth.heat.station;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/**
 * 加热底座：热源供应站底座主方块（3×1×3 底座结构的中心）。
 * 放置完成时自动在周围填充 8 个底座站位方块（仿机械动力大水车组装）；
 * 底座被破坏时整体倒塌：清除站位方块并解体上方热源发生器。
 */
public class HeatBaseBlock extends Block {
    private static final MapCodec<HeatBaseBlock> CODEC = simpleCodec(p -> new HeatBaseBlock());
    private static final VoxelShape SHAPE = Shapes.block();

    public HeatBaseBlock() {
        super(Block.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(3.5F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.HEAVY_CORE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected @NotNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        HeatStructureHelper.fillBaseCasing(level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            HeatStructureHelper.onBaseRemoved(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
