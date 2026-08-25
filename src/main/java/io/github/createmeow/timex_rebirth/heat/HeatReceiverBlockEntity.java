package io.github.createmeow.timex_rebirth.heat;

import com.momosoftworks.coldsweat.core.init.ModEffects;
import dev.anye.mc.basecore.block.entity.basecore.BaseCoreBlockEntity;
import io.github.createmeow.timex_rebirth.TimeXConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 热源接收器方块实体（基地热场发射器）：
 * - 储存热流流体（IFluidHandler，Create 管道可直接对接）
 * - 纯范围暖场：以基地核心为中心、边长 range×2 的 AABB 立方体内输出温暖
 *   （参照冷汗壁炉"范围内输出温暖"机制，但不要求封闭空间，无需屋顶/墙体），
 *   对范围内实体施加冷汗自带的 cold_sweat:warmth 效果（由冷汗自动转换为
 *   WarmthTempModifier 温度修正），带预热爬升，并按节奏从储罐抽取热流作为燃料。
 * - 供热开关/等级由相邻基地核心上的热源桥接模块决定，模块等级 = 最大绝缘等级。
 */
public class HeatReceiverBlockEntity extends BlockEntity implements IFluidHandler {

    private final List<LivingEntity> entities = new ArrayList<>();

    private int insulationLevel = 0; // 预热爬升（0 ~ HEAT_WARM_UP_TICKS）
    private int ticksExisted = 0;

    private int moduleLevel = 0; // 桥接模块等级（0 = 未启用供热）
    private int range = 8;       // 热场半径 = 基地核心 getRange()

    private UUID owner;
    private BlockPos corePos;
    private final FluidTank tank;

