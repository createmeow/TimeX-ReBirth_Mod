package io.github.createmeow.timex_rebirth.features;

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

/**
 * 晾晒架配方：仿照 StoneAge 的 {@code DryingRackRecipe}，用<b>数据驱动</b>方式
 * 定义「输入 ingredient → 输出 result + 干燥时长 dryingTime」。
 * <p>数据位于 {@code data/timex_rebirth/recipe/drying_rack/*.json}（自定义配方类型，
 * 不受 doLimitedCrafting 影响）。这样不用为每种可晾晒物硬编码转换逻辑。</p>
 */
public class DryingRackRecipe implements Recipe<SingleRecipeInput> {
    private final Ingredient ingredient;
    private final ItemStack result;
    private final int dryingTime;

    public DryingRackRecipe(Ingredient ingredient, ItemStack result, int dryingTime) {
        this.ingredient = ingredient;
        this.result = result;
        this.dryingTime = Math.max(1, dryingTime);
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    /** 输出结果（复制品）。 */
    public ItemStack getResult() {
        return result.copy();
    }

    /** 干燥所需 tick 数。 */
    public int getDryingTime() {
        return dryingTime;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return !input.item().isEmpty() && ingredient.test(input.item());
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
        return FireToolRegistry.DRYING_RACK_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return FireToolRegistry.DRYING_RACK_TYPE.get();
    }

    /** 不在合成配方书中显示。 */
    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<DryingRackRecipe> {
        public static final MapCodec<DryingRackRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(DryingRackRecipe::getIngredient),
                ItemStack.CODEC.fieldOf("result").forGetter(DryingRackRecipe::getResult),
                Codec.INT.fieldOf("dryingTime").forGetter(DryingRackRecipe::getDryingTime)
        ).apply(inst, DryingRackRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, DryingRackRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, DryingRackRecipe::getIngredient,
                        ItemStack.STREAM_CODEC, DryingRackRecipe::getResult,
                        ByteBufCodecs.VAR_INT, DryingRackRecipe::getDryingTime,
                        DryingRackRecipe::new);

        @Override
        public MapCodec<DryingRackRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DryingRackRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
