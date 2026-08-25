package io.github.createmeow.timex_rebirth.heat.station;

import dev.anye.mc.basecore.block.BlockRegister;
import dev.anye.mc.basecore.block.entity.PlaceholderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 热源供应站统一放置进度（复用基地核心占位放置机制）：
 * 右键后放置占位方块并进入 30 秒放置进度（进度条由基地核心模组客户端显示），
 * 完成后替换为目标方块并把 NBT（owner）加载到方块实体。
 * 多方块位置检查在开始放置时进行（不满足直接取消、不消耗物品），
 * 进度期间位置失效由 PlaceholderBlockEntityMixin 每 tick 取消并返还物品。
 */
public final class StationPlacement {

    /** 放置位置规则：开始校验 + 进度期间持续校验。 */
    public interface Rule {
        /** 开始放置校验：返回提示 lang key（取消放置）或 null（通过）。 */
        @Nullable
        String startCheck(Level level, BlockPos targetPos);

        /** 进度期间每 tick 校验：false 表示位置失效，取消放置。 */
        boolean keepValid(Level level, BlockPos placeholderPos);
    }

    /** 加热底座：需要 3×1×3 空地（中心为底座本身，周围 8 格留作站位方块）。 */
    public static final Rule BASE = new Rule() {
        @Override
        public String startCheck(Level level, BlockPos targetPos) {
            return baseSpaceClear(level, targetPos) ? null : "msg.timex_rebirth.heat_base.space_blocked";
        }

        @Override
        public boolean keepValid(Level level, BlockPos placeholderPos) {
            return baseSpaceClear(level, placeholderPos);
        }
    };

    private static boolean baseSpaceClear(Level level, BlockPos centerPos) {
        for (BlockPos p : HeatStructureHelper.baseCasingPositions(centerPos)) {
            BlockState state = level.getBlockState(p);
            if (!state.isAir() && !state.canBeReplaced()) return false;
        }
        return true;
    }

    /** 热源发生器：必须放置在加热底座上方，且 3×2×3 发生器容积（17 个站位位）全部可替换。 */
    public static final Rule GENERATOR = new Rule() {
        @Override
        public String startCheck(Level level, BlockPos targetPos) {
            if (!(level.getBlockState(targetPos.below()).getBlock() instanceof HeatBaseBlock)) {
                return "msg.timex_rebirth.heat_station.no_base";
            }
            return generatorSpaceClear(level, targetPos) ? null : "msg.timex_rebirth.heat_station.space_blocked";
        }

        @Override
        public boolean keepValid(Level level, BlockPos placeholderPos) {
            if (!(level.getBlockState(placeholderPos.below()).getBlock() instanceof HeatBaseBlock)) {
                return false;
            }
            return generatorSpaceClear(level, placeholderPos);
        }
    };

    private static boolean generatorSpaceClear(Level level, BlockPos stationPos) {
        for (BlockPos p : HeatStructureHelper.generatorVolumePositions(stationPos)) {
            BlockState state = level.getBlockState(p);
            if (!state.isAir() && !state.canBeReplaced()) return false;
        }
        return true;
    }

    /** 供应站部件（燃料接收器/适配器/模块插槽）：只能安装在发生器第一层预留的缺口安装槽中。 */
    public static final Rule PART = new Rule() {
        @Override
        public String startCheck(Level level, BlockPos targetPos) {
            if (HeatStructureHelper.findStation(level, targetPos) == null) {
                return "msg.timex_rebirth.heat_station.part_no_station";
            }
            if (!isGeneratorGap(level, targetPos)) {
                return "msg.timex_rebirth.heat_station.part_wrong_slot";
            }
            return null;
        }

        @Override
        public boolean keepValid(Level level, BlockPos placeholderPos) {
            return HeatStructureHelper.findStation(level, placeholderPos) != null
                    && isGeneratorGap(level, placeholderPos);
        }
    };

    /** 缺口判定：下方为底座结构（底座中心/底座站位方块），上方为发生器站位方块。 */
    private static boolean isGeneratorGap(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        BlockState above = level.getBlockState(pos.above());
        return (below.getBlock() instanceof HeatBaseBlock || below.getBlock() instanceof HeatBaseCasingBlock)
                && above.getBlock() instanceof HeatStationCasingBlock;
    }

    /**
     * 放置进度期间的持续有效性校验（供 PlaceholderBlockEntityMixin 委托）：
     * 底座/发生器/部件各自的空间与结构条件被破坏时返回 false，取消放置并返还物品。
     */
    public static boolean keepValid(String targetBlockId, Level level, BlockPos placeholderPos) {
        return switch (targetBlockId) {
            case "timex_rebirth:heat_base" -> BASE.keepValid(level, placeholderPos);
            case "timex_rebirth:heat_station" -> GENERATOR.keepValid(level, placeholderPos);
            case "timex_rebirth:heat_fuel_receiver",
                 "timex_rebirth:heat_adapter",
                 "timex_rebirth:heat_module_slot" -> PART.keepValid(level, placeholderPos);
            default -> true;
        };
    }

    /**
     * 开始放置进度。
     *
     * @return true 表示已处理（成功开始或取消）；false 表示位置无法放置，交由默认逻辑。
     */
    public static boolean startPlacement(ServerPlayer player, UseOnContext context,
                                         String blockId, String displayKey, Rule rule) {
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

        // 多方块位置检查（开始放置时校验，不满足则取消、不消耗物品）
        String errorKey = rule.startCheck(level, targetPos);
        if (errorKey != null) {
            player.displayClientMessage(Component.translatable(errorKey), true);
            return true;
        }

        // 保存 NBT：所有者（放置完成时加载到方块实体）
        CompoundTag data = new CompoundTag();
        data.putUUID("owner", player.getUUID());

        // 放置占位方块（基地核心模组的放置进度机制）
        level.setBlock(targetPos, BlockRegister.PLACEHOLDER.get().defaultBlockState(), 3);
        if (level.getBlockEntity(targetPos) instanceof PlaceholderBlockEntity be) {
            ItemStack returnStack = context.getItemInHand().copy();
            returnStack.setCount(1);
            be.init(player.getUUID(), data, blockId,
                    Component.translatable(displayKey).getString(), returnStack);
        }
        context.getItemInHand().shrink(1);

        player.displayClientMessage(
                Component.translatable("msg.timex_rebirth.placement.start",
                        Component.translatable(displayKey)), true);
        return true;
    }
}
