package com.createmeow.cm_plugins;

import com.createmeow.currency_plugin.cap.CurrencyHolder;
import com.createmeow.currency_plugin.currency.Currency;
import com.createmeow.currency_plugin.item.CoinItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 自动吸收钱币：每秒自动将玩家周围 1 格内掉落的
 * 腐空朽 / 归霜升 存入玩家的钱包余额（不改换货币种类）。
 * <p>按玩家独立开关（默认开启），可由 {@code /autocoin} 或「实用功能」面板切换。</p>
 */
public class PlayerCoinConsolidationHandler {

    /** 吸收周期（tick）：每秒一次。 */
    private static final int ABSORB_INTERVAL_TICKS = 20;

    /** 吸收半径：玩家包围盒向外扩展的格数。 */
    private static final double ABSORB_RADIUS = 1.0;

    /** 关闭自动吸收的玩家 UUID（默认所有玩家开启）。 */
    private static final Set<UUID> AUTO_ABSORB_DISABLED = new HashSet<>();

    public static boolean isAutoConsolidate(UUID uuid) {
        return !AUTO_ABSORB_DISABLED.contains(uuid);
    }

    /** 切换指定玩家的自动吸收开关，返回切换后的状态。 */
    public static boolean toggleAutoConsolidate(UUID uuid) {
        if (!AUTO_ABSORB_DISABLED.remove(uuid)) {
            AUTO_ABSORB_DISABLED.add(uuid);
        }
        return isAutoConsolidate(uuid);
    }

    /** 每秒扫描玩家周围 1 格内的钱币掉落物，存入钱包余额。 */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isAutoConsolidate(player.getUUID())) return;
        if (player.tickCount % ABSORB_INTERVAL_TICKS != 0) return;

        try {
            absorbNearbyCoins(player);
        } catch (Exception e) {
            createmeowsplugins.LOGGER.error("[AutoCoinAbsorb] 吸收货币时出错", e);
        }
    }

    /** 将玩家周围 1 格内的钱币物品实体存入钱包余额并移除。 */
    private static void absorbNearbyCoins(ServerPlayer player) {
        var items = player.level().getEntitiesOfClass(ItemEntity.class,
                player.getBoundingBox().inflate(ABSORB_RADIUS),
                e -> e.isAlive() && !e.getItem().isEmpty() && e.getItem().getItem() instanceof CoinItem);
        if (items.isEmpty()) return;

        boolean absorbedAny = false;
        for (ItemEntity itemEntity : items) {
            var stack = itemEntity.getItem();
            Currency currency = ((CoinItem) stack.getItem()).currency;
            CurrencyHolder.addBalance(player, currency, stack.getCount());
            itemEntity.discard();
            absorbedAny = true;
        }
        if (absorbedAny) {
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4f, 1.0f);
            createmeowsplugins.LOGGER.debug("[AutoCoinAbsorb] 玩家 {} 吸收了周围的钱币",
                    player.getName().getString());
        }
    }
}
