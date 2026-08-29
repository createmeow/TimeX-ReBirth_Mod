package io.github.createmeow.timex_rebirth.features;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;

/**
 * 涂蜡的纸板：80 秒引火棒（耐久 80 = 燃烧秒数）。
 * <ul>
 *   <li><b>放置</b>：右键地面放置为<strong>未点燃的纸板方块</strong>（携带剩余秒数），
 *       需<strong>外部热源</strong>（邻居明火 / SHIFT 引火）点燃后才会燃烧；</li>
 *   <li><b>引火</b>：SHIFT+右键篝火/灶台/熔炉 → 必定点燃，扣 1 点耐久（纸板保持自身状态）；</li>
 *   <li><b>燃料</b>：可作熔炉燃料，燃烧时长按剩余耐久折算；</li>
 *   <li>燃烧中耐久 &lt; 15 时手持者每秒受 1 火焰伤害；烧尽只还灰烬。</li>
 * </ul>
 * 耐久语义：damage = 已损坏值，剩余秒数 = maxDamage - damageValue。
 */
public class WaxedCardboardItem extends Item {

    public WaxedCardboardItem(Properties properties) {
        super(properties);
    }

    /** 剩余燃烧秒数 = 剩余耐久 = maxDamage - damageValue。 */
    public static int remainingSeconds(ItemStack stack) {
        return stack.getMaxDamage() - stack.getDamageValue();
    }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ItemStack stack = context.getItemInHand();

        // SHIFT+右键火焰类方块或未点燃纸板 → 仅<b>燃烧状态</b>的纸板才可引火（未点燃的不能点火）
        if (player.isShiftKeyDown() && stack.is(FireToolRegistry.LIT_WAXED_CARDBOARD.get())) {
            return igniteFromHand(context, level, player, stack);
        }

        // 普通右键：区分"点燃/未点燃"纸板 → 放置对应方块（保持燃烧状态）
        if (level.isClientSide()) {
            player.swing(context.getHand());
            return InteractionResult.SUCCESS;
        }
        BlockPos placePos = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(placePos).canBeReplaced()) return InteractionResult.FAIL;
        if (!level.getBlockState(placePos.below()).isSolidRender(level, placePos.below())) return InteractionResult.FAIL;

        boolean lit = stack.is(FireToolRegistry.LIT_WAXED_CARDBOARD.get());
        BlockState placed = lit
                ? FireToolRegistry.LIT_WAXED_CARDBOARD_BLOCK.get().defaultBlockState()
                : FireToolRegistry.WAXED_CARDBOARD_BLOCK.get().defaultBlockState();
        level.setBlock(placePos, placed, Block.UPDATE_ALL);
        if (level.getBlockEntity(placePos) instanceof LitWaxedCardboardBlockEntity litBe) {
            litBe.getData(FireManager.FUEL_DATA).setFuelTicks(remainingSeconds(stack));
            litBe.setChanged();
        } else if (level.getBlockEntity(placePos) instanceof UnlitWaxedCardboardBlockEntity unlitBe) {
            unlitBe.setSeconds(remainingSeconds(stack));
        }
        level.playSound(null, placePos, lit ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.WOOD_PLACE,
                SoundSource.BLOCKS, 0.8F, 1.0F);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    /** SHIFT+右键引火：点燃目标（篝火/灶台/熔炉）或未点燃的纸板方块。 */
    private InteractionResult igniteFromHand(net.minecraft.world.item.context.UseOnContext context,
                                             Level level, Player player, ItemStack stack) {
        BlockPos pos = context.getClickedPos();
        BlockState target = level.getBlockState(pos);
        if (level.isClientSide()) {
            player.swing(context.getHand());
            return InteractionResult.SUCCESS;
        }

        // 目标（篝火/灶台/熔炉）
        boolean ok;
        if (target.getBlock() instanceof net.minecraft.world.level.block.AbstractFurnaceBlock) {
            ok = FireManager.tryIgniteFurnace(level, pos, target, level.getBlockEntity(pos));
        } else if (target.getBlock() instanceof net.minecraft.world.level.block.CampfireBlock
                || (net.neoforged.fml.ModList.get().isLoaded("farmersdelight")
                    && target.getBlock() instanceof vectorwing.farmersdelight.common.block.AbstractStoveBlock)) {
            var be = level.getBlockEntity(pos);
            ok = be != null && FireManager.tryIgnite(level, pos, target, be, null);
        } else if (target.getBlock() instanceof UnlitWaxedCardboardBlock) {
            // 用本纸板点燃另一块未点燃纸板 → 直接把它替换为点燃方块（保留其剩余耐久）
            ok = FireInteractHandler.igniteNeighbors(level, pos);
        } else {
            // 其它方块：默认在点击面的相对位置放一个正常火焰（仿打火石）
            ok = FireManager.placeFire(level, pos, context.getClickedFace());
        }

        if (ok && !player.getAbilities().instabuild) {
            stack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        }
        return ok ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    // ── 事件层 ──

    /** 熔炉燃料：涂蜡纸板燃烧值 = 剩余耐久秒数 × 20 tick（纸板需打火许可，走 FurnaceFuelMixin 门控）。 */
    @EventBusSubscriber(modid = "timex_rebirth")
    public static class FuelEvents {
        @SubscribeEvent
        public static void onFurnaceFuel(FurnaceFuelBurnTimeEvent event) {
            ItemStack stack = event.getItemStack();
            if (stack.getItem() instanceof WaxedCardboardItem) {
                event.setBurnTime(remainingSeconds(stack) * 20);
            } else if (stack.is(FireToolRegistry.TORN_SOCKS.get()) || stack.is(FireToolRegistry.WET_TORN_SOCKS.get())) {
                event.setBurnTime(200); // 破袜子/湿袜子：10s
            } else if (stack.is(FireToolRegistry.FUZZ.get())) {
                event.setBurnTime(50); // 绒毛：2.5s（潮湿的绒毛不可作为燃料）
            }
        }
    }
}
