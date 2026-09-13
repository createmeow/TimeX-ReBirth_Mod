package com.createmeow.underwaterplugin;

import java.util.EnumMap;
import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 水下探索插件注册表（无第三方依赖部分）。
 * <p>
 * 防寒装备：数值与原版铁质完全一致（防御/耐久/附魔能力），但修复材料为铝锭
 * （铁砧与合成台修复用铝锭），每件 3 点抗寒值由 Cold Sweat 数据包注册表
 * item/insulator 提供。
 * <p>
 * Create 相关条目在 {@link UnderwaterCreateRegisters}（仅 Create 加载时才被触碰）。
 */
public final class UnderwaterRegisters {

    private UnderwaterRegisters() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(UnderwaterPlugin.MODID);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, UnderwaterPlugin.MODID);

    // ------------------------------------------------------------------
    // 防寒装备材质：原版铁质数值 + 铝锭修复
    // ------------------------------------------------------------------
    public static final Holder<ArmorMaterial> FROST_ARMOR = ARMOR_MATERIALS.register("frost", () -> {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.HELMET, 2);
        defense.put(ArmorItem.Type.CHESTPLATE, 6);
        defense.put(ArmorItem.Type.LEGGINGS, 5);
        defense.put(ArmorItem.Type.BOOTS, 2);
        defense.put(ArmorItem.Type.BODY, 5);
        return new ArmorMaterial(
                defense,
                14,
                SoundEvents.ARMOR_EQUIP_IRON,
                () -> net.minecraft.world.item.crafting.Ingredient.of(
                        io.github.createmeow.timex_rebirth.features.flint.FlintGearRegistry.ALUMINUM_INGOT.get()),
                List.of(new ArmorMaterial.Layer(
                        ResourceLocation.fromNamespaceAndPath(UnderwaterPlugin.MODID, "frost"))),
                0.0F,
                0.0F);
    });

    // ------------------------------------------------------------------
    // 防寒装备（无条件注册）
    // ------------------------------------------------------------------
    /** 防寒面罩：铁盔防御(2)与耐久(165)，铝锭修复 */
    public static final DeferredItem<ArmorItem> FROST_MASK = ITEMS.register("frost_mask",
            () -> new ArmorItem(FROST_ARMOR, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(15))));

    /** 防寒护腿：铁腿防御(5)与耐久(225)，铝锭修复 */
    public static final DeferredItem<ArmorItem> FROST_LEGGINGS = ITEMS.register("frost_leggings",
            () -> new ArmorItem(FROST_ARMOR, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(15))));

    /** 防寒靴子：铁靴防御(2)与耐久(195)，铝锭修复 */
    public static final DeferredItem<ArmorItem> FROST_BOOTS = ITEMS.register("frost_boots",
            () -> new ArmorItem(FROST_ARMOR, ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(15))));

    /** 模组构造阶段调用 */
    public static void register(IEventBus modEventBus) {
        ARMOR_MATERIALS.register(modEventBus);
        ITEMS.register(modEventBus);
        if (UnderwaterCreateRegisters.createLoaded()) {
            UnderwaterCreateRegisters.register(modEventBus);
        }
    }

    /** 防寒装备加入创造标签 */
    public static void addToTab(net.minecraft.world.item.CreativeModeTab.Output output) {
        output.accept(FROST_MASK.get());
        output.accept(FROST_LEGGINGS.get());
        output.accept(FROST_BOOTS.get());
    }
}
