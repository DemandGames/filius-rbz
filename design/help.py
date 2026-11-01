#!/usr/bin/env python3
"""Builds the help pages of Filius (RBZ Wirtschaft Kiel Version) (hilfe/*.html) from one content definition.

The pages are shown by Filius in a Swing JEditorPane (HTML 3.2 with a small CSS subset), so the
layout uses tables for boxes and only the CSS properties Swing understands. Filius reads the
file line by line and joins the lines without a line break, therefore every paragraph is
written on a single line.

usage: python3 help.py OUTDIR      (OUTDIR/hilfe/*.html, images are expected in OUTDIR/hilfe/gfx)
"""
import html
import os
import sys

from PIL import Image

OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), '..', 'dist-files')
GFX = os.path.join(OUT, 'hilfe', 'gfx')

INK, INK2, INK3 = '#1C2B3A', '#4A5B6C', '#8796A5'
LINE, LINE_SOFT, CANVAS = '#DDE4EB', '#EAEFF4', '#F2F5F8'

# mode colours: accent (bars), chip (white text on it, contrast >= 5:1), tint (boxes)
MODES = {
    'design':        {'accent': '#2E6BD8', 'chip': '#2E6BD8', 'tint': '#EAF1FC'},
    'simulation':    {'accent': '#13A35A', 'chip': '#0E8045', 'tint': '#E6F5EC'},
    'documentation': {'accent': '#E39B12', 'chip': '#9A5C00', 'tint': '#FCF2DF'},
}

# role tags of the software installation (same colours as in the program)
TAGS = {
    'server': ('#6B3FC2', '#EFEAF8'),
    'client': ('#2A5FC4', '#E8EDF9'),
    'p2p':    ('#0A6C66', '#E3EEED'),
    'tool':   ('#4A5B6C', '#EBEDEF'),
}

FILES = {
    'design': 'design_mode_{}.html',
    'simulation': 'simulation_mode_{}.html',
    'documentation': 'documentation_mode_{}.html',
}


LANG = None  # language of the page being built


def loc(name):
    """The language-specific variant of an image (e.g. dokumodus_en_GB.png), if there is one."""
    if LANG:
        base, ext = os.path.splitext(name)
        local = f'{base}_{LANG}{ext}'
        if os.path.exists(os.path.join(GFX, local)):
            return local
    return name


def size(name):
    with Image.open(os.path.join(GFX, loc(name))) as im:
        return im.size


def img(name, extra=''):
    w, h = size(name)
    return f'<img src="hilfe/gfx/{loc(name)}" width="{w}" height="{h}" border="0"{extra}>'


def css(mode):
    m = MODES[mode]
    rules = [
        f"body {{ font-family: 'Atkinson Hyperlegible', sans-serif; font-size: 13pt; color: {INK}; "
        "margin-top: 8px; margin-left: 6px; margin-right: 6px; margin-bottom: 30px; }",
        "p { margin-top: 0px; margin-bottom: 10px; }",
        f"h1 {{ font-size: 21pt; color: {INK}; margin-top: 10px; margin-bottom: 4px; }}",
        f"h2 {{ font-size: 15pt; color: {INK}; margin-top: 22px; margin-bottom: 8px; "
        f"border-left-width: 4px; border-left-style: solid; border-left-color: {m['accent']}; padding-left: 8px; }}",
        f"p.lead {{ color: {INK2}; font-size: 13.5pt; margin-bottom: 2px; }}",
        "td.chip { color: #FFFFFF; font-size: 11pt; font-weight: bold; }",
        f"td.shot {{ border-width: 1px; border-style: solid; border-color: {LINE}; }}",
        f"p.cap {{ color: {INK3}; font-size: 11.5pt; text-align: center; margin-top: 5px; margin-bottom: 16px; }}",
        "p.gap { margin-top: 0px; margin-bottom: 12px; }",
        f"td.tip {{ color: {INK}; }}",
        "p.ic { margin-top: 2px; margin-right: 10px; margin-bottom: 0px; }",
        "p.it { margin-top: 0px; margin-bottom: 14px; }",
        "p.mi { margin-top: 0px; margin-right: 10px; margin-bottom: 10px; }",
        "p.mt { margin-top: 0px; margin-bottom: 10px; }",
        f"span.key {{ font-family: 'JetBrains Mono', monospace; font-size: 11pt; background-color: {LINE_SOFT}; }}",
        "span.name { font-weight: bold; }",
    ]
    for t, (fg, bg) in TAGS.items():
        rules.append(f"span.tg-{t} {{ color: {fg}; background-color: {bg}; font-weight: bold; font-size: 11pt; }}")
    return '\n'.join(rules)


