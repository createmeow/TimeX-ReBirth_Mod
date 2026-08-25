package io.github.createmeow.timex_rebirth.heat.station;

import dev.anye.mc.basecore.item.module.BasecoreModuleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * 模块插槽方块实体：仅接受基地核心模块（BasecoreModuleItem）的容器。
 * 相邻发生器每 20 tick 统计库存内的模块数量获得升级效果：
 * - 荆棘模块 → 非成员破坏反伤；自动修复模块 → 定时修复结构血量
 * - 节能模块 → 降低燃料消耗；增产模块 → 提升热流产出
 */
public class HeatModuleSlotBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler modules = new ItemStackHandler(6) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof BasecoreModuleItem;
        }
    };

    public HeatModuleSlotBlockEntity(BlockPos pos, BlockState state) {
        super(HeatStationRegistry.HEAT_MODULE_SLOT_BE.get(), pos, state);
    }

    /** 待机 tick：效果统计由相邻发生器主动读取，本实体仅存储。 */
    public void tick() {
    }

    public ItemStackHandler getModules() {
        return this.modules;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.timex_rebirth.heat_module_slot");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new HeatModuleSlotMenu(containerId, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("modules", this.modules.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("modules")) this.modules.deserializeNBT(registries, tag.getCompound("modules"));
    }
}
