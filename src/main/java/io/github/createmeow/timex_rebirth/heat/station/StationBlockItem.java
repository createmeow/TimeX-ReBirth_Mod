package io.github.createmeow.timex_rebirth.heat.station;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * 热源供应站方块物品：右键统一触发放置进度（复用基地核心占位放置机制）。
 * 多方块位置检查在 StationPlacement.startPlacement 内进行，不满足直接取消。
 */
public class StationBlockItem extends BlockItem {
    private final String blockId;
    private final String displayKey;
    private final StationPlacement.Rule rule;

    public StationBlockItem(Block block, String blockId, String displayKey, StationPlacement.Rule rule) {
        super(block, new Item.Properties());
        this.blockId = blockId;
        this.displayKey = displayKey;
        this.rule = rule;
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() instanceof ServerPlayer sp) {
            return StationPlacement.startPlacement(sp, context, this.blockId, this.displayKey, this.rule)
                    ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        return InteractionResult.FAIL;
    }
}
