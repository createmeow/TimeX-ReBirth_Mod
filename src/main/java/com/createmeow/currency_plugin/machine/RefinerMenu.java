package com.createmeow.currency_plugin.machine;

import com.createmeow.currency_plugin.init.CurrencyMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 提炼机菜单：2 个机器槽位（0=原料输入，1=产物输出）+ 玩家背包。
 * ContainerData 同步提炼量（0~99）供进度条渲染。
 */
public class RefinerMenu extends AbstractContainerMenu {

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    private final Container refiner;
    private final ContainerData data;

    /** 客户端构造：使用临时容器（数据由服务端同步）。 */
    public RefinerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new net.minecraft.world.SimpleContainer(RefinerBlockEntity.CONTAINER_SIZE),
                new net.minecraft.world.inventory.SimpleContainerData(1));
    }

    /** 服务端构造：直接使用方块实体容器与数据。 */
    public RefinerMenu(int containerId, Inventory playerInventory, Container refiner, ContainerData data) {
        super(CurrencyMenuTypes.REFINER_MENU.get(), containerId);
        this.refiner = refiner;
        this.data = data;

        // 原料输入槽 (56, 35)
        this.addSlot(new Slot(refiner, INPUT_SLOT, 56, 35));
        // 产物输出槽 (116, 35) — 只许取出
        this.addSlot(new Slot(refiner, OUTPUT_SLOT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // 玩家背包 3×9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // 快捷栏
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(data);
    }

    /** 仅服务端：获取当前打开的提炼机方块实体。 */
    public Container getRefiner() {
        return this.refiner;
    }

    /** 当前提炼量（0~99），由 ContainerData 同步。 */
    public int getRefinePoints() {
        return this.data.get(0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index == INPUT_SLOT || index == OUTPUT_SLOT) {
            // 机器槽 → 玩家背包
            if (!this.moveItemStackTo(stack, 2, 38, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 玩家背包 → 原料输入槽
            if (!this.moveItemStackTo(stack, INPUT_SLOT, OUTPUT_SLOT, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.refiner.stillValid(player);
    }
}
