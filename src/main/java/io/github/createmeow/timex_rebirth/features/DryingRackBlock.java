package io.github.createmeow.timex_rebirth.features;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 晾晒架：仿照原版篝火的<b>多槽位放置</b>结构（最多 4 件）。
 * <ul>
 *   <li><b>右击放入</b>：手持枝条/干枝条 → 放到第一个空槽开始晾晒；</li>
 *   <li><b>右击取出</b>：空手 → 取出最后一个非空槽位的物品；</li>
 *   <li><b>晾晒</b>：枝条晒满 20 秒转成干枝条（见 {@link DryingRackBlockEntity}），以掉落物返还。</li>
 * </ul>
 */
public class DryingRackBlock extends BaseEntityBlock {
    public static final MapCodec<DryingRackBlock> CODEC = simpleCodec(DryingRackBlock::new);
    /** 四根落地腿柱 + 前后两根顶横杆（与 drying_rack.json 模型坐标完全一致）。 */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 3, 12, 3),      // 前左腿
            Block.box(13, 0, 1, 15, 12, 3),    // 前右腿
            Block.box(1, 0, 13, 3, 12, 15),    // 后左腿
            Block.box(13, 0, 13, 15, 12, 15),  // 后右腿
            Block.box(1, 12, 1, 15, 13, 3),    // 前顶横杆
            Block.box(1, 12, 13, 15, 13, 15)); // 后顶横杆

    public DryingRackBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                  BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            player.swing(hand);
            return ItemInteractionResult.SUCCESS;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof DryingRackBlockEntity rack)) {
            return ItemInteractionResult.FAIL;
        }

        if (held.isEmpty()) {
            // 空手取出最后一个非空槽位的物品
            for (int slot = DryingRackBlockEntity.SLOTS - 1; slot >= 0; slot--) {
                if (!rack.isSlotEmpty(slot)) {
                    ItemStack out = rack.takeContent(slot);
                    if (!player.addItem(out)) {
                        net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5,
                                pos.getY() + 0.8, pos.getZ() + 0.5, out);
                    }
                    level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
                    return ItemInteractionResult.SUCCESS;
                }
            }
            return ItemInteractionResult.FAIL;
        }

        // 手持任意"存在干燥配方"的物品 → 放到第一个空槽（setContent 内部校验配方，
        // 无匹配配方返回 -1 表示不可晾晒；有物品但没配方也不消耗原物品）
        int slot = rack.setContent(held);
        if (slot < 0) {
            // 已满 或 无匹配配方
            return ItemInteractionResult.FAIL;
        }
        held.shrink(1);
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DryingRackBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, FireToolRegistry.DRYING_RACK_BE.get(), DryingRackBlockEntity::serverTick);
    }

    /** 挖掘时若架上还有未完成的物品，则一并掉落（依槽位逐个返还）。 */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide() && !state.is(newState.getBlock()) && !movedByPiston
                && level.getBlockEntity(pos) instanceof DryingRackBlockEntity rack) {
            for (int slot = 0; slot < DryingRackBlockEntity.SLOTS; slot++) {
                ItemStack content = rack.getItem(slot);
                if (!content.isEmpty()) {
                    net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5,
                            pos.getY() + 0.5, pos.getZ() + 0.5, content);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