# ---------- blocks ----------

def P(text):
    return f'<p>{text}</p>'


def FIG(name, caption=None):
    w, _ = size(name)
    if w >= 240:   # screenshot: thin frame, edge to edge
        table = (f'<table width="100%" cellspacing="0" cellpadding="0" border="0"><tr>'
                 f'<td align="center" class="shot">{img(name)}</td></tr></table>')
    else:          # small picture: centred on a light box
        table = (f'<table width="100%" cellspacing="0" cellpadding="10" border="0"><tr>'
                 f'<td align="center" bgcolor="{CANVAS}">{img(name)}</td></tr></table>')
    tail = f'<p class="cap">{caption}</p>' if caption else '<p class="gap"></p>'
    return table + tail


def TIP(label, text, mode):
    m = MODES[mode]
    return ('<table width="100%" cellspacing="0" cellpadding="0" border="0"><tr>'
            f'<td width="4" bgcolor="{m["accent"]}"></td>'
            f'<td bgcolor="{m["tint"]}"><table width="100%" cellspacing="0" cellpadding="9" border="0"><tr>'
            f'<td class="tip"><b>{label}</b> {text}</td></tr></table></td></tr></table>')


def ITEMS(rows):
    """rows: (icon, name, text)"""
    out = ['<table width="100%" cellspacing="0" cellpadding="0" border="0">']
    for icon, name, text in rows:
        out.append(f'<tr><td width="58" valign="top"><p class="ic">{img(icon)}</p></td>'
                   f'<td valign="top"><p class="it"><span class="name">{name}</span><br>{text}</p></td></tr>')
    out.append('</table>')
    return ''.join(out)


def MODEROWS(rows):
    """rows: (image, text)"""
    out = ['<table width="100%" cellspacing="0" cellpadding="0" border="0">']
    for pill, text in rows:
        w, _ = size(pill)
        out.append(f'<tr><td valign="middle" width="{w + 12}"><p class="mi">{img(pill)}</p></td>'
                   f'<td valign="middle"><p class="mt">{text}</p></td></tr>')
    out.append('</table>')
    return ''.join(out)


def TAG(kind, label):
    return f'<span class="tg-{kind}">&nbsp;{label}&nbsp;</span>'


def page(mode, chip, title, lead, sections):
    head = (f'<html><head><meta charset="UTF-8"><style type="text/css">\n{css(mode)}\n</style></head><body>')
    top = (f'<table cellspacing="0" cellpadding="4" border="0"><tr><td class="chip" bgcolor="{MODES[mode]["chip"]}">'
           f'&nbsp;{chip}&nbsp;</td></tr></table>'
           f'<h1>{title}</h1><p class="lead">{lead}</p>')
    body = [top]
    for heading, blocks in sections:
        body.append(f'<h2>{heading}</h2>')
        body.extend(blocks)
    return head + '\n' + '\n'.join(body) + '\n</body></html>\n'


# ---------- content ----------

