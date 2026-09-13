package com.createmeow.underwaterplugin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Create（机械动力）集成层。
 * 所有对 Create API 的引用都集中在此类，仅当 Create 已加载时才会被类加载，
 * 避免 Create 缺席时的 NoClassDefFoundError。
 */
public final class CreateIntegration {

    private CreateIntegration() {}

    public static boolean isAvailable() {
        return net.neoforged.fml.ModList.get().isLoaded("create");
    }

    /**
     * 玩家是否正由背罐空气供氧（过深耗氧惩罚作用于背罐而非原版氧气）：
     * - 胸甲槽铝背罐且有气（BacktankBreatheHandler 供氧中），或
     * - 戴着 Create 潜水头盔（任意材质）且有带剩余空气的背罐。
     */
    public static boolean hasOxygenSupply(Player player) {
        ItemStack chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (chest.getItem() instanceof AluminumBacktankItem
                && com.simibubi.create.content.equipment.armor.BacktankUtil.getAir(chest) > 0) {
            return true;
        }
        if (!com.simibubi.create.content.equipment.armor.DivingHelmetItem.isWornBy(player)) return false;
        return !com.simibubi.create.content.equipment.armor.BacktankUtil.getAllWithAir(player).isEmpty();
    }

    /**
     * 额外消耗背罐空气（供过深耗氧惩罚使用）。
     * 走 Create 官方 API，保留其低气量/耗尽的标题警告。
     */
    public static void consumeExtraAir(Player player, int amount) {
        List<ItemStack> backtanks = com.simibubi.create.content.equipment.armor.BacktankUtil.getAllWithAir(player);
        if (backtanks.isEmpty()) return;
        com.simibubi.create.content.equipment.armor.BacktankUtil.consumeAir(player, backtanks.get(0), amount);
    }
}
