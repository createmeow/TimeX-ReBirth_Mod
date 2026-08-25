package io.github.createmeow.timex_rebirth.wasteland;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 爆炸箭：命中方块或生物时在【命中点】产生爆炸，箭矢本身随之消失、不可拾取。
 * - 爆炸位置使用命中点的坐标（hitResult.getLocation()），而非箭矢自身中心——
 *   高速飞行/向下射击时箭矢中心可能偏离实际命中面，导致爆炸位置不准、目标吃不到伤害。
 * - 威力：半径 1.0（普通 TNT 半径 4.0 的四分之一，破坏体积约 1/64）。
 *   自定义箭矢做法参考"额外扩展"模组的 RopeArrow。
 */
public class ExplosiveArrow extends AbstractArrow {
    /** 爆炸半径（格）。 */
    public static final float EXPLOSION_RADIUS = 1.0F;

    public ExplosiveArrow(EntityType<? extends AbstractArrow> entityType, Level level) {
        super(entityType, level);
        this.pickup = Pickup.DISALLOWED;
    }

    public ExplosiveArrow(Level level, LivingEntity shooter, ItemStack stack, @Nullable ItemStack weapon) {
        super(WastelandRegistry.EXPLOSIVE_ARROW_ENTITY.get(), shooter, level, stack, weapon);
        this.pickup = Pickup.DISALLOWED;
    }

    public ExplosiveArrow(Level level, double x, double y, double z, ItemStack stack, @Nullable ItemStack weapon) {
        super(WastelandRegistry.EXPLOSIVE_ARROW_ENTITY.get(), x, y, z, level, stack, weapon);
        this.pickup = Pickup.DISALLOWED;
    }

    /** 爆炸箭不可拾取（命中即爆炸消失）。 */
    @Override
    protected ItemStack getDefaultPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        explode(hitResult.getLocation());
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        explode(hitResult.getLocation());
    }

    /** 在命中点产生爆炸（命中点坐标比箭矢中心更准确，避免向下射击时爆炸偏移）。 */
    private void explode(Vec3 hitLocation) {
        if (level().isClientSide) return;
        level().explode(this, hitLocation.x, hitLocation.y, hitLocation.z, EXPLOSION_RADIUS, Level.ExplosionInteraction.BLOCK);
        discard();
    }
}
