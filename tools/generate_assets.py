#!/usr/bin/env python3
"""Original procedural fan-art textures. No extracted game/anime assets. Dev-only Pillow."""
import json
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/sigma_companions'
SPEC = json.loads((ROOT / 'tools/model_specs.json').read_text())
SKIN = (250, 218, 194)
THEMES = {
    'konata': dict(hair=(67, 119, 204), light=(111, 164, 235), dark=(40, 72, 139), eyes=(64, 145, 105), shirt=(239, 241, 247), collar=(69, 100, 165), skirt=(73, 103, 171), accent=(244, 207, 80), socks=(43, 53, 82)),
    'kagami': dict(hair=(151, 111, 190), light=(188, 151, 222), dark=(98, 67, 137), eyes=(151, 76, 151), shirt=(244, 239, 244), collar=(174, 62, 82), skirt=(163, 57, 81), accent=(55, 43, 69), socks=(53, 44, 66)),
    'tsukasa': dict(hair=(211, 157, 190), light=(239, 190, 216), dark=(157, 108, 149), eyes=(183, 100, 151), shirt=(247, 240, 242), collar=(181, 68, 88), skirt=(174, 64, 88), accent=(246, 211, 85), socks=(66, 48, 65)),
}


def shade(color, factor):
    return tuple(max(0, min(255, round(c * factor))) for c in color)


def color_for(name, theme):
    if name.startswith(('hair', 'fringe', 'ahoge', 'tail')):
        return theme['light'] if name in ('fringe', 'hair_left') else theme['hair']
    if name.startswith(('ribbon', 'bow')):
        return theme['accent']
    if name in ('torso', 'left_arm', 'right_arm'):
        return theme['shirt']
    if name == 'skirt':
        return theme['skirt']
    if name.endswith('_leg'):
        return theme['socks']
    if name == 'snack':
        return (213, 157, 86)
    if name == 'cup':
        return (222, 237, 243)
    return SKIN


def paint_box(image, part, color, face=None):
    x, y = part['uv']; w, h, d = part['size']
    draw = ImageDraw.Draw(image)
    draw.rectangle((x+d, y, x+d+w-1, y+d-1), fill=shade(color, 1.1))
    draw.rectangle((x+d+w, y, x+d+w+w-1, y+d-1), fill=shade(color, .75))
    for sx, sw, factor in [(x, d, .83), (x+d, w, 1), (x+d+w, d, .88), (x+d+w+d, w, .78)]:
        draw.rectangle((sx, y+d, sx+sw-1, y+d+h-1), fill=shade(color, factor))
    if face:
        face(draw, x+d, y+d, w, h)


def face_for(name, theme):
    def paint(d, x, y, w, h):
        # Large readable eyes at actual in-game texture resolution; no photorealistic skin.
        for ex in (x+1, x+5):
            d.rectangle((ex, y+3, ex+1, y+5), fill=(255, 250, 245))
            d.rectangle((ex, y+4, ex+1, y+5), fill=theme['eyes'])
            d.point((ex+1, y+5), fill=(40, 37, 58))
            d.point((ex, y+3), fill=(255, 255, 250))
            if name == 'konata':
                d.line((ex, y+2, ex+1, y+2), fill=theme['dark'])
                d.point((ex, y+3), fill=theme['dark'])
            elif name == 'kagami':
                d.line((ex, y+2, ex+1, y+1 if ex == x+1 else y+2), fill=theme['dark'])
            else:
                d.rectangle((ex, y+2, ex+1, y+2), fill=theme['dark'])
        d.point((x+3, y+5), fill=(216, 164, 147))
        if name == 'tsukasa':
            d.line((x+3, y+6, x+4, y+6), fill=(157, 91, 111)); d.point((x+4, y+7), fill=(211, 131, 145))
        elif name == 'konata':
            d.line((x+3, y+6, x+5, y+6), fill=(145, 83, 98)); d.point((x+5, y+5), fill=(132, 88, 96))
        else:
            d.line((x+3, y+6, x+4, y+6), fill=(145, 83, 98))
        d.point((x, y+5), fill=(237, 173, 172)); d.point((x+7, y+5), fill=(237, 173, 172))
    return paint


