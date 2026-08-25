package io.github.createmeow.timex_rebirth.mixin;

import io.github.createmeow.timex_rebirth.features.FrozenSoilHandler;
import io.github.createmeow.timex_rebirth.features.PlantFrostHandler;
import io.github.createmeow.timex_rebirth.features.SnowAccumulationHandler;
import io.github.createmeow.timex_rebirth.features.TemperatureBlockUpdateHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 温度驱动的区块更新统一挂钩（tickChunk RETURN）：
 * - 雪累积（SnowAccumulationHandler）
 * - 水面结冰等温度方块更新（TemperatureBlockUpdateHandler）
 * - 冻土转换（FrozenSoilHandler）
 * - 植物快速枯萎（PlantFrostHandler）
 * 参考 FrostedHeart ServerLevelMixin_PlaceExtraSnow / ServerLevelMixin_TemperatureUpdate。
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin_PlaceExtraSnow {

    @Inject(method = "tickChunk", at = @At("RETURN"))
    private void timex_rebirth$temperatureUpdates(LevelChunk chunk, int tickSpeed, CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        SnowAccumulationHandler.placeExtraSnow(level, chunk);
        TemperatureBlockUpdateHandler.tickChunk(level, chunk);
        FrozenSoilHandler.tickChunk(level, chunk);
        PlantFrostHandler.tickChunk(level, chunk);
    }
}
