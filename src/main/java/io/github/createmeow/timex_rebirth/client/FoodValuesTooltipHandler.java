package io.github.createmeow.timex_rebirth.client;

import dev.ghen.thirst.api.ThirstHelper;
import dev.anye.mc.reality_value.cap.PlayerExCap;
import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 食物 tooltip 整理：将 fiahi 温度行与饥饿/饱和/口渴/解渴符号行插入到物品名之后，
 * 而非追加到 tooltip 末尾（F3+H 高级信息之后），避免顺序混乱。
 * <p>使用 {@link EventPriority#LOW} 确保在 fiahi（NORMAL）之后执行：
 * <ol>
 *   <li>从 tooltip 末尾提取并移除 fiahi 添加的温度状态行与温度值行，合并为 {@code 状态|温度N}</li>
 *   <li>生成饥饿/饱和/口渴/解渴符号行</li>
 *   <li>将两行插入到物品名之后（index 1），使顺序为：物品名 → 温度 → 食物值 → F3+H 信息</li>
 * </ol>
 * <p>AppleSkin 的图形饥饿/饱和度 tooltip 与 ThirstWasTaken 的图形口渴/解渴度 tooltip
 * 分别由 {@link io.github.createmeow.timex_rebirth.mixin.AppleSkinTooltipMixin} 和
 * {@link io.github.createmeow.timex_rebirth.mixin.ThirstTooltipMixin} 取消。
 */
@EventBusSubscriber(modid = TimeX.MODID, value = Dist.CLIENT)
public class FoodValuesTooltipHandler {

    // TimeX-Fontpark 自定义字体符号（default.json 中的位图映射）
    private static final String ICON_HUNGER = "\uEC03";       // hungry.png
    private static final String ICON_SATURATION = "\uEC04";   // hungry_none.png
    private static final String ICON_THIRST = "\uEC05";       // thirsty.png
    private static final String ICON_QUENCHED = "\uEC06";     // thirsty_none.png
    private static final String ICON_HEALTHY = "\uEC07";      // healthy.png
    private static final String ICON_IMMUNITY = "\uEC08";     // healthy_none.png
    private static final String ICON_SANITY = "\uEC09";       // sanity.png
    private static final String ICON_ENERGY = "\uEC0A";       // sanity_none.png

    private static volatile boolean thirstChecked;
    private static volatile boolean thirstLoaded;
    private static volatile boolean fiahiChecked;
    private static volatile boolean fiahiLoaded;
    private static volatile boolean rvChecked;
    private static volatile boolean rvLoaded;

    private static boolean isThirstLoaded() {
        if (!thirstChecked) {
            thirstChecked = true;
            thirstLoaded = ModList.get() != null && ModList.get().isLoaded("thirst");
        }
        return thirstLoaded;
    }

    private static boolean isFiahiLoaded() {
        if (!fiahiChecked) {
            fiahiChecked = true;
            fiahiLoaded = ModList.get() != null && ModList.get().isLoaded("fiahi");
        }
        return fiahiLoaded;
    }

    private static boolean isRvLoaded() {
        if (!rvChecked) {
            rvChecked = true;
            rvLoaded = ModList.get() != null && ModList.get().isLoaded("reality_value");
        }
        return rvLoaded;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        List<Component> tooltip = event.getToolTip();

        // 从末尾提取并移除 fiahi 温度行，合并为单行
        Component tempLine = extractFiahiTemperatureLine(tooltip);

        // 生成食物值符号行
        Component foodLine = buildFoodValuesLine(stack);

        // 生成 RealityValue 恢复值符号行
        Component rvLine = buildRvValuesLine(stack);

        if (tempLine == null && foodLine == null && rvLine == null) return;

        // 插入到物品名之后（index 1），使顺序为：物品名 → 温度 → 食物值 → RV值 → F3+H
        int insertIdx = 1;
        if (tempLine != null) {
            tooltip.add(insertIdx++, tempLine);
        }
        if (foodLine != null) {
            tooltip.add(insertIdx++, foodLine);
        }
        if (rvLine != null) {
            tooltip.add(insertIdx, rvLine);
        }
    }

    /**
     * 从 tooltip 列表中提取所有 fiahi 温度行（状态行 + 温度值行），移除并合并为单行。
     * <p>状态行：{@code item.fiahi.temperature.normal/frozen.N/rotten.N}（新鲜/冷冻/腐烂）
     * <br>温度值行：{@code item.fiahi.temperature.description}（温度：N，仅 F3+H）
     * <p>合并格式：{@code 状态|温度N}（两行都有时）或单行（只有一行时）。
     */
    private static Component extractFiahiTemperatureLine(List<Component> tooltip) {
        if (!isFiahiLoaded()) return null;

        // 从后往前遍历，收集并移除 fiahi 温度行（从后往前移除不影响前面的索引）
        List<Component> fiahiLines = new ArrayList<>();
        for (int i = tooltip.size() - 1; i >= 0; i--) {
            if (tooltip.get(i).getContents() instanceof TranslatableContents tc) {
                if (tc.getKey().startsWith("item.fiahi.temperature.")) {
                    fiahiLines.add(0, tooltip.remove(i));
                }
            }
        }

        if (fiahiLines.isEmpty()) return null;
        if (fiahiLines.size() == 1) return fiahiLines.get(0);

        // 合并：状态行 | 温度行（状态行保留原色，温度行用灰色）
        MutableComponent merged = Component.empty();
        for (int i = 0; i < fiahiLines.size(); i++) {
            if (i > 0) {
                merged.append(Component.literal("|").withStyle(ChatFormatting.GRAY));
            }
            Component c = fiahiLines.get(i);
            if (c.getContents() instanceof TranslatableContents tc
                    && tc.getKey().equals("item.fiahi.temperature.description")) {
                merged.append(c.copy().withStyle(ChatFormatting.GRAY));
            } else {
                merged.append(c);
            }
        }
        return merged;
    }

    private static Component buildFoodValuesLine(ItemStack stack) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        boolean hasFood = food != null && food.nutrition() != 0;

        boolean hasThirst = false;
        int thirst = 0;
        int quenched = 0;
        // 口渴模组加载时，食物一律显示口渴/解渴值（即使为0）
        if (isThirstLoaded() && (hasFood || ThirstHelper.itemRestoresThirst(stack))) {
            hasThirst = true;
            try {
                if (ThirstHelper.itemRestoresThirst(stack)) {
                    thirst = ThirstHelper.getThirst(stack);
                    quenched = ThirstHelper.getQuenched(stack);
                }
            } catch (Throwable ignored) {
                // 某些物品调用 getThirst/getQuenched 会 NPE，默认 0
            }
        }

        if (!hasFood && !hasThirst) return null;

        MutableComponent line = Component.empty();
        if (hasFood) {
            // 字体包图标为2点对应值，显示值 = 实际值 / 2（保留 .5 小数）
            line.append(Component.literal(ICON_HUNGER + formatHalf(food.nutrition() / 2.0) + " "));
            line.append(Component.literal(ICON_SATURATION + formatHalf(food.saturation() / 2.0) + " "));
        }
        if (hasThirst) {
            line.append(Component.literal(ICON_THIRST + formatHalf(thirst / 2.0) + " "));
            line.append(Component.literal(ICON_QUENCHED + formatHalf(quenched / 2.0)));
        }
        return line.withStyle(ChatFormatting.GRAY);
    }

    /**
     * 构建 RealityValue 恢复值符号行：健康 免疫力 理智 精力。
     * <p>数据来源：{@link PlayerExCap#itemHealthRestore} / {@link PlayerExCap#itemImmunityRestore}
     * / {@link PlayerExCap#itemSanityRestore} / {@link PlayerExCap#itemEnergyRestore}。
     * <p>显示值 = 实际值 / 2（保留 .5 小数），与图标 2 点对应值一致。
     */
    private static Component buildRvValuesLine(ItemStack stack) {
        if (!isRvLoaded()) return null;

        float health = 0, immunity = 0, sanity = 0, energy = 0;
        try {
            health = PlayerExCap.itemHealthRestore(stack);
            immunity = PlayerExCap.itemImmunityRestore(stack);
            sanity = PlayerExCap.itemSanityRestore(stack);
            energy = PlayerExCap.itemEnergyRestore(stack);
        } catch (Throwable ignored) {
            return null;
        }

        // 全部为 0 时不显示
        if (health == 0 && immunity == 0 && sanity == 0 && energy == 0) return null;

        MutableComponent line = Component.empty();
        line.append(Component.literal(ICON_HEALTHY + formatHalf(health / 2.0) + " "));
        line.append(Component.literal(ICON_IMMUNITY + formatHalf(immunity / 2.0) + " "));
        line.append(Component.literal(ICON_SANITY + formatHalf(sanity / 2.0) + " "));
        line.append(Component.literal(ICON_ENERGY + formatHalf(energy / 2.0)));
        return line.withStyle(ChatFormatting.GRAY);
    }

    /** 格式化半值：整数显示为整数（3），非整数保留1位小数（2.5）。 */
    private static String formatHalf(double value) {
        double rounded = Math.round(value * 10.0) / 10.0;
        if (rounded == Math.floor(rounded)) {
            return String.valueOf((int) rounded);
        }
        return String.valueOf(rounded);
    }
}
