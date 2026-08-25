package io.github.createmeow.timex_rebirth.client;

import io.github.createmeow.timex_rebirth.network.ResearchActionPayload;
import io.github.createmeow.timex_rebirth.research.TechNode;
import io.github.createmeow.timex_rebirth.research.TechTree;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 研究站界面（客户端）：
 * 左侧科技树节点列表，右侧选中节点详情 + 单独研究 / 帮助 / 协助操作与基地成员列表。
 * 操作通过 ResearchActionPayload 发送到服务端执行。
 */
@OnlyIn(Dist.CLIENT)
public class ResearchStationScreen extends Screen {
    private static final int PANEL_W = 360;
    private static final int PANEL_H = 240;
    /** 节点列表可视区高度（px） */
    private static final int LIST_VIEW_H = 200;

    private int panelX;
    private int panelY;
    /** 列表像素滚动偏移 */
    private int scrollPx;
    private String selectedNode;

    public ResearchStationScreen() {
        super(Component.translatable("container.timex_rebirth.research_station"));
    }

    @Override
    protected void init() {
        panelX = (this.width - PANEL_W) / 2;
        panelY = (this.height - PANEL_H) / 2;
        selectedNode = null;
        // 若正在研究中，默认选中该节点便于查看
        if (ClientResearchState.hasActiveSession()) {
            selectedNode = ClientResearchState.getSessionNode();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // super.render 先绘制背景（模糊+暗化）；面板/文本画在其后，否则会被背景覆盖
        super.render(graphics, mouseX, mouseY, partialTick);
        drawPanel(graphics, mouseX, mouseY);
    }

    private void drawPanel(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xC0101010);
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + 1, 0xFF888888);
        g.fill(panelX, panelY + PANEL_H - 1, panelX + PANEL_W, panelY + PANEL_H, 0xFF888888);
        g.fill(panelX, panelY, panelX + 1, panelY + PANEL_H, 0xFF888888);
        g.fill(panelX + PANEL_W - 1, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xFF888888);

        g.drawCenteredString(this.font,
                Component.translatable("container.timex_rebirth.research_station"),
                panelX + PANEL_W / 2, panelY + 6, 0xFFFFFFFF);

