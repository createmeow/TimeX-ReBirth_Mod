package io.github.createmeow.timex_rebirth.research;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 科技树定义与查询（代码驱动）。
 * 分类分支：
 *  - survival 生存：铁砧 / 酿造台 / 附魔台 使用权限
 *  - equipment 装备：工具制造 → 制剑 → 盔甲打制 / 枪械组装
 *  - farming  农业：作物栽培 / 烘焙 / 造纸（黑麦/芜菁种植、面团烤制、秸秆/甘蔗/树皮造纸）
 *  - heat     供热工业：热源发生器 → 热源部件 → 模块制造（小研究已合并）
 *  - basecore 基地核心：基地知识（模块配方）→ 基地防御（防御炮/加密容器）
 *  - create   机械动力：物流 → 安山机械 → 黄铜机械 → 列车运输
 * 节点解锁内容：
 *  - 配方：未解锁时工作台无法合成（CraftingMenuMixin 拦截并清空结果）
 *  - 使用权限：未解锁时禁止放置/使用（附魔台/酿造台/铁砧/热源组件/基地建筑/机械动力方块），事件层拦截
 * 时间：单独研究 100%、帮助 75%、协助 50%（基础时长见各节点）。
 */
public final class TechTree {
    private static final Map<String, TechNode> NODES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, String> RECIPE_GATES = new HashMap<>();
    private static final Map<String, String> PERMISSION_GATES = new HashMap<>();
    /** 物品/方块放置使用权限门控：物品 id -> 需要的权限键 */
    private static final Map<ResourceLocation, String> ITEM_USE_GATES = new HashMap<>();

    private TechTree() {
    }

    private static void add(TechNode node) {
        NODES.put(node.id(), node);
        for (String recipe : node.unlockRecipes()) {
            RECIPE_GATES.put(ResourceLocation.tryParse(recipe), node.id());
        }
        for (String perm : node.usePermissions()) {
            PERMISSION_GATES.put(perm, node.id());
        }
    }

    private static void gate(ResourceLocation itemId, String permKey) {
        ITEM_USE_GATES.put(itemId, permKey);
    }

