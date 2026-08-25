package io.github.createmeow.timex_rebirth.wasteland;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 爆炸箭物品：弓/弩射出的自定义箭矢（参考"额外扩展" RopeArrowItem 的做法）。
 */
public class ExplosiveArrowItem extends ArrowItem {
    public ExplosiveArrowItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter, @Nullable ItemStack weapon) {
        return new ExplosiveArrow(level, shooter, stack, weapon);
    }

    @Override
    public Projectile asProjectile(Level level, Position position, ItemStack stack, Direction direction) {
        ExplosiveArrow arrow = new ExplosiveArrow(level, position.x(), position.y(), position.z(),
                stack.copyWithCount(1), null);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        return arrow;
    }
}
