package com.createmeow.underwaterplugin;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 铝背罐水下供氧：完整复刻 Create DivingHelmetItem.breatheUnderwater 的成熟写法：
 * <ul>
 * <li>胸甲槽穿铝背罐即可供氧（无需任何头盔）；</li>
 * <li>头戴防寒面罩 + 任意有气背罐（铜/下界合金/铝）也可供氧——面罩替代 Create 潜水头盔，
 *     Create 自家 handler 只认潜水头盔，此组合须由本 handler 补位；</li>
 * <li>HUD 剩余时间数据由 {@link #onClientPlayerTick}（PlayerTickEvent.Post 客户端）写入，
 *     时序必晚于 Create 的 breathe handler（baseTick 内），不会被其无条件 remove 删掉</li>
 * <li>不检查眼睛是否入水——依赖 LivingBreatheEvent 默认值：陆地/水肺等场景
 *     canBreathe 已为 true 直接跳过（Create 原版同款判定）</li>
 * <li>岩浆中不支持供氧（无 FIRE_RESISTANT 组件的背罐；下界合金背罐自带防火组件可用）</li>
 * <li>每秒从存量最少的有气背罐扣 1 点空气（BacktankUtil.getAllWithAir 升序排列；
 *     铝背罐已加入 create:pressurized_air_sources 标签，与铜/下界合金背罐统一调度，
 *     多背罐时优先耗尽存量最少的）</li>
 * <li>供氧时 canBreathe=true + 原版氧气条顶满（与 Create 头盔同款 setRefillAirAmount）</li>
 * </ul>
 * 与 Create 潜水头盔同穿时双方共享同一事件：先触发者扣气并置 canBreathe，
 * 后到者在 canBreathe 检查处跳过，不会重复消耗。
 * <p>
 * 防寒功能（体温恢复/暴雪雾效豁免）仍为"面罩 + 铝背罐"专属，
 * 见 {@link ColdGearHeatRestoreHandler} 与 BlizzardClientEvents。
 */
@EventBusSubscriber(modid = UnderwaterPlugin.MODID)
public final class BacktankBreatheHandler {

    private BacktankBreatheHandler() {}

    @SubscribeEvent
    public static void breatheUnderwater(LivingBreatheEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();

        // Create 未加载时铝背罐物品根本不存在；此处须先于 instanceof 检查，
        // 避免 AluminumBacktankItem 类加载连带解析其 Create 父类导致 NoClassDefFoundError
        if (!UnderwaterCreateRegisters.createLoaded())
            return;

        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        boolean aluminumWorn = chest.getItem() instanceof AluminumBacktankItem;
        // 防寒面罩 + 任意背罐（铜/下界合金/铝）也应可潜水：
        // Create 自家头盔逻辑要求头戴潜水头盔，面罩替代头盔时必须由本 handler 补位
        boolean frostMaskWorn = entity.getItemBySlot(EquipmentSlot.HEAD)
                .is(UnderwaterRegisters.FROST_MASK.get());
        if (!aluminumWorn && !frostMaskWorn)
            return;

        boolean lavaDiving = entity.isInLava();
        // 无防火组件的背罐不支持岩浆潜水（铝/铜一致；下界合金背罐自带防火组件可用）
        if (!chest.has(DataComponents.FIRE_RESISTANT) && lavaDiving)
            return;

        List<ItemStack> backtanks = com.simibubi.create.content.equipment.armor.BacktankUtil.getAllWithAir(entity);
        if (backtanks.isEmpty())
            return;

        // 已可呼吸（陆地/水肺药水/Create 头盔等其他供氧源）则不重复供氧
        if (event.canBreathe())
            return;

        if (level.getGameTime() % 20 == 0)
            com.simibubi.create.content.equipment.armor.BacktankUtil.consumeAir(entity, backtanks.get(0), 1);

        event.setCanBreathe(true);
        event.setRefillAirAmount(entity.getMaxAirSupply());
    }

    /**
     * HUD 剩余时间数据写入（仅客户端）。
     * <p>
     * 不再依赖 LivingBreatheEvent 写入——Create 的 DivingHelmetItem.breatheUnderwater
     * 开头<b>无条件 remove("VisualBacktankAir")</b>，两 mod 的监听器执行顺序取决于注册顺序，
     * 若我们先执行则数据被 Create 删掉（HUD 永不显示的根因）。
     * 改用 {@link PlayerTickEvent.Post}：触发点在 Player.tick 尾部，必然晚于
     * LivingEntity.baseTick 中的 LivingBreatheEvent，写入不会被 Create 删除。
     * <p>
     * 只穿铝背罐时每 tick 覆盖写入；脱下铝背罐且未戴 Create 头盔时清掉残留数据
     * （戴 Create 头盔时不动——其数据由 Create 自己每 tick 管理）。
     * RemainingAirOverlay 自身还有 canDrown/isAir 检查兜底，多写无副作用。
     */
    @SubscribeEvent
    public static void onClientPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()) return;
        if (!UnderwaterCreateRegisters.createLoaded()) return;

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean aluminumWorn = chest.getItem() instanceof AluminumBacktankItem;
        boolean frostMaskWorn = player.getItemBySlot(EquipmentSlot.HEAD)
                .is(UnderwaterRegisters.FROST_MASK.get());
        if (!aluminumWorn && !frostMaskWorn) {
            // 铝背罐/面罩都不在身：清理本 handler 写入的残留（Create 头盔场景交给 Create 管理）
            if (!com.simibubi.create.content.equipment.armor.DivingHelmetItem.isWornBy(player)) {
                player.getPersistentData().remove("VisualBacktankAir");
            }
            return;
        }

        java.util.List<ItemStack> backtanks =
                com.simibubi.create.content.equipment.armor.BacktankUtil.getAllWithAir(player);
        if (backtanks.isEmpty()) return;

        float visualBacktankAir = 0f;
        for (ItemStack stack : backtanks)
            visualBacktankAir += com.simibubi.create.content.equipment.armor.BacktankUtil.getAir(stack);
        player.getPersistentData().putInt("VisualBacktankAir", Math.round(visualBacktankAir));
    }
}
