package io.github.createmeow.timex_rebirth.compat;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;

/**
 * Immersive Weathering 联动：禁用冻土的"额外掉落"（金粒、铁粒、骨头）。
 * 替换 permafrost / grassy_permafrost 的战利品表为只掉自身。
 */
@EventBusSubscriber(modid = TimeX.MODID)
public class ImmersiveWeatheringLootHandler {

    private static final ResourceLocation PERMAFROST_LOOT =
            ResourceLocation.fromNamespaceAndPath("immersive_weathering", "blocks/permafrost");
    private static final ResourceLocation GRASSY_PERMAFROST_LOOT =
            ResourceLocation.fromNamespaceAndPath("immersive_weathering", "blocks/grassy_permafrost");

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (!ImmersiveWeatheringCompat.isLoaded()) return;

        ResourceLocation name = event.getName();
        if (!PERMAFROST_LOOT.equals(name) && !GRASSY_PERMAFROST_LOOT.equals(name)) {
            return;
        }

        // 只掉 1 个冻土：冻土草块也掉落冻土（grass 层只是表层的草皮）
        Block block = ImmersiveWeatheringCompat.getPermafrost();
        if (block == null || block.asItem() == null) return;

        LootTable.Builder builder = LootTable.lootTable();
        builder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(block.asItem())));
        event.setTable(builder.build());
        TimeX.LOGGER.info("[IW-Loot] 已替换冻土/冻土草块战利品表：掉落冻土");
    }
}
