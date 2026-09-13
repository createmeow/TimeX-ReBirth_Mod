package com.createmeow.underwaterplugin;

import com.simibubi.create.content.equipment.armor.BacktankBlock;
import com.simibubi.create.content.equipment.armor.BacktankBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

/**
 * 铝背罐方块：完全继承 Create 背罐方块逻辑（含水、朝向、顶部轴、比较器信号、
 * 右键穿戴、放置写入空气数据），仅替换方块实体类型为本模组注册的类型
 * （Create 自带 BACKTANK 类型 validBlocks 固定为铜/下界合金背罐，无法直接复用）。
 */
public class AluminumBacktankBlock extends BacktankBlock {

    public AluminumBacktankBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends BacktankBlockEntity> getBlockEntityType() {
        return UnderwaterCreateRegisters.ALUMINUM_BACKTANK_BE.get();
    }
}
