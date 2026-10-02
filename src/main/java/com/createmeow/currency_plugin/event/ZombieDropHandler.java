package com.createmeow.currency_plugin.event;

import com.createmeow.currency_plugin.init.CurrencyItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 僵尸掉落：僵尸死亡时 0.5% 概率额外掉落 1 个腐空朽（普通钱币）。
 */
public class ZombieDropHandler {

    private static final double DROP_CHANCE = 0.005;

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (event.getEntity().getType() != EntityType.ZOMBIE) return;
        if (ThreadLocalRandom.current().nextDouble() >= DROP_CHANCE) return;

        var drops = event.getDrops();
        drops.add(new ItemEntity(
                event.getEntity().level(),
                event.getEntity().getX(),
                event.getEntity().getY() + 0.5,
                event.getEntity().getZ(),
                new ItemStack(CurrencyItems.COMMON_COIN.get())));
    }
}
