package io.github.createmeow.timex_rebirth.features;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 冻结的沃土（frozen_rich_soil）：
 * 农夫乐事沃土（rich_soil / rich_soil_farmland）在低温下以普通土一半的速度冻结而成的方块。
 * - 外观：IW 永久冻土纹理以 25% 透明度叠加在农夫乐事沃土上（见 textures/block/frozen_rich_soil.png）。
 * - 解冻：与冻土一致，熔炉/篝火可将其解冻回沃土（配方 frozen_rich_soil_smelting / campfire）。
 * - 由 FrozenSoilHandler 触发：沃土 → 冻结的沃土（半速）。
 * 该方块属于本模组自身（timex_rebirth），不依赖农夫乐事是否安装即可存在；
 * 仅在农夫乐事安装时才会自然生成对应的冻结转换。
 */
public class FrozenRichSoilRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TimeX.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TimeX.MODID);

    public static final DeferredBlock<Block> FROZEN_RICH_SOIL =
            BLOCKS.register("frozen_rich_soil", () -> new Block(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.ICE)
                            .strength(0.5F)
                            .sound(SoundType.CALCITE)
                            .requiresCorrectToolForDrops()));

    public static final DeferredItem<BlockItem> FROZEN_RICH_SOIL_ITEM =
            ITEMS.register("frozen_rich_soil", () -> new BlockItem(FROZEN_RICH_SOIL.get(), new Item.Properties()));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}
