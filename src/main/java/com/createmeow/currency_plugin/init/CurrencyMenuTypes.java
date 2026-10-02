package com.createmeow.currency_plugin.init;

import com.createmeow.currency_plugin.CurrencyPlugin;
import com.createmeow.currency_plugin.machine.RefinerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 菜单类型注册：提炼机。
 */
public class CurrencyMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, CurrencyPlugin.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<RefinerMenu>> REFINER_MENU =
            MENU_TYPES.register("refining_machine",
                    () -> new MenuType<>((id, inv) -> new RefinerMenu(id, inv),
                            net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
}
