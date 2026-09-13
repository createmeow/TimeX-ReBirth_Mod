# -*- coding: utf-8 -*-
"""为 underwater_plugin 生成全部 JSON 资产：
1. 铝背罐：blockstate / 方块模型(复制Create铜背罐换贴图) / 物品模型 / 战利品表 / 标签
2. 防寒装备：物品模型 / Cold Sweat insulator 数据(每件3抗寒)
3. 配方：锻造(铝背罐) + 合成(面罩/护腿/靴子) + 对应解锁进度
4. 语言文件 en_us / zh_cn
"""
import json
import os

ROOT = r"d:\MODS\1.21.1\timexrebirth-template-1.21.1\src\main\resources"
CREATE = r"D:\非createmeow编写模组\Create-mc1.21.1-dev-gh源码\src\main\resources"
CREATE_ALT = r"D:\MODS\非createmeow编写模组\1.21.1\Create-mc1.21.1-dev-gh源码\src\main\resources"
CREATE_DIR = CREATE if os.path.isdir(CREATE) else CREATE_ALT

ASSETS = os.path.join(ROOT, "assets", "underwater_plugin")
DATA = os.path.join(ROOT, "data")

A = "timex_rebirth:aluminum_ingot"          # 铝锭
B = "timex_rebirth:cold_resistant_casing"   # 耐寒机壳
G = "timex_rebirth:insulated_glass"         # 隔热玻璃
AL_BLOCK = "timex_rebirth:aluminum_block"   # 铝块
CU_TANK = "create:copper_backtank"          # 铜背罐
TANK = "underwater_plugin:aluminum_backtank"


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("write", os.path.relpath(path, ROOT))


def read_json(path):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def copy_model_with_texture(src_path, dst_path, texture):
    obj = read_json(src_path)
    obj["textures"] = {"0": texture, "particle": texture}
    write_json(dst_path, obj)


# ======================================================================
# 1. 铝背罐 blockstate + 方块/物品模型
# ======================================================================
variants = {}
for facing, rot in [("north", 0), ("east", 90), ("south", 180), ("west", 270)]:
    for wl in ("false", "true"):
        v = {"model": "underwater_plugin:block/aluminum_backtank/block"}
        if rot:
            v["y"] = rot
        variants[f"facing={facing},waterlogged={wl}"] = v
write_json(os.path.join(ASSETS, "blockstates", "aluminum_backtank.json"), {"variants": variants})

bk_dir = os.path.join(CREATE_DIR, "assets", "create", "models", "block", "copper_backtank")
tex = "underwater_plugin:block/aluminum_backtank"
out_blk = os.path.join(ASSETS, "models", "block", "aluminum_backtank")
copy_model_with_texture(os.path.join(bk_dir, "block.json"), os.path.join(out_blk, "block.json"), tex)
copy_model_with_texture(os.path.join(bk_dir, "item.json"), os.path.join(out_blk, "item.json"), tex)

write_json(os.path.join(ASSETS, "models", "item", "aluminum_backtank.json"),
           {"parent": "underwater_plugin:block/aluminum_backtank/item"})
write_json(os.path.join(ASSETS, "models", "item", "aluminum_backtank_placeable.json"),
           {"parent": "minecraft:item/barrier"})

# 防寒装备物品模型
for name in ("frost_mask", "frost_leggings", "frost_boots"):
    write_json(os.path.join(ASSETS, "models", "item", f"{name}.json"),
               {"parent": "minecraft:item/generated",
                "textures": {"layer0": f"underwater_plugin:item/{name}"}})

# ======================================================================
# 2. 战利品表 + 标签
# ======================================================================
write_json(os.path.join(DATA, "underwater_plugin", "loot_table", "blocks", "aluminum_backtank.json"), {
    "type": "minecraft:block",
    "pools": [{
        "bonus_rolls": 0.0,
        "conditions": [{"condition": "minecraft:survives_explosion"}],
        "entries": [{
            "type": "minecraft:item",
            "functions": [{
                "function": "minecraft:copy_components",
                "include": ["create:banktank_air"],
                "source": "block_entity"
            }],
            "name": TANK
        }],
        "rolls": 1.0
    }],
    "random_sequence": "underwater_plugin:blocks/aluminum_backtank"
})

# minecraft 标签（追加）
# 注意：data/minecraft 下若已有同名文件（如项目此前手写合并的 pickaxe/needs_stone_tool），
# 直接重跑本段会覆盖丢失原有条目！重跑前先核对 git diff。
write_json(os.path.join(DATA, "minecraft", "tags", "block", "mineable", "pickaxe.json"),
           {"replace": False, "values": [TANK]})
write_json(os.path.join(DATA, "minecraft", "tags", "item", "chest_armor.json"),
           {"replace": False, "values": [TANK]})
