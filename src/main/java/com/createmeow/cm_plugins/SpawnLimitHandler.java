package com.createmeow.cm_plugins;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 自然生成数量限制 + 生成禁令（{@link FinalizeSpawnEvent}，仅服务端）：
 * <ul>
 * <li>僵尸（含尸壳/溺尸等 {@link Zombie} 变种）每维度最多自然存在 90 只</li>
 * <li>mutanter 怪物（7 种）每种每维度最多自然存在 10 只</li>
 * <li>发光鱿鱼每维度最多自然存在 10 只</li>
 * <li>鲑鱼/鳕鱼合计每维度最多自然存在 10 只</li>
 * <li>蝙蝠禁止生成；热带鱼除人为放置（水桶/刷怪蛋）外禁止生成</li>
 * </ul>
 * 数量统计按（维度+分组）做 40 tick 缓存，避免高频生成尝试反复遍历实体表。
 */
public class SpawnLimitHandler {

    /** 僵尸每维度自然生成上限 */
    public static final int ZOMBIE_LIMIT = 90;
    /** mutanter 怪物每种每维度自然生成上限 */
    public static final int MUTANTER_LIMIT = 10;
    /** 发光鱿鱼每维度自然生成上限 */
    public static final int GLOW_SQUID_LIMIT = 10;
    /** 鲑鱼/鳕鱼合计每维度自然生成上限 */
    public static final int FISH_LIMIT = 10;

    /** mutanter 模组的怪物实体注册名路径（7 种，不含投射物） */
    private static final Set<String> MUTANTER_MOBS = Set.of(
            "mini_slime", "slime", "raider", "devourer", "destroyer", "lurker", "amalgamation");

    /** 缓存过期时间（tick） */
    private static final long CACHE_TTL_TICKS = 40;

    /** (维度|分组) → [gameTime, count] 数量缓存 */
    private static final Map<String, long[]> COUNT_CACHE = new HashMap<>();

    @SubscribeEvent
    public void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Level level = event.getLevel().getLevel();
        if (level.isClientSide()) return;
        if (!(level instanceof ServerLevel server)) return;

        MobSpawnType reason = event.getSpawnType();
        EntityType<?> type = event.getEntity().getType();
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);

        // ── 生成禁令：蝙蝠全部禁止；热带鱼仅放行人为放置（水桶/刷怪蛋） ──
        if (type == EntityType.BAT) {
            event.setSpawnCancelled(true);
            return;
        }
        if (type == EntityType.TROPICAL_FISH && reason != MobSpawnType.BUCKET && reason != MobSpawnType.SPAWN_EGG) {
            event.setSpawnCancelled(true);
            return;
        }

        // ── 数量上限：仅约束自然生成 ──
        if (reason != MobSpawnType.NATURAL) return;

        if (event.getEntity() instanceof Zombie) {
            if (countGroup(server, "zombie", e -> e instanceof Zombie) >= ZOMBIE_LIMIT) {
                event.setSpawnCancelled(true);
            }
            return;
        }
        if (type == EntityType.GLOW_SQUID) {
            if (countGroup(server, "glow_squid", e -> e.getType() == EntityType.GLOW_SQUID) >= GLOW_SQUID_LIMIT) {
                event.setSpawnCancelled(true);
            }
            return;
        }
        if (type == EntityType.SALMON || type == EntityType.COD) {
            if (countGroup(server, "salmon_cod", e -> e.getType() == EntityType.SALMON
                    || e.getType() == EntityType.COD) >= FISH_LIMIT) {
                event.setSpawnCancelled(true);
            }
            return;
        }
        if ("mutanter".equals(typeId.getNamespace()) && MUTANTER_MOBS.contains(typeId.getPath())) {
            String group = "mutanter:" + typeId.getPath();
            if (countGroup(server, group, e -> e.getType() == type) >= MUTANTER_LIMIT) {
                event.setSpawnCancelled(true);
            }
        }
    }

    /**
     * 统计维度内满足条件的实体数量（带 40 tick 缓存）。
     * 生成尝试高频触发，直接遍历实体表代价高，按（维度+分组）缓存。
     * <p>不用 AABB 全表查询：Sable 的实体查询优化会拦截无限大 AABB 并中止（返回空列表），
     * 改用 {@link ServerLevel#getAllEntities()} 直接迭代。</p>
     */
    private static int countGroup(ServerLevel level, String group, Predicate<Entity> matcher) {
        String key = level.dimension().location() + "|" + group;
        long now = level.getGameTime();
        long[] cached = COUNT_CACHE.get(key);
        if (cached != null && now - cached[0] < CACHE_TTL_TICKS && cached[0] <= now) {
            return (int) cached[1];
        }
        int count = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity.isAlive() && matcher.test(entity)) count++;
        }
        COUNT_CACHE.put(key, new long[]{now, count});
        return count;
    }
}
