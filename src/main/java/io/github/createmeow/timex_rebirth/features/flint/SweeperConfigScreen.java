package io.github.createmeow.timex_rebirth.features.flint;

import io.github.createmeow.timex_rebirth.network.SweeperConfigPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 清刷机配置菜单（潜行 + 右键打开）：五个开关（清理积雪 / 清理落叶 / 清理杂草/花 /
 * 对实体造成伤害 / 清扫防浪费）+ 两个滑条（清刷半径 0.5~5 格 / 清刷伤害 0~5）。
 * 点击切换与滑条释放即时同步服务端（{@link SweeperConfigPayload}）。
 */
public class SweeperConfigScreen extends Screen {

    private static final double RANGE_STEP = 0.5;
    private static final float DAMAGE_STEP = 0.5f;

    private boolean snow;
    private boolean leaves;
    private boolean plants;
    private boolean damage;
    private boolean noWaste;
    private double range;
    private float damageAmt;

    public SweeperConfigScreen(boolean snow, boolean leaves, boolean plants,
                               boolean damage, boolean noWaste, double range, float damageAmt) {
        super(Component.translatable("item.timex_rebirth.snow_sweeper.config"));
        this.snow = snow;
        this.leaves = leaves;
        this.plants = plants;
        this.damage = damage;
        this.noWaste = noWaste;
        this.range = range;
        this.damageAmt = damageAmt;
    }

    /** 从玩家主手/副手的清刷机读当前配置并打开（仅客户端调用）。 */
    public static void open(net.minecraft.world.entity.player.Player player) {
        for (var hand : net.minecraft.world.InteractionHand.values()) {
            var stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof SnowSweeperItem) {
                net.minecraft.client.Minecraft.getInstance().setScreen(new SweeperConfigScreen(
                        SweeperConfig.clearSnow(stack),
                        SweeperConfig.clearLeaves(stack),
                        SweeperConfig.clearPlants(stack),
                        SweeperConfig.damageEntities(stack),
                        SweeperConfig.preventWaste(stack),
                        SweeperConfig.range(stack),
                        SweeperConfig.damageAmount(stack)));
                return;
            }
        }
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 108;
        addRow(cx, y, "snow", () -> snow, v -> snow = v);
        addRow(cx, y + 24, "leaves", () -> leaves, v -> leaves = v);
        addRow(cx, y + 48, "plants", () -> plants, v -> plants = v);
        addRow(cx, y + 72, "damage", () -> damage, v -> damage = v);
        addRow(cx, y + 96, "no_waste", () -> noWaste, v -> noWaste = v);
        addSlider(cx, y + 122, "range", (range - SweeperConfig.MIN_RANGE)
                / (SweeperConfig.MAX_RANGE - SweeperConfig.MIN_RANGE));
        addSlider(cx, y + 144, "damage_amt", damageAmt / SweeperConfig.MAX_DAMAGE);
        addRenderableWidget(Button.builder(Component.translatable("gui.timex_rebirth.sweeper.done"),
                b -> onClose()).bounds(cx - 75, y + 170, 150, 20).build());
    }

    private void addRow(int cx, int y, String key, java.util.function.BooleanSupplier get,
                        java.util.function.Consumer<Boolean> set) {
        addRenderableWidget(Button.builder(rowLabel(key, get.getAsBoolean()), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(rowLabel(key, get.getAsBoolean()));
            sendConfig();
        }).bounds(cx - 75, y, 150, 20).build());
    }

    /** 添加数值滑条（步进 0.5），释放时同步服务端。initial 为归一化位置（0~1）。 */
    private void addSlider(int cx, int y, String key, double initial) {
        // 初始标签与拖动后的 updateMessage 一致：用 snapValue 把归一化值换算为实际数值
        double initialActual = snapValue(key, initial);
        addRenderableWidget(new AbstractSliderButton(cx - 75, y, 150, 20,
                sliderLabel(key, initialActual), initial) {
            @Override
            protected void updateMessage() {
                setMessage(sliderLabel(key, snapValue(key, this.value)));
            }

            @Override
            protected void applyValue() {
                double v = snapValue(key, this.value);
                if ("range".equals(key)) {
                    range = v;
                } else {
                    damageAmt = (float) v;
                }
            }

            @Override
            public void onRelease(double mouseX, double mouseY) {
                super.onRelease(mouseX, mouseY);
                sendConfig();
            }
        });
    }

    /** 把滑条归一化值吸附到 0.5 步进并换算为实际数值。 */
    private static double snapValue(String key, double normalized) {
        double value;
        if ("range".equals(key)) {
            value = SweeperConfig.MIN_RANGE + normalized
                    * (SweeperConfig.MAX_RANGE - SweeperConfig.MIN_RANGE);
        } else {
            value = normalized * SweeperConfig.MAX_DAMAGE;
        }
        return Math.round(value * 2) / 2.0;
    }

    private static Component sliderLabel(String key, double value) {
        String formatted = (value == Math.floor(value)) ? String.valueOf((int) value) : String.valueOf(value);
        return Component.translatable("gui.timex_rebirth.sweeper." + key)
                .copy().append("：" + formatted);
    }

    private void sendConfig() {
        PacketDistributor.sendToServer(new SweeperConfigPayload(
                snow, leaves, plants, damage, noWaste, range, damageAmt));
    }

    private static Component rowLabel(String key, boolean on) {
        return Component.translatable("gui.timex_rebirth.sweeper." + key)
                .copy().append("：").append(on
                        ? Component.translatable("gui.timex_rebirth.sweeper.on").withStyle(net.minecraft.ChatFormatting.GREEN)
                        : Component.translatable("gui.timex_rebirth.sweeper.off").withStyle(net.minecraft.ChatFormatting.RED));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 124, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
