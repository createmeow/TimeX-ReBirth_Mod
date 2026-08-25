package io.github.createmeow.timex_rebirth.wasteland;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 废土物资相关事件：
 * - 吃完西瓜片（minecraft:melon_slice）获得 1 个西瓜皮；
 * - 击杀僵尸有 30% 概率掉落 1 件随机废旧物品（废土感来源，绘制台原料）；
 * - 右键原版火焰弹（minecraft:fire_charge）将其投掷出去（类似恶魂火球，伤害 20）。
 */
@EventBusSubscriber(modid = TimeX.MODID)
public class WastelandEvents {

    /** 僵尸掉落废旧物品的概率。 */
    private static final float SCRAP_DROP_CHANCE = 0.3F;

    @SubscribeEvent
    public static void onRightClickFireCharge(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!event.getItemStack().is(Items.FIRE_CHARGE)) return;
        event.setCanceled(true);
        if (player.level().isClientSide) {
            player.swing(event.getHand());
            return;
        }
        if (!player.getAbilities().instabuild) {
            event.getItemStack().shrink(1);
        }
        ThrownFireCharge fireball = new ThrownFireCharge(player.level(), player, player.getLookAngle().scale(1.5));
        player.level().addFreshEntity(fireball);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.getAbilities().instabuild) return;
        if (!event.getItem().is(Items.MELON_SLICE)) return;
        ItemStack rind = new ItemStack(WastelandRegistry.WATERMELON_RIND.get());
        if (!player.getInventory().add(rind)) {
            player.drop(rind, false);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof Zombie)) return;
        if (event.getEntity().getRandom().nextFloat() >= SCRAP_DROP_CHANCE) return;
        ItemStack scrap = WastelandRegistry.randomScrap(event.getEntity().getRandom());
        if (scrap.isEmpty()) return;
        var pos = event.getEntity().position();
        event.getDrops().add(new ItemEntity(event.getEntity().level(), pos.x, pos.y, pos.z, scrap));
    }
}
