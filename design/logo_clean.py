#!/usr/bin/env python3
"""Cleans the logo of Filius (RBZ Wirtschaft Kiel Version) as delivered by the teacher on 21 October 2026
(filius-logo-original.png: the network water tower with radio waves and two patch cables, transparent background)
and makes the three variants that logo.py uses:

- filius-logo.png        (1024 x 1024) the whole logo: splash screen, info window, start page, icons from 48 px
- filius-logo-klein.png  (1024 x 1024) without the plugs, cables cut at the sides: header mark, 32 px icons
- filius-logo-mini.png   (1024 x 1024) also without the radio waves: 16 and 24 px icons (the tower stays large)

Cleaning: only the logo itself and the two radio waves are kept (about 1000 almost invisible specks around it are
removed), pixels with an opacity below 10 become fully transparent.
The former logo and icon are in frueher/.

usage: python3 logo_clean.py
"""
import os

import numpy as np
from PIL import Image
from scipy import ndimage

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, 'filius-logo-original.png')

# measured on the original (1254 x 1254): the cables leave the body at about these x positions
CABLE_LEFT, CABLE_RIGHT = 205, 1050


def square(img, margin):
    box = img.getbbox()
    c = img.crop(box)
    s = int(max(c.size) * (1 + 2 * margin))
    q = Image.new('RGBA', (s, s), (0, 0, 0, 0))
    q.paste(c, ((s - c.width) // 2, (s - c.height) // 2), c)
    return q.resize((1024, 1024), Image.LANCZOS)


def main():
    a = np.array(Image.open(SRC).convert('RGBA'))
    alpha = a[:, :, 3]
    lab, n = ndimage.label(alpha > 0)
    sizes = ndimage.sum(np.ones_like(alpha), lab, range(1, n + 1))
    order = list(np.argsort(sizes)[::-1] + 1)
    body, waves = order[0], order[1:3]
    a[~np.isin(lab, [body] + waves), 3] = 0
    a[a[:, :, 3] < 10, 3] = 0

    full = Image.fromarray(a, 'RGBA')
    square(full, 0.02).save(os.path.join(HERE, 'filius-logo.png'))

    k = a.copy()
    k[:, :CABLE_LEFT, 3] = 0
    k[:, CABLE_RIGHT:, 3] = 0
    square(Image.fromarray(k, 'RGBA'), 0.01).save(os.path.join(HERE, 'filius-logo-klein.png'))

    m = k.copy()
    m[np.isin(lab, waves), 3] = 0
    square(Image.fromarray(m, 'RGBA'), 0.01).save(os.path.join(HERE, 'filius-logo-mini.png'))
    print('wrote filius-logo.png, filius-logo-klein.png, filius-logo-mini.png')


if __name__ == '__main__':
    main()
