package io.github.createmeow.timex_rebirth.heat.station;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 热源发生器：热源供应站主方块（3×2×3 发生器结构：第一层四角 + 第二层满 3×3 为站位方块，
 * 第一层边中点预留缺口安装热源适配器/模块插槽/燃料接收器）。
 * - 必须放置在加热底座上方才能工作；放置时校验底座与归属（参照基地核心）。
 * - 内部燃烧双燃料（原版熔炉燃料 + 冷血锅炉燃料/熔岩）产出热流流体。
 * - 右键打开 GUI；手持空桶右键可获取热流桶（无适配器时的降级输运手段）。
 * - 结构血量保存在其方块实体上，破坏任意部件都会折算到该结构血量。
 * - 放置完成时经 tick 自动填充站位方块；被破坏时整体解体（站位方块清除、缺口部件掉落）。
 */
public class HeatStationBlock extends BaseEntityBlock {
    private static final MapCodec<HeatStationBlock> CODEC = simpleCodec(p -> new HeatStationBlock());
    private static final VoxelShape SHAPE = Shapes.block();

    public HeatStationBlock() {
        super(Block.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(5.0F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.HEAVY_CORE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeatStationBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof HeatStationBlockEntity station) {
                station.tick();
            }
        };
    }

    /**
     * 放置完成（占位进度结束 / 创造模式直接放置）→ 调度 1 tick 后自动填充站位方块。
     */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    /**
     * 组装 tick：校验底座仍在后填充发生器站位方块（第一层四角 + 第二层满 3×3，缺口留空）。
     * 若底座已消失则整体拆除（主方块掉落，其余由 onRemove 清理）。
     */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockState(pos.below()).getBlock() instanceof HeatBaseBlock)) {
            level.destroyBlock(pos, true);
            return;
        }
        HeatStructureHelper.fillStationCasing(level, pos);
    }

    /** 主方块被移除（玩家破坏 / 解体 / 爆炸等）：整体解体，站位方块清除、缺口部件掉落。 */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            HeatStructureHelper.onStationRemoved(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /**
     * 归属记录：正常路径经放置进度完成（占位 NBT 携带 owner 加载到方块实体），
     * 此处作为创造模式/命令等直接放置路径的兜底。
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.isClientSide || !(placer instanceof ServerPlayer sp)) return;
        if (level.getBlockEntity(pos) instanceof HeatStationBlockEntity station) {
            station.setOwner(sp.getUUID());
        }
    }

    /**
     * 右键交互（手动投燃料；自动化输入只能通过燃料接收器）：
     * - 空桶 → 装取热流桶
     * - 岩浆桶 → 注入熔岩罐并返还空桶
     * - 其他燃料物品 → 投入固体燃料槽
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof HeatStationBlockEntity station)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // 空桶：获取热流桶（无热源适配器时的降级输运）
        if (stack.is(Items.BUCKET)) {
            if (level.isClientSide) return ItemInteractionResult.sidedSuccess(true);
            if (station.tryExtractBucket(player, hand)) {
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // 岩浆桶：注入熔岩储罐并返还空桶
        if (stack.is(Items.LAVA_BUCKET)) {
            if (level.isClientSide) return ItemInteractionResult.sidedSuccess(true);
            int filled = station.getLavaTank().fill(new FluidStack(Fluids.LAVA, 1000),
                    IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) {
                stack.shrink(1);
                ItemStack empty = new ItemStack(Items.BUCKET);
                if (!player.getInventory().add(empty)) {
                    player.drop(empty, false);
                }
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.FAIL; // 储罐已满，阻止默认倒出
        }
        // 其他燃料物品：手动投入固体燃料槽
        if (StationFuelUtil.isSolidFuel(stack)) {
            if (level.isClientSide) return ItemInteractionResult.sidedSuccess(true);
            ItemStack inserted = station.getFuelSlot().insertItem(0, stack.copy(), false);
            if (inserted.getCount() < stack.getCount()) {
                stack.setCount(inserted.getCount());
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.FAIL;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** 右键打开发生器 GUI。 */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (level.getBlockEntity(pos) instanceof HeatStationBlockEntity station) {
                sp.openMenu(station);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
