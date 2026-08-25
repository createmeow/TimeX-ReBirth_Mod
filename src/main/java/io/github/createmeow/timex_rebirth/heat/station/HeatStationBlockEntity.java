package io.github.createmeow.timex_rebirth.heat.station;

import dev.anye.mc.basecore.item.module.BasecoreModuleItem;
import dev.anye.mc.basecore.item.module.basecore.AutoRepairModuleItem;
import dev.anye.mc.basecore.item.module.basecore.ThornsModuleItem;
import io.github.createmeow.timex_rebirth.TimeXConfig;
import io.github.createmeow.timex_rebirth.heat.HeatRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.UUID;

/**
 * 热源发生器方块实体：热源供应站主方块。
 * - 结构血量：与基地核心同款的方块血量机制，血量归零时整体结构解体；
 *   部件破坏伤害统一折算到本方块实体。
 * - 双燃料燃烧：固体燃料（原版熔炉燃料 + 冷血锅炉燃料）优先，
 *   无固体燃料时燃烧熔岩（流体），产出热流流体。
 * - 模块效果（由相邻模块插槽容器提供）：荆棘反伤、自动修复结构血量、
 *   节能（降低燃料消耗）、增产（提升热流产出）。
 * - 热流输出：优先转移到相邻热源适配器；无适配器时可手持空桶右键获取热流桶。
 */
public class HeatStationBlockEntity extends BlockEntity implements MenuProvider, ContainerData {
    // ── 结构血量 ──
    private UUID owner;
    private int health;
    private int maxHealth;

