package com.createmeow.cm_plugins;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 玩家行为吸引怪物：玩家触发的爆炸、放置/破坏方块、手持光源物品时，
 * 将周围 {@link #ATTRACT_RADIUS} 格内的僵尸（含尸壳/溺尸变种）与
 * mutanter 怪物（7 种）的攻击目标设为该玩家，使其向声源/光源追击。
 * <ul>
 *   <li>爆炸 / 放置 / 破坏：事件驱动，发生即吸引</li>
 *   <li>手持光源：主手或副手持有光照 &gt; 0 的方块物品（火把/灯笼/萤石等），
 *       每玩家每 {@link #LIGHT_SCAN_INTERVAL} tick 持续吸引一次</li>
 * </ul>
 * 实体扫描使用 {@link ServerLevel#getAllEntities()} 直接迭代（规避 Sable 对大 AABB 查询的拦截）。
 */
public class ActivityAttractionHandler {

    /** 吸引半径（格） */
    public static final double ATTRACT_RADIUS = 72.0;

    /** 手持光源的吸引周期（tick）：2 秒一次 */
    private static final int LIGHT_SCAN_INTERVAL = 40;

    // ========== 玩家触发的爆炸 ==========

    /**
     * 爆炸爆炸事件：直接或间接来源为玩家（徒手引爆水晶/床/重生锚，
     * 或点燃的 TNT 为间接来源）即视为玩家触发的爆炸。
     */
    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        ExplosionSource src = resolveSource(event);
        if (src == null) return;
        attractAround(level, src.player());
    }

    private record ExplosionSource(ServerPlayer player) {}

    private static ExplosionSource resolveSource(ExplosionEvent.Detonate event) {
        Entity direct = event.getExplosion().getDirectSourceEntity();
        if (direct instanceof ServerPlayer p) return new ExplosionSource(p);
        Entity indirect = event.getExplosion().getIndirectSourceEntity();
        if (indirect instanceof ServerPlayer p) return new ExplosionSource(p);
        return null;
    }

    // ========== 放置 / 破坏方块 ==========

    @SubscribeEvent
    public static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        attractAround((ServerLevel) player.level(), player);
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        attractAround((ServerLevel) player.level(), player);
    }

    // ========== 手持光源 ==========

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        if (player.tickCount % LIGHT_SCAN_INTERVAL != 0) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (!holdsLightSource(player)) return;
        attractAround((ServerLevel) player.level(), player);
    }

    /** 主手或副手是否持有光照 &gt; 0 的方块物品（火把/灯笼/萤石/营火/岩浆块等）。 */
    private static boolean holdsLightSource(ServerPlayer player) {
        return holdsLight(player.getMainHandItem()) || holdsLight(player.getOffhandItem());
    }

    private static boolean holdsLight(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Block block = Block.byItem(stack.getItem());
        if (block == Blocks.AIR) return false;
        BlockState state = block.defaultBlockState();
        return state.getLightEmission() > 0;
    }

    // ========== 吸引逻辑 ==========

    /** 将范围内目标怪物的攻击目标设为玩家（直接迭代全实体，不做 AABB 查询）。 */
    public static void attractAround(ServerLevel level, ServerPlayer player) {
        attractAround(level, player, ATTRACT_RADIUS);
    }

    /** 将 {@code radius} 格内目标怪物的攻击目标设为玩家（供清刷机等外部调用）。 */
    public static void attractAround(ServerLevel level, ServerPlayer player, double radius) {
        double radiusSqr = radius * radius;
        int attracted = 0;
        for (Entity e : level.getAllEntities()) {
            if (!(e instanceof Mob mob) || !mob.isAlive()) continue;
            if (!(mob instanceof Zombie) && !GunfireAttractionHandler.isMutanterMob(mob)) continue;
            if (mob.distanceToSqr(player) > radiusSqr) continue;
            mob.setTarget(player);
            attracted++;
        }
        if (attracted > 0) {
            createmeowsplugins.LOGGER.debug("[ActivityAttraction] 玩家 {} 的行为吸引了 {} 只怪物",
                    player.getName().getString(), attracted);
        }
    }
}
