package io.github.createmeow.timex_rebirth.wheelmenu;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 轮盘选项行为抽象（参考 FrostedHeart wheelmenu Action）。
 * 选中（selected）与悬停（hovered）行为均以 Action 表示。
 */
@OnlyIn(Dist.CLIENT)
@FunctionalInterface
public interface Action {
    /** 无操作 */
    Action NO_ACTION = selection -> {
    };

    /**
     * 执行行为。
     *
     * @param selection 当前触发的选项
     */
    void execute(WheelSelection selection);

    /** 将 Runnable 包装为 Action */
    static Action of(Runnable runnable) {
        return selection -> runnable.run();
    }
}
