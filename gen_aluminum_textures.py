"""
Generate all aluminum-related textures for TimeX Rebirth mod.
Textures:
  Item: raw_aluminum, aluminum_sheet, incomplete_defend, incomplete_hash_chest
  Block: raw_aluminum_block, aluminum_ore, deepslate_aluminum_ore,
         cold_resistant_casing, cold_resistant_casing_connected (128x128)
"""
from PIL import Image, ImageDraw
import os
import random

# ── Deterministic randomness ──────────────────────────────────────────
random.seed(42)

# ── Paths ─────────────────────────────────────────────────────────────
TEX_DIR = r"d:\MODS\1.21.1\timexrebirth-template-1.21.1\src\main\resources\assets\timex_rebirth\textures"
ITEM_DIR = os.path.join(TEX_DIR, "item")
BLOCK_DIR = os.path.join(TEX_DIR, "block")

# ── Color palette ────────────────────────────────────────────────────
# Raw aluminum
RAW_AL_MAIN   = (0xB8, 0xC4, 0xD0)   # light gray-blue
RAW_AL_DARK   = (0x6B, 0x78, 0x84)   # darker gray-blue
RAW_AL_LIGHT  = (0xD8, 0xE0, 0xE8)   # highlight

# Aluminum sheet
AL_SHEET_MAIN  = (0xC8, 0xD0, 0xD8)  # silver-white
AL_SHEET_DARK  = (0x98, 0xA0, 0xA8)  # edge shadow
AL_SHEET_LIGHT = (0xE0, 0xE8, 0xF0)  # reflection

# Incomplete items
INC_GRAY  = (0x88, 0x88, 0x88)
INC_DARK  = (0x55, 0x55, 0x55)
INC_LIGHT = (0xAA, 0xAA, 0xAA)
RED_WIRE  = (0xAA, 0x33, 0x33)
RED_DARK  = (0x88, 0x22, 0x22)
LOCK_COL  = (0x66, 0x66, 0x66)

# Stone / ore backgrounds
STONE_BG     = (0x7E, 0x7E, 0x7E)
STONE_DARK    = (0x6E, 0x6E, 0x6E)
DEEPSLATE_BG  = (0x4A, 0x4A, 0x4A)
DEEPSLATE_DARK = (0x3A, 0x3A, 0x3A)

# Cold resistant casing
CASING_MAIN   = (0x3A, 0x4A, 0x5C)  # dark blue-gray
CASING_LIGHT  = (0x5A, 0x6A, 0x7C)  # lighter, rivets/borders
CASING_DARK   = (0x2A, 0x3A, 0x4C)  # darker, shadows
CASING_BORDER = (0x5A, 0x6A, 0x7C)  # border line color


# ── Helpers ──────────────────────────────────────────────────────────
def jitter(color, n=8):
    """Return a color with random brightness variation."""
    r, g, b = color[:3]
    d = random.randint(-n, n)
    return (max(0, min(255, r + d)),
            max(0, min(255, g + d)),
            max(0, min(255, b + d)))


def fill_noise(img, color, n=8, size=16, ox=0, oy=0):
    """Fill a region with noisy base color."""
    for x in range(size):
        for y in range(size):
            img.putpixel((ox + x, oy + y), jitter(color, n))


# ── 1. raw_aluminum.png (item, 16x16) ────────────────────────────────
def gen_raw_aluminum():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    fill_noise(img, RAW_AL_MAIN, 8)
    # dark spots
    spots = [(2,3),(3,2),(4,4),(7,2),(8,3),(10,5),(11,2),(13,4),
             (2,8),(4,9),(6,7),(9,9),(11,8),(13,10),
             (3,12),(5,13),(8,11),(10,12),(12,13),(14,11),
             (1,5),(6,3),(9,7),(12,6),(5,11),(14,3)]
    for sx, sy in spots:
        if 0 <= sx < 16 and 0 <= sy < 16:
            img.putpixel((sx, sy), jitter(RAW_AL_DARK, 5))
    # light highlights
    for hx, hy in [(1,1),(5,3),(9,5),(12,1),(6,9),(10,10),(3,7),(14,5),(7,12),(4,6)]:
        if 0 <= hx < 16 and 0 <= hy < 16:
            img.putpixel((hx, hy), RAW_AL_LIGHT)
    img.save(os.path.join(ITEM_DIR, "raw_aluminum.png"))
    print("  [OK] item/raw_aluminum.png")


