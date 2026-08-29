package io.github.createmeow.timex_rebirth.features;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * 湿化与灼伤 tick 处理：
 * <ul>
 *   <li>穿着破袜子 + 踩水 / 下雪天露天 → 破袜子变湿水的破袜子（脚部直接替换）；</li>
 *   <li>地上的绒毛/掉落物绒毛：下雪天露天或被水冲 → 变潮湿的绒毛（不可作点火原）；</li>
 *   <li>手持点燃的涂蜡纸板且耐久 &lt; 15s → 每秒 1 点火焰伤害（方块侧在 BE 内处理，此处处理物品形态）。</li>
 * </ul>
 */
@EventBusSubscriber(modid = TimeX.MODID)
public class WetnessHandler {

    /** 每 20 tick 检查一次。 */
    @SubscribeEvent
    public static void onPlayerTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().getGameTime() % 20 != 0) return;

        // ── 脚部袜子湿化 ──
        ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);
        if (feet.is(FireToolRegistry.TORN_SOCKS.get())) {
            boolean inWater = player.isInWater() || player.level().getBlockState(player.blockPosition()).liquid()
                    || player.level().getBlockState(player.blockPosition().below()).liquid();
            boolean snowExposed = player.level().isRaining() && isSkyVisibleSnow(player);
            if (inWater || snowExposed) {
                ItemStack wet = new ItemStack(FireToolRegistry.WET_TORN_SOCKS.get());
                wet.setDamageValue(feet.getDamageValue());
                player.setItemSlot(EquipmentSlot.FEET, wet);
            }
        }

        // ── 手持点燃纸板低耐久灼伤 ──
        for (ItemStack held : new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()}) {
            if (held.getItem() instanceof WaxedCardboardItem && WaxedCardboardItem.remainingSeconds(held) < 15
                    && isCardboardLitComponent(held)) {
                player.hurt(player.damageSources().inFire(), 1.0F);
            }
        }
    }

    /** 掉落物湿化：绒毛掉落物在雨雪天露天或水中 → 潮湿的绒毛。 */
    @SubscribeEvent
    public static void onItemEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) return;
        if (itemEntity.level().isClientSide()) return;
        if (itemEntity.level().getGameTime() % 40 != 0) return;

        ItemStack stack = itemEntity.getItem();
        ServerLevel level = (ServerLevel) itemEntity.level();
        BlockPos pos = itemEntity.blockPosition();

        // 绒毛 → 潮湿的绒毛
        if (stack.is(FireToolRegistry.FUZZ.get())) {
            boolean wet = level.getBlockState(pos).liquid()
                    || (level.isRaining() && level.canSeeSky(pos));
            if (wet) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    itemEntity.discard();
                }
                ItemEntity wetFuzz = new ItemEntity(level,
                        itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                        new ItemStack(FireToolRegistry.WET_FUZZ.get(), 1));
                level.addFreshEntity(wetFuzz);
            }
            return;
        }

        // 点燃的绒毛 → 入水/雨雪 → 熄灭成"潮湿绒毛"（仍可用作暖手/擦干后用作点火原料）
        if (stack.is(FireToolRegistry.LIT_FUZZ.get())) {
            boolean wet = level.getBlockState(pos).liquid()
                    || (level.isRaining() && level.canSeeSky(pos));
            if (wet) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    itemEntity.discard();
                }
                ItemEntity wetFuzz = new ItemEntity(level,
                        itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                        new ItemStack(FireToolRegistry.WET_FUZZ.get(), 1));
                level.addFreshEntity(wetFuzz);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.2F);
            }
            return;
        }

        // 涂蜡纸板（未点燃） → 入水/雨雪 → 仍可继续使用（不影响），跳过
        // 涂蜡的燃烧纸板（LIT_WAXED_CARDBOARD）→ 入水/雨雪 → 熄灭为未点燃的纸板（保留剩余秒数）
        if (stack.is(FireToolRegistry.LIT_WAXED_CARDBOARD.get())) {
            boolean wet = level.getBlockState(pos).liquid()
                    || (level.isRaining() && level.canSeeSky(pos));
            if (wet) {
                // 把物品替换为 WAXED_CARDBOARD（同一个物品类，但已熄灭的版本：通过
                // 检查 damageValue 即可——WaxedCardboardItem.remainingSeconds 仍可用）
                ItemStack extinguished = new ItemStack(FireToolRegistry.WAXED_CARDBOARD.get(), 1);
                // 保留剩余秒数：damage = maxDamage - remaining
                extinguished.setDamageValue(stack.getDamageValue());
                stack.shrink(1);
                if (stack.isEmpty()) {
                    itemEntity.discard();
                }
                ItemEntity ex = new ItemEntity(level,
                        itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                        extinguished);
                level.addFreshEntity(ex);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.2F);
            }
            return;
        }
    }

    /** 玩家是否处于降雪天气下的露天位置。 */
    private static boolean isSkyVisibleSnow(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        // 本模组的气候体系以降雪为主：isRaining 即视为可能下雪，露天判定用 canSeeSky
        return level.canSeeSky(player.blockPosition().above());
    }

    /** 物品是否带"点燃"组件标记。 */
    private static boolean isCardboardLitComponent(ItemStack stack) {
        // 点燃状态通过自定义 data component 标记（LitCardboardComponent），此处简化为物品类型判断：
        // LIT_WAXED_CARDBOARD 是独立注册物品
        return stack.is(FireToolRegistry.LIT_WAXED_CARDBOARD.get());
    }
}