    static {
        // ── 生存：工具与魔法使用权限 ──
        // 点数已按节点难度整体上调 10%~70%（难度越高增幅越大）：
        // 入门节点 +10~25%，进阶节点 +25~33%，高级节点 +33~47%，顶级节点 +50~68%
        add(new TechNode("tools_maintenance", "survival",
                9, 300, List.of(),
                List.of(), List.of("use.anvil")));
        add(new TechNode("alchemy_basics", "survival",
                12, 600, List.of("tools_maintenance"),
                List.of(), List.of("use.brewing_stand")));
        add(new TechNode("enchanting_knowledge", "survival",
                15, 900, List.of("tools_maintenance"),
                List.of(), List.of("use.enchanting_table")));

        // ── 装备：工具制造 → 制剑 → 盔甲打制 / 枪械组装（递进解锁）──
        // 工具制造：门控斧/镐/铲/锄（原版各材质）与农夫乐事刀具的合成配方
        add(new TechNode("tool_making", "equipment",
                10, 300, List.of("tools_maintenance"),
                List.of("minecraft:wooden_pickaxe", "minecraft:stone_pickaxe", "minecraft:iron_pickaxe",
                        "minecraft:golden_pickaxe", "minecraft:diamond_pickaxe",
                        "minecraft:wooden_axe", "minecraft:stone_axe", "minecraft:iron_axe",
                        "minecraft:golden_axe", "minecraft:diamond_axe",
                        "minecraft:wooden_shovel", "minecraft:stone_shovel", "minecraft:iron_shovel",
                        "minecraft:golden_shovel", "minecraft:diamond_shovel",
                        "minecraft:wooden_hoe", "minecraft:stone_hoe", "minecraft:iron_hoe",
                        "minecraft:golden_hoe", "minecraft:diamond_hoe",
                        "farmersdelight:flint_knife", "farmersdelight:iron_knife",
                        "farmersdelight:diamond_knife", "farmersdelight:golden_knife"),
                List.of()));
        // 制剑：门控剑/弓/弩/盾牌（冷兵器：近战与远程武器）的合成配方
        add(new TechNode("sword_crafting", "equipment",
                10, 300, List.of("tool_making"),
                List.of("minecraft:wooden_sword", "minecraft:stone_sword", "minecraft:iron_sword",
                        "minecraft:golden_sword", "minecraft:diamond_sword",
                        "minecraft:bow", "minecraft:crossbow", "minecraft:shield"),
                List.of()));
        // 盔甲打制：门控皮革/铁/金/钻石护甲四件套的合成配方
        add(new TechNode("armor_forging", "equipment",
                13, 600, List.of("sword_crafting"),
                List.of("minecraft:leather_helmet", "minecraft:leather_chestplate",
                        "minecraft:leather_leggings", "minecraft:leather_boots",
                        "minecraft:iron_helmet", "minecraft:iron_chestplate",
                        "minecraft:iron_leggings", "minecraft:iron_boots",
                        "minecraft:golden_helmet", "minecraft:golden_chestplate",
                        "minecraft:golden_leggings", "minecraft:golden_boots",
                        "minecraft:diamond_helmet", "minecraft:diamond_chestplate",
                        "minecraft:diamond_leggings", "minecraft:diamond_boots"),
                List.of()));
        // 枪械组装：解锁枪械工作台的使用权限（组装枪械与弹药，配方走工作台内部而非合成台）
        add(new TechNode("gun_assembly", "equipment",
                30, 900, List.of("sword_crafting"),
                List.of(), List.of("use.gun_workbench")));

        // ── 农业：作物栽培（种子）→ 烘焙（面团/烤制）→ 造纸（秸秆/甘蔗/树皮）──
        // 作物栽培：门控黑麦/芜菁（本模组）与农夫乐事作物（卷心菜/番茄/洋葱/稻米）的种植
        add(new TechNode("crop_cultivation", "farming",
                9, 300, List.of(),
                List.of(), List.of("use.crop_seeds")));
        // 烘焙：门控"面粉+水桶→面团"等面团配方（本模组黑麦面团 + 农夫乐事面团 + 机械动力面团），面团经熔炉/烟熏炉/营火烤制成面包
        add(new TechNode("baking", "farming",
                12, 600, List.of("crop_cultivation"),
                List.of("timex_rebirth:rye_dough_crafting",
                        "farmersdelight:wheat_dough_from_water",
                        "farmersdelight:wheat_dough_from_egg",
                        "create:dough"), List.of()));
        // 造纸技术：统一门控秸秆（本模组）/ 甘蔗（原版纸配方）/ 树皮（FarmersDelight）的造纸配方
        add(new TechNode("papermaking", "farming",
                9, 300, List.of(),
                List.of("timex_rebirth:rye_straw_to_paper",
                        "minecraft:paper", "farmersdelight:paper_from_tree_bark"),
                List.of()));
        // 废土食物加工（配方经农夫乐事炖锅/烟熏炉制作，门控"制造"而非"食用"——食用是生物本能）：
        // 炖汤：农夫乐事锅（炖菜/浓汤）；加工：农夫乐事砧板（罐头/肉干/方便面）；
        // 摆盘：农夫乐事工作台菜品（三明治/汉堡/沙拉等盛装配菜）
        add(new TechNode("soup_cooking", "farming",
                10, 400, List.of("baking"),
                List.of("timex_rebirth:wasteland_stew",
                        "timex_rebirth:wasteland_broth"),
                List.of("use.fd_cooking_pot")));
        add(new TechNode("food_processing", "farming",
                13, 500, List.of("soup_cooking"),
                List.of("timex_rebirth:canned_food",
                        "timex_rebirth:dried_meat_from_smoking",
                        "timex_rebirth:instant_noodles"),
                List.of("use.fd_cutting_board")));
        add(new TechNode("food_plating", "farming",
                20, 400, List.of("food_processing"),
                List.of("farmersdelight:mixed_salad",
                        "farmersdelight:nether_salad",
                        "farmersdelight:barbecue_stick",
                        "farmersdelight:egg_sandwich",
                        "farmersdelight:chicken_sandwich",
                        "farmersdelight:hamburger",
                        "farmersdelight:bacon_sandwich",
                        "farmersdelight:mutton_wrap",
                        "farmersdelight:stuffed_potato",
                        "farmersdelight:salmon_roll",
                        "farmersdelight:cod_roll",
                        "farmersdelight:kelp_roll",
                        "farmersdelight:grilled_salmon",
                        "farmersdelight:steak_and_potatoes",
                        "farmersdelight:roasted_mutton_chops",
                        "farmersdelight:bacon_and_eggs",
                        "farmersdelight:roast_chicken_block",
                        "farmersdelight:shepherds_pie_block",
                        "farmersdelight:honey_glazed_ham_block",
                        "farmersdelight:gleaming_salad_block",
                        "farmersdelight:rice_roll_medley_block"),
                List.of()));

        // ── 供热工业：底座 → 发生器 → 部件 → 模块（进度递进）──
        // 加热底座：先解锁底座（发生器必须放置在底座上方，故底座先研究）
        add(new TechNode("heat_base", "heat",
                25, 600, List.of("tools_maintenance"),
                List.of("timex_rebirth:heat_base"), List.of("use.heat_base")));
        add(new TechNode("heat_core", "heat",
                35, 900, List.of("heat_base"),
                List.of("timex_rebirth:heat_station", "timex_rebirth:heat_bridge_module"),
                List.of("use.heat_station")));
        add(new TechNode("heat_parts", "heat",
                28, 600, List.of("heat_core"),
                List.of("timex_rebirth:heat_fuel_receiver",
                        "timex_rebirth:heat_adapter", "timex_rebirth:heat_module_slot",
                        "timex_rebirth:heat_receiver"),
                List.of()));
        add(new TechNode("heat_modules", "heat",
                28, 600, List.of("heat_parts"),
                List.of("timex_rebirth:energy_save_module", "timex_rebirth:production_module"),
                List.of()));

        // ── 基地核心：核心知识（基地交互）→ 模块制造（零件购买）→ 基地防御 ──
        // 基地核心知识：未研究时无法与基地核心方块交互（右键打开基地核心界面）；
        // 模块配方在 component 模式下走零件购买不走合成，故模块制造内容由"模块制造"节点门控
        add(new TechNode("basecore_core", "basecore",
                32, 900, List.of("tools_maintenance"),
                List.of(), List.of("use.basecore_interact")));
        // 模块制造：component 模式下模块用零件购买而非合成，故单独门控基地核心/防御炮升级界面的购买
        add(new TechNode("module_crafting", "basecore",
                28, 600, List.of("basecore_core"),
                List.of(), List.of("use.basecore_modules")));
        add(new TechNode("basecore_defense", "basecore",
                30, 600, List.of("module_crafting"),
                List.of(), List.of("use.basecore_defense")));
        // 基地攻击：电磁脉冲炸弹 / 三种伪装装置 / 干扰装置（对敌方基地使用），防御研究之后解锁
        add(new TechNode("basecore_attack", "basecore",
                38, 900, List.of("basecore_defense"),
                List.of(), List.of("use.basecore_attack")));

        // ── 机械动力：物流 → 安山机械 → 黄铜机械 → 列车运输 ──
        add(new TechNode("create_logistics", "create",
                32, 1200, List.of("tools_maintenance"),
                List.of(), List.of("use.create_logistics")));
        add(new TechNode("create_andesite", "create",
                35, 1200, List.of("create_logistics"),
                List.of(), List.of("use.create_andesite")));
        add(new TechNode("create_brass", "create",
                50, 1500, List.of("create_andesite"),
                List.of(), List.of("use.create_brass")));
        add(new TechNode("create_trains", "create",
                70, 1800, List.of("create_brass"),
                List.of(), List.of("use.create_trains")));

        // ── 物品/方块放置门控 ──
        // 加热底座：heat_base 解锁（发生器必须先放在底座上，故底座单独门控）
        gate(ResourceLocation.parse("timex_rebirth:heat_base"), "use.heat_base");
        // 热源供应站其余组件（含桥接模块），heat_core 解锁
        gate(ResourceLocation.parse("timex_rebirth:heat_station"), "use.heat_station");
        gate(ResourceLocation.parse("timex_rebirth:heat_fuel_receiver"), "use.heat_station");
        gate(ResourceLocation.parse("timex_rebirth:heat_adapter"), "use.heat_station");
        gate(ResourceLocation.parse("timex_rebirth:heat_module_slot"), "use.heat_station");
        gate(ResourceLocation.parse("timex_rebirth:heat_receiver"), "use.heat_station");
        gate(ResourceLocation.parse("timex_rebirth:heat_bridge_module"), "use.heat_station");
        gate(ResourceLocation.parse("timex_rebirth:energy_save_module"), "use.heat_station");
        gate(ResourceLocation.parse("timex_rebirth:production_module"), "use.heat_station");
        // 作物种子：种植冬季作物需要"作物栽培"研究（本模组黑麦/芜菁 + 农夫乐事卷心菜/番茄/洋葱/稻米）
        gate(ResourceLocation.parse("timex_rebirth:rye_seeds"), "use.crop_seeds");
        gate(ResourceLocation.parse("timex_rebirth:turnip_seeds"), "use.crop_seeds");
        gate(ResourceLocation.parse("farmersdelight:cabbage_seeds"), "use.crop_seeds");
        gate(ResourceLocation.parse("farmersdelight:tomato_seeds"), "use.crop_seeds");
        gate(ResourceLocation.parse("farmersdelight:onion"), "use.crop_seeds");
        gate(ResourceLocation.parse("farmersdelight:rice"), "use.crop_seeds");
        // 农夫乐事：锅（炖汤研究解锁使用）、砧板（加工研究解锁使用）
        gate(ResourceLocation.parse("farmersdelight:cooking_pot"), "use.fd_cooking_pot");
        gate(ResourceLocation.parse("farmersdelight:cutting_board"), "use.fd_cutting_board");
        // 枪械工作台（MrCrayfish Gun Mod）：装备制造解锁使用（放置/右键打开组装枪械与弹药）
        gate(ResourceLocation.parse("cgm:workbench"), "use.gun_workbench");
        // 基地核心：与基地核心方块交互（打开基地核心界面）需要"基地核心知识"研究
        gate(ResourceLocation.parse("basecore:basecore"), "use.basecore_interact");
        // 基地防御：防御炮 + 加密容器，basecore_defense 解锁
        gate(ResourceLocation.parse("basecore:defend"), "use.basecore_defense");
        gate(ResourceLocation.parse("basecore:hash_chest"), "use.basecore_defense");
        // 基地攻击：电磁脉冲炸弹 / 三种伪装装置 / 干扰装置（保护信号屏蔽），basecore_attack 解锁
        gate(ResourceLocation.parse("basecore:electromagnetic_pulse_bomb"), "use.basecore_attack");
        gate(ResourceLocation.parse("basecore:disguise"), "use.basecore_attack");
        gate(ResourceLocation.parse("basecore:advanced_disguise_device"), "use.basecore_attack");
        gate(ResourceLocation.parse("basecore:extreme_disguise_device"), "use.basecore_attack");
        gate(ResourceLocation.parse("basecore:protection_signal_shield"), "use.basecore_attack");
        // 基地模块：基地核心/防御炮零件升级界面购买模块需要"模块制造"研究
        gate(ResourceLocation.parse("basecore:range_module"), "use.basecore_modules");
        gate(ResourceLocation.parse("basecore:def_module"), "use.basecore_modules");
        gate(ResourceLocation.parse("basecore:secure_module"), "use.basecore_modules");
        gate(ResourceLocation.parse("basecore:auto_repair_module"), "use.basecore_modules");
        gate(ResourceLocation.parse("basecore:thorns_module"), "use.basecore_modules");
        gate(ResourceLocation.parse("basecore:exp_def_module"), "use.basecore_modules");
        gate(ResourceLocation.parse("basecore:strength_module"), "use.basecore_modules");
        gate(ResourceLocation.parse("basecore:jump_boost_module"), "use.basecore_modules");
        gate(ResourceLocation.parse("basecore:regeneration_module"), "use.basecore_modules");
        gate(ResourceLocation.parse("basecore:resistance_module"), "use.basecore_modules");
        gate(ResourceLocation.parse("basecore:dig_speed_module"), "use.basecore_modules");
        gate(ResourceLocation.parse("basecore:movement_speed_module"), "use.basecore_modules");
        gate(ResourceLocation.parse("basecore:dig_slowdown_module"), "use.basecore_modules");
        gate(ResourceLocation.parse("basecore:weakness_module"), "use.basecore_modules");
        gate(ResourceLocation.parse("basecore:movement_slowdown_module"), "use.basecore_modules");
        gate(ResourceLocation.parse("basecore:basecore_counter_reconnaissance_module"), "use.basecore_modules");
        // 机械动力 - 物流：传送带/漏斗/隧道/溜槽/便携接口
        gate(ResourceLocation.parse("create:belt"), "use.create_logistics");
        gate(ResourceLocation.parse("create:chute"), "use.create_logistics");
        gate(ResourceLocation.parse("create:smart_chute"), "use.create_logistics");
        gate(ResourceLocation.parse("create:andesite_funnel"), "use.create_logistics");
        gate(ResourceLocation.parse("create:brass_funnel"), "use.create_logistics");
        gate(ResourceLocation.parse("create:andesite_tunnel"), "use.create_logistics");
        gate(ResourceLocation.parse("create:brass_tunnel"), "use.create_logistics");
        gate(ResourceLocation.parse("create:portable_storage_interface"), "use.create_logistics");
        // 机械动力 - 安山机械：动力源与基础加工机（按合成配方材料判定，机身为安山岩机壳）
        gate(ResourceLocation.parse("create:water_wheel"), "use.create_andesite");
        gate(ResourceLocation.parse("create:encased_fan"), "use.create_andesite");
        gate(ResourceLocation.parse("create:millstone"), "use.create_andesite");
        gate(ResourceLocation.parse("create:crushing_wheel"), "use.create_andesite");
        gate(ResourceLocation.parse("create:mechanical_press"), "use.create_andesite");
        gate(ResourceLocation.parse("create:mechanical_piston"), "use.create_andesite");
        gate(ResourceLocation.parse("create:mechanical_bearing"), "use.create_andesite");
        gate(ResourceLocation.parse("create:mechanical_drill"), "use.create_andesite");
        gate(ResourceLocation.parse("create:mechanical_saw"), "use.create_andesite");
        // 部署器/搅拌机：配方不含黄铜锭/机壳/板（机身均为安山机壳），属安山机械
        gate(ResourceLocation.parse("create:deployer"), "use.create_andesite");
        gate(ResourceLocation.parse("create:mechanical_mixer"), "use.create_andesite");
        // 机械动力 - 黄铜机械（合成使用黄铜机壳/黄铜板）
        gate(ResourceLocation.parse("create:mechanical_crafter"), "use.create_brass");
        gate(ResourceLocation.parse("create:sequenced_gearshift"), "use.create_brass");
        gate(ResourceLocation.parse("create:mechanical_arm"), "use.create_brass");
        gate(ResourceLocation.parse("create:clockwork_bearing"), "use.create_brass");
        // 机械动力 - 列车运输
        gate(ResourceLocation.parse("create:track"), "use.create_trains");
        gate(ResourceLocation.parse("create:track_station"), "use.create_trains");
        gate(ResourceLocation.parse("create:track_signal"), "use.create_trains");
        gate(ResourceLocation.parse("create:controls"), "use.create_trains");
        gate(ResourceLocation.parse("create:train_door"), "use.create_trains");
    }

