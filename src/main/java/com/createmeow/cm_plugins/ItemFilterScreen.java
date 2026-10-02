package com.createmeow.cm_plugins;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 物品过滤设置界面：顶部搜索框支持三种搜索模式（前缀切换）：
 * <ul>
 *   <li>纯文本 — 按物品名称 / 注册名搜索物品；</li>
 *   <li>{@code #词} — 搜索物品标签（如 #planks），点击结果过滤整个标签内的物品；</li>
 *   <li>{@code @词} — 搜索模组 ID（如 @minecraft），点击结果过滤该模组全部物品。</li>
 * </ul>
 * 搜索框为空时显示当前已过滤的物品（便于取消过滤）。
 * 点击物品切换「过滤 / 不过滤」，立即同步服务端。
 * 由「实用功能 → 物品过滤 → 设置」打开。
 */
public class ItemFilterScreen extends Screen {

    /** 客户端缓存的过滤开关（S2C 同步，供实用功能面板显示）。 */
    static volatile boolean clientEnabled = false;
    /** 客户端缓存的过滤物品注册名集合（S2C 同步）。 */
    private static volatile Set<String> clientItems = new HashSet<>();
    private static ItemFilterScreen instance;

    /** 全部已注册物品：注册名 → 本地化显示名（首次打开时构建一次）。 */
    private static volatile Map<String, String> allItems;

    /** 搜索结果条目：物品 / 标签 / 模组。 */
    private record Entry(String key, ItemStack icon, String prefix, List<String> itemIds) {
    }

    private EditBox searchBox;
    private net.minecraft.client.gui.components.Button prevButton;
    private net.minecraft.client.gui.components.Button nextButton;
    /** 当前搜索词匹配的全部条目。 */
    private List<Entry> matched = List.of();
    /** 当前页（0 起）。 */
    private int page = 0;
    /** 当前页展示的条目。 */
    private List<Entry> pageEntries = List.of();
    private int matchCount = 0;

    private static final int CELL = 20;     // 单元格间距
    private static final int ITEM = 16;     // 物品渲染尺寸
    private static final int COLS = 9;      // 每行数量
    private static final int MAX_CELLS = COLS * 5;

    public ItemFilterScreen() {
        super(Component.literal("物品过滤设置"));
    }

    /** 总页数。 */
    private int pageCount() {
        return Math.max(1, (matched.size() + MAX_CELLS - 1) / MAX_CELLS);
    }

    /** S2C 同步入口：更新客户端缓存并刷新打开的界面。 */
    public static void setClientState(boolean enabled, String joinedIds) {
        clientEnabled = enabled;
        clientItems = joinedIds.isEmpty()
                ? new HashSet<>()
                : new HashSet<>(Arrays.asList(joinedIds.split("\\|")));
        if (instance != null) {
            instance.buildDisplay(false);
        }
        UtilityScreen.refreshFilterButton();
    }

    /** 首次调用时构建全注册物品索引（注册名 → 本地化名）。 */
    private static Map<String, String> getAllItems() {
        Map<String, String> map = allItems;
        if (map == null) {
            map = new HashMap<>();
            for (var item : BuiltInRegistries.ITEM) {
                String id = BuiltInRegistries.ITEM.getKey(item).toString();
                map.put(id, new ItemStack(item).getHoverName().getString());
            }
            map = Map.copyOf(map);
            allItems = map;
        }
        return map;
    }

    @Override
    protected void init() {
        instance = this;
        int cx = this.width / 2;
        this.searchBox = this.addRenderableWidget(new EditBox(this.font, cx - 100, this.height / 2 - 86, 200, 16,
                Component.literal("搜索物品")));
        this.searchBox.setHint(Component.literal("名称 / #标签 / @模组ID").withStyle(ChatFormatting.DARK_GRAY));
        this.searchBox.setMaxLength(128);
        this.searchBox.setResponder(s -> buildDisplay(true));
        this.setInitialFocus(this.searchBox);

        // 翻页按钮（网格下方固定位置）
        this.prevButton = this.addRenderableWidget(net.minecraft.client.gui.components.Button
                .builder(Component.literal("◀"), b -> {
                    if (page > 0) {
                        page--;
                        buildDisplay(false);
                    }
                })
                .bounds(cx - 74, this.height / 2 + 66, 30, 16)
                .build());
        this.nextButton = this.addRenderableWidget(net.minecraft.client.gui.components.Button
                .builder(Component.literal("▶"), b -> {
                    if (page < pageCount() - 1) {
                        page++;
                        buildDisplay(false);
                    }
                })
                .bounds(cx + 44, this.height / 2 + 66, 30, 16)
                .build());
        buildDisplay(false);
    }

    /** 根据搜索词重建匹配条目并切分当前页。 */
    private void buildDisplay(boolean resetPage) {
        String term = searchBox != null ? searchBox.getValue().trim().toLowerCase(Locale.ROOT) : "";
        List<Entry> result = new ArrayList<>();

        if (term.isEmpty()) {
            // 无搜索词：显示当前已过滤的物品
            for (String id : clientItems.stream().sorted().toList()) {
                ItemStack icon = itemIcon(id);
                if (!icon.isEmpty()) {
                    result.add(new Entry(id, icon, "", List.of(id)));
                }
            }
        } else if (term.startsWith("#")) {
            // 标签搜索：匹配标签 id，点击过滤整个标签
            String q = term.substring(1);
            List<ResourceLocation> tagNames = BuiltInRegistries.ITEM.getTagNames()
                    .map(k -> (ResourceLocation) k.location())
                    .filter(rl -> rl.toString().toLowerCase(Locale.ROOT).contains(q))
                    .sorted(ResourceLocation::compareTo)
                    .toList();
            for (ResourceLocation tagRl : tagNames) {
                List<String> itemIds = BuiltInRegistries.ITEM
                        .getTag(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, tagRl))
                        .map(named -> named.stream()
                                .map(h -> BuiltInRegistries.ITEM.getKey(h.value()).toString())
                                .sorted()
                                .toList())
                        .orElse(List.of());
                if (itemIds.isEmpty()) continue;
                ItemStack icon = itemIcon(itemIds.get(0));
                if (icon.isEmpty()) continue;
                result.add(new Entry("#" + tagRl, icon, "#", itemIds));
            }
        } else if (term.startsWith("@")) {
            // 模组 ID 搜索：匹配命名空间，点击过滤该模组全部物品
            String q = term.substring(1);
            Map<String, List<String>> byNamespace = new TreeMap<>();
            for (var item : BuiltInRegistries.ITEM) {
                byNamespace.computeIfAbsent(BuiltInRegistries.ITEM.getKey(item).getNamespace(), k -> new ArrayList<>())
                        .add(BuiltInRegistries.ITEM.getKey(item).toString());
            }
            for (Map.Entry<String, List<String>> e : byNamespace.entrySet()) {
                if (e.getKey().toLowerCase(Locale.ROOT).contains(q)) {
                    List<String> itemIds = e.getValue().stream().sorted().toList();
                    ItemStack icon = itemIcon(itemIds.get(0));
                    if (icon.isEmpty()) continue;
                    result.add(new Entry("@" + e.getKey(), icon, "@", itemIds));
                }
            }
        } else {
            // 名称 / 注册名搜索
            Map<String, String> all = getAllItems();
            for (Map.Entry<String, String> entry : all.entrySet()) {
                if (entry.getValue().toLowerCase(Locale.ROOT).contains(term)
                        || entry.getKey().toLowerCase(Locale.ROOT).contains(term)) {
                    result.add(new Entry(entry.getKey(), itemIcon(entry.getKey()), "", List.of(entry.getKey())));
                }
            }
            result.sort((a, b) -> a.key().compareTo(b.key()));
        }

        matched = result;
        matchCount = result.size();
        if (resetPage) {
            page = 0;
        }
        page = Math.min(page, pageCount() - 1);

        // 切分当前页
        int from = page * MAX_CELLS;
        int to = Math.min((page + 1) * MAX_CELLS, matched.size());
        pageEntries = matched.subList(from, to);
    }

    /** 按注册名取物品展示用堆栈，无效则返回空堆栈。 */
    private static ItemStack itemIcon(String id) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** 条目关联的物品是否全部被过滤。 */
    private boolean isFiltered(Entry entry) {
        return clientItems.containsAll(entry.itemIds());
    }

    /** 点击条目：物品则切换单个；标签/模组则整批切换（部分过滤时视为补全过滤）。 */
    private void toggleEntry(Entry entry) {
        if (isFiltered(entry)) {
            clientItems.removeAll(entry.itemIds());
        } else {
            clientItems.addAll(entry.itemIds());
        }
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        PacketDistributor.sendToServer(new SaveItemFilterPayload(String.join("|", clientItems)));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        graphics.drawCenteredString(this.font, this.title, cx, this.height / 2 - 104, 0xFFFFFF);

        String term = searchBox != null ? searchBox.getValue().trim() : "";
        String hint;
        if (term.isEmpty()) {
            hint = "搜索：名称（默认）/ #标签 / @模组ID；搜索框为空时显示已过滤物品";
        } else if (term.startsWith("#")) {
            hint = "匹配 " + matchCount + " 个标签（点击过滤整个标签）";
        } else if (term.startsWith("@")) {
            hint = "匹配 " + matchCount + " 个模组（点击过滤该模组全部物品）";
        } else {
            hint = "匹配 " + matchCount + " 种物品";
        }
        graphics.drawCenteredString(this.font, Component.literal(hint), cx, this.height / 2 - 66, 0xAAAAAA);

        // 翻页按钮可见性
        boolean hasPages = pageCount() > 1;
        prevButton.visible = hasPages && page > 0;
        nextButton.visible = hasPages && page < pageCount() - 1;

        if (pageEntries.isEmpty()) {
            String emptyText = term.isEmpty() ? "暂未过滤任何物品，输入搜索词开始"
                    : term.startsWith("#") ? "没有匹配的标签"
                    : term.startsWith("@") ? "没有匹配的模组" : "没有匹配的物品";
            graphics.drawCenteredString(this.font, Component.literal(emptyText), cx, this.height / 2, 0x888888);
            return;
        }

        int cols = Math.min(COLS, pageEntries.size());
        int rows = (int) Math.ceil((double) pageEntries.size() / COLS);
        int startX = cx - (cols * CELL) / 2 + (CELL - ITEM) / 2;
        int startY = this.height / 2 - (rows * CELL) / 2;
        int hovered = -1;

        for (int i = 0; i < pageEntries.size(); i++) {
            Entry entry = pageEntries.get(i);
            int col = i % COLS;
            int row = i / COLS;
            int x = startX + col * CELL;
            int y = startY + row * CELL;
            // 底色
            graphics.fill(x - 2, y - 2, x + ITEM + 2, y + ITEM + 2, 0xFF2A2A2A);
            graphics.renderItem(entry.icon(), x, y);
            graphics.renderItemDecorations(this.font, entry.icon(), x, y);
            if (!entry.prefix().isEmpty()) {
                // 标签/模组条目：左上角前缀徽标
                graphics.drawString(this.font, entry.prefix(), x - 2, y - 2, 0xFF55FFFF, false);
            }
            if (isFiltered(entry)) {
                graphics.fill(x - 2, y - 2, x + ITEM + 2, y + ITEM + 2, 0x80FF4040);
                graphics.drawString(this.font, "✗", x + 4, y + 4, 0xFFFF5555, false);
            }
            if (mouseX >= x - 2 && mouseX < x + ITEM + 2 && mouseY >= y - 2 && mouseY < y + ITEM + 2) {
                hovered = i;
            }
        }

        graphics.drawCenteredString(this.font,
                Component.literal("已过滤 " + clientItems.size() + " 种物品（过滤开关：" + (clientEnabled ? "§a开" : "§c关") + "§r）"),
                cx, startY + rows * CELL + 8, 0xFFFFFF);

        // 页码（位于翻页按钮之间）
        if (hasPages) {
            graphics.drawCenteredString(this.font,
                    Component.literal("第 " + (page + 1) + " / " + pageCount() + " 页"),
                    cx, this.height / 2 + 69, 0xFFFFFF);
        }

        // 悬停提示
        if (hovered >= 0 && !isHoveringSearchBox(mouseX, mouseY)) {
            Entry entry = pageEntries.get(hovered);
            List<Component> tooltip = new ArrayList<>();
            if (entry.prefix().isEmpty()) {
                tooltip.add(entry.icon().getHoverName());
                tooltip.add(Component.literal(entry.key()).withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.literal(isFiltered(entry) ? "已过滤：你将无法捡起" : "点击过滤此物品")
                        .withStyle(isFiltered(entry) ? ChatFormatting.RED : ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.literal(entry.key()).withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.literal("共 " + entry.itemIds().size() + " 个物品").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.literal(isFiltered(entry) ? "已过滤：整个" + (entry.prefix().equals("#") ? "标签" : "模组")
                        : "点击过滤" + (entry.prefix().equals("#") ? "整个标签" : "该模组全部物品"))
                        .withStyle(isFiltered(entry) ? ChatFormatting.RED : ChatFormatting.GRAY));
            }
            graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    private boolean isHoveringSearchBox(double mouseX, double mouseY) {
        return searchBox != null && mouseX >= searchBox.getX() && mouseX < searchBox.getX() + searchBox.getWidth()
                && mouseY >= searchBox.getY() && mouseY < searchBox.getY() + searchBox.getHeight();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!pageEntries.isEmpty() && button == 0 && !isHoveringSearchBox(mouseX, mouseY)) {
            int cols = Math.min(COLS, pageEntries.size());
            int rows = (int) Math.ceil((double) pageEntries.size() / COLS);
            int startX = this.width / 2 - (cols * CELL) / 2 + (CELL - ITEM) / 2;
            int startY = this.height / 2 - (rows * CELL) / 2;

            for (int i = 0; i < pageEntries.size(); i++) {
                int col = i % COLS;
                int row = i / COLS;
                int x = startX + col * CELL - 2;
                int y = startY + row * CELL - 2;
                if (mouseX >= x && mouseX < x + ITEM + 4 && mouseY >= y && mouseY < y + ITEM + 4) {
                    toggleEntry(pageEntries.get(i));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
