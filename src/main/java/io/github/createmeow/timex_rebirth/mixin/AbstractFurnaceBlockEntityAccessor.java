package io.github.createmeow.timex_rebirth.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 熔炉 BE 成员访问器：litTime 剩余燃烧 tick（状态显示）、getBurnDuration（Redirect 内调用）。
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public interface AbstractFurnaceBlockEntityAccessor {

    @Accessor("litTime")
    int timex_rebirth$getLitTime();

    @Invoker("getBurnDuration")
    int timex_rebirth$invokeGetBurnDuration(ItemStack fuel);
}
