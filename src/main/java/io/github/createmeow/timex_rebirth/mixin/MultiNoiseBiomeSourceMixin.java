package io.github.createmeow.timex_rebirth.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BiomeTags;
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
        // 只映射原版生物群系，模组群系保持原样
        if (!source.getNamespace().equals("minecraft")) return;

        ResourceLocation target = COLD_VARIANT.get(source);
        if (target == null) return; // 本身已是寒冷群系（如 snowy_plains）或无需映射

        Registry<Biome> registry = getBiomeRegistry();
        if (registry == null) return;

        cir.setReturnValue(registry.getHolderOrThrow(ResourceKey.create(Registries.BIOME, target)));
    }

    private static Registry<Biome> getBiomeRegistry() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        return server.registryAccess().registryOrThrow(Registries.BIOME);
    }
}
