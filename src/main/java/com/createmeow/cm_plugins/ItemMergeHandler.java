package com.createmeow.cm_plugins;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 爆炸掉落物合并（服务端）：爆炸/大范围破坏后同类掉落物散落在爆炸位置附近，
 * 原版合并（碰撞盒相邻 ~0.5 格）合并率低，一次爆炸可残留 250+ 个掉落物实体。
 * <p>两阶段实现：</p>
 * <ol>
 * <li>{@link ExplosionEvent.Detonate}（此时方块掉落尚未生成）仅记录爆炸位置与半径；</li>
 * <li>{@link ServerTickEvent.Post}（本 tick 所有爆炸的掉落物已生成完毕）对
 *     位于任一爆炸半径 + {@link #MERGE_MARGIN} 范围内的掉落物做网格聚类合并。</li>
 * </ol>
 * <p>仅影响爆炸掉落物，不触碰其他场景的掉落物（原版合并语义不受影响）。
 * 合并数量守恒，超过最大堆叠的部分按堆叠上限在同位置拆出新实体。</p>
 */
public class ItemMergeHandler {

    /** 合并网格边长（格）：候选掉落物按此网格聚类后再按物品分组 */
    private static final double CELL = 1.5;

    /** 合并范围 = 爆炸半径 + 该缓冲（格） */
    private static final double MERGE_MARGIN = 2.0;

    /** 本 tick 各维度的爆炸记录（x, y, z, radius），tick 末处理并清空 */
    private static final Map<ServerLevel, List<double[]>> PENDING_EXPLOSIONS = new HashMap<>();

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        // Detonate 触发于 finalizeExplosion 之前，此处仅记录；掉落物在 tick 末合并
        Vec3 center = event.getExplosion().center();
        PENDING_EXPLOSIONS.computeIfAbsent(level, k -> new ArrayList<>())
                .add(new double[]{center.x, center.y, center.z, event.getExplosion().radius()});
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING_EXPLOSIONS.isEmpty()) return;
        // 本 tick 爆炸的掉落物此时已全部生成，逐一处理对应维度
        for (Map.Entry<ServerLevel, List<double[]>> entry : PENDING_EXPLOSIONS.entrySet()) {
            mergeExplosionDrops(entry.getKey(), entry.getValue());
        }
        PENDING_EXPLOSIONS.clear();
    }

    /** 合并位于任一爆炸范围内的同类掉落物。 */
    private static void mergeExplosionDrops(ServerLevel level, List<double[]> explosions) {
        // 收集位于爆炸范围内的掉落物（直接迭代实体，规避 Sable 对异常大 AABB 的拦截）
        List<ItemEntity> candidates = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof ItemEntity item) || !item.isAlive() || item.getItem().isEmpty()) continue;
            double x = item.getX(), y = item.getY(), z = item.getZ();
            for (double[] boom : explosions) {
                double dx = x - boom[0], dy = y - boom[1], dz = z - boom[2];
                double r = boom[3] + MERGE_MARGIN;
                if (dx * dx + dy * dy + dz * dz <= r * r) {
                    candidates.add(item);
                    break;
                }
            }
        }
        if (candidates.size() < 2) return;

        // 按（网格坐标）分桶
        Map<Long, List<ItemEntity>> cells = new HashMap<>();
        for (ItemEntity item : candidates) {
            cells.computeIfAbsent(cellKey(item), k -> new ArrayList<>()).add(item);
        }

        for (List<ItemEntity> cell : cells.values()) {
            if (cell.size() < 2) continue;
            // 网格内按（物品+组件）线性分组
            List<List<ItemEntity>> groups = new ArrayList<>();
            outer:
            for (ItemEntity item : cell) {
                for (List<ItemEntity> group : groups) {
                    if (ItemStack.isSameItemSameComponents(group.get(0).getItem(), item.getItem())) {
                        group.add(item);
                        continue outer;
                    }
                }
                List<ItemEntity> group = new ArrayList<>();
                group.add(item);
                groups.add(group);
            }
            for (List<ItemEntity> group : groups) {
                if (group.size() >= 2) {
                    mergeGroup(level, group);
                }
            }
        }
    }

    /** 将组内掉落物合并到最早生成的实体，超堆叠部分在同位置拆出新实体。 */
    private static void mergeGroup(ServerLevel level, List<ItemEntity> group) {
        ItemEntity target = group.get(0);
        ItemStack stack = target.getItem();
        int total = 0;
        for (ItemEntity item : group) {
            total += item.getItem().getCount();
        }

        int max = stack.getMaxStackSize();
        int merged = Math.min(total, max);
        int remaining = total - merged;
        stack.setCount(merged);
        for (int i = 1; i < group.size(); i++) {
            group.get(i).discard();
        }
        while (remaining > 0) {
            ItemStack extra = stack.copy();
            extra.setCount(Math.min(remaining, max));
            remaining -= extra.getCount();
            ItemEntity extraEntity = new ItemEntity(level, target.getX(), target.getY(), target.getZ(), extra);
            level.addFreshEntity(extraEntity);
        }
    }

    /** 网格坐标打包为长整型键。 */
    private static long cellKey(ItemEntity item) {
        int cx = (int) Math.floor(item.getX() / CELL);
        int cy = (int) Math.floor(item.getY() / CELL);
        int cz = (int) Math.floor(item.getZ() / CELL);
        return (long) cx * 341873128712L + (long) cy * 132897987541L + (long) cz;
    }
}
