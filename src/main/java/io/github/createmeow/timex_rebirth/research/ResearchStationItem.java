package io.github.createmeow.timex_rebirth.research;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * 研究站物品：可放置在任意位置（不依赖基地核心），右键打开研究界面。
 * 放宽放置限制以打破"基地核心←序列组装←机械手←黄铜机械研究←研究站"的死循环。
 */
public class ResearchStationItem extends BlockItem {
    public ResearchStationItem(Block block) {
        super(block, new Item.Properties());
    }
}
