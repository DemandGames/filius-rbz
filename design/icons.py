"""
Filius (RBZ Wirtschaft Kiel Version) – vector sources of all graphics.

Every entry maps a target resource path (relative to the class path, e.g. gfx/hardware/laptop.png)
to (width, height, svg_body). render.py turns them into PNG files at 1x and 2x ("@2x").

Palette (see filius.modern.Theme):
  ink #1C2B3A, ink-2 #4A5B6C, ink-3 #8796A5, line #DDE4EB
  patch blue #2E6BD8, LED green #13A35A / #1FD173, marker amber #E39B12, signal red #DB3B3B
"""
import os

INK = "#1C2B3A"
INK2 = "#4A5B6C"
INK3 = "#8796A5"
LINE = "#DDE4EB"
BLUE = "#2E6BD8"
GREEN = "#13A35A"
LED = "#1FD173"
AMBER = "#E39B12"
RED = "#DB3B3B"

ICONS = {}


def icon(path, w, h, body, defs=""):
    ICONS[path] = (w, h, f'<defs>{defs}</defs>{body}')


def shadow(cx, cy, rx, ry, opacity=0.18):
    return (f'<ellipse cx="{cx}" cy="{cy}" rx="{rx}" ry="{ry}" fill="url(#shadow)" opacity="{opacity / 0.18:.3f}"/>')


SHADOW_DEF = ('<radialGradient id="shadow"><stop offset="0" stop-color="#1C2B3A" stop-opacity=".30"/>'
              '<stop offset="1" stop-color="#1C2B3A" stop-opacity="0"/></radialGradient>')
SCREEN_DEF = ('<linearGradient id="screen" x1="0" y1="0" x2="1" y2="1">'
              '<stop offset="0" stop-color="#4B8BF5"/><stop offset=".55" stop-color="#2E6BD8"/>'
              '<stop offset="1" stop-color="#1E4FAE"/></linearGradient>')


# =============================================================================== hardware

# Rechner – monitor and tower
icon("gfx/hardware/server.png", 76, 72, f'''
{shadow(38, 66, 34, 5)}
<rect x="47" y="10" width="22" height="54" rx="4" fill="#E9EEF3"/>
<rect x="47" y="10" width="22" height="54" rx="4" fill="none" stroke="#B9C5D1" stroke-width="1.2"/>
<rect x="51" y="16" width="14" height="3" rx="1.5" fill="#B9C5D1"/>
<rect x="51" y="22" width="14" height="3" rx="1.5" fill="#B9C5D1"/>
<circle cx="58" cy="54" r="3" fill="{LED}"/><circle cx="58" cy="54" r="5.5" fill="{LED}" opacity=".22"/>
<rect x="4" y="6" width="50" height="38" rx="5" fill="{INK}"/>
<rect x="7.5" y="9.5" width="43" height="29" rx="2.5" fill="url(#screen)"/>
<path d="M7.5 30 L50.5 16 L50.5 9.5 L7.5 9.5 Z" fill="#fff" opacity=".10"/>
<g stroke="#fff" stroke-width="1.6" stroke-linecap="round" opacity=".9" fill="none">
  <path d="M18 29 L29 20 L40 27"/></g>
<g fill="#fff"><circle cx="18" cy="29" r="2.6"/><circle cx="29" cy="20" r="2.6"/><circle cx="40" cy="27" r="2.6"/></g>
<path d="M24 44 h10 l2.5 10 h-15 z" fill="#5B6B7C"/>
<rect x="15" y="53" width="28" height="4.5" rx="2.25" fill="#33475B"/>
''', SHADOW_DEF + SCREEN_DEF)

# Notebook – opened, seen slightly from above: the deck is only a little wider than the lid (version 21)
_keys = ""
for _row, (_y, _x0, _x1) in enumerate(((44.0, 18.6, 63.4), (46.6, 17.6, 64.4), (49.2, 16.6, 65.4))):
    _step = (_x1 - _x0) / 12
    for _i in range(12):
        if _row == 2 and 4 <= _i <= 7:
            if _i == 4:  # space bar
                _keys += f'<rect x="{_x0 + 4 * _step + .35:.2f}" y="{_y}" width="{4 * _step - .7:.2f}" height="1.9" rx=".5" fill="#C3CED9"/>'
            continue
        _keys += f'<rect x="{_x0 + _i * _step + .35:.2f}" y="{_y}" width="{_step - .7:.2f}" height="1.9" rx=".5" fill="#C3CED9"/>'
icon("gfx/hardware/laptop.png", 82, 62, f'''
{shadow(41, 58, 35, 4.5)}
<rect x="16" y="3" width="50" height="37" rx="4.5" fill="{INK}"/>
<rect x="19.5" y="6.5" width="43" height="30" rx="2" fill="url(#screen)"/>
<path d="M19.5 29 L62.5 12 L62.5 6.5 L19.5 6.5 Z" fill="#fff" opacity=".10"/>
<g stroke="#fff" stroke-width="1.5" stroke-linecap="round" opacity=".9" fill="none"><path d="M30 28 L41 18 L52 26"/></g>
<g fill="#fff"><circle cx="30" cy="28" r="2.4"/><circle cx="41" cy="18" r="2.4"/><circle cx="52" cy="26" r="2.4"/></g>
<path d="M16 40.6 H66 L72 54.5 Q72.6 56.5 70 56.5 H12 Q9.4 56.5 10 54.5 Z" fill="#E1E7ED"/>
<path d="M16 40.6 H66 L72 54.5 Q72.6 56.5 70 56.5 H12 Q9.4 56.5 10 54.5 Z" fill="none" stroke="#B9C5D1" stroke-width="1.2"/>
{_keys}
<rect x="33.5" y="52" width="15" height="3" rx="1" fill="#CCD6E0"/>
''', SHADOW_DEF + SCREEN_DEF)

