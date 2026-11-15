package filius.modern.lernpfad;

import java.util.function.Function;

import filius.modern.Texts;

/**
 * One part of a practice task that Filius checks: either a condition of the network as it is right now (a
 * state step, e.g. "Laptop-3 has the address 192.168.10.13") or something that was observed in the simulation
 * (an evidence step, e.g. "Laptop-1 got an answer to its ping"). A {@link Regel rule} is a list of such steps.
 * The same steps decide whether a task is done and tell the student in the hint of the task what is done
 * already and what comes next, so the hint always says exactly where the check is stuck.
 */
public final class Schritt {

    final Regel.Zustand zustand;
    final Regel.Nachweis nachweis;
    private final String de;
    private final String en;
    /** the current value of a setting, shown with the step while it is open ("jetzt: ...") */
    Function<Netz, String> ist;
    /** a state step that the evidence does not have to wait for (it is independent of the test) */
    boolean frei;
    /** the hint shows this step as open until the steps before it are done ("afterwards put the cable back") */
    boolean danach;

    private Schritt(Regel.Zustand zustand, Regel.Nachweis nachweis, String de, String en) {
        this.zustand = zustand;
        this.nachweis = nachweis;
        this.de = de;
        this.en = en;
    }

    /** a condition of the network; the text says what to do, German and English */
    public static Schritt z(String de, String en, Regel.Zustand z) {
        return new Schritt(z, null, de, en);
    }

    /** something that has to be observed in the simulation; the text says what to do, German and English */
    public static Schritt n(String de, String en, Regel.Nachweis n) {
        return new Schritt(null, n, de, en);
    }

    /** shows the current value while the step is open */
    public Schritt ist(Function<Netz, String> wert) {
        this.ist = wert;
        return this;
    }

    /** the evidence of the task does not have to wait for this state step */
    public Schritt frei() {
        this.frei = true;
        return this;
    }

    /** the hint shows the step as open until all steps before it are done */
    public Schritt danach() {
        this.danach = true;
        return this;
    }

    public boolean istZustand() {
        return zustand != null;
    }

    /** what to do, in the language Filius runs in */
    public String text() {
        return Texts.englisch() ? en : de;
    }

    /** what to do, in German or in English */
    String text(boolean englisch) {
        return englisch ? en : de;
    }
}
