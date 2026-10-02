package io.github.createmeow.timex_rebirth.features.flint;

import com.simibubi.create.content.equipment.armor.BacktankUtil;
import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 清刷机：Create 背罐气动工具（仿土豆加农炮的供能模式），
 * 一次清刷视角 120° 锥形（半角 60°）、前方 3 格内的积雪与可疑积雪：
 * <ul>
 * <li>普通积雪（雪层/雪块）→ 直接移除，<b>不掉落雪球</b></li>
 * <li>可疑积雪 → 完成清刷：按积雪层数掉落对应战利品（1-3 层 1 种、4-5 层 2 种、6-8 层 3 种），
 *     并转为普通积雪层——同样不掉雪球</li>
 * </ul>
 * 供能（单位：背罐空气点）：每次清刷消耗 {@code 1 + 积雪类方块数/5 + 清刷伤害×命中实体数/3}。
 * 穿戴 Create 背罐（含铝背罐）时消耗背罐空气；背罐空气不足或无背罐时按
 * 1 耐久 ≈ 4 空气点（土豆加农炮 maxAir/MAX_USES 折算）损耗自身耐久。
 */
public class SnowSweeperItem extends Item {

    /** 自身耐久对应的清刷次数（同为背罐供能时的 1 空气点折算使用次数，与土豆加农炮 maxPotatoCannonShots 同语义） */
    public static final int MAX_USES = 200;
    /** 视角 120° 锥形 → 半角 60°，cos(60°) = 0.5 */
    private static final double HALF_ANGLE_COS = 0.5;
    /** 土豆加农炮折算：1 点耐久 ≈ maxAir/MAX_USES ≈ 4 点背罐空气 */
    private static final int AIR_PER_DURABILITY = 4;
    /** 每次清刷的冷却（tick）：0.2 秒 */
    private static final int COOLDOWN_TICKS = 4;
    /** 每次清刷喷射的白雾粒子数量（参考呼出冷气，量更大） */
    private static final int MIST_PARTICLES = 30;
    /** 嘴部（粒子喷射源）相对眼睛的下移量 */
    private static final double MIST_MOUTH_OFFSET = 0.6;

    public SnowSweeperItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // 潜行右键：打开配置菜单（放行给 use()），不清刷
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        // 右键方块（如对准雪面）同样触发清刷（土豆加农炮同款重定向）
        return use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        // 潜行 + 右键：打开配置菜单（客户端本地打开，CONSUME 不发包）
        if (player.isShiftKeyDown()) {
            if (level.isClientSide()) {
                SweeperConfigScreen.open(player);
                return InteractionResultHolder.consume(held);
            }
            return InteractionResultHolder.success(held);
        }
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(held);
        if (level.isClientSide()) return InteractionResultHolder.success(held);

        boolean clearSnow = SweeperConfig.clearSnow(held);
        boolean clearLeaves = SweeperConfig.clearLeaves(held);
        boolean clearPlants = SweeperConfig.clearPlants(held);
        boolean damageEntities = SweeperConfig.damageEntities(held);
        boolean preventWaste = SweeperConfig.preventWaste(held);
        double range = SweeperConfig.range(held);
        float damageAmount = SweeperConfig.damageAmount(held);

        ServerLevel server = (ServerLevel) level;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        int minX = (int) Math.floor(eye.x - range), maxX = (int) Math.floor(eye.x + range);
        int minY = (int) Math.floor(eye.y - range), maxY = (int) Math.floor(eye.y + range);
        int minZ = (int) Math.floor(eye.z - range), maxZ = (int) Math.floor(eye.z + range);

        int swept = 0;
        int snowSwept = 0;
        double rangeSqr = range * range;
        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            Vec3 toBlock = Vec3.atCenterOf(pos).subtract(eye);
            if (toBlock.lengthSqr() > rangeSqr) continue;
            // 120° 视角锥形判定（视线夹角 ≤ 60°）
            if (toBlock.normalize().dot(look) < HALF_ANGLE_COS) continue;

