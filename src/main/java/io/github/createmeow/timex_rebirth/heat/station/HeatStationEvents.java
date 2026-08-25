package io.github.createmeow.timex_rebirth.heat.station;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * 热源供应站破坏拦截（仿基地核心）：
 * - 非结构成员破坏任一部件（发生器/底座/燃料接收器/适配器/模块插槽）
 *   折算为该结构血量损失并取消破坏；
 * - 模块插槽装配荆棘模块时，非成员破坏反伤 3 点。
 * - 结构血量归零由发生器方块实体触发整体解体（各部件掉落）。
 */
@EventBusSubscriber(modid = TimeX.MODID)
public class HeatStationEvents {

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer serverPlayer)) return;
        if (!(event.getLevel() instanceof Level level)) return;

        BlockPos pos = event.getPos();
        BlockEntity be = level.getBlockEntity(pos);
        HeatStationBlockEntity station = null;
        if (be instanceof HeatStationBlockEntity s) {
            station = s;
        } else if (be instanceof HeatFuelReceiverBlockEntity
                || be instanceof HeatAdapterBlockEntity
                || be instanceof HeatModuleSlotBlockEntity
                || level.getBlockState(pos).getBlock() instanceof HeatBaseBlock) {
            station = HeatStructureHelper.findStation(level, pos);
        }
        if (station == null) return;

        // 非成员破坏：扣结构血量 + 荆棘反伤 + 取消破坏
        if (!station.canUse(serverPlayer.getUUID())) {
            float dmg = (float) serverPlayer.getAttributeValue(Attributes.ATTACK_DAMAGE);
            if (dmg < 1.0F) dmg = 1.0F;
            station.damage((int) dmg);
            if (station.hasThorns()) {
                serverPlayer.hurt(serverPlayer.damageSources().fellOutOfWorld(), 3.0F);
            }
            serverPlayer.displayClientMessage(
                    Component.translatable("msg.timex_rebirth.heat_station.under_attack"), true);
            event.setCanceled(true);
        }
    }
}
