package io.github.createmeow.timex_rebirth.features;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 破袜子：
 * <ul>
 *   <li>右键 → 穿戴到脚部（可装备槽位）；</li>
 *   <li>SHIFT+右键地面 → 放置为破袜子方块（刮绒毛用）；</li>
 *   <li>湿水状态 {@link FireToolRegistry#WET_TORN_SOCKS} 需烤干恢复。</li>
 * </ul>
 */
public class TornSocksItem extends Item implements Equipable {

    public TornSocksItem(Properties properties) {
        super(properties);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.FEET;
    }

    /** 右键穿戴到脚部。 */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);
        if (feet.isEmpty()) {
            // 从手上取出 1 个穿到脚部，剩余的返回（手上不应再有原物品）
            ItemStack remaining = stack.split(1);
            player.setItemSlot(EquipmentSlot.FEET, remaining);
            level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_GENERIC.value(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            // 返回穿戴后手部剩余的 ItemStack（原 < 1 个时为空），避免手上残留幽灵物品
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return InteractionResultHolder.pass(stack);
    }

    /** SHIFT+右键：放置为地上的破袜子方块。 */
    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;

        net.minecraft.core.BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        var placeContext = new net.minecraft.world.item.context.BlockPlaceContext(level, player,
                context.getHand(), context.getItemInHand(), new net.minecraft.world.phys.BlockHitResult(
                context.getClickLocation(), context.getClickedFace(), pos, false));
        net.minecraft.world.level.block.state.BlockState state =
                FireToolRegistry.TORN_SOCKS_BLOCK.get().getStateForPlacement(placeContext);
        if (state != null && level.getBlockState(pos).canBeReplaced() && state.canSurvive(level, pos)) {
            level.setBlock(pos, state, Block.UPDATE_ALL);
            // 把当前物品的耐久同步到方块实体
            if (level.getBlockEntity(pos) instanceof TornSocksBlockEntity be) {
                be.syncFromItem(context.getItemInHand());
            }
            if (!player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
            level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8F, 0.9F);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    /** 被穿戴时检测湿化（由 WetnessHandler 统一调用）。 */
    public static boolean isSocks(ItemStack stack) {
        return stack.is(FireToolRegistry.TORN_SOCKS.get()) || stack.is(FireToolRegistry.WET_TORN_SOCKS.get());
    }

    /** LivingEntity 脚部是否穿着任意袜子（供湿化判定）。 */
    public static boolean wearingSocks(net.minecraft.world.entity.LivingEntity entity) {
        return isSocks(entity.getItemBySlot(EquipmentSlot.FEET));
    }
}
