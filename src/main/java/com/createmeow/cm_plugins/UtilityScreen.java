package com.createmeow.cm_plugins;

import io.github.createmeow.timex_rebirth.client.hud.HudVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 实用功能面板：命令快捷方式 + 自动合并钱币开关（C2S 同步）+ 当前坐标（X Y Z D）显示开关。
 * 由轮盘「实用功能」选项打开。
 */
public class UtilityScreen extends Screen {

    /** 客户端缓存的「自动合并钱币」状态（由服务端 S2C 同步）。 */
    private static boolean clientAutoCoin = true;
    private static UtilityScreen instance;

    private Button coinButton;
    private Button coordsButton;

    public UtilityScreen() {
        super(Component.literal("实用功能"));
    }

    /** 服务端 S2C 同步状态到客户端，并刷新打开的屏幕按钮文字。 */
    public static void setClientAutoCoin(boolean enabled) {
        clientAutoCoin = enabled;
        if (instance != null) {
            instance.refreshCoinButton();
        }
    }

    @Override
    protected void init() {
        instance = this;
        int cx = this.width / 2;
        int w = 200;
        int x = cx - w / 2;
        int y = this.height / 2 - 60;

        this.addRenderableWidget(Button.builder(Component.literal("戴帽子 (/hat)"), b -> runCommand("hat"))
                .bounds(x, y, w, 20).build());
        y += 24;

        this.addRenderableWidget(Button.builder(Component.literal("展示坐标 (/showxyz)"), b -> runCommand("showxyz"))
                .bounds(x, y, w, 20).build());
        y += 24;

        this.coinButton = this.addRenderableWidget(Button.builder(coinLabel(), b ->
                        PacketDistributor.sendToServer(new ToggleAutoCoinPayload()))
                .bounds(x, y, w, 20).build());
        y += 24;

        this.coordsButton = this.addRenderableWidget(Button.builder(coordsLabel(), b -> {
                    HudVariables.set("xyz", HudVariables.is("xyz", "开") ? "关" : "开");
                    coordsButton.setMessage(coordsLabel());
                })
                .bounds(x, y, w, 20).build());
        y += 30;

        this.addRenderableWidget(Button.builder(Component.literal("关闭"), b -> this.onClose())
                .bounds(x, y, w, 20).build());
    }

    private Component coinLabel() {
        return Component.literal("自动合并钱币: " + (clientAutoCoin ? "开" : "关"));
    }

    private Component coordsLabel() {
        return Component.literal("坐标显示: " + (HudVariables.is("xyz", "开") ? "开" : "关"));
    }

    private void refreshCoinButton() {
        if (coinButton != null) {
            coinButton.setMessage(coinLabel());
        }
    }

    private void runCommand(String command) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            mc.getConnection().sendUnsignedCommand(command);
        }
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, Component.literal("实用功能"),
                this.width / 2, this.height / 2 - 80, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        if (instance == this) {
            instance = null;
        }
    }
}