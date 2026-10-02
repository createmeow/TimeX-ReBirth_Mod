package io.github.createmeow.timex_rebirth.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Sable issue #1510 / Create Aeronautics issue #1395 修复的延迟处理部分。
 * <p>
 * ExplosiveBlockCallback.onHit 被取消后由 {@link #schedule} 调度：
 * 延迟 10 tick（0.5s）后在服务端 tick 末尾执行原 TNT 转换逻辑。
 * 执行前先移除 TNT 六面的 simulated:iron_handle，
 * 使后续 setBlock(AIR) 的级联更新不再破坏被玩家持有的把手。
 * <p>
 * 额外收益：实体生成与方块修改发生在服务端 tick 末尾，
 * 脱离物理步进的 JNI 回调上下文，避免物理引擎内部状态被中途修改。
 */
@EventBusSubscriber(modid = "timex_rebirth")
public final class SableTntDelayHandler {

    /** 每位置去重：已调度的坐标，直到实际执行时才移除 */
    private static final Set<Long> SCHEDULED = new HashSet<>();

    private record PendingHit(ServerLevel level, BlockPos pos, long runAt) {}

    private static final List<PendingHit> PENDING = new ArrayList<>();

    private SableTntDelayHandler() {
    }

    /** 延迟 10 tick（0.5s）后执行 TNT 碰撞转换 */
    public static final int DELAY_TICKS = 10;

    public static void schedule(ServerLevel level, BlockPos pos) {
        if (!SCHEDULED.add(pos.asLong())) {
            return; // 该位置已有待处理的转换
        }
        PENDING.add(new PendingHit(level, pos.immutable(), level.getGameTime() + DELAY_TICKS));
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            return;
        }
        Iterator<PendingHit> it = PENDING.iterator();
        while (it.hasNext()) {
            PendingHit hit = it.next();
            if (hit.level().getGameTime() < hit.runAt()) {
                continue;
            }
            it.remove();
            SCHEDULED.remove(hit.pos().asLong());

            ServerLevel level = hit.level();
            BlockPos pos = hit.pos();

            // 1. 先移除六面的 simulated:iron_handle（不产生掉落物）
            for (Direction dir : Direction.values()) {
                BlockPos adj = pos.relative(dir);
                BlockState adjState = level.getBlockState(adj);
                String blockId = BuiltInRegistries.BLOCK.getKey(adjState.getBlock()).toString();
                if (blockId.equals("simulated:iron_handle")) {
                    level.destroyBlock(adj, false);
                }
            }

            // 2. 复刻 ExplosiveBlockCallback.onHit 的原始逻辑
            PrimedTnt primedTnt = new PrimedTnt(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, null);
            primedTnt.setFuse(4);
            level.addFreshEntity(primedTnt);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
        }
    }
}
