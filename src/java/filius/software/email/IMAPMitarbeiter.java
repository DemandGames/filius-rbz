package filius.software.email;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import filius.software.clientserver.ServerMitarbeiter;
import filius.software.transportschicht.TCPSocket;

/**
 * One IMAP connection of the mail server. It understands the commands an e-mail program needs:
 * CAPABILITY, LOGIN, LIST, SELECT/EXAMINE INBOX, FETCH, STORE, EXPUNGE, CLOSE, NOOP and LOGOUT (also as
 * UID FETCH / UID STORE). Every command is answered with one message: untagged lines "* ..." and the
 * tagged status line "&lt;tag&gt; OK|NO|BAD ...". Mail texts are sent as literals "{length}" + text.
 */
public class IMAPMitarbeiter extends ServerMitarbeiter {

    private final EmailServer emailServer;
    private EmailKonto konto;
    private boolean ausgewaehlt;
    private boolean nurLesen;

    public IMAPMitarbeiter(TCPSocket socket, IMAPServer server) {
        super(server, socket);
        this.emailServer = server.holeEmailServer();
        this.emailServer.benachrichtigeBeobachter(EmailServer.LINE_SEPARATOR);
        this.antworten("* OK [CAPABILITY IMAP4rev1] IMAP server ready");
    }

    @Override
    protected void verarbeiteNachricht(String nachricht) {
        this.emailServer.benachrichtigeBeobachter(this.socket.holeZielIPAdresse() + "< " + nachricht);
        String zeile = nachricht.trim();
        String[] t = zeile.split("\\s+", 3);
        if (t.length < 2) {
            this.antworten("* BAD missing command");
            return;
        }
        String tag = t[0];
        String befehl = t[1].toUpperCase(Locale.ROOT);
        String rest = t.length > 2 ? t[2].trim() : "";
        boolean uid = false;
        if (befehl.equals("UID")) {
            String[] u = rest.split("\\s+", 2);
            befehl = u[0].toUpperCase(Locale.ROOT);
            rest = u.length > 1 ? u[1].trim() : "";
            uid = true;
            if (!befehl.equals("FETCH") && !befehl.equals("STORE")) {
                this.antworten(tag + " BAD unknown UID command");
                return;
            }
        }
        switch (befehl) {
        case "CAPABILITY":
            this.antworten("* CAPABILITY IMAP4rev1\n" + tag + " OK CAPABILITY completed");
            break;
        case "NOOP":
            this.antworten((this.ausgewaehlt ? "* " + this.postfach().size() + " EXISTS\n" : "") + tag + " OK NOOP completed");
            break;
        case "LOGIN":
            this.login(tag, rest);
            break;
        case "LIST":
            if (this.konto == null) {
                this.antworten(tag + " NO please LOGIN first");
            } else {
                this.antworten("* LIST (\\HasNoChildren) \"/\" INBOX\n" + tag + " OK LIST completed");
            }
            break;
        case "SELECT":
        case "EXAMINE":
            this.select(tag, befehl, rest);
            break;
        case "FETCH":
            this.fetch(tag, rest, uid);
            break;
        case "STORE":
            this.store(tag, rest, uid);
            break;
        case "EXPUNGE":
            if (!this.ausgewaehlt) {
                this.antworten(tag + " NO no mailbox selected");
            } else {
                this.antworten(this.expunge(true) + tag + " OK EXPUNGE completed");
            }
            break;
        case "CLOSE":
            if (this.ausgewaehlt && !this.nurLesen) {
                this.expunge(false);
            }
            this.ausgewaehlt = false;
            this.antworten(tag + " OK CLOSE completed");
            break;
        case "LOGOUT":
            this.konto = null;
            this.ausgewaehlt = false;
            this.antworten("* BYE IMAP server logging out\n" + tag + " OK LOGOUT completed");
            this.emailServer.benachrichtigeBeobachter();
            this.schliesseSocket();
            break;
        default:
            this.antworten(tag + " BAD unknown command");
        }
    }

    private void login(String tag, String rest) {
        String[] a = argumente(rest);
        if (a.length < 2) {
            this.antworten(tag + " BAD LOGIN needs user name and password");
            return;
        }
        EmailKonto k = this.emailServer.sucheKonto(a[0], a[1]);
        if (k == null) {
            this.antworten(tag + " NO [AUTHENTICATIONFAILED] user name or password wrong");
            return;
        }
        this.konto = k;
        this.ausgewaehlt = false;
        this.antworten(tag + " OK LOGIN completed");
    }

