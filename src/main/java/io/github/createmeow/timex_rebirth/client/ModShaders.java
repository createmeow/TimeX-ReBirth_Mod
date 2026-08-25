package io.github.createmeow.timex_rebirth.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

/**
 * Registers and provides access to custom GLSL shaders used by the wheel menu.
 * <p>
 * The ring shader renders an anti-aliased circular ring or arc in a single
 * GPU draw call (one quad), replacing the old CPU-based pixel-iteration
 * approach that caused severe lag on low-end devices.
 */
@SuppressWarnings("removal")
@EventBusSubscriber(modid = TimeX.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModShaders {

    @Nullable
    private static ShaderInstance ring;

    /**
     * @return the ring shader instance; never null after resource loading.
     * @throws NullPointerException if called before shaders have loaded.
     */
    public static ShaderInstance getRingShader() {
        return Objects.requireNonNull(ring, "Ring shader not loaded yet — attempted use before RegisterShadersEvent");
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        TimeX.rl("ring"),
                        DefaultVertexFormat.POSITION_TEX
                ),
                shader -> ring = shader
        );
        TimeX.LOGGER.info("TimeX ring shader registered");
    }
}
