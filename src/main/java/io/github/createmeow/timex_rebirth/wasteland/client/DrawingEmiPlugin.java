package io.github.createmeow.timex_rebirth.wasteland.client;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.wasteland.DrawingRecipe;
import io.github.createmeow.timex_rebirth.wasteland.WastelandRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/**
 * EMI 配方展示：绘制台（输入废旧物品 → N 阅历）。
 * 本类仅被 EMI 的入口点扫描器加载；未安装 EMI 时不会加载。
 * 玩家包实际使用 EMI（run/mods 中为 emi），此插件保证绘制配方在 EMI 中可见。
 */
@EmiEntrypoint
public class DrawingEmiPlugin implements EmiPlugin {
    public static final EmiRecipeCategory CATEGORY =
            new EmiRecipeCategory(TimeX.rl("drawing"), EmiStack.of(WastelandRegistry.DRAWING_TABLE_ITEM.get()));

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(CATEGORY);
        registry.addWorkstation(CATEGORY, EmiStack.of(WastelandRegistry.DRAWING_TABLE_ITEM.get()));
        List<RecipeHolder<DrawingRecipe>> recipes =
                registry.getRecipeManager().getAllRecipesFor(WastelandRegistry.DRAWING_TYPE.get());
        for (RecipeHolder<DrawingRecipe> holder : recipes) {
            registry.addRecipe(new DrawingEmiRecipe(holder));
        }
    }

    /** EMI 绘制配方：输入槽 + "N 阅历"文本。 */
    public static class DrawingEmiRecipe extends BasicEmiRecipe {
        private final DrawingRecipe recipe;

        public DrawingEmiRecipe(RecipeHolder<DrawingRecipe> holder) {
            super(CATEGORY, holder.id(), 116, 36);
            this.recipe = holder.value();
            this.inputs = List.of(EmiIngredient.of(recipe.getIngredient()));
            this.outputs = List.of();
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addSlot(inputs.get(0), 18, 10);
            widgets.addText(Component.translatable("jei.timex_rebirth.drawing.points",
                            recipe.getMinPoints(), recipe.getMaxPoints()),
                    40, 15, 0xFF333333, false);
        }
    }
}
