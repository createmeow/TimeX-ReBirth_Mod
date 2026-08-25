package com.createmeow.nightvisiondevice;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(createmeow.MODID)
public class createmeow {
    public static final String MODID = "night_vision_device";
    public static final Logger LOGGER = LogUtils.getLogger();

    public createmeow(IEventBus modEventBus, ModContainer modContainer) {
        NVItems.register(modEventBus);
    }
}