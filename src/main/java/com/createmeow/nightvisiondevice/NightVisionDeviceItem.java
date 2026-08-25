package com.createmeow.nightvisiondevice;

import java.util.EnumMap;
import java.util.List;

import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class NightVisionDeviceItem extends ArmorItem {

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, createmeow.MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> NIGHT_VISION =
            ARMOR_MATERIALS.register("night_vision",
                    () -> new ArmorMaterial(
                            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                                map.put(ArmorItem.Type.BOOTS, 2);
                                map.put(ArmorItem.Type.LEGGINGS, 2);
                                map.put(ArmorItem.Type.CHESTPLATE, 2);
                                map.put(ArmorItem.Type.HELMET, 2);
                                map.put(ArmorItem.Type.BODY, 2);
                            }),
                            15,
                            SoundEvents.ARMOR_EQUIP_IRON,
                            () -> Ingredient.EMPTY,
                            List.of(new ArmorMaterial.Layer(
                                    ResourceLocation.fromNamespaceAndPath(createmeow.MODID, "night_vision_device"))),
                            0.0F,
                            0.0F));

    public NightVisionDeviceItem(Properties properties) {
        super(NIGHT_VISION, ArmorItem.Type.HELMET, properties);
    }
}