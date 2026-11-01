"""
Renders the vector sources in icons.py (and backgrounds.py) to PNG files at 1x and 2x.

usage: python3 render.py [output-dir] [--sheet sheet.png]
"""
import base64
import importlib
import os
import sys

from playwright.sync_api import sync_playwright

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
FONT_DIR = os.path.join(HERE, "..", "src", "resources", "filius", "modern", "fonts")


def font_face():
    css = ""
    for weight, name in ((400, "Regular"), (700, "Bold")):
        with open(os.path.join(FONT_DIR, f"AtkinsonHyperlegible-{name}.ttf"), "rb") as f:
            data = base64.b64encode(f.read()).decode()
        css += (f"@font-face{{font-family:'Atkinson Hyperlegible';font-weight:{weight};"
                f"src:url(data:font/ttf;base64,{data}) format('truetype');}}")
    return css


def to_gif(png_bytes, target):
    """GIF has only on/off transparency: keep pixels that are at least half opaque."""
    import io
    from PIL import Image
    im = Image.open(io.BytesIO(png_bytes)).convert("RGBA")
    bg = Image.new("RGBA", im.size, (255, 255, 255, 0))
    flat = Image.alpha_composite(Image.new("RGBA", im.size, (246, 248, 250, 255)), im)
    mask = im.split()[3].point(lambda a: 255 if a >= 110 else 0)
    pal = flat.convert("RGB").convert("P", palette=Image.ADAPTIVE, colors=255)
    pal.paste(255, (0, 0) + im.size, Image.eval(mask, lambda a: 255 - a))
    pal.save(target, transparency=255)


def collect():
    icons = {}
    for module in ("icons", "backgrounds"):
        try:
            m = importlib.import_module(module)
        except ModuleNotFoundError:
            continue
        icons.update(m.ICONS)
    return icons


def render(out_dir, only=None):
    icons = collect()
    css = font_face()
    os.environ.setdefault("PLAYWRIGHT_BROWSERS_PATH", "/opt/pw-browsers")
    written = []
    with sync_playwright() as p:
        browser = p.chromium.launch()
        for scale in (1, 2):
            page = browser.new_page(device_scale_factor=scale)
            for path, spec in icons.items():
                if only and only not in path:
                    continue
                w, h, body = spec[0], spec[1], spec[2]
                opaque = len(spec) > 3 and "opaque" in spec[3:]
                if scale == 2 and (len(spec) > 3 and "nohidpi" in spec[3:] or path.endswith(".gif")):
                    continue
                svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" '
                       f'viewBox="0 0 {w} {h}">{body}</svg>')
                html = (f"<html><head><style>{css} html,body{{margin:0;padding:0;background:transparent}}"
                        f"svg{{display:block}}</style></head><body>{svg}</body></html>")
                page.set_viewport_size({"width": w, "height": h})
                page.set_content(html)
                page.evaluate("document.fonts.ready")
                target = os.path.join(out_dir, path if scale == 1 else path[:-4] + "@2x.png")
                os.makedirs(os.path.dirname(target), exist_ok=True)
                if target.endswith(".gif"):
                    png = page.screenshot(omit_background=True, clip={"x": 0, "y": 0, "width": w, "height": h})
                    to_gif(png, target)
                else:
                    page.screenshot(path=target, omit_background=not opaque,
                                    clip={"x": 0, "y": 0, "width": w, "height": h})
                written.append(target)
            page.close()
        browser.close()
    return written


def contact_sheet(files, sheet_path, bg=(242, 245, 248)):
    from PIL import Image, ImageDraw
    files = [f for f in files if "@2x" not in f and "/app/" not in f]
    cell = 120
    cols = 10
    rows = (len(files) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * cell, rows * cell), bg)
    d = ImageDraw.Draw(sheet)
    for i, f in enumerate(sorted(files)):
        im = Image.open(f).convert("RGBA")
        im.thumbnail((cell - 10, cell - 26))
        x = (i % cols) * cell
        y = (i // cols) * cell
        sheet.paste(im, (x + (cell - im.width) // 2, y + 6), im)
        d.text((x + 4, y + cell - 16), os.path.basename(f)[:19], fill=(40, 50, 60))
    sheet.save(sheet_path)


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 and not sys.argv[1].startswith("--") else os.path.join(HERE, "..", "src", "resources")
    only = None
    if "--only" in sys.argv:
        only = sys.argv[sys.argv.index("--only") + 1]
    files = render(out, only)
    print(f"rendered {len(files)} files into {out}")
    if "--sheet" in sys.argv:
        contact_sheet(files, sys.argv[sys.argv.index("--sheet") + 1])
