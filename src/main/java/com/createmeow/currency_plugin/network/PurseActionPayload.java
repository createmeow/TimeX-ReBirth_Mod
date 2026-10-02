package com.createmeow.currency_plugin.network;

import com.createmeow.currency_plugin.CurrencyPlugin;
import com.createmeow.currency_plugin.cap.CurrencyHolder;
import com.createmeow.currency_plugin.currency.Currency;
import com.createmeow.currency_plugin.item.CoinItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.EnumMap;
import java.util.Map;

/**
 * C2S：客户端请求从钱包存入/取出钱币。
 * <ul>
 *   <li>{@link Action#STORE_ALL} — 将背包中所有钱币按种类存入对应余额</li>
 *   <li>{@link Action#EXTRACT} — 取出指定枚数的指定货币到背包</li>
 *   <li>{@link Action#EXTRACT_ALL} — 取出指定货币的全部余额</li>
 * </ul>
 * <p><b>腐空朽与归霜升互不转换</b>：存什么货币就进哪个余额，取也只取该余额。</p>
 */
public record PurseActionPayload(Action action, long value, Currency currency) implements CustomPacketPayload {

    public enum Action {
        STORE_ALL,
        EXTRACT,
        EXTRACT_ALL
    }

    public static final Type<PurseActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CurrencyPlugin.MODID, "purse_action"));

    public static final StreamCodec<ByteBuf, PurseActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.idMapper(i -> Action.values()[i], Action::ordinal),
                    PurseActionPayload::action,
                    ByteBufCodecs.VAR_LONG,
                    PurseActionPayload::value,
                    ByteBufCodecs.idMapper(i -> Currency.values()[i], Currency::ordinal),
                    PurseActionPayload::currency,
                    PurseActionPayload::new
            );

    public static PurseActionPayload storeAll() {
        return new PurseActionPayload(Action.STORE_ALL, 0L, Currency.COMMON);
    }

    /** @param amount 指定货币的枚数 */
    public static PurseActionPayload extract(Currency currency, long amount) {
        return new PurseActionPayload(Action.EXTRACT, amount, currency);
    }

    public static PurseActionPayload extractAll(Currency currency) {
        return new PurseActionPayload(Action.EXTRACT_ALL, 0L, currency);
    }

    public static void handle(PurseActionPayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        if (!(player.containerMenu instanceof InventoryMenu)) return;

        context.enqueueWork(() -> {
            switch (payload.action()) {
                case STORE_ALL -> storeAll(player);
                case EXTRACT -> extract(player, payload.currency(), payload.value());
                case EXTRACT_ALL -> extract(player, payload.currency(),
                        CurrencyHolder.getBalance(player, payload.currency()));
            }
        });
    }

    /** 将背包中所有钱币按种类存入对应余额（不换算）。 */
    private static void storeAll(ServerPlayer player) {
        var inventory = player.getInventory();
        Map<Currency, Long> totals = new EnumMap<>(Currency.class);
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof CoinItem coinItem) {
                totals.merge(coinItem.currency, (long) stack.getCount(), Long::sum);
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
        if (totals.isEmpty()) return;

        MutableComponent message = net.minecraft.network.chat.Component.literal("\u00A7a+ \u00A77[");
        boolean first = true;
        for (var entry : totals.entrySet()) {
            CurrencyHolder.addBalance(player, entry.getKey(), entry.getValue());
            if (!first) message.append(", ");
            first = false;
            message.append("\u00A7b" + entry.getValue() + " ");
            message.append(entry.getKey().displayName());
        }
        message.append("\u00A77]");
        player.displayClientMessage(message, true);
    }

    /**
     * 取出指定货币的指定枚数到背包（请求超过余额时钳制到全部余额）。
     * 只取出该货币本身，不换算成另一种货币。
     */
    private static void extract(ServerPlayer player, Currency currency, long amount) {
        long balance = CurrencyHolder.getBalance(player, currency);
        long actual = Math.min(amount, balance);
        if (actual <= 0) return;

        long remain = actual;
        while (remain > 0) {
            int size = (int) Math.min(remain, 99);
            player.getInventory().placeItemBackInInventory(new ItemStack(currency.asItem(), size));
            remain -= size;
        }
        CurrencyHolder.setBalance(player, currency, balance - actual);
    }

    @Override
    public Type<? extends PurseActionPayload> type() {
        return TYPE;
    }
}
