package com.createmeow.nightvisiondevice;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(modid = createmeow.MODID)
public class NVEventHandler {
    private static final Set<Player> playersWithNV = new HashSet<>();

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            EquipmentSlot slot = event.getSlot();
            if (slot == EquipmentSlot.HEAD) {
                ItemStack oldStack = event.getFrom();
                ItemStack newStack = event.getTo();

                boolean wasNV = oldStack.getItem() instanceof NightVisionDeviceItem;
                boolean isNV = newStack.getItem() instanceof NightVisionDeviceItem;

                if (wasNV && !isNV) {
                    removeNightVisionEffect(player);
                    playersWithNV.remove(player);
                } else if (!wasNV && isNV) {
                    playersWithNV.add(player);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (playersWithNV.contains(player)) {
            if (player.tickCount % 20 == 0) {
                addNightVisionEffect(player);
            }
        }
    }

    private static void addNightVisionEffect(Player player) {
        player.removeEffect(MobEffects.NIGHT_VISION);
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 232, false, false, true));
    }

    private static void removeNightVisionEffect(Player player) {
        player.removeEffect(MobEffects.NIGHT_VISION);
    }
}