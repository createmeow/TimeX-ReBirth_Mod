# -*- coding: utf-8 -*-
"""为 underwater_plugin 生成贴图：
1.铜背罐方块/盔甲贴图 -> 铝色（去饱和提亮+微冷灰）
2.原版铁质装备图标 -> 防寒装备图标（微冰蓝色调）
"""
from PIL import Image
import zipfile, os

CREATE = r"D:\MODS\非createmeow编写模组\1.21.1\Create-mc1.21.1-dev-gh源码\src\main\resources\assets\create\textures"
CLIENT_JAR = r"C:\Users\Administrator\.gradle\caches\neoformruntime\artifacts\minecraft_1.21.1_client.jar"
OUT = r"d:\MODS\1.21.1\timexrebirth-template-1.21.1\src\main\resources\assets\underwater_plugin\textures"

def tint(img, tint_rgb, brightness=1.0, keep_shades=True):
    """按亮度重着色：out = gray * tint * brightness，保留 alpha"""
    img = img.convert("RGBA")
    px = img.load()
    tr, tg, tb = tint_rgb
    norm = max(tr, tg, tb) / 255.0
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            gray = (r * 299 + g * 587 + b * 114) // 1000
            nr = min(255, int(gray * tr / 255.0 / norm * brightness))
            ng = min(255, int(gray * tg / 255.0 / norm * brightness))
            nb = min(255, int(gray * tb / 255.0 / norm * brightness))
            px[x, y] = (nr, ng, nb, a)
    return img

os.makedirs(OUT + r"\block", exist_ok=True)
os.makedirs(OUT + r"\models\armor", exist_ok=True)
os.makedirs(OUT + r"\item", exist_ok=True)

# 1. 铝背罐方块贴图（铜 -> 铝）
src = Image.open(CREATE + r"\block\copper_backtank.png")
tint(src, (216, 221, 228), 1.02).save(OUT + r"\block\aluminum_backtank.png")
print("block texture ok", src.size)

# 2. 穿戴贴图（铜潜水系 -> 铝潜水系），层1（胸/头/脚）
src = Image.open(CREATE + r"\models\armor\copper_diving_layer_1.png")
tint(src, (216, 221, 228), 1.02).save(OUT + r"\models\armor\aluminum_diving_layer_1.png")
print("armor layer1 ok", src.size)

# 2b. 第一人称手臂贴图（下界合金 -> 铝），供 AluminumBacktankFirstPersonRenderer 使用
src = Image.open(CREATE + r"\models\armor\netherite_diving_arm.png")
tint(src, (216, 221, 228), 1.02).save(OUT + r"\models\armor\aluminum_diving_arm.png")
print("armor arm ok", src.size)

# 2c. 第三人称手臂：铜套装 layer_1 的手臂 UV 区 (40,16)-(56,32) 为空（0 像素），
#     而下界合金 layer_1 此区域有内容（第三人称双臂即由 HumanoidArmorLayer 外层模型渲染此区）。
#     把下界合金 layer_1 整图铝色化后取手臂区，合成进铝穿戴层1，补齐第三人称手臂渲染。
nl1 = Image.open(CREATE + r"\models\armor\netherite_diving_layer_1.png")
arm_region = tint(nl1, (216, 221, 228), 1.02).crop((40, 16, 56, 32))
layer1 = Image.open(OUT + r"\models\armor\aluminum_diving_layer_1.png").convert("RGBA")
layer1.alpha_composite(arm_region, (40, 16))
layer1.save(OUT + r"\models\armor\aluminum_diving_layer_1.png")
print("armor layer1 arms composited")

# 2d. 穿戴层2（下界合金 -> 铝）：铝背罐物品为 BacktankItem.Layered（与下界合金背罐同构），
#     HumanoidArmorLayerMixin 拦截后由 LayeredArmorItem.renderArmorPiece 渲染双层：
#     内层模型用 layer_2（手臂区 224px，贴合皮肤的内衬手臂），外层模型用 layer_1。
nl2 = Image.open(CREATE + r"\models\armor\netherite_diving_layer_2.png")
tint(nl2, (216, 221, 228), 1.02).save(OUT + r"\models\armor\aluminum_diving_layer_2.png")
print("armor layer2 ok", nl2.size)

# 3. 防寒装备图标（原版铁装备 -> 微冰蓝）
ALUM = (196, 208, 224)  # 冰蓝灰
with zipfile.ZipFile(CLIENT_JAR) as z:
    for vanilla, out in [
        ("iron_helmet.png", "frost_mask.png"),
        ("iron_leggings.png", "frost_leggings.png"),
        ("iron_boots.png", "frost_boots.png"),
    ]:
        img = Image.open(z.open("assets/minecraft/textures/item/" + vanilla))
        tint(img, ALUM, 1.05).save(OUT + r"\item" + "\\" + out)
        print("item", out, "ok", img.size)

# 4. 防寒装备穿戴贴图（原版铁甲 -> 微冰蓝）：层1（头/胸/脚）+ 层2（腿）
with zipfile.ZipFile(CLIENT_JAR) as z:
    for vanilla in ("iron_layer_1.png", "iron_layer_2.png"):
        img = Image.open(z.open("assets/minecraft/textures/models/armor/" + vanilla))
        out = vanilla.replace("iron", "frost")
        tint(img, ALUM, 1.05).save(OUT + r"\models\armor" + "\\" + out)
        print("armor", out, "ok", img.size)
print("ALL DONE")
