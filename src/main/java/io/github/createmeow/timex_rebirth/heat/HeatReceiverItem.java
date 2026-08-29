package io.github.createmeow.timex_rebirth.heat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.NotNull;

/**
 * 热源接收器物品：右键触发放置进度（复用基地核心模组的占位放置机制），
 * 目标位置必须紧贴基地核心且拥有权限，否则取消放置。
 */
public class HeatReceiverItem extends BlockItem {

    public HeatReceiverItem() {
        super(HeatRegistry.HEAT_RECEIVER.get(), new Item.Properties().rarity(Rarity.RARE));
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() instanceof ServerPlayer sp) {
            return HeatPlacement.startPlacement(sp, context) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        return InteractionResult.FAIL;
    }
}
