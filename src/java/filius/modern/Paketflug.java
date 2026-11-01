/*
 * Filius (RBZ Wirtschaft Kiel Version): new in Filius (RBZ Wirtschaft Kiel Version), not part of Filius 2.9.3.
 * Written by Thorben Demand @ RBZ Wirtschaft Kiel, 2026. Licensed like Filius under the GPL.
 */
package filius.modern;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import filius.hardware.Port;
import filius.hardware.Verbindung;

/**
 * Connects the simulation with the picture of a cable: every frame that goes onto a cable is reported here
 * with its sending port (from SimplexVerbindung), and the cable on the canvas shows a packet flying in that
 * direction (JCablePanel: a blue shooting star on a cable, radio waves and a signal point on a Wi-Fi link).
 */
public final class Paketflug {

    /** a packet is visible for at least this long, also when the simulation runs fast */
    public static final int MIN_FLUG_MS = 380;

    /** what shows the flights of one connection (the cable on the canvas) */
    public interface Anzeige {
        /** a frame left the given port; it arrives at the other end after dauerMs */
        void paketGesendet(Port von, int dauerMs);
    }

    private static final Map<Verbindung, WeakReference<Anzeige>> ANZEIGEN = new WeakHashMap<>();

    private Paketflug() {}

    /** the picture of a connection registers itself (again) whenever it is set up or notified */
    public static void anmelden(Verbindung verbindung, Anzeige anzeige) {
        if (verbindung == null || anzeige == null) {
            return;
        }
        synchronized (ANZEIGEN) {
            WeakReference<Anzeige> alt = ANZEIGEN.get(verbindung);
            if (alt == null || alt.get() != anzeige) {
                ANZEIGEN.put(verbindung, new WeakReference<>(anzeige));
            }
        }
    }

    /** called by the simulation (thread of the cable) when a frame goes onto the cable */
    public static void gesendet(Verbindung verbindung, Port von) {
        if (!Prefs.effectsEnabled()) {
            return;
        }
        Anzeige a;
        synchronized (ANZEIGEN) {
            WeakReference<Anzeige> r = ANZEIGEN.get(verbindung);
            a = r == null ? null : r.get();
        }
        if (a != null) {
            try {
                a.paketGesendet(von, Math.max(MIN_FLUG_MS, Verbindung.holeVerzoegerung()));
            } catch (RuntimeException e) {
                // the picture must never disturb the simulation
            }
        }
    }
}
