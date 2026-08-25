package io.github.createmeow.timex_rebirth.heat;

import com.mojang.serialization.MapCodec;
import dev.anye.mc.basecore.block.entity.basecore.BaseCoreBlockEntity;
import io.github.createmeow.timex_rebirth.heat.station.HeatStationRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
 * 热源接收器：紧邻基地核心放置，接收经管道输送的热流流体并储存。
 * 基地核心上的热源桥接模块从它抽取热流产生基地热场。
 * 属性（大小/硬度/声音）显式注册，参照 basecore 基地核心：铁块强度、
 * 需正确工具、HEAVY_CORE 破坏声音，全方块碰撞箱。
 */
public class HeatReceiverBlock extends BaseEntityBlock {
    private static final MapCodec<HeatReceiverBlock> CODEC = simpleCodec(p -> new HeatReceiverBlock());
    private static final VoxelShape SHAPE = Shapes.block();

    public HeatReceiverBlock() {
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
        return new HeatReceiverBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof HeatReceiverBlockEntity receiver) {
                receiver.tick();
            }
        };
    }

    /**
     * 放置校验：必须紧邻基地核心，且放置者须为核心所有者/成员。
     * 不满足时立即移除方块并提示。
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.isClientSide) return;
        if (!(placer instanceof ServerPlayer sp)) return;

        BaseCoreBlockEntity core = findAdjacentCore(level, pos);
        if (core == null) {
            level.removeBlock(pos, false);
            sp.displayClientMessage(Component.translatable("msg.timex_rebirth.heat_receiver.no_basecore"), true);
            sp.sendSystemMessage(Component.translatable("msg.timex_rebirth.heat_receiver.removed"));
            return;
        }
        if (!core.canUse(sp.getUUID())) {
            level.removeBlock(pos, false);
            sp.displayClientMessage(Component.translatable("msg.timex_rebirth.heat_receiver.no_permission"), true);
            sp.sendSystemMessage(Component.translatable("msg.timex_rebirth.heat_receiver.removed"));
            return;
        }
        if (level.getBlockEntity(pos) instanceof HeatReceiverBlockEntity be) {
            be.setOwner(sp.getUUID());
            be.setCorePos(core.getBlockPos());
        }
    }

    /** 在 pos 的 6 个方向寻找基地核心方块实体。 */
    public static BaseCoreBlockEntity findAdjacentCore(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(pos.relative(dir)) instanceof BaseCoreBlockEntity core) {
                return core;
            }
        }
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (level.getBlockEntity(pos) instanceof HeatReceiverBlockEntity be) {
                int amount = be.getTank().getFluidAmount();
                sp.displayClientMessage(Component.translatable(
                        "msg.timex_rebirth.heat_receiver.info", amount, be.getTank().getCapacity()), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 手持热流桶右键：注入 1000mb 热流并返还空桶（无适配器时的降级输运）。 */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(HeatStationRegistry.HEAT_FLUX_BUCKET.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) return ItemInteractionResult.sidedSuccess(true);
        if (level.getBlockEntity(pos) instanceof HeatReceiverBlockEntity be) {
            int filled = be.getTank().fill(
                    new FluidStack(HeatRegistry.HEAT_FLUX.get(), 1000),
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
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