# shared parts of switch and router (version 21): blue top like the screens, light housing like the PC tower
TOP_SIDE_DEF = ('<linearGradient id="top" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#5B97F7"/>'
                '<stop offset=".6" stop-color="#2E6BD8"/><stop offset="1" stop-color="#1E4FAE"/></linearGradient>'
                '<linearGradient id="side" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#DCE3EA"/>'
                '<stop offset=".45" stop-color="#F3F6F9"/><stop offset="1" stop-color="#D3DCE5"/></linearGradient>')


def _led(x, y, on=True, r=1.5):
    if not on:
        return f'<circle cx="{x}" cy="{y}" r="{r}" fill="#C3CED9"/>'
    return f'<circle cx="{x}" cy="{y}" r="{r * 2.4:.1f}" fill="{LED}" opacity=".22"/><circle cx="{x}" cy="{y}" r="{r}" fill="{LED}"/>'


def _ports(x0, y, n, pitch=7.8, w=5.6, h=5.4):
    s = ""
    for i in range(n):
        x = x0 + i * pitch
        s += (f'<rect x="{x:.1f}" y="{y}" width="{w}" height="{h}" rx="1" fill="#33475B"/>'
              f'<rect x="{x + 1.6:.1f}" y="{y + h - 1.8:.1f}" width="{w - 3.2:.1f}" height="1.8" fill="#1C2B3A"/>')
    return s


def _squash(inner, cx, cy, sy):
    """a flat glyph lying on a top face seen slightly from above"""
    return f'<g transform="translate({cx} {cy}) scale(1 {sy}) translate({-cx} {-cy})">{inner}</g>'


def _arrows_switch(cx, cy, s=1.0, color="#fff", sw=2.0):
    """network-plan switch symbol: an arrow to the right above an arrow to the left"""
    def a(y, right):
        x1, x2 = (cx - 9 * s, cx + 9 * s) if right else (cx + 9 * s, cx - 9 * s)
        d = 1 if right else -1
        return (f'<path d="M{x1:.1f} {y:.1f} H{x2:.1f}"/>'
                f'<path d="M{x2 - d * 3.6 * s:.1f} {y - 3.2 * s:.1f} L{x2:.1f} {y:.1f} L{x2 - d * 3.6 * s:.1f} {y + 3.2 * s:.1f}"/>')
    return (f'<g stroke="{color}" stroke-width="{sw}" stroke-linecap="round" stroke-linejoin="round" fill="none">'
            + a(cy - 3.6 * s, True) + a(cy + 3.6 * s, False) + '</g>')


def _arrows_router(cx, cy, s=1.0, color="#fff", sw=2.2):
    """network-plan router symbol: four diagonal arrows, two out and two in"""
    def seg(x1, y1, x2, y2, h1, h2):
        return (f'<path d="M{cx + x1 * s:.1f} {cy + y1 * s:.1f} L{cx + x2 * s:.1f} {cy + y2 * s:.1f}"/>'
                f'<path d="M{cx + (x2 + h1[0]) * s:.1f} {cy + (y2 + h1[1]) * s:.1f} L{cx + x2 * s:.1f} {cy + y2 * s:.1f} '
                f'L{cx + (x2 + h2[0]) * s:.1f} {cy + (y2 + h2[1]) * s:.1f}"/>')
    g = (seg(-3, -3, -8.5, -8.5, (0, 4), (4, 0)) + seg(3, 3, 8.5, 8.5, (0, -4), (-4, 0))
         + seg(8.5, -8.5, 3, -3, (0, -4), (4, 0)) + seg(-8.5, 8.5, -3, 3, (0, 4), (-4, 0)))
    return f'<g stroke="{color}" stroke-width="{sw}" stroke-linecap="round" stroke-linejoin="round" fill="none">{g}</g>'


# Switch / WLAN – flat box seen slightly from above: blue top with the switch arrows, light front with
# status LEDs, four ports and the WLAN sign (in Filius the switch is also the access point) (version 21)
icon("gfx/hardware/switch.png", 80, 44, f'''<g transform="translate(0 -4)">
{shadow(40, 43, 37, 4.5)}
<path d="M4 22 H76 V36 Q76 40 72 40 H8 Q4 40 4 36 Z" fill="url(#side)" stroke="#B9C5D1" stroke-width="1.2" stroke-linejoin="round"/>
<path d="M13 8 H67 Q70 8 70.8 10 L76 22 H4 L9.2 10 Q10 8 13 8 Z" fill="url(#top)" stroke="#1E4FAE" stroke-width="1.1" stroke-linejoin="round"/>
<path d="M13 10 H67 L69 13 Q40 11 11 13 Z" fill="#fff" opacity=".16"/>
{_squash(_arrows_switch(40, 15, 1.05, sw=2.3), 40, 15, .6)}
{_led(11, 31)}{_led(16.5, 31)}
{_ports(23, 28, 4)}
<g stroke="{BLUE}" stroke-width="1.9" stroke-linecap="round" fill="none">
  <path d="M60.4 30.4 a6 6 0 0 1 8.6 0"/><path d="M62.4 32.7 a3.2 3.2 0 0 1 4.6 0"/></g>
<circle cx="64.7" cy="35" r="1.3" fill="{BLUE}"/>
</g>''', SHADOW_DEF + TOP_SIDE_DEF)

# Vermittlungsrechner (router) – the network-plan router: a flat cylinder, blue top with the four arrows,
# light side with status LEDs (version 21)
icon("gfx/hardware/router.png", 78, 56, f'''
{shadow(39, 50, 35, 5.5)}
<path d="M5 22 V34 A34 11 0 0 0 73 34 V22 Z" fill="url(#side)"/>
<path d="M5 22 V34 A34 11 0 0 0 73 34 V22" fill="none" stroke="#B9C5D1" stroke-width="1.2"/>
<ellipse cx="39" cy="22" rx="34" ry="11" fill="url(#top)"/>
<ellipse cx="39" cy="22" rx="34" ry="11" fill="none" stroke="#1E4FAE" stroke-width="1"/>
<path d="M10 19 Q39 9 68 19 Q39 13 10 19 Z" fill="#fff" opacity=".12"/>
{_squash(_arrows_router(39, 22, 1.25, sw=2.6), 39, 22, .55)}
{_led(16, 37)}{_led(22, 38.6)}<circle cx="56" cy="38.6" r="1.5" fill="{AMBER}"/>{_led(62, 37)}
''', SHADOW_DEF + TOP_SIDE_DEF)

