package filius.modern.lernpfad;

import java.util.ArrayList;
import java.util.List;

/**
 * When a practice task counts as done. A task is done when
 * <ol>
 * <li>its state condition holds in the network right now,</li>
 * <li>the required events were observed <em>after</em> that condition became true for the first time (so a
 * ping from before the addresses were right does not count), and</li>
 * <li>a question, if any, was answered correctly.</li>
 * </ol>
 * Evidence stays valid once it counted: if the condition is false for a while afterwards (an address handed
 * out anew by DHCP at every start of the simulation, an experiment, a setting changed and changed back), the
 * test does not have to be repeated. At the end of the practice part every task is checked again: what was
 * right then must still be right. Tasks that only make sense in an intermediate state (e.g. "the ping fails
 * before the routes exist") are marked as transitional and skip that final check.
 *
 * <p>A rule is usually a list of {@link Schritt steps}. The steps are the check and at the same time the
 * material of the hint that tells the student which parts are done and what comes next.
 */
public final class Regel {

    public interface Zustand {
        boolean ok(Netz n);
    }

    public interface Nachweis {
        boolean ok(Netz n, Ereignisse e, long seit);
    }

    public interface Antwort {
        boolean ok(Netz n, Ereignisse e, String antwort);
    }

    /** a step of a task as the hint shows it */
    public static final class Stand {
        public final String text;
        public final boolean ok;
        /** current value of the setting of an open step, or null */
        public final String ist;
        /** open evidence that was observed before the steps it depends on were done: it has to be repeated */
        public final boolean frueher;

        Stand(String text, boolean ok, String ist, boolean frueher) {
            this.text = text;
            this.ok = ok;
            this.ist = ist;
            this.frueher = frueher;
        }
    }

    Zustand zustand;
    Nachweis nachweis;
    Antwort antwort;
    boolean uebergang;
    /** the parts of the rule in the order the student works on them (empty for rules without steps) */
    List<Schritt> schritte = List.of();

    public static Regel zustand(Zustand z) {
        Regel r = new Regel();
        r.zustand = z;
        return r;
    }

    public static Regel nachweis(Nachweis n) {
        Regel r = new Regel();
        r.nachweis = n;
        return r;
    }

    /** a rule made of steps: all state steps must hold now, all evidence steps must have been observed */
    public static Regel schritte(Schritt... s) {
        Regel r = new Regel();
        r.schritte = List.of(s);
        boolean hatZustand = false;
        boolean hatNachweis = false;
        for (Schritt x : s) {
            hatZustand |= x.zustand != null;
            hatNachweis |= x.nachweis != null;
        }
        if (hatZustand) {
            r.zustand = n -> {
                for (Schritt x : s) {
                    if (x.zustand != null && !x.zustand.ok(n)) {
                        return false;
                    }
                }
                return true;
            };
        }
        if (hatNachweis) {
            r.nachweis = (n, e, t) -> {
                for (Schritt x : s) {
                    if (x.nachweis != null && !x.nachweis.ok(n, e, t)) {
                        return false;
                    }
                }
                return true;
            };
        }
        return r;
    }

    public Regel und(Nachweis n) {
        this.nachweis = n;
        return this;
    }

    public Regel antwort(Antwort a) {
        this.antwort = a;
        return this;
    }

    /** The task describes an intermediate state; it is not checked again at the end. */
    public Regel uebergang() {
        this.uebergang = true;
        return this;
    }

    boolean zustandOk(Netz n) {
        try {
            return zustand == null || zustand.ok(n);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * The part of the state the evidence has to wait for: all state steps except the free ones (rules without
     * steps: the whole state).
     */
    boolean torOk(Netz n) {
        if (schritte.isEmpty()) {
            return zustandOk(n);
        }
        try {
            for (Schritt x : schritte) {
                if (x.zustand != null && !x.frei && !x.zustand.ok(n)) {
                    return false;
                }
            }
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    boolean nachweisOk(Netz n, Ereignisse e, long seit) {
        try {
            return nachweis == null || nachweis.ok(n, e, seit);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    boolean antwortOk(Netz n, Ereignisse e, String a) {
        if (antwort == null) {
            return true;
        }
        try {
            return a != null && !a.isBlank() && antwort.ok(n, e, a.trim());
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /**
     * The steps of the rule as they stand right now, for the hint of the task.
     *
     * @param ab
     *            evidence counts from this time on (Long.MAX_VALUE: not yet, the state it waits for was never true)
     * @param anfang
     *            start of the practice part: evidence between anfang and ab was too early
     */
    List<Stand> stand(Netz n, Ereignisse e, long ab, long anfang) {
        List<Stand> l = new ArrayList<>();
        for (Schritt x : schritte) {
            boolean ok;
            String ist = null;
            boolean frueher = false;
            try {
                if (x.zustand != null) {
                    ok = x.zustand.ok(n);
                    if (!ok && x.ist != null) {
                        ist = x.ist.apply(n);
                    }
                } else {
                    ok = ab != Long.MAX_VALUE && x.nachweis.ok(n, e, ab);
                    frueher = !ok && x.nachweis.ok(n, e, anfang);
                }
            } catch (RuntimeException ex) {
                ok = false;
            }
            if (x.danach && ok) {
                // "afterwards ...": only counts in the hint when everything before it is done
                for (Stand vorher : l) {
                    ok &= vorher.ok;
                }
            }
            l.add(new Stand(x.text(), ok, ist, frueher));
        }
        return l;
    }
}
