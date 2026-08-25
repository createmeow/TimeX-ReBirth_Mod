package io.github.createmeow.timex_rebirth.heat.station;

import io.github.createmeow.timex_rebirth.TimeXConfig;
import io.github.createmeow.timex_rebirth.heat.HeatRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * 热源适配器方块实体：
 * 存储相邻热源发生器产出的热流，并对外暴露流体能力，
 * 供 Create 流体管道抽取输送到热源接收器。
 */
public class HeatAdapterBlockEntity extends BlockEntity {
    private final FluidTank heatTank = new FluidTank(TimeXConfig.HEAT_ADAPTER_CAPACITY.get(),
            f -> f.getFluid() == HeatRegistry.HEAT_FLUX.get()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    public HeatAdapterBlockEntity(BlockPos pos, BlockState state) {
        super(HeatStationRegistry.HEAT_ADAPTER_BE.get(), pos, state);
    }

    /** 待机 tick：热流注入由相邻发生器主动推送，本实体仅存储与对外暴露。 */
    public void tick() {
    }

    public FluidTank getHeatTank() {
        return this.heatTank;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("heat", this.heatTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("heat")) this.heatTank.readFromNBT(registries, tag.getCompound("heat"));
    }
}
