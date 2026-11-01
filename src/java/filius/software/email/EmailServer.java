/*
 * Mail server of Filius 2.9.3 (decompiled), changed for Filius (RBZ Wirtschaft Kiel Version):
 * mailboxes are read with IMAP (port 143) instead of POP3 (port 110). Mails stay on the server
 * until a client deletes them (STORE \\Deleted and EXPUNGE). UIDs are given per run of the server.
 */
package filius.software.email;

import filius.exception.CreateAccountException;
import filius.exception.DeleteAccountException;
import filius.rahmenprogramm.EingabenUeberpruefung;
import filius.rahmenprogramm.I18n;
import filius.software.Anwendung;
import filius.software.email.AddressEntry;
import filius.software.email.Email;
import filius.software.email.EmailKonto;
import filius.software.email.EmailUtils;
import filius.software.email.POP3Server;
import filius.software.email.IMAPServer;
import java.util.IdentityHashMap;
import java.util.Map;
import filius.software.email.SMTPClient;
import filius.software.email.SMTPServer;
import filius.software.system.Datei;
import filius.software.system.Dateisystem;
import filius.software.system.InternetKnotenBetriebssystem;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.tree.DefaultMutableTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailServer
extends Anwendung
implements I18n {
    private static Logger LOG = LoggerFactory.getLogger(EmailServer.class);
    public static final String LINE_SEPARATOR = "----";
    private List<EmailKonto> listeBenutzerkonten = new LinkedList<EmailKonto>();
    private String mailDomain = "filius.de";
    /** IMAP port of the mail server */
    public static final int IMAP_PORT = 143;
    private IMAPServer imap;
    /** UID of every mail the IMAP server has seen in this run, and the next UID per mailbox */
    private final Map<Email, Integer> uids = new IdentityHashMap<Email, Integer>();
    private final Map<EmailKonto, Integer> naechsteUid = new IdentityHashMap<EmailKonto, Integer>();
    private int uidValidity = 1;
    private SMTPServer smtp;
    private boolean aktiv = false;
    private DefaultMutableTreeNode verzeichnis;

    public boolean isAktiv() {
        return this.aktiv;
    }

    public void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
        if (this.imap != null) {
            this.imap.setAktiv(aktiv);
        }
        if (this.smtp != null) {
            this.smtp.setAktiv(aktiv);
        }
    }

    @Override
    public void beenden() {
        LOG.debug("INVOKED (" + this.hashCode() + ", T" + this.getId() + ") " + String.valueOf(this.getClass()) + " (EmailServer), beenden()");
        super.beenden();
        this.kontenSpeichern();
        if (this.imap != null) {
            this.imap.beenden();
        }
        if (this.smtp != null) {
            this.smtp.beenden();
        }
    }

    @Override
    public void starten() {
        LOG.debug("INVOKED (" + this.hashCode() + ", T" + this.getId() + ") " + String.valueOf(this.getClass()) + " (EmailServer), starten()");
        super.starten();
        Datei konten = this.getSystemSoftware().getDateisystem().holeDatei(this.verzeichnis, "konten.txt");
        if (konten == null) {
            konten = new Datei("konten.txt", "txt", "");
            this.getSystemSoftware().getDateisystem().speicherDatei(this.verzeichnis, konten);
        }
        this.kontenLaden();
        synchronized (this.uids) {
            this.uids.clear();
            this.naechsteUid.clear();
            this.uidValidity = (int) (System.currentTimeMillis() / 1000L % 1000000000L);
        }
        this.imap = new IMAPServer(IMAP_PORT, this);
        this.imap.setSystemSoftware(this.getSystemSoftware());
        this.imap.setAktiv(this.aktiv);
        this.imap.starten();
        this.smtp = new SMTPServer(25, this);
        this.smtp.setSystemSoftware(this.getSystemSoftware());
        this.smtp.setAktiv(this.aktiv);
        this.smtp.starten();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean benutzerHinzufuegen(String benutzername, String passwort, String nachname, String vorname) throws CreateAccountException {
        try {
            EmailKonto konto = new EmailKonto();
            for (EmailKonto kontoAusListe : this.getListeBenutzerkonten()) {
                if (kontoAusListe.getBenutzername().equalsIgnoreCase(benutzername)) {
                    return false;
                }
                if (kontoAusListe.getBenutzername().equals(benutzername) && kontoAusListe.getNachname().equalsIgnoreCase(nachname) && kontoAusListe.getVorname().equals(vorname)) {
                    return false;
                }
                if (!kontoAusListe.getNachname().equals("") && !kontoAusListe.getVorname().equals("")) continue;
                return false;
            }
            konto.setBenutzername(benutzername);
            konto.setPasswort(passwort);
            konto.setNachname(nachname);
            konto.setVorname(vorname);
            List<EmailKonto> list = this.getListeBenutzerkonten();
            synchronized (list) {
                this.getListeBenutzerkonten().add(konto);
            }
            this.benachrichtigeBeobachter();
        }
        catch (Exception e) {
            throw new CreateAccountException("-ERR This account could not be created. Please try again!");
        }
        this.kontenSpeichern();
        return true;
    }

    public boolean kontoLoeschen(String benutzername, String passwort) throws DeleteAccountException {
        try {
            for (EmailKonto konto : this.getListeBenutzerkonten()) {
                if (!konto.getBenutzername().equals(benutzername) || !konto.getPasswort().equals(passwort)) continue;
                return this.kontoLoeschen(konto);
            }
        }
        catch (Exception e) {
            throw new DeleteAccountException(messages.getString("sw_emailserver_msg1"));
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean kontoLoeschen(EmailKonto konto) {
        List<EmailKonto> list = this.getListeBenutzerkonten();
        synchronized (list) {
            this.getListeBenutzerkonten().remove(konto);
        }
        this.kontenSpeichern();
        this.benachrichtigeBeobachter();
        return true;
    }

    public EmailKonto sucheKonto(String benName, String passwd) {
        for (EmailKonto konto : this.getListeBenutzerkonten()) {
            if (!konto.getBenutzername().equalsIgnoreCase(benName) || !konto.getPasswort().equals(passwd)) continue;
            return konto;
        }
        return null;
    }

    public EmailKonto sucheKonto(String benName) {
        for (EmailKonto tempKonto : this.listeBenutzerkonten) {
            if (!tempKonto.getBenutzername().equalsIgnoreCase(benName)) continue;
            return tempKonto;
        }
        return null;
    }

    public void kontenSpeichern() {
        String tmp = this.listeBenutzerkontenZuString(this.listeBenutzerkonten);
        Datei konten = this.getSystemSoftware().getDateisystem().holeDatei(this.verzeichnis, "konten.txt");
        konten.setDateiInhalt(tmp);
    }

    private String nltobr(String originalText) {
        return originalText.replaceAll("\n", "|");
    }

    private String brtonl(String originalText) {
        return originalText.replaceAll("\\|", "\n");
    }

    private String undoSpecialChar(String escapeStr) {
        String result = escapeStr.replaceAll("&&00036&&", "\\$");
        result = result.replaceAll("&&00059&&", ";");
        result = result.replaceAll("&&00124&&", "\\|");
        result = result.replaceAll("&&00035&&", "\\#");
        return result;
    }

    private String replaceSpecialChar(String mailtext) {
        String result = mailtext.replaceAll("\\$", "&&00036&&");
        result = result.replaceAll(";", "&&00059&&");
        result = result.replaceAll("\\|", "&&00124&&");
        result = result.replaceAll("#", "&&00035&&");
        return result;
    }

    private String listeBenutzerkontenZuString(List<EmailKonto> benutzerkonten) {
        String ergebnis = "";
        for (EmailKonto konto : benutzerkonten) {
            String emailStr = "";
            for (Email email : konto.getNachrichten()) {
                emailStr = emailStr + "#" + String.valueOf(email.getAbsender() != null ? email.getAbsender() : "") + "$" + EmailUtils.addressEntryListToString(email.getEmpfaenger()) + "$" + EmailUtils.addressEntryListToString(email.getCc()) + "$" + EmailUtils.addressEntryListToString(email.getBcc()) + "$" + email.getDateReceived() + "$" + (email.getBetreff() != null ? email.getBetreff() : "") + "$" + this.nltobr(this.replaceSpecialChar(email.getText()));
            }
            ergebnis = ergebnis + konto.getBenutzername() + ";" + this.mailDomain + ";" + konto.getPasswort() + ";" + konto.getNachname() + ";" + konto.getVorname() + ";" + emailStr + "\n";
        }
        return ergebnis;
    }

    private List<EmailKonto> stringZuListeBenutzerkonten(String speicherung) {
        LinkedList<EmailKonto> temp = new LinkedList<EmailKonto>();
        StringTokenizer speicherungTokenizer = new StringTokenizer(speicherung, "\n");
        while (speicherungTokenizer.hasMoreTokens()) {
            EmailKonto konto = new EmailKonto();
            String emailKontoInString = speicherungTokenizer.nextToken();
            StringTokenizer emailKontoInStringTokenizer = new StringTokenizer(emailKontoInString, ";");
            konto.setBenutzername(emailKontoInStringTokenizer.nextToken());
            emailKontoInStringTokenizer.nextToken();
            konto.setPasswort(emailKontoInStringTokenizer.nextToken());
            konto.setNachname(emailKontoInStringTokenizer.nextToken());
            konto.setVorname(emailKontoInStringTokenizer.nextToken());
            if (emailKontoInStringTokenizer.hasMoreTokens()) {
                String nachrichten = emailKontoInStringTokenizer.nextToken();
                try {
                    String[] nachrichtenArray = nachrichten.split("#");
                    for (int i = 0; i < nachrichtenArray.length; ++i) {
                        String[] strArray = nachrichtenArray[i].split("\\$");
                        if (strArray.length != 7) continue;
                        Email email = new Email();
                        email.setAbsender(strArray[0]);
                        email.setEmpfaenger(EmailUtils.stringToAddressEntryList(strArray[1]));
                        email.setCc(EmailUtils.stringToAddressEntryList(strArray[2]));
                        email.setBcc(EmailUtils.stringToAddressEntryList(strArray[3]));
                        email.setDateReceived(strArray[4]);
                        email.setBetreff(strArray[5]);
                        email.setText(this.undoSpecialChar(this.brtonl(strArray[6])));
                        konto.getNachrichten().add(email);
                    }
                }
                catch (Exception e) {
                    LOG.debug("", e);
                }
            }
            temp.add(konto);
        }
        return temp;
    }

    public void kontenLaden() {
        Datei konten = this.getSystemSoftware().getDateisystem().holeDatei(this.verzeichnis, "konten.txt");
        if (konten != null) {
            this.setListeBenutzerkonten(this.stringZuListeBenutzerkonten(konten.getDateiInhalt()));
        } else {
            LOG.error("Konten laden fehlgeschlagen");
        }
    }

    @Override
    public void setSystemSoftware(InternetKnotenBetriebssystem bs) {
        super.setSystemSoftware(bs);
        this.getSystemSoftware().getDateisystem().erstelleVerzeichnis(this.getSystemSoftware().getDateisystem().getRoot(), "mailserver");
        this.verzeichnis = Dateisystem.verzeichnisKnoten(this.getSystemSoftware().getDateisystem().getRoot(), "mailserver");
    }

    public synchronized void setListeBenutzerkonten(List<EmailKonto> listeBenutzerkonten) {
        this.listeBenutzerkonten = listeBenutzerkonten;
    }

    /** kept for project files of the original Filius; there is no POP3 server any more */
    public void setPOP3Server(POP3Server pop3) {
    }

    public void setSMTPServer(SMTPServer smtp) {
        this.smtp = smtp;
    }

    public synchronized List<EmailKonto> getListeBenutzerkonten() {
        return this.listeBenutzerkonten;
    }

    /** kept for project files of the original Filius; there is no POP3 server any more */
    public POP3Server getPOP3Server() {
        return null;
    }

    public IMAPServer holeImap() {
        return this.imap;
    }

    /** UID of a mail in a mailbox; a mail gets its UID when the IMAP server sees it for the first time */
    public int uid(EmailKonto konto, Email email) {
        synchronized (this.uids) {
            Integer uid = this.uids.get(email);
            if (uid == null) {
                int naechste = this.naechsteUid.getOrDefault(konto, 1);
                uid = naechste;
                this.uids.put(email, uid);
                this.naechsteUid.put(konto, naechste + 1);
            }
            return uid;
        }
    }

    public int uidNext(EmailKonto konto) {
        synchronized (this.uids) {
            return this.naechsteUid.getOrDefault(konto, 1);
        }
    }

    public int uidValidity() {
        synchronized (this.uids) {
            return this.uidValidity;
        }
    }

    public SMTPServer getSMTPServer() {
        return this.smtp;
    }

    /** kept for compatibility; there is no POP3 server any more */
    public POP3Server holePop3() {
        return null;
    }

    public SMTPServer holeSmtp() {
        return this.smtp;
    }

    public String getMailDomain() {
        return this.mailDomain;
    }

    public void setMailDomain(String mailDomain) {
        if (mailDomain != null && EingabenUeberpruefung.isGueltig(mailDomain, EingabenUeberpruefung.musterDomain)) {
            this.mailDomain = mailDomain;
        }
    }

    public void emailWeiterleiten(Email email, String absender, String rcpt) {
        Object[] args = new Object[]{email, absender, rcpt};
        this.ausfuehren("weiterleiten", args);
    }

    public void weiterleiten(Email email, String absender, String rcpt) {
        SMTPClient clientFuerWeiterleitung = new SMTPClient(this);
        clientFuerWeiterleitung.starten();
        clientFuerWeiterleitung.versendeEmail(null, email, absender, rcpt);
    }

    public void sendUnknownReceiverResponse(Email email, String sender, List<String> unknownRecipients) {
        if (!sender.equalsIgnoreCase("<>")) {
            Email unknownRecipientResponse = new Email();
            unknownRecipientResponse.setAbsender("noreply@" + this.mailDomain);
            unknownRecipientResponse.setEmpfaenger(Arrays.asList(email.getAbsender()));
            unknownRecipientResponse.setBetreff("Non Delivery Notification");
            StringBuilder mailBody = new StringBuilder();
            mailBody.append("The mail could not be delivered either because user or mail domain is unknown.").append("\n");
            mailBody.append("Message could not be delivered to the following recipients:\n");
            for (String unknownRecipient : unknownRecipients) {
                mailBody.append("\t").append(unknownRecipient).append("\n");
            }
            mailBody.append("\nDate: ").append(DateFormat.getInstance().format(new Date())).append("\n");
            mailBody.append("Mail-Server: ").append(this.getSystemSoftware().primaryIPAdresse()).append("\n");
            mailBody.append("\n______________________________________________________________________\n");
            mailBody.append(email.toString());
            unknownRecipientResponse.setText(mailBody.toString());
            AddressEntry senderAddress = new AddressEntry(sender);
            if (this.pruefeAufSelbeDomain(senderAddress.getMailAddress())) {
                EmailKonto empfaengerKonto = this.sucheKonto(senderAddress.getMailAddress().split("@")[0]);
                empfaengerKonto.getNachrichten().add(unknownRecipientResponse);
                this.benachrichtigeBeobachter(messages.getString("sw_smtpmitarbeiter_msg12") + " " + empfaengerKonto.getBenutzername() + " " + messages.getString("sw_smtpmitarbeiter_msg13"));
            } else {
                this.weiterleiten(unknownRecipientResponse, "<>", senderAddress.toString());
            }
        }
    }

    public boolean pruefeAufSelbeDomain(String str) {
        String[] emailAdresse = str.split("@");
        String pruefdomain = emailAdresse[1].substring(0, emailAdresse[1].length());
        return this.mailDomain.equals(pruefdomain);
    }
}

