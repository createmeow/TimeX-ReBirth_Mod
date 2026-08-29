package io.github.createmeow.timex_rebirth.features;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 地上的破袜子：手持剑/FD 刀右键刮取绒毛。
 * <ul>
 *   <li>每次刮取必得 1 个绒毛，扣 1 点工具耐久；</li>
 *   <li>耐久存储在方块实体，挖掉返还保留耐久的物品（防刷）；</li>
 *   <li>耐久归零时（刮完或挖放刷）自动移除方块。</li>
 * </ul>
 */
public class TornSocksBlock extends BaseEntityBlock {
    public static final com.mojang.serialization.MapCodec<TornSocksBlock> CODEC = simpleCodec(TornSocksBlock::new);
    /** 选区（视觉模型高度，匹配 wool 贴图覆盖的 1~4 行）。 */
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0);
    /** 碰撞箱（极薄，避免绊脚）。 */
    private static final VoxelShape COLLISION_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 0.5, 16.0);

    public TornSocksBlock(Properties properties) {
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
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION_SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        // 剑（SwordItem）或农夫乐事刀（KnifeItem）→ 刮绒毛
        boolean isBlade = held.getItem() instanceof net.minecraft.world.item.SwordItem
                || (net.neoforged.fml.ModList.get().isLoaded("farmersdelight")
                    && held.getItem() instanceof vectorwing.farmersdelight.common.item.KnifeItem);
        if (!isBlade) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (level.isClientSide()) {
            level.addParticle(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                    0, 0.05, 0);
            player.swing(hand);
            return ItemInteractionResult.SUCCESS;
        }

        // 方块实体存储耐久：刮取扣减
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof TornSocksBlockEntity socksBe)) {
            return ItemInteractionResult.FAIL;
        }
        int remaining = socksBe.remainingDurability();
        if (remaining <= 0) {
            // 防御性：耐久<=0 时直接清理
            level.removeBlock(pos, false);
            return ItemInteractionResult.FAIL;
        }
        // 每次刮取扣 5~10 点耐久（破袜子总耐久 60，约可刮 6~12 次）
        int damage = 5 + level.random.nextInt(6); // [5, 10]
        int newRemaining = Math.max(0, remaining - damage);
        socksBe.setDurability(newRemaining);

        // 必定损失耐久（工具磨损）
        held.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND
                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);

        // 必定获得 1 个绒毛
        ItemStack fuzz = new ItemStack(FireToolRegistry.FUZZ.get());
        ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, fuzz);
        level.addFreshEntity(drop);
        level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.8F, 1.2F);

        // 刮取后若耐久归零 → 散架移除
        if (socksBe.remainingDurability() <= 0) {
            level.removeBlock(pos, false);
            level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.8F, 0.8F);
        }
        return ItemInteractionResult.SUCCESS;
    }

    /** 挖掉时返还保留耐久的破袜子物品；若方块处于水中则掉落湿水版本。 */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.getAbilities().instabuild) {
            if (level.getBlockEntity(pos) instanceof TornSocksBlockEntity be) {
                int remaining = be.remainingDurability();
                if (remaining > 0) {
                    // 处于水中/雨雪露天 → 掉落湿袜子；否则掉落普通破袜子
                    boolean wet = isSockInWater(level, pos);
                    ItemStack drop = new ItemStack(wet
                            ? FireToolRegistry.WET_TORN_SOCKS.get()
                            : FireToolRegistry.TORN_SOCKS.get());
                    drop.setDamageValue(drop.getMaxDamage() - remaining);
                    popResource(level, pos, drop);
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** 流体冲毁时（破袜子被水流冲走）→ 必定掉落湿水的破袜子，保留耐久。
     * 1.21.1 没有 onDestroyedByWater 钩子；改在 {@link TornSocksBlock#onRemove} 中检测：
     * 新状态是水/液体时 → 主动掉落湿袜子（在 super.onRemove 之前，因为 super 会清掉 BE）。 */
    public static void dropWetSocks(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        if (!(level.getBlockEntity(pos) instanceof TornSocksBlockEntity be)) return;
        int remaining = be.remainingDurability();
        if (remaining <= 0) return;
        ItemStack drop = new ItemStack(FireToolRegistry.WET_TORN_SOCKS.get());
        drop.setDamageValue(drop.getMaxDamage() - remaining);
        popResource(level, pos, drop);
        level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.6F, 0.9F);
    }

    /** 被替换时：若新方块是水/液体 → 在 BE 还存在时立即掉落湿袜子。
     * 玩家正常挖掘走 playerWillDestroy 路径（已扣物品），不会触发这里。 */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide()
                && !state.is(newState.getBlock())
                && (newState.getFluidState().is(net.minecraft.world.level.material.Fluids.WATER)
                    || newState.liquid())) {
            // 水/液体冲毁：BE 还在，立即掉落湿袜子
            dropWetSocks(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** 判定方块位置是否处于液态水中或被水冲刷。 */
    private static boolean isSockInWater(Level level, BlockPos pos) {
        if (level.getBlockState(pos).liquid()) return true;
        if (level.getBlockState(pos.above()).liquid()) return true;
        // 简单水流检测：相邻方块有 water 而自身是空气已被替换时
        for (Direction d : Direction.values()) {
            if (level.getBlockState(pos.relative(d)).liquid()) return true;
        }
        return false;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TornSocksBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, FireToolRegistry.TORN_SOCKS_BE.get(), TornSocksBlockEntity::serverTick);
    }
}
