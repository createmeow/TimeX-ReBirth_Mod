package io.github.createmeow.timex_rebirth.mixin;

import com.mojang.datafixers.util.Pair;
import com.ordana.immersive_weathering.util.WeatheringHelper;
import io.github.createmeow.timex_rebirth.features.flint.IwTreeBarkBridge;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * IW 树皮刮削掉落替换（用户要求 mixin 修改，非配方）：
 * <ul>
 * <li>{@link WeatheringHelper#getBarkToStrip}——斧头刮削原木/竹块时的掉落物，
 *     替换为 {@code farmersdelight:tree_bark}（树）/ {@code farmersdelight:straw}（竹块）</li>
 * <li>{@link WeatheringHelper#getBarkForStrippedLog}——手持树皮修复剥皮原木的判定，
 *     同步替换，使 FD 树皮/秸秆可修复 IW 剥皮原木（与掉落物一致）</li>
 * </ul>
 * 注意：本 mixin 依赖 IW 常驻（与项目现有 FarmersDelight/Fiahi mixin 同惯例），
 * 若从整合包移除 IW 需同步从 timex_rebirth.mixins.json 移除本条目。
 */
@Mixin(WeatheringHelper.class)
public class WeatheringHelperMixin {

    @Inject(method = "getBarkToStrip", at = @At("RETURN"), cancellable = true)
    private static void timex$replaceBarkToStrip(BlockState normalLog, CallbackInfoReturnable<Item> cir) {
        Item original = cir.getReturnValue();
        if (original == null) return;
        cir.setReturnValue(IwTreeBarkBridge.replaceBark(normalLog, original));
    }

    @Inject(method = "getBarkForStrippedLog", at = @At("RETURN"), cancellable = true)
    private static void timex$replaceBarkForStrippedLog(
            BlockState stripped, CallbackInfoReturnable<Optional<Pair<Item, Block>>> cir) {
        Optional<Pair<Item, Block>> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) return;
        Pair<Item, Block> pair = original.get();
        cir.setReturnValue(Optional.of(Pair.of(
                IwTreeBarkBridge.replaceBark(stripped, pair.getFirst()), pair.getSecond())));
    }
}