    private void select(String tag, String befehl, String rest) {
        if (this.konto == null) {
            this.antworten(tag + " NO please LOGIN first");
            return;
        }
        String[] a = argumente(rest);
        if (a.length < 1 || !a[0].equalsIgnoreCase("INBOX")) {
            this.ausgewaehlt = false;
            this.antworten(tag + " NO mailbox does not exist (there is only INBOX)");
            return;
        }
        List<Email> p = this.postfach();
        int neu = 0;
        for (Email e : p) {
            this.emailServer.uid(this.konto, e);
            if (e.getNeu()) {
                neu++;
            }
        }
        this.ausgewaehlt = true;
        this.nurLesen = befehl.equals("EXAMINE");
        this.antworten("* " + p.size() + " EXISTS\n* " + neu + " RECENT\n* OK [UIDVALIDITY " + this.emailServer.uidValidity() + "] UIDs valid\n"
                + tag + " OK [" + (this.nurLesen ? "READ-ONLY" : "READ-WRITE") + "] " + befehl + " completed");
    }

    private void fetch(String tag, String rest, boolean uid) {
        if (!this.ausgewaehlt) {
            this.antworten(tag + " NO no mailbox selected");
            return;
        }
        String[] a = rest.split("\\s+", 2);
        if (a.length < 2) {
            this.antworten(tag + " BAD FETCH needs a sequence set and data items");
            return;
        }
        List<Email> p = this.postfach();
        List<Integer> auswahl = this.auswahl(a[0], p, uid);
        if (auswahl == null) {
            this.antworten(tag + " BAD invalid sequence set");
            return;
        }
        String items = a[1].toUpperCase(Locale.ROOT);
        boolean text = items.contains("BODY[]") || items.contains("RFC822") || items.contains("BODY.PEEK[]");
        boolean peek = items.contains("BODY.PEEK[]");
        boolean flags = items.contains("FLAGS") || items.contains("ALL") || items.contains("FAST") || items.contains("FULL");
        StringBuilder b = new StringBuilder();
        for (int i : auswahl) {
            Email e = p.get(i);
            if (text && !peek && !this.nurLesen) {
                e.setNeu(false);
            }
            b.append("* ").append(i + 1).append(" FETCH (");
            List<String> teile = new ArrayList<>();
            if (uid || items.contains("UID")) {
                teile.add("UID " + this.emailServer.uid(this.konto, e));
            }
            if (flags) {
                teile.add("FLAGS (" + flags(e) + ")");
            }
            if (text) {
                String inhalt = e.toString();
                teile.add("BODY[] {" + inhalt.length() + "}\n" + inhalt);
            }
            b.append(String.join(" ", teile)).append(")\n");
        }
        b.append(tag).append(" OK ").append(uid ? "UID " : "").append("FETCH completed");
        this.emailServer.kontenSpeichern();
        this.antworten(b.toString());
    }

    private void store(String tag, String rest, boolean uid) {
        if (!this.ausgewaehlt) {
            this.antworten(tag + " NO no mailbox selected");
            return;
        }
        if (this.nurLesen) {
            this.antworten(tag + " NO mailbox is read-only");
            return;
        }
        String[] a = rest.split("\\s+", 3);
        if (a.length < 3) {
            this.antworten(tag + " BAD STORE needs a sequence set, an action and flags");
            return;
        }
        List<Email> p = this.postfach();
        List<Integer> auswahl = this.auswahl(a[0], p, uid);
        if (auswahl == null) {
            this.antworten(tag + " BAD invalid sequence set");
            return;
        }
        String aktion = a[1].toUpperCase(Locale.ROOT);
        String f = a[2].toUpperCase(Locale.ROOT);
        boolean still = aktion.endsWith(".SILENT");
        boolean dazu = aktion.startsWith("+");
        boolean weg = aktion.startsWith("-");
        boolean geloescht = f.contains("\\DELETED");
        boolean gelesen = f.contains("\\SEEN");
        StringBuilder b = new StringBuilder();
        for (int i : auswahl) {
            Email e = p.get(i);
            if (dazu) {
                if (geloescht) {
                    e.setDelete(true);
                }
                if (gelesen) {
                    e.setNeu(false);
                }
            } else if (weg) {
                if (geloescht) {
                    e.setDelete(false);
                }
                if (gelesen) {
                    e.setNeu(true);
                }
            } else {
                e.setDelete(geloescht);
                e.setNeu(!gelesen);
            }
            if (!still) {
                b.append("* ").append(i + 1).append(" FETCH (").append(uid ? "UID " + this.emailServer.uid(this.konto, e) + " " : "")
                        .append("FLAGS (").append(flags(e)).append("))\n");
            }
        }
        b.append(tag).append(" OK ").append(uid ? "UID " : "").append("STORE completed");
        this.antworten(b.toString());
    }