    // ── 燃料与产物 ──
    private final ItemStackHandler fuelSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return StationFuelUtil.isSolidFuel(stack);
        }
    };
    private final FluidTank lavaTank = new FluidTank(TimeXConfig.HEAT_STATION_TANK_CAPACITY.get(),
            f -> f.getFluid() == Fluids.LAVA) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank heatTank = new FluidTank(TimeXConfig.HEAT_STATION_TANK_CAPACITY.get(),
            f -> f.getFluid() == HeatRegistry.HEAT_FLUX.get()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    /** 固体燃料剩余燃烧时间（tick，浮点以便节能模块按比例降低消耗速率）。 */
    private double burnTimeRemaining;
    private int burnTimeTotal;
    /** 熔岩累积消耗（每 tick 0.05mb，与岩浆桶 1000mb/20000tick 等值）。 */
    private double lavaAccum;

    // ── 模块效果缓存（每 20 tick 从相邻模块插槽刷新）──
    private int autoRepairLvl;
    private boolean hasThorns;
    private int energySaveLvl;
    private int productionLvl;
    private int autoRepairTick;

    private int ticksExisted;

    public HeatStationBlockEntity(BlockPos pos, BlockState state) {
        super(HeatStationRegistry.HEAT_STATION_BE.get(), pos, state);
        this.maxHealth = TimeXConfig.HEAT_STATION_BASE_HEALTH.get();
        this.health = this.maxHealth;
    }

    public void tick() {
        Level level = this.level;
        if (level == null || level.isClientSide) return;
        this.ticksExisted++;

        // 模块效果刷新（每 20 tick）
        if (this.ticksExisted % 20 == 0) {
            this.refreshModules();
        }
        // 从相邻燃料接收器补充燃料（每 5 tick）
        if (this.ticksExisted % 5 == 0) {
            this.pullFromReceivers();
        }

        boolean hasBase = level.getBlockState(this.worldPosition.below()).getBlock() instanceof HeatBaseBlock;

        if (hasBase) {
            this.burnTick();
            this.pushToAdapters();
            this.autoRepairTick();
        }
    }

    /**
     * 燃烧一个 tick：产出热流（受增产模块），消耗固体燃料或熔岩（受节能模块）。
     * 熔岩与岩浆桶等值：1000mb 熔岩 ≈ 20000 tick 燃烧（岩浆桶 burnTime），
     * 因此熔岩按每 tick 0.05mb 累积消耗，避免"熔岩不如直接投岩浆桶划算"。
     */
    private void burnTick() {
        double consumeRate = Math.max(0.1, 1.0 - 0.1 * this.energySaveLvl);
        int rate = (int) Math.max(1, TimeXConfig.HEAT_STATION_BASE_RATE.get() * (1.0 + 0.1 * this.productionLvl));

        boolean burning = false;
        if (this.burnTimeRemaining > 0) {
            this.burnTimeRemaining -= consumeRate;
            burning = true;
        } else if (!this.fuelSlot.getStackInSlot(0).isEmpty()) {
            ItemStack fuel = this.fuelSlot.extractItem(0, 1, false);
            this.burnTimeTotal = StationFuelUtil.getBurnTime(fuel);
            this.burnTimeRemaining = this.burnTimeTotal;
            // 桶类燃料烧尽后返还空容器（岩浆桶 → 空桶）
            if (fuel.is(Items.LAVA_BUCKET)) {
                net.minecraft.world.Containers.dropItemStack(this.level,
                        this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(),
                        new ItemStack(Items.BUCKET));
            }
            burning = this.burnTimeRemaining > 0;
        }
        if (!burning && this.lavaTank.getFluidAmount() > 0) {
            this.lavaAccum += 0.05 * consumeRate;
            if (this.lavaAccum >= 1.0) {
                int mb = (int) this.lavaAccum;
                this.lavaAccum -= mb;
                this.lavaTank.drain(mb, IFluidHandler.FluidAction.EXECUTE);
            }
            burning = true;
        }

        if (burning) {
            this.heatTank.fill(new FluidStack(HeatRegistry.HEAT_FLUX.get(), rate), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    /** 自动修复模块：每 300 tick 修复结构血量（等级 1/2/3/4 → 1/2/4/6，参照基地核心）。 */
    private void autoRepairTick() {
        if (this.autoRepairLvl <= 0) return;
        if (++this.autoRepairTick < 300) return;
        this.autoRepairTick = 0;
        this.addHealth(switch (this.autoRepairLvl) {
            case 2 -> 2;
            case 3 -> 4;
            case 4 -> 6;
            default -> 1;
        });
    }

    /** 从相邻燃料接收器转移物品燃料与熔岩。 */
    private void pullFromReceivers() {
        Level level = this.level;
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(this.worldPosition.relative(dir)) instanceof HeatFuelReceiverBlockEntity receiver) {
                if (this.fuelSlot.getStackInSlot(0).isEmpty() && !receiver.getFuelSlot().getStackInSlot(0).isEmpty()) {
                    ItemStack taken = receiver.getFuelSlot().extractItem(0, 1, false);
                    this.fuelSlot.insertItem(0, taken, false);
                }
                if (this.lavaTank.getFluidAmount() < this.lavaTank.getCapacity()) {
                    int transfer = Math.min(1000, this.lavaTank.getCapacity() - this.lavaTank.getFluidAmount());
                    FluidStack drained = receiver.getLavaTank().drain(transfer,
                            net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                    this.lavaTank.fill(drained, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

    /** 将热流转移到相邻热源适配器（Create 管道从适配器抽取）。 */
    private void pushToAdapters() {
        Level level = this.level;
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(this.worldPosition.relative(dir)) instanceof HeatAdapterBlockEntity adapter) {
                FluidTank target = adapter.getHeatTank();
                if (this.heatTank.getFluidAmount() > 0 && target.getFluidAmount() < target.getCapacity()) {
                    int transfer = Math.min(1000, target.getCapacity() - target.getFluidAmount());
                    FluidStack drained = this.heatTank.drain(transfer,
                            net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                    target.fill(drained, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

    /** 扫描 6 面相邻模块插槽，统计模块效果。 */
    private void refreshModules() {
        int autoRepair = 0;
        boolean thorns = false;
        int energySave = 0;
        int production = 0;
        Level level = this.level;
        for (Direction dir : Direction.values()) {
            if (!(level.getBlockEntity(this.worldPosition.relative(dir)) instanceof HeatModuleSlotBlockEntity slot)) {
                continue;
            }
            for (int i = 0; i < slot.getModules().getSlots(); i++) {
                ItemStack stack = slot.getModules().getStackInSlot(i);
                if (stack.isEmpty() || !(stack.getItem() instanceof BasecoreModuleItem module)) continue;
                int count = stack.getCount();
                if (module instanceof AutoRepairModuleItem) {
                    autoRepair += count;
                } else if (module instanceof ThornsModuleItem) {
                    thorns = true;
                } else if (module instanceof EnergySaveModuleItem) {
                    energySave += count;
                } else if (module instanceof ProductionModuleItem) {
                    production += count;
                }
            }
        }
        this.autoRepairLvl = Math.min(4, autoRepair);
        this.hasThorns = thorns;
        this.energySaveLvl = Math.min(5, energySave);
        this.productionLvl = Math.min(5, production);
    }

    // ── 结构血量 ──

    public void damage(int amount) {
        if (this.level == null || this.level.isClientSide) return;
        this.health -= Math.max(1, amount);
        this.setChanged();
        if (this.health <= 0) {
            HeatStructureHelper.disassemble(this.level, this.worldPosition);
        }
    }

    public void addHealth(int amount) {
        if (this.level == null || this.level.isClientSide) return;
        this.health = Math.min(this.maxHealth, this.health + Math.max(0, amount));
        this.setChanged();
    }

    public int getHealth() {
        return this.health;
    }

    public int getMaxHealth() {
        return this.maxHealth;
    }

    public boolean hasThorns() {
        return this.hasThorns;
    }

    public boolean canUse(UUID user) {
        return this.owner == null || this.owner.equals(user);
    }

    public UUID getOwner() {
        return this.owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        this.setChanged();
    }

    /** 手持空桶右键：消耗 1000mb 热流（1 桶），返还热流桶（无适配器时的降级输运）。 */
    public boolean tryExtractBucket(Player player, InteractionHand hand) {
        if (this.level == null || this.level.isClientSide) return false;
        int amount = 1000; // 标准桶容量
        if (this.heatTank.getFluidAmount() < amount) return false;
        ItemStack handStack = player.getItemInHand(hand);
        if (!handStack.is(Items.BUCKET)) return false;

        this.heatTank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        handStack.shrink(1);
        ItemStack bucket = new ItemStack(HeatStationRegistry.HEAT_FLUX_BUCKET.get());
        if (!player.getInventory().add(bucket)) {
            player.drop(bucket, false);
        }
        return true;
    }

    // ── 访问器 ──

    public ItemStackHandler getFuelSlot() {
        return this.fuelSlot;
    }

    public FluidTank getLavaTank() {
        return this.lavaTank;
    }

    public FluidTank getHeatTank() {
        return this.heatTank;
    }

    public int getBurnTimeRemaining() {
        return (int) this.burnTimeRemaining;
    }

    public int getBurnTimeTotal() {
        return this.burnTimeTotal;
    }

    public int getProductionLvl() {
        return this.productionLvl;
    }

    public int getEnergySaveLvl() {
        return this.energySaveLvl;
    }

    public int getAutoRepairLvl() {
        return this.autoRepairLvl;
    }

    // ── MenuProvider / ContainerData ──

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.timex_rebirth.heat_station");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new HeatStationMenu(containerId, inventory, this, this);
    }

    /** data: 0 热流量 1 热流容量 2 熔岩量 3 熔岩容量 4 燃烧剩余 5 燃烧总长 6 增产等级 7 节能等级 8 血量 9 最大血量 */
    @Override
    public int get(int index) {
        return switch (index) {
            case 0 -> this.heatTank.getFluidAmount();
            case 1 -> this.heatTank.getCapacity();
            case 2 -> this.lavaTank.getFluidAmount();
            case 3 -> this.lavaTank.getCapacity();
            case 4 -> (int) this.burnTimeRemaining;
            case 5 -> this.burnTimeTotal;
            case 6 -> this.productionLvl;
            case 7 -> this.energySaveLvl;
            case 8 -> this.health;
            case 9 -> this.maxHealth;
            default -> 0;
        };
    }

    @Override
    public void set(int index, int value) {
        // 只读数据
    }

    @Override
    public int getCount() {
        return 10;
    }

    // ── 持久化 ──

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.owner != null) tag.putUUID("owner", this.owner);
        tag.putInt("health", this.health);
        tag.putInt("max_health", this.maxHealth);
        tag.put("fuel", this.fuelSlot.serializeNBT(registries));
        tag.put("lava", this.lavaTank.writeToNBT(registries, new CompoundTag()));
        tag.put("heat", this.heatTank.writeToNBT(registries, new CompoundTag()));
        tag.putDouble("burn_time", this.burnTimeRemaining);
        tag.putInt("burn_time_total", this.burnTimeTotal);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("owner")) this.owner = tag.getUUID("owner");
        if (tag.contains("health")) this.health = tag.getInt("health");
        if (tag.contains("max_health")) this.maxHealth = tag.getInt("max_health");
        if (tag.contains("fuel")) this.fuelSlot.deserializeNBT(registries, tag.getCompound("fuel"));
        if (tag.contains("lava")) this.lavaTank.readFromNBT(registries, tag.getCompound("lava"));
        if (tag.contains("heat")) this.heatTank.readFromNBT(registries, tag.getCompound("heat"));
        if (tag.contains("burn_time")) this.burnTimeRemaining = tag.getDouble("burn_time");
        if (tag.contains("burn_time_total")) this.burnTimeTotal = tag.getInt("burn_time_total");
    }
}
