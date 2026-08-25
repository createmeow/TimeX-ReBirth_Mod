package io.github.createmeow.timex_rebirth.features;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * 植物温度数据映射注册（NeoForge DataMap）。
 * 数据包文件：data/timex_rebirth/data_maps/block/plant_temp.json
 * （DataMap JSON 必须位于 data/<namespace>/data_maps/<registry>/<id>.json，
 *  错误放在 data/ 根目录会导致整包不被加载，全部回退到默认等级）
 */
public class PlantTempDataMap {
    public static final DataMapType<Block, PlantTempData> TYPE = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(TimeX.MODID, "plant_temp"),
            Registries.BLOCK,
            PlantTempData.CODEC
    ).build();

    public static void register(RegisterDataMapTypesEvent event) {
        event.register(TYPE);
    }
}
