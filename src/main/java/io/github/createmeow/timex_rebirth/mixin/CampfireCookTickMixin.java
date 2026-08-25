package io.github.createmeow.timex_rebirth.mixin;

import io.github.createmeow.timex_rebirth.TimeXConfig;
import io.github.createmeow.timex_rebirth.compat.FiahiCompatHelper;
import io.github.createmeow.timex_rebirth.compat.ImmersiveWeatheringCompat;
import io.github.createmeow.timex_rebirth.features.SeedBonusHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 营火烧炼联动：
 * - 烧炼冻土完成（掉落泥土产物）时，小概率额外掉落随机作物种子；
 * - fiahi 联动：把正在烧炼的原料携带的温度（腐烂/冻结程度）保留到产物，
 *   避免营火加工后腐烂/冻结程度直接消失（如冷冻面团烤成面包仍是"冷冻面包"）。
 * 在 cookTick 的 Containers.dropItemStack 调用点重定向，无需局部变量捕获。
 */
@Mixin(CampfireBlockEntity.class)
public abstract class CampfireCookTickMixin {

    @Redirect(method = "cookTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/Containers;dropItemStack(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V"))
    private static void timex_rebirth$seedBonus(Level level, double x, double y, double z, ItemStack stack) {
        // fiahi 温度保留：营火内正在烧炼原料的温度复制到即将掉落的产物（dropItemStack 触发时输入尚未清空）
        BlockPos pos = BlockPos.containing(x, y, z);
        if (level.getBlockEntity(pos) instanceof CampfireBlockEntity campfire) {
            for (ItemStack item : campfire.getItems()) {
                if (FiahiCompatHelper.getFoodTemperature(item) != 0) {
                    FiahiCompatHelper.copyTemperature(item, stack);
                    break;
                }
            }
        }
        Containers.dropItemStack(level, x, y, z, stack);

        // 检查该营火是否有冻土正在烧炼（dropItemStack 触发时输入尚未清空）
        if (level.getBlockEntity(pos) instanceof CampfireBlockEntity campfire) {
            boolean hasPermafrost = false;
            for (ItemStack item : campfire.getItems()) {
                if (ImmersiveWeatheringCompat.isPermafrostItem(item.getItem())) {
                    hasPermafrost = true;
                    break;
                }
            }
            if (hasPermafrost && level.getRandom().nextDouble() < TimeXConfig.PERMAFROST_SEED_CHANCE.get()) {
                ItemStack seed = SeedBonusHelper.getRandomSeed(level.getRandom());
                if (!seed.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, seed);
                }
            }
        }
    }
}