    /** 全部节点（按定义顺序）。 */
    public static List<TechNode> getNodes() {
        return new ArrayList<>(NODES.values());
    }

    /** 按分类分组（保持定义顺序）。 */
    public static Map<String, List<TechNode>> getNodesByCategory() {
        Map<String, List<TechNode>> map = new LinkedHashMap<>();
        for (TechNode node : NODES.values()) {
            map.computeIfAbsent(node.category(), k -> new ArrayList<>()).add(node);
        }
        return map;
    }

    public static List<String> getCategories() {
        return new ArrayList<>(getNodesByCategory().keySet());
    }

    public static TechNode getNode(String id) {
        return NODES.get(id);
    }

    /** 前置节点是否全部已解锁。 */
    public static boolean hasDependencies(Player player, TechNode node) {
        for (String dep : node.dependencies()) {
            if (!ResearchData.isUnlocked(player, dep)) return false;
        }
        return true;
    }

    /** 该配方被哪个节点门控，无门控返回 null。 */
    public static TechNode getRecipeGate(ResourceLocation recipeId) {
        String nodeId = RECIPE_GATES.get(recipeId);
        return nodeId == null ? null : NODES.get(nodeId);
    }

    /** 配方是否对玩家锁定（被门控且未解锁对应节点）。 */
    public static boolean isRecipeLockedFor(Player player, ResourceLocation recipeId) {
        TechNode gate = getRecipeGate(recipeId);
        return gate != null && !ResearchData.isUnlocked(player, gate.id());
    }

    /** 权限键对应的门控节点，无门控返回 null。 */
    public static TechNode getPermissionGate(String permKey) {
        String nodeId = PERMISSION_GATES.get(permKey);
        return nodeId == null ? null : NODES.get(nodeId);
    }

    /** 玩家是否拥有指定使用权限（未门控的权限视为拥有）。 */
    public static boolean hasUsePermission(Player player, String permKey) {
        TechNode gate = getPermissionGate(permKey);
        return gate == null || ResearchData.isUnlocked(player, gate.id());
    }

    /** 物品被哪个使用权限门控，未门控返回 null。 */
    public static String getItemUsePermission(ResourceLocation itemId) {
        return ITEM_USE_GATES.get(itemId);
    }
}
