package io.github.createmeow.timex_rebirth.mixin;

import io.github.createmeow.timex_rebirth.features.CropTempBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 让所有的 CropBlock 作物成为可携带 BlockEntity 的方块（用于存储种子温度）。
 * 通过 {@link EntityBlock#newBlockEntity} 为作物方块提供 {@link CropTempBlockEntity}。
 * 与 CropTempRegistry 绑定的 BlockEntityType 配合：只有属于该类型的作物才会真正生成 BlockEntity。
 */
@Mixin(CropBlock.class)
public abstract class CropBlockMixin implements EntityBlock {

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CropTempBlockEntity(pos, state);
    }
}
