package io.github.createmeow.timex_rebirth.mixin;

import io.github.createmeow.timex_rebirth.compat.FiahiCompatHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * fiahi 联动：种子腐烂/冻结 → 枯萎的灌木（玩家背包/装备/副手/末影箱）。
 * 与原版 FIAHI 一样在 {@link Inventory#tick} 末尾遍历所有 compartment，
 * 当种子温度达到 ±100（腐烂或冻结临界）时，把它替换为原版枯死的灌木 minecraft:dead_bush。
 * 阈值 ±100 早于 FIAHI 自身的转换（腐烂&gt;120 / 冻结 level3），避免与剩菜转换冲突。
 */
@Mixin(Inventory.class)
public abstract class InventorySeedWitherMixin {

    @Shadow @Final
    private List<NonNullList<ItemStack>> compartments;

    @Inject(method = "tick", at = @At("TAIL"))
    private void timex_rebirth$witherFrozenSeed(CallbackInfo ci) {
        for (NonNullList<ItemStack> list : this.compartments) {
            for (int i = 0; i < list.size(); i++) {
                ItemStack food = list.get(i);
                if (food.isEmpty()) continue;
                final int index = i;
                FiahiCompatHelper.witherFrozenSeed(food, itemStack -> list.set(index, itemStack));
            }
        }
    }
}