# ── 2. aluminum_sheet.png (item, 16x16) ──────────────────────────────
def gen_aluminum_sheet():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            if x == 0 or x == 15 or y == 0 or y == 15:
                img.putpixel((x, y), AL_SHEET_DARK)
            elif x == 1 or x == 14 or y == 1 or y == 14:
                img.putpixel((x, y), AL_SHEET_MAIN)
            else:
                img.putpixel((x, y), jitter(AL_SHEET_MAIN, 5))
    # vertical brushed-metal lines
    for i in range(3, 13, 3):
        for y in range(3, 13):
            img.putpixel((i, y), AL_SHEET_DARK)
    # top reflection band
    for x in range(4, 12):
        img.putpixel((x, 2), AL_SHEET_LIGHT)
        img.putpixel((x, 3), (0xD8, 0xE0, 0xE8))
    img.save(os.path.join(ITEM_DIR, "aluminum_sheet.png"))
    print("  [OK] item/aluminum_sheet.png")


# ── 3. incomplete_defend.png (item, 16x16) ───────────────────────────
def gen_incomplete_defend():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # gray metal base
    fill_noise(img, INC_GRAY, 8)
    # darker frame
    for i in range(16):
        img.putpixel((i, 0), INC_DARK)
        img.putpixel((i, 15), INC_DARK)
        img.putpixel((0, i), INC_DARK)
        img.putpixel((15, i), INC_DARK)
    # red wires – horizontal segment
    for x in range(3, 10):
        img.putpixel((x, 5), RED_WIRE)
        img.putpixel((x, 6), RED_DARK)
    # red wires – vertical segment
    for y in range(7, 12):
        img.putpixel((11, y), RED_WIRE)
        img.putpixel((12, y), RED_DARK)
    # incomplete gaps (transparent)
    for gx, gy in [(5, 8), (6, 9), (8, 11), (9, 3)]:
        img.putpixel((gx, gy), (0, 0, 0, 0))
    # metal highlights
    for hx, hy in [(3, 3), (4, 3), (10, 3), (3, 12), (13, 12)]:
        img.putpixel((hx, hy), INC_LIGHT)
    img.save(os.path.join(ITEM_DIR, "incomplete_defend.png"))
    print("  [OK] item/incomplete_defend.png")


# ── 4. incomplete_hash_chest.png (item, 16x16) ───────────────────────
def gen_incomplete_hash_chest():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # gray iron box
    fill_noise(img, INC_GRAY, 8)
    # box frame
    for i in range(16):
        img.putpixel((i, 0), INC_DARK)
        img.putpixel((i, 15), INC_DARK)
        img.putpixel((0, i), INC_DARK)
        img.putpixel((15, i), INC_DARK)
    # lock mechanism (center 4px)
    for lx in range(6, 10):
        for ly in range(6, 10):
            img.putpixel((lx, ly), LOCK_COL)
    img.putpixel((7, 7), INC_DARK)
    img.putpixel((8, 8), INC_DARK)
    # lock frame corners
    for cx, cy in [(6, 6), (9, 6), (6, 9), (9, 9)]:
        img.putpixel((cx, cy), INC_DARK)
    # incomplete gaps
    for gx, gy in [(3, 4), (12, 5), (4, 11), (11, 12)]:
        img.putpixel((gx, gy), (0, 0, 0, 0))
    # metal highlights
    for hx, hy in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        img.putpixel((hx, hy), INC_LIGHT)
    img.save(os.path.join(ITEM_DIR, "incomplete_hash_chest.png"))
    print("  [OK] item/incomplete_hash_chest.png")


# ── 5. raw_aluminum_block.png (block, 16x16) ─────────────────────────
def gen_raw_aluminum_block():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    fill_noise(img, RAW_AL_MAIN, 10)
    # horizontal dark bands
    for x in range(16):
        img.putpixel((x, 5), jitter(RAW_AL_DARK, 3))
        img.putpixel((x, 10), jitter(RAW_AL_DARK, 3))
    # scattered dark spots
    spots = [(1,1),(4,2),(7,1),(10,3),(13,1),
             (2,7),(5,8),(8,7),(11,8),(14,7),
             (1,12),(4,13),(7,12),(10,13),(13,12),
             (3,3),(9,4),(6,11),(12,14)]
    for sx, sy in spots:
        if 0 <= sx < 16 and 0 <= sy < 16:
            img.putpixel((sx, sy), jitter(RAW_AL_DARK, 5))
    # highlights
    for hx, hy in [(2,2),(6,3),(9,2),(12,4),(3,8),(7,9),(11,7),(14,9),(2,13),(5,12),(8,13),(11,14)]:
        if 0 <= hx < 16 and 0 <= hy < 16:
            img.putpixel((hx, hy), RAW_AL_LIGHT)
    img.save(os.path.join(BLOCK_DIR, "raw_aluminum_block.png"))
    print("  [OK] block/raw_aluminum_block.png")


