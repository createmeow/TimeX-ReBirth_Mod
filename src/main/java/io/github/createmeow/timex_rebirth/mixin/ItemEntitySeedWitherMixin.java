package io.github.createmeow.timex_rebirth.mixin;

import io.github.createmeow.timex_rebirth.compat.FiahiCompatHelper;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * fiahi 联动：种子腐烂/冻结 → 枯萎的灌木（掉落地上的种子）。
 * 与原版 FIAHI 一样在 {@link ItemEntity#tick} 的实体 tick 之后检查，
 * 当种子温度达到 ±100（腐烂或冻结临界）时，把它替换为原版枯死的灌木 minecraft:dead_bush。
 * 阈值 ±100 早于 FIAHI 自身的转换（腐烂&gt;120 / 冻结 level3），避免与剩菜转换冲突。
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntitySeedWitherMixin {

    @Inject(method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V", shift = At.Shift.AFTER))
    private void timex_rebirth$witherFrozenSeed(CallbackInfo ci) {
        ItemEntity current = (ItemEntity) (Object) this;
        if (current.level().isClientSide) return;
        FiahiCompatHelper.witherFrozenSeed(current.getItem(), current::setItem);
    }
}
