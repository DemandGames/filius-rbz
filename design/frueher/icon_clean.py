#!/usr/bin/env python3
"""Cleans the Filius RBZ icon (filius-icon-original.png, as delivered by the teacher): the rounded
square gets a clean, smooth edge (no specks around it), it is cut out and made square.
Result: filius-icon.png (1024 x 1024), used by logo.py.

usage: python3 icon_clean.py
"""
import os

import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, 'filius-icon-original.png')
OUT = os.path.join(HERE, 'filius-icon.png')

# measured on the original: the rounded square and the radius of its corners
LEFT, TOP, RIGHT, BOTTOM, RADIUS = 30, 43, 1225, 1214, 250
SS = 4


def main():
    im = np.array(Image.open(SRC).convert('RGBA')).astype(np.float64)
    h, w = im.shape[:2]
    # smooth mask of the rounded square (drawn 4x larger, then scaled down)
    big = Image.new('L', (w * SS, h * SS), 0)
    ImageDraw.Draw(big).rounded_rectangle((LEFT * SS, TOP * SS, RIGHT * SS - 1, BOTTOM * SS - 1), radius=RADIUS * SS, fill=255)
    mask = np.array(big.resize((w, h), Image.LANCZOS)).astype(np.float64)
    # colours at the edge: take the colour of the nearest fully opaque pixel (no dark fringe)
    solid = im[:, :, 3] >= 250
    _, (iy, ix) = ndimage.distance_transform_edt(~solid, return_indices=True)
    rgb = im[iy, ix, :3]
    out = np.dstack([rgb, mask]).clip(0, 255).astype(np.uint8)
    icon = Image.fromarray(out, 'RGBA').crop((LEFT, TOP, RIGHT, BOTTOM))
    size = max(icon.size)
    square = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    square.paste(icon, ((size - icon.width) // 2, (size - icon.height) // 2), icon)
    square.resize((1024, 1024), Image.LANCZOS).save(OUT)
    print('wrote', OUT)


if __name__ == '__main__':
    main()
