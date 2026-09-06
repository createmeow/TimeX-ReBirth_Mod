package io.github.createmeow.timex_rebirth.compat;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * basecore 基地核心序列组装兼容：注册半成品中间体。
 * 基地核心由机械手序列组装生产（配方 basecore_assembled.json，带 basecore+create 条件）。
 * 防御炮和加密容器同样由序列组装生产，使用耐寒机壳为基础原料。
 */
public class BasecoreCompatRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TimeX.MODID);

    public static final DeferredItem<Item> INCOMPLETE_BASECORE =
            ITEMS.register("incomplete_basecore", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> INCOMPLETE_DEFEND =
            ITEMS.register("incomplete_defend", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> INCOMPLETE_HASH_CHEST =
            ITEMS.register("incomplete_hash_chest", () -> new Item(new Item.Properties()));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
