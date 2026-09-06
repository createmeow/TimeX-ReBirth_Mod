package io.github.createmeow.timex_rebirth.workbench;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 工作台(半成品)方块实体：用 step 计数器追踪组装进度。
 * <p>
 * 步骤映射（与 Create 序列组装配方顺序一致）：
 * <ul>
 *   <li>step 0 → 无工具</li>
 *   <li>step 1 → shears 已安装</li>
 *   <li>step 2 → shears + hammer 已安装</li>
 *   <li>step 3 → shears + hammer + saw 已安装（完成 → 变为工作台）</li>
 * </ul>
 */
public class WorkbenchHalfBlockEntity extends BlockEntity {

    private int step = 0;

    /** 序列组装配方总步数 */
    public static final int TOTAL_STEPS = 3;

    public WorkbenchHalfBlockEntity(BlockPos pos, BlockState state) {
        super(WorkbenchRegistry.WORKBENCH_HALF_BE.get(), pos, state);
    }

    /** 获取当前步骤 */
    public int getStep() {
        return step;
    }

    /** 设置步骤（从 create:sequenced_assembly 同步时使用） */
    public void setStep(int step) {
        this.step = Math.max(0, Math.min(step, TOTAL_STEPS));
        setChanged();
    }

    /** 尝试安装工具，仅允许按顺序安装（step 0→剪, 1→锤, 2→锯）。成功返回 true。 */
    public boolean installTool(ToolType type) {
        ToolType expected = expectedTool();
        if (type != expected) return false;
        step++;
        setChanged();
        return true;
    }

    /** 当前应该安装的工具类型。 */
    public ToolType expectedTool() {
        return switch (step) {
            case 0 -> ToolType.SHEARS;
            case 1 -> ToolType.HAMMER;
            case 2 -> ToolType.SAW;
            default -> null;
        };
    }

    /** 是否已完成全部步骤。 */
    public boolean isComplete() {
        return step >= TOTAL_STEPS;
    }

    /** 进度百分比（0~100）。 */
    public int progressPercent() {
        return step * 100 / TOTAL_STEPS;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("step", step);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        step = tag.getInt("step");
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // 旧存档/区块加载时同步渲染阶段（STEP 方块状态可能在存档前未及时更新）
        if (level != null && !level.isClientSide) {
            WorkbenchHalfBlock.syncStep(level, worldPosition, this);
        }
    }

    public enum ToolType {
        SHEARS,
        HAMMER,
        SAW
    }
}