# create 标签：BacktankUtil.getAllWithAir 只认此标签（潜水头盔供气/补气必需）
write_json(os.path.join(DATA, "create", "tags", "item", "pressurized_air_sources.json"),
           {"replace": False, "values": [TANK]})

# ======================================================================
# 3. 配方 + 解锁进度
# ======================================================================
recipes = {}

# 锻造：耐寒机壳(模板) + 铜背罐(底) + 铝块(添加) -> 铝背罐
recipes["aluminum_backtank"] = {
    "type": "minecraft:smithing_transform",
    "template": {"item": B},
    "base": {"item": CU_TANK},
    "addition": {"item": AL_BLOCK},
    "result": {"id": TANK, "count": 1}
}

# 合成配方（A=铝锭 B=耐寒机壳 G=隔热玻璃）
recipes["frost_mask"] = {
    "type": "minecraft:crafting_shaped",
    "category": "equipment",
    "pattern": ["AAA", "BGB"],
    "key": {"A": {"item": A}, "B": {"item": B}, "G": {"item": G}},
    "result": {"id": "underwater_plugin:frost_mask", "count": 1}
}
recipes["frost_leggings"] = {
    "type": "minecraft:crafting_shaped",
    "category": "equipment",
    "pattern": ["BAB", "A A", "A A"],
    "key": {"A": {"item": A}, "B": {"item": B}},
    "result": {"id": "underwater_plugin:frost_leggings", "count": 1}
}
recipes["frost_boots"] = {
    "type": "minecraft:crafting_shaped",
    "category": "equipment",
    "pattern": ["B B", "A A"],
    "key": {"A": {"item": A}, "B": {"item": B}},
    "result": {"id": "underwater_plugin:frost_boots", "count": 1}
}

# 进度解锁判据（首个特殊原料）
criteria_item = {
    "aluminum_backtank": CU_TANK,
    "frost_mask": B,
    "frost_leggings": B,
    "frost_boots": B,
}
for name, recipe in recipes.items():
    write_json(os.path.join(DATA, "underwater_plugin", "recipe", f"{name}.json"), recipe)
    crit_item = criteria_item[name]
    crit_key = "has_" + crit_item.split(":")[1]
    write_json(os.path.join(DATA, "underwater_plugin", "advancement", "recipe", f"{name}.json"), {
        "parent": "minecraft:recipes/root",
        "rewards": {"recipes": [f"underwater_plugin:{name}"]},
        "criteria": {
            crit_key: {
                "trigger": "minecraft:inventory_changed",
                "conditions": {"items": [{"items": [crit_item]}]}
            },
            "has_the_recipe": {
                "trigger": "minecraft:recipe_unlocked",
                "conditions": {"recipe": f"underwater_plugin:{name}"}
            }
        },
        "requirements": [[crit_key, "has_the_recipe"]]
    })

# ======================================================================
# 4. Cold Sweat insulator 数据（每件 3 抗寒）
# ======================================================================
for name in ("frost_mask", "frost_leggings", "frost_boots"):
    write_json(os.path.join(DATA, "coldsweat", "cold_sweat", "item", "insulator",
                            "underwater_plugin", f"{name}.json"), {
        "required_mods": ["underwater_plugin"],
        "type": "armor",
        "item": {"items": [f"underwater_plugin:{name}"]},
        "insulation": {"cold": 3, "heat": 0}
    })

# ======================================================================
# 5. 语言文件
# ======================================================================
en = {
    "block.underwater_plugin.aluminum_backtank": "Aluminum Backtank",
    "item.underwater_plugin.aluminum_backtank": "Aluminum Backtank",
    "item.underwater_plugin.aluminum_backtank.desc": "Forged from a copper backtank. Consumes compressed air to breathe underwater; maintains body temperature when worn with the full frost gear set.",
    "item.underwater_plugin.frost_mask": "Frost-Resistant Mask",
    "item.underwater_plugin.frost_leggings": "Frost-Resistant Leggings",
    "item.underwater_plugin.frost_boots": "Frost-Resistant Boots"
}
zh = {
    "block.underwater_plugin.aluminum_backtank": "铝背罐",
    "item.underwater_plugin.aluminum_backtank": "铝背罐",
    "item.underwater_plugin.aluminum_backtank.desc": "由铜背罐锻造而成。水下时消耗压缩空气供呼吸；配合全套防寒装备可维持体温。",
    "item.underwater_plugin.frost_mask": "防寒面罩",
    "item.underwater_plugin.frost_leggings": "防寒护腿",
    "item.underwater_plugin.frost_boots": "防寒靴子"
}
write_json(os.path.join(ASSETS, "lang", "en_us.json"), en)
write_json(os.path.join(ASSETS, "lang", "zh_cn.json"), zh)

print("ALL ASSETS DONE")
