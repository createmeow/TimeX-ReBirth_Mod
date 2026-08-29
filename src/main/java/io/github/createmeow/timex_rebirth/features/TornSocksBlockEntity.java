package io.github.createmeow.timex_rebirth.features;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 破袜子方块实体：存储剩余耐久（单位=点，初始值等同物品最大耐久）。
 */
public class TornSocksBlockEntity extends BlockEntity {

    public TornSocksBlockEntity(BlockPos pos, BlockState state) {
        super(FireToolRegistry.TORN_SOCKS_BE.get(), pos, state);
    }

    /** 当前剩余耐久点数。 */
    public int remainingDurability() {
        return getData(FireManager.FUEL_DATA).getFuelTicks();
    }

    /** 设置剩余耐久（>=0）。 */
    public void setDurability(int value) {
        getData(FireManager.FUEL_DATA).setFuelTicks(Math.max(0, value));
        setChanged();
    }

    /** 从放置物品同步耐久。 */
    public void syncFromItem(ItemStack stack) {
        int remaining = stack.getMaxDamage() - stack.getDamageValue();
        getData(FireManager.FUEL_DATA).setFuelTicks(Math.max(0, remaining));
        setChanged();
    }

    /** 服务端 ticker：耐久为 0 时自动移除方块。 */
    public static void serverTick(Level level, BlockPos pos, BlockState state, TornSocksBlockEntity be) {
        if (level.isClientSide()) return;
        if (be.remainingDurability() <= 0) {
            level.removeBlock(pos, false);
        }
    }
}