"""Pictures of the installer (Filius (RBZ Wirtschaft Kiel Version)): side picture of the welcome and finish
pages (164 x 314) and the header picture (150 x 57), as 24-bit BMP files for NSIS.

usage: python3 bilder.py ZIELORDNER
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
LOGO = os.path.join(ROOT, 'design', 'filius-logo.png')
FONTS = os.path.join(ROOT, 'src', 'resources', 'filius', 'modern', 'fonts')


def font(name, size):
    return ImageFont.truetype(os.path.join(FONTS, f'AtkinsonHyperlegible-{name}.ttf'), size)


def verlauf(w, h, oben, unten):
    im = Image.new('RGB', (w, h))
    px = im.load()
    for y in range(h):
        for x in range(w):
            t = (0.75 * y / max(1, h - 1)) + (0.25 * x / max(1, w - 1))
            px[x, y] = tuple(round(oben[i] + (unten[i] - oben[i]) * t) for i in range(3))
    return im


def seitenbild(ziel, s=4):
    w, h = 164 * s, 314 * s
    im = verlauf(w, h, (0x2E, 0x6B, 0xD8), (0x17, 0x3F, 0x93))
    d = ImageDraw.Draw(im)
    # white rounded tile with the logo (as in the head of the info window)
    kachel = 112 * s
    x0, y0 = (w - kachel) // 2, 56 * s
    d.rounded_rectangle((x0, y0, x0 + kachel, y0 + kachel), radius=24 * s, fill=(255, 255, 255))
    logo = Image.open(LOGO).convert('RGBA')
    g = 96 * s
    logo = logo.resize((g, g), Image.LANCZOS)
    im.paste(logo, (x0 + (kachel - g) // 2, y0 + (kachel - g) // 2), logo)
    mitte = w // 2
    d.text((mitte, y0 + kachel + 22 * s), 'Filius', font=font('Bold', 30 * s), fill=(255, 255, 255), anchor='mt')
    d.text((mitte, y0 + kachel + 60 * s), 'RBZ Wirtschaft Kiel', font=font('Bold', 13 * s), fill=(0xDC, 0xE8, 0xFF), anchor='mt')
    d.text((mitte, y0 + kachel + 78 * s), 'Version', font=font('Bold', 13 * s), fill=(0xDC, 0xE8, 0xFF), anchor='mt')
    im.resize((164, 314), Image.LANCZOS).save(os.path.join(ziel, 'seite.bmp'))


def kopfbild(ziel, s=4):
    w, h = 150 * s, 57 * s
    im = Image.new('RGB', (w, h), (255, 255, 255))
    logo = Image.open(LOGO).convert('RGBA')
    g = 46 * s
    logo = logo.resize((g, g), Image.LANCZOS)
    im.paste(logo, (w - g - 8 * s, (h - g) // 2), logo)
    im.resize((150, 57), Image.LANCZOS).save(os.path.join(ziel, 'kopf.bmp'))


if __name__ == '__main__':
    ziel = sys.argv[1]
    os.makedirs(ziel, exist_ok=True)
    seitenbild(ziel)
    kopfbild(ziel)
    print('ok')
