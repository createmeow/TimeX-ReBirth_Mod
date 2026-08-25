package io.github.createmeow.timex_rebirth.heat.station;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * 燃料接收器方块实体：
 * 暂存物品燃料与熔岩流体，供相邻热源发生器每 5 tick 自动拉取。
 * 无 GUI（纯自动化中转接口）：物品侧接受漏斗 / Create 漏斗，
 * 流体侧接受 Create 管道注入熔岩。
 */
public class HeatFuelReceiverBlockEntity extends BlockEntity {
    private final ItemStackHandler fuelSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return StationFuelUtil.isSolidFuel(stack);
        }
    };
    private final FluidTank lavaTank = new FluidTank(8000, f -> f.getFluid() == Fluids.LAVA) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    public HeatFuelReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(HeatStationRegistry.HEAT_FUEL_RECEIVER_BE.get(), pos, state);
    }

    /** 待机 tick：燃料转移由相邻发生器主动拉取，本实体仅存储。 */
    public void tick() {
    }

    public ItemStackHandler getFuelSlot() {
        return this.fuelSlot;
    }

    public FluidTank getLavaTank() {
        return this.lavaTank;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("fuel", this.fuelSlot.serializeNBT(registries));
        tag.put("lava", this.lavaTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("fuel")) this.fuelSlot.deserializeNBT(registries, tag.getCompound("fuel"));
        if (tag.contains("lava")) this.lavaTank.readFromNBT(registries, tag.getCompound("lava"));
    }
}
