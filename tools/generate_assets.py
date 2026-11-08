#!/usr/bin/env python3
"""Original procedural fan skins on a player-proportioned 32 px rig.

The uploaded 64x64 skins were visual references only; no reference pixels are
embedded. The first 64x64 islands follow the vanilla player layout, while the
128x128 atlas adds long hair, ribbons, bows and held props. Dev-only Pillow.
"""
import json
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/sigma_companions'
SPEC = json.loads((ROOT / 'tools/model_specs.json').read_text())
SKIN = (255, 219, 181)
SKIN_SHADE = (232, 184, 143)
WHITE = (247, 252, 250)
RED = (226, 75, 91)
DARK_RED = (163, 38, 62)
SHOE = (83, 55, 43)
THEMES = {
    'konata': dict(hair=(83, 105, 184), light=(119, 143, 218), dark=(52, 68, 137), eyes=(73, 160, 88), accent=(246, 211, 85)),
    'kagami': dict(hair=(177, 128, 226), light=(207, 169, 244), dark=(112, 72, 163), eyes=(112, 76, 181), accent=(48, 35, 58)),
    'tsukasa': dict(hair=(205, 157, 232), light=(230, 192, 247), dark=(146, 100, 181), eyes=(157, 94, 181), accent=(246, 211, 85)),
}
TRANSPARENT = (0, 0, 0, 0)


def shade(color, factor):
    return tuple(max(0, min(255, round(c * factor))) for c in color)


def box_faces(part):
    x, y = part['uv']; w, h, d = part['size']
    return {
        'top': (x+d, y, w, d), 'bottom': (x+d+w, y, w, d),
        'right': (x, y+d, d, h), 'front': (x+d, y+d, w, h),
        'left': (x+d+w, y+d, d, h), 'back': (x+d+w+d, y+d, w, h),
    }


def fill_face(image, rect, color):
    x, y, w, h = rect
    ImageDraw.Draw(image).rectangle((x, y, x+w-1, y+h-1), fill=color)


def paint_box(image, part, color):
    for name, rect in box_faces(part).items():
        factor = {'top': 1.08, 'bottom': .68, 'right': .84, 'front': 1.0, 'left': .9, 'back': .78}[name]
        fill_face(image, rect, shade(color, factor))


def paint_hair(image, part, theme):
    paint_box(image, part, theme['hair'])
    d = ImageDraw.Draw(image)
    for name, (x, y, w, h) in box_faces(part).items():
        if name in ('top', 'bottom'):
            d.line((x+1, y+1, x+w-2, y+1), fill=theme['light'])
            continue
        for sx in range(x+1, x+w, 3):
            d.line((sx, y+1, sx, y+h-2), fill=theme['light'])
        d.line((x, y+h-1, x+w-1, y+h-1), fill=theme['dark'])


def paint_face(image, part, profile, theme):
    paint_box(image, part, SKIN)
    x, y, w, h = box_faces(part)['front']; d = ImageDraw.Draw(image)
    # Brows and large but compact anime eyes; keep the 8x8 player face readable.
    for ex in (x+1, x+5):
        d.line((ex, y+2, ex+1, y+2), fill=theme['dark'])
        d.rectangle((ex, y+3, ex+1, y+4), fill=(255, 255, 255))
        d.point((ex, y+4), fill=theme['eyes']); d.point((ex+1, y+4), fill=shade(theme['eyes'], .72))
        d.point((ex, y+3), fill=(255, 255, 255))
        if profile == 'kagami':
            d.point((ex+1, y+2), fill=theme['dark'])
    d.point((x+3, y+5), fill=SKIN_SHADE)
    if profile == 'konata':
        d.line((x+3, y+6, x+4, y+6), fill=(156, 89, 102)); d.point((x+1, y+5), fill=(128, 88, 98))
    elif profile == 'kagami':
        d.line((x+3, y+6, x+4, y+6), fill=(146, 78, 96))
    else:
        d.line((x+3, y+6, x+4, y+6), fill=(171, 96, 119)); d.point((x+4, y+7), fill=(219, 139, 158))
    d.point((x, y+5), fill=(244, 177, 167)); d.point((x+7, y+5), fill=(244, 177, 167))


