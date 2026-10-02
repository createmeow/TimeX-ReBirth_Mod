package com.createmeow.currency_plugin.machine;

import com.createmeow.currency_plugin.init.CurrencyBlockEntities;
import com.createmeow.currency_plugin.init.CurrencyItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * 提炼机方块实体。
 * <p>2 个槽位（0=原料输入，1=产物输出），内部维护提炼量（0~99）。
 * 服务端每 {@link #PROCESS_INTERVAL_TICKS} tick 消耗 1 个原料，
 * 按配方表随机获得提炼量，每满 100 点产出 1 个腐空朽。</p>
 * <p>提炼量配方（随机区间）：
 * 腐肉 1~5，利爪 10~20，暗影腺体 5~15，聚合心脏 30~50，异变肿瘤 20~30。</p>
 */
public class RefinerBlockEntity extends BlockEntity implements Container, WorldlyContainer,
        net.minecraft.world.MenuProvider {

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int CONTAINER_SIZE = 2;
    /** 每 100 点提炼量产出 1 个腐空朽。 */
    public static final int POINTS_PER_COIN = 100;
    /** 消耗 1 个原料的周期（tick），2 秒。 */
    public static final int PROCESS_INTERVAL_TICKS = 40;

    /** 提炼量配方：物品注册名 → [最小提炼量, 最大提炼量]（含两端）。 */
    private static final Map<String, int[]> REFINE_VALUES = Map.of(
            "minecraft:rotten_flesh", new int[]{1, 5},
            "mutanter:claw", new int[]{10, 20},
            "mutanter:shadow_gland", new int[]{5, 15},
            "mutanter:amalgamation_heart", new int[]{30, 50},
            "mutanter:abnormal_tumor", new int[]{20, 30}
    );

    /** 配方表只读视图（供 JEI 展示使用）。 */
    public static Map<String, int[]> getRefineValues() {
        return REFINE_VALUES;
    }

    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    /** 当前累积提炼量（0~99）。 */
    private int refinePoints = 0;
    /** 处理冷却计数。 */
    private int cooldown = 0;

    /** 服务端→客户端同步的提炼量（进度条）。 */
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? refinePoints : 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) refinePoints = value;
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public RefinerBlockEntity(BlockPos pos, BlockState state) {
        super(CurrencyBlockEntities.REFINING_MACHINE.get(), pos, state);
    }

    // ─────────────────────────── 服务端逻辑 ───────────────────────────

    public static void serverTick(Level level, BlockPos pos, BlockState state, RefinerBlockEntity refiner) {
        if (refiner.cooldown > 0) {
            refiner.cooldown--;
            return;
        }
        refiner.cooldown = PROCESS_INTERVAL_TICKS;

        ItemStack input = refiner.items.get(INPUT_SLOT);
        if (input.isEmpty()) return;

        int[] range = REFINE_VALUES.get(BuiltInRegistries.ITEM.getKey(input.getItem()).toString());
        if (range == null) return;

        // 产物槽必须为空或还能堆叠腐空朽
        ItemStack output = refiner.items.get(OUTPUT_SLOT);
        ItemStack coin = new ItemStack(CurrencyItems.COMMON_COIN.get());
        if (!output.isEmpty() && !ItemStack.isSameItemSameComponents(output, coin)) return;
        if (output.getCount() >= output.getMaxStackSize()) return;

        // 消耗 1 个原料，随机获得提炼量
        input.shrink(1);
        int gained = RandomSource.create().nextInt(range[0], range[1] + 1);
        refiner.refinePoints += gained;

        // 每满 100 点产出 1 个腐空朽
        while (refiner.refinePoints >= POINTS_PER_COIN) {
            refiner.refinePoints -= POINTS_PER_COIN;
            if (output.isEmpty()) {
                refiner.items.set(OUTPUT_SLOT, coin.copy());
            } else {
                output.grow(1);
            }
            output = refiner.items.get(OUTPUT_SLOT);
        }

        refiner.setChanged();
    }

    /** 方块被移除时掉落槽位内容物。 */
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        items.clear();
    }

    // ─────────────────────────── NBT 持久化 ───────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("RefinePoints", refinePoints);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        refinePoints = tag.getInt("RefinePoints");
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    // ─────────────────────────── Container（供 Menu 使用） ───────────────────────────

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        // ContainerData（提炼量）由 ServerPlayer.tick 的 broadcastChanges 每 tick 自动同步
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player, 8.0F);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    // ─────────────────────────── WorldlyContainer（漏斗自动化） ───────────────────────────
    // 漏斗只能往输入槽（槽 0）放入合法原料，只能从输出槽（槽 1）取出腐空朽。

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{INPUT_SLOT, OUTPUT_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return slot == INPUT_SLOT && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot != INPUT_SLOT) return false;
        return REFINE_VALUES.containsKey(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    // ─────────────────────────── MenuProvider ───────────────────────────

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.currency_plugin.refining_machine");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory playerInventory, Player player) {
        return new RefinerMenu(containerId, playerInventory, this, this.dataAccess);
    }

    public int getRefinePoints() {
        return refinePoints;
    }
}
