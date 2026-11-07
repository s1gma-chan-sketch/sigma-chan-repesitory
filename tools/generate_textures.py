#!/usr/bin/env python3
"""Generate the project's original pixel-art textures; requires only Python 3."""
from pathlib import Path
import struct
import zlib

OUT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/luckystar/textures/entity'
SIZE = 128

def png(path, pixels):
    def chunk(kind, data):
        return struct.pack('!I', len(data)) + kind + data + struct.pack('!I', zlib.crc32(kind + data) & 0xffffffff)
    rows = b''.join(b'\x00' + bytes(sum(row, ())) for row in pixels)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('!2I5B', SIZE, SIZE, 8, 6, 0, 0, 0))
    path.write_bytes(data + chunk(b'IDAT', zlib.compress(rows, 9)) + chunk(b'IEND', b''))

def draw(name, hair, eye, bow):
    skin = (255, 218, 202, 255)
    white = (246, 242, 234, 255)
    red = (164, 53, 76, 255)
    shade = (111, 38, 58, 255)
    pink = (230, 104, 124, 255)
    pixels = [[skin for _ in range(SIZE)] for _ in range(SIZE)]
    def rect(x1, y1, x2, y2, color):
        for y in range(y1, y2):
            for x in range(x1, x2):
                pixels[y][x] = color
    # The vanilla biped uses the classic skin UV positions in the upper 64x32 area.
    rect(0, 16, 16, 32, (245, 231, 220, 255))  # legs
    rect(4, 20, 8, 24, skin)
    rect(4, 28, 8, 32, (87, 61, 72, 255))
    rect(16, 16, 40, 32, white)                 # blouse
    rect(40, 16, 56, 32, white)                 # sleeves
    rect(44, 28, 48, 32, skin)
    rect(44, 25, 48, 27, red)
    rect(20, 20, 28, 21, red)                  # sailor collar and tie
    rect(21, 21, 23, 22, red); rect(25, 21, 27, 22, red)
    rect(22, 22, 26, 24, red)
    rect(23, 23, 25, 30, pink)
    rect(20, 30, 28, 32, red)
    # Face: front of the head is x=8..15, y=8..15.
    rect(9, 10, 11, 13, eye); rect(13, 10, 15, 13, eye)
    rect(9, 10, 10, 11, (255, 255, 255, 255)); rect(13, 10, 14, 11, (255, 255, 255, 255))
    rect(11, 14, 13, 15, (174, 96, 102, 255))
    # Dedicated uniform-colour UV regions for the custom 3D pieces.
    rect(0, 64, 48, 96, hair)
    rect(64, 64, 100, 82, red)
    for x in (69, 73, 77, 81, 85, 89): rect(x, 69, x + 1, 74, shade)
    rect(64, 96, 90, 110, bow)
    rect(100, 96, 128, 108, (88, 63, 84, 255))
    png(OUT / (name + '.png'), pixels)

OUT.mkdir(parents=True, exist_ok=True)
draw('konata', (46, 118, 198, 255), (58, 158, 81, 255), (250, 221, 104, 255))
draw('kagami', (145, 115, 202, 255), (81, 113, 187, 255), (179, 76, 96, 255))
draw('tsukasa', (168, 143, 211, 255), (89, 112, 192, 255), (247, 223, 117, 255))
draw('miyuki', (232, 165, 186, 255), (142, 102, 188, 255), (245, 223, 117, 255))
