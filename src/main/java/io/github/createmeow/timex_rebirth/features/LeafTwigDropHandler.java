package io.github.createmeow.timex_rebirth.features;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * 采集枝条：玩家用<b>剑</b>或<b>农夫乐事刀</b>破坏树叶（LeavesBlock）时，
 * 有概率掉落一根「枝条」（twig），可用于在晾晒架上晒成干枝条。
 *
 * <p>同时也有概率掉落「堆肥枝条」（compost_twig），堆肥值更高。</p>
 *
 * <p>使用 {@link BlockEvent.BreakEvent}（方块被玩家破坏后触发），
 * 在叶子被破坏的位置补一根枝条掉落。</p>
 */
@EventBusSubscriber(modid = TimeX.MODID)
public class LeafTwigDropHandler {

    /** 剑/刀采集树叶掉落枝条的概率（20%）。 */
    private static final float DROP_CHANCE = 0.2F;

    /** 剑/刀采集树叶掉落堆肥枝条的概率（5%）。 */
    private static final float COMPOST_TWIG_CHANCE = 0.05F;

    /** 堆肥枝条掉落数量上限（5个）。 */
    private static final int MAX_COMPOST_TWIG_DROP = 5;

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() == null) return;
        BlockState state = event.getState();
        if (!(state.getBlock() instanceof LeavesBlock)) return;

        ItemStack held = event.getPlayer().getMainHandItem();
        boolean isBlade = held.getItem() instanceof net.minecraft.world.item.SwordItem
                || (net.neoforged.fml.ModList.get().isLoaded("farmersdelight")
                    && held.getItem() instanceof vectorwing.farmersdelight.common.item.KnifeItem);
        if (!isBlade) return;

        if (event.getPlayer().level().random.nextDouble() >= DROP_CHANCE) return;

        BlockPos pos = event.getPos();
        ServerLevel level = (ServerLevel) event.getPlayer().level();
        net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(level,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(FireToolRegistry.TWIG.get()));
        drop.setDefaultPickUpDelay();
        level.addFreshEntity(drop);

        // 概率掉落堆肥枝条
        if (level.random.nextDouble() < COMPOST_TWIG_CHANCE) {
            int count = level.random.nextInt(MAX_COMPOST_TWIG_DROP) + 1;
            net.minecraft.world.entity.item.ItemEntity compostDrop = new net.minecraft.world.entity.item.ItemEntity(
                    level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    new ItemStack(FireToolRegistry.COMPOST_TWIG.get(), count));
            compostDrop.setDefaultPickUpDelay();
            level.addFreshEntity(compostDrop);
            // 只播放一次声音，避免过于嘈杂
            level.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 0.6F, 0.8F);
        } else {
            level.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 0.6F, 0.8F);
        }
    }
}
