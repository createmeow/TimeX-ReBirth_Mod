package io.github.createmeow.timex_rebirth.workbench;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 工作台组装事件处理（必须按顺序：剪→锤→锯）：
 * 1. 手持工作剪右键木板 → 木板变为工作台(半成品)，step=1
 * 2. 手持工作锤右键工作台(半成品) → step=2
 * 3. 手持工作锯右键工作台(半成品) → step=3 → 变为原版工作台
 * <p>
 * 所有非法操作均给出错误提示（不静默失败）：
 * - 非工作工具/空手右键半成品 → 提示需要工作工具（不取消事件，保留原版交互）
 * - 工作锤/锯右键未组装的木板 → 提示需要先用工作剪
 * - 顺序错误的工具 → 提示当前需要的工具
 */
@EventBusSubscriber(modid = TimeX.MODID)
public class WorkbenchAssemblyHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled()) return;

        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        ItemStack held = event.getItemStack();

        WorkbenchHalfBlockEntity.ToolType toolType = getToolType(held);

        // ── 非工作工具/空手：对半成品给出提示，不静默失败 ──
        if (toolType == null) {
            if (level.isClientSide() && state.getBlock() == WorkbenchRegistry.WORKBENCH_HALF.get()) {
                player.displayClientMessage(Component.translatable(
                        "message.timex_rebirth.workbench_assembly.need_tool"), true);
            }
            return;
        }

        // ── 情况1：右键木板 → 变为工作台(半成品)，安装剪 ──
        if (isPlanks(state)) {
            // 木板只能用剪开始
            if (toolType != WorkbenchHalfBlockEntity.ToolType.SHEARS) {
                if (level.isClientSide()) {
                    player.displayClientMessage(Component.translatable(
                            "message.timex_rebirth.workbench_assembly.need_shears_first"), true);
                }
                return;
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            if (level.isClientSide()) {
                player.swing(InteractionHand.MAIN_HAND);
                return;
            }
            level.setBlockAndUpdate(pos, WorkbenchRegistry.WORKBENCH_HALF.get().defaultBlockState());
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof WorkbenchHalfBlockEntity half) {
                half.installTool(WorkbenchHalfBlockEntity.ToolType.SHEARS);
                WorkbenchHalfBlock.syncStep(level, pos, half);
            }
            if (!player.isCreative()) held.shrink(1);
            level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.8F, 1.0F);
            player.displayClientMessage(Component.translatable("message.timex_rebirth.workbench_assembly.progress", 33), true);
            return;
        }

        // ── 情况2：右键工作台(半成品) → 按顺序安装工具 ──
        if (state.getBlock() == WorkbenchRegistry.WORKBENCH_HALF.get()) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            if (level.isClientSide()) {
                player.swing(InteractionHand.MAIN_HAND);
                return;
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof WorkbenchHalfBlockEntity half)) return;

            // 检查是否是正确的工具顺序
            WorkbenchHalfBlockEntity.ToolType expected = half.expectedTool();
            if (expected == null) {
                // 已经完成（不应该到这里，但防御性检查）
                return;
            }
            if (toolType != expected) {
                // 工具顺序错误
                player.displayClientMessage(Component.translatable(
                        "message.timex_rebirth.workbench_assembly.wrong_tool",
                        Component.translatable(toolNameKey(expected))), true);
                return;
            }

            half.installTool(toolType);
            WorkbenchHalfBlock.syncStep(level, pos, half);
            if (!player.isCreative()) held.shrink(1);
            level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.8F, 1.0F);

            // 检查是否完成
            if (half.isComplete()) {
                level.setBlockAndUpdate(pos, Blocks.CRAFTING_TABLE.defaultBlockState());
                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                player.displayClientMessage(Component.translatable("message.timex_rebirth.workbench_assembly.complete"), true);
            } else {
                player.displayClientMessage(Component.translatable("message.timex_rebirth.workbench_assembly.progress",
                        half.progressPercent()), true);
            }
        }
    }

    private static boolean isPlanks(BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.PLANKS);
    }

    private static WorkbenchHalfBlockEntity.ToolType getToolType(ItemStack stack) {
        if (stack.is(WorkbenchRegistry.WORK_SHEARS.get())) return WorkbenchHalfBlockEntity.ToolType.SHEARS;
        if (stack.is(WorkbenchRegistry.WORK_HAMMER.get())) return WorkbenchHalfBlockEntity.ToolType.HAMMER;
        if (stack.is(WorkbenchRegistry.WORK_SAW.get())) return WorkbenchHalfBlockEntity.ToolType.SAW;
        return null;
    }

    private static String toolNameKey(WorkbenchHalfBlockEntity.ToolType type) {
        return switch (type) {
            case SHEARS -> "item.timex_rebirth.work_shears";
            case HAMMER -> "item.timex_rebirth.work_hammer";
            case SAW -> "item.timex_rebirth.work_saw";
        };
    }
}
