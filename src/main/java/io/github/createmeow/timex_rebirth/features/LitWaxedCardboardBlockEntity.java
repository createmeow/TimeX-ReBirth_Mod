package io.github.createmeow.timex_rebirth.features;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 点燃的涂蜡纸板方块实体：剩余燃烧秒数存于 FUEL_DATA 附件（<strong>单位=秒</strong>，不与 tick 混用）。
 * serverTick 每秒扣 1 秒，引燃邻居，低耐久灼伤手持者；
 * 归零时置 {@link #burnedOut} 并只掉落灰烬（避免 onRemove 再次掉落物品）。
 */
public class LitWaxedCardboardBlockEntity extends BlockEntity {

    /** 是否已烧尽（防止 onRemove 重复掉落）。 */
    public boolean burnedOut = false;

    public LitWaxedCardboardBlockEntity(BlockPos pos, BlockState state) {
        super(FireToolRegistry.LIT_WAXED_CARDBOARD_BE.get(), pos, state);
    }

    /** 当前剩余秒数。 */
    public int remainingSeconds() {
        return Math.max(0, getData(FireManager.FUEL_DATA).getFuelTicks());
    }

    /** 服务端 ticker（由方块 getTicker 调用）。 */
    public static void serverTick(Level level, BlockPos pos, BlockState state, LitWaxedCardboardBlockEntity be) {
        if (level.isClientSide()) return;
        if (level.getGameTime() % 20 != 0) return; // 每 20 tick 执行一次 = 每秒

        int seconds = be.remainingSeconds();
        int next = seconds - 1;

        // 粒子
        ((net.minecraft.server.level.ServerLevel) level).sendParticles(ParticleTypes.FLAME,
                pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 2, 0.15, 0.15, 0.15, 0.005);

        if (next <= 0) {
            // 烧尽：标记后移除，只掉灰烬（onRemove 因 burnedOut 不再掉落物品）
            be.burnedOut = true;
            be.getData(FireManager.FUEL_DATA).setFuelTicks(0);
            be.setChanged();
            level.removeBlock(pos, false);
            net.minecraft.world.Containers.dropItemStack(level,
                    pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                    new ItemStack(FireToolRegistry.ASH.get()));
            return;
        }

        be.getData(FireManager.FUEL_DATA).setFuelTicks(next);
        be.setChanged();

        // 引燃六向邻居（含未点燃的涂蜡纸板）
        FireInteractHandler.igniteNeighbors(level, pos);

        // 低耐久（<15s）灼伤附近手持点燃纸板的玩家：1 火焰伤害/秒
        if (next < 15) {
            for (var player : level.players()) {
                ItemStack main = player.getMainHandItem();
                if (main.getItem() instanceof WaxedCardboardItem
                        && player.blockPosition().distSqr(pos) < 16) {
                    player.hurt(level.damageSources().inFire(), 1.0F);
                }
            }
        }
    }
}
