package io.github.createmeow.timex_rebirth.mineral;

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
 * 石英矿石：主世界新增矿物，参考 Create 的锌矿生成方式。
 * <ul>
 *   <li>quartz_ore：浅层普通石头中的石英矿石；</li>
 *   <li>deepslate_quartz_ore：深层深板岩中的石英矿石。</li>
 * </ul>
 * 掉落石英（受时运影响，精准采集掉落自身），可经 Create 粉碎/冲洗或熔炉熔炼产出石英。
 */
public class QuartzOreRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TimeX.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TimeX.MODID);

    public static final DeferredBlock<Block> QUARTZ_ORE =
            BLOCKS.register("quartz_ore", () -> new Block(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .requiresCorrectToolForDrops()
                            .strength(3.0F, 3.0F)
                            .sound(SoundType.STONE)));

    public static final DeferredBlock<Block> DEEPSLATE_QUARTZ_ORE =
            BLOCKS.register("deepslate_quartz_ore", () -> new Block(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.DEEPSLATE)
                            .requiresCorrectToolForDrops()
                            .strength(4.5F, 3.0F)
                            .sound(SoundType.DEEPSLATE)));

    public static final DeferredItem<BlockItem> QUARTZ_ORE_ITEM =
            ITEMS.register("quartz_ore", () -> new BlockItem(QUARTZ_ORE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_QUARTZ_ORE_ITEM =
            ITEMS.register("deepslate_quartz_ore", () -> new BlockItem(DEEPSLATE_QUARTZ_ORE.get(), new Item.Properties()));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}