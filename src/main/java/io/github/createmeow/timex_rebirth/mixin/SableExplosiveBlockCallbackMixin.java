package io.github.createmeow.timex_rebirth.mixin;

import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.physics.callback.ExplosiveBlockCallback;
import io.github.createmeow.timex_rebirth.compat.SableTntDelayHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 修复 Sable issue #1510 / Create Aeronautics issue #1395：
 * 子层级 TNT 碰撞时，原 onHit 在物理步进的 JNI 回调中生成实体并 setBlock(AIR)，
 * 级联破坏被玩家持有的 simulated:iron_handle，导致服务端冻结。
 * <p>
 * 修复：取消原始 onHit（同时移除该碰撞对），将 TNT 转换逻辑
 * 调度到 0.5 秒后由 {@link SableTntDelayHandler} 在服务端 tick 末尾执行，
 * 执行前先移除六面的 iron_handle。
 */
@Mixin(ExplosiveBlockCallback.class)
public abstract class SableExplosiveBlockCallbackMixin {

    @Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
    private void timex_rebirth$delayCollisionTnt(
            ServerLevel level, BlockPos pos, BlockState state, Vector3d hitPos,
            CallbackInfoReturnable<BlockSubLevelCollisionCallback.CollisionResult> cir) {
        // 调度延迟执行；返回 removeCollision=true 移除该碰撞对避免持续触发
        SableTntDelayHandler.schedule(level, pos);
        cir.setReturnValue(new BlockSubLevelCollisionCallback.CollisionResult(new Vector3d(), true));
    }
}
