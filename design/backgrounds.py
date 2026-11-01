"""
Filius (RBZ Wirtschaft Kiel Version) – large graphics: canvas tiles, splash screen, desktop wallpaper, info dialog.
Uses the device drawings from icons.py so the splash shows the same visual language as the app.
"""
from icons import ICONS as DEVICE_ICONS, INK, INK2, INK3, BLUE, LED, GREEN, AMBER

ICONS = {}


def image(path, w, h, body, opaque=True, hidpi=False):
    flags = ("opaque",) if opaque else ()
    if not hidpi:
        flags += ("nohidpi",)
    ICONS[path] = (w, h, body) + flags


def device(name, x, y, scale=1.0):
    w, h, body = DEVICE_ICONS[f"gfx/hardware/{name}.png"][:3]
    return f'<svg x="{x}" y="{y}" width="{w * scale}" height="{h * scale}" viewBox="0 0 {w} {h}">{body}</svg>'


def dots(w, h, step, color, r, bg):
    out = f'<rect width="{w}" height="{h}" fill="{bg}"/>'
    for yy in range(step // 2, h, step):
        for xx in range(step // 2, w, step):
            out += f'<circle cx="{xx}" cy="{yy}" r="{r}" fill="{color}"/>'
    return out


# canvas: quiet dot grid (design / documentation) and a faint green tint while simulating
image("gfx/allgemein/entwurfshg.png", 48, 48, dots(48, 48, 24, "#C3CEDA", 1.15, "#F2F5F8"))
image("gfx/allgemein/simulationshg.png", 48, 48, dots(48, 48, 24, "#B6D3C3", 1.15, "#EEF5F1"))
# dark mode: the same grid on deep slate (design) and with a faint green tint (simulation)
image("gfx/allgemein/entwurfshg_dunkel.png", 48, 48, dots(48, 48, 24, "#2A3A4A", 1.15, "#0F1720"))
image("gfx/allgemein/simulationshg_dunkel.png", 48, 48, dots(48, 48, 24, "#23443A", 1.15, "#0D1A17"))

# plain panel surfaces
for name in ("konfigPanel_hg", "eigenschaftspanel_hg", "menue_hg", "leisten_hg"):
    image(f"gfx/allgemein/{name}.png", 64, 64, '<rect width="64" height="64" fill="#FFFFFF"/>')

BRAND_SMALL = '''
<g stroke="#fff" stroke-width="4" stroke-linecap="round" fill="none" opacity=".95"><path d="M18 42 L32 22 L46 42 Z"/></g>
<g fill="#fff"><circle cx="32" cy="21" r="7"/><circle cx="18" cy="43" r="7"/><circle cx="46" cy="43" r="7"/></g>
<circle cx="46" cy="43" r="3.2" fill="#1FD173"/>'''

GRAD = ('<linearGradient id="brandbg" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#4B8BF5"/>'
        '<stop offset="1" stop-color="#1E4FAE"/></linearGradient>'
        '<radialGradient id="shadow"><stop offset="0" stop-color="#1C2B3A" stop-opacity=".30"/>'
        '<stop offset="1" stop-color="#1C2B3A" stop-opacity="0"/></radialGradient>'
        '<linearGradient id="screen" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#4B8BF5"/>'
        '<stop offset=".55" stop-color="#2E6BD8"/><stop offset="1" stop-color="#1E4FAE"/></linearGradient>')


def logo_uri(size):
    """the logo of Filius (RBZ Wirtschaft Kiel Version) as a data URI for the SVG"""
    import base64
    import io
    import os
    from PIL import Image
    im = Image.open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'filius-logo.png')).convert('RGBA')
    im = im.resize((size, size), Image.LANCZOS)
    buf = io.BytesIO()
    im.save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


SUBTITLE = {'de': 'Lernsoftware zu Rechnernetzen', 'en': 'Learning software for computer networks'}


# colours of the packet effects on the canvas (JCablePanel, light mode)
PATCH_BLUE = "#2E6BD8"
GLUT = "#5C95F2"
CABLE = "#5B6B7C"
CABLE_WIRELESS = "#9AA8B6"


