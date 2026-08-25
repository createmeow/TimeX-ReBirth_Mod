package io.github.createmeow.timex_rebirth.antifreeze;

import io.github.createmeow.timex_rebirth.compat.ImmersiveWeatheringCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * 防冻剂：右键泥土/砂土/草方块 → 抗冻土壤；右键耕地 → 抗冻耕地；
 * 右键冻土（Immersive Weathering）→ 解冻为抗冻土壤。
 * 每次使用消耗 1 点耐久（参照机械动力强力胶的右键使用交互），成功转换时触发成就触发器。
 */
public class AntiFreezeItem extends Item {

    public AntiFreezeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        BlockState target = null;
        AntiFreezeUsedTrigger.SoilType soilType = null;
        if (state.is(Blocks.FARMLAND)) {
            target = AntiFreezeRegistry.ANTI_FREEZE_FARMLAND.get().defaultBlockState();
            soilType = AntiFreezeUsedTrigger.SoilType.FARMLAND;
        } else if (state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.GRASS_BLOCK)) {
            target = AntiFreezeRegistry.ANTI_FREEZE_DIRT.get().defaultBlockState();
            soilType = AntiFreezeUsedTrigger.SoilType.DIRT;
        } else if (ImmersiveWeatheringCompat.isPermafrost(state.getBlock())) {
            // 防冻剂解冻冻土 → 抗冻土壤（成就"冫东 土"）
            target = AntiFreezeRegistry.ANTI_FREEZE_DIRT.get().defaultBlockState();
            soilType = AntiFreezeUsedTrigger.SoilType.PERMAFROST;
        }
        if (target == null) return InteractionResult.PASS;

        if (!level.isClientSide) {
            level.setBlock(pos, target, 3);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(context.getPlayer(), target));
            level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
            ItemStack stack = context.getItemInHand();
            Player player = context.getPlayer();
            if (player != null) {
                stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                if (player instanceof ServerPlayer serverPlayer) {
                    AntiFreezeRegistry.ANTI_FREEZE_USED.get().trigger(serverPlayer, soilType);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }
}
