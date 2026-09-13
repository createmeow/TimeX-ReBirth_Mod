package io.github.createmeow.timex_rebirth.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Optional;

/**
 * 世界生成修改：在原版群系基础上，将每个非寒冷生物群系固定映射为其寒冷变体
 * （丛林→积雪针叶林、海洋→冰刺平原、河流→冻河等）。
 * 冻洋（frozen_ocean / deep_frozen_ocean）被排除：不生成冻洋，海洋类统一映射为冰刺平原。
 * 服务端与客户端同时生效（保证生物群系渲染一致）。
 */
@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceMixin {

    /** 原版主世界生物群系 → 寒冷变体 固定映射表。已属于积雪群系的不在表中（保持不变）。 */
    private static final Map<ResourceLocation, ResourceLocation> COLD_VARIANT = Map.ofEntries(
            // ── 平原/草原类 → 雪原 ──
            Map.entry(ResourceLocation.withDefaultNamespace("plains"), ResourceLocation.withDefaultNamespace("snowy_plains")),
            Map.entry(ResourceLocation.withDefaultNamespace("sunflower_plains"), ResourceLocation.withDefaultNamespace("snowy_plains")),
            Map.entry(ResourceLocation.withDefaultNamespace("savanna"), ResourceLocation.withDefaultNamespace("snowy_plains")),
            Map.entry(ResourceLocation.withDefaultNamespace("savanna_plateau"), ResourceLocation.withDefaultNamespace("snowy_plains")),
            Map.entry(ResourceLocation.withDefaultNamespace("windswept_savanna"), ResourceLocation.withDefaultNamespace("snowy_plains")),
            Map.entry(ResourceLocation.withDefaultNamespace("desert"), ResourceLocation.withDefaultNamespace("snowy_plains")),
            Map.entry(ResourceLocation.withDefaultNamespace("badlands"), ResourceLocation.withDefaultNamespace("snowy_plains")),
            Map.entry(ResourceLocation.withDefaultNamespace("eroded_badlands"), ResourceLocation.withDefaultNamespace("snowy_plains")),
            Map.entry(ResourceLocation.withDefaultNamespace("mushroom_fields"), ResourceLocation.withDefaultNamespace("snowy_plains")),

            // ── 森林/丛林/沼泽类 → 积雪针叶林 ──
            Map.entry(ResourceLocation.withDefaultNamespace("forest"), ResourceLocation.withDefaultNamespace("snowy_taiga")),
            Map.entry(ResourceLocation.withDefaultNamespace("flower_forest"), ResourceLocation.withDefaultNamespace("snowy_taiga")),
            Map.entry(ResourceLocation.withDefaultNamespace("birch_forest"), ResourceLocation.withDefaultNamespace("snowy_taiga")),
            Map.entry(ResourceLocation.withDefaultNamespace("old_growth_birch_forest"), ResourceLocation.withDefaultNamespace("snowy_taiga")),
            Map.entry(ResourceLocation.withDefaultNamespace("dark_forest"), ResourceLocation.withDefaultNamespace("snowy_taiga")),
            Map.entry(ResourceLocation.withDefaultNamespace("jungle"), ResourceLocation.withDefaultNamespace("snowy_taiga")),
            Map.entry(ResourceLocation.withDefaultNamespace("sparse_jungle"), ResourceLocation.withDefaultNamespace("snowy_taiga")),
            Map.entry(ResourceLocation.withDefaultNamespace("bamboo_jungle"), ResourceLocation.withDefaultNamespace("snowy_taiga")),
            Map.entry(ResourceLocation.withDefaultNamespace("taiga"), ResourceLocation.withDefaultNamespace("snowy_taiga")),
            Map.entry(ResourceLocation.withDefaultNamespace("old_growth_pine_taiga"), ResourceLocation.withDefaultNamespace("snowy_taiga")),
            Map.entry(ResourceLocation.withDefaultNamespace("old_growth_spruce_taiga"), ResourceLocation.withDefaultNamespace("snowy_taiga")),
            Map.entry(ResourceLocation.withDefaultNamespace("swamp"), ResourceLocation.withDefaultNamespace("snowy_taiga")),
            Map.entry(ResourceLocation.withDefaultNamespace("mangrove_swamp"), ResourceLocation.withDefaultNamespace("snowy_taiga")),
            Map.entry(ResourceLocation.withDefaultNamespace("wooded_badlands"), ResourceLocation.withDefaultNamespace("snowy_taiga")),

            // ── 山地类 → 雪山坡/冻峰 ──
            Map.entry(ResourceLocation.withDefaultNamespace("windswept_hills"), ResourceLocation.withDefaultNamespace("snowy_slopes")),
            Map.entry(ResourceLocation.withDefaultNamespace("windswept_gravelly_hills"), ResourceLocation.withDefaultNamespace("snowy_slopes")),
            Map.entry(ResourceLocation.withDefaultNamespace("windswept_forest"), ResourceLocation.withDefaultNamespace("snowy_slopes")),
            Map.entry(ResourceLocation.withDefaultNamespace("stony_peaks"), ResourceLocation.withDefaultNamespace("frozen_peaks")),
            Map.entry(ResourceLocation.withDefaultNamespace("jagged_peaks"), ResourceLocation.withDefaultNamespace("frozen_peaks")),
            Map.entry(ResourceLocation.withDefaultNamespace("meadow"), ResourceLocation.withDefaultNamespace("grove")),
            Map.entry(ResourceLocation.withDefaultNamespace("cherry_grove"), ResourceLocation.withDefaultNamespace("grove")),

            // ── 水域类：海洋 → 冰刺平原；冻洋排除（不生成冻洋）──
            Map.entry(ResourceLocation.withDefaultNamespace("ocean"), ResourceLocation.withDefaultNamespace("ice_spikes")),
            Map.entry(ResourceLocation.withDefaultNamespace("deep_ocean"), ResourceLocation.withDefaultNamespace("ice_spikes")),
            Map.entry(ResourceLocation.withDefaultNamespace("warm_ocean"), ResourceLocation.withDefaultNamespace("ice_spikes")),
            Map.entry(ResourceLocation.withDefaultNamespace("lukewarm_ocean"), ResourceLocation.withDefaultNamespace("ice_spikes")),
            Map.entry(ResourceLocation.withDefaultNamespace("deep_lukewarm_ocean"), ResourceLocation.withDefaultNamespace("ice_spikes")),
            Map.entry(ResourceLocation.withDefaultNamespace("cold_ocean"), ResourceLocation.withDefaultNamespace("ice_spikes")),
            Map.entry(ResourceLocation.withDefaultNamespace("deep_cold_ocean"), ResourceLocation.withDefaultNamespace("ice_spikes")),
            Map.entry(ResourceLocation.withDefaultNamespace("frozen_ocean"), ResourceLocation.withDefaultNamespace("ice_spikes")),
            Map.entry(ResourceLocation.withDefaultNamespace("deep_frozen_ocean"), ResourceLocation.withDefaultNamespace("ice_spikes")),
            Map.entry(ResourceLocation.withDefaultNamespace("river"), ResourceLocation.withDefaultNamespace("frozen_river")),

            // ── 岸边类 → 积雪沙滩 ──
            Map.entry(ResourceLocation.withDefaultNamespace("beach"), ResourceLocation.withDefaultNamespace("snowy_beach")),
            Map.entry(ResourceLocation.withDefaultNamespace("stony_shore"), ResourceLocation.withDefaultNamespace("snowy_beach"))
    );

    /**
     * Terralith 兼容（极寒主题）：官方温度标签为 temperate/lukewarm/warm 的非寒冷群系
     * 映射为 Terralith 自带寒冷群系（保持地形特色：沙漠→砾石荒漠、丛林→西伯利亚针叶林等）。
     * 官方已标 frozen/cold 或未标温度的群系（洞穴、alpha/mirage 特殊群系）不在表中，保持原样。
     * 河流/海洋类对齐原版映射（frozen_river / ice_spikes）。
     */
    private static final Map<ResourceLocation, ResourceLocation> TERRALITH_COLD_VARIANT = Map.ofEntries(
            // ── 温带 temperate ──
            tEntry("birch_taiga", "wintry_forest"),
            tEntry("blooming_plateau", "wintry_lowlands"),
            tEntry("blooming_valley", "wintry_lowlands"),
            tEntry("caldera", "frozen_cliffs"),
            Map.entry(rl("terralith", "gravel_beach"), rl("minecraft", "snowy_beach")),
            tEntry("haze_mountain", "rocky_mountains"),
            tEntry("highlands", "snowy_shield"),
            tEntry("lavender_forest", "wintry_forest"),
            tEntry("lavender_valley", "wintry_lowlands"),
            tEntry("orchid_swamp", "ice_marsh"),
            tEntry("sakura_grove", "snowy_maple_forest"),
            tEntry("sakura_valley", "snowy_maple_forest"),
            tEntry("shrubland", "cold_shrubland"),
            tEntry("skylands_autumn", "skylands_winter"),
            tEntry("skylands_spring", "skylands_winter"),
            tEntry("steppe", "wintry_lowlands"),
            tEntry("stony_spires", "frozen_cliffs"),
            tEntry("temperate_highlands", "siberian_grove"),
            tEntry("valley_clearing", "wintry_lowlands"),
            // ── 微温 lukewarm ──
            tEntry("amethyst_canyon", "gravel_desert"),
            tEntry("amethyst_rainforest", "siberian_taiga"),
            tEntry("jungle_mountains", "siberian_grove"),
            tEntry("rocky_jungle", "siberian_taiga"),
            tEntry("tropical_jungle", "siberian_taiga"),
            tEntry("arid_highlands", "wintry_lowlands"),
            tEntry("ashen_savanna", "cold_shrubland"),
            tEntry("fractured_savanna", "cold_shrubland"),
            tEntry("savanna_badlands", "snowy_badlands"),
            tEntry("savanna_slopes", "snowy_shield"),
            tEntry("basalt_cliffs", "gravel_desert"),
            tEntry("brushland", "cold_shrubland"),
            tEntry("granite_cliffs", "frozen_cliffs"),
            tEntry("hot_shrubland", "cold_shrubland"),
            tEntry("skylands_summer", "skylands_winter"),
            tEntry("volcanic_crater", "frozen_cliffs"),
            tEntry("volcanic_peaks", "emerald_peaks"),
            // ── 温暖 warm ──
            tEntry("bryce_canyon", "snowy_badlands"),
            tEntry("painted_mountains", "emerald_peaks"),
            tEntry("red_oasis", "gravel_desert"),
            tEntry("white_mesa", "gravel_desert"),
            tEntry("desert_oasis", "gravel_desert"),
            tEntry("desert_spires", "frozen_cliffs"),
            tEntry("lush_desert", "gravel_desert"),
            tEntry("sandstone_valley", "wintry_lowlands"),
            tEntry("ancient_sands", "gravel_desert"),
            tEntry("desert_canyon", "gravel_desert"),
            Map.entry(rl("terralith", "warm_river"), rl("minecraft", "frozen_river")),
            Map.entry(rl("terralith", "deep_warm_ocean"), rl("minecraft", "ice_spikes"))
    );

    /** Terralith 兜底气候标签（未显式映射的新增群系用；Terralith 未加载时标签为空，不触发）。 */
    private static final TagKey<Biome> C_IS_HOT = TagKey.create(Registries.BIOME, rl("c", "is_hot"));
    private static final TagKey<Biome> C_IS_TEMPERATE = TagKey.create(Registries.BIOME, rl("c", "is_temperate"));

    private static ResourceLocation rl(String ns, String path) {
        return ResourceLocation.fromNamespaceAndPath(ns, path);
    }

    private static Map.Entry<ResourceLocation, ResourceLocation> tEntry(String name, String cold) {
        return Map.entry(rl("terralith", name), rl("terralith", cold));
    }

    /**
     * 直接读取当前服务器的 registry（不做跨调用缓存）：
     * 新开世界或 /reload 数据包重载都会重建 RegistryAccess，产生新的 Biome/PlacedFeature 实例，
     * 若缓存旧 registry 的 Holder，会触发 applyBiomeDecoration 中 FeatureSorter 的
     * 引用相等索引返回 -1（IndexOutOfBoundsException）导致世界创建卡死。
     */
    @Inject(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At("RETURN"), cancellable = true)
    private void timex_rebirth$forceSnowy(int x, int y, int z, Climate.Sampler sampler,
                                          CallbackInfoReturnable<Holder<Biome>> cir) {
        Holder<Biome> biome = cir.getReturnValue();
        if (biome == null) return;
        // 只处理主世界生物群系；下界/末地不处理
        if (!biome.is(BiomeTags.IS_OVERWORLD)) return;

        Optional<ResourceKey<Biome>> keyOpt = biome.unwrapKey();
        if (keyOpt.isEmpty()) return;
        ResourceLocation source = keyOpt.get().location();

        Registry<Biome> registry = getBiomeRegistry();
        if (registry == null) return;

        if (!source.getNamespace().equals("minecraft")) {
            // 模组群系兼容（Terralith）：映射已知非寒冷群系；未映射的寒冷群系保持原样
            ResourceLocation target = TERRALITH_COLD_VARIANT.get(source);
            if (target == null) {
                // 新版本新增群系兜底：官方标记为 hot/temperate 的映射到安全寒冷群系
                if (biome.is(C_IS_HOT)) target = rl("terralith", "gravel_desert");
                else if (biome.is(C_IS_TEMPERATE)) target = rl("terralith", "wintry_lowlands");
                else return;
            }
            // 目标群系不存在（如旧版本 Terralith 缺群系）时保留原样，避免崩溃
            Holder<Biome> cold = registry.getHolder(ResourceKey.create(Registries.BIOME, target)).orElse(null);
            if (cold != null) cir.setReturnValue(cold);
            return;
        }

        ResourceLocation target = COLD_VARIANT.get(source);
        if (target == null) return; // 本身已是寒冷群系（如 snowy_plains）或无需映射

        cir.setReturnValue(registry.getHolderOrThrow(ResourceKey.create(Registries.BIOME, target)));
    }

    private static Registry<Biome> getBiomeRegistry() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        return server.registryAccess().registryOrThrow(Registries.BIOME);
    }
}
