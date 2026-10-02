package com.createmeow.currency_plugin.cap;

import com.createmeow.currency_plugin.currency.Currency;
import com.createmeow.currency_plugin.init.CurrencyAttachments;
import com.createmeow.currency_plugin.network.UpdateCurrencyPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 玩家货币余额的读写入口。
 * <p>腐空朽 / 归霜升 各自独立余额（枚数），<b>禁止任何形式的互相转换</b>：
 * 存入腐空朽只能取出腐空朽，存入归霜升只能取出归霜升。
 * 通过 {@link UpdateCurrencyPayload} 将两个余额同步到客户端。</p>
 */
public class CurrencyHolder {

    /** 读取指定货币的余额（枚数）。 */
    public static long getBalance(Player player, Currency currency) {
        return player.getData(attachmentOf(currency));
    }

    /** 设置指定货币的余额（枚数），并同步到客户端。 */
    public static void setBalance(Player player, Currency currency, long count) {
        player.setData(attachmentOf(currency), Math.max(0, count));
        sync(player);
    }

    /** 增减指定货币的余额（枚数），并同步到客户端。结果不会小于 0。 */
    public static void addBalance(Player player, Currency currency, long count) {
        long balance = getBalance(player, currency);
        setBalance(player, currency, balance + count);
    }

    /**
     * 按总值消耗（用于系统扣费）：先扣腐空朽，再扣归霜升，不找零。
     *
     * @return 总值足够则扣款成功返回 true，否则不动余额返回 false
     */
    public static boolean spend(Player player, long rawCost) {
        if (rawCost <= 0) return true;
        long common = getBalance(player, Currency.COMMON);
        long rare = getBalance(player, Currency.RARE);
        long total = common + rare * Currency.RARE_VALUE;
        if (total < rawCost) return false;

        long takeCommon = Math.min(common, rawCost);
        long remain = rawCost - takeCommon;
        long takeRare = (remain + Currency.RARE_VALUE - 1) / Currency.RARE_VALUE;
        player.setData(CurrencyAttachments.COMMON_VALUE.get(), common - takeCommon);
        player.setData(CurrencyAttachments.RARE_VALUE.get(), rare - takeRare);
        sync(player);
        return true;
    }

    /** 将服务端当前两种余额同步到客户端（登录/重生/维度切换时使用）。 */
    public static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new UpdateCurrencyPayload(
                    getBalance(serverPlayer, Currency.COMMON),
                    getBalance(serverPlayer, Currency.RARE)));
        }
    }

    private static net.neoforged.neoforge.attachment.AttachmentType<Long> attachmentOf(Currency currency) {
        return currency == Currency.COMMON
                ? CurrencyAttachments.COMMON_VALUE.get()
                : CurrencyAttachments.RARE_VALUE.get();
    }
}
