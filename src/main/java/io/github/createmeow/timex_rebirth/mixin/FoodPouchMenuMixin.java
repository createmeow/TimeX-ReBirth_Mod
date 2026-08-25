package io.github.createmeow.timex_rebirth.mixin;

import com.hexagram2021.fiahi.common.item.capability.impl.FoodPouchData;
import com.hexagram2021.fiahi.common.item.data.PouchedFoodKey;
import com.hexagram2021.fiahi.common.menu.FoodPouchMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 修复 fiahi 食物袋超限堆叠导致的存档崩溃（fiahi 自身 bug）：
 * 食物袋每类食物允许堆叠到 1024（MAX_FOOD_COUNT），但 ItemStack 持久化
 * 编码的 count 上限为 99。保存（getContent）时把 count>99 的堆叠拆分为
 * 多组（每组 ≤99），读取（setContent）时按物品合并回原始数量，数据不丢失。
 */
@Mixin(FoodPouchMenu.class)
public abstract class FoodPouchMenuMixin {

    /** ItemStack 持久化编码的 count 上限（1.21.1 为 [1;99]）。 */
    private static final int MAX_SAVE_COUNT = 99;

    @Shadow
    @Final
    private Map<PouchedFoodKey, Integer> stackedItems;

    @Shadow
    private void maintainItems() {
    }

    @Inject(method = "getContent", at = @At("RETURN"), cancellable = true)
    private void timex_rebirth$splitOverstacked(CallbackInfoReturnable<FoodPouchData> cir) {
        FoodPouchData original = cir.getReturnValue();
        boolean needsSplit = original.items().stream().anyMatch(s -> s.getCount() > MAX_SAVE_COUNT);
        if (!needsSplit) return;
        List<ItemStack> split = new ArrayList<>();
        for (ItemStack stack : original.items()) {
            int count = stack.getCount();
            while (count > 0) {
                int take = Math.min(count, MAX_SAVE_COUNT);
                ItemStack copy = stack.copy();
                copy.setCount(take);
                split.add(copy);
                count -= take;
            }
        }
        cir.setReturnValue(new FoodPouchData(original.temperature(), split));
    }

    @Inject(method = "setContent", at = @At("HEAD"), cancellable = true)
    private void timex_rebirth$mergeStacked(FoodPouchData content, CallbackInfo ci) {
        // 合并同一物品的多组堆叠（由拆分产生），恢复原始数量
        Map<PouchedFoodKey, Integer> merged = new HashMap<>();
        for (ItemStack stack : content.items()) {
            merged.merge(FoodPouchMenu.getKeyFromItem(stack), stack.getCount(), Integer::sum);
        }
        this.stackedItems.clear();
        this.stackedItems.putAll(merged);
        this.maintainItems();
        ((FoodPouchMenu) (Object) this).setTemperature(content.temperature());
        ci.cancel();
    }
}