# ── 6. aluminum_ore.png (block, 16x16) ───────────────────────────────
def gen_aluminum_ore():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    fill_noise(img, STONE_BG, 10)
    # darker stone specks
    for sx, sy in [(1,2),(3,1),(5,3),(7,1),(9,2),(11,1),(13,3),
                   (2,6),(4,7),(6,5),(8,7),(10,6),(12,5),(14,7),
                   (1,10),(3,9),(5,11),(7,9),(9,10),(11,9),(13,11),
                   (2,14),(4,13),(6,14),(8,12),(10,13),(12,14),(14,13)]:
        if 0 <= sx < 16 and 0 <= sy < 16:
            img.putpixel((sx, sy), jitter(STONE_DARK, 5))
    # aluminum ore clusters (light gray-blue)
    clusters = [
        [(3,3),(4,3),(3,4),(4,4)],
        [(8,5),(9,5),(8,6)],
        [(11,2),(12,2),(12,3)],
        [(6,9),(7,9),(6,10)],
        [(10,11),(11,11),(10,12)],
        [(13,8),(14,8),(13,9)],
    ]
    for cluster in clusters:
        for sx, sy in cluster:
            if 0 <= sx < 16 and 0 <= sy < 16:
                img.putpixel((sx, sy), jitter(AL_SHEET_MAIN, 5))
        # highlight on first pixel of cluster
        sx, sy = cluster[0]
        if 0 <= sx < 16 and 0 <= sy < 16:
            img.putpixel((sx, sy), RAW_AL_LIGHT)
    img.save(os.path.join(BLOCK_DIR, "aluminum_ore.png"))
    print("  [OK] block/aluminum_ore.png")


# ── 7. deepslate_aluminum_ore.png (block, 16x16) ─────────────────────
def gen_deepslate_aluminum_ore():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    fill_noise(img, DEEPSLATE_BG, 8)
    # darker deepslate specks
    for sx, sy in [(1,2),(3,1),(5,3),(7,1),(9,2),(11,1),(13,3),
                   (2,6),(4,7),(6,5),(8,7),(10,6),(12,5),(14,7),
                   (1,10),(3,9),(5,11),(7,9),(9,10),(11,9),(13,11),
                   (2,14),(4,13),(6,14),(8,12),(10,13),(12,14),(14,13)]:
        if 0 <= sx < 16 and 0 <= sy < 16:
            img.putpixel((sx, sy), jitter(DEEPSLATE_DARK, 5))
    # aluminum ore clusters
    clusters = [
        [(3,3),(4,3),(3,4),(4,4)],
        [(8,5),(9,5),(8,6)],
        [(11,2),(12,2),(12,3)],
        [(6,9),(7,9),(6,10)],
        [(10,11),(11,11),(10,12)],
        [(13,8),(14,8),(13,9)],
    ]
    for cluster in clusters:
        for sx, sy in cluster:
            if 0 <= sx < 16 and 0 <= sy < 16:
                img.putpixel((sx, sy), jitter(AL_SHEET_MAIN, 5))
        sx, sy = cluster[0]
        if 0 <= sx < 16 and 0 <= sy < 16:
            img.putpixel((sx, sy), RAW_AL_LIGHT)
    img.save(os.path.join(BLOCK_DIR, "deepslate_aluminum_ore.png"))
    print("  [OK] block/deepslate_aluminum_ore.png")


