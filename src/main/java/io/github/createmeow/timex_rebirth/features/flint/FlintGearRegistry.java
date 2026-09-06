package io.github.createmeow.timex_rebirth.features.flint;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 燧石工具体系：
 * - 草绳（2 干枝条竖排合成）
 * - 燧石镐/斧/锹/锄（燧石 + 绳子类物品标签 + 木棍）
 * - 可疑的积雪（可刷刮的战利品雪层：世界生成特征 + 暴雪沉积转化）
 * - 燧石工具修复配方（自定义 Serializer）
 */
public class FlintGearRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TimeX.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TimeX.MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, TimeX.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, TimeX.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, TimeX.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TimeX.MODID);

    /** 绳子类物品标签（配方 S 位）：本模组草绳 / AdditionalAdditions 绳 / 农夫乐事绳 / 原版线 */
    public static final TagKey<Item> ROPES = TagKey.create(Registries.ITEM, TimeX.rl("ropes"));

    /** 尖锐物品标签（配方 A 位）：燧石 + 尖锐废品（断裂螺丝/破碎显示屏），可替代燧石合成与修复工具 */
    public static final TagKey<Item> SHARP_ITEMS = TagKey.create(Registries.ITEM, TimeX.rl("sharp_items"));

    /** 可疑的积雪刷刮完成时使用的战利品表 */
    public static final ResourceKey<LootTable> SUSPICIOUS_SNOW_LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, TimeX.rl("archaeology/suspicious_snow"));

    /**
     * 燧石等级：石级采集能力（能挖铁矿、红石等），耐久/速度略高于石制，
     * 打击加成 0.5，附魔值低（石器粗糙），用燧石修补。
     */
    public static final Tier FLINT_TIER = new Tier() {
        @Override
        public int getUses() {
            return 105;
        }

        @Override
        public float getSpeed() {
            return 3.0F;
        }

        @Override
        public float getAttackDamageBonus() {
            return 0.5F;
        }

        @Override
        public TagKey<Block> getIncorrectBlocksForDrops() {
            return BlockTags.INCORRECT_FOR_STONE_TOOL;
        }

        @Override
        public int getEnchantmentValue() {
            return 5;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(Items.FLINT);
        }
    };

    // ── 草绳 ──
    public static final DeferredItem<Item> GRASS_ROPE =
            ITEMS.register("grass_rope", () -> new Item(new Item.Properties().stacksTo(16)));

    // ── 铝（废土常见轻金属，铝板材粉碎产物 + 铝矿石/粗铝/铝板）──
    public static final DeferredBlock<Block> ALUMINUM_BLOCK =
            BLOCKS.register("aluminum_block", () -> new Block(
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> ALUMINUM_BLOCK_ITEM =
            ITEMS.register("aluminum_block", () -> new BlockItem(ALUMINUM_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<Item> ALUMINUM_INGOT =
            ITEMS.register("aluminum_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ALUMINUM_NUGGET =
            ITEMS.register("aluminum_nugget", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ALUMINUM_SHEET =
            ITEMS.register("aluminum_sheet", () -> new Item(new Item.Properties()));

    // ── 粗铝 + 粗铝块 ──
    public static final DeferredItem<Item> RAW_ALUMINUM =
            ITEMS.register("raw_aluminum", () -> new Item(new Item.Properties()));
    public static final DeferredBlock<Block> RAW_ALUMINUM_BLOCK =
            BLOCKS.register("raw_aluminum_block", () -> new Block(
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> RAW_ALUMINUM_BLOCK_ITEM =
            ITEMS.register("raw_aluminum_block", () -> new BlockItem(RAW_ALUMINUM_BLOCK.get(), new Item.Properties()));

    // ── 铝矿石 / 深层铝矿石 ──
    public static final DeferredBlock<Block> ALUMINUM_ORE =
            BLOCKS.register("aluminum_ore", () -> new Block(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .requiresCorrectToolForDrops()
                            .strength(3.0F, 3.0F)
                            .sound(SoundType.STONE)));
    public static final DeferredItem<BlockItem> ALUMINUM_ORE_ITEM =
            ITEMS.register("aluminum_ore", () -> new BlockItem(ALUMINUM_ORE.get(), new Item.Properties()));
    public static final DeferredBlock<Block> DEEPSLATE_ALUMINUM_ORE =
            BLOCKS.register("deepslate_aluminum_ore", () -> new Block(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.DEEPSLATE)
                            .requiresCorrectToolForDrops()
                            .strength(4.5F, 3.0F)
                            .sound(SoundType.DEEPSLATE)));
    public static final DeferredItem<BlockItem> DEEPSLATE_ALUMINUM_ORE_ITEM =
            ITEMS.register("deepslate_aluminum_ore", () -> new BlockItem(DEEPSLATE_ALUMINUM_ORE.get(), new Item.Properties()));

    // ── 燧石工具四件套 ──
    public static final DeferredItem<PickaxeItem> FLINT_PICKAXE = ITEMS.register("flint_pickaxe",
            () -> new PickaxeItem(FLINT_TIER, new Item.Properties()
                    .attributes(PickaxeItem.createAttributes(FLINT_TIER, 1.0F, -2.8F))));
    public static final DeferredItem<AxeItem> FLINT_AXE = ITEMS.register("flint_axe",
            () -> new AxeItem(FLINT_TIER, new Item.Properties()
                    .attributes(AxeItem.createAttributes(FLINT_TIER, 6.0F, -3.2F))));
    public static final DeferredItem<ShovelItem> FLINT_SHOVEL = ITEMS.register("flint_shovel",
            () -> new ShovelItem(FLINT_TIER, new Item.Properties()
                    .attributes(ShovelItem.createAttributes(FLINT_TIER, 2.5F, -3.0F))));
    public static final DeferredItem<HoeItem> FLINT_HOE = ITEMS.register("flint_hoe",
            () -> new HoeItem(FLINT_TIER, new Item.Properties()
                    .attributes(HoeItem.createAttributes(FLINT_TIER, 0.0F, -2.0F))));

    // ── 可疑的积雪（方块 + 调试用物品形态）──
    public static final DeferredBlock<SuspiciousSnowBlock> SUSPICIOUS_SNOW =
            BLOCKS.register("suspicious_snow", () -> new SuspiciousSnowBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.SNOW)
                            .noOcclusion()
                            .strength(0.2F)
                            .sound(SoundType.SNOW)
                            .pushReaction(PushReaction.DESTROY),
                    SoundEvents.BRUSH_GENERIC,
                    SoundEvents.BRUSH_SAND_COMPLETED));
    public static final DeferredItem<BlockItem> SUSPICIOUS_SNOW_ITEM =
            ITEMS.register("suspicious_snow", () -> new BlockItem(SUSPICIOUS_SNOW.get(), new Item.Properties()));

    // ── 可疑积雪专用方块实体：指定工厂为自定义实体，保证刷刮逻辑在区块重载后仍为新逻辑 ──
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SuspiciousSnowBlockEntity>> SUSPICIOUS_SNOW_BE =
            BLOCK_ENTITIES.register("suspicious_snow",
                    () -> BlockEntityType.Builder.of(SuspiciousSnowBlockEntity::new, SUSPICIOUS_SNOW.get()).build(null));

    // ── 燧石工具修复配方 ──
    public static final DeferredHolder<RecipeType<?>, RecipeType<FlintToolRepairRecipe>> FLINT_REPAIR_TYPE =
            RECIPE_TYPES.register("flint_tool_repair", () -> RecipeType.simple(TimeX.rl("flint_tool_repair")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FlintToolRepairRecipe>> FLINT_REPAIR_SERIALIZER =
            RECIPE_SERIALIZERS.register("flint_tool_repair", FlintToolRepairRecipe.Serializer::new);

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        // 将可疑积雪注册为原版 brushable_block 方块实体的有效方块：
        // 让旧存档中残留的 minecraft:brushable_block 实体能正常加载（新版使用自定义方块实体）。
        modEventBus.addListener(FlintGearRegistry::onBlockEntityTypeAddBlocks);
    }

    public static void onBlockEntityTypeAddBlocks(BlockEntityTypeAddBlocksEvent event) {
        event.modify(BlockEntityType.BRUSHABLE_BLOCK, SUSPICIOUS_SNOW.get());
    }
}
