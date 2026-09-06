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
import io.github.createmeow.timex_rebirth.workbench.ManualAssemblyRecipe;
import io.github.createmeow.timex_rebirth.workbench.WorkbenchRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/**
 * EMI 配方展示：手动物品组装（基底 → 步骤1 → 步骤2 → ... → 产物）。
 * 本类仅被 EMI 的入口点扫描器加载；未安装 EMI 时不会加载。
 * <p>布局示意（工作台配方）：</p>
 * <pre>[木板] → 工作剪 → 工作锤 → 工作锯 → [工作台]</pre>
 */
@EmiEntrypoint
public class ManualAssemblyEmiPlugin implements EmiPlugin {
    public static final EmiRecipeCategory CATEGORY =
            new EmiRecipeCategory(TimeX.rl("manual_assembly"), EmiStack.of(Items.OAK_PLANKS));

    private static final int SLOT = 18;
    private static final int PITCH = 24;
    private static final int WIDTH = 4 + 6 * PITCH - (PITCH - SLOT);
    private static final int HEIGHT = 38;

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(CATEGORY);
        // 以木板/工作工具为查看入口
        registry.addWorkstation(CATEGORY, EmiStack.of(Items.OAK_PLANKS));
        registry.addWorkstation(CATEGORY, EmiStack.of(WorkbenchRegistry.WORK_SHEARS.get()));
        registry.addWorkstation(CATEGORY, EmiStack.of(WorkbenchRegistry.WORK_HAMMER.get()));
        registry.addWorkstation(CATEGORY, EmiStack.of(WorkbenchRegistry.WORK_SAW.get()));
        List<RecipeHolder<ManualAssemblyRecipe>> recipes =
                registry.getRecipeManager().getAllRecipesFor(WorkbenchRegistry.MANUAL_ASSEMBLY_TYPE.get());
        for (RecipeHolder<ManualAssemblyRecipe> holder : recipes) {
            registry.addRecipe(new ManualAssemblyEmiRecipe(holder));
        }
    }

    /** EMI 手动组装配方：基底槽 + 有序步骤槽 + 产物槽，槽间箭头与步骤序号。 */
    public static class ManualAssemblyEmiRecipe extends BasicEmiRecipe {
        private final ManualAssemblyRecipe recipe;

        public ManualAssemblyEmiRecipe(RecipeHolder<ManualAssemblyRecipe> holder) {
            super(CATEGORY, holder.id(), WIDTH, HEIGHT);
            this.recipe = holder.value();
            // inputs: 基底 + 各步骤；outputs: 产物
            java.util.ArrayList<EmiIngredient> ins = new java.util.ArrayList<>();
            ins.add(EmiIngredient.of(recipe.getBase()));
            for (var step : recipe.getSteps()) {
                ins.add(EmiIngredient.of(step));
            }
            this.inputs = ins;
            this.outputs = List.of(EmiStack.of(recipe.getResult()));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            int slotCount = inputs.size() + 1; // 输入槽 + 产物槽
            int startX = (width - (slotCount * PITCH - (PITCH - SLOT))) / 2;

            for (int i = 0; i < inputs.size(); i++) {
                widgets.addSlot(inputs.get(i), startX + i * PITCH, 10);
            }
            int outX = startX + (slotCount - 1) * PITCH;
            widgets.addSlot(outputs.get(0), outX, 10).recipeContext(this);

            // 槽间箭头 + 步骤序号
            for (int i = 0; i < slotCount - 1; i++) {
                widgets.addText(Component.literal("→"),
                        startX + i * PITCH + SLOT, 15, 0xFF555555, false);
                if (i < recipe.getSteps().size()) {
                    widgets.addText(Component.literal(String.valueOf(i + 1)),
                            startX + (i + 1) * PITCH + 7, 30, 0xFF888888, false);
                }
            }
        }
    }
}
