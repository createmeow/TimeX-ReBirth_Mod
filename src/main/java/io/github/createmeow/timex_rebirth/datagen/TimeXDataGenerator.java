package io.github.createmeow.timex_rebirth.datagen;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

/**
 * 数据生成入口（DataGen）：收集配方/文案/掉落表等 JSON。
 * 运行方式：{@code ./gradlew runData}，输出到 {@code src/generated/resources}。
 */
@EventBusSubscriber(modid = TimeX.MODID, bus = EventBusSubscriber.Bus.MOD)
public class TimeXDataGenerator {

    @SubscribeEvent
    public static void gather(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = event.getGenerator().getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        ExistingFileHelper helper = event.getExistingFileHelper();

        // 配方
        generator.addProvider(event.includeServer(), new TimeXRecipeProvider(output, lookupProvider));
    }
}
