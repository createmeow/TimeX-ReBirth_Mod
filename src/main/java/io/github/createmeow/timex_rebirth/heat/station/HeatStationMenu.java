package io.github.createmeow.timex_rebirth.heat.station;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * 热源发生器菜单：燃料槽 + 玩家背包 + 10 组同步数据
 * （热流量/容量、熔岩量/容量、燃烧进度、增产/节能等级、结构血量）。
 */
public class HeatStationMenu extends AbstractContainerMenu {
    private static final ItemStackHandler EMPTY = new ItemStackHandler(1);
    private final ContainerData data;

    /** 服务端：传入发生器方块实体与数据。 */
    public HeatStationMenu(int containerId, Inventory inventory, HeatStationBlockEntity be, ContainerData data) {
        super(HeatStationRegistry.HEAT_STATION_MENU.get(), containerId);
        this.data = data;

        ItemStackHandler fuel = be != null ? be.getFuelSlot() : EMPTY;
        // 燃料槽：燃烧进度条正下方（说明文字紧邻其下）
        this.addSlot(new SlotItemHandler(fuel, 0, 80, 78));

        // 玩家背包（3 行）+ 快捷栏
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 19 + col * 18, 118 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 19 + col * 18, 172));
        }

        this.addDataSlots(data);
    }

    /** 客户端：仅使用同步数据渲染，方块实体操作（快捷移动）仅服务端执行。 */
    public HeatStationMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, new SimpleContainerData(10));
    }

    public ContainerData getData() {
        return this.data;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (player.level().isClientSide) return ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (index == 0) {
            if (!this.moveItemStackTo(stack, 1, 37, true)) return ItemStack.EMPTY;
        } else {
            if (StationFuelUtil.isSolidFuel(stack)) {
                if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
