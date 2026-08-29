package com.createmeow.nightvisiondevice;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 夜视镜：装备到头部（普通物品，非盔甲）。
 * 头部穿戴时不使用盔甲材质层，而是由原版按物品模型的 {@code display.head}
 * 配置渲染出 3D 眼镜（同「末地烛」「雕刻南瓜」的头戴物品渲染机制）。
 */
public class NightVisionDeviceItem extends Item implements Equipable {

    public NightVisionDeviceItem(Properties properties) {
        super(properties);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    /** 右键穿戴到头部。 */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty()) {
            player.setItemSlot(EquipmentSlot.HEAD, stack.split(1));
            level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return InteractionResultHolder.pass(stack);
    }
}