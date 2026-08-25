package io.github.createmeow.timex_rebirth.mixin;

import io.github.createmeow.timex_rebirth.features.CropTempBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 让所有 SaplingBlock 树苗成为可携带 BlockEntity 的方块（用于存储种子温度）。
 * 与 CropTempRegistry 绑定的 BlockEntityType 配合：属于该类型的树苗放置后生成 BlockEntity，
 * 从而种下→拔起时能保留种子温度。
 */
@Mixin(SaplingBlock.class)
public abstract class SaplingBlockMixin implements EntityBlock {

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CropTempBlockEntity(pos, state);
    }
}
