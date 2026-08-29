package io.github.createmeow.timex_rebirth.features;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 未点燃的涂蜡纸板方块实体：仅存储剩余燃烧秒数（FUEL_DATA 附件，单位=秒）。
 * 无燃烧逻辑；被外部热源点燃时由 igniteNeighbors 替换为点燃方块并转移该数据。
 */
public class UnlitWaxedCardboardBlockEntity extends BlockEntity {

    public UnlitWaxedCardboardBlockEntity(BlockPos pos, BlockState state) {
        super(FireToolRegistry.WAXED_CARDBOARD_BE.get(), pos, state);
    }

    /** 读取剩余秒数。 */
    public int remainingSeconds() {
        return Math.max(0, getData(FireManager.FUEL_DATA).getFuelTicks());
    }

    /** 写入剩余秒数。 */
    public void setSeconds(int seconds) {
        getData(FireManager.FUEL_DATA).setFuelTicks(Math.max(0, seconds));
        setChanged();
    }

    /** 造一个保留剩余耐久的未点燃纸板物品（挖掘掉落用）。 */
    public ItemStack toItem() {
        ItemStack drop = new ItemStack(FireToolRegistry.WAXED_CARDBOARD.get());
        int remaining = Math.max(1, remainingSeconds());
        // 剩余耐久 = max - damage；damage = max - remaining
        drop.setDamageValue(drop.getMaxDamage() - remaining);
        return drop;
    }
}
