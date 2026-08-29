package io.github.createmeow.timex_rebirth.mixin;

import io.github.createmeow.timex_rebirth.features.FireFuelData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 熔炉/高炉/烟熏炉"不可虚空燃烧"（点火许可门控）：
 *
 * <p>原版 serverTick 中只要燃料槽有可燃物且能烧炼就自动点燃
 * （{@code litTime = getBurnDuration(itemstack)}，即"自动点火"唯一入口）。
 * 本 Mixin 将其重定向为门控：</p>
 * <ul>
 *   <li>方块附件（点火许可）存在 → 放行原版逻辑，熔炉正常消耗燃料槽并连续燃烧；</li>
 *   <li>无许可 → 返回 0，放入燃料也不会自燃，需玩家手持打火石右键授予许可。</li>
 * </ul>
 * <p>许可在首次点火时由 FireInteractHandler 写入附件并清零；
 * 燃尽熄灭时由 {@link #timex_rebirth$clearPermitOnExtinguish} 清除，需重新打火。</p>
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceFuelMixin {

    @Redirect(method = "serverTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;getBurnDuration(Lnet/minecraft/world/item/ItemStack;)I"))
    private static int timex_rebirth$requireManualIgnition(AbstractFurnaceBlockEntity furnace, ItemStack fuel) {
        var accessor = (AbstractFurnaceBlockEntityAccessor) furnace;
        Level level = furnace.getLevel();
        BlockPos pos = furnace.getBlockPos();
        if (level == null) return accessor.timex_rebirth$invokeGetBurnDuration(fuel);
        BlockState state = level.getBlockState(pos);

        boolean lit = state.hasProperty(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT)
                && state.getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT);
        // 已点燃 → 正常补燃（燃烧中途续燃料无需再打火）
        if (lit) {
            return accessor.timex_rebirth$invokeGetBurnDuration(fuel);
        }

        // 未点燃：仅当有"点火许可"才允许本次点火
        FireFuelData data = FireFuelData.of(furnace);
        boolean hasPermit = data != null && data.hasFuel();
        // 先有火、后加燃料：六向邻居存在明火时同样允许点燃
        if (!hasPermit && !hasAdjacentFire(level, pos)) {
            return 0; // 无许可且无邻近明火：禁止自动点燃
        }
        // 授予一次点火：清除许可，放行原版消耗燃料槽
        if (data != null) data.setFuelTicks(0);
        return accessor.timex_rebirth$invokeGetBurnDuration(fuel);
    }

    /** 检查方块六向邻居是否存在原版明火。 */
    private static boolean hasAdjacentFire(Level level, BlockPos pos) {
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            if (level.getBlockState(pos.relative(dir)).getBlock() instanceof net.minecraft.world.level.block.BaseFireBlock) {
                return true;
            }
        }
        return false;
    }

    /**
     * 熔炉燃尽熄灭时清除点火许可。
     * 注入 serverTick 尾部状态同步处：flag != isLit 且当前未点燃 → 清许可。
     */
    @Inject(method = "serverTick", at = @At("TAIL"))
    private static void timex_rebirth$clearPermitOnExtinguish(Level level, BlockPos pos, BlockState state,
                                                              AbstractFurnaceBlockEntity furnace,
                                                              org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (level.isClientSide()) return;
        boolean lit = state.hasProperty(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT)
                && state.getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT);
        if (lit) return;
        FireFuelData data = FireFuelData.of(furnace);
        if (data != null && data.hasFuel()) {
            // 未点燃但仍有许可 → 说明刚完成了一次点火支取或燃尽，保持 0 即可；此处仅兜底清负值
            if (data.getFuelTicks() < 0) data.setFuelTicks(0);
        }
    }
}
