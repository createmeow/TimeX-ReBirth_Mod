package io.github.createmeow.timex_rebirth.heat;

import dev.anye.mc.basecore.item.module.BasecoreModuleItem;

/**
 * 热源桥接模块（BaseCore 模块，最多 3 个）：
 * 放入基地核心槽位即启用基地热场（供热由热源接收器方块实体运行，
 * 模块数量决定温暖等级：1 个 → 3 级，2 个 → 7 级，3 个 → 10 级）。
 */
public class HeatBridgeModuleItem extends BasecoreModuleItem {

    public HeatBridgeModuleItem() {
        super(3, false); // 最多 3 个，无需自身 tick（接收器每 20 tick 读取模块数量）
    }
}