    /** removes the mails marked \Deleted; returns the untagged EXPUNGE lines */
    private String expunge(boolean melden) {
        StringBuilder b = new StringBuilder();
        List<Email> liste = this.konto.getNachrichten();
        synchronized (liste) {
            for (int i = liste.size() - 1; i >= 0; i--) {
                if (liste.get(i).getDelete()) {
                    liste.remove(i);
                    if (melden) {
                        b.append("* ").append(i + 1).append(" EXPUNGE\n");
                    }
                }
            }
        }
        this.emailServer.kontenSpeichern();
        this.emailServer.benachrichtigeBeobachter();
        return b.toString();
    }

    private List<Email> postfach() {
        if (this.konto == null) {
            return new ArrayList<>();
        }
        List<Email> liste = this.konto.getNachrichten();
        synchronized (liste) {
            return new ArrayList<>(liste);
        }
    }

    private static String flags(Email e) {
        List<String> f = new ArrayList<>();
        if (!e.getNeu()) {
            f.add("\\Seen");
        }
        if (e.getDelete()) {
            f.add("\\Deleted");
        }
        return String.join(" ", f);
    }

    /** indices (0-based) of a sequence set like 1:*, 2,4:5 or a UID set; null if it cannot be read */
    private List<Integer> auswahl(String menge, List<Email> p, boolean uid) {
        List<Integer> ergebnis = new ArrayList<>();
        int max = p.size();
        int[] nummern = new int[max];
        int hoechste = 0;
        for (int i = 0; i < max; i++) {
            nummern[i] = uid ? this.emailServer.uid(this.konto, p.get(i)) : i + 1;
            hoechste = Math.max(hoechste, nummern[i]);
        }
        try {
            for (String teil : menge.split(",")) {
                int von;
                int bis;
                if (teil.contains(":")) {
                    String[] vb = teil.split(":");
                    von = vb[0].equals("*") ? hoechste : Integer.parseInt(vb[0]);
                    bis = vb[1].equals("*") ? hoechste : Integer.parseInt(vb[1]);
                } else {
                    von = teil.equals("*") ? hoechste : Integer.parseInt(teil);
                    bis = von;
                }
                if (von > bis) {
                    int x = von;
                    von = bis;
                    bis = x;
                }
                for (int i = 0; i < max; i++) {
                    if (nummern[i] >= von && nummern[i] <= bis && !ergebnis.contains(i)) {
                        ergebnis.add(i);
                    }
                }
            }
        } catch (RuntimeException e) {
            return null;
        }
        ergebnis.sort(null);
        return ergebnis;
    }

    /** arguments separated by spaces, "quoted" arguments may contain spaces */
    static String[] argumente(String s) {
        List<String> a = new ArrayList<>();
        StringBuilder b = new StringBuilder();
        boolean inAnf = false;
        boolean hat = false;
        for (char c : s.toCharArray()) {
            if (c == '"') {
                inAnf = !inAnf;
                hat = true;
            } else if (Character.isWhitespace(c) && !inAnf) {
                if (hat || b.length() > 0) {
                    a.add(b.toString());
                    b.setLength(0);
                    hat = false;
                }
            } else {
                b.append(c);
            }
        }
        if (hat || b.length() > 0) {
            a.add(b.toString());
        }
        return a.toArray(new String[0]);
    }

    private void antworten(String daten) {
        this.emailServer.benachrichtigeBeobachter(this.socket.holeZielIPAdresse() + "> " + daten);
        this.sendeNachricht(daten);
    }

    private void schliesseSocket() {
        if (this.socket != null) {
            this.socket.schliessen();
            this.socket = null;
            this.beenden();
        }
    }
}
