package io.github.createmeow.timex_rebirth.crops;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

/**
 * 冬季作物注册：黑麦 / 芜菁。
 * - 作物方块：CropBlock 子类，耐寒生长（PlantTempData 配 frost=high、雪/暴风雪不枯萎），
 *   置于基地热场中生长显著加速。
 * - 种子：ItemNameBlockItem，右击耕地种植。
 * - 产物：黑麦粒（原料）/ 黑麦面粉 / 黑麦面包（食物）/ 黑麦秸秆（燃料 + 造纸原料）、
 *   芜菁（可生食耐储存）/ 烤芜菁（熔炉烤制）。
 */
public class WinterCropRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TimeX.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TimeX.MODID);

    private static BlockBehaviour.Properties cropProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .randomTicks()
                .instabreak()
                .sound(SoundType.CROP)
                .pushReaction(PushReaction.DESTROY);
    }

    // ── 作物方块 ──
    public static final DeferredBlock<RyeCropBlock> RYE_CROP =
            BLOCKS.register("rye_crop", () -> new RyeCropBlock(cropProperties()));
    public static final DeferredBlock<TurnipCropBlock> TURNIP_CROP =
            BLOCKS.register("turnip_crop", () -> new TurnipCropBlock(cropProperties()));

    // ── 种子（可种植）──
    public static final DeferredItem<ItemNameBlockItem> RYE_SEEDS =
            ITEMS.register("rye_seeds", () -> new ItemNameBlockItem(RYE_CROP.get(), new Item.Properties()));
    public static final DeferredItem<ItemNameBlockItem> TURNIP_SEEDS =
            ITEMS.register("turnip_seeds", () -> new ItemNameBlockItem(TURNIP_CROP.get(), new Item.Properties()));

    // ── 产物 ──
    public static final DeferredItem<Item> RYE =
            ITEMS.register("rye", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RYE_FLOUR =
            ITEMS.register("rye_flour", () -> new Item(new Item.Properties()));
    // 黑麦面团：石磨/粉碎轮磨出的面粉经搅拌机混合（或工作台合成）得到，再经熔炉/烟熏炉/营火烤制成黑麦面包
    public static final DeferredItem<Item> RYE_DOUGH =
            ITEMS.register("rye_dough", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RYE_BREAD =
            ITEMS.register("rye_bread", () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(6).saturationModifier(7.2F).build())
                    .rarity(Rarity.UNCOMMON)));
    // 黑麦秸秆：可作熔炉燃料（200 tick ≈ 烧制 1 个物品），同时是造纸原料
    public static final DeferredItem<Item> RYE_STRAW =
            ITEMS.register("rye_straw", () -> new Item(new Item.Properties()) {
                @Override
                public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
                    return 200;
                }
            });
    public static final DeferredItem<Item> TURNIP =
            ITEMS.register("turnip", () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.9F).build())));
    public static final DeferredItem<Item> BAKED_TURNIP =
            ITEMS.register("baked_turnip", () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(4).saturationModifier(4.8F).build())
                    .rarity(Rarity.UNCOMMON)));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}
