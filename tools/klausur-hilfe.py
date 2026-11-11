#!/usr/bin/env python3
"""Help pages of the exam edition (Klausurversion): removes everything about the learning path and the tip
boxes (they give subject hints such as which gateway to enter). Works in place on a copied hilfe/ folder:

    python3 tools/klausur-hilfe.py out/Filius-RBZ-Klausur/hilfe

Every change must match exactly; if the help pages change (design/help.py), this script stops with an
error instead of leaving learning path text behind.
"""
import os
import re
import sys

ordner = sys.argv[1]

# sentences that mention the learning path inside otherwise useful paragraphs
ERSETZEN = {
    'de_DE': [
        (' Im Lernpfad sind Laptops die Geräte, an denen jemand arbeitet und surft; PCs bieten als Server anderen etwas an, z. B. Webseiten.', ''),
        ('<b>Neu</b> beginnt ein leeres Projekt, auch der Lernpfad beginnt dann von vorn.', '<b>Neu</b> beginnt ein leeres Projekt.'),
        ('sichert dein Netz zusammen mit dem Fortschritt im Lernpfad.', 'sichert dein Netz.'),
        ('Filius gibt es auf Deutsch und Englisch, auch den Lernpfad.', 'Filius gibt es auf Deutsch und Englisch.'),
        ('Die Hilfe und die Bilder im Lernpfad und in der Hilfe passen sich an.', 'Die Hilfe und ihre Bilder passen sich an.'),
    ],
    'en_GB': [
        (' In the learning path, laptops are the devices people work and browse on; PCs offer something to others as servers, e.g. web pages.', ''),
        ('<b>New</b> starts an empty project, and the learning path starts from the beginning.', '<b>New</b> starts an empty project.'),
        ('saves your network together with your progress in the learning path.', 'saves your network.'),
        ('Filius is available in English and German, including the learning path.', 'Filius is available in English and German.'),
        ('The help and the pictures in the learning path and in the help follow your choice.', 'The help and its pictures follow your choice.'),
    ],
}
ABSCHNITT = {'de_DE': 'Lernpfad', 'en_GB': 'Learning path'}

fehler = []
for name in sorted(os.listdir(ordner)):
    if not name.endswith('.html'):
        continue
    sprache = name[-10:-5]
    pfad = os.path.join(ordner, name)
    with open(pfad, encoding='utf-8') as f:
        text = f.read()

    # tip boxes: one line each, a table around <td class="tip">
    zeilen = text.split('\n')
    tipps = [z for z in zeilen if 'class="tip"' in z]
    for z in tipps:
        if not (z.startswith('<table') and z.rstrip().endswith('</table>')):
            fehler.append(f'{name}: Tipp-Kasten nicht erkannt')
    text = '\n'.join(z for z in zeilen if 'class="tip"' not in z)

    # the section "Lernpfad" / "Learning path": from its heading to the next one
    kopf = f'<h2>{ABSCHNITT[sprache]}</h2>'
    if kopf in text:
        anfang = text.index(kopf)
        ende = text.index('<h2>', anfang + len(kopf))
        text = text[:anfang] + text[ende:]
    elif name.startswith('design_mode'):
        fehler.append(f'{name}: Abschnitt {kopf} fehlt')

    if name.startswith('design_mode'):
        for alt, neu in ERSETZEN[sprache]:
            if text.count(alt) != 1:
                fehler.append(f'{name}: nicht gefunden: {alt[:60]}')
            text = text.replace(alt, neu)

    rest = re.findall(r'(?i)[^.>]*(lernpfad|learning path|glossar|glossary)[^.<]*', text)
    if rest:
        fehler.append(f'{name}: noch erwähnt: {rest[:3]}')
    with open(pfad, 'w', encoding='utf-8') as f:
        f.write(text)
    print(f'{name}: {len(tipps)} Tipp(s) entfernt')

gfx = os.path.join(ordner, 'gfx')
for name in sorted(os.listdir(gfx)):
    if name.startswith('lernpfad'):
        os.remove(os.path.join(gfx, name))
        print(f'gfx/{name} entfernt')

if fehler:
    print('\n'.join(fehler))
    sys.exit(1)
