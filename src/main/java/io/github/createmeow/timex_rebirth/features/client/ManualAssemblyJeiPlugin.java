package io.github.createmeow.timex_rebirth.features.client;

import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.workbench.ManualAssemblyRecipe;
import io.github.createmeow.timex_rebirth.workbench.WorkbenchRegistry;
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
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * JEI 配方展示：手动物品组装（基底 → 步骤1 → 步骤2 → ... → 产物）。
 * 本类仅被 JEI 的插件扫描器加载；未安装 JEI 时不会加载（避免缺少 mezz.jei.api 崩溃）。
 * <p>布局示意（工作台配方）：</p>
 * <pre>[木板] → 工作剪 → 工作锤 → 工作锯 → [工作台]</pre>
 */
@JeiPlugin
public class ManualAssemblyJeiPlugin implements IModPlugin {
    public static final RecipeType<ManualAssemblyRecipe> TYPE =
            RecipeType.create(TimeX.MODID, "manual_assembly", ManualAssemblyRecipe.class);

    /** 类别布局参数：槽距 24（18px 槽 + 6px 箭头间隙）。 */
    private static final int SLOT = 18;
    private static final int PITCH = 24;
    /** 支持的最大槽位数（基底 + 步骤 + 产物）。 */
    private static final int MAX_SLOTS = 6;
    private static final int WIDTH = 4 + MAX_SLOTS * PITCH - (PITCH - SLOT);
    private static final int HEIGHT = 38;

    @Override
    public ResourceLocation getPluginUid() {
        return TimeX.rl("manual_assembly_jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new ManualAssemblyRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var recipes = level.getRecipeManager().getAllRecipesFor(WorkbenchRegistry.MANUAL_ASSEMBLY_TYPE.get())
                .stream().map(holder -> holder.value()).toList();
        registration.addRecipes(TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // 以木板为查看入口（手持木板查配方即可看到组装流程）
        registration.addRecipeCatalyst(new ItemStack(Items.OAK_PLANKS), TYPE);
        registration.addRecipeCatalyst(new ItemStack(WorkbenchRegistry.WORK_SHEARS.get()), TYPE);
        registration.addRecipeCatalyst(new ItemStack(WorkbenchRegistry.WORK_HAMMER.get()), TYPE);
        registration.addRecipeCatalyst(new ItemStack(WorkbenchRegistry.WORK_SAW.get()), TYPE);
    }

    /** 手动物品组装 JEI 分类：基底槽 + 有序步骤槽 + 产物槽，槽间箭头与步骤序号。 */
    public static class ManualAssemblyRecipeCategory implements IRecipeCategory<ManualAssemblyRecipe> {
        private final IGuiHelper guiHelper;

        public ManualAssemblyRecipeCategory(IGuiHelper guiHelper) {
            this.guiHelper = guiHelper;
        }

        @Override
        public RecipeType<ManualAssemblyRecipe> getRecipeType() {
            return TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("jei.timex_rebirth.manual_assembly.title");
        }

        @Override
        public int getWidth() {
            return WIDTH;
        }

        @Override
        public int getHeight() {
            return HEIGHT;
        }

        @Override
        public IDrawable getIcon() {
            return guiHelper.createDrawableItemStack(new ItemStack(WorkbenchRegistry.WORKBENCH_HALF_ITEM.get()));
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, ManualAssemblyRecipe recipe, IFocusGroup focuses) {
            // 总槽位 = 基底 + 步骤数 + 产物，整体水平居中
            int slotCount = 2 + recipe.getSteps().size();
            int startX = (WIDTH - (slotCount * PITCH - (PITCH - SLOT))) / 2;

            builder.addSlot(RecipeIngredientRole.INPUT, startX, 10)
                    .addIngredients(recipe.getBase())
                    .setStandardSlotBackground();

            for (int i = 0; i < recipe.getSteps().size(); i++) {
                int x = startX + (i + 1) * PITCH;
                builder.addSlot(RecipeIngredientRole.INPUT, x, 10)
                        .addIngredients(recipe.getSteps().get(i))
                        .setStandardSlotBackground();
            }

            int outX = startX + (slotCount - 1) * PITCH;
            builder.addSlot(RecipeIngredientRole.OUTPUT, outX, 10)
                    .addItemStack(recipe.getResult())
                    .setStandardSlotBackground();
        }

        @Override
        public void draw(ManualAssemblyRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                         double mouseX, double mouseY) {
            var font = Minecraft.getInstance().font;
            int slotCount = 2 + recipe.getSteps().size();
            int startX = (WIDTH - (slotCount * PITCH - (PITCH - SLOT))) / 2;

            // 槽间箭头 + 步骤序号
            for (int i = 0; i < slotCount - 1; i++) {
                int arrowX = startX + i * PITCH + SLOT + 2;
                guiGraphics.drawString(font, "→", arrowX, 16, 0xFF555555, false);
                if (i < recipe.getSteps().size()) {
                    String label = String.valueOf(i + 1);
                    guiGraphics.drawString(font, label, startX + (i + 1) * PITCH + 7, 30, 0xFF888888, false);
                }
            }
        }
    }
}