def _quad(p0, c, p1, n=80):
    """points of a quadratic curve at even distances (like the cable on the canvas)"""
    import math
    pts = []
    for i in range(n + 1):
        t = i / n
        pts.append(((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * c[0] + t * t * p1[0],
                    (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * c[1] + t * t * p1[1]))
    acc = [0.0]
    for a, b in zip(pts, pts[1:]):
        acc.append(acc[-1] + math.hypot(b[0] - a[0], b[1] - a[1]))
    total = acc[-1]

    def at(f):
        d = f * total
        for i in range(1, len(acc)):
            if acc[i] >= d:
                k = (d - acc[i - 1]) / max(1e-9, acc[i] - acc[i - 1])
                return (pts[i - 1][0] + k * (pts[i][0] - pts[i - 1][0]), pts[i - 1][1] + k * (pts[i][1] - pts[i - 1][1]))
        return pts[-1]
    return at


def _teil(at, f0, f1, n=40):
    pts = [at(f0 + (f1 - f0) * i / n) for i in range(n + 1)]
    return "M" + " L".join(f"{x:.2f} {y:.2f}" for x, y in pts)


def splash_net(s, uid):
    """the network of the splash screen with the packet effects of the canvas: radio waves from the switch to the
    laptop with the signal point at the laptop, a cable filling with blue from the switch towards the router (with
    its shining edge), and the end of a packet that flows on into the server"""
    cy = 282 * s
    c = {"laptop": (72 * s, cy), "switch": (212 * s, cy + 2 * s), "router": (346 * s, cy), "server": (456 * s, cy - 4 * s)}
    out = f'<defs><radialGradient id="kopf{uid}"><stop offset="0" stop-color="#fff" stop-opacity=".9"/>' \
          f'<stop offset=".35" stop-color="{GLUT}" stop-opacity=".7"/><stop offset="1" stop-color="{PATCH_BLUE}" stop-opacity="0"/></radialGradient>' \
          f'<radialGradient id="signal{uid}"><stop offset="0" stop-color="{GLUT}" stop-opacity=".8"/>' \
          f'<stop offset="1" stop-color="{PATCH_BLUE}" stop-opacity="0"/></radialGradient></defs>'

    def kurve(a, b, bogen):
        p0, p1 = c[a], c[b]
        ctrl = ((p0[0] + p1[0]) / 2, (p0[1] + p1[1]) / 2 - bogen * s)
        return p0, ctrl, p1

    # Wi-Fi: dashed link, as on the canvas
    p0, ctrl, p1 = kurve("switch", "laptop", 14)
    out += (f'<path d="M{p0[0]} {p0[1]} Q {ctrl[0]} {ctrl[1]} {p1[0]} {p1[1]}" stroke="{CABLE_WIRELESS}" stroke-width="{2.2 * s}" '
            f'fill="none" stroke-linecap="round" stroke-dasharray="{6 * s} {7 * s}"/>')
    # cables
    kabel = {}
    for a, b, bogen in (("switch", "router", 26), ("router", "server", 24)):
        p0, ctrl, p1 = kurve(a, b, bogen)
        kabel[(a, b)] = _quad(p0, ctrl, p1)
        out += (f'<path d="M{p0[0]} {p0[1]} Q {ctrl[0]} {ctrl[1]} {p1[0]} {p1[1]}" stroke="{CABLE}" stroke-width="{2.4 * s}" '
                f'fill="none" stroke-linecap="round"/>')

    def fuellung(at, hinten, vorne, kopf):
        d = _teil(at, hinten, vorne)
        t = (f'<path d="{d}" stroke="{GLUT}" stroke-opacity=".22" stroke-width="{11 * s}" fill="none" stroke-linecap="round" stroke-linejoin="round"/>'
             f'<path d="{d}" stroke="{PATCH_BLUE}" stroke-width="{4.4 * s}" fill="none" stroke-linecap="round" stroke-linejoin="round"/>')
        if kopf:
            x, y = at(vorne)
            t += f'<circle cx="{x:.2f}" cy="{y:.2f}" r="{9 * s}" fill="url(#kopf{uid})"/>'
        return t
    # a packet on its way from the switch to the router: the blue has filled the cable up to its shining edge
    out += fuellung(kabel[("switch", "router")], 0.0, 0.62, True)
    # the end of the packet before: the cable empties towards the server
    out += fuellung(kabel[("router", "server")], 0.38, 1.0, False)

    out += (device("laptop", c["laptop"][0] - 41 * 0.9 * s, c["laptop"][1] - 31 * 0.9 * s, 0.9 * s)
            + device("switch", c["switch"][0] - 40 * 0.85 * s, c["switch"][1] - 22 * 0.85 * s, 0.85 * s)
            + device("router", c["router"][0] - 39 * 0.85 * s, c["router"][1] - 28 * 0.85 * s, 0.85 * s)
            + device("server", c["server"][0] - 38 * 0.8 * s, c["server"][1] - 36 * 0.8 * s, 0.8 * s))

    # radio waves from the switch towards the laptop (three waves, the outer ones fading)
    import math
    sx, sy = c["switch"]
    lx, ly = c["laptop"]
    richtung = math.atan2(ly - sy, lx - sx)
    for q in (0.08, 0.38, 0.68):
        r = (36 + 58 * q) * s
        a0, a1 = richtung - math.radians(35.5), richtung + math.radians(35.5)
        x0, y0 = sx + r * math.cos(a0), sy + r * math.sin(a0)
        x1, y1 = sx + r * math.cos(a1), sy + r * math.sin(a1)
        out += (f'<path d="M{x0:.2f} {y0:.2f} A {r:.2f} {r:.2f} 0 0 1 {x1:.2f} {y1:.2f}" stroke="{PATCH_BLUE}" '
                f'stroke-opacity="{(1 - q) * 0.95:.2f}" stroke-width="{(3.4 - 1.2 * q) * s:.2f}" fill="none" stroke-linecap="round"/>')
    # the signal point at the laptop: the packet has arrived
    px, py = lx + 47 * s, ly - 1 * s
    out += (f'<circle cx="{px}" cy="{py}" r="{17 * s}" fill="url(#signal{uid})"/>'
            f'<circle cx="{px}" cy="{py}" r="{4.8 * s}" fill="{PATCH_BLUE}"/>'
            f'<circle cx="{px - 1.2 * s}" cy="{py - 1.2 * s}" r="{1.4 * s}" fill="#fff" fill-opacity=".9"/>')
    return out


def splash(w, h, scale, lang='de'):
    s = scale
    uid = f"{lang}{int(w)}"
    return (f'<defs>{GRAD}<linearGradient id="sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#FFFFFF"/>'
            f'<stop offset="1" stop-color="#EEF3F8"/></linearGradient></defs>'
            f'<rect width="{w}" height="{h}" fill="url(#sky)"/>'
            + dots(w, h, int(24 * s), "#DCE4EC", 1.1 * s, "none").replace('<rect width', '<rect fill-opacity="0" width', 1)
            + f'<image x="{26*s}" y="{22*s}" width="{128*s}" height="{128*s}" href="{logo_uri(384)}"/>'
            f'<text x="{166*s}" y="{88*s}" font-family="Atkinson Hyperlegible" font-weight="700" font-size="{48*s}" fill="{INK}">Filius</text>'
            f'<text x="{168*s}" y="{115*s}" font-family="Atkinson Hyperlegible" font-size="{16*s}" fill="{INK2}">{SUBTITLE[lang]}</text>'
            f'<rect x="{166*s}" y="{132*s}" width="{204*s}" height="{26*s}" rx="{13*s}" fill="#EAF1FC"/>'
            f'<circle cx="{182*s}" cy="{145*s}" r="{4*s}" fill="{BLUE}"/>'
            f'<text x="{194*s}" y="{150*s}" font-family="Atkinson Hyperlegible" font-weight="700" font-size="{13*s}" fill="{BLUE}">RBZ Wirtschaft Kiel Version</text>'
            + splash_net(s, uid))


image("gfx/allgemein/splashscreen.png", 512, 384, splash(512, 384, 1.0), hidpi=True)
image("gfx/allgemein/splashscreen_en.png", 512, 384, splash(512, 384, 1.0, 'en'), hidpi=True)
image("gfx/allgemein/splashscreen-mini.png", 128, 96, splash(128, 96, 0.25))

# wallpaper of the simulated desktop (fallback picture; Filius paints it in code): soft blue with a calm network
# motif towards the lower right
wall_nodes = [(470, 300), (560, 250), (600, 360), (520, 420), (400, 400), (330, 330), (610, 170), (440, 210)]
wall_links = [(0, 1), (1, 2), (2, 3), (3, 4), (4, 5), (5, 0), (1, 6), (0, 7), (7, 6), (0, 3)]
links = "".join(f'<line x1="{wall_nodes[a][0]}" y1="{wall_nodes[a][1]}" x2="{wall_nodes[b][0]}" y2="{wall_nodes[b][1]}"/>'
                for a, b in wall_links)
nodes = "".join(f'<circle cx="{x}" cy="{y}" r="{5 if i % 3 else 7}"/>' for i, (x, y) in enumerate(wall_nodes))
# the soft blue gradient of the desktop (same as filius.modern.Tapete): darker at the upper left, lighter at the
# lower right, 17 stops along an S-curve
def _wall_stops():
    a, b = (0x1B, 0x37, 0x69), (0x3A, 0x70, 0xCF)
    out = ""
    for i in range(17):
        t = i / 16
        k = t * t * (3 - 2 * t)
        c = "".join(f"{round(a[j] + (b[j] - a[j]) * k):02X}" for j in range(3))
        out += f'<stop offset="{t:.4f}" stop-color="#{c}"/>'
    return out


image("gfx/desktop/hintergrundbild.png", 640, 480, f'''
<defs>
 <linearGradient id="wbg" x1="0" y1="0" x2="1" y2="1">{_wall_stops()}</linearGradient>
</defs>
<rect width="640" height="480" fill="url(#wbg)"/>
<g stroke="#9CC0FF" stroke-opacity=".28" stroke-width="1.6">{links}</g>
<g fill="#CFE0FF" fill-opacity=".55">{nodes}</g>
<circle cx="560" cy="250" r="12" fill="#1FD173" fill-opacity=".18"/><circle cx="560" cy="250" r="4.5" fill="#1FD173" fill-opacity=".9"/>
''')

# the network motif of the wallpaper alone (transparent), for the sharp desktop: JBackgroundDesktopPane paints
# the gradient in code at every window size and puts this picture unchanged into the lower right corner.
# It is the part x 300..640, y 150..455 of the picture above (as seen in the 640 x 430 desktop of Filius).
NETZ_X, NETZ_Y = 300, 150
netz_links = "".join(f'<line x1="{wall_nodes[a][0] - NETZ_X}" y1="{wall_nodes[a][1] - NETZ_Y}" '
                     f'x2="{wall_nodes[b][0] - NETZ_X}" y2="{wall_nodes[b][1] - NETZ_Y}"/>' for a, b in wall_links)
netz_nodes = "".join(f'<circle cx="{x - NETZ_X}" cy="{y - NETZ_Y}" r="{5 if i % 3 else 7}"/>'
                     for i, (x, y) in enumerate(wall_nodes))
image("gfx/desktop/hintergrund_netz.png", 340, 305, f'''
<g stroke="#9CC0FF" stroke-opacity=".28" stroke-width="1.6">{netz_links}</g>
<g fill="#CFE0FF" fill-opacity=".55">{netz_nodes}</g>
<circle cx="{560 - NETZ_X}" cy="{250 - NETZ_Y}" r="12" fill="#1FD173" fill-opacity=".18"/>
<circle cx="{560 - NETZ_X}" cy="{250 - NETZ_Y}" r="4.5" fill="#1FD173" fill-opacity=".9"/>
''', opaque=False, hidpi=True)

# background of the info dialog (white text is printed on top)
info_nodes = [(60, 560), (150, 610), (250, 570), (330, 620), (390, 540), (200, 500), (100, 470)]
info_links = [(0, 1), (1, 2), (2, 3), (3, 4), (2, 5), (5, 6), (6, 0), (5, 4)]
image("gfx/allgemein/info_hintergrund.png", 420, 660, f'''
<defs>
 <linearGradient id="ibg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#16345C"/><stop offset="1" stop-color="#0F1C2B"/></linearGradient>
 <radialGradient id="iglow" cx=".2" cy=".0" r=".7"><stop offset="0" stop-color="#4B8BF5" stop-opacity=".35"/><stop offset="1" stop-color="#4B8BF5" stop-opacity="0"/></radialGradient>
</defs>
<rect width="420" height="660" fill="url(#ibg)"/>
<rect width="420" height="660" fill="url(#iglow)"/>
''')
