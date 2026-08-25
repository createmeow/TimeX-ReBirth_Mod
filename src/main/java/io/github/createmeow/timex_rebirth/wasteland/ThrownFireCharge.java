package io.github.createmeow.timex_rebirth.wasteland;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 投掷火焰弹：右键投掷原版火焰弹（minecraft:fire_charge）产生的火球，
 * 外观与恶魂火球类似（直线飞行 + 命中爆炸特效），但伤害固定为 20 点。
 * 命中生物：20 点火焰弹伤害 + 点燃 5 秒；命中方块：点燃降落处方块 +
 * 小规模火焰/爆炸特效，不破坏方块、不额外造成伤害。
 * 引信机制：距发射点 {@link #ARM_DISTANCE} 格内命中方块会直接熄灭（不爆炸），
 * 避免向下/贴近地面发射时炸到发射者脚下。
 * 射程限制：飞出 {@link #MAX_RANGE} 格后无论是否命中方块都强制爆炸，
 * 防止向高处/无方块的天空发射时火球永远不命中、大量堆积导致游戏卡死。
 */
public class ThrownFireCharge extends LargeFireball {
    /** 命中伤害。 */
    public static final float DAMAGE = 20.0F;
    /** 引信距离（格）：距离发射点不足该距离时，命中方块不爆炸、直接熄灭。 */
    public static final float ARM_DISTANCE = 2.5F;
    /** 最大射程（格）：飞出该距离后无论是否命中都直接爆炸。 */
    public static final float MAX_RANGE = 64.0F;

    /** 发射点（用于引信判定）；反序列化（无发射者）时为 null 视为已引信。 */
    private Vec3 spawnPos;

    public ThrownFireCharge(EntityType<? extends LargeFireball> entityType, Level level) {
        super(entityType, level);
    }

    public ThrownFireCharge(Level level, LivingEntity shooter, Vec3 movement) {
        super(level, shooter, movement, 1);
        // 原版 AbstractHurtingProjectile 用 owner.getY()（脚部）作为发射点，
        // 导致向下/贴近地面发射时火球直接贴地、在脚下爆炸。
        // 改为从发射者眼睛高度发射，与投掷视角一致。
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
        this.spawnPos = this.position();
    }

    /**
     * 射程限制：先走父类移动/命中逻辑，若仍存活且距发射点超过最大射程，
     * 强制爆炸（无论是否命中方块），避免向高空发射时火球永不命中、无限堆积。
     * 命中后父类会调用 {@link #onHit} 并 {@link #discard()}，此时实体已移除，
     * <code>isRemoved()</code> 为 true，直接跳过，避免同一帧二次爆炸。
     */
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || this.isRemoved() || this.spawnPos == null) {
            return;
        }
        if (this.spawnPos.distanceToSqr(this.position()) > MAX_RANGE * MAX_RANGE) {
            this.impactEffects(this.position());
            this.discard();
        }
    }

    /** 持久化发射点坐标，供射程判定在重新加载实体后依然可用。 */
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.spawnPos != null) {
            ListTag pos = new ListTag();
            pos.add(DoubleTag.valueOf(this.spawnPos.x));
            pos.add(DoubleTag.valueOf(this.spawnPos.y));
            pos.add(DoubleTag.valueOf(this.spawnPos.z));
            tag.put("SpawnPos", pos);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SpawnPos", Tag.TAG_LIST)) {
            ListTag pos = tag.getList("SpawnPos", Tag.TAG_DOUBLE);
            if (pos.size() == 3) {
                this.spawnPos = new Vec3(pos.getDouble(0), pos.getDouble(1), pos.getDouble(2));
            }
        }
        // 无发射点记录（旧存档/其他来源）时以当前位置为发射点兜底，保证射程判定可用。
        if (this.spawnPos == null) {
            this.spawnPos = this.position();
        }
    }

    /** 是否已引信：命中点距发射点达到引信距离，或无从判断（无发射点）时视为已引信。 */
    private boolean isArmed(Vec3 hitLocation) {
        return this.spawnPos == null || this.spawnPos.distanceToSqr(hitLocation) >= ARM_DISTANCE * ARM_DISTANCE;
    }

    /** 不调用父类 onHit：恶魂火球会附带 1 级爆炸，这里改为固定 20 伤害 + 特效。 */
    @Override
    protected void onHit(HitResult hitResult) {
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            this.onHitEntity((EntityHitResult) hitResult);
        } else if (hitResult.getType() == HitResult.Type.BLOCK) {
            this.onHitBlock((BlockHitResult) hitResult);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        if (this.level().isClientSide) return;
        Entity target = hitResult.getEntity();
        Entity owner = this.getOwner();
        target.hurt(this.damageSources().fireball(this, owner), DAMAGE);
        if (target instanceof LivingEntity livingTarget) {
            livingTarget.igniteForSeconds(5.0F);
        }
        this.impactEffects(hitResult.getLocation());
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        if (this.level().isClientSide) return;
        if (!this.isArmed(hitResult.getLocation())) {
            // 未引信（离发射者过近）：直接熄灭，防止向下/贴近地面发射时炸到发射者脚下
            this.discard();
            return;
        }
        this.igniteLandingBlock(hitResult);
        this.impactEffects(hitResult.getLocation());
        this.discard();
    }

    /** 点燃降落处方块（与原版火焰弹一致）：在命中面外侧放置火焰，若该位置为空。 */
    private void igniteLandingBlock(BlockHitResult hitResult) {
        BlockPos pos = hitResult.getBlockPos().relative(hitResult.getDirection());
        if (this.level().isEmptyBlock(pos)) {
            this.level().setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
        }
    }

    /**
     * 命中特效：爆炸 + 火焰粒子与声响（不破坏方块、不额外造成伤害）。
     * 特效位置使用【命中点】坐标（高速飞行的火球中心会越过命中面，导致向下发射时
     * 爆炸出现在脚下/地下）；爆炸音量 4.0（可闻范围约 64 格，默认 1.0 只有 16 格，
     * 超过 10 格后几乎听不到）。
     */
    private void impactEffects(Vec3 hitLocation) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, hitLocation.x, hitLocation.y, hitLocation.z, 1, 0.0, 0.0, 0.0, 0.0);
            serverLevel.sendParticles(ParticleTypes.FLAME, hitLocation.x, hitLocation.y, hitLocation.z, 24, 0.5, 0.5, 0.5, 0.05);
            serverLevel.playSound(null, hitLocation.x, hitLocation.y, hitLocation.z,
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F, 1.2F);
        }
    }
}
