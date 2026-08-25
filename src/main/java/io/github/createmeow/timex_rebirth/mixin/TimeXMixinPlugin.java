package io.github.createmeow.timex_rebirth.mixin;

import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin 条件应用插件：仅当对应可选模组加载时才应用其兼容 Mixin。
 * <p>注意：禁止在 onLoad 中用 {@code Class.forName} 探测模组类（会连带加载目标类导致其它模组
 * 抛 {@code MixinTargetAlreadyLoadedException}），一律用 {@link ModList#get().isLoaded} 判断。
 */
public class TimeXMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains("AppleSkin")) {
            return ModList.get() != null && ModList.get().isLoaded("appleskin");
        }
        if (mixinClassName.contains("FarmersDelight")) {
            return ModList.get() != null && ModList.get().isLoaded("farmersdelight");
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
