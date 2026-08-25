package io.github.createmeow.timex_rebirth.mixin;

import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import net.createmod.catnip.data.Pair;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

/**
 * 补全 ThirstWasTaken 缺失的"水瓶 → 水"纯净度转换。
 * <p>ThirstWasTaken 只 hook 了水桶路径（MixinFluidBucketWrapper：桶物品 → 流体时携带纯净度），
 * 但玩家把可堆叠纯净水瓶（water potion）倒进 Create 盆地时走
 * {@code PotionFluidHandler.emptyPotion}，返回的水 FluidStack 纯净度丢失，
 * 导致加热搅拌产出的草药茶液纯净度为 -1——表现为"被赋予了纯净度但没有继承输入水的纯净度"。
 * <p>这里在 Create 通用倒空入口 {@code GenericItemEmptying.emptyItem} 的返回处：
 * 原物品带纯净度 → 补到倒出的流体上（幂等：流体已有纯净度则跳过，不影响水桶路径）。
 */
@Mixin(value = GenericItemEmptying.class, remap = false)
public class GenericItemEmptyingMixin {

    private static Method HAS_PURITY_ITEM;
    private static Method HAS_PURITY_FLUID;
    private static Method GET_PURITY_ITEM;
    private static Method ADD_PURITY_FLUID;
    private static boolean thirstChecked = false;

    private static void initThirst() {
        if (thirstChecked)
            return;
        thirstChecked = true;
        try {
            Class<?> wp = Class.forName("dev.ghen.thirst.content.purity.WaterPurity");
            HAS_PURITY_ITEM = wp.getMethod("hasPurity", ItemStack.class);
            HAS_PURITY_FLUID = wp.getMethod("hasPurity", FluidStack.class);
            GET_PURITY_ITEM = wp.getMethod("getPurity", ItemStack.class);
            ADD_PURITY_FLUID = wp.getMethod("addPurity", FluidStack.class, int.class);
        } catch (Exception ignored) {
            // ThirstWasTaken 未安装：无需纯净度联动
        }
    }

    @Inject(method = "emptyItem", at = @At("RETURN"), cancellable = true)
    private static void timex_rebirth$transferPurity(Level level, ItemStack stack, boolean simulate,
                                                     CallbackInfoReturnable<Pair<FluidStack, ItemStack>> cir) {
        Pair<FluidStack, ItemStack> result = cir.getReturnValue();
        if (result == null || result.getFirst() == null || result.getFirst().isEmpty())
            return;
        initThirst();
        if (HAS_PURITY_ITEM == null)
            return;
        try {
            if (!(Boolean) HAS_PURITY_ITEM.invoke(null, stack))
                return;
            FluidStack fluid = result.getFirst();
            if ((Boolean) HAS_PURITY_FLUID.invoke(null, fluid))
                return; // 流体已带纯净度（如水桶路径），幂等跳过
            int purity = (Integer) GET_PURITY_ITEM.invoke(null, stack);
            ADD_PURITY_FLUID.invoke(null, fluid, purity);
        } catch (Exception ignored) {
        }
    }
}
