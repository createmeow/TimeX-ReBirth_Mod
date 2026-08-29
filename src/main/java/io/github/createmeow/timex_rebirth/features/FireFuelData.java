package io.github.createmeow.timex_rebirth.features;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * 篝火 / 农夫乐事炉灶·厨锅的"燃料时长"存储。
 *
 * <p>设计：原版篝火与 FD 炉灶点燃后永久燃烧（无燃料概念）。本模组通过附件
 * {@link FireManager#FUEL_DATA} 为这些方块实体附加剩余燃烧 tick：
 * <ul>
 *   <li>放置时燃料为 0 → 下一 tick 自动熄灭（"不可虚空燃烧"）。</li>
 *   <li>手持可燃物（由熔炉燃料表判定）右键 → 每次消耗 1 个，累加其燃烧时长。</li>
 *   <li>燃烧中每 tick 扣 1；归零即熄灭，需重新填充并打火点燃。</li>
 * </ul>
 * 打火（点燃）逻辑见 {@link FireManager}：仅当燃料 &gt; 0 时允许点燃。
 */
public class FireFuelData implements INBTSerializable<CompoundTag> {
    /** 剩余燃烧 tick。 */
    private int fuelTicks;

    public int getFuelTicks() {
        return fuelTicks;
    }

    public void setFuelTicks(int ticks) {
        this.fuelTicks = Math.max(0, ticks);
    }

    public void addFuel(int ticks) {
        this.fuelTicks = Math.max(0, this.fuelTicks + ticks);
    }

    public void tickDown() {
        if (fuelTicks > 0) fuelTicks--;
    }

    public boolean hasFuel() {
        return fuelTicks > 0;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("FuelTicks", fuelTicks);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.fuelTicks = tag.getInt("FuelTicks");
    }

    // ── 工具方法 ──

    /** 读取方块实体的燃料数据附件（不存在则返回 null）。 */
    public static FireFuelData of(BlockEntity be) {
        return be.getData(FireManager.FUEL_DATA);
    }

    /** 方块是否处于点燃状态（有 LIT 属性且为 true）。 */
    public static boolean isLit(BlockState state) {
        return state.hasProperty(BlockStateProperties.LIT)
                && state.getValue(BlockStateProperties.LIT);
    }

    /** 若方块有 LIT 属性则设置之；返回是否成功设置。 */
    public static boolean setLit(Level level, BlockPos pos, BlockState state, boolean lit) {
        if (!state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)) return false;
        level.setBlock(pos, state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, lit), 11);
        return true;
    }
}
