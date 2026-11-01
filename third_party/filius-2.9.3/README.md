# Filius 2.9.3 – original program files (unchanged)

These are the program files of the official **Filius 2.9.3** release (2025-06-02), unchanged:
`filius.jar` (built by the Filius project on AppVeyor, see `META-INF/MANIFEST.MF`), the libraries in `lib/`,
`config/`, `tmpl/`, `img/`, the examples in `beispiele/`, `Changelog.md` and `Einfuehrung_Filius.pdf`.
The help pages (`hilfe/`) of the original are not included, because this version brings its own.

- Filius: https://www.lernsoftware-filius.de – © Stefan Freischlad, Universität Siegen and others
- Licence: GNU GPL v2 or v3 (`GPLv2.txt`, `GPLv3.txt`); the libraries in `lib/` keep their own open source
  licences (Apache Batik, Apache Commons, Spring, iText 5 AGPL, Logback, SLF4J, HTMLParser, …)
- Source code of Filius: https://gitlab.com/filius1/filius

`SHA256SUMS` lists the checksum of every file. The build (`build.sh`, `package.sh`) uses these files as
they are; Filius (RBZ Wirtschaft Kiel Version) loads its own classes from `filius-rbz.jar` in front of
`filius.jar`. Version 2.9.3 is no longer offered for download by the Filius project (newer versions are);
the files were taken from the official 2.9.3 release when this version was started (October 2026).
