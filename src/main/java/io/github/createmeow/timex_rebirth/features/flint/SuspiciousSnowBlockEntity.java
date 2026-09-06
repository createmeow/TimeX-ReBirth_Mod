package io.github.createmeow.timex_rebirth.features.flint;

import io.github.createmeow.timex_rebirth.compat.FiahiCompatHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * 可疑积雪专用方块实体。
 * <p>
 * 继承原版 {@link BrushableBlockEntity} 以兼容旧存档 NBT 格式，但 {@link #brush} 改为 no-op：
 * 实际清刷节奏由 {@link SuspiciousSnowBrushHandler}（PlayerTickEvent）驱动，
 * 每 2 秒提升一级 DUSTED，满级后掉落战利品并转为普通积雪。
 */
public class SuspiciousSnowBlockEntity extends BrushableBlockEntity {

    public SuspiciousSnowBlockEntity(BlockPos pos, BlockState blockState) {
        super(pos, blockState);
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntityType<?> getType() {
        return FlintGearRegistry.SUSPICIOUS_SNOW_BE.get();
    }

    /**
     * No-op：不执行原版刷刮逻辑。实际清刷由 {@link SuspiciousSnowBrushHandler} 驱动。
     * 保留此覆写是为了防止原版 {@code BrushableBlockEntity.brush} 干扰（它会用
     * 自带 lootTable 字段掉落，并按原版节奏推进 brushCount）。
     */
    @Override
    public boolean brush(long startTick, Player player, Direction hitDirection) {
        return false;
    }

    /** 完成清刷：掉落战利品并转为普通积雪 */
    public void complete(ServerLevel level, Player player) {
        BlockState state = this.getBlockState();
        Block block = state.getBlock();
        if (block instanceof SuspiciousSnowBlock suspicious) {
            level.playSound(null, this.getBlockPos(),
                    suspicious.getBrushCompletedSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        dropLoot(level, player);
        int layers = state.getValue(BlockStateProperties.LAYERS);
        level.setBlockAndUpdate(this.getBlockPos(),
                Blocks.SNOW.defaultBlockState().setValue(BlockStateProperties.LAYERS, layers));
    }

    private void dropLoot(ServerLevel level, Player player) {
        LootTable lootTable = level.getServer().reloadableRegistries()
                .getLootTable(FlintGearRegistry.SUSPICIOUS_SNOW_LOOT);
        ItemStack tool = player.getMainHandItem().is(Items.BRUSH)
                ? player.getMainHandItem()
                : player.getOffhandItem();
        LootParams lootParams = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(this.getBlockPos()))
                .withParameter(LootContextParams.BLOCK_STATE, this.getBlockState())
                .withParameter(LootContextParams.TOOL, tool)
                .create(LootContextParamSets.BLOCK);
        // 层数决定掉落次数：1-3层1种、4-5层2种、6-8层3种
        int layers = this.getBlockState().getValue(BlockStateProperties.LAYERS);
        int drops = layers <= 3 ? 1 : layers <= 5 ? 2 : 3;
        for (int i = 0; i < drops; i++) {
            lootTable.getRandomItems(lootParams,
                    itemStack -> {
                        if (FiahiCompatHelper.isSeedLike(itemStack)) {
                            FiahiCompatHelper.setTemperature(itemStack, -(40 + level.getRandom().nextInt(41)));
                        }
                        Block.popResource(level, this.getBlockPos(), itemStack);
                    });
        }
    }
}
