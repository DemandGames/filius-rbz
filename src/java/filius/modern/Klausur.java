package filius.modern;

/**
 * Exam edition ("Klausurversion") of Filius (RBZ Wirtschaft Kiel Version): the same program, but without the
 * learning path. No button, no panel, no progress from project files, no glossary, quiz or hints; the help
 * of the exam package has no learning path parts either (tools/klausur.sh).
 *
 * <p>The switch is the resource {@code filius/modern/klausur.flag} inside filius-rbz.jar, which only the exam
 * package contains. It is deliberately not a setting in config/filius.ini, so students cannot switch the
 * learning path back on. Adding the file can only switch the exam mode on, never off.
 */
public final class Klausur {

    /** true in the exam edition */
    public static final boolean AKTIV = Klausur.class.getResource("/filius/modern/klausur.flag") != null;

    private Klausur() {}
}
