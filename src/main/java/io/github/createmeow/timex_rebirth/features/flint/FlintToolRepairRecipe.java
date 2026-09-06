package io.github.createmeow.timex_rebirth.features.flint;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 燧石工具修复配方（合成台，无序）：
 * 1 个受损的燧石工具 + 1 个燧石 → 恢复 25% 耐久（附魔等组件保留）的同一工具。
 * 每种工具对应一份 JSON（data/timex_rebirth/recipe/flint_*_repair.json），
 * 使配方书能正确展示各自的产物。
 */
public class FlintToolRepairRecipe implements Recipe<CraftingInput> {
    /** 每消耗 1 个燧石恢复的最大耐久比例 */
    public static final float RESTORE_RATIO = 0.25F;

    private final Ingredient tool;
    private final Ingredient filler;
    private final ItemStack result;

    public FlintToolRepairRecipe(Ingredient tool, Ingredient filler, ItemStack result) {
        this.tool = tool;
        this.filler = filler;
        this.result = result;
    }

    public Ingredient getTool() {
        return tool;
    }

    public Ingredient getFiller() {
        return filler;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack toolStack = null;
        int fillers = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (toolStack == null && tool.test(stack) && stack.isDamageableItem() && stack.getDamageValue() > 0) {
                toolStack = stack;
            } else if (filler.test(stack) && stack.getCount() == 1) {
                fillers++;
            } else {
                return false;
            }
        }
        return toolStack != null && fillers == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack toolStack = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && tool.test(stack) && stack.isDamageableItem()) {
                toolStack = stack;
                break;
            }
        }
        if (toolStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = toolStack.copyWithCount(1);
        int restore = Math.max(1, (int) (out.getMaxDamage() * RESTORE_RATIO));
        out.setDamageValue(Math.max(0, out.getDamageValue() - restore));
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FlintGearRegistry.FLINT_REPAIR_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return FlintGearRegistry.FLINT_REPAIR_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<FlintToolRepairRecipe> {
        private static final MapCodec<FlintToolRepairRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("tool").forGetter(FlintToolRepairRecipe::getTool),
                Ingredient.CODEC_NONEMPTY.fieldOf("filler").forGetter(FlintToolRepairRecipe::getFiller),
                ItemStack.CODEC.fieldOf("result").forGetter(r -> r.result)
        ).apply(inst, FlintToolRepairRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, FlintToolRepairRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, FlintToolRepairRecipe::getTool,
                        Ingredient.CONTENTS_STREAM_CODEC, FlintToolRepairRecipe::getFiller,
                        ItemStack.STREAM_CODEC, r -> r.result,
                        FlintToolRepairRecipe::new);

        @Override
        public MapCodec<FlintToolRepairRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FlintToolRepairRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
