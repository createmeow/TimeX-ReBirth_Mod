package io.github.createmeow.timex_rebirth.heat.station;

import io.github.createmeow.timex_rebirth.advancement.AdvancementTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * 热源供应站方块物品：右键统一触发放置进度（复用基地核心占位放置机制）。
 * 多方块位置检查在 StationPlacement.startPlacement 内进行，不满足直接取消。
 * 当"热源发生器"（heat_station）成功开始放置时触发"不再寒冷"成就。
 */
public class StationBlockItem extends BlockItem {
    private final String blockId;
    private final String displayKey;
    private final StationPlacement.Rule rule;

    public StationBlockItem(Block block, String blockId, String displayKey, StationPlacement.Rule rule) {
        super(block, new Item.Properties().rarity(Rarity.RARE));
        this.blockId = blockId;
        this.displayKey = displayKey;
        this.rule = rule;
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() instanceof ServerPlayer sp) {
            boolean started = StationPlacement.startPlacement(sp, context, this.blockId, this.displayKey, this.rule);
            if (started && "timex_rebirth:heat_station".equals(this.blockId)) {
                // 组装热源发生器 → 触发"不再寒冷"成就
                AdvancementTriggers.triggerHeatStationBuilt(sp);
            }
            return started ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        return InteractionResult.FAIL;
    }
}
