#!/usr/bin/env python3
"""Dependency-free JSON/resource/UV checks, also run in CI."""
import json
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSET = RES / 'assets/sigma_companions'
for path in RES.rglob('*.json'):
    json.loads(path.read_text(encoding='utf-8'))
meta = json.loads((RES/'fabric.mod.json').read_text())
assert meta['id'] == 'sigma_companions'
assert meta['depends']['minecraft'] == '~1.21.1'
for name in ('akari', 'mio', 'rei'):
    texture = ASSET/'textures/entity'/f'{name}.png'
    data = texture.read_bytes()
    assert data[:8] == b'\x89PNG\r\n\x1a\n'
    assert struct.unpack('>II',data[16:24]) == (128,128)
    assert (ASSET/'textures/item'/f'invitation_{name}.png').exists()
    model = json.loads((ASSET/'models/item'/f'invitation_{name}.json').read_text())
    namespace, texture_path = model['textures']['layer0'].split(':', 1)
    assert (RES/'assets'/namespace/'textures'/(texture_path + '.png')).exists()
    recipe = json.loads((RES/'data/sigma_companions/recipe'/f'invitation_{name}.json').read_text())
    assert recipe['result']['id'] == f'sigma_companions:invitation_{name}'
# These are the exact cuboid UV rectangles used by CompanionModel.
boxes = [(0,0,8,8,8),(0,16,8,11,5),(32,16,3,11,4),(48,16,3,11,4),
         (64,0,9,4,9),(104,0,8,3,1),(0,64,9,13,3),(32,64,2,10,5),
         (56,64,2,10,5),(104,8,2,3,3),(64,32,4,13,4),(80,32,4,13,4)]
rectangles=[]
for u,v,w,h,d in boxes:
    rectangles += [(u+d,v,w,d),(u+d+w,v,w,d),(u,v+d,d,h),(u+d,v+d,w,h),(u+d+w,v+d,d,h),(u+d+w+d,v+d,w,h)]
for x,y,w,h in rectangles:
    assert x>=0 and y>=0 and x+w<=128 and y+h<=128, (x,y,w,h)
for i,a in enumerate(rectangles):
    for b in rectangles[i+1:]:
        ax,ay,aw,ah=a; bx,by,bw,bh=b
        assert ax+aw<=bx or bx+bw<=ax or ay+ah<=by or by+bh<=ay, (a,b)
languages = [json.loads(p.read_text()) for p in (ASSET/'lang').glob('*.json')]
assert len(languages)>=2 and set(languages[0]) == set(languages[1])
print('Resources OK: JSON, textures, recipes, language parity and non-overlapping model UVs.')