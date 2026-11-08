#!/usr/bin/env python3
"""Dev-only software preview of the exact cuboid spec + atlases (Pillow + NumPy).

This is not a Minecraft screenshot; it is a deterministic UV/geometry check.
Usage: PYTHONPATH=... python3 tools/preview_models.py --out release/previews
"""
import argparse
import json
import math
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
SPEC = json.loads((ROOT / 'tools/model_specs.json').read_text())


def rotation(yaw):
    c, s = math.cos(yaw), math.sin(yaw)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]], dtype=float)


def corners(part):
    ox, oy, oz = part['offset']; w, h, d = part['size']; e = part.get('dilation', 0)
    x0, y0, z0 = ox-e, oy-e, oz-e; x1, y1, z1 = ox+w+e, oy+h+e, oz+d+e
    return np.array([[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],
                     [x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]], dtype=float)


def faces(part):
    u, v = part['uv']; w, h, d = part['size']
    # index order matches UV rectangle top-left, top-right, bottom-right, bottom-left.
    return [
        ('front', [0,1,2,3], (u+d, v+d, w, h), (0,0,-1)),
        ('back',  [5,4,7,6], (u+d+w+d, v+d, w, h), (0,0,1)),
        ('right', [1,5,6,2], (u+d+w, v+d, d, h), (1,0,0)),
        ('left',  [4,0,3,7], (u, v+d, d, h), (-1,0,0)),
        ('top',   [4,5,1,0], (u+d, v, w, d), (0,-1,0)),
        ('bottom',[3,2,6,7], (u+d+w, v, w, d), (0,1,0)),
    ]


def render(profile, yaw=0.0, width=420, height=640, scale=16.0):
    tex = np.array(Image.open(ROOT / f'src/main/resources/assets/sigma_companions/textures/entity/{profile}.png').convert('RGBA'))
    img = np.zeros((height, width, 4), dtype=np.uint8); img[:, :, :3] = (245, 246, 250); img[:, :, 3] = 255
    zbuf = np.full((height, width), np.inf)
    parts = {p['name']: p for p in SPEC['common'] + SPEC['profiles'][profile]}
    origins = {}
    def origin(name):
        if name in origins: return origins[name]
        p = parts[name]; base = np.zeros(3) if p['parent'] is None else origin(p['parent'])
        origins[name] = base + np.array(p['pivot'], dtype=float); return origins[name]
    rot = rotation(yaw)
    light = np.array([-.4, -.7, -.65]); light /= np.linalg.norm(light)
    quads = []
    for name, part in parts.items():
        if name in ('snack', 'cup'): continue
        pts = corners(part) + origin(name)
        for fname, idx, uv, normal in faces(part):
            n = rot @ np.array(normal, dtype=float)
            if n[2] >= -1e-6: continue  # camera looks along +z from negative z
            world = pts[idx] @ rot.T
            shade = .62 + .38 * max(0, float(np.dot(n, light)))
            quads.append((world, uv, shade))
    cx, cy = width/2, 200
    for world, (u, v, tw, th), shade in quads:
        sx = cx + world[:, 0] * scale
        sy = cy + world[:, 1] * scale
        depth = world[:, 2]
        uvpts = np.array([[u, v], [u+tw, v], [u+tw, v+th], [u, v+th]], dtype=float)
        for tri in ((0,1,2),(0,2,3)):
            x = sx[list(tri)]; y = sy[list(tri)]; z = depth[list(tri)]; t = uvpts[list(tri)]
            minX = max(0, int(math.floor(min(x)))); maxX = min(width-1, int(math.ceil(max(x))))
            minY = max(0, int(math.floor(min(y)))); maxY = min(height-1, int(math.ceil(max(y))))
            if maxX < minX or maxY < minY: continue
            xs = np.arange(minX, maxX+1) + .5; ys = np.arange(minY, maxY+1) + .5
            px, py = np.meshgrid(xs, ys)
            x0,y0=x[0],y[0]; x1,y1=x[1],y[1]; x2,y2=x[2],y[2]
            den = (y1-y2)*(x0-x2)+(x2-x1)*(y0-y2)
            if abs(den) < 1e-9: continue
            a = ((y1-y2)*(px-x2)+(x2-x1)*(py-y2))/den
            b = ((y2-y0)*(px-x2)+(x0-x2)*(py-y2))/den
            c = 1-a-b
            inside = (a>=-1e-4)&(b>=-1e-4)&(c>=-1e-4)
            zz = a*z[0]+b*z[1]+c*z[2]
            yy, xx = np.nonzero(inside)
            if len(xx)==0: continue
            gx = xx+minX; gy = yy+minY; gz = zz[yy,xx]
            closer = gz < zbuf[gy,gx]
            gx,gy,gz = gx[closer],gy[closer],gz[closer]
            aa,bb,cc = a[yy,xx][closer],b[yy,xx][closer],c[yy,xx][closer]
            uu = np.clip(np.floor(aa*t[0,0]+bb*t[1,0]+cc*t[2,0]).astype(int), 0, tex.shape[1]-1)
            vv = np.clip(np.floor(aa*t[0,1]+bb*t[1,1]+cc*t[2,1]).astype(int), 0, tex.shape[0]-1)
            color = tex[vv,uu]
            opaque = color[:,3] > 0
            gx,gy,gz,color = gx[opaque],gy[opaque],gz[opaque],color[opaque]
            rgb = np.clip(color[:,:3].astype(float)*shade, 0, 255).astype(np.uint8)
            img[gy,gx,:3] = rgb; zbuf[gy,gx] = gz
    out = Image.fromarray(img, 'RGBA')
    # Ground reference line at player feet (model y=24).
    d = ImageDraw.Draw(out); ground = int(cy + 24*scale)
    d.line((40, ground+2, width-40, ground+2), fill=(205,210,220), width=2)
    return out


if __name__ == '__main__':
    ap = argparse.ArgumentParser(); ap.add_argument('--out', default='release/previews'); args = ap.parse_args()
    out = ROOT / args.out; out.mkdir(parents=True, exist_ok=True)
    for profile in SPEC['profiles']:
        render(profile, 0).save(out / f'{profile}-front.png')
        render(profile, math.radians(28)).save(out / f'{profile}-three-quarter.png')
    print(f'Wrote geometry/UV previews to {out}')
