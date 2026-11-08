#!/usr/bin/env python3
"""Dependency-free resource/UV/generated-code checks, also run in CI."""
import json
import struct
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSET = RES / 'assets/sigma_companions'
CLIENT_RES = ROOT / 'src/client/resources'
for path in list(RES.rglob('*.json')) + list(CLIENT_RES.rglob('*.json')):
    json.loads(path.read_text(encoding='utf-8'))
meta = json.loads((RES / 'fabric.mod.json').read_text())
assert meta['id'] == 'sigma_companions'
assert meta['depends']['minecraft'] == '~1.21.1'
assert any(m['config'] == 'sigma_companions.client.mixins.json' and m['environment'] == 'client' for m in meta['mixins'])
mixin = json.loads((CLIENT_RES / 'sigma_companions.client.mixins.json').read_text())
assert mixin['refmap'] == 'sigma_companions.refmap.json'
assert 'HeldItemRendererMixin' in mixin['client']
assert (ROOT / 'src/client/java/dev/sigmachan/companions/client/mixin/HeldItemRendererMixin.java').exists()

spec = json.loads((ROOT / 'tools/model_specs.json').read_text())
assert spec['texture_size'] == [256, 256]
profiles = ('konata', 'kagami', 'tsukasa')
assert set(spec['profiles']) == set(profiles)


def png_size(path):
    data = path.read_bytes()
    assert data[:8] == b'\x89PNG\r\n\x1a\n', path
    return struct.unpack('>II', data[16:24])


def rectangles(parts):
    result = []
    for part in parts:
        u, v = part['uv']; w, h, d = part['size']
        result += [(u+d, v, w, d), (u+d+w, v, w, d), (u, v+d, d, h),
                   (u+d, v+d, w, h), (u+d+w, v+d, d, h), (u+d+w+d, v+d, w, h)]
    return result


def assert_no_overlap(rects, label):
    for x, y, w, h in rects:
        assert x >= 0 and y >= 0 and x+w <= 256 and y+h <= 256, (label, x, y, w, h)
    for i, a in enumerate(rects):
        for b in rects[i+1:]:
            ax, ay, aw, ah = a; bx, by, bw, bh = b
            assert ax+aw <= bx or bx+bw <= ax or ay+ah <= by or by+bh <= ay, (label, a, b)


for name in profiles:
    texture = ASSET / 'textures/entity' / f'{name}.png'
    assert png_size(texture) == (256, 256)
    assert png_size(ASSET / 'textures/item' / f'invitation_{name}.png') == (16, 16)
    model = json.loads((ASSET / 'models/item' / f'invitation_{name}.json').read_text())
    namespace, texture_path = model['textures']['layer0'].split(':', 1)
    assert (RES / 'assets' / namespace / 'textures' / (texture_path + '.png')).exists()
    recipe = json.loads((RES / 'data/sigma_companions/recipe' / f'invitation_{name}.json').read_text())
    assert recipe['result']['id'] == f'sigma_companions:invitation_{name}'
    assert_no_overlap(rectangles(spec['common'] + spec['profiles'][name]), name)
    # Distinct silhouette requirements from the user feedback.
    parts = {p['name']: p for p in spec['profiles'][name]}
    if name == 'konata':
        assert parts['hair_back']['size'][1] >= 16 and 'ahoge' in parts and 'ribbon' in parts
    elif name == 'kagami':
        assert 'tail_left' in parts and 'tail_right' in parts and parts['tail_left']['size'][1] >= 12
    else:
        assert 'bow_left' in parts and 'bow_right' in parts and parts['hair_back']['size'][1] <= 10

assert png_size(ASSET / 'textures/item/chocolate_cornet.png') == (16, 16)
cornet_recipe = json.loads((RES / 'data/sigma_companions/recipe/chocolate_cornet.json').read_text())
assert cornet_recipe['result']['id'] == 'sigma_companions:chocolate_cornet'
for old in ('akari', 'mio', 'rei'):
    assert not (ASSET / 'textures/entity' / f'{old}.png').exists()
    assert not (RES / 'data/sigma_companions/recipe' / f'invitation_{old}.json').exists()
    legacy = json.loads((ASSET / 'models/item' / f'invitation_{old}.json').read_text())
    assert legacy['textures']['layer0'].startswith('sigma_companions:item/invitation_')
assert not (RES / 'data/sigma_companions/recipe/promise_ring.json').exists()

languages = {p.stem: json.loads(p.read_text()) for p in (ASSET / 'lang').glob('*.json')}
assert len(languages) >= 2 and set(languages['ru_ru']) == set(languages['en_us'])
for key in languages['ru_ru']:
    assert 'романтика' not in key or 'promise_ring' in key
subprocess.run([sys.executable, str(ROOT / 'tools/generate_models.py'), '--check'], check=True)
print('Resources OK: JSON, 256px atlases, distinct silhouettes, recipes, language parity, mixin config and non-overlapping UVs.')