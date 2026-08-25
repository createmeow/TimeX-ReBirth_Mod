package io.github.createmeow.timex_rebirth.wasteland;

import io.github.createmeow.timex_rebirth.research.ResearchData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Optional;

/**
 * 绘制台：把废土中捡到的废旧物品等绘制成图纸心得，兑换"阅历"。
 * - 手持可识别物品右键：消耗 1 个并给予阅历（ResearchData.addPoints）；
 * - 手持无法识别的物品右键：提示无法识别；
 * - 空手右键：提示放入物品。
 * 配方为自定义 RecipeType（data/timex_rebirth/recipe/drawing/*.json），
 * 在 JEI / EMI 中以"输入物品 → N 阅历"的形式展示。
 */
public class DrawingTableBlock extends Block {
    public DrawingTableBlock() {
        super(Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.0F)
                .sound(SoundType.WOOD));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return ItemInteractionResult.sidedSuccess(true);
        }
        if (stack.isEmpty()) {
            player.displayClientMessage(Component.translatable("msg.timex_rebirth.drawing_table.empty"), true);
            return ItemInteractionResult.CONSUME;
        }
        Optional<RecipeHolder<DrawingRecipe>> found = level.getRecipeManager()
                .getRecipeFor(WastelandRegistry.DRAWING_TYPE.get(), new SingleRecipeInput(stack), level);
        if (found.isEmpty()) {
            player.displayClientMessage(Component.translatable("msg.timex_rebirth.drawing_table.unknown"), true);
            return ItemInteractionResult.CONSUME;
        }
        DrawingRecipe recipe = found.get().value();
        stack.shrink(1);
        int gained = recipe.rollPoints(player.getRandom());
        ResearchData.addPoints((ServerPlayer) player, gained);
        player.displayClientMessage(Component.translatable(
                "msg.timex_rebirth.drawing_table.success", gained), true);
        level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.2F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                    12, 0.3D, 0.3D, 0.3D, 0.05D);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        player.displayClientMessage(Component.translatable("msg.timex_rebirth.drawing_table.empty"), true);
        return InteractionResult.CONSUME;
    }
}
