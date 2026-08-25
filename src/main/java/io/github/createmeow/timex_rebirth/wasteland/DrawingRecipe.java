package io.github.createmeow.timex_rebirth.wasteland;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * 绘制台配方：消耗特定物品（废旧物资等）兑换阅历。
 * 每次绘制的阅历在 [minPoints, maxPoints] 区间内随机，区间由物品的技术价值决定
 * （高价值电子元件如 GPU/内存条区间更高，普通废料如螺丝/弹簧区间更低）。
 * 结果不是物品而是"阅历"数值，故 getResultItem 返回空；
 * 界面展示交给 JEI/EMI（输入物品槽 + "min ~ max 阅历"文本）。
 * 数据位于 data/timex_rebirth/recipe/drawing/*.json（自定义配方类型，不受 doLimitedCrafting 影响）。
 */
public class DrawingRecipe implements Recipe<SingleRecipeInput> {
    private final Ingredient ingredient;
    private final int minPoints;
    private final int maxPoints;

    public DrawingRecipe(Ingredient ingredient, int minPoints, int maxPoints) {
        this.ingredient = ingredient;
        // 兜底：保证 max >= min（JSON 配置错误时也不崩溃）
        this.minPoints = Math.min(minPoints, maxPoints);
        this.maxPoints = Math.max(minPoints, maxPoints);
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    /** 最小阅历（含）。 */
    public int getMinPoints() {
        return minPoints;
    }

    /** 最大阅历（含）。 */
    public int getMaxPoints() {
        return maxPoints;
    }

    /** 本次绘制随机获得的阅历（闭区间 [min, max]）。 */
    public int rollPoints(RandomSource random) {
        return minPoints + random.nextInt(maxPoints - minPoints + 1);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return !input.item().isEmpty() && ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return WastelandRegistry.DRAWING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return WastelandRegistry.DRAWING_TYPE.get();
    }

    /** 不在合成配方书中显示。 */
    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<DrawingRecipe> {
        public static final MapCodec<DrawingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(DrawingRecipe::getIngredient),
                Codec.INT.fieldOf("min").forGetter(DrawingRecipe::getMinPoints),
                Codec.INT.fieldOf("max").forGetter(DrawingRecipe::getMaxPoints)
        ).apply(inst, DrawingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, DrawingRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, DrawingRecipe::getIngredient,
                        ByteBufCodecs.VAR_INT, DrawingRecipe::getMinPoints,
                        ByteBufCodecs.VAR_INT, DrawingRecipe::getMaxPoints,
                        DrawingRecipe::new);

        @Override
        public MapCodec<DrawingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DrawingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
