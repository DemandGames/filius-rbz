package filius.modern.lernpfad;

import java.io.File;

/**
 * The two learning paths. Standard: 8 chapters, one router with three cards. Advanced: the
 * 12 chapters with HTTPS, home network, Wi-Fi, NAT, port forwarding, firewall and VPN. Each has its own folder, rules,
 * start networks and progress.
 */
public enum Pfad {
    STANDARD("standard", 8), FORTGESCHRITTEN("fortgeschritten", 12);

    /** name in project files and in config/filius.ini */
    public final String kennung;
    /** number of chapters; the last one is the final project with three variants of faults */
    public final int kapitel;

    Pfad(String kennung, int kapitel) {
        this.kennung = kennung;
        this.kapitel = kapitel;
    }

    /** the chapter whose start network is chosen at random from three variants (a, b, c) */
    public int variantenKapitel() {
        return kapitel;
    }

    /** folder with the chapter files: lernpfad/standard or lernpfad */
    File ordner() {
        File basis = Lernpfad.ordner();
        return this == STANDARD ? new File(basis, "standard") : basis;
    }

    Regel regel(String id) {
        return this == STANDARD ? RegelnStandard.regel(id) : Regeln.regel(id);
    }

    /** key of the texts of this path, e.g. "lp.path.standard" */
    String textKey() {
        return this == STANDARD ? "lp.path.standard" : "lp.path.advanced";
    }

    static Pfad aus(String kennung) {
        for (Pfad p : values()) {
            if (p.kennung.equalsIgnoreCase(kennung == null ? "" : kennung.trim())) {
                return p;
            }
        }
        return null;
    }
}
