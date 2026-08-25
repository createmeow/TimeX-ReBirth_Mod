package io.github.createmeow.timex_rebirth.mixin;

import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import io.github.createmeow.timex_rebirth.food.WastelandFoodRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

/**
 * 让草药茶液可被玻璃瓶直接装取（Create 的 {@code GenericItemFilling} 硬编码
 * 只允许水 / 药水 / create:tea 三种流体用玻璃瓶内部装瓶，其余流体一律拒绝）。
 * <p>仿 ThirstWasTaken {@code MixinGenericItemFilling} 的写法（remap=false，
 * Create 编译/运行时均为官方映射名）：
 * <ul>
 *     <li>{@code canFillGlassBottleInternally}：草药茶 → true（令装瓶量计算返回 250mb）</li>
 *     <li>{@code fillItem}：玻璃瓶 + 草药茶 → 直接产出瓶装草药茶，并<b>自托管</b>把
 *         茶液的纯净度复制到物品（不依赖 ThirstWasTaken 的 RETURN mixin，保证装出后纯净度与盆地内一致）</li>
 * </ul>
 */
@Mixin(value = GenericItemFilling.class, remap = false)
public class GenericItemFillingMixin {

    private static Method HAS_PURITY_FLUID;
    private static Method GET_PURITY_FLUID;
    private static Method ADD_PURITY_ITEM;
    private static boolean thirstChecked = false;

    private static void initThirst() {
        if (thirstChecked)
            return;
        thirstChecked = true;
        try {
            Class<?> wp = Class.forName("dev.ghen.thirst.content.purity.WaterPurity");
            HAS_PURITY_FLUID = wp.getMethod("hasPurity", FluidStack.class);
            GET_PURITY_FLUID = wp.getMethod("getPurity", FluidStack.class);
            ADD_PURITY_ITEM = wp.getMethod("addPurity", ItemStack.class, int.class);
        } catch (Exception ignored) {
            // ThirstWasTaken 未安装：无需纯净度联动
        }
    }

    /** 把草药茶液的纯净度复制到装出的草药茶物品。 */
    private static void copyPurity(FluidStack fluid, ItemStack item) {
        initThirst();
        if (HAS_PURITY_FLUID == null || ADD_PURITY_ITEM == null)
            return;
        try {
            if (!(Boolean) HAS_PURITY_FLUID.invoke(null, fluid))
                return;
            int purity = (Integer) GET_PURITY_FLUID.invoke(null, fluid);
            ADD_PURITY_ITEM.invoke(null, item, purity);
        } catch (Exception ignored) {
        }
    }

    @Inject(method = "canFillGlassBottleInternally", at = @At("RETURN"), cancellable = true)
    private static void timex_rebirth$canFillHerbalTea(FluidStack availableFluid, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && availableFluid.getFluid() == WastelandFoodRegistry.HERBAL_TEA_FLUID.get()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "fillItem", at = @At("HEAD"), cancellable = true)
    private static void timex_rebirth$fillHerbalTea(Level world, int requiredAmount, ItemStack stack,
                                                    FluidStack availableFluid, CallbackInfoReturnable<ItemStack> cir) {
        if (stack.getItem() == Items.GLASS_BOTTLE
                && availableFluid.getFluid() == WastelandFoodRegistry.HERBAL_TEA_FLUID.get()) {
            availableFluid.shrink(requiredAmount);
            stack.shrink(1);
            ItemStack result = new ItemStack(WastelandFoodRegistry.HERBAL_TEA.get());
            copyPurity(availableFluid, result);
            cir.setReturnValue(result);
        }
    }
}
