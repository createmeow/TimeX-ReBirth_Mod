package io.github.createmeow.timex_rebirth.mixin;

import dev.anye.mc.basecore.block.entity.PlaceholderBlockEntity;
import io.github.createmeow.timex_rebirth.heat.HeatReceiverBlock;
import io.github.createmeow.timex_rebirth.heat.station.StationPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * 放置进度附加校验（复用基地核心占位放置机制）：
 * 放置进度期间位置失效时立即取消放置并把物品返还给放置者。
 * - 热源接收器：相邻基地核心消失
 * - 加热底座 / 热源发生器 / 供应站部件：空间与结构条件失效（StationPlacement.keepValid）
 * 仅对 timex_rebirth 的占位放置（按 targetBlockId 区分）生效。
 */
@Mixin(PlaceholderBlockEntity.class)
public abstract class PlaceholderBlockEntityMixin {

    @Shadow
    private String targetBlockId;

    @Shadow
    private UUID placerUUID;

    @Shadow
    private void cancelPlacement(Level level, BlockPos pos, ServerPlayer player) {
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void timex_rebirth$checkPlacementValidity(Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (this.targetBlockId == null) return;

        boolean invalid;
        if ("timex_rebirth:heat_receiver".equals(this.targetBlockId)) {
            invalid = HeatReceiverBlock.findAdjacentCore(level, pos) == null;
        } else {
            invalid = !StationPlacement.keepValid(this.targetBlockId, level, pos);
        }

        if (invalid) {
            ServerPlayer placer = level.getPlayerByUUID(this.placerUUID) instanceof ServerPlayer sp ? sp : null;
            this.cancelPlacement(level, pos, placer);
        }
    }
}
