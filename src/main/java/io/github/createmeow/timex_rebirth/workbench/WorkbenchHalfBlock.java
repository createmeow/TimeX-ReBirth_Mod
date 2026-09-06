package io.github.createmeow.timex_rebirth.workbench;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作台(半成品)方块：
 * - STEP 方块状态（0~2）对应 3 种组装阶段的渲染纹理
 * - 挖掘时将BlockEntity数据保存到ItemStack（仿basecore模式）
 * - tooltip 由 Create 的序列组装组件自动显示
 */
public class WorkbenchHalfBlock extends BaseEntityBlock {

    /** 渲染阶段：0=无工具，1=已装剪，2=已装剪+锤（step 3 立即变为工作台，无需渲染） */
    public static final IntegerProperty STEP = IntegerProperty.create("step", 0, 2);

    private static final MapCodec<WorkbenchHalfBlock> CODEC = simpleCodec(p -> new WorkbenchHalfBlock(p));

    @Nullable
    private ItemStack dropItem = null;

    protected WorkbenchHalfBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(STEP, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STEP);
    }

    /**
     * 将 BlockEntity 的 step 同步到方块状态的 STEP 属性（用于切换渲染纹理）。
     */
    public static void syncStep(Level level, BlockPos pos, WorkbenchHalfBlockEntity be) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() != WorkbenchRegistry.WORKBENCH_HALF.get()) return;
        int shown = Math.min(Math.max(be.getStep(), 0), 2);
        if (state.getValue(STEP) != shown) {
            level.setBlock(pos, state.setValue(STEP, shown), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WorkbenchHalfBlockEntity(pos, state);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof WorkbenchHalfBlockEntity half) {
                dropItem = new ItemStack(this, 1);
                half.saveToItem(dropItem, level.registryAccess());
                // 同时同步到 create:sequenced_assembly 组件（使两种机制互通）
                CreateWorkbenchCompat.setCreateStep(dropItem, half.getStep());
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        if (dropItem != null) {
            List<ItemStack> drops = new ArrayList<>();
            drops.add(dropItem);
            dropItem = null;
            return drops;
        }
        return super.getDrops(state, builder);
    }
}
