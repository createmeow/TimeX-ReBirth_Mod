package io.github.createmeow.timex_rebirth.heat.station;

import dev.anye.mc.basecore.item.module.BasecoreModuleItem;

/**
 * 增产模块（热源供应站新增模块）：
 * 放入模块插槽后每级提升热源发生器热流产出速率 10%，最大 5 级。
 */
public class ProductionModuleItem extends BasecoreModuleItem {
    public ProductionModuleItem() {
        super(5);
    }
}
