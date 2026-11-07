#!/usr/bin/env python3
"""Deterministic original pixel art; Pillow is a development-only dependency."""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/sigma_companions'
THEMES = {
    'akari': ((106, 53, 74), (155, 78, 106), (238, 181, 163), (83, 126, 95), (48, 65, 63), (229, 171, 89)),
    'mio': ((49, 56, 91), (80, 92, 139), (237, 199, 183), (94, 105, 157), (47, 51, 73), (172, 190, 230)),
    'rei': ((54, 92, 94), (83, 143, 140), (215, 169, 140), (184, 123, 67), (57, 66, 77), (126, 219, 198)),
}

def box_texture(image, uv, size, color, face=None):
    x, y = uv; w, h, d = size
    draw = ImageDraw.Draw(image)
    dark = tuple(max(0, int(c * .76)) for c in color)
    light = tuple(min(255, int(c * 1.1)) for c in color)
    draw.rectangle((x+d, y, x+d+w-1, y+d-1), fill=light)
    draw.rectangle((x+d+w, y, x+d+w+w-1, y+d-1), fill=dark)
    for sx, sw, col in [(x,d,dark), (x+d,w,color), (x+d+w,d,dark), (x+d+w+d,w,dark)]:
        draw.rectangle((sx, y+d, sx+sw-1, y+d+h-1), fill=col)
    if face:
        face(draw, x+d, y+d, w, h)

def head_face(hair, eyes):
    def draw(d, x, y, w, h):
        d.rectangle((x,y,x+w-1,y+1), fill=hair)
        d.point((x,y+2), fill=hair); d.point((x+w-1,y+2), fill=hair)
        for ex in (x+1,x+5):
            d.rectangle((ex,y+3,ex+1,y+4), fill=(249,245,239))
            d.point((ex+1,y+3), fill=eyes); d.point((ex+1,y+4), fill=(37,35,51))
            d.point((ex,y+2), fill=(50,38,48))
        d.point((x+3,y+5), fill=(184,125,117))
        d.line((x+3,y+6,x+4,y+6), fill=(143,80,91))
        d.point((x,y+5), fill=(226,146,151)); d.point((x+7,y+5), fill=(226,146,151))
    return draw

for name, (hair, hair_light, skin, shirt, trousers, accent) in THEMES.items():
    image = Image.new('RGBA', (128,128), (0,0,0,0))
    box_texture(image, (0,0), (8,8,8), skin, head_face(hair, accent))
    box_texture(image, (0,16), (8,11,5), shirt)
    box_texture(image, (32,16), (3,11,4), shirt)
    box_texture(image, (48,16), (3,11,4), shirt)
    box_texture(image, (64,0), (9,4,9), hair)
    box_texture(image, (104,0), (8,3,1), hair_light)
    box_texture(image, (0,64), (9,13,3), hair)
    box_texture(image, (32,64), (2,10,5), hair_light)
    box_texture(image, (56,64), (2,10,5), hair)
    box_texture(image, (104,8), (2,3,3), accent)
    box_texture(image, (64,32), (4,13,4), trousers)
    box_texture(image, (80,32), (4,13,4), trousers)
    d = ImageDraw.Draw(image)
    # Collar, belt, pockets and cuffs on the front UV island.
    d.rectangle((5,21,10,22), fill=accent)
    d.rectangle((4,29,11,30), fill=(63,49,54))
    d.point((7,29), fill=accent); d.point((8,29), fill=accent)
    for x in (36,52):
        d.rectangle((x,27,x+2,29), fill=skin)
        d.line((x,26,x+2,26), fill=accent)
    for x in (68,84):
        d.rectangle((x,44,x+3,46), fill=(44,40,49))
        d.point((x+1,44), fill=accent)
    # Hair highlights, intentionally pixel-aligned.
    for x in (68,73,78,84,89): d.line((x,10,x,12), fill=hair_light)
    for y in (72,76,80): d.line((4,y,8,y), fill=hair_light)
    folder = ASSETS / 'textures/entity'; folder.mkdir(parents=True, exist_ok=True)
    image.save(folder / f'{name}.png')

for name, (_, _, _, shirt, _, accent) in THEMES.items():
    image = Image.new('RGBA',(16,16),(0,0,0,0)); d = ImageDraw.Draw(image)
    d.rectangle((3,2,12,14), fill=(51,43,61)); d.rectangle((4,3,11,13), fill=(241,226,196))
    d.rectangle((4,3,11,5), fill=shirt); d.line((5,11,10,11), fill=(159,135,121))
    d.polygon([(7,7),(5,9),(7,11),(9,9)], fill=accent)
    folder = ASSETS/'textures/item'; folder.mkdir(parents=True,exist_ok=True)
    image.save(folder/f'invitation_{name}.png')
image = Image.new('RGBA',(16,16),(0,0,0,0)); d=ImageDraw.Draw(image)
d.ellipse((3,6,12,14),outline=(111,76,36),width=3); d.ellipse((3,5,12,13),outline=(237,191,91),width=2)
d.polygon([(8,1),(12,4),(8,8),(4,4)],fill=(71,147,164)); d.polygon([(8,2),(10,4),(8,6)],fill=(170,244,235))
image.save(ASSETS/'textures/item/promise_ring.png')
icon=Image.new('RGBA',(64,64),(27,29,43,255)); d=ImageDraw.Draw(icon)
d.rounded_rectangle((3,3,60,60),radius=12,outline=(151,174,221),width=2)
d.polygon([(32,13),(16,28),(16,38),(32,53),(48,38),(48,28)],fill=(219,139,169))
d.polygon([(32,20),(23,29),(32,44),(41,29)],fill=(251,216,222))
d.line((16,28,48,28),fill=(160,87,128),width=2)
icon.save(ASSETS/'icon.png')
print('Generated original entity/item textures and mod icon.')