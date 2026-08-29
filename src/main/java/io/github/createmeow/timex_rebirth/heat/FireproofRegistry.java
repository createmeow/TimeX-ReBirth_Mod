package io.github.createmeow.timex_rebirth.heat;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 防火材料注册：耐火砖 + 防火机壳。
 * - 耐火砖（fireproof_brick）：砖 + 烈焰粉烧制（配方 fireproof_brick.json），防火机壳的原料，耐火
 * - 防火机壳（fireproof_casing）：手持耐火砖右键深板岩圆石获得（Create item_application，机械手亦可执行），
 *   模仿机械动力"手持安山岩/铜/黄铜右击去皮木获得对应机壳"的交互；方块与掉落物均防火（火/岩浆无法烧毁，
 *   fireResistant），挖掘使用深板岩的声音（仿照安山机壳使用去皮原木声音的做法）
 */
public class FireproofRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TimeX.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TimeX.MODID);

    public static final DeferredItem<Item> FIREPROOF_BRICK =
            ITEMS.register("fireproof_brick", () -> new Item(new Item.Properties().fireResistant().rarity(Rarity.UNCOMMON)));

    public static final DeferredBlock<Block> FIREPROOF_CASING =
            BLOCKS.register("fireproof_casing", () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.DEEPSLATE)
                    .pushReaction(PushReaction.NORMAL)));
    public static final DeferredItem<BlockItem> FIREPROOF_CASING_ITEM =
            ITEMS.register("fireproof_casing", () -> new BlockItem(FIREPROOF_CASING.get(), new Item.Properties().fireResistant().rarity(Rarity.UNCOMMON)));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}
