package io.github.createmeow.timex_rebirth.features;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 点燃的涂蜡纸板（地上燃烧的引火棒）：
 * <ul>
 *   <li>剩余燃烧秒数存于方块实体附件 FUEL_DATA（<strong>单位=秒</strong>），每秒扣 1；归零烧尽只掉灰烬；</li>
 *   <li>被水/降雪熄灭 → 掉落点燃纸板物品（保留剩余耐久）；</li>
 *   <li>引燃六向邻居；玩家手持点燃纸板且耐久 &lt; 15 时每秒受 1 火焰伤害。</li>
 * </ul>
 */
public class LitWaxedCardboardBlock extends BaseEntityBlock {
    public static final com.mojang.serialization.MapCodec<LitWaxedCardboardBlock> CODEC = simpleCodec(LitWaxedCardboardBlock::new);
    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 6.0, 13.0);

    public LitWaxedCardboardBlock(Properties properties) {
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
        return new LitWaxedCardboardBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, FireToolRegistry.LIT_WAXED_CARDBOARD_BE.get(), LitWaxedCardboardBlockEntity::serverTick);
    }

    /**
     * 被破坏/熄灭：烧尽时只掉灰烬（BE 已处理）；否则返还保留剩余耐久的点燃纸板物品。
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()
                && level.getBlockEntity(pos) instanceof LitWaxedCardboardBlockEntity be && !be.burnedOut) {
            int remaining = Math.max(1, be.remainingSeconds());
            ItemStack drop = new ItemStack(FireToolRegistry.LIT_WAXED_CARDBOARD.get());
            drop.setDamageValue(drop.getMaxDamage() - remaining);
            popResource(level, pos, drop);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
