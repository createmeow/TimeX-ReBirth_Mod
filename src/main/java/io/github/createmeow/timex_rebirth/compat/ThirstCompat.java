package io.github.createmeow.timex_rebirth.compat;

import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.crops.WinterCropRegistry;
import io.github.createmeow.timex_rebirth.food.WastelandFoodRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * Thirst Was Taken 口渴值联动（反射，无编译期依赖）。
 *
 * <p>ThirstWasTaken 在 {@link ServerStartedEvent} 时初始化
 * {@code ThirstHelper.VALID_FOODS / VALID_DRINKS}（其官方入口 RegisterThirstValueEvent 亦在此时发布），
 * 我们于同一时机向两张表 putIfAbsent 注册口渴恢复值（不覆盖口渴模组自身/配置已有的值）：
 * <ul>
 *     <li>芜菁、烤芜菁（本模组，生吃含水）</li>
 *     <li>农夫乐事：热可可、牛奶瓶（饮品）；番茄、卷心菜、洋葱汤、骨头汤、发光浆果羹（含水菜品，
 *         默认配置未覆盖）</li>
 *     <li>冷汗：满水袋（filled_waterskin）</li>
 * </ul>
 */
@EventBusSubscriber(modid = TimeX.MODID)
public class ThirstCompat {
    private static boolean checked = false;
    private static boolean loaded = false;

    public static boolean isLoaded() {
        if (!checked) {
            checked = true;
            try {
                Class.forName("dev.ghen.thirst.api.ThirstHelper");
                loaded = true;
            } catch (Exception e) {
                loaded = false;
            }
        }
        return loaded;
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!isLoaded()) {
            return;
        }
        try {
            Class<?> helper = Class.forName("dev.ghen.thirst.api.ThirstHelper");
            Field drinksField = helper.getField("VALID_DRINKS");
            Field foodsField = helper.getField("VALID_FOODS");
            @SuppressWarnings("unchecked")
            Map<Item, Number[]> drinks = (Map<Item, Number[]>) drinksField.get(null);
            @SuppressWarnings("unchecked")
            Map<Item, Number[]> foods = (Map<Item, Number[]>) foodsField.get(null);

            // 本模组：芜菁（生吃含水）、烤芜菁
            put(foods, WinterCropRegistry.TURNIP.get(), 3, 4);
            put(foods, WinterCropRegistry.BAKED_TURNIP.get(), 2, 3);

            // 本模组：废土食物（热食含汤汁、罐头含水分）
            put(foods, WastelandFoodRegistry.WASTELAND_STEW.get(), 5, 6);
            put(foods, WastelandFoodRegistry.WASTELAND_BROTH.get(), 4, 5);
            put(foods, WastelandFoodRegistry.CANNED_FOOD.get(), 2, 3);
            put(foods, WastelandFoodRegistry.INSTANT_NOODLES.get(), 2, 3);
            put(drinks, WastelandFoodRegistry.HERBAL_TEA.get(), 5, 7);

            // 农夫乐事：饮品（默认配置仅覆盖苹果酒/西瓜汁）
            put(drinks, item("farmersdelight", "hot_cocoa"), 5, 9);
            put(drinks, item("farmersdelight", "milk_bottle"), 5, 8);

            // 农夫乐事：含水菜品（默认配置未覆盖）
            put(foods, item("farmersdelight", "tomato"), 3, 4);
            put(foods, item("farmersdelight", "cabbage"), 2, 3);
            put(foods, item("farmersdelight", "onion_soup"), 4, 5);
            put(foods, item("farmersdelight", "bone_broth"), 4, 5);
            put(foods, item("farmersdelight", "glow_berry_custard"), 3, 4);

            // 冷汗：满水袋
            put(drinks, item("coldsweat", "filled_waterskin"), 8, 10);

            // 草药茶注册为 ThirstWasTaken"水容器"：
            // Create 装瓶（filling）时 MixinGenericItemFilling 会把茶液的纯净度写入瓶装草药茶物品
            registerTeaContainer();
        } catch (Exception e) {
            // 联调失败不影响游戏
        }
    }

    /**
     * 反射调用 WaterPurity.addContainer(new ContainerWithPurity(草药茶))（单参静态容器，
     * 与 ThirstWasTaken 处理 ContainerConfig.CONTAINERS 的方式一致）。
     * 使草药茶物品被识别为"受纯净度影响的容器"：装瓶时 MixinGenericItemFilling
     * 会把茶液的纯净度写入瓶装草药茶物品（tooltip 显示、饮用生效），
     * 且加热搅拌产出的草药茶液已由 ThirstWasTaken 的 MixinBasinRecipe 自动继承输入水的纯净度（流体名含 "tea"）。
     */
    private static void registerTeaContainer() {
        try {
            Class<?> wp = Class.forName("dev.ghen.thirst.content.purity.WaterPurity");
            Class<?> cwp = Class.forName("dev.ghen.thirst.content.purity.ContainerWithPurity");
            Constructor<?> ctor = cwp.getConstructor(Item.class);
            Object container = ctor.newInstance(WastelandFoodRegistry.HERBAL_TEA.get());
            wp.getMethod("addContainer", cwp).invoke(null, container);
        } catch (Exception e) {
            // 纯净度联动失败不影响游戏
        }
    }

    private static void put(Map<Item, Number[]> map, Item item, int thirst, int quenched) {
        if (item != null && item != Items.AIR) {
            map.putIfAbsent(item, new Number[]{thirst, quenched});
        }
    }

    private static Item item(String modid, String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(modid, path));
    }
}
