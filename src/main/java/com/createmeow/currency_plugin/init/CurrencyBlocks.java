package com.createmeow.currency_plugin.init;

import com.createmeow.currency_plugin.CurrencyPlugin;
import com.createmeow.currency_plugin.machine.RefinerBlock;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 方块注册：提炼机。
 */
public class CurrencyBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(CurrencyPlugin.MODID);
    public static final DeferredRegister.Items BLOCK_ITEMS =
            DeferredRegister.createItems(CurrencyPlugin.MODID);

    public static final DeferredBlock<RefinerBlock> REFINING_MACHINE =
            BLOCKS.register("refining_machine", RefinerBlock::new);

    public static final DeferredItem<BlockItem> REFINING_MACHINE_ITEM =
            BLOCK_ITEMS.registerSimpleBlockItem("refining_machine", REFINING_MACHINE);
}