# ── 8. cold_resistant_casing.png (block, 16x16) ──────────────────────
def draw_casing_tile(img, ox, oy, size=16):
    """Draw the base cold-resistant casing texture into a 16x16 region."""
    # fill base with noise
    for x in range(size):
        for y in range(size):
            img.putpixel((ox + x, oy + y), jitter(CASING_MAIN, 6))
    # darker frame (1px inner border)
    for i in range(size):
        img.putpixel((ox + i, oy), jitter(CASING_DARK, 3))
        img.putpixel((ox + i, oy + size - 1), jitter(CASING_DARK, 3))
        img.putpixel((ox, oy + i), jitter(CASING_DARK, 3))
        img.putpixel((ox + size - 1, oy + i), jitter(CASING_DARK, 3))
    # rivets at corners and mid-edges
    rivet_positions = [(1, 1), (size - 2, 1), (1, size - 2), (size - 2, size - 2),
                       (size // 2, 1), (1, size // 2), (size - 2, size // 2), (size // 2, size - 2)]
    for rx, ry in rivet_positions:
        px, py = ox + rx, oy + ry
        if 0 <= px < img.width and 0 <= py < img.height:
            img.putpixel((px, py), CASING_LIGHT)
    # center rivet with dark ring
    cx, cy = ox + size // 2, oy + size // 2
    img.putpixel((cx, cy), CASING_LIGHT)
    for dx, dy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
        px, py = cx + dx, cy + dy
        if 0 <= px < img.width and 0 <= py < img.height:
            img.putpixel((px, py), jitter(CASING_DARK, 3))
    # scattered darker texture pixels
    for _ in range(8):
        tx = ox + random.randint(2, size - 3)
        ty = oy + random.randint(2, size - 3)
        if 0 <= tx < img.width and 0 <= ty < img.height:
            img.putpixel((tx, ty), CASING_DARK)


def gen_cold_resistant_casing():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw_casing_tile(img, 0, 0, 16)
    img.save(os.path.join(BLOCK_DIR, "cold_resistant_casing.png"))
    print("  [OK] block/cold_resistant_casing.png")


# ── 9. cold_resistant_casing_connected.png (block, 128x128) ──────────
def gen_cold_resistant_casing_connected():
    """
    Create's OMNIDIRECTIONAL connected texture (128x128 = 8x8 tiles of 16x16).

    Index layout (6-bit connection mask → col/row):
      col bit 0 = North connected   → removes top border
      col bit 1 = South connected   → removes bottom border
      col bit 2 = Up connected      → removes top AND bottom border
      row bit 0 = West connected    → removes left border
      row bit 1 = East connected    → removes right border
      row bit 2 = Down connected     → removes left AND right border

    Tile (0,0) = fully isolated (all 4 borders)
    Tile (7,7) = fully connected (no borders)
    """
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))

    for tile_row in range(8):
        for tile_col in range(8):
            ox = tile_col * 16
            oy = tile_row * 16

            # 1) draw base casing texture
            draw_casing_tile(img, ox, oy, 16)

            # 2) determine connections from bitmask
            north = (tile_col & 1) != 0
            south = (tile_col & 2) != 0
            up     = (tile_col & 4) != 0
            west  = (tile_row & 1) != 0
            east  = (tile_row & 2) != 0
            down   = (tile_row & 4) != 0

            # 3) draw border lines on non-connected sides
            bc = CASING_BORDER  # (0x5A, 0x6A, 0x7C)

            # Top border
            if not (north or up):
                for x in range(16):
                    img.putpixel((ox + x, oy), bc)
            # Bottom border
            if not (south or down):
                for x in range(16):
                    img.putpixel((ox + x, oy + 15), bc)
            # Left border
            if not west:
                for y in range(16):
                    img.putpixel((ox, oy + y), bc)
            # Right border
            if not east:
                for y in range(16):
                    img.putpixel((ox + 15, oy + y), bc)

            # Corner pixels: ensure consistency (border color when any two adjacent borders meet)
            top_border = not (north or up)
            bot_border = not (south or down)
            left_border = not west
            right_border = not east

            if top_border and left_border:
                img.putpixel((ox, oy), bc)
            if top_border and right_border:
                img.putpixel((ox + 15, oy), bc)
            if bot_border and left_border:
                img.putpixel((ox, oy + 15), bc)
            if bot_border and right_border:
                img.putpixel((ox + 15, oy + 15), bc)

    img.save(os.path.join(BLOCK_DIR, "cold_resistant_casing_connected.png"))
    print("  [OK] block/cold_resistant_casing_connected.png")


# ── Main ─────────────────────────────────────────────────────────────
if __name__ == "__main__":
    print("=" * 60)
    print("Generating aluminum textures for TimeX Rebirth")
    print(f"  Item dir: {ITEM_DIR}")
    print(f"  Block dir: {BLOCK_DIR}")
    print("=" * 60)

    gen_raw_aluminum()
    gen_aluminum_sheet()
    gen_incomplete_defend()
    gen_incomplete_hash_chest()
    gen_raw_aluminum_block()
    gen_aluminum_ore()
    gen_deepslate_aluminum_ore()
    gen_cold_resistant_casing()
    gen_cold_resistant_casing_connected()

    print("=" * 60)
    print("All 9 textures generated successfully!")