# Heimrouter (gateway) – white home router with antennas
icon("gfx/hardware/gateway.png", 78, 62, f'''
{shadow(39, 57, 35, 5)}
<rect x="14" y="3" width="4" height="26" rx="2" fill="#33475B"/>
<rect x="60" y="3" width="4" height="26" rx="2" fill="#33475B"/>
<path d="M6 32 Q6 26 12 26 H66 Q72 26 72 32 V44 Q72 51 65 51 H13 Q6 51 6 44 Z" fill="#F4F7FA"/>
<path d="M6 32 Q6 26 12 26 H66 Q72 26 72 32 V44 Q72 51 65 51 H13 Q6 51 6 44 Z" fill="none" stroke="#B9C5D1" stroke-width="1.3"/>
<path d="M7 41 H71" stroke="#DDE4EB" stroke-width="1.2"/>
<circle cx="18" cy="35" r="2" fill="{LED}"/><circle cx="18" cy="35" r="4" fill="{LED}" opacity=".2"/>
<circle cx="26" cy="35" r="2" fill="{LED}"/>
<circle cx="34" cy="35" r="2" fill="{AMBER}"/>
<g stroke="{BLUE}" stroke-width="2" stroke-linecap="round" fill="none">
  <path d="M51 37 a6 6 0 0 1 12 0"/><path d="M54 37 a3 3 0 0 1 6 0"/></g>
<circle cx="57" cy="37" r="1.4" fill="{BLUE}"/>
''', SHADOW_DEF)
ICONS["gfx/hardware/gatewayE.png"] = ICONS["gfx/hardware/gateway.png"]

# Modem – compact box with signal waves
icon("gfx/hardware/modem.png", 76, 54, f'''
{shadow(38, 49, 33, 5)}
<rect x="8" y="16" width="62" height="28" rx="7" fill="#2A3C4F"/>
<rect x="8" y="16" width="62" height="9" rx="6" fill="#3A5068"/>
<rect x="8" y="21" width="62" height="4" fill="#3A5068"/>
<g stroke="{AMBER}" stroke-width="2.2" stroke-linecap="round" fill="none">
  <path d="M30 35 q4 -7 8 0 t8 0"/></g>
<circle cx="18" cy="37" r="1.8" fill="{LED}"/><circle cx="18" cy="37" r="3.8" fill="{LED}" opacity=".22"/>
<circle cx="60" cy="37" r="1.8" fill="{AMBER}"/>
''', SHADOW_DEF)

# Cloud (switch shown as cloud)
icon("gfx/hardware/cloud.png", 78, 54, f'''
{shadow(39, 50, 33, 4)}
<path d="M20 44 Q6 44 6 33 Q6 23 17 22 Q19 9 33 9 Q43 9 47 17 Q51 14 56 15 Q66 17 66 27 Q73 29 73 36 Q73 44 63 44 Z"
 fill="url(#cloud)" stroke="#AFC0D2" stroke-width="1.4"/>
<path d="M17 22 Q19 9 33 9 Q43 9 47 17" fill="none" stroke="#fff" stroke-width="2" opacity=".8" stroke-linecap="round"/>
''', SHADOW_DEF + '<linearGradient id="cloud" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#FFFFFF"/>'
     '<stop offset="1" stop-color="#DCE6F0"/></linearGradient>')

# Kabel – thin patch cable in patch blue with a loop and two RJ-45 plugs (clear body, gold contacts, latch,
# blue boot) (version 21)
PLUG_DEF = ('<linearGradient id="boot" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#1E4FAE"/>'
            '<stop offset=".5" stop-color="#3F7FE6"/><stop offset="1" stop-color="#1E4FAE"/></linearGradient>'
            '<linearGradient id="clear" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#E3EAF1"/>'
            '<stop offset=".45" stop-color="#FBFCFE"/><stop offset="1" stop-color="#D9E2EB"/></linearGradient>')


def _rj45(x, y, rot=0, s=1.0):
    """RJ-45 plug: the cable ends at (x, y), the tip points in direction rot (0 = up)"""
    contacts = "".join(f'<path d="M{-3.85 + i * 1.1:.2f} -16.2 V-13.4"/>' for i in range(8))
    return (f'<g transform="translate({x} {y}) rotate({rot}) scale({s})">'
            '<path d="M-2.4 0 H2.4 L4.1 -5.6 H-4.1 Z" fill="url(#boot)"/>'
            '<path d="M-4.1 -5.6 H4.1" stroke="#163E8A" stroke-width=".6"/>'
            '<rect x="-5.2" y="-17" width="10.4" height="11.6" rx="1.4" fill="url(#clear)" stroke="#9FB2C5" stroke-width=".9"/>'
            f'<g stroke="#D9A21E" stroke-width=".62">{contacts}</g>'
            '<rect x="-2.1" y="-11.6" width="4.2" height="6.4" rx=".8" fill="#CDD7E1" stroke="#9FB2C5" stroke-width=".6"/>'
            '</g>')


_KABEL = "M15 27 C 10 10, 40 4, 46 16 C 52 28, 24 34, 28 22 C 31 13, 62 10, 63 26"
icon("gfx/hardware/kabel.png", 78, 52, f'''
{shadow(39, 48, 33, 4)}
<path d="{_KABEL}" stroke="#1E4FAE" stroke-width="3.8" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
<path d="{_KABEL}" stroke="{BLUE}" stroke-width="2.4" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
<path d="{_KABEL}" stroke="#8DB3F3" stroke-width=".8" fill="none" stroke-linecap="round" opacity=".8" transform="translate(-.4 -.5)"/>
{_rj45(15, 27, 180)}{_rj45(63, 26, 180)}
''', SHADOW_DEF + PLUG_DEF)

