package io.github.createmeow.timex_rebirth.wasteland;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

/**
 * 废土物资注册：
 * - 绘制台（方块：消耗废旧物品兑换阅历）
 * - 爆炸箭（物品 + 实体：命中爆炸）
 * - 西瓜皮（吃完西瓜片返还，可堆肥、可换西瓜种子）
 * - 废旧物品（提升废土感的可收集物资，绘制台原料，僵尸概率掉落）
 * - 绘制配方（自定义 RecipeType + Serializer，JEI/EMI 展示）
 */
public class WastelandRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TimeX.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TimeX.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, TimeX.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, TimeX.MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, TimeX.MODID);

    // ── 绘制台（方块 + 方块物品）──
    public static final DeferredBlock<DrawingTableBlock> DRAWING_TABLE =
            BLOCKS.register("drawing_table", DrawingTableBlock::new);
    public static final DeferredItem<BlockItem> DRAWING_TABLE_ITEM =
            ITEMS.register("drawing_table", () -> new BlockItem(DRAWING_TABLE.get(), new Item.Properties()));

    // ── 爆炸箭（物品 + 实体）──
    public static final DeferredItem<ExplosiveArrowItem> EXPLOSIVE_ARROW =
            ITEMS.register("explosive_arrow", () -> new ExplosiveArrowItem(new Item.Properties()));
    public static final DeferredHolder<EntityType<?>, EntityType<ExplosiveArrow>> EXPLOSIVE_ARROW_ENTITY =
            ENTITIES.register("explosive_arrow", () -> EntityType.Builder.<ExplosiveArrow>of(
                            (type, level) -> new ExplosiveArrow(type, level), MobCategory.MISC)
                    .sized(0.5F, 0.5F).eyeHeight(0.13F).clientTrackingRange(4).updateInterval(20)
                    .build("explosive_arrow"));

    // ── 投掷火焰弹（右键原版火焰弹投掷，类似恶魂火球但伤害固定 20）──
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownFireCharge>> THROWN_FIRE_CHARGE_ENTITY =
            ENTITIES.register("thrown_fire_charge", () -> EntityType.Builder.<ThrownFireCharge>of(
                            (type, level) -> new ThrownFireCharge(type, level), MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(10)
                    .build("thrown_fire_charge"));

    // ── 西瓜皮（吃西瓜片获得；可堆肥；1 个合成 1 个西瓜种子）──
    public static final DeferredItem<Item> WATERMELON_RIND =
            ITEMS.register("watermelon_rind", () -> new Item(new Item.Properties()));

    // ── 碗装腐烂食物（FD 等碗装菜品腐烂后的产物，保留木碗）──
    // craftRemainder(碗)：无论进食还是参与合成，木碗都能保留；堆肥时也会返还木碗。
    // 已加入 fiahi:leftovers 标签，不会二次腐烂。
    // 食用负面效果与 FIAHI 腐烂食物（leftover_vegetable/leftover_meat）一致：
    // 80% 概率饥饿 20 秒 + 80% 概率反胃 20 秒。
    public static final DeferredItem<Item> ROTTEN_MEAL_BOWL = ITEMS.register("rotten_meal_bowl",
            () -> new Item(new Item.Properties()
                    .craftRemainder(Items.BOWL)
                    .food(rottenBowlFood(0.1F))));
    public static final DeferredItem<Item> ROTTEN_MEAT_BOWL = ITEMS.register("rotten_meat_bowl",
            () -> new Item(new Item.Properties()
                    .craftRemainder(Items.BOWL)
                    .food(rottenBowlFood(0.3F))));

    /** 腐烂食物的食物属性：营养 1，80% 概率饥饿(20s) + 80% 概率反胃(20s)，与 FIAHI 腐烂食物一致。 */
    private static FoodProperties rottenBowlFood(float saturation) {
        return new FoodProperties.Builder()
                .nutrition(1).saturationModifier(saturation)
                .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 400, 0), 0.8F)
                .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 400, 0), 0.8F)
                .build();
    }

    // ── 废旧物品（废土感物资，绘制台原料，僵尸概率掉落）──
    public static final DeferredItem<Item> DAMAGED_GPU = ITEMS.register("damaged_gpu", plainItem());
    public static final DeferredItem<Item> DAMAGED_RAM = ITEMS.register("damaged_ram", plainItem());
    public static final DeferredItem<Item> IRON_DEBRIS = ITEMS.register("iron_debris", plainItem());
    public static final DeferredItem<Item> RUSTY_COPPER_SHEET = ITEMS.register("rusty_copper_sheet", plainItem());
    public static final DeferredItem<Item> SCRAP_ALUMINUM_SHEET = ITEMS.register("scrap_aluminum_sheet", plainItem());
    public static final DeferredItem<Item> BURNT_CHIP = ITEMS.register("burnt_chip", plainItem());
    public static final DeferredItem<Item> DAMAGED_WIRE = ITEMS.register("damaged_wire", plainItem());
    public static final DeferredItem<Item> DAMAGED_FILTER = ITEMS.register("damaged_filter", plainItem());
    public static final DeferredItem<Item> DAMAGED_CIRCUIT_BOARD = ITEMS.register("damaged_circuit_board", plainItem());
    public static final DeferredItem<Item> BROKEN_SCREW = ITEMS.register("broken_screw", plainItem());
    public static final DeferredItem<Item> RUSTY_SPRING = ITEMS.register("rusty_spring", plainItem());
    public static final DeferredItem<Item> SHATTERED_SCREEN = ITEMS.register("shattered_screen", plainItem());
    public static final DeferredItem<Item> DAMAGED_MOTOR = ITEMS.register("damaged_motor", plainItem());
    public static final DeferredItem<Item> BURNT_CABLE = ITEMS.register("burnt_cable", plainItem());

    // ── 绘制配方类型与序列化器 ──
    public static final DeferredHolder<RecipeType<?>, RecipeType<DrawingRecipe>> DRAWING_TYPE =
            RECIPE_TYPES.register("drawing", () -> RecipeType.simple(TimeX.rl("drawing")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DrawingRecipe>> DRAWING_SERIALIZER =
            RECIPE_SERIALIZERS.register("drawing", DrawingRecipe.Serializer::new);

    private WastelandRegistry() {
    }

    private static java.util.function.Supplier<Item> plainItem() {
        return () -> new Item(new Item.Properties());
    }

    /** 全部废旧物品（按定义顺序，供创造标签/僵尸掉落/JEI 展示使用）。 */
    public static List<DeferredItem<Item>> scrapItems() {
        return List.of(DAMAGED_GPU, DAMAGED_RAM, IRON_DEBRIS, RUSTY_COPPER_SHEET, SCRAP_ALUMINUM_SHEET,
                BURNT_CHIP, DAMAGED_WIRE, DAMAGED_FILTER, DAMAGED_CIRCUIT_BOARD, BROKEN_SCREW,
                RUSTY_SPRING, SHATTERED_SCREEN, DAMAGED_MOTOR, BURNT_CABLE);
    }

    /** 随机取一件废旧物品（僵尸掉落用），返回空物品表示列表为空。 */
    public static ItemStack randomScrap(RandomSource random) {
        List<DeferredItem<Item>> scraps = scrapItems();
        if (scraps.isEmpty()) return ItemStack.EMPTY;
        return new ItemStack(scraps.get(random.nextInt(scraps.size())).get());
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITIES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
    }
}
