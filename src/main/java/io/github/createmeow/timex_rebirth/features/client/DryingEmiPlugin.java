package io.github.createmeow.timex_rebirth.features.client;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.features.DryingRackRecipe;
import io.github.createmeow.timex_rebirth.features.FireToolRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/**
 * EMI 配方展示：晾晒架（输入湿物 → 晒干产物 + 干燥时长）。
 * 本类仅被 EMI 的入口点扫描器加载；未安装 EMI 时不会加载。
 */
@EmiEntrypoint
public class DryingEmiPlugin implements EmiPlugin {
    public static final EmiRecipeCategory CATEGORY =
            new EmiRecipeCategory(TimeX.rl("drying_rack"), EmiStack.of(FireToolRegistry.DRYING_RACK_ITEM.get()));

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(CATEGORY);
        registry.addWorkstation(CATEGORY, EmiStack.of(FireToolRegistry.DRYING_RACK_ITEM.get()));
        List<RecipeHolder<DryingRackRecipe>> recipes =
                registry.getRecipeManager().getAllRecipesFor(FireToolRegistry.DRYING_RACK_TYPE.get());
        for (RecipeHolder<DryingRackRecipe> holder : recipes) {
            registry.addRecipe(new DryingEmiRecipe(holder));
        }
    }

    /** EMI 晾晒配方：输入槽 + 输出槽 + 干燥时长文本。 */
    public static class DryingEmiRecipe extends BasicEmiRecipe {
        private final DryingRackRecipe recipe;

        public DryingEmiRecipe(RecipeHolder<DryingRackRecipe> holder) {
            super(CATEGORY, holder.id(), 116, 36);
            this.recipe = holder.value();
            this.inputs = List.of(EmiIngredient.of(recipe.getIngredient()));
            this.outputs = List.of(EmiStack.of(recipe.getResult()));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addSlot(inputs.get(0), 18, 10);
            widgets.addSlot(outputs.get(0), 82, 10).recipeContext(this);
            widgets.addText(Component.translatable("jei.timex_rebirth.drying_rack.time",
                            recipe.getDryingTime() / 20),
                    40, 15, 0xFF333333, false);
        }
    }
}