def detail(image, part, theme):
    name = part['name']; x, y = part['uv']; w, h, d = part['size']; fx, fy = x+d, y+d
    draw = ImageDraw.Draw(image)
    if name.startswith(('hair', 'fringe', 'tail', 'ahoge')):
        for sx in range(fx, fx+w, 3):
            draw.line((sx, fy, sx, fy+h-2), fill=theme['light'])
        draw.line((fx, fy+h-1, fx+w-1, fy+h-1), fill=theme['dark'])
        # Hair highlight on the top island too.
        draw.line((x+d+1, y+1, x+d+w-2, y+1), fill=theme['light'])
    elif name == 'torso':
        draw.rectangle((fx, fy, fx+w-1, fy+1), fill=theme['collar'])
        draw.line((fx, fy+2, fx+3, fy+4), fill=theme['collar'])
        draw.line((fx+w-1, fy+2, fx+3, fy+4), fill=theme['collar'])
        draw.rectangle((fx+3, fy+3, fx+3, fy+6), fill=theme['accent'] if name == 'torso' else theme['collar'])
        draw.line((fx, fy+h-1, fx+w-1, fy+h-1), fill=theme['collar'])
        draw.point((fx+1, fy+6), fill=(188, 190, 203))
    elif name in ('left_arm', 'right_arm'):
        draw.rectangle((fx, fy, fx+w-1, fy+4), fill=theme['shirt'])
        draw.line((fx, fy+4, fx+w-1, fy+4), fill=theme['collar'])
        draw.point((fx+1, fy+1), fill=(255, 255, 255))
    elif name == 'skirt':
        for sx in range(fx+1, fx+w, 2):
            draw.line((sx, fy, sx, fy+h-1), fill=shade(theme['skirt'], .76))
    elif name.endswith('_leg'):
        draw.rectangle((fx, fy, fx+w-1, fy+1), fill=SKIN)
        draw.rectangle((fx, fy+h-2, fx+w-1, fy+h-1), fill=(48, 39, 47))
        draw.point((fx+1, fy+3), fill=shade(theme['socks'], 1.3))
    elif name.startswith(('bow', 'ribbon')):
        draw.line((fx, fy, fx+w-1, fy+h-1), fill=shade(theme['accent'], .78))
        draw.point((fx+w//2, fy+h//2), fill=shade(theme['accent'], 1.15))
    elif name == 'snack':
        draw.line((fx, fy, fx+w-1, fy+1), fill=(241, 196, 125))
        draw.rectangle((fx, fy+2, fx+w-1, fy+2), fill=(91, 54, 43))
    elif name == 'cup':
        draw.rectangle((fx, fy, fx+w-1, fy), fill=(113, 171, 197))
        draw.point((fx, fy+1), fill=(255, 255, 255))


for name, theme in THEMES.items():
    image = Image.new('RGBA', tuple(SPEC['texture_size']), (0, 0, 0, 0))
    for part in SPEC['common'] + SPEC['profiles'][name]:
        paint_box(image, part, color_for(part['name'], theme), face_for(name, theme) if part['name'] == 'head' else None)
        detail(image, part, theme)
    folder = ASSETS / 'textures/entity'; folder.mkdir(parents=True, exist_ok=True)
    image.save(folder / f'{name}.png')
    item = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); draw = ImageDraw.Draw(item)
    draw.rectangle((2, 1, 13, 14), fill=(63, 51, 71)); draw.rectangle((3, 2, 12, 13), fill=(248, 237, 214))
    draw.rectangle((3, 2, 12, 4), fill=theme['collar'])
    draw.rectangle((5, 6, 10, 11), fill=theme['hair']); draw.rectangle((6, 7, 9, 10), fill=SKIN)
    draw.point((6, 8), fill=theme['eyes']); draw.point((9, 8), fill=theme['eyes'])
    if name == 'konata':
        draw.rectangle((4, 6, 4, 12), fill=theme['hair']); draw.rectangle((11, 6, 11, 12), fill=theme['hair']); draw.point((8, 5), fill=theme['hair'])
    elif name == 'kagami':
        draw.rectangle((3, 6, 4, 12), fill=theme['hair']); draw.rectangle((11, 6, 12, 12), fill=theme['hair']); draw.rectangle((3, 5, 4, 6), fill=theme['accent']); draw.rectangle((11, 5, 12, 6), fill=theme['accent'])
    else:
        draw.rectangle((6, 4, 10, 5), fill=theme['accent']); draw.point((8, 5), fill=(187, 143, 53))
    folder = ASSETS / 'textures/item'; folder.mkdir(parents=True, exist_ok=True)
    item.save(folder / f'invitation_{name}.png')

cornet = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); draw = ImageDraw.Draw(cornet)
draw.polygon([(2, 12), (3, 8), (10, 2), (13, 3), (14, 6), (7, 13)], fill=(112, 70, 42))
draw.polygon([(3, 11), (4, 8), (10, 3), (12, 4), (13, 6), (6, 12)], fill=(221, 166, 94))
draw.line((5, 8, 8, 11), fill=(246, 205, 135), width=2); draw.line((8, 5, 11, 8), fill=(246, 205, 135), width=2)
draw.polygon([(10, 3), (12, 3), (14, 5), (13, 7), (11, 6)], fill=(83, 48, 42)); draw.point((12, 4), fill=(153, 95, 70))
cornet.save(ASSETS / 'textures/item/chocolate_cornet.png')
icon = Image.new('RGBA', (64, 64), (27, 29, 43, 255)); draw = ImageDraw.Draw(icon)
draw.rounded_rectangle((3, 3, 60, 60), radius=12, outline=(151, 174, 221), width=2)
for x, theme in zip((16, 32, 48), THEMES.values()):
    draw.rectangle((x-7, 22, x+7, 44), fill=theme['hair']); draw.rectangle((x-5, 26, x+5, 39), fill=SKIN)
    draw.rectangle((x-6, 40, x+6, 48), fill=theme['collar'])
    draw.point((x-3, 32), fill=theme['eyes']); draw.point((x+3, 32), fill=theme['eyes'])
draw.polygon([(32, 9), (35, 15), (42, 16), (37, 21), (38, 28), (32, 24), (26, 28), (27, 21), (22, 16), (29, 15)], fill=(246, 211, 85))
icon.save(ASSETS / 'icon.png')
print('Generated three distinct 256×256 fan-art atlases, invitations, cornet and icon.')