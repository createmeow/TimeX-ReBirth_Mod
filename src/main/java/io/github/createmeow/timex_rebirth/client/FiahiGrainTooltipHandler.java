package io.github.createmeow.timex_rebirth.client;

import com.hexagram2021.fiahi.common.item.capability.IFrozenRottenFood;
import com.hexagram2021.fiahi.register.FIAHIAttachmentTypes;
import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.compat.FiahiCompatHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * fiahi 联动：为无食物组件的"粮食"（麦类/面粉/面团/种子等）显示温度提示。
 * FIAHI 的温度 tooltip 只对带食物组件且非 leftovers 的物品触发（能腐烂/冻结的食物）；
 * 但本模组通过补注册让这些粮食也参与温度 tick，却不会显示温度值，难以观察。
 * 这里监听 {@link ItemTooltipEvent}，复用 FIAHI 的温度翻译键，为这些粮食显示
 * "正常/冷冻x级/腐烂x级" 状态，并在 F3+H 高级提示下显示具体温度值。
 */
@EventBusSubscriber(modid = TimeX.MODID, value = Dist.CLIENT)
public class FiahiGrainTooltipHandler {

    @SubscribeEvent
    public static void onToolTipShow(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        if (!FiahiCompatHelper.isPerishableGrain(stack) && !FiahiCompatHelper.isSeedLike(stack)) return;
        // 带食物组件的物品已由 FIAHI 显示温度提示，这里只处理无食物组件的粮食，避免重复。
        if (IFrozenRottenFood.canBeFrozenRotten(stack)) return;

        Integer attachment = stack.get(FIAHIAttachmentTypes.FOOD_TEMPERATURE);
        int temp = attachment == null ? 0 : attachment;
        int frozen = IFrozenRottenFood.getFrozenLevel(temp);
        int rotten = IFrozenRottenFood.getRottenLevel(temp);
        // 种子/粮食不是食物：正常温度（未冻结也未腐烂）不显示"新鲜食物"，仅显示冷冻/腐烂级别。
        if (frozen > 0 || rotten > 0) {
            Component status = Component.translatable("item.fiahi.temperature.normal").withStyle(ChatFormatting.GRAY);
            if (frozen > 0) {
                status = Component.translatable("item.fiahi.temperature.frozen.%d".formatted(Mth.clamp(frozen, 0, 3)))
                        .withStyle(ChatFormatting.DARK_AQUA);
            }
            if (rotten > 0) {
                status = Component.translatable("item.fiahi.temperature.rotten.%d".formatted(Mth.clamp(rotten, 0, 3)))
                        .withStyle(ChatFormatting.DARK_RED);
            }
            event.getToolTip().add(status);
        }
        if (Minecraft.getInstance().options.advancedItemTooltips) {
            event.getToolTip().add(Component.translatable("item.fiahi.temperature.description", temp));
        }
    }
}
