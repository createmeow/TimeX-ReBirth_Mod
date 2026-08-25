package io.github.createmeow.timex_rebirth;

import dev.anye.mc.basecore.api.BasecoreUpgradeApi;
import io.github.createmeow.timex_rebirth.antifreeze.AntiFreezeRegistry;
import io.github.createmeow.timex_rebirth.crops.WinterCropRegistry;
import io.github.createmeow.timex_rebirth.food.WastelandFoodRegistry;
import io.github.createmeow.timex_rebirth.features.PlantTempDataMap;
import io.github.createmeow.timex_rebirth.heat.HeatRegistry;
import io.github.createmeow.timex_rebirth.heat.FireproofRegistry;
import io.github.createmeow.timex_rebirth.heat.HeatMaterialsRegistry;
import io.github.createmeow.timex_rebirth.heat.station.HeatStationEvents;
import io.github.createmeow.timex_rebirth.compat.BasecoreCompatRegistry;
import io.github.createmeow.timex_rebirth.heat.station.HeatStationRegistry;
import io.github.createmeow.timex_rebirth.network.TimeXNetwork;
import io.github.createmeow.timex_rebirth.research.ResearchEvents;
import io.github.createmeow.timex_rebirth.research.ResearchNetwork;
import io.github.createmeow.timex_rebirth.research.ResearchRegistry;
import io.github.createmeow.timex_rebirth.wasteland.WastelandEvents;
import io.github.createmeow.timex_rebirth.wasteland.WastelandRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(TimeX.MODID)
public class TimeX {
    public static final String MODID = "timex_rebirth";
    public static final Logger LOGGER = LoggerFactory.getLogger(TimeX.class);

    public TimeX(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("TimeX: Rebirth initializing");

        // 注册配置
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, TimeXConfig.SPEC);

        // 注册网络
        modEventBus.addListener(TimeXNetwork::register);

        // 注册植物温度数据映射（DataMap）
        modEventBus.addListener(PlantTempDataMap::register);

        // ── 冬季作物：黑麦 / 芜菁 ──
        WinterCropRegistry.register(modEventBus);

        // ── 废土食物：炖菜/浓汤/罐头/肉干/蒸馏水/草药茶 ──
        WastelandFoodRegistry.register(modEventBus);

        // ── 防冻系统：抗冻土壤 / 抗冻耕地 / 防冻剂 ──
        AntiFreezeRegistry.register(modEventBus);

        // ── 基地供热（热源联动）：方块/物品/流体/方块实体/创造标签 ──
        HeatRegistry.register(modEventBus);
        modEventBus.addListener(HeatRegistry::onRegisterCapabilities);

        // ── 供热工业基础材料：耐热合金锭 / 隔热玻璃 ──
        HeatMaterialsRegistry.register(modEventBus);

        // ── 防火材料：耐火砖 / 防火机壳 ──
        FireproofRegistry.register(modEventBus);

        // ── basecore 兼容：基地核心序列组装半成品 ──
        BasecoreCompatRegistry.register(modEventBus);

        // ── 热源供应站（松散多方块）：发生器/底座/燃料接收器/适配器/模块插槽 ──
        HeatStationRegistry.register(modEventBus);
        modEventBus.addListener(HeatStationRegistry::onRegisterCapabilities);
        NeoForge.EVENT_BUS.register(HeatStationEvents.class);

        // component 模式：向 BaseCore 升级 API 注册热源桥接模块（零件购买）
        if (ModList.get().isLoaded("basecore")) {
            BasecoreUpgradeApi.registerUpgrade(
                    HeatRegistry.HEAT_BRIDGE_MODULE::get,
                    () -> TimeXConfig.HEAT_BRIDGE_MODULE_COST.get());
        }

        // ── 团队研究 / 科技树：研究站 + 玩家研究数据 + 解锁拦截 ──
        // ResearchCommands 通过 @EventBusSubscriber 自动注册（勿重复手动注册，否则命令注册事件触发两次）
        ResearchRegistry.register(modEventBus);
        modEventBus.addListener(ResearchNetwork::register);
        NeoForge.EVENT_BUS.register(ResearchEvents.class);

        // ── 废土物资：废旧物品 / 西瓜皮 / 爆炸箭 / 绘制台（废旧物品→阅历）──
        WastelandRegistry.register(modEventBus);
        NeoForge.EVENT_BUS.register(WastelandEvents.class);
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
