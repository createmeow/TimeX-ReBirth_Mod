package io.github.createmeow.timex_rebirth.heat.station;

import dev.anye.mc.basecore.item.module.BasecoreModuleItem;

/**
 * 节能模块（热源供应站新增模块）：
 * 放入模块插槽后每级降低热源发生器燃料消耗速率 10%，最大 5 级。
 */
public class EnergySaveModuleItem extends BasecoreModuleItem {
    public EnergySaveModuleItem() {
        super(5);
    }
}
