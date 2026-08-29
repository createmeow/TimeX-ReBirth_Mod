package io.github.createmeow.timex_rebirth.features;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 未点燃的涂蜡纸板（地上等待引燃的引火棒）：
 * <ul>
 *   <li>放置后为未点燃状态，需<strong>外部热源</strong>（邻居明火 / 引火交互）点燃
 *       → 见 {@link FireInteractHandler#igniteNeighbors} 与 {@link WaxedCardboardItem}；</li>
 *   <li>剩余秒数存于方块实体附件（FUEL_DATA，单位秒），挖掘返还未点燃纸板物品（保留剩余耐久）；</li>
 *   <li>不会变潮湿（不在湿化判定内），仅被明火点燃成 {@link LitWaxedCardboardBlock}。</li>
 * </ul>
 */
public class UnlitWaxedCardboardBlock extends BaseEntityBlock {
    public static final com.mojang.serialization.MapCodec<UnlitWaxedCardboardBlock> CODEC = simpleCodec(UnlitWaxedCardboardBlock::new);
    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 6.0, 13.0);

    public UnlitWaxedCardboardBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UnlitWaxedCardboardBlockEntity(pos, state);
    }

    /** 挖掘返还保留剩余耐久的未点燃纸板物品。 */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof UnlitWaxedCardboardBlockEntity be
                && !player.getAbilities().instabuild) {
            popResource(level, pos, be.toItem());
            level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.7F, 0.9F);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** 邻居（含六向）有原版火焰时立刻被引燃 → 替换为点燃方块。 */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && FireInteractHandler.hasAdjacentFireSource(level, pos)) {
            FireInteractHandler.igniteSelfIfFlammable(level, pos);
        }
    }
}
