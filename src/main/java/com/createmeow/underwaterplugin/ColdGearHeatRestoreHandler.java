package com.createmeow.underwaterplugin;

import com.momosoftworks.coldsweat.api.util.Temperature;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 防寒全套体温维持：
 * 防寒面罩 + 防寒护腿 + 防寒靴子 + <b>铝背罐</b>（胸甲槽）全穿戴时，
 * 每秒计算（中性体温值 0 − 当前体温）的差值，若为正数则恢复该差值的体温，
 * 并从铝背罐消耗（差值 / 4 向上取整）点压缩空气（4 点体温 : 1 点空气，受存量上限约束）。
 * <p>
 * <b>防寒功能为铝背罐专属</b>——面罩配铜/下界合金背罐只提供潜水功能（水下视物/供氧），
 * 不维持体温（空气耗尽后失去保护）。
 */
@EventBusSubscriber(modid = UnderwaterPlugin.MODID)
public final class ColdGearHeatRestoreHandler {

    private ColdGearHeatRestoreHandler() {}

    /** 每秒结算一次 */
    private static final int INTERVAL_TICKS = 20;

    /** Cold Sweat CORE 维度的中性体温值 */
    private static final double NEUTRAL_CORE = 0.0;

    /** 每 1 点压缩空气可恢复的体温（差值消耗 = ceil(差值 / 该值)） */
    private static final double TEMP_PER_AIR = 4.0;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (player.isSpectator() || player.isCreative()) return;
        if (player.tickCount % INTERVAL_TICKS != 0) return;

        if (!ColdSweatIntegration.isAvailable() || !UnderwaterCreateRegisters.createLoaded()) return;

        if (!isFullColdGearSet(player)) return;

        double core = Temperature.get(player, Temperature.Trait.CORE);
        double deficit = NEUTRAL_CORE - core;
        if (deficit <= 0) return;

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        int air = com.simibubi.create.content.equipment.armor.BacktankUtil.getAir(chest);
        if (air <= 0) return;

        double restore = Math.min(deficit, air * TEMP_PER_AIR);
        int cost = Mth.clamp(Mth.ceil(restore / TEMP_PER_AIR), 1, air);
        com.simibubi.create.content.equipment.armor.BacktankUtil.consumeAir(player, chest, cost);

        Temperature.add(player, Temperature.Trait.CORE, restore);
    }

    /** 防寒面罩 + 护腿 + 靴子 + 铝背罐（防寒功能铝背罐专属；其他背罐仅潜水功能） */
    private static boolean isFullColdGearSet(Player player) {
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(UnderwaterRegisters.FROST_MASK.get())) return false;
        if (!player.getItemBySlot(EquipmentSlot.LEGS).is(UnderwaterRegisters.FROST_LEGGINGS.get())) return false;
        if (!player.getItemBySlot(EquipmentSlot.FEET).is(UnderwaterRegisters.FROST_BOOTS.get())) return false;
        return player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof AluminumBacktankItem;
    }
}
