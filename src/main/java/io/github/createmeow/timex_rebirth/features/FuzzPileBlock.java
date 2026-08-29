package io.github.createmeow.timex_rebirth.features;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 绒毛堆（海龟蛋式 1~4 叠）：
 * <ul>
 *   <li>放置时若目标位置已有绒毛堆则 +1（最多 4）；</li>
 *   <li>手持燧石右键生火：成功率 = 20% × 绒毛数量；成功后绒毛全消耗、原地变火焰，
 *       并主动引燃六向邻居（篝火/灶台/熔炉/其它木条绒毛）；</li>
 *   <li>极易燃，火焰蔓延直接烧毁。</li>
 * </ul>
 */
public class FuzzPileBlock extends Block {
    public static final IntegerProperty FUZZ = IntegerProperty.create("fuzz", 1, 4);
    public static final com.mojang.serialization.MapCodec<FuzzPileBlock> CODEC = simpleCodec(FuzzPileBlock::new);

    private static final VoxelShape ONE_SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 2.0, 13.0);
    private static final VoxelShape MULTIPLE_SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 4.0, 15.0);

    public FuzzPileBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FUZZ, 1));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FUZZ);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FUZZ) > 1 ? MULTIPLE_SHAPE : ONE_SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState existing = level.getBlockState(pos);
        // 叠加到已有绒毛堆
        if (existing.getBlock() instanceof FuzzPileBlock) {
            int cur = existing.getValue(FUZZ);
            return cur >= 4 ? null : existing.setValue(FUZZ, cur + 1);
        }
        return super.getStateForPlacement(context);
    }

    @Override
    protected boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext context) {
        // 手持绒毛且未满 4 时允许"替换"实现叠加
        return context.getItemInHand().is(FireToolRegistry.FUZZ.get()) && state.getValue(FUZZ) < 4;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!held.is(Items.FLINT)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (level.isClientSide()) {
            level.addParticle(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5,
                    0, 0.03, 0);
            player.swing(hand);
            return ItemInteractionResult.SUCCESS;
        }

        double chance = 0.20 * state.getValue(FUZZ);
        if (level.random.nextDouble() < chance) {
            // 绒毛全消耗：原地变火焰并引燃邻居
            level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
            FireInteractHandler.igniteNeighbors(level, pos);
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.timex_rebirth.fire_success"), true);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
        } else {
            // 失败：消耗 1 个绒毛（打滑损耗）
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.6F, 1.5F);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.timex_rebirth.fire_fail"), true);
        }
        return ItemInteractionResult.SUCCESS;
    }

    /** 掉落对应数量的绒毛。 */
    @Override
    protected void spawnAfterBreak(BlockState state, net.minecraft.server.level.ServerLevel level,
                                   BlockPos pos, ItemStack tool, boolean dropExp) {
        super.spawnAfterBreak(state, level, pos, tool, dropExp);
        popResource(level, pos, new ItemStack(FireToolRegistry.FUZZ.get(), state.getValue(FUZZ)));
    }

    /** 邻居（含六向）有原版火焰时立刻引燃（变火 + 掉落 lit_fuzz）。 */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (FireInteractHandler.hasAdjacentFireSource(level, pos)) {
            FireInteractHandler.igniteSelfIfFlammable(level, pos);
        }
    }
}
