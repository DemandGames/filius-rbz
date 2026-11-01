"""Filius.icns for the macOS app from the logo: python3 macicon.py ZIEL.icns"""
import os, sys
from PIL import Image
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
im = Image.open(os.path.join(ROOT, 'design', 'filius-logo.png')).convert('RGBA')
# macOS icons sit a little inside the tile: 12 % margin
s = 1024
tile = Image.new('RGBA', (s, s), (0, 0, 0, 0))
g = int(s * 0.80)
tile.paste(im.resize((g, g), Image.LANCZOS), ((s - g) // 2, (s - g) // 2))
tile.save(sys.argv[1], format='ICNS', sizes=[(16, 16), (32, 32), (64, 64), (128, 128), (256, 256), (512, 512), (1024, 1024)])
print('ok')
