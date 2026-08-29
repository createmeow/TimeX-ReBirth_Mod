package io.github.createmeow.timex_rebirth.datagen;

import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.features.FireToolRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

/**
 * 生火/晾晒系统配方生成器：
 * <ul>
 *   <li>4 干枝条 → 干燥的木条（无序）；</li>
 *   <li>4 木棍（2×2）→ 晾晒架（有序）。</li>
 * </ul>
 */
public class TimeXRecipeProvider extends RecipeProvider {

    public TimeXRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // 4 干枝条 → 干燥的木条
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC,
                        io.github.createmeow.timex_rebirth.features.KindlingRegistry.DRY_KINDLING_ITEM.get(), 1)
                .requires(FireToolRegistry.DRY_TWIG.get(), 4)
                .unlockedBy("has_dry_twig", has(FireToolRegistry.DRY_TWIG.get()))
                .save(output, TimeX.rl("dry_kindling"));

        // 4 木棍（2×2）→ 晾晒架
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, FireToolRegistry.DRYING_RACK_ITEM, 1)
                .pattern("SS")
                .pattern("SS")
                .define('S', Items.STICK)
                .unlockedBy("has_stick", has(Items.STICK))
                .save(output, TimeX.rl("drying_rack"));
    }
}