# RJ45 plug (network card in connection dialog)
icon("gfx/hardware/rj45.png", 32, 26, f'''
<rect x="6" y="2" width="20" height="18" rx="3" fill="#E9EEF3" stroke="#AFC0D2" stroke-width="1.2"/>
<rect x="9" y="14" width="14" height="9" rx="1.5" fill="#C9D3DD" stroke="#AFC0D2" stroke-width="1"/>
<g stroke="{AMBER}" stroke-width="1.1"><path d="M11.5 16v5M14.5 16v5M17.5 16v5M20.5 16v5"/></g>
<rect x="12" y="5" width="8" height="5" rx="1" fill="{BLUE}"/>
''')

# cable cursor: plug 1 and plug 2 while connecting two devices (thin cable and RJ-45 plug like the cable picture)
for n in (1, 2):
    icon(f"gfx/allgemein/ziel{n}.png", 44, 33, f'''<defs>{PLUG_DEF}</defs>
<path d="M2 26 C 8 26, 9 14, 16 14" stroke="#1E4FAE" stroke-width="3.6" fill="none" stroke-linecap="round"/>
<path d="M2 26 C 8 26, 9 14, 16 14" stroke="{BLUE}" stroke-width="2.3" fill="none" stroke-linecap="round"/>
{_rj45(16, 14, 90, 1.0)}
<circle cx="35" cy="24" r="8" fill="{BLUE}" stroke="#fff" stroke-width="1.2"/>
<text x="35" y="28.3" text-anchor="middle" font-family="Atkinson Hyperlegible, sans-serif" font-weight="700" font-size="12" fill="#fff">{n}</text>
''')

# port status icons used as tab icons (router / home router configuration)
icon("gfx/allgemein/conn_ok.png", 40, 29, f'''
<rect x="9" y="5" width="22" height="19" rx="4" fill="#E9F7EF" stroke="{GREEN}" stroke-width="1.5"/>
<rect x="14" y="10" width="12" height="9" rx="1.5" fill="{GREEN}"/>
<g stroke="#E9F7EF" stroke-width="1.2"><path d="M16.5 11v3.5M19 11v3.5M21.5 11v3.5M24 11v3.5"/></g>
''')
icon("gfx/allgemein/conn_fail.png", 32, 26, f'''
<rect x="5" y="4" width="22" height="18" rx="4" fill="#F4F6F8" stroke="#AFC0D2" stroke-width="1.5"/>
<rect x="10" y="9" width="12" height="8" rx="1.5" fill="#C9D3DD"/>
<circle cx="25" cy="20" r="5.5" fill="{RED}"/>
<path d="M22.8 17.8 L27.2 22.2 M27.2 17.8 L22.8 22.2" stroke="#fff" stroke-width="1.6" stroke-linecap="round"/>
''')

# =============================================================================== header icons

def stroke_icon(path, size, inner, color=INK2, sw=1.8):
    icon(path, size, size, f'<g fill="none" stroke="{color}" stroke-width="{sw}" stroke-linecap="round" '
                           f'stroke-linejoin="round">{inner}</g>')


S = 22
stroke_icon("gfx/modern/tb-new.png", S, '<path d="M6 3h7l4 4v12H6z"/><path d="M13 3v4h4"/><path d="M11.5 10.5v5M9 13h5"/>')
stroke_icon("gfx/modern/tb-open.png", S, '<path d="M3 7V5.5A1.5 1.5 0 0 1 4.5 4H9l2 2h6.5A1.5 1.5 0 0 1 19 7.5V9"/>'
                                         '<path d="M3 7v10.5h13.8l2.7-7.8H5.7L3 17.5"/>')
stroke_icon("gfx/modern/tb-save.png", S, '<path d="M4 5.5A1.5 1.5 0 0 1 5.5 4h9.5l3 3v9.5a1.5 1.5 0 0 1-1.5 1.5h-11A1.5 1.5 0 0 1 4 16.5z"/>'
                                         '<path d="M7.5 4v4h6V4"/><rect x="7" y="12" width="8" height="6" rx="1"/>')
stroke_icon("gfx/modern/tb-wizard.png", S, '<path d="M4 18L14 8"/><path d="M12.5 6.5l3 3"/>'
                                           '<path d="M16.5 2.8v2.4M15.3 4h2.4"/><path d="M19 9.3v2M18 10.3h2"/><path d="M8.8 3.6v1.8M7.9 4.5h1.8"/>')
stroke_icon("gfx/modern/tb-help.png", S, '<circle cx="11" cy="11" r="8"/><path d="M8.6 8.8a2.5 2.5 0 0 1 4.8.9c0 1.7-2.4 2-2.4 3.6"/>'
                                         '<circle cx="11" cy="15.6" r=".4" fill="' + INK2 + '"/>')
stroke_icon("gfx/modern/tb-info.png", S, '<circle cx="11" cy="11" r="8"/><path d="M11 10v5.2"/>'
                                         '<circle cx="11" cy="7.2" r=".4" fill="' + INK2 + '"/>')
stroke_icon("gfx/modern/tb-settings.png", S,
            '<circle cx="11" cy="11" r="2.7"/>'
            '<path d="M11 2.8l1.4 2.2 2.5-.7.5 2.6 2.5.8-.9 2.4 1.9 1.8-1.9 1.8.9 2.4-2.5.8-.5 2.6-2.5-.7L11 19.2l-1.4-2.2-2.5.7-.5-2.6-2.5-.8.9-2.4L3.1 11l1.9-1.8-.9-2.4 2.5-.8.5-2.6 2.5.7z"/>',
            sw=1.6)

