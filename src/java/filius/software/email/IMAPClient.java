package filius.software.email;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import filius.rahmenprogramm.I18n;
import filius.software.clientserver.ClientAnwendung;
import filius.software.transportschicht.TCPSocket;

/**
 * IMAP client of the e-mail program (Filius RBZ Wirtschaft Kiel Version). It replaces the POP3 client of
 * Filius 2.9.3. "Retrieve emails" reads the whole mailbox on the server (LOGIN, SELECT INBOX, FETCH,
 * LOGOUT); the inbox of the program then shows exactly the mails on the server, which stay there.
 * Deleting a mail in the program deletes it on the server as well (UID STORE +FLAGS (\Deleted), EXPUNGE).
 */
public class IMAPClient extends ClientAnwendung implements I18n {

    private static final long WARTEZEIT = 60_000;
    private static final Pattern FETCH = Pattern.compile("\\* (\\d+) FETCH \\(");
    private static final Pattern UID = Pattern.compile("UID (\\d+)");
    private static final Pattern LITERAL = Pattern.compile("BODY\\[\\] \\{(\\d+)\\}\n");
    private static final Pattern EXISTS = Pattern.compile("\\* (\\d+) EXISTS");

    private final EmailAnwendung anwendung;
    private TCPSocket verbindung;
    private int tagNummer;

    public IMAPClient(EmailAnwendung anwendung) {
        this.anwendung = anwendung;
        this.setSystemSoftware(anwendung.getSystemSoftware());
    }

    /** reads the mailbox; the inbox of the program is replaced by the mails on the server */
    public void emailsHolen(String server, String port, String benutzer, String passwort) {
        this.ausfuehren("abgleich", new Object[] { server, port, benutzer, passwort });
    }

    /** deletes a mail of the inbox on the server and reads the mailbox again */
    public void emailLoeschen(String server, String port, String benutzer, String passwort, Email email) {
        this.ausfuehren("loeschung", new Object[] { server, port, benutzer, passwort, email });
    }

    void abgleich(String server, String port, String benutzer, String passwort) {
        this.sitzung(server, port, benutzer, passwort, null);
    }

    void loeschung(String server, String port, String benutzer, String passwort, Email email) {
        this.sitzung(server, port, benutzer, passwort, email);
    }

    private static final class Mail {
        int uid;
        Email email;
    }

    private void sitzung(String server, String port, String benutzer, String passwort, Email loeschen) {
        try {
            this.tagNummer = 0;
            this.verbindung = new TCPSocket(this.getSystemSoftware(), server, Integer.parseInt(port.trim()));
            this.verbindung.verbinden();
            String gruss = this.verbindung.empfangen(WARTEZEIT);
            if (gruss == null || !gruss.startsWith("* OK")) {
                throw new Exception(gruss == null ? "IMAP: no answer" : gruss);
            }
            this.befehl("LOGIN " + benutzer + " " + passwort);
            String select = this.befehl("SELECT INBOX");
            int anzahl = 0;
            Matcher m = EXISTS.matcher(select);
            if (m.find()) {
                anzahl = Integer.parseInt(m.group(1));
            }
            List<Mail> postfach = anzahl > 0 ? lesen(this.befehl("FETCH 1:* (UID FLAGS BODY[])")) : new ArrayList<>();
            if (loeschen != null) {
                String schluessel = schluessel(loeschen);
                for (Mail mail : postfach) {
                    if (schluessel(mail.email).equals(schluessel)) {
                        this.befehl("UID STORE " + mail.uid + " +FLAGS (\\Deleted)");
                        this.befehl("EXPUNGE");
                        postfach.remove(mail);
                        break;
                    }
                }
            }
            this.befehl("LOGOUT");
            List<Email> eingang = new ArrayList<>();
            for (Mail mail : postfach) {
                eingang.add(mail.email);
            }
            this.anwendung.posteingangUebernehmen(eingang);
        } catch (Exception e) {
            this.anwendung.benachrichtigeBeobachter(e);
        } finally {
            if (this.verbindung != null) {
                try {
                    this.verbindung.schliessen();
                } catch (Exception e) {
                    // already closed
                }
                this.verbindung = null;
                this.benachrichtigeBeobachter(messages.getString("sw_pop3client_msg1"));
            }
            this.anwendung.benachrichtigeBeobachter();
        }
    }

    /** sends a tagged command and returns the whole answer; NO and BAD become an exception */
    private String befehl(String befehl) throws Exception {
        String tag = "A" + (++this.tagNummer);
        this.verbindung.senden(tag + " " + befehl);
        String antwort = this.verbindung.empfangen(WARTEZEIT);
        if (antwort == null) {
            throw new Exception("IMAP: no answer");
        }
        for (String zeile : antwort.split("\n")) {
            if (zeile.startsWith(tag + " ")) {
                String status = zeile.substring(tag.length() + 1);
                if (status.toUpperCase(Locale.ROOT).startsWith("OK")) {
                    return antwort;
                }
                throw new Exception("IMAP: " + status);
            }
        }
        throw new Exception("IMAP: " + antwort);
    }

    /** the mails of a FETCH answer: "* n FETCH (UID u FLAGS (...) BODY[] {length}\n text)" */
    static List<Mail> lesen(String antwort) {
        List<Mail> liste = new ArrayList<>();
        int pos = 0;
        Matcher f = FETCH.matcher(antwort);
        while (f.find(pos)) {
            Matcher l = LITERAL.matcher(antwort);
            if (!l.find(f.end())) {
                break;
            }
            Mail mail = new Mail();
            Matcher u = UID.matcher(antwort.substring(f.end(), l.start()));
            mail.uid = u.find() ? Integer.parseInt(u.group(1)) : -1;
            int laenge = Integer.parseInt(l.group(1));
            int start = l.end();
            int ende = Math.min(antwort.length(), start + laenge);
            Email email = new Email(antwort.substring(start, ende));
            email.setNeu(true);
            mail.email = email;
            liste.add(mail);
            pos = ende;
        }
        return liste;
    }

    /** a mail is the same if sender, subject and text are the same */
    static String schluessel(Email e) {
        return String.valueOf(e.getAbsender()) + "\u0000" + String.valueOf(e.getBetreff()).trim() + "\u0000" + String.valueOf(e.getText()).trim();
    }
}
