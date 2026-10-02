package com.createmeow.cm_plugins;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

import java.util.Collection;
import java.util.List;

/**
 * 物品收集过滤：玩家可以选择一组物品，开启后这些物品将无法被该玩家捡起（其他玩家不受影响）。
 * <ul>
 *   <li>数据以 NeoForge AttachmentType 持久化在玩家身上（跨维度/死亡保留）；</li>
 *   <li>开关与物品列表由「实用功能 → 物品过滤」界面管理；</li>
 *   <li>仅影响开启者本人的拾取行为。</li>
 * </ul>
 */
public class ItemFilterHandler {

    /** 单个玩家的过滤数据：开关 + 被过滤物品的注册名列表。 */
    public record FilterData(boolean enabled, List<String> items) {
        public FilterData {
            items = List.copyOf(items);
        }

        public static final Codec<FilterData> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.BOOL.fieldOf("enabled").forGetter(FilterData::enabled),
                Codec.STRING.listOf().fieldOf("items").forGetter(FilterData::items)
        ).apply(i, FilterData::new));
    }

    /** 读取玩家过滤数据。 */
    public static FilterData get(ServerPlayer player) {
        return player.getData(createmeowsplugins.ITEM_FILTER.get());
    }

    /** 切换玩家过滤开关，返回切换后的状态。 */
    public static boolean toggle(ServerPlayer player) {
        FilterData data = get(player);
        FilterData updated = new FilterData(!data.enabled(), data.items());
        player.setData(createmeowsplugins.ITEM_FILTER.get(), updated);
        return updated.enabled();
    }

    /** 保存玩家的过滤物品列表（保持开关状态不变）。 */
    public static void save(ServerPlayer player, Collection<String> itemIds) {
        FilterData data = get(player);
        player.setData(createmeowsplugins.ITEM_FILTER.get(), new FilterData(data.enabled(), List.copyOf(itemIds)));
    }

    /** 服务端拦截拾取：开启过滤且物品在列表中时阻止拾取（仅影响本人）。 */
    @SubscribeEvent
    public void onItemPickup(ItemEntityPickupEvent.Pre event) {
        net.minecraft.world.entity.player.Player entity = event.getPlayer();
        if (entity.level().isClientSide()) return;
        if (!(entity instanceof ServerPlayer player)) return;

        FilterData data = get(player);
        if (!data.enabled() || data.items().isEmpty()) return;

        ItemEntity itemEntity = event.getItemEntity();
        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty()) return;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (data.items().contains(id)) {
            event.setCanPickup(net.neoforged.neoforge.common.util.TriState.FALSE);
        }
    }
}
