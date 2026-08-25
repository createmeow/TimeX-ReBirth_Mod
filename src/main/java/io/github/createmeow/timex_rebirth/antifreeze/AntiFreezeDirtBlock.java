package io.github.createmeow.timex_rebirth.antifreeze;

import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.ItemAbilities;

/**
 * 抗冻土壤：受防冻剂处理过的泥土，低温下不会转换为冻土；
 * 可被锄头耕作为抗冻耕地（经 getToolModifiedState 扩展，NeoForge ItemAbility 体系）。
 */
public class AntiFreezeDirtBlock extends Block {

    public AntiFreezeDirtBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility itemAbility, boolean simulate) {
        if (itemAbility == ItemAbilities.HOE_TILL
                && context.getLevel().getBlockState(context.getClickedPos().above()).isAir()) {
            return AntiFreezeRegistry.ANTI_FREEZE_FARMLAND.get().defaultBlockState();
        }
        return super.getToolModifiedState(state, context, itemAbility, simulate);
    }
}