        drawNodeList(g, mouseX, mouseY);
        drawRightPanel(g, mouseX, mouseY);
    }

    // ── 左侧：节点列表（分类标题 + 节点，按像素滚动）──

    private void drawNodeList(GuiGraphics g, int mouseX, int mouseY) {
        List<Object> entries = buildEntries();
        int x = panelX + 10;
        int top = panelY + 28;
        int bottom = top + LIST_VIEW_H;
        for (int i = 0; i < entries.size(); i++) {
            Object entry = entries.get(i);
            int rowY = top + entryOffset(i) - scrollPx;
            if (rowY >= bottom) break;
            if (rowY + entryHeight(entry) <= top) continue; // 已滚出顶部
            if (entry instanceof String category) {
                g.drawString(this.font, Component.translatable("research.timex_rebirth.category." + category),
                        x, rowY, 0xFFFFAA55);
                continue;
            }
            TechNode node = (TechNode) entry;
            int rowH = 20;
            boolean selected = node.id().equals(selectedNode);
            boolean hovered = isInside(mouseX, mouseY, x, rowY, 138, rowH - 2);
            int bg = selected ? 0xFF3A3A66 : hovered ? 0xFF2A2A3A : 0xFF202020;
            g.fill(x, rowY, x + 138, rowY + rowH - 2, bg);
            int color;
            if (ClientResearchState.isUnlocked(node.id())) {
                color = 0xFF9A9A9A;
            } else if (ClientResearchState.hasActiveSession() && node.id().equals(ClientResearchState.getSessionNode())) {
                color = 0xFF55FFFF;
            } else if (!hasDepsClient(node)) {
                color = 0xFFAA5555;
            } else {
                color = 0xFFFFFFFF;
            }
            g.drawString(this.font, Component.translatable(node.getNameTranslationKey()), x + 4, rowY + 5, color);
            if (ClientResearchState.isUnlocked(node.id())) {
                g.drawString(this.font, "✓", x + 128, rowY + 5, 0xFF55AA55);
            }
        }
    }

    /** 扁平展示项：分类标题（String）+ 节点（TechNode）。 */
    private List<Object> buildEntries() {
        List<Object> entries = new ArrayList<>();
        for (Map.Entry<String, List<TechNode>> group : TechTree.getNodesByCategory().entrySet()) {
            entries.add(group.getKey());
            entries.addAll(group.getValue());
        }
        return entries;
    }

    private static int entryHeight(Object entry) {
        return entry instanceof String ? 12 : 20;
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

    private int totalEntriesHeight() {
        int y = 0;
        for (Object entry : buildEntries()) {
            y += entryHeight(entry);
        }
        return y;
    }

    private int maxScrollPx() {
        return Math.max(0, totalEntriesHeight() - LIST_VIEW_H);
    }

    private boolean hasDepsClient(TechNode node) {
        for (String dep : node.dependencies()) {
            if (!ClientResearchState.isUnlocked(dep)) return false;
        }
        return true;
    }

    // ── 右侧：详情 + 操作 ──

    private void drawRightPanel(GuiGraphics g, int mouseX, int mouseY) {
        int x = panelX + 158;
        int y = panelY + 28;
        int w = 192;

        g.drawString(this.font, Component.translatable("research.timex_rebirth.points", ClientResearchState.getPoints()),
                x, y, 0xFFFFFF55);

        TechNode node = selectedNode == null ? null : TechTree.getNode(selectedNode);
        int detailY = y + 16;
        if (node == null) {
            g.drawString(this.font, Component.translatable("research.timex_rebirth.select_hint"), x, detailY, 0xFF888888);
        } else {
            g.drawString(this.font, Component.translatable(node.getNameTranslationKey()), x, detailY, 0xFFFFFFFF);
            g.drawString(this.font, Component.translatable("research.timex_rebirth.cost", node.cost()),
                    x, detailY + 10, 0xFFCCCCCC);
            g.drawString(this.font, Component.translatable("research.timex_rebirth.duration", formatDuration(node.durationSeconds())),
                    x, detailY + 20, 0xFFCCCCCC);
            // 前置
            if (!node.dependencies().isEmpty()) {
                StringBuilder deps = new StringBuilder();
                for (String dep : node.dependencies()) {
                    TechNode depNode = TechTree.getNode(dep);
                    if (depNode != null) {
                        if (deps.length() > 0) deps.append(", ");
                        deps.append(Component.translatable(depNode.getNameTranslationKey()).getString());
                    }
                }
                g.drawString(this.font, Component.translatable("research.timex_rebirth.deps", deps.toString()),
                        x, detailY + 30, hasDepsClient(node) ? 0xFF55CC55 : 0xFFCC5555);
            }
            // 详细介绍（最多显示 3 行，超出末尾省略）
            List<FormattedCharSequence> descLines = this.font.split(Component.translatable(node.getDescTranslationKey()), w);
            int descY = detailY + 42;
            for (int i = 0; i < descLines.size() && i < 3; i++) {
                if (i == 2 && descLines.size() > 3) {
                    String s = formattedToString(descLines.get(i));
                    g.drawString(this.font, s.substring(0, Math.max(1, s.length() - 1)) + "…", x, descY, 0xFF888888);
                } else {
                    g.drawString(this.font, descLines.get(i), x, descY, 0xFF888888);
                }
                descY += 10;
            }
            // 单独研究按钮
            boolean canSolo = !ClientResearchState.isUnlocked(node.id())
                    && !ClientResearchState.isGlobalResearching() && hasDepsClient(node);
            drawButton(g, x, detailY + 76, w, 14, Component.translatable("research.timex_rebirth.btn_solo"),
                    canSolo, mouseX, mouseY);
            // 帮助/协助按钮
            g.drawString(this.font, Component.translatable("research.timex_rebirth.members_title"),
                    x, detailY + 98, 0xFFAAAAFF);
            // 成员列表与会话进度区（panelY+194 起）重叠：限制可见行数，其余以 "+N 人" 提示
            int memberY = detailY + 110;
            int memberLimitY = panelY + 190;
            int shown = 0;
            List<ClientResearchState.Member> members = ClientResearchState.getMembers();
            for (ClientResearchState.Member member : members) {
                if (memberY + 14 > memberLimitY) break;
                g.drawString(this.font, member.name(), x, memberY, 0xFFEEEEEE);
                boolean canHelp = ClientResearchState.isUnlocked(node.id()) && !ClientResearchState.isGlobalResearching();
                drawButton(g, x + 78, memberY, 32, 12, Component.literal("帮助"), canHelp, mouseX, mouseY);
                boolean canCollab = !ClientResearchState.isUnlocked(node.id()) && !ClientResearchState.isGlobalResearching();
                drawButton(g, x + 114, memberY, 32, 12, Component.literal("协助"), canCollab, mouseX, mouseY);
                shown++;
                memberY += 14;
            }
            if (shown < members.size()) {
                g.drawString(this.font, Component.translatable("research.timex_rebirth.members_more",
                                members.size() - shown),
                        x, memberY, 0xFF888888);
            }
        }

        // 会话进度
        int sessionY = y + 166;
        if (ClientResearchState.hasActiveSession()) {
            g.drawString(this.font, Component.translatable("research.timex_rebirth.session_active",
                            Component.translatable(sessionNameKey(ClientResearchState.getSessionNode()))),
                    x, sessionY, 0xFF55FFFF);
            String participants = String.join(", ", ClientResearchState.getParticipantNames());
            g.drawString(this.font, Component.translatable("research.timex_rebirth.session_participants", participants),
                    x, sessionY + 10, 0xFFCCCCCC);
            int progress = ClientResearchState.getSessionProgress();
            int duration = Math.max(1, ClientResearchState.getSessionDuration());
            g.fill(x, sessionY + 24, x + w, sessionY + 28, 0xFF333333);
            int fill = (int) (w * Math.min(1.0F, (float) progress / duration));
            g.fill(x, sessionY + 24, x + fill, sessionY + 28, 0xFF55FF55);
            g.drawString(this.font, Component.translatable("research.timex_rebirth.session_time",
                            progress / 20 / 60, duration / 20 / 60),
                    x, sessionY + 32, 0xFFCCCCCC);
        } else if (ClientResearchState.isGlobalResearching()) {
            // 在其他研究站研究中：本站无会话进度，提示已在进行其他研究
            g.drawString(this.font, Component.translatable("research.timex_rebirth.already_researching"),
                    x, sessionY, 0xFFFFAA55);
        }
    }

    private String sessionNameKey(String nodeId) {
        TechNode node = TechTree.getNode(nodeId);
        return node == null ? nodeId : node.getNameTranslationKey();
    }

    private void drawButton(GuiGraphics g, int x, int y, int w, int h, Component text, boolean enabled, int mouseX, int mouseY) {
        boolean hovered = enabled && isInside(mouseX, mouseY, x, y, w, h);
        g.fill(x, y, x + w, y + h, enabled ? (hovered ? 0xFF4455AA : 0xFF333366) : 0xFF333333);
        g.fill(x, y, x + w, y + 1, 0xFF888888);
        g.drawCenteredString(this.font, text, x + w / 2, y + (h - 8) / 2, enabled ? 0xFFFFFFFF : 0xFF777777);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        // 节点列表点击（分类标题不可选，仅节点可选中）
        List<Object> entries = buildEntries();
        int x = panelX + 10;
        int top = panelY + 28;
        for (int i = 0; i < entries.size(); i++) {
            Object entry = entries.get(i);
            if (entry instanceof String) continue;
            int rowY = top + entryOffset(i) - scrollPx;
            if (rowY < top || rowY >= top + LIST_VIEW_H) continue;
            if (isInside((int) mouseX, (int) mouseY, x, rowY, 138, entryHeight(entry) - 2)) {
                selectedNode = ((TechNode) entry).id();
                return true;
            }
        }
        // 右侧按钮
        TechNode node = selectedNode == null ? null : TechTree.getNode(selectedNode);
        if (node != null) {
            int dx = panelX + 158;
            int dy = panelY + 28 + 16 + 76;
            boolean canSolo = !ClientResearchState.isUnlocked(node.id())
                    && !ClientResearchState.isGlobalResearching() && hasDepsClient(node);
            if (canSolo && isInside((int) mouseX, (int) mouseY, dx, dy, 192, 14)) {
                sendAction("start_solo", node.id(), null);
                return true;
            }
            int memberY = panelY + 28 + 16 + 98 + 12;
            int memberLimitY = panelY + 190;
            for (ClientResearchState.Member member : ClientResearchState.getMembers()) {
                if (memberY + 14 > memberLimitY) break; // 与渲染一致：会话进度区以上的成员才可点击
                if (isInside((int) mouseX, (int) mouseY, dx + 78, memberY, 32, 12)) {
                    if (ClientResearchState.isUnlocked(node.id()) && !ClientResearchState.isGlobalResearching()) {
                        sendAction("help", node.id(), member.id());
                        return true;
                    }
                }
                if (isInside((int) mouseX, (int) mouseY, dx + 114, memberY, 32, 12)) {
                    if (!ClientResearchState.isUnlocked(node.id()) && !ClientResearchState.isGlobalResearching()) {
                        sendAction("collab", node.id(), member.id());
                        return true;
                    }
                }
                memberY += 14;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int old = scrollPx;
        int step = 12;
        scrollPx = Math.max(0, Math.min(maxScrollPx(), scrollPx - (int) Math.signum(deltaY) * step));
        return scrollPx != old || super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    private void sendAction(String action, String nodeId, UUID partner) {
        PacketDistributor.sendToServer(new ResearchActionPayload(action, nodeId, partner,
                ClientResearchState.getStationPos()));
        // 关闭界面，等服务端同步回来（界面数据自动刷新）
        this.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean isInside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** 将 FormattedCharSequence 还原为字符串（用于省略号截断）。 */
    private static String formattedToString(FormattedCharSequence seq) {
        StringBuilder sb = new StringBuilder();
        seq.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        return sb.toString();
    }

    private static String formatDuration(int seconds) {
        int minutes = seconds / 60;
        if (minutes < 60) return minutes + " 分";
        int hours = minutes / 60;
        int rem = minutes % 60;
        return rem == 0 ? hours + " 小时" : hours + " 时" + rem + " 分";
    }
}
