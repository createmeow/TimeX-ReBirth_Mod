package io.github.createmeow.timex_rebirth.workbench;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 工作台(半成品)物品：
 * - 放置时从 create:sequenced_assembly 组件同步 step 到 BlockEntity
 * - 挖掘后保留 BlockEntity 数据 + create:sequenced_assembly 组件
 * - tooltip 由 Create 的序列组装系统自动显示（不自定义）
 */
public class WorkbenchHalfItem extends BlockItem {

    public WorkbenchHalfItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        InteractionResult result = super.place(context);
        if (result.consumesAction()) {
            Level level = context.getLevel();
            BlockPos pos = context.getClickedPos();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof WorkbenchHalfBlockEntity half) {
                // 如果物品有 create:sequenced_assembly，同步 step 到 BlockEntity
                int createStep = CreateWorkbenchCompat.getCreateStep(context.getItemInHand());
                if (createStep > 0) {
                    half.setStep(createStep);
                }
                // 同步渲染阶段
                WorkbenchHalfBlock.syncStep(level, pos, half);
            }
        }
        return result;
    }
}
