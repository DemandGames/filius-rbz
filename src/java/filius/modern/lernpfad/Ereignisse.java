package filius.modern.lernpfad;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import filius.hardware.knoten.Knoten;

/** All events of the current session, oldest first. Only touched on the event thread. */
public final class Ereignisse {

    private static final int MAX = 20_000;
    private final List<Ereignis> liste = new ArrayList<>();

    public void add(Ereignis e) {
        liste.add(e);
        if (liste.size() > MAX) {
            liste.subList(0, MAX / 4).clear();
        }
    }

    public void leeren() {
        liste.clear();
    }

    void auffrischen(Ereignis.Art art, Knoten k, long zeit) {
        for (int i = liste.size() - 1; i >= 0; i--) {
            Ereignis e = liste.get(i);
            if (e.art == art && e.knoten == k) {
                e.zeit = zeit;
                return;
            }
        }
    }

    public List<Ereignis> alle() {
        return liste;
    }

    /** Events of the given kind at or after {@code seit} that match the filter. */
    public List<Ereignis> finde(Ereignis.Art art, long seit, Predicate<Ereignis> filter) {
        List<Ereignis> result = new ArrayList<>();
        for (Ereignis e : liste) {
            if (e.art == art && e.zeit >= seit && (filter == null || filter.test(e))) {
                result.add(e);
            }
        }
        return result;
    }

    public boolean gibt(Ereignis.Art art, long seit, Predicate<Ereignis> filter) {
        for (int i = liste.size() - 1; i >= 0; i--) {
            Ereignis e = liste.get(i);
            if (e.art == art && e.zeit >= seit && (filter == null || filter.test(e))) {
                return true;
            }
        }
        return false;
    }

    /** The latest event of the kind that matches, or null. */
    public Ereignis letztes(Ereignis.Art art, long seit, Predicate<Ereignis> filter) {
        for (int i = liste.size() - 1; i >= 0; i--) {
            Ereignis e = liste.get(i);
            if (e.art == art && e.zeit >= seit && (filter == null || filter.test(e))) {
                return e;
            }
        }
        return null;
    }

    /** The earliest event of the kind that matches, or null. */
    public Ereignis erstes(Ereignis.Art art, long seit, Predicate<Ereignis> filter) {
        for (Ereignis e : liste) {
            if (e.art == art && e.zeit >= seit && (filter == null || filter.test(e))) {
                return e;
            }
        }
        return null;
    }
}