def paint_hat(image, part, profile, theme):
    # Vanilla outer-head layer: opaque hair on top/sides/back, transparent face window.
    for name, rect in box_faces(part).items():
        if name != 'front':
            paint_hair_rect(image, rect, theme)
    x, y, w, h = box_faces(part)['front']; d = ImageDraw.Draw(image)
    d.rectangle((x, y, x+w-1, y+1), fill=theme['hair'])
    d.line((x, y, x, y+h-1), fill=theme['hair']); d.line((x+w-1, y, x+w-1, y+h-1), fill=theme['hair'])
    fringe = [2, 3, 2, 1, 2, 3, 2, 2] if profile != 'tsukasa' else [2, 2, 3, 2, 1, 2, 3, 2]
    for i, length in enumerate(fringe):
        d.line((x+i, y+2, x+i, y+min(length, 2)), fill=theme['light'] if i % 3 == 0 else theme['hair'])


def paint_hair_rect(image, rect, theme):
    x, y, w, h = rect; d = ImageDraw.Draw(image)
    d.rectangle((x, y, x+w-1, y+h-1), fill=theme['hair'])
    for sx in range(x+1, x+w, 3):
        d.line((sx, y, sx, y+h-1), fill=theme['light'])
    d.line((x, y+h-1, x+w-1, y+h-1), fill=theme['dark'])


def paint_torso(image, part, theme):
    paint_box(image, part, WHITE)
    d = ImageDraw.Draw(image)
    for name in ('front', 'back'):
        x, y, w, h = box_faces(part)[name]
        d.rectangle((x, y, x+w-1, y+1), fill=RED)
        if name == 'front':
            d.line((x, y+2, x+3, y+5), fill=RED); d.line((x+w-1, y+2, x+4, y+5), fill=RED)
            d.rectangle((x+3, y+5, x+4, y+7), fill=RED); d.point((x+3, y+8), fill=DARK_RED)
        else:
            d.rectangle((x+1, y+2, x+w-2, y+4), fill=RED)
        d.rectangle((x, y+h-2, x+w-1, y+h-1), fill=RED)
    for name in ('left', 'right'):
        x, y, w, h = box_faces(part)[name]
        d.rectangle((x, y+h-2, x+w-1, y+h-1), fill=RED)


def paint_arm(image, part):
    paint_box(image, part, WHITE)
    d = ImageDraw.Draw(image)
    for name, rect in box_faces(part).items():
        x, y, w, h = rect
        if name == 'top':
            d.rectangle((x, y, x+w-1, y+h-1), fill=WHITE); continue
        if name == 'bottom':
            d.rectangle((x, y, x+w-1, y+h-1), fill=SKIN); continue
        d.rectangle((x, y+8, x+w-1, y+9), fill=RED)
        d.rectangle((x, y+10, x+w-1, y+h-1), fill=SKIN)


def paint_leg(image, part, profile):
    paint_box(image, part, SKIN)
    d = ImageDraw.Draw(image)
    for name, rect in box_faces(part).items():
        x, y, w, h = rect
        if name == 'top':
            d.rectangle((x, y, x+w-1, y+h-1), fill=(47, 38, 58) if profile == 'kagami' else WHITE); continue
        if name == 'bottom':
            d.rectangle((x, y, x+w-1, y+h-1), fill=SHOE); continue
        if profile == 'kagami':
            d.rectangle((x, y, x+w-1, y+7), fill=(47, 38, 58))
            d.line((x, y+1, x+w-1, y+1), fill=(70, 56, 84))
        else:
            d.rectangle((x, y, x+w-1, y+1), fill=WHITE)
            d.line((x, y+2, x+w-1, y+2), fill=RED)
        d.rectangle((x, y+h-3, x+w-1, y+h-1), fill=SHOE)
        d.line((x, y+h-3, x+w-1, y+h-3), fill=(113, 78, 58))


def paint_hem(image, part):
    paint_box(image, part, WHITE)
    d = ImageDraw.Draw(image)
    for name, (x, y, w, h) in box_faces(part).items():
        if name in ('top', 'bottom'): continue
        d.rectangle((x, y+h-1, x+w-1, y+h-1), fill=RED)
        for sx in range(x+1, x+w, 2):
            d.line((sx, y, sx, y+h-2), fill=(225, 235, 234))


