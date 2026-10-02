package io.github.createmeow.timex_rebirth.mixin;

import com.ordana.immersive_weathering.events.ModEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 修复 IW 刮削刷物品漏洞：
 * 原版 {@code AxeItem.playerHasShieldUseIntent}——主手持斧 + 副手持盾（未潜行）时，
 * {@code AxeItem.useOn} 直接 PASS（让盾牌优先举起），<b>不会剥皮</b>；
 * 而 IW 的 {@code axeStripping} 只判"手持斧 + 可剥"，无此检查 → 在事件里照常掉落树皮 → 可刷物品。
 * <p>
 * 此处在 IW 掉落逻辑执行前复刻原版盾牌意图判定，命中则跳过掉落（返回 PASS，
 * 与 IW 原方法默认返回一致，不拦截后续交互）。
 * <p>
 * 注意：本 mixin 依赖 IW 常驻（与项目 FarmersDelight/Fiahi mixin 同惯例），
 * 移除 IW 时需同步从 timex_rebirth.mixins.json 移除本条目。
 */
@Mixin(value = ModEvents.class, remap = false)
public class ModEventsMixin {

    @Inject(method = "axeStripping", at = @At("HEAD"), cancellable = true)
    private static void timex$guardShieldUseIntent(Item item, ItemStack stack, BlockPos pos, BlockState state,
                                                   Player player, Level level, InteractionHand hand,
                                                   BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        // 复刻原版 AxeItem.playerHasShieldUseIntent：命中时原版不会剥皮，IW 也不得掉落
        if (hand == InteractionHand.MAIN_HAND
                && player.getOffhandItem().is(Items.SHIELD)
                && !player.isSecondaryUseActive()) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
