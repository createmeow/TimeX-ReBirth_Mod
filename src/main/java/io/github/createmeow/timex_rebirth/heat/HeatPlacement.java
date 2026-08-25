package io.github.createmeow.timex_rebirth.heat;

import dev.anye.mc.basecore.block.BlockRegister;
import dev.anye.mc.basecore.block.entity.PlaceholderBlockEntity;
import dev.anye.mc.basecore.block.entity.basecore.BaseCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 热源接收器放置进度（参照基地核心模组的占位放置机制）：
 * 右键后放置占位方块并进入 30 秒放置进度，进度条由基地核心模组客户端显示；
 * 目标位置必须紧贴基地核心且拥有权限，否则直接取消（不消耗物品）。
 * 进度期间相邻基地核心消失时，由 PlaceholderBlockEntityMixin 取消放置并返还物品。
 */
public class HeatPlacement {

    public static boolean startPlacement(ServerPlayer player, UseOnContext context) {
        Level level = context.getLevel();

        // 计算放置位置
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        BlockPos targetPos = placeContext.getClickedPos();
        BlockState existing = level.getBlockState(targetPos);
        if (!existing.isAir() && !existing.canBeReplaced()) {
            targetPos = placeContext.getClickedPos().relative(placeContext.getClickedFace());
            BlockState adjacent = level.getBlockState(targetPos);
            if (!adjacent.isAir() && !adjacent.canBeReplaced()) {
                return false;
            }
        }

        // 该位置已有正在进行的放置
        if (level.getBlockEntity(targetPos) instanceof PlaceholderBlockEntity) {
            player.displayClientMessage(Component.translatable("msg.timex_rebirth.heat_receiver.placing"), true);
            return true;
        }

        // 必须紧贴基地核心
        BaseCoreBlockEntity core = HeatReceiverBlock.findAdjacentCore(level, targetPos);
        if (core == null) {
            player.displayClientMessage(Component.translatable("msg.timex_rebirth.heat_receiver.no_basecore"), true);
            return true; // 取消放置
        }
        if (!core.canUse(player.getUUID())) {
            player.displayClientMessage(Component.translatable("msg.timex_rebirth.heat_receiver.no_permission"), true);
            return true;
        }

        // 保存 NBT：所有者 + 绑定的核心位置（放置完成时加载到方块实体）
        CompoundTag data = new CompoundTag();
        data.putUUID("owner", player.getUUID());
        data.putLong("core_pos", core.getBlockPos().asLong());

        // 放置占位方块（基地核心模组的放置进度机制）
        level.setBlock(targetPos, BlockRegister.PLACEHOLDER.get().defaultBlockState(), 3);
        if (level.getBlockEntity(targetPos) instanceof PlaceholderBlockEntity be) {
            ItemStack returnStack = context.getItemInHand().copy();
            returnStack.setCount(1);
            be.init(player.getUUID(), data, "timex_rebirth:heat_receiver",
                    Component.translatable("block.timex_rebirth.heat_receiver").getString(), returnStack);
        }
        context.getItemInHand().shrink(1);

        player.displayClientMessage(Component.translatable("msg.timex_rebirth.heat_receiver.placing_start"), true);
        return true;
    }
}
