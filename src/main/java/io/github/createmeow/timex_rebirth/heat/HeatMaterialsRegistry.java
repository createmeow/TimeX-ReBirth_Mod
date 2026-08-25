package io.github.createmeow.timex_rebirth.heat;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 供热工业基础材料注册：耐热合金锭 / 隔热玻璃 / 半成品中间体。
 * - 耐热合金锭（heat_alloy_ingot）：铁+铜+烈焰粉经机械动力加热搅拌熔合
 *   （配方 heat_alloy_ingot.json，create:mixing heated），热源部件的基础金属，耐火
 * - 隔热玻璃（insulated_glass）：玻璃+雪+羊毛制成（配方 insulated_glass.json），保温材料
 * - incomplete_*：机械手序列组装（sequenced_assembly）的半成品中间体，
 *   序列组装的结果为概率产出（主产物 + 铁锭/铁板/耐火砖等副材料）
 */
public class HeatMaterialsRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TimeX.MODID);

    public static final DeferredItem<Item> HEAT_ALLOY_INGOT =
            ITEMS.register("heat_alloy_ingot", () -> new Item(new Item.Properties().fireResistant()));
    public static final DeferredItem<Item> INSULATED_GLASS =
            ITEMS.register("insulated_glass", () -> new Item(new Item.Properties()));

    // ── 序列组装半成品（不可直接获得，仅作为流水线中间态）──
    public static final DeferredItem<Item> INCOMPLETE_HEAT_FUEL_RECEIVER =
            ITEMS.register("incomplete_heat_fuel_receiver", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> INCOMPLETE_HEAT_MODULE_SLOT =
            ITEMS.register("incomplete_heat_module_slot", () -> new Item(new Item.Properties()));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
