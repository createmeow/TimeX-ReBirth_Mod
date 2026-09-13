# -*- coding: utf-8 -*-
"""生成"用完的打火机"16x16 物品贴图（timex_rebirth:item/used_lighter）：
红色塑料机身（磨损划痕）+ 金属防风罩 + 打火轮 + 喷嘴，透明背景。
"""
from PIL import Image

OUT = r"d:\MODS\1.21.1\timexrebirth-template-1.21.1\src\main\resources\assets\timex_rebirth\textures\item\used_lighter.png"

img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
px = img.load()

# 配色
BODY      = (178, 52, 40, 255)    # 红色塑料机身
BODY_DARK = (130, 36, 28, 255)    # 机身暗部/描边
BODY_HI   = (214, 92, 76, 255)    # 机身高光
METAL     = (156, 158, 162, 255)  # 防风罩金属
METAL_HI  = (200, 202, 206, 255)  # 金属高光
METAL_DK  = (104, 106, 110, 255)  # 金属暗部
WHEEL     = (70, 72, 76, 255)     # 打火轮
NOZZLE    = (210, 190, 120, 255)  # 喷嘴（黄铜色）
SCORCH    = (60, 50, 46, 255)     # 熏黑

# ── 机身（y=6..14，x=5..10，塑料壳）──
for y in range(6, 15):
    for x in range(5, 11):
        px[x, y] = BODY
# 左缘高光、右缘暗部（简单立体感）
for y in range(6, 15):
    px[5, y] = BODY_HI
    px[10, y] = BODY_DARK
# 底部描边
for x in range(5, 11):
    px[x, 14] = BODY_DARK
# 磨损划痕（旧物感）
px[7, 8]  = BODY_DARK
px[9, 11] = BODY_HI
px[6, 12] = BODY_DARK

# ── 金属防风罩（y=1..5，x=4..11，略宽于机身）──
for y in range(1, 6):
    for x in range(4, 12):
        px[x, y] = METAL
# 顶部开口（点火孔）
px[6, 1] = (0, 0, 0, 0)
px[7, 1] = (0, 0, 0, 0)
px[8, 1] = (0, 0, 0, 0)
# 竖缝（防风孔）
px[5, 3] = METAL_DK
px[5, 4] = METAL_DK
px[10, 3] = METAL_DK
px[10, 4] = METAL_DK
# 金属高光/暗部
px[4, 2] = METAL_HI
px[4, 3] = METAL_HI
px[11, 4] = METAL_DK
px[11, 5] = METAL_DK

# ── 打火轮（y=2，x=8..9，深灰圆轮带纹路）──
px[8, 2] = WHEEL
px[9, 2] = WHEEL
px[9, 3] = WHEEL

# ── 喷嘴（y=2，x=6..7，黄铜小管）──
px[6, 2] = NOZZLE
px[7, 2] = NOZZLE

# ── 熏黑（旧打火机罩顶烟渍）──
px[6, 5] = SCORCH
px[9, 5] = SCORCH

img.save(OUT)
print("used_lighter texture ok", img.size)
