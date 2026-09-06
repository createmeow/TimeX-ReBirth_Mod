package io.github.createmeow.timex_rebirth.features.flint;

import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.compat.FiahiCompatHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 可疑积雪清刷处理器（服务端 PlayerTickEvent 驱动）。
 * <p>
 * 原版 {@code BrushItem.onUseTick} 会调用 {@code BrushableBlockEntity.brush()}，但该调用链
 * 依赖 {@code level.getBlockEntity(pos) instanceof BrushableBlockEntity} 检查，在某些环境下
 * 会因方块实体缺失而跳过，导致清刷逻辑永远不执行。
 * <p>
 * 本处理器改为在服务端 tick 中独立追踪：当玩家手持刷子长按右键（{@code isUsingItem}）且
 * 准星对准可疑积雪时，每持续 2 秒（40 tick）提升一级 DUSTED；满级后掉落战利品并转为普通积雪。
 * 原版刷子动画/粒子/音效仍由 {@code BrushItem.onUseTick} 正常播放。
 */
@EventBusSubscriber(modid = TimeX.MODID)
public class SuspiciousSnowBrushHandler {

    /** 每提升一级清刷进度所需的持续清刷时长（tick，30 = 1.5 秒） */
    private static final int BRUSH_TICKS_PER_LEVEL = 30;
    /** DUSTED 满级值（0→1→2→3，到达 3 即完成） */
    private static final int MAX_DUSTED = 3;

    private static final Map<UUID, BrushSession> SESSIONS = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        tickPlayer(player);
    }

    private static void tickPlayer(ServerPlayer player) {
        // 必须正在使用物品（长按右键）
        if (!player.isUsingItem()) {
            clearSession(player);
            return;
        }
        // 必须手持刷子
        if (!isHoldingBrush(player)) {
            clearSession(player);
            return;
        }

        // 服务端射线检测玩家准星指向的方块
        HitResult hitResult = player.pick(player.blockInteractionRange(), 0.0F, false);
        if (!(hitResult instanceof BlockHitResult blockHit) || hitResult.getType() != HitResult.Type.BLOCK) {
            clearSession(player);
            return;
        }

        BlockPos pos = blockHit.getBlockPos();
        BlockState state = player.level().getBlockState(pos);
        if (!(state.getBlock() instanceof SuspiciousSnowBlock)) {
            clearSession(player);
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        long currentTick = level.getGameTime();
        int dusted = state.getValue(BlockStateProperties.DUSTED);

        // 获取或创建清刷会话
        BrushSession session = SESSIONS.get(player.getUUID());
        if (session == null || !session.pos.equals(pos) || session.dustedAtStart != dusted) {
            session = new BrushSession(pos, currentTick, dusted);
            SESSIONS.put(player.getUUID(), session);
            TimeX.LOGGER.info("[SuspiciousSnow] 开始清刷 pos={} dusted={}", pos, dusted);
            return;
        }

        // 判断是否已持续清刷足够时长
        long elapsed = currentTick - session.lastIncrementTick;
        if (elapsed < BRUSH_TICKS_PER_LEVEL) return;

        int next = dusted + 1;
        if (next > MAX_DUSTED) {
            // 已在 dusted=3 后继续刷够一段时间 → 完成：掉落战利品并转为普通积雪
            TimeX.LOGGER.info("[SuspiciousSnow] 清刷完成 pos={}", pos);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SuspiciousSnowBlockEntity snowBE) {
                snowBE.complete(level, player);
            } else {
                completeFallback(level, pos, state, player);
            }
            damageBrush(player);
            clearSession(player);
        } else {
            TimeX.LOGGER.info("[SuspiciousSnow] 提升 dusted {} -> {} pos={}", dusted, next, pos);
            level.setBlock(pos, state.setValue(BlockStateProperties.DUSTED, next), Block.UPDATE_CLIENTS);
            session.lastIncrementTick = currentTick;
            session.dustedAtStart = next;
        }
    }

    /** 方块实体缺失时的兜底完成逻辑 */
    private static void completeFallback(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        if (state.getBlock() instanceof SuspiciousSnowBlock snow) {
            level.playSound(null, pos, snow.getBrushCompletedSound(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        ItemStack tool = player.getMainHandItem().is(Items.BRUSH)
                ? player.getMainHandItem()
                : player.getOffhandItem();
        var lootTable = level.getServer().reloadableRegistries()
                .getLootTable(FlintGearRegistry.SUSPICIOUS_SNOW_LOOT);
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
                        net.minecraft.world.phys.Vec3.atCenterOf(pos))
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, state)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, tool)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK);
        // 层数决定掉落次数：1-3层1种、4-5层2种、6-8层3种
        int layers = state.getValue(BlockStateProperties.LAYERS);
        int drops = layers <= 3 ? 1 : layers <= 5 ? 2 : 3;
        for (int i = 0; i < drops; i++) {
            lootTable.getRandomItems(params, is -> {
                if (FiahiCompatHelper.isSeedLike(is)) {
                    FiahiCompatHelper.setTemperature(is, -(40 + level.getRandom().nextInt(41)));
                }
                Block.popResource(level, pos, is);
            });
        }
        level.setBlockAndUpdate(pos,
                net.minecraft.world.level.block.Blocks.SNOW.defaultBlockState()
                        .setValue(BlockStateProperties.LAYERS, layers));
    }

    private static boolean isHoldingBrush(ServerPlayer player) {
        return player.getMainHandItem().is(Items.BRUSH)
                || player.getOffhandItem().is(Items.BRUSH);
    }

    private static void damageBrush(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        if (mainHand.is(Items.BRUSH)) {
            mainHand.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        } else if (offHand.is(Items.BRUSH)) {
            offHand.hurtAndBreak(1, player, EquipmentSlot.OFFHAND);
        }
    }

    private static void clearSession(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
    }

    private static class BrushSession {
        final BlockPos pos;
        long lastIncrementTick;
        int dustedAtStart;

        BrushSession(BlockPos pos, long startTick, int dustedAtStart) {
            this.pos = pos;
            this.lastIncrementTick = startTick;
            this.dustedAtStart = dustedAtStart;
        }
    }
}
