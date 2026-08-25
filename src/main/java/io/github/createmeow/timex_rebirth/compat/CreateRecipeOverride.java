package io.github.createmeow.timex_rebirth.compat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * create 配方运行时覆盖：把机械动力的"面团"产线统一到农夫乐事的面团。
 * <p>
 * 为什么不受数据包覆盖影响：dev/ModDevGradle 环境下，本项目（源码 mod）的数据包层叠
 * 在第三方 mod（create）jar 的数据包之下，data/create/recipe/... 的同名 JSON 会被 create
 * 的原版配方覆盖，因此需要运行时把 recipe manager 里的这几个配方替换掉。
 * <p>
 * 实现：在服务端启动后，列出所有配方，按 id 移除 create 的 5 个旧配方，注入用同 id 解析的
 * 农夫乐事版配方，然后调用 {@link RecipeManager#replaceRecipes} 重建 byType/byName 索引。
 * 通过反射调用 protected static 的 {@code RecipeManager.fromJson} 以保持与原版一致的解析行为。
 */
public final class CreateRecipeOverride {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation SLIME = ResourceLocation.fromNamespaceAndPath("create", "crafting/appliances/slime_ball");
    private static final ResourceLocation DOUGH = ResourceLocation.fromNamespaceAndPath("create", "mixing/dough_by_mixing");
    private static final ResourceLocation BREAD_SMELT = ResourceLocation.fromNamespaceAndPath("create", "smelting/bread");
    private static final ResourceLocation BREAD_SMOKE = ResourceLocation.fromNamespaceAndPath("create", "smoking/bread");
    private static final ResourceLocation BREAD_CAMPFIRE = ResourceLocation.fromNamespaceAndPath("create", "campfire_cooking/bread");

    private static final String SLIME_JSON = """
            {"type":"minecraft:crafting_shapeless","category":"misc",
             "ingredients":[{"tag":"c:foods/dough"},{"tag":"c:dyes/lime"}],
             "result":{"count":1,"id":"minecraft:slime_ball"}}""";

    private static final String DOUGH_JSON = """
            {"type":"create:mixing",
             "ingredients":[{"tag":"c:flours/wheat"},{"type":"neoforge:single","amount":1000,"fluid":"minecraft:water"}],
             "results":[{"id":"farmersdelight:wheat_dough"}]}""";

    private static final String BREAD_SMELT_JSON = """
            {"type":"minecraft:smelting","category":"food","cookingtime":200,"experience":0.0,
             "ingredient":{"item":"farmersdelight:wheat_dough"},
             "result":{"count":1,"id":"minecraft:bread"}}""";

    private static final String BREAD_SMOKE_JSON = """
            {"type":"minecraft:smoking","category":"food","cookingtime":100,"experience":0.0,
             "ingredient":{"item":"farmersdelight:wheat_dough"},
             "result":{"count":1,"id":"minecraft:bread"}}""";

    private static final String BREAD_CAMPFIRE_JSON = """
            {"type":"minecraft:campfire_cooking","category":"food","cookingtime":600,"experience":0.0,
             "ingredient":{"item":"farmersdelight:wheat_dough"},
             "result":{"count":1,"id":"minecraft:bread"}}""";

    private CreateRecipeOverride() {
    }

    /** 在服务端启动后调用：替换 create 的面团相关配方为农夫乐事版。 */
    public static void apply(RecipeManager recipeManager, HolderLookup.Provider registries) {
        List<RecipeHolder<?>> all = new ArrayList<>(recipeManager.getRecipes());
        int before = all.size();

        all.removeIf(h -> h.id().equals(SLIME) || h.id().equals(DOUGH)
                || h.id().equals(BREAD_SMELT) || h.id().equals(BREAD_SMOKE) || h.id().equals(BREAD_CAMPFIRE));

        addFromJson(all, SLIME, SLIME_JSON, registries);
        addFromJson(all, DOUGH, DOUGH_JSON, registries);
        addFromJson(all, BREAD_SMELT, BREAD_SMELT_JSON, registries);
        addFromJson(all, BREAD_SMOKE, BREAD_SMOKE_JSON, registries);
        addFromJson(all, BREAD_CAMPFIRE, BREAD_CAMPFIRE_JSON, registries);

        recipeManager.replaceRecipes(all);
        LOGGER.info("[TimeXRecipeOverride] replaced create dough recipes: {} -> {} recipes", before, all.size());
    }

    private static void addFromJson(List<RecipeHolder<?>> all, ResourceLocation id, String json, HolderLookup.Provider registries) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            Method fromJson = RecipeManager.class.getDeclaredMethod(
                    "fromJson", ResourceLocation.class, JsonObject.class, HolderLookup.Provider.class);
            fromJson.setAccessible(true);
            @SuppressWarnings("unchecked")
            RecipeHolder<?> holder = (RecipeHolder<?>) fromJson.invoke(null, id, obj, registries);
            all.add(holder);
            LOGGER.info("[TimeXRecipeOverride] added {} -> {}", id, holder.value().getResultItem(registries));
        } catch (Exception e) {
            LOGGER.error("[TimeXRecipeOverride] failed to inject recipe {}", id, e);
        }
    }
}