def paint_accessory(image, part, theme):
    paint_box(image, part, theme['accent'])
    x, y, w, h = box_faces(part)['front']; d = ImageDraw.Draw(image)
    d.line((x, y, x+w-1, y+h-1), fill=shade(theme['accent'], .75))
    if w > 1 and h > 1: d.point((x+w//2, y+h//2), fill=shade(theme['accent'], 1.15))


def paint_prop(image, part):
    if part['name'] == 'snack':
        paint_box(image, part, (218, 164, 94))
        x, y, w, h = box_faces(part)['front']; d = ImageDraw.Draw(image)
        d.rectangle((x, y, x+w-1, y+1), fill=(244, 205, 139)); d.rectangle((x, y+2, x+w-1, y+2), fill=(88, 51, 42))
    else:
        paint_box(image, part, (225, 240, 246))
        x, y, w, h = box_faces(part)['top']; ImageDraw.Draw(image).rectangle((x, y, x+w-1, y+h-1), fill=(105, 169, 199))


for profile, theme in THEMES.items():
    image = Image.new('RGBA', tuple(SPEC['texture_size']), TRANSPARENT)
    for part in SPEC['common'] + SPEC['profiles'][profile]:
        name = part['name']
        if name == 'head': paint_face(image, part, profile, theme)
        elif name == 'hat': paint_hat(image, part, profile, theme)
        elif name == 'torso': paint_torso(image, part, theme)
        elif name in ('left_arm', 'right_arm'): paint_arm(image, part)
        elif name.endswith('_leg'): paint_leg(image, part, profile)
        elif name == 'uniform_hem': paint_hem(image, part)
        elif name.startswith(('hair', 'tail', 'ahoge')): paint_hair(image, part, theme)
        elif name.startswith(('ribbon', 'bow')): paint_accessory(image, part, theme)
        elif name in ('snack', 'cup'): paint_prop(image, part)
        # jacket/sleeves/pants remain transparent: details are on the base layer.
    folder = ASSETS / 'textures/entity'; folder.mkdir(parents=True, exist_ok=True)
    image.save(folder / f'{profile}.png')

    item = Image.new('RGBA', (16, 16), TRANSPARENT); draw = ImageDraw.Draw(item)
    draw.rectangle((2, 1, 13, 14), fill=(63, 51, 71)); draw.rectangle((3, 2, 12, 13), fill=(248, 237, 214))
    draw.rectangle((3, 2, 12, 4), fill=RED)
    draw.rectangle((5, 6, 10, 11), fill=theme['hair']); draw.rectangle((6, 7, 9, 10), fill=SKIN)
    draw.point((6, 8), fill=theme['eyes']); draw.point((9, 8), fill=theme['eyes'])
    if profile == 'konata':
        draw.rectangle((4, 6, 4, 12), fill=theme['hair']); draw.rectangle((11, 6, 11, 12), fill=theme['hair']); draw.point((8, 5), fill=theme['hair'])
    elif profile == 'kagami':
        draw.rectangle((3, 6, 4, 12), fill=theme['hair']); draw.rectangle((11, 6, 12, 12), fill=theme['hair']); draw.rectangle((3, 5, 4, 6), fill=theme['accent']); draw.rectangle((11, 5, 12, 6), fill=theme['accent'])
    else:
        draw.rectangle((6, 4, 10, 5), fill=theme['accent']); draw.point((8, 5), fill=(187, 143, 53))
    folder = ASSETS / 'textures/item'; folder.mkdir(parents=True, exist_ok=True)
    item.save(folder / f'invitation_{profile}.png')

cornet = Image.new('RGBA', (16, 16), TRANSPARENT); draw = ImageDraw.Draw(cornet)
draw.polygon([(2, 12), (3, 8), (10, 2), (13, 3), (14, 6), (7, 13)], fill=(112, 70, 42))
draw.polygon([(3, 11), (4, 8), (10, 3), (12, 4), (13, 6), (6, 12)], fill=(221, 166, 94))
draw.line((5, 8, 8, 11), fill=(246, 205, 135), width=2); draw.line((8, 5, 11, 8), fill=(246, 205, 135), width=2)
draw.polygon([(10, 3), (12, 3), (14, 5), (13, 7), (11, 6)], fill=(83, 48, 42)); draw.point((12, 4), fill=(153, 95, 70))
cornet.save(ASSETS / 'textures/item/chocolate_cornet.png')
icon = Image.new('RGBA', (64, 64), (27, 29, 43, 255)); draw = ImageDraw.Draw(icon)
draw.rounded_rectangle((3, 3, 60, 60), radius=12, outline=(151, 174, 221), width=2)
for x, theme in zip((16, 32, 48), THEMES.values()):
    draw.rectangle((x-6, 18, x+6, 34), fill=theme['hair']); draw.rectangle((x-4, 24, x+4, 32), fill=SKIN)
    draw.rectangle((x-5, 34, x+5, 48), fill=WHITE); draw.rectangle((x-5, 34, x+5, 36), fill=RED)
    draw.rectangle((x-4, 48, x-1, 56), fill=SKIN); draw.rectangle((x+1, 48, x+4, 56), fill=SKIN)
    draw.point((x-2, 28), fill=theme['eyes']); draw.point((x+2, 28), fill=theme['eyes'])
icon.save(ASSETS / 'icon.png')
print('Generated three original 128x128 player-proportioned fan atlases, invitations, cornet and icon.')