def content(lang):
    global LANG
    LANG = lang
    T = TEXT[lang]
    pages = {}

    pages['design'] = page('design', T['chip_design'], T['design_title'], T['design_lead'], [
        (T['d_add_h'], [P(T['d_add_p']), FIG('draganddrop.gif', T['d_add_cap']), P(T['d_del_p']),
                        FIG('kontextmenue_entwurf.png', T['d_del_cap'])]),
        (T['d_con_h'], [P(T['d_con_p']), FIG('verkabeln.png', T['d_con_cap']), P(T['d_con_p2']), P(T['d_con_wlan'])]),
        (T['d_cfg_h'], [P(T['d_cfg_p']), FIG('konfiguration.png', T['d_cfg_cap']),
                        ITEMS([('bauteil_server.png', T['n_host'], T['d_host']),
                               ('bauteil_switch.png', T['n_switch'], T['d_switch']),
                               ('bauteil_gateway.png', T['n_gateway'], T['d_gateway']),
                               ('bauteil_router.png', T['n_router'], T['d_router'])]),
                        TIP(T['tip'], T['d_tip'], 'design')]),
        (T['d_mis_h'], [FIG('lernpfad.png'), P(T['d_mis_p'])]),
        (T['d_file_h'], [FIG('dateien.png'), P(T['d_file_p'])]),
        (T['d_lang_h'], [MODEROWS([('sprache.png', T['d_lang_p'])])]),
        (T['d_dark_h'], [MODEROWS([('dunkelmodus.png', T['d_dark_p'])])]),
        (T['switch_h'], [MODEROWS([('aktionsmodus.png', T['go_sim']), ('dokumodus.png', T['go_doc'])])]),
    ])

    pages['simulation'] = page('simulation', T['chip_sim'], T['sim_title'], T['sim_lead'], [
        (T['s_use_h'], [P(T['s_use_p']), FIG('icon_softwareinstallation.png'), P(T['s_tags_p']),
                        ITEMS_TAGS(T), FIG('softwareinstallation.png', T['s_tags_cap']), P(T['s_use_p2'])]),
        (T['s_web_h'], [P(T['s_web_p']), P(T['s_web_p2']), TIP(T['tip'], T['s_web_tip'], 'simulation')]),
        (T['s_https_h'], [P(T['s_https_p']), P(T['s_https_p2'])]),
        (T['s_vpn_h'], [P(T['s_vpn_p']), P(T['s_vpn_p2'])]),
        (T['s_ip_h'], [P(T['s_ip_p']), FIG('netzwek_aus.png')]),
        (T['s_data_h'], [P(T['s_glow_p']), FIG('kabel_aktiv.png', T['s_glow_cap']),
                         P(T['s_speed_p']), FIG('tempo.png'),
                         P(T['s_ex_p']), FIG('kontextmenue_simulation.png'), P(T['s_table_p'])]),
        (T['s_model_h'], [P(T['s_model_p']), FIG('schichtmodell.png'), P(T['s_model_p2']),
                          FIG('osi_stapel.png', T['s_model_cap']), TIP(T['tip'], T['s_model_tip'], 'simulation')]),
        (T['s_fw_h'], [P(T['s_fw_p'])]),
        (T['s_sat_h'], [P(T['s_sat_p'])]),
        (T['switch_h'], [MODEROWS([('entwurfsmodus.png', T['go_design']), ('dokumodus.png', T['go_doc'])])]),
    ])

    pages['documentation'] = page('documentation', T['chip_doc'], T['doc_title'], T['doc_lead'], [
        (T['o_el_h'], [ITEMS([('doku_add_text_small.png', T['n_text'], T['o_text']),
                              ('doku_add_small.png', T['n_field'], T['o_field']),
                              ('doku_download_small.png', T['n_export'], T['o_export']),
                              ('doku_pdf_small.png', T['n_report'], T['o_report'])]),
                       TIP(T['tip'], T['o_tip'], 'documentation')]),
        (T['switch_h'], [MODEROWS([('entwurfsmodus.png', T['go_design']), ('aktionsmodus.png', T['go_sim'])])]),
    ])
    return pages


def ITEMS_TAGS(T):
    rows = ['<table width="100%" cellspacing="0" cellpadding="0" border="0">']
    for kind in ('server', 'client', 'p2p', 'tool'):
        rows.append(f'<tr><td valign="top" nowrap><p class="mi">{TAG(kind, T["tag_" + kind])}</p></td>'
                    f'<td valign="top"><p class="mt">{T["tagd_" + kind]}</p></td></tr>')
    rows.append('</table>')
    return ''.join(rows)


from help_text import TEXT  # noqa: E402  (texts live in their own file)


def main():
    out_dir = os.path.join(OUT, 'hilfe')
    os.makedirs(out_dir, exist_ok=True)
    for lang in TEXT:
        for mode, text in content(lang).items():
            path = os.path.join(out_dir, FILES[mode].format(lang))
            with open(path, 'w', encoding='utf-8') as f:
                f.write(text)
            print('wrote', path)


if __name__ == '__main__':
    main()
