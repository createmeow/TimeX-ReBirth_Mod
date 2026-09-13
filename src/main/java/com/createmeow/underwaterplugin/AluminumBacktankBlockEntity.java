package com.createmeow.underwaterplugin;

import com.simibubi.create.content.equipment.armor.BacktankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 铝背罐方块实体：继承 Create 背罐方块实体（轴动力充气、NBT 读写、组件补丁）。
 * 父类默认名称硬编码为铜背罐，这里在构造时用自定义名称覆盖。
 */
public class AluminumBacktankBlockEntity extends BacktankBlockEntity {

    public AluminumBacktankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // 覆盖父类的默认名（铜背罐）；玩家铁砧改名场景不适用背罐，无副作用
        setCustomName(Component.translatable("block.underwater_plugin.aluminum_backtank"));
    }
}
