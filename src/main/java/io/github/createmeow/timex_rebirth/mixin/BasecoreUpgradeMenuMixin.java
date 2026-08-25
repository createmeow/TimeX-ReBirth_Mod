package io.github.createmeow.timex_rebirth.mixin;

import dev.anye.mc.basecore.menu.upgrade.BasecoreUpgradeMenu;
import dev.anye.mc.basecore.menu.upgrade.UpgradeEntry;
import io.github.createmeow.timex_rebirth.research.TechTree;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 基地核心"模块制造"研究门控：
 * basecore 的模块在 component 模式下用零件购买（升级界面），不走工作台合成，
 * 因此在 BasecoreUpgradeMenu.processAction 购买时按研究权限拦截，未研究"模块制造"的玩家无法购买。
 */
@Mixin(BasecoreUpgradeMenu.class)
public abstract class BasecoreUpgradeMenuMixin {

    @Shadow
    private ServerPlayer serverPlayer;

    @Inject(method = "processAction", at = @At("HEAD"), cancellable = true)
    private void timex_rebirth$gateModulePurchase(int entryIndex, boolean isBuy, CallbackInfo ci) {
        if (!isBuy) return; // 仅拦截购买，卖出/退还不受研究限制
        BasecoreUpgradeMenu menu = (BasecoreUpgradeMenu) (Object) this;
        List<UpgradeEntry> entries = menu.getEntries();
        if (entryIndex < 0 || entryIndex >= entries.size()) return;
        if (serverPlayer == null) return;
        Item moduleItem = entries.get(entryIndex).getModule();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(moduleItem);
        String perm = TechTree.getItemUsePermission(id);
        if (perm != null && !TechTree.hasUsePermission(serverPlayer, perm)) {
            ci.cancel();
            // 动作栏提示（与研究系统一致的沉浸式文案）
            serverPlayer.displayClientMessage(
                    Component.translatable("research.timex_rebirth.locked_use"), true);
        }
    }
}
