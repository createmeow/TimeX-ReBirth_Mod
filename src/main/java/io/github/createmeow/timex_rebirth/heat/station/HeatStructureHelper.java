package io.github.createmeow.timex_rebirth.heat.station;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 热源供应站结构工具（大水车式整体组装）：
 * - 加热底座：3×1×3。中心为加热底座方块，周围 8 格为底座站位方块。
 * - 热源发生器：3×2×3。主方块位于第一层中心；第一层四角 + 第二层满 3×3 为发生器站位方块，
 *   第一层四个边中点预留缺口（与主方块 6 面相邻）用于安装热源适配器 / 模块插槽 / 燃料接收器。
 * - 站位方块由主方块放置时自动填充，无物品无掉落；破坏站位方块 → 摧毁主方块 → 整体解体。
 * - 结构血量归零 / 底座消失时整体解体（部件连同内部物品掉落）。
 */
public final class HeatStructureHelper {

    private HeatStructureHelper() {
    }

    /** 解体保护标志：主方块清理站位方块期间，禁止站位方块再次级联摧毁主方块（防重复掉落）。 */
    public static boolean disassembling = false;

    /** 在 pos 的 6 个方向寻找热源发生器方块实体（缺口部件均与主方块 6 面相邻）。 */
    public static HeatStationBlockEntity findStation(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockEntity be = level.getBlockEntity(pos.relative(dir));
            if (be instanceof HeatStationBlockEntity station) {
                return station;
            }
        }
        return null;
    }

    /** 收集与发生器 6 面相邻的所有供应站部件位置（燃料接收器/适配器/模块插槽，不含底座）。 */
    public static List<BlockPos> collectParts(Level level, BlockPos stationPos) {
        List<BlockPos> parts = new ArrayList<>();
        if (level == null) return parts;
        for (Direction dir : Direction.values()) {
            BlockPos pos = stationPos.relative(dir);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HeatFuelReceiverBlockEntity
                    || be instanceof HeatAdapterBlockEntity
                    || be instanceof HeatModuleSlotBlockEntity) {
                parts.add(pos);
            }
        }
        return parts;
    }

    // ── 结构几何 ──

    /** 底座站位方块位置：围绕底座中心的 3×3（不含中心）。 */
    public static List<BlockPos> baseCasingPositions(BlockPos basePos) {
        List<BlockPos> list = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                list.add(basePos.offset(dx, 0, dz));
            }
        }
        return list;
    }

    /** 发生器站位方块位置：第一层四角 + 第二层满 3×3（不含主方块本身）。 */
    public static List<BlockPos> stationCasingPositions(BlockPos stationPos) {
        List<BlockPos> list = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                list.add(stationPos.offset(dx, 1, dz));
            }
        }
        list.add(stationPos.offset(1, 0, 1));
        list.add(stationPos.offset(1, 0, -1));
        list.add(stationPos.offset(-1, 0, 1));
        list.add(stationPos.offset(-1, 0, -1));
        return list;
    }

    /** 发生器整体容积（3×2×3 除主方块外的全部 17 个位置，含缺口）：用于放置空间检测。 */
    public static List<BlockPos> generatorVolumePositions(BlockPos stationPos) {
        List<BlockPos> list = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                list.add(stationPos.offset(dx, 0, dz));
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                list.add(stationPos.offset(dx, 1, dz));
            }
        }
        return list;
    }

    // ── 填充（放置完成时自动组装）──

    /** 填充底座站位方块；若站位被异物占用则拆除底座中心方块（整体失败，底座掉落）。 */
    public static void fillBaseCasing(ServerLevel level, BlockPos basePos) {
        for (BlockPos pos : baseCasingPositions(basePos)) {
            BlockState occupied = level.getBlockState(pos);
            if (occupied.isAir() || occupied.canBeReplaced()) {
                level.setBlockAndUpdate(pos, HeatStationRegistry.HEAT_BASE_CASING.get()
                        .defaultBlockState().setValue(HeatCasingBlock.FACING, casingFacing(basePos, pos)));
            } else if (!(occupied.getBlock() instanceof HeatBaseCasingBlock)) {
                level.destroyBlock(basePos, true);
                return;
            }
        }
    }

    /** 填充发生器站位方块（第一层四角朝上、第二层朝向主方块），缺口留空用于安装部件。 */
    public static void fillStationCasing(ServerLevel level, BlockPos stationPos) {
        var casing = HeatStationRegistry.HEAT_STATION_CASING.get().defaultBlockState();
        // 第一层四角：朝上（指向第二层四角 → 第二层中心 → 主方块）
        for (int dx : new int[]{-1, 1}) {
            for (int dz : new int[]{-1, 1}) {
                if (!placeStationCasing(level, stationPos, stationPos.offset(dx, 0, dz),
                        casing.setValue(HeatCasingBlock.FACING, Direction.UP))) {
                    return;
                }
            }
        }
        // 第二层满 3×3：中心朝下指向主方块，其余水平朝向中心
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = stationPos.offset(dx, 1, dz);
                Direction facing;
                if (dx == 0 && dz == 0) {
                    facing = Direction.DOWN;
                } else if (dx != 0 && dz == 0) {
                    facing = dx == 1 ? Direction.WEST : Direction.EAST;
                } else if (dx == 0) {
                    facing = dz == 1 ? Direction.NORTH : Direction.SOUTH;
                } else {
                    facing = dx == 1 ? Direction.WEST : Direction.EAST;
                }
                if (!placeStationCasing(level, stationPos, pos,
                        casing.setValue(HeatCasingBlock.FACING, facing))) {
                    return;
                }
            }
        }
    }

    private static boolean placeStationCasing(ServerLevel level, BlockPos masterPos, BlockPos pos, BlockState casingState) {
        BlockState occupied = level.getBlockState(pos);
        if (occupied.isAir() || occupied.canBeReplaced()) {
            level.setBlockAndUpdate(pos, casingState);
            return true;
        }
        if (!(occupied.getBlock() instanceof HeatStationCasingBlock)) {
            // 站位被异物占用 → 组装失败，整体拆除（主方块掉落，其余由 onRemove 清理）
            level.destroyBlock(masterPos, true);
            return false;
        }
        return true;
    }

    /** 从 pos 指向 target 的朝向（优先水平轴，用于底座站位方块）。 */
    private static Direction casingFacing(BlockPos target, BlockPos pos) {
        if (pos.getX() != target.getX()) return pos.getX() < target.getX() ? Direction.EAST : Direction.WEST;
        if (pos.getZ() != target.getZ()) return pos.getZ() < target.getZ() ? Direction.SOUTH : Direction.NORTH;
        return Direction.NORTH;
    }

    // ── 解体 ──

    /** 发生器主方块被移除：清除全部发生器站位方块，并让缺口中的部件掉落（含内部物品）。 */
    public static void onStationRemoved(Level level, BlockPos stationPos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        boolean prev = disassembling;
        disassembling = true;
        try {
            for (BlockPos p : stationCasingPositions(stationPos)) {
                if (level.getBlockState(p).getBlock() instanceof HeatStationCasingBlock) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                }
            }
            for (BlockPos p : collectParts(level, stationPos)) {
                dropBlock(serverLevel, p);
            }
        } finally {
            disassembling = prev;
        }
    }

    /** 底座中心方块被移除：清除全部底座站位方块，并解体上方发生器（整体倒塌）。 */
    public static void onBaseRemoved(Level level, BlockPos basePos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        boolean prev = disassembling;
        disassembling = true;
        try {
            for (BlockPos p : baseCasingPositions(basePos)) {
                if (level.getBlockState(p).getBlock() instanceof HeatBaseCasingBlock) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        } finally {
            disassembling = prev;
        }
        disassembleIfPresent(level, basePos.above());
    }

    /** 若指定位置存在热源发生器则整体解体（结构血量归零 / 底座消失时调用）。 */
    public static void disassembleIfPresent(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof HeatStationBlockEntity) {
            disassemble(level, pos);
        }
    }

    /** 结构解体：发生器按掉落表掉落自身方块，站位方块与部件清理由主方块 onRemove 级联完成。 */
    public static void disassemble(Level level, BlockPos stationPos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        dropBlock(serverLevel, stationPos);
    }

    /** 使方块按自身掉落表掉落物品并移除（无方块实体直接移除；结构解体时返还内部物品）。 */
    public static void dropBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            be.setRemoved();
        }
        LootParams.Builder params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY);
        for (ItemStack stack : state.getDrops(params)) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
        // 结构解体时返还内部物品（模块插槽的模块、燃料接收器的燃料）
        if (be instanceof HeatModuleSlotBlockEntity slot) {
            dropHandler(level, pos, slot.getModules());
        } else if (be instanceof HeatFuelReceiverBlockEntity receiver) {
            dropHandler(level, pos, receiver.getFuelSlot());
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private static void dropHandler(Level level, BlockPos pos, net.neoforged.neoforge.items.ItemStackHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                    handler.getStackInSlot(i));
        }
    }
}
