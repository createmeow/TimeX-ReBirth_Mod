package io.github.createmeow.timex_rebirth.features;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 生火系统注册：干燥的木条方块/物品。
 * 合成：任意原木（#minecraft:logs）→ 4 个干燥的木条（data/recipe/dry_kindling.json）。
 */
public class KindlingRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TimeX.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TimeX.MODID);

    public static final DeferredBlock<DryKindlingBlock> DRY_KINDLING =
            BLOCKS.register("dry_kindling", () -> new DryKindlingBlock(
                    BlockBehaviour.Properties.of()
                            .strength(0.1F)
                            .sound(SoundType.GRASS)
                            .ignitedByLava()
                            .noOcclusion()));

    public static final DeferredItem<BlockItem> DRY_KINDLING_ITEM =
            ITEMS.register("dry_kindling", () -> new BlockItem(DRY_KINDLING.get(), new Item.Properties()));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}
