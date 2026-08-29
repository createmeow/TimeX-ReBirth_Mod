package io.github.createmeow.timex_rebirth.features;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 干燥的木条（dry_kindling）：
 * 生火用的引火方块，由任意原木合成（1 原木 → 4 木条）。
 * 手持木棍右键钻木取火：每次 20% 成功且必定消耗 1 个木棍；
 * 成功时该木条被点燃（变为原版火焰），可用于点燃篝火/炉灶或作为临时光源。
 * 极易燃：任何火源蔓延到它都会直接烧毁。
 */
public class DryKindlingBlock extends Block {
    public static final MapCodec<DryKindlingBlock> CODEC = simpleCodec(DryKindlingBlock::new);
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 3.0, 14.0);

    public DryKindlingBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** 六向邻居有原版火焰时立刻被引燃（直接烧毁并放火）。 */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (FireInteractHandler.hasAdjacentFireSource(level, pos)) {
            FireInteractHandler.igniteSelfIfFlammable(level, pos);
        }
    }
}