# mode icons: open-end wrench = build (drawn upright, turned 45 degrees), play = run, pencil = document
M = 18
for suffix, col in (("", INK2), ("-on", "#FFFFFF")):
    stroke_icon(f"gfx/modern/mode-design{suffix}.png", M,
                '<path transform="rotate(45 9 9)" d="M7.30 0.84L7.30 4.14A1.7 1.7 0 0 0 10.70 4.14L10.70 0.84A4.4 4.4 0 0 1 10.45 9.05L10.45 15.60A1.45 1.45 0 0 1 7.55 15.60L7.55 9.05A4.4 4.4 0 0 1 7.30 0.84Z"/>',
                color=col, sw=1.7)
    icon(f"gfx/modern/mode-sim{suffix}.png", M, M,
         f'<path d="M5.5 3.6v10.8a.9.9 0 0 0 1.4.8l8.4-5.4a.9.9 0 0 0 0-1.6L6.9 2.8a.9.9 0 0 0-1.4.8z" fill="{col}"/>')
    stroke_icon(f"gfx/modern/mode-doc{suffix}.png", M,
                '<path d="M3 15l.8-3.3L12.2 3.3a1.6 1.6 0 0 1 2.3 0l.2.2a1.6 1.6 0 0 1 0 2.3L6.3 14.2z"/><path d="M10.8 4.7l2.5 2.5"/>',
                color=col, sw=1.7)

# former brand mark: three linked nodes on patch blue (no longer rendered)
BRAND = f'''
<rect x="0" y="0" width="64" height="64" rx="16" fill="url(#brandbg)"/>
<g stroke="#fff" stroke-width="4" stroke-linecap="round" fill="none" opacity=".95">
  <path d="M18 42 L32 22 L46 42 Z"/></g>
<g fill="#fff"><circle cx="32" cy="21" r="7"/><circle cx="18" cy="43" r="7"/><circle cx="46" cy="43" r="7"/></g>
<circle cx="46" cy="43" r="3.2" fill="{LED}"/>
'''
BRAND_DEF = (f'<linearGradient id="brandbg" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#4B8BF5"/>'
             f'<stop offset="1" stop-color="#1E4FAE"/></linearGradient>')
# the header mark (gfx/modern/brand.png) and the window icons (gfx/modern/app/) are made from the logo
# of Filius (RBZ Wirtschaft Kiel Version) by logo.py

# =============================================================================== desktop apps

APP_DEF = ''.join(
    f'<linearGradient id="g{name}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="{a}"/><stop offset="1" stop-color="{b}"/></linearGradient>'
    for name, a, b in [
        ("green", "#2BC274", "#0E8C4C"), ("blue", "#4B8BF5", "#1E4FAE"), ("ink", "#33475B", "#16222E"),
        ("amber", "#F5B43A", "#D3860B"), ("teal", "#25C2B4", "#0B8A87"), ("slate", "#7A8DA1", "#4A5B6C"),
        ("purple", "#9277F7", "#5B3FD4"), ("navy", "#3B6FC2", "#183D7A"), ("orange", "#F08A3C", "#C45A12"),
        ("red", "#F06464", "#BE2B2B"), ("pink", "#F06B9B", "#C93370"), ("indigo", "#6D78F2", "#3D44C4"),
        ("cyan", "#38C6DA", "#118FA3"), ("deepcyan", "#2AA5B5", "#0B6A78"),
        ("emerald", "#2CC392", "#0D8A62"), ("forest", "#1E9C74", "#0B5E45")])


def app(path, color, glyph, w=64, h=64):
    body = (f'<rect x="4" y="5" width="56" height="56" rx="15" fill="#0B1620" opacity=".22"/>'
            f'<rect x="4" y="3" width="56" height="56" rx="15" fill="url(#g{color})"/>'
            f'<rect x="4.6" y="3.6" width="54.8" height="54.8" rx="14.4" fill="none" stroke="#fff" stroke-opacity=".18" stroke-width="1.2"/>'
            f'<g fill="none" stroke="#fff" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round">{glyph}</g>')
    icon(path, w, h, body, APP_DEF)


app("gfx/desktop/icon_softwareinstallation.png", "green",
    '<path d="M18 25l14-7 14 7v16l-14 7-14-7z"/><path d="M18 25l14 7 14-7"/><path d="M32 32v16"/>')
app("gfx/desktop/icon_browser.png", "blue",
    '<circle cx="32" cy="31" r="15"/><path d="M17 31h30"/><path d="M32 16c-5 4-7 9-7 15s2 11 7 15"/><path d="M32 16c5 4 7 9 7 15s-2 11-7 15"/>')
app("gfx/desktop/icon_terminal.png", "ink",
    '<path d="M19 23l9 8-9 8"/><path d="M32 41h13"/>')
app("gfx/desktop/icon_emailprogramm.png", "amber",
    '<rect x="16" y="20" width="32" height="23" rx="3.5"/><path d="M17 22l15 11 15-11"/>')
app("gfx/desktop/icon_filebrowser.png", "teal",
    '<path d="M16 24v-3a2.5 2.5 0 0 1 2.5-2.5h8l3 3.5h16a2.5 2.5 0 0 1 2.5 2.5V40a2.5 2.5 0 0 1-2.5 2.5h-27A2.5 2.5 0 0 1 16 40z"/><path d="M16 27h32"/>')
app("gfx/desktop/icon_texteditor.png", "slate",
    '<path d="M21 16h15l7 7v22H21z"/><path d="M36 16v7h7"/><path d="M26 30h12M26 36h12M26 42h7"/>')
app("gfx/desktop/icon_dns.png", "purple",
    '<path d="M18 43h28" stroke-width="2.6"/>'
    '<text x="32" y="37" text-anchor="middle" font-family="Atkinson Hyperlegible" font-weight="700" font-size="17" '
    'fill="#fff" stroke="none" letter-spacing=".5">DNS</text>'
    '<circle cx="32" cy="17" r="4.5" stroke-width="2.4"/>')
