package io.github.createmeow.timex_rebirth.research;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 团队科技研究系统注册：研究站方块 / 物品 / 方块实体。
 */
public class ResearchRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TimeX.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TimeX.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TimeX.MODID);

    public static final DeferredBlock<ResearchStationBlock> RESEARCH_STATION =
            BLOCKS.register("research_station", ResearchStationBlock::new);
    public static final DeferredItem<ResearchStationItem> RESEARCH_STATION_ITEM =
            ITEMS.register("research_station", () -> new ResearchStationItem(RESEARCH_STATION.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResearchStationBlockEntity>> RESEARCH_STATION_BE =
            BLOCK_ENTITIES.register("research_station",
                    () -> BlockEntityType.Builder.of(ResearchStationBlockEntity::new, RESEARCH_STATION.get()).build(null));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
    }
}
