package io.github.createmeow.timex_rebirth.features;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 用完的打火机：仅剩一口气的凑合火源。
 * <ul>
 *   <li>64 点耐久；右键对绒毛堆使用 → <b>直接点燃</b>（无视燧石的"20%×数量"概率），
 *       行为与燧石成功路径一致：绒毛堆原地变火焰并主动引燃六向邻居，每次消耗 1 耐久；</li>
 *   <li>耐久耗尽报废（考古可从可疑的积雪中挖出残存 5~20 耐久的）。</li>
 * </ul>
 */
public class UsedLighterItem extends Item {

    public UsedLighterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        // 只对绒毛堆生效；其余情况交给原版逻辑
        if (!(state.getBlock() instanceof FuzzPileBlock)) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // 直接点燃：变火焰 + 掉落对应耐久的点燃绒毛 + 引燃六向邻居（不依赖层数概率）
        io.github.createmeow.timex_rebirth.features.FireInteractHandler
                .igniteFuzzPileSelf(level, pos);
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
        player.displayClientMessage(Component.translatable("message.timex_rebirth.fire_success"), true);

        // 每次消耗 1 耐久（创造模式豁免）；耗尽即报废
        if (!player.getAbilities().instabuild) {
            context.getItemInHand().hurtAndBreak(1, player,
                    context.getHand() == InteractionHand.MAIN_HAND
                            ? EquipmentSlot.MAINHAND
                            : EquipmentSlot.OFFHAND);
        }
        return InteractionResult.SUCCESS;
    }
}
