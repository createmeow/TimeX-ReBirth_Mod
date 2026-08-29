package com.createmeow.nightvisiondevice;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class NVItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(createmeow.MODID);

    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, createmeow.MODID);

    public static final DeferredItem<NightVisionDeviceItem> NIGHT_VISION_DEVICE = ITEMS.register("night_vision_device",
            () -> new NightVisionDeviceItem(new NightVisionDeviceItem.Properties()
                    .stacksTo(1)
                    .durability(300)
                    .rarity(Rarity.EPIC)));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> NO_GREEN =
            DATA_COMPONENTS.register("no_green", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
        ITEMS.register(eventBus);
    }
}