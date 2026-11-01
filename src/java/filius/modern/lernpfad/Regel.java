package filius.modern.lernpfad;

/**
 * When a practice task counts as done. A task is done when
 * <ol>
 * <li>its state condition holds in the network right now,</li>
 * <li>the required events were observed <em>after</em> that condition last became true (so a
 * ping from before the addresses were right does not count), and</li>
 * <li>a question, if any, was answered correctly.</li>
 * </ol>
 * At the end of the practice part every task is checked again: what was right then must still
 * be right. Tasks that only make sense in an intermediate state (e.g. "the ping fails before the
 * routes exist") are marked as transitional and skip that final check.
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

    Zustand zustand;
    Nachweis nachweis;
    Antwort antwort;
    boolean uebergang;

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
}
