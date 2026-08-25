package io.github.createmeow.timex_rebirth.mixin;

import io.github.createmeow.timex_rebirth.wasteland.WastelandRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 碗装腐烂食物堆肥返还木碗：
 * 把 timex_rebirth:rotten_meal_bowl / rotten_meat_bowl 放入堆肥桶（被接受为堆肥）时，
 * 消耗腐烂食物的同时返还玩家 1 个木碗（玩家背包优先，背包满则掉落；非玩家触发则掉落在桶旁）。
 * <p>
 * 注入点选在 {@link ComposterBlock#addItem}（所有堆肥入口的公共调用点）：
 * 手动右键走 {@code useItemOn}、漏斗等自动化走 {@code InputContainer.setChanged}、
 * 以及静态 {@code insertItem} 都会经由 {@code addItem} 接受物品，因此统一在此返还木碗。
 * 配合 craftRemainder(碗)，确保碗装菜品腐烂后木碗不会凭空消失。
 */
@Mixin(ComposterBlock.class)
public abstract class ComposterRottenBowlMixin {

    @Inject(method = "addItem",
            at = @At("RETURN"))
    private static void timex_rebirth$compostReturnsBowl(Entity entity, BlockState state, LevelAccessor level,
                                                         BlockPos pos, ItemStack stack,
                                                         CallbackInfoReturnable<BlockState> cir) {
        if (!stack.is(WastelandRegistry.ROTTEN_MEAL_BOWL.get()) && !stack.is(WastelandRegistry.ROTTEN_MEAT_BOWL.get())) {
            return;
        }
        ItemStack bowl = new ItemStack(Items.BOWL);
        if (entity instanceof Player player) {
            if (player.getAbilities().instabuild) return;
            if (!player.getInventory().add(bowl)) {
                player.drop(bowl, false);
            }
        } else if (level instanceof Level realLevel) {
            // addItem 的 level 参数是 LevelAccessor，而 dropItemStack 需要 Level；
            // 实际堆肥场景的 level 基本都是 Level（ServerLevel/ClientLevel）。
            Containers.dropItemStack(realLevel, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, bowl);
        }
    }
}
