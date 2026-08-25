package io.github.createmeow.timex_rebirth.features;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 作物温度 BlockEntity：把"种植时种子的 fiahi 温度"作为方块数据存到作物方块本体上，
 * 收获/破坏作物时读取并回写到掉落的种子上，使"种下→拔起"温度不丢失。
 * 这是专属于作物方块的数据（随存档持久），而非全局外部 Map。
 */
public class CropTempBlockEntity extends BlockEntity {

    private int temperature = 0;

    public CropTempBlockEntity(BlockPos pos, BlockState state) {
        super(CropTempRegistry.CROP_TEMP_TYPE.get(), pos, state);
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        if (this.temperature == temperature) return;
        this.temperature = temperature;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("TimeXTemp", temperature);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = tag.getInt("TimeXTemp");
    }
}
