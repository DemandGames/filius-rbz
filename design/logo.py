#!/usr/bin/env python3
"""Pictures made from the logo of Filius (RBZ Wirtschaft Kiel Version), the network water tower
(since 21 October 2026; the variants are made by logo_clean.py from filius-logo-original.png):

- filius-logo.png: the whole logo. Splash screen (backgrounds.py), welcome card on the empty work area, info window
  (on a white tile, because the head of the info window is blue), start page of new web servers (rbz-logo.png, transparent), window icons from 48 px.
- filius-logo-klein.png: without the plugs, so the tower is larger. Mark in the header, 32 px icons.
- filius-logo-mini.png: also without the radio waves. 16 and 24 px icons (title bar, small taskbar).

The logo is not protected according to its publisher.

usage: python3 logo.py     (writes into ../src/resources and ../dist-files/Filius.ico)
"""
import os

from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, '..', 'src', 'resources')
LOGO = os.path.join(HERE, 'filius-logo.png')
KLEIN = os.path.join(HERE, 'filius-logo-klein.png')
MINI = os.path.join(HERE, 'filius-logo-mini.png')
DIST = os.path.join(HERE, '..', 'dist-files')


def scaled(size, source=LOGO):
    im = Image.open(source).convert('RGBA').resize((size, size), Image.LANCZOS)
    if size <= 64:
        # small sizes lose contrast when scaled down: sharpen a little
        im = im.filter(ImageFilter.UnsharpMask(radius=0.6, percent=60, threshold=1))
    return im


def icon(size):
    """window and taskbar icon of the given size: the variant that stays recognisable at that size"""
    return scaled(size, MINI if size <= 24 else KLEIN if size <= 32 else LOGO)


def tile(size):
    """the logo on a white rounded tile (for the blue head of the info window)"""
    ss = 4
    big = size * ss
    im = Image.new('RGBA', (big, big), (0, 0, 0, 0))
    ImageDraw.Draw(im).rounded_rectangle((0, 0, big - 1, big - 1), radius=int(big * 0.22), fill=(255, 255, 255, 255))
    inset = int(big * 0.08)
    logo = Image.open(LOGO).convert('RGBA').resize((big - 2 * inset, big - 2 * inset), Image.LANCZOS)
    im.alpha_composite(logo, (inset, inset))
    return im.resize((size, size), Image.LANCZOS)


def save(im, path):
    target = os.path.join(RES, path)
    os.makedirs(os.path.dirname(target), exist_ok=True)
    im.save(target)
    print('wrote', path, im.size)


def main():
    # mark in the header (38 px, sharp on high-resolution screens)
    save(scaled(38, KLEIN), 'gfx/modern/brand.png')
    save(scaled(76, KLEIN), 'gfx/modern/brand@2x.png')
    # logo in the info dialog
    save(tile(72), 'gfx/modern/logo-72.png')
    save(tile(144), 'gfx/modern/logo-72@2x.png')
    # welcome card on the empty work area
    save(scaled(72), 'gfx/modern/logo-willkommen.png')
    save(scaled(144), 'gfx/modern/logo-willkommen@2x.png')
    # window and taskbar icons
    sizes = (16, 24, 32, 48, 64, 128, 256)
    for size in sizes:
        save(icon(size), f'gfx/modern/app/filius-{size}.png')
    # Filius.ico next to "Filius starten.bat", for a desktop shortcut or a published app
    images = [icon(size) for size in sizes]
    ico = os.path.join(DIST, 'Filius.ico')
    images[-1].save(ico, format='ICO', sizes=[(s, s) for s in sizes], append_images=images[:-1])
    print('wrote Filius.ico', os.path.getsize(ico), 'bytes')
    # logo of the start page of new web servers: transparent PNG (it stands on the dark head of the page)
    target = os.path.join(RES, 'filius', 'modern', 'web', 'startseite', 'rbz-logo.png')
    # 192 px (shown at 96 px) with a palette of 256 colours: about 9 KB, so the page loads fast in the simulation
    scaled(192).quantize(colors=256, method=Image.Quantize.FASTOCTREE, dither=Image.Dither.NONE).save(target, optimize=True)
    old = os.path.join(RES, 'filius', 'modern', 'web', 'startseite', 'rbz-logo.jpg')
    if os.path.exists(old):
        os.remove(old)
    print('wrote rbz-logo.png', os.path.getsize(target), 'bytes')


if __name__ == '__main__':
    main()
