package io.github.createmeow.timex_rebirth.features;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 点燃的绒毛：由绒毛堆被明火点燃时掉落，作为引火工具。
 * <ul>
 *   <li><b>耐久规则</b>：物品 maxDamage=40；初始 damageValue 由绒毛数量决定（数量×10 耐久），
 *       即 1 个绒毛的绒毛块=10 耐久，4 个的绒毛块=40 耐久。</li>
 *   <li>SHIFT+右键篝火/灶台/熔炉 → 点燃，必定消耗 1 点耐久；</li>
 *   <li>可作熔炉燃料，燃烧时长按剩余耐久折算。</li>
 * </ul>
 */
public class LitFuzzItem extends Item {

    /** 单个 lit_fuzz 的最大耐久上限（4 个绒毛 = 40 耐久）。 */
    public static final int MAX_DURABILITY = 40;

    /** 单个绒毛提供的耐久（1 个绒毛 → 10 耐久）。 */
    public static final int DURABILITY_PER_FUZZ = 10;

    public LitFuzzItem(Properties properties) {
        super(properties);
    }

    /** 剩余秒数 = 剩余耐久。 */
    public static int remainingSeconds(ItemStack stack) {
        return stack.getMaxDamage() - stack.getDamageValue();
    }

    /**
     * 按绒毛数量设置初始耐久（用于绒毛块被点燃后掉落 lit_fuzz）。
     * @param fuzzCount 绒毛数量 1~4
     */
    public static void initFromFuzzCount(ItemStack stack, int fuzzCount) {
        int durability = Math.max(1, Math.min(MAX_DURABILITY, fuzzCount * DURABILITY_PER_FUZZ));
        // damage = maxDamage - 目标耐久
        stack.setDamageValue(MAX_DURABILITY - durability);
    }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        Level level = context.getLevel();
        {
            net.minecraft.core.BlockPos pos = context.getClickedPos();
            BlockState target = level.getBlockState(pos);
            boolean ok;
            if (target.getBlock() instanceof net.minecraft.world.level.block.AbstractFurnaceBlock) {
                ok = FireManager.tryIgniteFurnace(level, pos, target, level.getBlockEntity(pos));
            } else if (target.getBlock() instanceof net.minecraft.world.level.block.CampfireBlock
                    || (net.neoforged.fml.ModList.get().isLoaded("farmersdelight")
                        && target.getBlock() instanceof vectorwing.farmersdelight.common.block.AbstractStoveBlock)) {
                var be = level.getBlockEntity(pos);
                ok = be != null && FireManager.tryIgnite(level, pos, target, be, null);
            } else {
                // 其它方块：默认在点击面的相对位置放一个正常火焰（仿打火石）
                ok = FireManager.placeFire(level, pos, context.getClickedFace());
            }
            if (ok && !player.getAbilities().instabuild) {
                context.getItemInHand().hurtAndBreak(1, player,
                        net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            }
            if (ok) {
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            return ok ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
    }
}
