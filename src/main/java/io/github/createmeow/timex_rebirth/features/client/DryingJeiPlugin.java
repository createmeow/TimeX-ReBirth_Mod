package io.github.createmeow.timex_rebirth.features.client;

import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.features.DryingRackRecipe;
import io.github.createmeow.timex_rebirth.features.FireToolRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * JEI 配方展示：晾晒架（输入湿物 → 晒干产物 + 干燥时长）。
 * 本类仅被 JEI 的插件扫描器加载；未安装 JEI 时不会加载（避免缺少 mezz.jei.api 崩溃）。
 */
@JeiPlugin
public class DryingJeiPlugin implements IModPlugin {
    public static final RecipeType<DryingRackRecipe> TYPE =
            RecipeType.create(TimeX.MODID, "drying_rack", DryingRackRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return TimeX.rl("drying_rack_jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new DryingRackRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var recipes = level.getRecipeManager().getAllRecipesFor(FireToolRegistry.DRYING_RACK_TYPE.get())
                .stream().map(holder -> holder.value()).toList();
        registration.addRecipes(TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(FireToolRegistry.DRYING_RACK_ITEM.get()), TYPE);
    }

    /** 晾晒架 JEI 配方分类：输入槽 + 输出槽 + 干燥时长文本。 */
    public static class DryingRackRecipeCategory implements IRecipeCategory<DryingRackRecipe> {
        private final IGuiHelper guiHelper;

        public DryingRackRecipeCategory(IGuiHelper guiHelper) {
            this.guiHelper = guiHelper;
        }

        @Override
        public RecipeType<DryingRackRecipe> getRecipeType() {
            return TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("jei.timex_rebirth.drying_rack.title");
        }

        @Override
        public int getWidth() {
            return 116;
        }

        @Override
        public int getHeight() {
            return 36;
        }

        @Override
        public IDrawable getIcon() {
            return guiHelper.createDrawableItemStack(new ItemStack(FireToolRegistry.DRYING_RACK_ITEM.get()));
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, DryingRackRecipe recipe, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, 18, 10)
                    .addIngredients(recipe.getIngredient())
                    .setStandardSlotBackground();
            builder.addSlot(RecipeIngredientRole.OUTPUT, 82, 10)
                    .addItemStack(recipe.getResult())
                    .setStandardSlotBackground();
        }

        @Override
        public void draw(DryingRackRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                         double mouseX, double mouseY) {
            String text = Component.translatable("jei.timex_rebirth.drying_rack.time",
                    recipe.getDryingTime() / 20).getString();
            guiGraphics.drawString(Minecraft.getInstance().font, text, 40, 15, 0xFF333333, false);
        }
    }
}