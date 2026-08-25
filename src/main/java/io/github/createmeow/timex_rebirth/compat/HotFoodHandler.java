package io.github.createmeow.timex_rebirth.compat;

import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.TimeXConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * 热食驱寒：食用/饮用 timex_rebirth:heating_food 标签内的物品时，
 * 提升玩家核心体温（Cold Sweat CORE 温度），短暂驱散寒意。
 * 标签默认包含热汤/热饮（朗姆酒等可在数据包中追加）。
 */
@EventBusSubscriber(modid = TimeX.MODID)
public class HotFoodHandler {
    public static final TagKey<Item> HEATING_FOOD =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TimeX.MODID, "heating_food"));

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        double warmth = TimeXConfig.HOT_FOOD_WARMTH.get();
        if (warmth <= 0) return;
        if (!event.getItem().is(HEATING_FOOD)) return;

        ColdSweatCompat.addCore(player, warmth);
        player.displayClientMessage(Component.translatable("msg.timex_rebirth.food.hot_drink"), true);
    }
}