            BlockState state = server.getBlockState(pos);
            int result = sweepBlock(server, pos.immutable(), state, player, held, clearSnow, clearLeaves, clearPlants);
            if (result > 0) {
                swept++;
                if (result == SWEEP_SNOW) snowSwept++;
            }
        }

        // 清扫防浪费（默认开）：范围内无可清扫方块时不执行清刷
        if (swept == 0 && preventWaste) return InteractionResultHolder.pass(held);

        // 允许浪费（防浪费关闭）时：空刷也播放音效与白雾粒子
        server.playSound(null, player.blockPosition(), SoundEvents.SNOW_BREAK,
                SoundSource.PLAYERS, 0.7F, 1.0F);
        spawnMistParticles(server, player);

        // 清刷声吸引怪物：48 格内的僵尸与 mutanter 怪物将清刷玩家设为攻击目标
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.createmeow.cm_plugins.ActivityAttractionHandler.attractAround(server, serverPlayer, 48.0);
        }
        if (swept > 0) {
            TimeX.LOGGER.debug("[SnowSweeper] 清刷 {} 个方块 (其中积雪 {}): {}", swept, snowSwept, player.getName().getString());
        }

        // 对实体造成伤害：清刷执行时对锥形范围内实体造成可配置伤害（不含玩家自身），返回命中数
        int hitCount = 0;
        if (damageEntities && damageAmount > 0) {
            var targets = server.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                    net.minecraft.world.phys.AABB.ofSize(eye, range * 2, range * 2, range * 2),
                    e -> e != player && e.isAlive() && !e.isSpectator());
            for (var target : targets) {
                Vec3 toEntity = target.getEyePosition().subtract(eye);
                if (toEntity.lengthSqr() > rangeSqr) continue;
                if (toEntity.normalize().dot(look) < HALF_ANGLE_COS) continue;
                target.hurt(server.damageSources().playerAttack(player), damageAmount);
                hitCount++;
            }
        }

        // 供能消耗：1 + 清刷掉的积雪类方块/5 + 清刷伤害×实体数/3（单位：背罐空气点）
        int cost = 1 + (int) Math.ceil(snowSwept / 5.0) + (int) Math.ceil(damageAmount * hitCount / 3.0);
        if (!player.isCreative()) {
            var backtanks = BacktankUtil.getAllWithAir(player);
            if (!backtanks.isEmpty()) {
                int totalAir = 0;
                for (var bt : backtanks) totalAir += BacktankUtil.getAir(bt);
                if (totalAir >= cost) {
                    int remaining = cost;
                    for (var bt : backtanks) {
                        int take = Math.min(BacktankUtil.getAir(bt), remaining);
                        BacktankUtil.consumeAir(player, bt, take);
                        remaining -= take;
                        if (remaining <= 0) break;
                    }
                } else {
                    // 背罐空气不足：按 1 耐久 ≈ 4 空气点折算损耗自身耐久
                    held.hurtAndBreak(Math.max(1, cost / AIR_PER_DURABILITY), player, LivingEntity.getSlotForHand(hand));
                }
            } else {
                held.hurtAndBreak(Math.max(1, cost / AIR_PER_DURABILITY), player, LivingEntity.getSlotForHand(hand));
            }
        }
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.success(held);
    }

    /** 清刷结果：无作用 / 非积雪目标（叶/草）/ 积雪类目标（雪层/雪块/可疑积雪）。 */
    private static final int SWEEP_NONE = 0;
    private static final int SWEEP_OTHER = 1;
    private static final int SWEEP_SNOW = 2;

    /** 清刷单个方块，返回 SWEEP_* 结果（按配置菜单开关分桶）。 */
    private int sweepBlock(ServerLevel level, BlockPos pos, BlockState state, Player player, ItemStack tool,
                           boolean clearSnow, boolean clearLeaves, boolean clearPlants) {
        Block block = state.getBlock();

        // ── 清理落叶：IW 叶堆全集（leaf_piles 标签）→ 直接移除，不掉落 ──
        if (clearLeaves && state.is(SweeperConfig.LEAF_PILES)) {
            removeWithParticles(level, pos, state);
            return SWEEP_OTHER;
        }
        // ── 清理杂草/花：IW grass_spread_source 标签 + 结霜的草/蕨 → 直接移除，不掉落 ──
        if (clearPlants && SweeperConfig.isPlantBlock(state)) {
            removeWithParticles(level, pos, state);
            return SWEEP_OTHER;
        }
        if (!clearSnow) return SWEEP_NONE;

        // ── 清理积雪 ──
        // 普通积雪：雪层 / 雪块 → 直接移除，不掉落雪球
        if (block instanceof SnowLayerBlock || block == Blocks.SNOW_BLOCK) {
            removeWithParticles(level, pos, state);
            return SWEEP_SNOW;
        }
        // 可疑积雪：按层数掉落战利品并转为普通积雪层
        if (block instanceof SuspiciousSnowBlock) {
            // 可疑积雪同样喷积雪破坏粒子（基于其当前方块状态）
            level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                            net.minecraft.core.particles.ParticleTypes.BLOCK, state),
                    pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                    8, 0.25, 0.25, 0.25, 0.15);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SuspiciousSnowBlockEntity snowBE) {
                snowBE.complete(level, player);
            } else {
                SuspiciousSnowBrushHandler.completeFallback(level, pos, state, player, tool);
            }
            return SWEEP_SNOW;
        }
        return SWEEP_NONE;
    }

    /** 直接移除方块（不掉落）并喷对应方块的破碎粒子。 */
    private void removeWithParticles(ServerLevel level, BlockPos pos, BlockState state) {
        level.removeBlock(pos, false);
        level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                        net.minecraft.core.particles.ParticleTypes.BLOCK, state),
                pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                8, 0.25, 0.25, 0.25, 0.15);
    }

    /**
     * 喷射白雾粒子（参考 Freeze-It-And-Heat-It 玩家呼出冷气，按需求调整）：
     * 源点下移 0.6（胸口/嘴部高度）、随机散布更大、一次 30 个粒子、向外喷射速度更快。
     * 使用原版 CLOUD 白雾粒子 + 服务端速度模式广播（count=0 时 dx/dy/dz 即初速度）。
     */
    private void spawnMistParticles(ServerLevel level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        // 喷射源：眼前 0.6 格、下移 0.6（嘴部高度）
        double mx = eye.x + look.x * 0.6;
        double my = eye.y - MIST_MOUTH_OFFSET + look.y * 0.6;
        double mz = eye.z + look.z * 0.6;
        var random = player.getRandom();

        for (int i = 0; i < MIST_PARTICLES; i++) {
            // 随机散布（±0.7）
            double sx = (random.nextFloat() - 0.5) * 1.4;
            double sy = (random.nextFloat() - 0.5) * 1.4;
            double sz = (random.nextFloat() - 0.5) * 1.4;
            // 沿视线向外喷射（速度约 0.45~0.7，呼气原版 0.15 的 3~4 倍）+ 随机扰动
            Vec3 vel = look.scale(0.45 + random.nextFloat() * 0.25)
                    .add((random.nextFloat() - 0.5) * 0.2,
                         (random.nextFloat() - 0.5) * 0.2,
                         (random.nextFloat() - 0.5) * 0.2);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                    mx + sx, my + sy, mz + sz, 0, vel.x, vel.y, vel.z, 1.0);
        }
    }

    // ── 耐久条：显示背罐空气 / 自身耐久折算的剩余可用次数（土豆加农炮同款） ──

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return BacktankUtil.isBarVisible(stack, MAX_USES);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return BacktankUtil.getBarWidth(stack, MAX_USES);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BacktankUtil.getBarColor(stack, MAX_USES);
    }
}
