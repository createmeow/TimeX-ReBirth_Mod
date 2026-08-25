package io.github.createmeow.timex_rebirth.heat.station;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * 模块插槽菜单：6 格模块容器 + 玩家背包。
 * 容器仅接受基地核心模块（BasecoreModuleItem）。
 */
public class HeatModuleSlotMenu extends AbstractContainerMenu {
    private static final ItemStackHandler EMPTY = new ItemStackHandler(6);

    public HeatModuleSlotMenu(int containerId, Inventory inventory, HeatModuleSlotBlockEntity be) {
        super(HeatStationRegistry.HEAT_MODULE_SLOT_MENU.get(), containerId);

        ItemStackHandler modules = be != null ? be.getModules() : EMPTY;
        // 6 格模块槽（2 行 × 3 列）
        int startX = 62;
        int startY = 20;
        for (int i = 0; i < 6; i++) {
            this.addSlot(new SlotItemHandler(modules, i, startX + (i % 3) * 18, startY + (i / 3) * 18));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    /** 客户端：仅渲染容器，快捷移动等方块实体操作仅服务端执行。 */
    public HeatModuleSlotMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (player.level().isClientSide) return ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (index < 6) {
            if (!this.moveItemStackTo(stack, 6, 42, true)) return ItemStack.EMPTY;
        } else {
            if (!(stack.getItem() instanceof dev.anye.mc.basecore.item.module.BasecoreModuleItem)) {
                return ItemStack.EMPTY;
            }
            if (!this.moveItemStackTo(stack, 0, 6, false)) return ItemStack.EMPTY;
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
