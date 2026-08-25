package io.github.createmeow.timex_rebirth.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

/**
 * 临时诊断：确认 create 的粘液球/搅拌配方在服务端实际加载的是哪一个版本。
 * 打印 ingredient 可区分 create 原版(create:dough) 与我们覆盖(farmersdelight:wheat_dough)。
 */
public class RecipeDebugHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation SLIME = ResourceLocation.fromNamespaceAndPath("create", "crafting/appliances/slime_ball");
    private static final ResourceLocation DOUGH_MIX = ResourceLocation.fromNamespaceAndPath("create", "mixing/dough_by_mixing");
    private static final ResourceLocation BREAD_SMELT = ResourceLocation.fromNamespaceAndPath("create", "smelting/bread");

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        RecipeManager rm = event.getServer().getRecipeManager();
        // 数据包覆盖在 dev 下不生效，这里做运行时替换
        CreateRecipeOverride.apply(rm, event.getServer().registryAccess());
        rm.byKey(SLIME).ifPresent(holder -> {
            if (holder.value() instanceof ShapelessRecipe shapeless) {
                LOGGER.info("[TimeXRecipeDebug] AFTER slime_ball INGREDIENTS={}",
                        shapeless.getIngredients());
            }
        });
        rm.byKey(DOUGH_MIX).ifPresent(holder -> {
            LOGGER.info("[TimeXRecipeDebug] AFTER dough_mix RESULT={}",
                    holder.value().getResultItem(event.getServer().registryAccess()));
        });
        rm.byKey(BREAD_SMELT).ifPresent(holder -> {
            LOGGER.info("[TimeXRecipeDebug] AFTER smelting bread INGREDIENT={}", holder.value().getIngredients());
        });
    }
}
