package io.github.createmeow.timex_rebirth.crops;

import net.minecraft.world.level.ItemLike;

/**
 * 黑麦作物（冬季作物，耐寒）：
 * - 低温/积雪/暴风雪中正常生长（PlantTempData 配 frost=high、snow/blizzard_vulnerable=false）
 * - 置于基地热场中生长显著加速（WinterCropBlock.randomTick）
 * 收获获得黑麦（可食用 + 工业原料）与黑麦种子。
 */
public class RyeCropBlock extends WinterCropBlock {

    public RyeCropBlock(Properties properties) {
        super(properties);
    }

    @Override
    public ItemLike getBaseSeedId() {
        return WinterCropRegistry.RYE_SEEDS.get();
    }
}