app("gfx/desktop/icon_webserver.png", "navy",
    '<rect x="16" y="17" width="24" height="11" rx="2.5"/><rect x="16" y="31" width="24" height="11" rx="2.5"/>'
    '<path d="M21 22.5h1M21 36.5h1"/><circle cx="42" cy="40" r="9" fill="#183D7A"/><circle cx="42" cy="40" r="9"/>'
    '<path d="M33 40h18M42 31c-2.5 2.5-3.5 5.5-3.5 9s1 6.5 3.5 9M42 31c2.5 2.5 3.5 5.5 3.5 9s-1 6.5-3.5 9" stroke-width="2.4"/>')
app("gfx/desktop/icon_emailserver.png", "orange",
    '<rect x="16" y="17" width="24" height="11" rx="2.5"/><rect x="16" y="31" width="24" height="11" rx="2.5"/>'
    '<path d="M21 22.5h1M21 36.5h1"/><rect x="33" y="35" width="18" height="13" rx="2.5" fill="#C45A12"/>'
    '<rect x="33" y="35" width="18" height="13" rx="2.5"/><path d="M34 37l8 6 8-6" stroke-width="2.4"/>')
app("gfx/desktop/icon_firewall.png", "red",
    '<path d="M32 15l14 5v10c0 9-6 15-14 18-8-3-14-9-14-18V20z"/><path d="M18.5 27h27M20 35h24M32 20v7M25 27v8M39 27v8M32 35v8"/>')
app("gfx/desktop/icon_imageviewer.png", "pink",
    '<rect x="16" y="18" width="32" height="26" rx="3.5"/><circle cx="25" cy="26" r="3"/><path d="M17 41l10-9 7 6 5-4 9 8"/>')
app("gfx/desktop/icon_peertopeer.png", "indigo",
    '<circle cx="32" cy="19" r="5"/><circle cx="19" cy="41" r="5"/><circle cx="45" cy="41" r="5"/>'
    '<path d="M29.5 23.5l-8 13M34.5 23.5l8 13M24 41h16"/>')
app("gfx/desktop/icon_clientbaustein.png", "cyan",
    '<path d="M18 22h8a4 4 0 1 1 8 0h8v8a4 4 0 1 1 0 8v8H18z"/><path d="M24 32h7M28 29l3 3-3 3" stroke-width="2.6"/>')
app("gfx/desktop/icon_serverbaustein.png", "deepcyan",
    '<path d="M18 22h8a4 4 0 1 1 8 0h8v8a4 4 0 1 1 0 8v8H18z"/><rect x="23" y="29" width="11" height="9" rx="1.5" stroke-width="2.4"/>')
# VPN (Filius RBZ Wirtschaft Kiel Version): client = a tunnel with a lock, server = server with a lock badge
app("gfx/desktop/icon_vpnclient.png", "emerald",
    '<path d="M15 46V33a17 17 0 0 1 34 0v13"/><path d="M15 46h34" stroke-width="2.6"/>'
    '<rect x="25.5" y="33" width="13" height="10.5" rx="2.2"/><path d="M28.5 33v-3.2a3.5 3.5 0 0 1 7 0V33"/>')
app("gfx/desktop/icon_vpnserver.png", "forest",
    '<rect x="16" y="17" width="24" height="11" rx="2.5"/><rect x="16" y="31" width="24" height="11" rx="2.5"/>'
    '<path d="M21 22.5h1M21 36.5h1"/><circle cx="42" cy="40" r="9.5" fill="#0B5E45"/><circle cx="42" cy="40" r="9.5"/>'
    '<rect x="37.2" y="39.3" width="9.6" height="7.2" rx="1.6" stroke-width="2.2"/>'
    '<path d="M39.4 39.3v-2.1a2.6 2.6 0 0 1 5.2 0v2.1" stroke-width="2.2"/>')
app("gfx/desktop/icon_fileimporter.png", "teal",
    '<path d="M27 16h11l7 7v22H27"/><path d="M38 16v7h7"/><path d="M15 33h17M26 27l6 6-6 6"/>')
app("gfx/desktop/icon_emailloeschen.png", "red",
    '<rect x="16" y="20" width="32" height="23" rx="3.5"/><path d="M17 22l15 11 15-11"/>')
app("gfx/desktop/peertopeer_netzwerk.png", "indigo",
    '<circle cx="32" cy="19" r="5"/><circle cx="19" cy="41" r="5"/><circle cx="45" cy="41" r="5"/><path d="M29.5 23.5l-8 13M34.5 23.5l8 13M24 41h16"/>')
app("gfx/desktop/peertopeer_suchen.png", "indigo", '<circle cx="29" cy="28" r="10"/><path d="M36.5 35.5l9 9"/>')
app("gfx/desktop/peertopeer_dateien.png", "indigo", '<path d="M22 16h14l7 7v22H22z"/><path d="M36 16v7h7"/>')

# taskbar network status (light on the dark task bar)
icon("gfx/desktop/netzwek_aus.png", 18, 18, '<g fill="none" stroke="#8796A5" stroke-width="1.6" stroke-linecap="round">'
     '<rect x="3" y="5" width="12" height="8" rx="2"/><path d="M6 13v2M12 13v2M6 9h6"/></g>'
     f'<circle cx="14" cy="4.5" r="3.2" fill="{RED}"/><path d="M12.8 3.3l2.4 2.4M15.2 3.3l-2.4 2.4" stroke="#fff" stroke-width="1"/>')
for name in ("a", "b", "c"):
    icon(f"gfx/desktop/netzwek_{name}.png", 18, 18, '<g fill="none" stroke="#E9EEF3" stroke-width="1.6" stroke-linecap="round">'
         '<rect x="3" y="5" width="12" height="8" rx="2"/><path d="M6 13v2M12 13v2M6 9h6"/></g>'
         f'<circle cx="14.5" cy="4" r="2.6" fill="{LED}"/>')

# =============================================================================== panels and handles

# collapse / expand handles (config panel at the bottom, help at the right)
# dark mode variants (name_dunkel.png) use the dark surface, line and ink colours
HANDLE_HELL = ("#FFFFFF", "#DDE4EB", INK3)
HANDLE_DUNKEL = ("#17212C", "#2D3B4A", "#ADBCCB")


