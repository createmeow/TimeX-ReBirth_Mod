package io.github.createmeow.timex_rebirth.workbench;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 手动物品组装配方（数据驱动，仅用于配方查看器展示）：
 * 基底 → 步骤1 → 步骤2 → ... → 产物。
 * <p>与 {@link WorkbenchAssemblyHandler} 的右键组装流程对应
 * （工作台：木板 → 工作剪 → 工作锤 → 工作锯 → 工作台）。
 * 实际组装逻辑仍由事件处理器执行，本配方类型供 JEI/EMI 展示，
 * 数据位于 {@code data/timex_rebirth/recipe/*.json}。</p>
 */
public class ManualAssemblyRecipe implements Recipe<SingleRecipeInput> {
    private final Ingredient base;
    private final List<Ingredient> steps;
    private final ItemStack result;

    public ManualAssemblyRecipe(Ingredient base, List<Ingredient> steps, ItemStack result) {
        this.base = base;
        this.steps = List.copyOf(steps);
        this.result = result;
    }

    /** 基底（如木板）。 */
    public Ingredient getBase() {
        return base;
    }

    /** 有序步骤物品（如 工作剪→工作锤→工作锯）。 */
    public List<Ingredient> getSteps() {
        return steps;
    }

    /** 输出结果（复制品）。 */
    public ItemStack getResult() {
        return result.copy();
    }

    /** 结果堆栈（内部引用，供 StreamCodec 编码用）。 */
    public ItemStack getResultStack() {
        return result;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return !input.item().isEmpty() && base.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return getResult();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return getResult();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return WorkbenchRegistry.MANUAL_ASSEMBLY_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return WorkbenchRegistry.MANUAL_ASSEMBLY_TYPE.get();
    }

    /** 不在合成配方书中显示。 */
    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<ManualAssemblyRecipe> {
        public static final MapCodec<ManualAssemblyRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("base").forGetter(ManualAssemblyRecipe::getBase),
                Ingredient.CODEC_NONEMPTY.listOf().fieldOf("steps").forGetter(ManualAssemblyRecipe::getSteps),
                ItemStack.CODEC.fieldOf("result").forGetter(ManualAssemblyRecipe::getResultStack)
        ).apply(inst, ManualAssemblyRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ManualAssemblyRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, ManualAssemblyRecipe::getBase,
                        Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), ManualAssemblyRecipe::getSteps,
                        ItemStack.STREAM_CODEC, ManualAssemblyRecipe::getResultStack,
                        ManualAssemblyRecipe::new);

        @Override
        public MapCodec<ManualAssemblyRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ManualAssemblyRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
