package com.createmeow.currency_plugin.client;

import com.createmeow.currency_plugin.CurrencyPlugin;
import com.createmeow.currency_plugin.init.CurrencyBlocks;
import com.createmeow.currency_plugin.init.CurrencyItems;
import com.createmeow.currency_plugin.machine.RefinerBlockEntity;
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
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JEI 配方展示：提炼机（原料 → 1 腐空朽 / 100 点提炼量）。
 * 每条配方显示单次消耗原料获得的提炼量区间（X ~ Y 点）。
 * 本类仅被 JEI 的插件扫描器加载；未安装 JEI 时不会加载（避免缺少 mezz.jei.api 崩溃）。
 */
@JeiPlugin
public class RefinerJeiPlugin implements IModPlugin {

    public static final RecipeType<RefinerRecipe> TYPE =
            RecipeType.create(CurrencyPlugin.MODID, "refining", RefinerRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(CurrencyPlugin.MODID, "refiner_jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new RefinerRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<RefinerRecipe> recipes = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : RefinerBlockEntity.getRefineValues().entrySet()) {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.getKey()));
            if (item == null || item == net.minecraft.world.item.Items.AIR) continue;
            recipes.add(new RefinerRecipe(new ItemStack(item), entry.getValue()[0], entry.getValue()[1]));
        }
        registration.addRecipes(TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(CurrencyBlocks.REFINING_MACHINE_ITEM.get()), TYPE);
    }

    /** 提炼配方（JEI 展示用）：原料物品 + 单次提炼量区间。 */
    public record RefinerRecipe(ItemStack input, int minPoints, int maxPoints) {
    }

    /** 提炼机 JEI 分类：原料槽 + 产物槽（1 腐空朽）+ 提炼量区间文本。 */
    public static class RefinerRecipeCategory implements IRecipeCategory<RefinerRecipe> {
        private final IGuiHelper guiHelper;

        public RefinerRecipeCategory(IGuiHelper guiHelper) {
            this.guiHelper = guiHelper;
        }

        @Override
        public RecipeType<RefinerRecipe> getRecipeType() {
            return TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("jei.currency_plugin.refiner.title");
        }

        @Override
        public int getWidth() {
            return 128;
        }

        @Override
        public int getHeight() {
            return 46;
        }

        @Override
        public IDrawable getIcon() {
            return guiHelper.createDrawableItemStack(new ItemStack(CurrencyBlocks.REFINING_MACHINE_ITEM.get()));
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, RefinerRecipe recipe, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, 6, 6)
                    .addItemStack(recipe.input())
                    .setStandardSlotBackground();
            builder.addSlot(RecipeIngredientRole.OUTPUT, 104, 6)
                    .addItemStack(new ItemStack(CurrencyItems.COMMON_COIN.get()))
                    .setStandardSlotBackground();
        }

        @Override
        public void draw(RefinerRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                         double mouseX, double mouseY) {
            var font = Minecraft.getInstance().font;
            String points = Component.translatable("jei.currency_plugin.refiner.points",
                    recipe.minPoints(), recipe.maxPoints()).getString();
            guiGraphics.drawCenteredString(font, points, 64, 8, 0xFF333333);
            Component per = Component.translatable("jei.currency_plugin.refiner.per_coin")
                    .withStyle(ChatFormatting.DARK_GRAY);
            guiGraphics.drawCenteredString(font, per, 64, 32, 0xFFFFFFFF);
        }
    }
}