    public HeatReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(HeatRegistry.HEAT_RECEIVER_BE.get(), pos, state);
        this.tank = new FluidTank(TimeXConfig.HEAT_RECEIVER_CAPACITY.get(),
                f -> f.getFluid() == HeatRegistry.HEAT_FLUX.get()) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
    }

    public void tick() {
        Level level = this.level;
        if (level == null || level.isClientSide) return;
        this.ticksExisted++;

        // 每 20 tick：刷新模块等级/范围 + 扫描暖场范围内的实体 + 同步热场注册表
        if (this.ticksExisted % 20 == 0) {
            this.refreshConfig();
            this.scanEntities(level);
            boolean active = this.moduleLevel > 0 && this.tank.getFluidAmount() > 0;
            HeatFieldState.update(this.getBlockPos(), level.dimension(), this.corePos, this.range, active);
        }

        boolean enabled = this.moduleLevel > 0 && this.tank.getFluidAmount() > 0;

        if (enabled) {
            // 预热爬升（暖场强度随预热线性提升）
            if (this.insulationLevel < TimeXConfig.HEAT_WARM_UP_TICKS.get()) {
                this.insulationLevel++;
            }

            // 每 20 tick：对暖场范围内的实体施加温暖效果（不需要封闭空间）
            if (this.ticksExisted % 20 == 0) {
                AABB heatedArea = this.getHeatedArea();
                for (LivingEntity entity : this.entities) {
                    if (entity == null || !entity.isAlive()) continue;
                    if (heatedArea.intersects(entity.getBoundingBox())) {
                        this.insulateEntity(entity);
                    }
                }
            }

            // 消耗热流（替代壁炉燃料）
            this.tickDrainHeat();
        }
    }

    /** 刷新模块等级与热场半径（从相邻基地核心读取）。 */
    private void refreshConfig() {
        this.moduleLevel = 0;
        if (this.corePos == null || this.level == null) return;
        if (this.level.getBlockEntity(this.corePos) instanceof BaseCoreBlockEntity core) {
            this.moduleLevel = core.getModuleLvl(HeatRegistry.HEAT_BRIDGE_MODULE.get());
            this.range = Math.max(1, core.getRange());
        }
    }

    /**
     * 扫描暖场范围内的实体。仅遍历在线玩家（数量有限），避免热场范围大时
     * 对大范围 AABB 做全量实体扫描造成的性能开销；热场主要服务玩家保暖。
     */
    private void scanEntities(Level level) {
        this.entities.clear();
        if (this.range <= 0) return;
        AABB heatedArea = this.getHeatedArea();
        for (Player player : level.players()) {
            if (heatedArea.intersects(player.getBoundingBox())) {
                this.entities.add(player);
            }
        }
    }

    /** 暖场区域：以基地核心为中心、边长 range×2 的立方体 AABB。 */
    private AABB getHeatedArea() {
        BlockPos core = this.getOrigin();
        int r = Math.max(1, this.range);
        return new AABB(core.getX() - r, core.getY() - r, core.getZ() - r,
                core.getX() + r + 1, core.getY() + r + 1, core.getZ() + r + 1);
    }

    /** 暖场中心：基地核心位置（用户确认：以核心为中心铺满基地）。 */
    private BlockPos getOrigin() {
        return this.corePos != null ? this.corePos : this.getBlockPos();
    }

    /**
     * 持续供热模式：对暖场内的实体施加冷汗 WARMTH 效果（效果等级随预热爬升）。
     * 最大温暖等级由桥接模块数量决定：1 个 → 3 级，2 个 → 7 级，3 个 → 10 级
     * （受 heat.max_warmth_level 配置上限限制）。
     */
    private void insulateEntity(LivingEntity entity) {
        int maxInsulation = this.getMaxInsulationLevel();
        int maxEffect = maxInsulation - 1;
        if (maxEffect < 0) return;
        int effectLevel = (int) Math.min(maxEffect,
                (this.insulationLevel / (double) TimeXConfig.HEAT_WARM_UP_TICKS.get()) * maxEffect);
        entity.addEffect(new MobEffectInstance(ModEffects.WARMTH, 60, effectLevel, false, false, true));
    }

    private int getMaxInsulationLevel() {
        int warmth = switch (this.moduleLevel) {
            case 1 -> 3;
            case 2 -> 7;
            default -> 10; // 3 个及以上
        };
        return Math.min(TimeXConfig.HEAT_MAX_WARMTH_LEVEL.get(), warmth);
    }

    /** 按配置节奏从储罐抽取热流（每等级每间隔消耗 HEAT_CONSUME_MB_PER_LEVEL mb）。 */
    private void tickDrainHeat() {
        int interval = TimeXConfig.HEAT_DRAIN_INTERVAL.get();
        if (interval > 0 && this.ticksExisted % interval == 0) {
            int mb = TimeXConfig.HEAT_CONSUME_MB_PER_LEVEL.get() * this.moduleLevel;
            this.tank.drain(mb, FluidAction.EXECUTE);
        }
    }

    // ── 生命周期：加载时立即注册热场，销毁/卸载时注销 ──

    @Override
    public void onLoad() {
        super.onLoad();
        // 区块加载时立即注册（无需等待首次 tick），避免玩家进入世界/重生瞬间
        // 接收器尚未 tick、热场注册表为空时，范围内的土地被误转为冻土。
        // 重生/区块加载瞬间相邻核心区块可能尚未加载，模块等级暂读为 0；
        // 因此只要绑定核心且储罐存有热流即乐观注册热场，供热开关由后续 tick 严格校正。
        if (this.level != null && !this.level.isClientSide && this.corePos != null) {
            this.refreshConfig();
            boolean active = this.tank.getFluidAmount() > 0;
            HeatFieldState.update(this.getBlockPos(), this.level.dimension(), this.corePos, this.range, active);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        HeatFieldState.remove(this.getBlockPos());
    }

    @Override
    public void onChunkUnloaded() {
        HeatFieldState.remove(this.getBlockPos());
    }

    // ── 访问器 ──

    public FluidTank getTank() {
        return tank;
    }

    public UUID getOwner() {
        return owner;
    }

    public BlockPos getCorePos() {
        return corePos;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public void setCorePos(BlockPos corePos) {
        this.corePos = corePos;
        setChanged();
    }

    public int getModuleLevel() {
        return moduleLevel;
    }

    public int getInsulationLevel() {
        return insulationLevel;
    }

    // ── IFluidHandler 委托给储罐 ──

    @Override
    public int getTanks() {
        return tank.getTanks();
    }

    @Override
    public FluidStack getFluidInTank(int tankIndex) {
        return tank.getFluidInTank(tankIndex);
    }

    @Override
    public int getTankCapacity(int tankIndex) {
        return tank.getTankCapacity(tankIndex);
    }

    @Override
    public boolean isFluidValid(int tankIndex, FluidStack stack) {
        return tank.isFluidValid(tankIndex, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return tank.fill(resource, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return tank.drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return tank.drain(maxDrain, action);
    }

    // ── 持久化 ──

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) tag.putUUID("owner", owner);
        if (corePos != null) tag.putLong("core_pos", corePos.asLong());
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("owner")) owner = tag.getUUID("owner");
        if (tag.contains("core_pos")) corePos = BlockPos.of(tag.getLong("core_pos"));
        if (tag.contains("tank")) tank.readFromNBT(registries, tag.getCompound("tank"));
    }
}
