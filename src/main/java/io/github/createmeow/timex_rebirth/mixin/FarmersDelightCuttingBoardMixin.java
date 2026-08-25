package io.github.createmeow.timex_rebirth.mixin;

import io.github.createmeow.timex_rebirth.compat.FiahiCompatHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vectorwing.farmersdelight.common.block.entity.CuttingBoardBlockEntity;
import vectorwing.farmersdelight.common.utility.ItemUtils;

/**
 * fiahi 联动：食品加工保留温度（农夫乐事砧板）。
 * 玩家用工具处理砧板上物品（切菜）时，把砧板上原料携带的 fiahi 温度（腐烂/冻结程度）
 * 复制到产出的掉落物，使"冷冻/腐烂食材 → 砧板切块"不再丢失腐烂/冻结程度。
 * <p>
 * FD 的 {@link CuttingBoardBlockEntity#processStoredItemUsingTool} 中产物生成与掉落
 * （rollResults / ItemUtils.spawnItemEntity / inventory.extractItem）都位于
 * {@code matchingRecipe.ifPresent} 的 lambda 合成方法内，而 Mixin 不支持对 lambda 合成方法注入，
 * 故改在稳定的静态方法 {@link ItemUtils#spawnItemEntity} 的入口处拦截：
 * 产物掉落前，从掉落位置附近的砧板读取原料温度复制到产物。
 * 此时砧板原料仍在槽 0（extractItem 在该方法调用之后才执行），仅有带温度原料的砧板才生效。
 * <p>
 * 注意：本类名含 FarmersDelight，由 TimeXMixinPlugin 按 farmersdelight 是否加载门控。
 */
@Mixin(ItemUtils.class)
public abstract class FarmersDelightCuttingBoardMixin {

    @Inject(method = "spawnItemEntity", at = @At("HEAD"))
    private static void timex_rebirth$inheritCuttingTemperature(Level level, ItemStack stack,
                                                                double x, double y, double z,
                                                                double xMotion, double yMotion, double zMotion,
                                                                CallbackInfo ci) {
        CuttingBoardBlockEntity board = findNearbyBoard(level, x, y, z);
        if (board == null) return;
        ItemStack ingredient = board.getStoredItem();
        if (FiahiCompatHelper.getFoodTemperature(ingredient) != 0) {
            FiahiCompatHelper.copyTemperature(ingredient, stack);
        }
    }

    /** 在 spawn 位置周围找砧板（产物朝砧板朝向偏移 0.2，可能落在邻格），找不到返回 null。 */
    private static CuttingBoardBlockEntity findNearbyBoard(Level level, double x, double y, double z) {
        if (level == null) return null;
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockEntity be = level.getBlockEntity(mutable.set(pos.getX() + dx, pos.getY(), pos.getZ() + dz));
                if (be instanceof CuttingBoardBlockEntity board) {
                    return board;
                }
            }
        }
        return null;
    }
}
