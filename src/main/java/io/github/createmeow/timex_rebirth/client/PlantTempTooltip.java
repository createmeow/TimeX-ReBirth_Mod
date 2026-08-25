package io.github.createmeow.timex_rebirth.client;

import com.mojang.datafixers.util.Either;
import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.client.key.ModKeys;
import io.github.createmeow.timex_rebirth.features.PlantFrostHandler;
import io.github.createmeow.timex_rebirth.features.PlantTempData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

/**
 * 作物环境信息 Tooltip（参考 FrostedHeart PlantTempStats / Cold Sweat TooltipHandler）：
 * 在背包/容器等 GUI 内鼠标悬停植物方块物品时，默认显示一行提示；
 * 按住 S 键（按键设置中可改）时展开该作物能存活的环境信息：耐寒/耐热评级、是否怕积雪、是否耐暴风雪。
 * 按键检测使用 GLFW 底层状态（同 FrostedHeart CInputHelper.isDown），
 * 因为 GUI 打开时 KeyMapping.isDown() 不可靠。
 */
@EventBusSubscriber(modid = TimeX.MODID, value = Dist.CLIENT)
public class PlantTempTooltip {

    @SubscribeEvent
    public static void onGatherTooltip(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) return;
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        Block block = blockItem.getBlock();
        if (!isPlantBlock(block)) return;

        boolean pressed = isKeyDown();
        Component keyName = ModKeys.KEY_CROP_INFO.getTranslatedKeyMessage();
        List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();

        // 提示行：未按住时仅显示"按住 [S] 查看"，按住时同样置顶
        MutableComponent hint = Component.translatable("tooltip.timex_rebirth.hold", keyName)
                .withStyle(pressed ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY);
        elements.add(Either.left(hint));
        if (!pressed) return;

        BlockState dummy = block.defaultBlockState();
        PlantTempData data = PlantFrostHandler.getPlantTempData(level, block);
        PlantFrostHandler.Rating frost = PlantFrostHandler.getFrostRating(dummy, data);
        PlantFrostHandler.Rating heat = PlantFrostHandler.getHeatRating(dummy, data);

        elements.add(Either.left(Component.empty()));
        elements.add(Either.left(Component.translatable("tooltip.timex_rebirth.frost_heat",
                rating(frost), rating(heat))));
        if (data != null && !data.snowVulnerable()) {
            elements.add(Either.left(Component.translatable("tooltip.timex_rebirth.snow_resist")));
        }
        if (data != null && !data.blizzardVulnerable()) {
            elements.add(Either.left(Component.translatable("tooltip.timex_rebirth.blizzard_resist")));
        }
    }

    /** 直接读取 GLFW 底层按键状态（同 FrostedHeart CInputHelper.isDown），尊重玩家改键。 */
    private static boolean isKeyDown() {
        return GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(),
                ModKeys.KEY_CROP_INFO.getKey().getValue()) == GLFW.GLFW_PRESS;
    }

    private static Component rating(PlantFrostHandler.Rating rating) {
        ChatFormatting color = switch (rating) {
            case LOW -> ChatFormatting.RED;
            case MEDIUM -> ChatFormatting.YELLOW;
            case HIGH -> ChatFormatting.GREEN;
        };
        return Component.translatable("tooltip.timex_rebirth.rating." + rating.name().toLowerCase(Locale.ROOT))
                .withStyle(color);
    }

    /** 与 PlantFrostHandler.isPlant 判定一致：只对植物类方块显示环境信息。 */
    private static boolean isPlantBlock(Block block) {
        if (block instanceof CropBlock || block instanceof SaplingBlock || block instanceof SweetBerryBushBlock
                || block instanceof CactusBlock || block instanceof FlowerBlock || block instanceof VineBlock
                || block instanceof MushroomBlock) {
            return true;
        }
        BlockState dummy = block.defaultBlockState();
        return dummy.is(BlockTags.CROPS) || dummy.is(BlockTags.SAPLINGS);
    }
}
