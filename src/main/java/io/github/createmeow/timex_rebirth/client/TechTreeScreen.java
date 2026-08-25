package io.github.createmeow.timex_rebirth.client;

import io.github.createmeow.timex_rebirth.research.TechNode;
import io.github.createmeow.timex_rebirth.research.TechTree;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 科技树查看界面（只读）：从 ALT 轮盘打开。
 * 展示全部节点：名称 / 所需研究点数 / 时长 / 前置 / 详细介绍（解锁的配方与方块使用方法）/ 是否已解锁。
 * 行高随介绍行数自适应，按像素滚动。
 */
@OnlyIn(Dist.CLIENT)
public class TechTreeScreen extends Screen {
    /** 列表内容起始 y（标题下方） */
    private static final int TOP = 28;
    /** 每行文字高度 */
    private static final int LINE_H = 10;
    /** 列表像素滚动偏移 */
    private int scrollPx;

    public TechTreeScreen() {
        super(Component.translatable("gui.timex_rebirth.tech_tree.title"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // super.render 会先绘制背景（模糊+暗化）；文本必须画在其后，否则会被背景覆盖
        super.render(g, mouseX, mouseY, partialTick);
        int x = 12;
        g.drawString(this.font, Component.translatable("gui.timex_rebirth.tech_tree.title"), x, 12, 0xFFFFFF55);
        g.drawString(this.font, Component.translatable("research.timex_rebirth.points", ClientResearchState.getPoints()),
                this.width - 100, 12, 0xFFFFFF55);

        int contentWidth = this.width - 36;
        List<Object> entries = buildEntries();
        for (int i = 0; i < entries.size(); i++) {
            Object entry = entries.get(i);
            int rowY = TOP + entryOffset(i) - scrollPx;
            if (rowY >= this.height - 14) break;
            if (rowY + entryHeight(entry) <= TOP) continue; // 已滚出顶部
            if (entry instanceof String category) {
                g.drawString(this.font, Component.translatable("research.timex_rebirth.category." + category),
                        x, rowY, 0xFFFFAA55);
                continue;
            }
            TechNode node = (TechNode) entry;
            boolean unlocked = ClientResearchState.isUnlocked(node.id());
            boolean deps = hasDeps(node);
            int headerColor = unlocked ? 0xFF55AA55 : deps ? 0xFFFFFFFF : 0xFFAA5555;

            String name = Component.translatable(node.getNameTranslationKey()).getString();
            String cost = Component.translatable("research.timex_rebirth.cost", node.cost()).getString();
            String duration = Component.translatable("research.timex_rebirth.duration", formatDuration(node.durationSeconds())).getString();
            String status = unlocked ? "✓ " + Component.translatable("research.timex_rebirth.status_unlocked").getString()
                    : Component.translatable("research.timex_rebirth.status_locked").getString();

            g.drawString(this.font, name + "  " + status, x, rowY, headerColor);
            g.drawString(this.font, cost + "  " + duration, x + 8, rowY + LINE_H, 0xFFCCCCCC);
            int lineY = rowY + 2 * LINE_H;
            if (!node.dependencies().isEmpty()) {
                StringBuilder depsStr = new StringBuilder();
                for (String dep : node.dependencies()) {
                    TechNode depNode = TechTree.getNode(dep);
                    if (depNode != null) {
                        if (depsStr.length() > 0) depsStr.append(", ");
                        depsStr.append(Component.translatable(depNode.getNameTranslationKey()).getString());
                    }
                }
                g.drawString(this.font, Component.translatable("research.timex_rebirth.deps", depsStr.toString()),
                        x + 8, lineY, deps ? 0xFF55CC55 : 0xFFCC5555);
                lineY += LINE_H;
            }
            // 详细介绍（自动换行显示完整介绍）
            for (FormattedCharSequence line : this.font.split(Component.translatable(node.getDescTranslationKey()), contentWidth)) {
                g.drawString(this.font, line, x + 8, lineY, 0xFF888888);
                lineY += LINE_H;
            }
        }
        g.drawCenteredString(this.font, Component.translatable("gui.timex_rebirth.tech_tree.close_hint"),
                this.width / 2, this.height - 12, 0xFF888888);
    }

    /** 扁平展示项：分类标题（String）+ 节点（TechNode），保持分类内顺序。 */
    private List<Object> buildEntries() {
        List<Object> entries = new ArrayList<>();
        for (Map.Entry<String, List<TechNode>> group : TechTree.getNodesByCategory().entrySet()) {
            entries.add(group.getKey());
            entries.addAll(group.getValue());
        }
        return entries;
    }

    /** 第 index 个条目相对列表顶部的像素偏移。 */
    private int entryOffset(int index) {
        int y = 0;
        List<Object> entries = buildEntries();
        for (int i = 0; i < index && i < entries.size(); i++) {
            y += entryHeight(entries.get(i));
        }
        return y;
    }

    /** 条目高度：分类标题固定 14，节点按 名称/消耗时长/前置 + 介绍行数 自适应。 */
    private int entryHeight(Object entry) {
        if (entry instanceof String) return 14;
        TechNode node = (TechNode) entry;
        int lines = node.dependencies().isEmpty() ? 2 : 3;
        lines += this.font.split(Component.translatable(node.getDescTranslationKey()), this.width - 36).size();
        return lines * LINE_H + 8;
    }

    private int totalHeight() {
        int y = 0;
        for (Object entry : buildEntries()) {
            y += entryHeight(entry);
        }
        return y;
    }

    private boolean hasDeps(TechNode node) {
        for (String dep : node.dependencies()) {
            if (!ClientResearchState.isUnlocked(dep)) return false;
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int old = scrollPx;
        int max = Math.max(0, totalHeight() - (this.height - TOP - 20));
        scrollPx = Math.max(0, Math.min(max, scrollPx - (int) Math.signum(deltaY) * 12));
        return scrollPx != old || super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String formatDuration(int seconds) {
        int minutes = seconds / 60;
        if (minutes < 60) return minutes + " 分";
        int hours = minutes / 60;
        int rem = minutes % 60;
        return rem == 0 ? hours + " 小时" : hours + " 时" + rem + " 分";
    }
}
