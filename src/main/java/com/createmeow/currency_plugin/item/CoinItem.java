package com.createmeow.currency_plugin.item;

import com.createmeow.currency_plugin.cap.CurrencyHolder;
import com.createmeow.currency_plugin.currency.Currency;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 钱币物品：右键使用将整组钱币存入钱包余额。
 * <p>腐空朽（普通钱币）可作为熔炉燃料（50 个物品燃烧时长 = 10000 tick）；
 * Create 烈焰人燃烧室的超级加热燃料由 data map 定义（与烈焰蛋糕相同的 3200 tick）；
 * Cold Sweat 锅炉/壁炉燃料由 datapack 注册表定义（半个岩浆桶）。</p>
 */
public class CoinItem extends Item implements CurrencyItem {
    /** 腐空朽的熔炉燃烧时长（tick）：50 个物品 = 50 × 200。 */
    public static final int FURNACE_BURN_TIME = 10000;

    public final Currency currency;
    public final Style NAME_STYLE;

    public CoinItem(Currency currency) {
        super(new Item.Properties().stacksTo(99));
        this.currency = currency;
        this.NAME_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(currency.getNameColor()));
    }

    @Override
    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
        return this.currency == Currency.COMMON ? FURNACE_BURN_TIME : 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        ItemStack clickedStack = user.getItemInHand(hand);
        int count = clickedStack.getCount();
        if (!world.isClientSide) {
            // 按原货币种类存入对应余额，不换算成其他货币
            CurrencyHolder.addBalance(user, this.currency, count);
            user.displayClientMessage(net.minecraft.network.chat.Component.literal("\u00A7a+ \u00A77[\u00A7b" + count + " ")
                    .append(this.currency.displayName())
                    .append(net.minecraft.network.chat.Component.literal("\u00A77]")), true);
        }
        return InteractionResultHolder.success(ItemStack.EMPTY);
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().setStyle(this.NAME_STYLE);
    }

    @Override
    public Component getDescription() {
        return super.getDescription().copy().setStyle(this.NAME_STYLE);
    }

    // ── CurrencyItem 接口 ──

    @Override
    public boolean wasAdjusted(ItemStack other) {
        return other.getItem() != this;
    }

    @Override
    public long getValue(ItemStack stack) {
        return this.currency.getRawValue(stack.getCount());
    }

    @Override
    public long[] getCombinedValue(ItemStack stack) {
        long[] values = new long[2];
        values[this.currency.ordinal()] = stack.getCount();
        return values;
    }
}
