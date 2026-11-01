#!/bin/bash
# Builds the Debian/Ubuntu package out/installer/filius-rbz_<version>_amd64.deb: program in /opt/filius-rbz
# with its own Java runtime (Liberica JDK 21 "full", jlink, with the browser engine), starter /usr/bin/filius-rbz,
# menu entry, icons and the file type .fls.
#   RUNTIMES: folder with rt-linux-amd64 from tools/runtimes.sh (default .deps)
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(dirname "$HERE")"
RUNTIMES="${RUNTIMES:-${DEPS:-$ROOT/.deps}}"
OUT="$ROOT/out/installer"
STAND="$(grep -o 'STAND = "[^"]*"' "$ROOT/src/java/filius/gui/InfoDialog.java" | cut -d'"' -f2)"
STANDNR="$(echo "$STAND" | awk -F. '{print $3 $2 $1}')"
VERSION="2.9.3+rbz$STANDNR"
D="$OUT/deb"
rm -rf "$D" && mkdir -p "$D/DEBIAN" "$D/opt" "$D/usr/bin" "$D/usr/share/applications" "$D/usr/share/mime/packages" "$D/usr/share/doc/filius-rbz"
"$HERE/stage-program.sh" "$D/opt/filius-rbz"
cp -r "$RUNTIMES/rt-linux-amd64" "$D/opt/filius-rbz/java-runtime"
chmod 755 "$D/opt/filius-rbz/java-runtime/bin/"* "$D/opt/filius-rbz/java-runtime/lib/jspawnhelper" 2>/dev/null || true

cat > "$D/usr/bin/filius-rbz" <<'SH'
#!/bin/sh
# Filius (RBZ Wirtschaft Kiel Version): starts the program in /opt/filius-rbz with its own Java runtime
cd /opt/filius-rbz || exit 1
exec /opt/filius-rbz/java-runtime/bin/java -XX:-UsePerfData -jar /opt/filius-rbz/filius-rbz.jar "$@"
SH
chmod 755 "$D/usr/bin/filius-rbz"

cat > "$D/usr/share/applications/filius-rbz.desktop" <<'DESK'
[Desktop Entry]
Type=Application
Name=Filius RBZ
Name[de]=Filius RBZ
GenericName=Network simulation
GenericName[de]=Netzwerksimulation
Comment=Filius (RBZ Wirtschaft Kiel Version): learning software about computer networks with a learning path
Comment[de]=Filius (RBZ Wirtschaft Kiel Version): Lernsoftware zu Rechnernetzen mit Lernpfad
Exec=filius-rbz %f
Icon=filius-rbz
Terminal=false
Categories=Education;Network;
MimeType=application/x-filius-project;
StartupNotify=true
StartupWMClass=filius-modern-ModernFilius
DESK

cat > "$D/usr/share/mime/packages/filius-rbz.xml" <<'MIME'
<?xml version="1.0" encoding="UTF-8"?>
<mime-info xmlns="http://www.freedesktop.org/standards/shared-mime-info">
  <mime-type type="application/x-filius-project">
    <comment>Filius project</comment>
    <comment xml:lang="de">Filius-Projekt</comment>
    <glob pattern="*.fls"/>
    <sub-class-of type="application/zip"/>
    <icon name="filius-rbz"/>
  </mime-type>
</mime-info>
MIME

python3 - "$D" "$ROOT/design/filius-logo.png" <<'PY'
import os, sys
from PIL import Image
d = sys.argv[1]
logo = Image.open(sys.argv[2]).convert('RGBA')
for s in (16, 24, 32, 48, 64, 128, 256, 512):
    p = os.path.join(d, 'usr/share/icons/hicolor/%dx%d/apps' % (s, s))
    os.makedirs(p, exist_ok=True)
    logo.resize((s, s), Image.LANCZOS).save(os.path.join(p, 'filius-rbz.png'))
PY

cp "$ROOT/out/Filius-RBZ-Wirtschaft-Kiel/LIESMICH.txt" "$D/usr/share/doc/filius-rbz/LIESMICH.txt"
cp "$ROOT/out/Filius-RBZ-Wirtschaft-Kiel/README.txt" "$D/usr/share/doc/filius-rbz/README.txt"
cp "$ROOT/out/Filius-RBZ-Wirtschaft-Kiel/lizenzen/GPLv3.txt" "$D/usr/share/doc/filius-rbz/copyright"

SIZE=$(du -sk "$D" --exclude=DEBIAN | cut -f1)
cat > "$D/DEBIAN/control" <<CTRL
Package: filius-rbz
Version: $VERSION
Section: education
Priority: optional
Architecture: amd64
Installed-Size: $SIZE
Maintainer: Thorben Demand <thorben.demand@rbz-wirtschaft-kiel.de>
Homepage: https://www.demand-unterricht.de
Depends: libc6 (>= 2.27), libgtk-3-0t64 | libgtk-3-0, libfreetype6, libfontconfig1, libx11-6, libxext6, libxi6, libxrender1, libxtst6, libxxf86vm1, libgl1
Recommends: libasound2t64 | libasound2
Description: Filius (RBZ Wirtschaft Kiel Version) - learning software about computer networks
 Filius simulates computer networks: laptops, switches, routers, servers, DNS,
 DHCP, e-mail, HTTPS, VPN and firewalls. This version of Filius 2.9.3 has a
 redesigned user interface and a learning path for lessons (German and
 English). It brings its own Java runtime with the browser engine.
CTRL

cat > "$D/DEBIAN/postinst" <<'POST'
#!/bin/sh
set -e
if command -v update-mime-database >/dev/null 2>&1; then update-mime-database /usr/share/mime >/dev/null 2>&1 || true; fi
if command -v update-desktop-database >/dev/null 2>&1; then update-desktop-database -q /usr/share/applications || true; fi
if command -v gtk-update-icon-cache >/dev/null 2>&1; then gtk-update-icon-cache -q -t -f /usr/share/icons/hicolor 2>/dev/null || true; fi
exit 0
POST
cp "$D/DEBIAN/postinst" "$D/DEBIAN/postrm"
chmod 755 "$D/DEBIAN/postinst" "$D/DEBIAN/postrm"
find "$D" -type d -exec chmod 755 {} +

DEB="$OUT/filius-rbz_${VERSION}_amd64.deb"
rm -f "$DEB"
dpkg-deb -Zxz --root-owner-group -b "$D" "$DEB" > /dev/null
ls -la "$DEB" | awk '{print $5, $9}'
