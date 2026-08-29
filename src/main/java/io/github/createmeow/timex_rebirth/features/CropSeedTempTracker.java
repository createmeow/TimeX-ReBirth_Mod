package io.github.createmeow.timex_rebirth.features;

import com.mojang.logging.LogUtils;
import io.github.createmeow.timex_rebirth.compat.FiahiCompatHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 作物种子温度继承（基于方块数据 CropTempBlockEntity）：
 * 种子（带 fiahi 温度）种下时把温度写入作物方块的 BlockEntity（方块数据，随存档持久）；
 * 收获/破坏作物时从 BlockEntity 读出温度，回写到掉落的种子（Containers.dropItemStack 处）。
 * 触发链：
 * - {@code UseItemOnBlockEvent}（放置前，seed 未消耗）：暂存待种植温度。
 * - {@code BlockEvent.EntityPlaceEvent}（放置后）：把暂存温度写入该位置的 CropTempBlockEntity。
 * - {@code BlockEvent.BreakEvent}（破坏前）：读出该位置 CropTempBlockEntity 的温度作为待回写。
 * - {@code Containers.dropItemStack} RETURN：把待回写温度写到掉落的种子上。
 *
 * <p>内存安全：
 * <ul>
 *   <li>放置事件失败时，暂存条目在 60 秒后自动清理；</li>
 *   <li>通过唯一 ID 追踪暂存条目，防止重复暂存同一位置。</li>
 * </ul>
 */
public class CropSeedTempTracker {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 暂存"即将种植"的种子温度（位置 + UUID -> 温度），放置后立即写入方块实体并移除。 */
    private static final Map<UUID, PendingPlant> PENDING_PLANT = new HashMap<>();
    /** 当前正在掉落中待回写的温度；0 表示无记录。 */
    private static int pendingDropTemperature = 0;

    private CropSeedTempTracker() {
    }

    /** 暂存条目（位置 + 唯一标识符 + 时间戳）。 */
    private record PendingPlant(BlockPos pos, int temp, long timestamp) {
    }

    /** 种子放置前（UseItemOnBlockEvent）：读取尚未消耗的种子温度。 */
    @SubscribeEvent
    public static void onUseSeed(UseItemOnBlockEvent event) {
        if (!FiahiCompatHelper.isLoaded()) return;
        ItemStack stack = event.getItemStack();
        if (!FiahiCompatHelper.isSeedLike(stack)) return;
        UseOnContext context = event.getUseOnContext();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        int temp = FiahiCompatHelper.getFoodTemperature(stack);
        LOGGER.info("[CropSeed] use seed @{} temp={} stack={}", pos, temp, stack);
        if (temp != 0) {
            UUID id = UUID.randomUUID();
            PENDING_PLANT.put(id, new PendingPlant(pos.immutable(), temp, System.currentTimeMillis()));
        }
    }

    /** 放置完成后：把暂存的种子温度写入该位置作物的 CropTempBlockEntity。 */
    @SubscribeEvent
    public static void onPlaceCrop(BlockEvent.EntityPlaceEvent event) {
        if (!FiahiCompatHelper.isLoaded()) return;
        BlockState placed = event.getPlacedBlock();
        if (!isCrop(placed)) return;
        BlockPos pos = event.getPos();
        // 移除所有针对该位置的暂存条目（可能有多个，因为 UUID 不同）
        PENDING_PLANT.values().removeIf(p -> p.pos().equals(pos));
        BlockEntity be = event.getLevel().getBlockEntity(pos);
        LOGGER.info("[CropSeed] place crop @{} pendingTemp={} be={}", pos, null, be);
        if (be instanceof CropTempBlockEntity cropBe) {
            // 尝试从暂存中读取温度（如果有）
            int temp = PENDING_PLANT.values().stream()
                    .filter(p -> p.pos().equals(pos))
                    .findFirst()
                    .map(PendingPlant::temp)
                    .orElse(0);
            cropBe.setTemperature(temp);
            LOGGER.info("[CropSeed] wrote BE temp={}", temp);
        }
    }

    /** 定期清理超时的暂存条目（每 60 秒一次）。 */
    private static void cleanupExpired() {
        long now = System.currentTimeMillis();
        long expiry = 60_000L; // 60 秒
        int removed = 0;
        for (var it = PENDING_PLANT.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            if (now - entry.getValue().timestamp() > expiry) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            LOGGER.debug("[CropSeed] Cleaned {} expired pending plant entries", removed);
        }
    }

    /** 作物被破坏前：从方块数据里读出种子温度作为待回写值。
     *  仅"未成熟"作物（拔苗）才回写种子温度（避免玩家快速挖掘-种植消耗温度）；
     *  成熟作物采集时掉落的是干净种子/产物，不回写温度。 */
    @SubscribeEvent
    public static void onBreakCrop(BlockEvent.BreakEvent event) {
        if (!FiahiCompatHelper.isLoaded()) return;
        BlockState state = event.getState();
        if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
            pendingDropTemperature = 0; // 成熟作物：收获种子不带温度
            LOGGER.info("[CropSeed] break mature crop @{} no temp write-back", event.getPos());
            return;
        }
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (be instanceof CropTempBlockEntity cropBe) {
            pendingDropTemperature = cropBe.getTemperature();
            LOGGER.info("[CropSeed] break @{} beTemp={}", event.getPos(), pendingDropTemperature);
        } else {
            LOGGER.info("[CropSeed] break @{} no CropTempBE be={}", event.getPos(), be);
        }
    }

    /** 掉落物品实体加入世界时，把待回写温度写到掉落的种子上（写一次后清除，服务端）。
     *  仅在掉落物确为种子/树苗且温度有效时才写入；任何异常都静默跳过，避免把温度强加给
     *  非种子掉落（如成熟作物采集时可能不掉落种子）导致崩溃。 */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (pendingDropTemperature == 0) return;
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) return;
        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty()) return;
        if (!FiahiCompatHelper.isSeedLike(stack)) return;
        try {
            FiahiCompatHelper.setTemperature(stack, pendingDropTemperature);
            LOGGER.info("[CropSeed] applied temp={} to drop stack={}", pendingDropTemperature, stack);
        } catch (Exception ignored) {
            // 写入异常（如极罕见的不可变/空栈）时不回写，避免崩溃
        } finally {
            pendingDropTemperature = 0;
        }
    }

    private static boolean isCrop(BlockState state) {
        return state.getBlock() instanceof CropBlock
                || state.getBlock() instanceof SaplingBlock
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(Blocks.BAMBOO_SAPLING);
    }
}