def handle_h(path, up, farben=HANDLE_HELL):
    fill, line, ink = farben
    chev = '<path d="M25.5 8.5l4-3.5 4 3.5"/>' if up else '<path d="M25.5 5l4 3.5 4-3.5"/>'
    icon(path, 59, 13, f'<path d="M0 13 Q6 13 8 7 Q10 0 17 0 H42 Q49 0 51 7 Q53 13 59 13 Z" fill="{fill}" stroke="{line}" stroke-width="1"/>'
         f'<g fill="none" stroke="{ink}" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">{chev}</g>')
    if farben is HANDLE_HELL:
        handle_h(path.replace(".png", "_dunkel.png"), up, HANDLE_DUNKEL)


handle_h("gfx/allgemein/maximieren.png", True)
handle_h("gfx/allgemein/minimieren.png", False)
handle_h("gfx/allgemein/minmieren.png", False)


def handle_v(path, left, farben=HANDLE_HELL):
    fill, line, ink = farben
    chev = '<path d="M8.5 25.5l-3.5 4 3.5 4"/>' if left else '<path d="M5 25.5l3.5 4-3.5 4"/>'
    icon(path, 13, 59, f'<path d="M13 0 Q13 6 7 8 Q0 10 0 17 V42 Q0 49 7 51 Q13 53 13 59 Z" fill="{fill}" stroke="{line}" stroke-width="1"/>'
         f'<g fill="none" stroke="{ink}" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">{chev}</g>')
    if farben is HANDLE_HELL:
        handle_v(path.replace(".png", "_dunkel.png"), left, HANDLE_DUNKEL)


handle_v("gfx/allgemein/max_to_left.png", True)
handle_v("gfx/allgemein/min_to_right.png", False)

# arrows in the software installation dialog
stroke_icon("gfx/allgemein/pfeil_links.png", 16, '<path d="M10 3L5 8l5 5"/>', color=BLUE, sw=2.2)
ICONS["gfx/allgemein/pfeil_links.png"] = (16, 24, ICONS["gfx/allgemein/pfeil_links.png"][2].replace('<g ', '<g transform="translate(0 4)" ', 1))
stroke_icon("gfx/allgemein/pfeil_rechts.png", 16, '<path d="M6 3l5 5-5 5"/>', color=BLUE, sw=2.2)
ICONS["gfx/allgemein/pfeil_rechts.png"] = (16, 24, ICONS["gfx/allgemein/pfeil_rechts.png"][2].replace('<g ', '<g transform="translate(0 4)" ', 1))

# window icon of the data exchange dialog
icon("gfx/allgemein/nachrichtenfenster_icon.png", 44, 32,
     f'<rect x="4" y="3" width="36" height="26" rx="5" fill="#fff" stroke="{BLUE}" stroke-width="2"/>'
     f'<path d="M4 10h36" stroke="{BLUE}" stroke-width="2"/><g stroke="{INK3}" stroke-width="2" stroke-linecap="round">'
     f'<path d="M10 16h24M10 21h17"/></g>')

# =============================================================================== documentation tools (40 px)

def tool(path, color, glyph):
    body = (f'<rect x="3" y="4" width="34" height="34" rx="10" fill="#0B1620" opacity=".18"/>'
            f'<rect x="3" y="2" width="34" height="34" rx="10" fill="url(#g{color})"/>'
            f'<g fill="none" stroke="#fff" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">{glyph}</g>')
    icon(path, 40, 40, body, APP_DEF)


tool("gfx/dokumentation/add_text_small.png", "amber", '<path d="M12 12h12M18 12v15"/><path d="M27 22v8M23 26h8"/>')
tool("gfx/dokumentation/add_small.png", "amber",
     '<rect x="10" y="10" width="17" height="14" rx="2.5" stroke-dasharray="3 2.6"/><path d="M27 23v8M23 27h8"/>')
tool("gfx/dokumentation/export_small.png", "slate",
     '<rect x="10" y="10" width="20" height="16" rx="2.5"/><path d="M12 24l5-5 4 3 3-2 6 4"/><path d="M20 27v5M17 30l3 3 3-3" stroke-width="2"/>')
tool("gfx/dokumentation/pdf_small.png", "red",
     '<path d="M13 9h9l5 5v15H13z"/><path d="M22 9v5h5"/>'
     '<text x="20" y="25.5" text-anchor="middle" font-family="Atkinson Hyperlegible" font-weight="700" font-size="7" '
     'fill="#fff" stroke="none">PDF</text>')

# =============================================================================== Waterwolf browser start page

def waterwolf_bild(groesse=480):
    """The Waterwolf logo (waterwolf-logo.png, transparent background) as a data URL for the SVG."""
    import base64
    import io
    from PIL import Image
    im = Image.open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "waterwolf-logo.png")).convert("RGBA")
    im = im.resize((groesse, groesse), Image.LANCZOS)
    puffer = io.BytesIO()
    im.save(puffer, "PNG")
    return "data:image/png;base64," + base64.b64encode(puffer.getvalue()).decode()


icon("gfx/desktop/browser_waterwolf_logo.png", 320, 216, f'''
<image x="16" y="46" width="124" height="124" href="{waterwolf_bild()}"/>
<text x="150" y="104" font-family="Atkinson Hyperlegible" font-weight="700" font-size="31" fill="#1C2B3A">Waterwolf</text>
<text x="151" y="132" font-family="Atkinson Hyperlegible" font-size="21" fill="#4A5B6C">Browser</text>
''')
# the start page of Waterwolf in the dark mode: the same logo with light lettering
icon("gfx/desktop/browser_waterwolf_logo_dunkel.png", 320, 216, f'''
<image x="16" y="46" width="124" height="124" href="{waterwolf_bild()}"/>
<text x="150" y="104" font-family="Atkinson Hyperlegible" font-weight="700" font-size="31" fill="#E6EDF4">Waterwolf</text>
<text x="151" y="132" font-family="Atkinson Hyperlegible" font-size="21" fill="#ADBCCB">Browser</text>
''')

