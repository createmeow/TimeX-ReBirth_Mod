package io.github.createmeow.timex_rebirth.compat;

import com.hexagram2021.fiahi.common.item.capability.IFrozenRottenItemStack;
import com.hexagram2021.fiahi.register.FIAHICapabilities;
import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * 为"粮食/种子/树苗"补注册 fiahi 的温度 capability。
 * <p>
 * fiahi 默认只为"带食物组件"的物品注册 FOOD_CAPABILITY（见 FIAHICapabilities），
 * 缺少食物组件的粮食/种子/树苗无法被 fiahi 的温度 tick 处理，因而不会升温/降温，
 * 其温度组件也不会被 fiahi 持久化维护。这里把本模组管理的粮食/种子/树苗物品
 * 也注册到同一条 capability 上（复用 fiahi 的温度公式与腐烂/冻结/持久化逻辑），
 * 使其同样具备温度属性。在 TimeX 构造器中通过 modEventBus.addListener 挂载。
 */
public class FiahiSeedCapabilityRegistration {

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        if (!FiahiCompatHelper.isLoaded()) return;
        ItemLike[] managed = BuiltInRegistries.ITEM.stream()
                .filter(item -> FiahiCompatHelper.isPerishableGrain(item) || FiahiCompatHelper.isSeedLike(item))
                .toArray(ItemLike[]::new);
        event.registerItem(
                FIAHICapabilities.FOOD_CAPABILITY,
                (itemStack, ignored) -> ((IFrozenRottenItemStack) (Object) itemStack).fiahi$getFrozenRottenFood(),
                managed);
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(FiahiSeedCapabilityRegistration::onRegisterCapabilities);
    }
}
