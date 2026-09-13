package com.createmeow.underwaterplugin;

import java.util.EnumMap;
import java.util.List;

import com.simibubi.create.content.equipment.armor.BacktankItem;
import com.simibubi.create.content.equipment.armor.BacktankItem.BacktankBlockItem;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 铝背罐注册表（依赖 Create）。
 * 本类仅在 Create 已加载时被加载/触碰（见 {@link UnderwaterRegisters#register}），
 * 静态字段初始化期间引用 Create 类是安全的。
 */
public final class UnderwaterCreateRegisters {

    private UnderwaterCreateRegisters() {}

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(UnderwaterPlugin.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(UnderwaterPlugin.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, UnderwaterPlugin.MODID);
    private static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, UnderwaterPlugin.MODID);

    public static boolean createLoaded() {
        return ModList.get().isLoaded("create");
    }

    // ------------------------------------------------------------------
    // 铝背罐
    // ------------------------------------------------------------------
    /** 钻石级护甲材质（防御力取钻石胸甲 8），无耐久限制（物品属性不设 durability） */
    public static final Holder<ArmorMaterial> ALUMINUM_BACKTANK_MATERIAL = registerArmorMaterial();

    public static final DeferredBlock<AluminumBacktankBlock> ALUMINUM_BACKTANK_BLOCK =
            BLOCKS.register("aluminum_backtank", () -> new AluminumBacktankBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK)
                            .noOcclusion()));

    /** 穿戴用物品（胸甲槽，含空气条与描述） */
    public static final DeferredItem<AluminumBacktankItem> ALUMINUM_BACKTANK =
            ITEMS.register("aluminum_backtank", () -> new AluminumBacktankItem(
                    ALUMINUM_BACKTANK_MATERIAL,
                    new Item.Properties().rarity(Rarity.UNCOMMON),
                    ResourceLocation.fromNamespaceAndPath(UnderwaterPlugin.MODID, "aluminum_diving"),
                    // 限定名引用避免字段初始化的非法前向引用
                    UnderwaterCreateRegisters.ALUMINUM_BACKTANK_PLACEABLE));

    /** 放置用包装物品（与 Create 的 *_placeable 同机制，正常游玩无法获得） */
    public static final DeferredItem<BacktankBlockItem> ALUMINUM_BACKTANK_PLACEABLE =
            ITEMS.register("aluminum_backtank_placeable", () -> new BacktankBlockItem(
                    ALUMINUM_BACKTANK_BLOCK.get(),
                    ALUMINUM_BACKTANK::get,
                    new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AluminumBacktankBlockEntity>> ALUMINUM_BACKTANK_BE =
            BLOCK_ENTITIES.register("aluminum_backtank",
                    // BlockEntitySupplier 签名为 (BlockPos, BlockState)；类型自身在运行时经 holder 解析
                    // （限定名引用避免初始化器自引用编译错误）
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new AluminumBacktankBlockEntity(
                                    UnderwaterCreateRegisters.ALUMINUM_BACKTANK_BE.get(), pos, state),
                            ALUMINUM_BACKTANK_BLOCK.get())
                            .build(null));

    /**
     * 钻石级护甲材质：防御 {头盔3, 胸甲8, 腿6, 靴3, 动物体3}，附魔能力 10，韧性 2。
     * layers 指向本模组贴图名（实际穿戴贴图由 BaseArmorItem.getArmorTexture 用 textureLoc 覆盖，
     * 此 Layer仅需非空以驱动渲染循环）。
     */
    private static Holder<ArmorMaterial> registerArmorMaterial() {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.HELMET, 3);
        defense.put(ArmorItem.Type.CHESTPLATE, 8);
        defense.put(ArmorItem.Type.LEGGINGS, 6);
        defense.put(ArmorItem.Type.BOOTS, 3);
        defense.put(ArmorItem.Type.BODY, 3);
        return ARMOR_MATERIALS.register("aluminum_backtank", () -> new ArmorMaterial(
                defense, 10, SoundEvents.ARMOR_EQUIP_DIAMOND,
                () -> Ingredient.of(Items.DIAMOND),
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(UnderwaterPlugin.MODID, "aluminum_diving"))),
                2.0F, 0.0F));
    }

    /** 模组构造阶段调用（仅 Create 加载时） */
    public static void register(IEventBus modEventBus) {
        ARMOR_MATERIALS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
    }

    /** 铝背罐加入创造标签（仅 Create 加载时调用） */
    public static void addToTab(net.minecraft.world.item.CreativeModeTab.Output output) {
        output.accept(ALUMINUM_BACKTANK.get());
    }
}
