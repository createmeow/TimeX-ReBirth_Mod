package io.github.createmeow.timex_rebirth.features;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import vectorwing.farmersdelight.common.block.AbstractStoveBlock;

/**
 * 丢弃添柴（参考 FrostedHeart CampfireBlockMixin_TimeLimit.stepOn）：
 * 将可燃物品实体丢到篝火/炉灶上自动吸收为燃料。
 *
 * <p>实现：监听物品实体 tick，检查其所在/下方方块是否为火焰类方块。
 * 只吸收"已点燃"或"已有燃料"的火焰；完全空燃的火需手动打火，防止靠丢物复燃。</p>
 */
@EventBusSubscriber(modid = TimeX.MODID)
public class FireItemAbsorbHandler {

    private static boolean isFireBlock(BlockState state) {
        return state.getBlock() instanceof CampfireBlock
                || (net.neoforged.fml.ModList.get().isLoaded("farmersdelight")
                    && state.getBlock() instanceof AbstractStoveBlock);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) return;
        // 服务端处理：物品实体必须真正落在火焰方块上才吸收
        if (itemEntity.level().isClientSide()) return;

        var level = itemEntity.level();
        BlockPos pos = itemEntity.blockPosition();
        BlockState state = level.getBlockState(pos);
        if (!isFireBlock(state)) {
            state = level.getBlockState(pos.below());
            if (!isFireBlock(state)) return;
            pos = pos.below();
        }
        FireManager.absorbItemEntity(level, pos, state, itemEntity);
    }
}
