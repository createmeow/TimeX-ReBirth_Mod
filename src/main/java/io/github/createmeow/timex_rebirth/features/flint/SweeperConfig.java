package io.github.createmeow.timex_rebirth.features.flint;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 清刷机配置（按物品实例存储，{@link DataComponents#CUSTOM_DATA}）：
 * <ul>
 * <li>清理积雪（默认开）：雪层/雪块/可疑积雪</li>
 * <li>清理落叶（默认关）：IW {@code immersive_weathering:leaf_piles} 标签全集（10 种叶堆）</li>
 * <li>清理杂草/花（默认关）：IW {@code grass_spread_source} 标签（weeds/短草/高草/蕨/大型蕨）
 *     + IW 结霜的草/蕨（{@code frosty_grass}/{@code frosty_fern}）+ 原版 {@code #minecraft:flowers} 标签</li>
 * <li>对实体造成伤害（默认关）：每次清刷对清刷范围内实体造成 2 点伤害</li>
 * <li>清扫防浪费（默认开）：开启后范围内无可清扫方块时不执行清刷；
 *     关闭后允许空刷（粒子+音效），配合伤害选项时空刷也能造成伤害</li>
 * </ul>
 * IW 方块按注册表 id/标签比较，零类加载依赖（IW 缺席时标签为空、id 不存在，自然失效）。
 */
public final class SweeperConfig {

    /** IW 落叶全集（叶堆）标签 */
    public static final TagKey<Block> LEAF_PILES =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("immersive_weathering", "leaf_piles"));
    /** IW 草蔓延源标签（weeds、短草、高草、蕨、大型蕨） */
    public static final TagKey<Block> GRASS_SPREAD_SOURCE =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("immersive_weathering", "grass_spread_source"));
    /** IW 结霜的草 */
    public static final ResourceLocation FROSTY_GRASS =
            ResourceLocation.fromNamespaceAndPath("immersive_weathering", "frosty_grass");
    /** IW 结霜的蕨 */
    public static final ResourceLocation FROSTY_FERN =
            ResourceLocation.fromNamespaceAndPath("immersive_weathering", "frosty_fern");

    private SweeperConfig() {}

    /** 清刷半径上限（格） */
    public static final double MAX_RANGE = 5.0;
    /** 清刷半径下限（格） */
    public static final double MIN_RANGE = 0.5;
    /** 清刷伤害上限（点） */
    public static final float MAX_DAMAGE = 5.0f;

    public static boolean clearSnow(ItemStack stack) {
        return readFlag(stack, "snow", true);
    }

    public static boolean clearLeaves(ItemStack stack) {
        return readFlag(stack, "leaves", false);
    }

    public static boolean clearPlants(ItemStack stack) {
        return readFlag(stack, "plants", false);
    }

    /** 对实体造成伤害（默认关）。 */
    public static boolean damageEntities(ItemStack stack) {
        return readFlag(stack, "damage", false);
    }

    /** 清扫防浪费（默认开）：无可清扫方块时不执行清刷。 */
    public static boolean preventWaste(ItemStack stack) {
        return readFlag(stack, "no_waste", true);
    }

    /** 清刷半径（格，默认 5 = 上限），写入时已钳制到 0.5~5。 */
    public static double range(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.copyTag().contains("range")) return MAX_RANGE;
        double v = data.copyTag().getDouble("range");
        return Math.clamp(v, MIN_RANGE, MAX_RANGE);
    }

    /** 清刷伤害（点，默认 2），写入时已钳制到 0~5。 */
    public static float damageAmount(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.copyTag().contains("damage_amt")) return 2.0f;
        float v = data.copyTag().getFloat("damage_amt");
        return Math.clamp(v, 0.0f, MAX_DAMAGE);
    }

    public static void set(ItemStack stack, boolean snow, boolean leaves, boolean plants,
                           boolean damage, boolean noWaste, double range, float damageAmt) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("snow", snow);
        tag.putBoolean("leaves", leaves);
        tag.putBoolean("plants", plants);
        tag.putBoolean("damage", damage);
        tag.putBoolean("no_waste", noWaste);
        tag.putDouble("range", Math.clamp(range, MIN_RANGE, MAX_RANGE));
        tag.putFloat("damage_amt", Math.clamp(damageAmt, 0.0f, MAX_DAMAGE));
        stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }

    private static boolean readFlag(ItemStack stack, String key, boolean defaultValue) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return defaultValue;
        CompoundTag tag = data.copyTag();
        return tag.contains(key) ? tag.getBoolean(key) : defaultValue;
    }

    /** 是否为"杂草/花"类目标（grass_spread_source 标签 + 结霜草/蕨 + 原版 #minecraft:flowers）。 */
    public static boolean isPlantBlock(BlockState state) {
        if (state.is(GRASS_SPREAD_SOURCE)) return true;
        if (state.is(net.minecraft.tags.BlockTags.FLOWERS)) return true;
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return FROSTY_GRASS.equals(id) || FROSTY_FERN.equals(id);
    }
}
