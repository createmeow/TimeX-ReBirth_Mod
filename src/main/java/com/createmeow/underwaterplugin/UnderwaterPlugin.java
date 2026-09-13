package com.createmeow.underwaterplugin;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * 水下探索插件（underwater_plugin）— Fourth mod in this JAR。
 * 功能：
 * - 深度加权溺水伤害（越深溺水伤害越高）
 * - 溺水值系统（缺氧积累、满氧恢复；水中按溺水值减速，18 点完全无法移动）
 * - 过深耗氧飙升（BetterDiving 机制：超过 20 格后每 8 格耗氧 +1 倍；
 *   无装备作用于原版氧气，Create 潜水头盔+背罐作用于背罐空气）
 * - 深度温度削减（Cold Sweat 联动：每下潜 1 格环境温度额外 -2）
 */
@Mod(UnderwaterPlugin.MODID)
public class UnderwaterPlugin {
    public static final String MODID = "underwater_plugin";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UnderwaterPlugin(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        UnderwaterAttachments.ATTACHMENTS.register(modEventBus);
        // 物品/方块/方块实体注册（Create 相关条目仅在 Create 已加载时注册）
        UnderwaterRegisters.register(modEventBus);

        // Cold Sweat 深度温度修正注册（TempModifier 需在其 ServerAboutToStart 事件前注册）
        if (ModList.get().isLoaded("cold_sweat")) {
            ColdSweatIntegration.init();
        }

        // DrowningDamageHandler / DrowningValueHandler / DepthOxygenHandler /
        // DepthTemperatureHandler 通过 @EventBusSubscriber 自动注册到 NeoForge.EVENT_BUS
        LOGGER.info("[underwater_plugin] 水下探索插件已加载 - 溺水值/过深耗氧/深度温度");
    }
}
