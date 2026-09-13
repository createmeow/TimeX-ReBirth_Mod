package io.github.createmeow.timex_rebirth.client;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * 统一渲染物品描述：若存在 "item.&lt;命名空间&gt;.&lt;id&gt;.desc" 语言键则追加到 Tooltip 底部（灰色），
 * 使 anti_freeze.desc / fireproof_brick.desc / heat_bridge_module.desc 等键生效。
 * <p>
 * 注意：basecore 模块物品（{@link BasecoreModuleItem} 子类）由基类 appendHoverText 自行添加描述，
 * 此处必须排除，否则描述会显示两次。
 */
@EventBusSubscriber(modid = TimeX.MODID, value = Dist.CLIENT)
public class ItemDescTooltip {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        Item item = stack.getItem();
        if (item instanceof dev.anye.mc.basecore.item.module.BasecoreModuleItem) return;
        String key = item.getDescriptionId() + ".desc";
        if (I18n.exists(key)) {
            event.getToolTip().add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        }
    }
}