# =============================================================================== e-mail program

def mail_icon(name, size, glyph, gif_too=True):
    stroke_icon(f"gfx/desktop/{name}.png", size, glyph, color=INK2, sw=2 if size >= 32 else 1.7)
    # dark mode variant: light grey strokes, brighter blue accent
    stroke_icon(f"gfx/desktop/{name}_dunkel.png", size, glyph.replace(BLUE, "#4C8DF6"), color="#ADBCCB",
                sw=2 if size >= 32 else 1.7)
    if gif_too:
        stroke_icon(f"gfx/desktop/{name}.gif", size, glyph, color=BLUE, sw=2 if size >= 32 else 1.7)
        ICONS[f"gfx/desktop/{name}.gif"] = ICONS[f"gfx/desktop/{name}.gif"] + ("nohidpi",)


mail_icon("email_emails_abholen", 32, '<path d="M5 18l3-10h16l3 10v7a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2z"/><path d="M5 18h6l1.5 3h7l1.5-3h6"/>'
          '<path d="M16 4v9M12.5 9.5L16 13l3.5-3.5" stroke="' + BLUE + '"/>')
mail_icon("email_email_verfassen", 32, '<path d="M8 4h11l5 5v6"/><path d="M8 4v24h10"/><path d="M19 4v5h5"/>'
          '<path d="M27.5 15.5l2 2-8 8-3 1 1-3z" stroke="' + BLUE + '"/>')
mail_icon("email_email_antworten", 32, '<rect x="9" y="10" width="19" height="14" rx="2.5"/><path d="M9.5 11.5l9 6.5 9-6.5"/>'
          '<path d="M10 6H6.5a3 3 0 0 0-3 3v9" stroke="' + BLUE + '"/><path d="M1 15l2.5 3 2.5-3" stroke="' + BLUE + '"/>')
mail_icon("email_email_verfassen_icon", 24, '<path d="M6 3h8l4 4v4"/><path d="M6 3v18h7"/><path d="M14 3v4h4"/>'
          '<path d="M20.5 12l1.5 1.5-6 6-2.3.8.8-2.3z" stroke="' + BLUE + '"/>', gif_too=False)
mail_icon("email_ordner_posteingang", 24, '<path d="M3 13l2.5-8h13l2.5 8v5.5a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 18.5z"/>'
          '<path d="M3 13h5l1 2h6l1-2h5"/>', gif_too=False)
mail_icon("email_ordner_gesendet", 24, '<path d="M21 3L3 10.5l7 2.5 2.5 7z"/><path d="M21 3L10 13"/>', gif_too=False)
# version 30: the folders Spam and Papierkorb, and one set of tool bar symbols (24 px, symbol above its label)
mail_icon("email_ordner_spam", 24, '<path d="M12 3l7 3v5.5c0 4.4-3 8-7 9.5-4-1.5-7-5.1-7-9.5V6z"/>'
          '<path d="M12 8.5v4.5" stroke="' + BLUE + '"/><path d="M12 16.2v.3" stroke="' + BLUE + '"/>', gif_too=False)
mail_icon("email_ordner_papierkorb", 24, '<path d="M4.5 6.5h15"/><path d="M9.5 6.5V4.5a1 1 0 0 1 1-1h3a1 1 0 0 1 1 1v2"/>'
          '<path d="M6.5 6.5l1 13a1.5 1.5 0 0 0 1.5 1.4h6a1.5 1.5 0 0 0 1.5-1.4l1-13"/>', gif_too=False)
mail_icon("email_tb_abrufen", 24, '<path d="M4 13.5l2.2-7h2.3M15.5 6.5h2.3l2.2 7v5a1.5 1.5 0 0 1-1.5 1.5H5.5A1.5 1.5 0 0 1 4 18.5v-5"/>'
          '<path d="M4 13.5h4.5l1 2h5l1-2H20"/><path d="M12 2.5v8M9.3 7.8L12 10.5l2.7-2.7" stroke="' + BLUE + '"/>', gif_too=False)
mail_icon("email_tb_neu", 24, '<path d="M6 3h8l4 4v4.5"/><path d="M6 3v18h7"/><path d="M14 3v4h4"/>'
          '<path d="M20.5 12.5l1.5 1.5-6 6-2.3.8.8-2.3z" stroke="' + BLUE + '"/>', gif_too=False)
mail_icon("email_tb_antworten", 24, '<rect x="7" y="8" width="14" height="11" rx="2"/><path d="M7.5 9l6.5 5 6.5-5"/>'
          '<path d="M8 4.5H5.2a2.2 2.2 0 0 0-2.2 2.2V13" stroke="' + BLUE + '"/><path d="M1 11l2 2.2L5 11" stroke="' + BLUE + '"/>', gif_too=False)
mail_icon("email_tb_loeschen", 24, '<path d="M4.5 6.5h15"/><path d="M9.5 6.5V4.5a1 1 0 0 1 1-1h3a1 1 0 0 1 1 1v2"/>'
          '<path d="M6.5 6.5l1 13a1.5 1.5 0 0 0 1.5 1.4h6a1.5 1.5 0 0 0 1.5-1.4l1-13"/>'
          '<path d="M10.3 10.5v6M13.7 10.5v6" stroke="' + BLUE + '"/>', gif_too=False)
mail_icon("email_tb_konto", 24, '<circle cx="11" cy="8.5" r="4"/><path d="M3.5 20.5c.6-4 3.5-6.3 7.5-6.3 2 0 3.7.6 5 1.6"/>'
          '<path d="M19 13.5v7M15.5 17h7" stroke="' + BLUE + '"/>', gif_too=False)

# welcome picture of the e-mail program (lives in img/ of the installation)
app("../../dist-files/img/email_icon.png", "amber", '<rect x="16" y="20" width="32" height="23" rx="3.5"/><path d="M17 22l15 11 15-11"/>')
ICONS["../../dist-files/img/email_icon.png"] = ICONS["../../dist-files/img/email_icon.png"] + ("nohidpi",)
