package io.github.createmeow.timex_rebirth.features.flint;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 可疑的积雪物品：手持时右键已有的可疑积雪堆可直接加一层（消耗 1 个物品），
 * 满 8 层时转交原版放置逻辑（放在堆顶上方）。
 */
public class SuspiciousSnowBlockItem extends BlockItem {

    public SuspiciousSnowBlockItem(SuspiciousSnowBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SuspiciousSnowBlock snowBlock))
            return super.useOn(context);

        int layers = state.getValue(SuspiciousSnowBlock.LAYERS);
        if (layers >= 8) // 已满层：允许玩家放在堆顶
            return super.useOn(context);

        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(SuspiciousSnowBlock.LAYERS, layers + 1), 3);
            level.playSound(null, pos, SoundEvents.SNOW_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            Player player = context.getPlayer();
            if (player == null || !player.getAbilities().instabuild)
                context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
