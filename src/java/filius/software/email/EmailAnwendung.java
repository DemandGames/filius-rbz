/*
 * E-mail program of Filius 2.9.3 (decompiled), changed for Filius (RBZ Wirtschaft Kiel Version):
 * mails are read with IMAP instead of POP3 (see IMAPClient). The inbox shows the mails on the server;
 * deleting a mail in the inbox deletes it on the server, too. Accounts with the old POP3 port 110 use 143.
 * Version 30: deleted mails go into the folder Papierkorb (trash, on this computer) and are only gone when they
 * are deleted there; "abmelden" (log out) removes the account and the mails shown, a new account must be set up.
 */
package filius.software.email;

import filius.rahmenprogramm.EingabenUeberpruefung;
import filius.software.Anwendung;
import filius.software.email.Email;
import filius.software.email.EmailKonto;
import filius.software.email.EmailUtils;
import filius.software.email.Kontakt;
import filius.software.email.IMAPClient;
import filius.software.email.SMTPClient;
import filius.software.system.Datei;
import filius.software.system.Dateisystem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailAnwendung
extends Anwendung {
    private static Logger LOG = LoggerFactory.getLogger(EmailAnwendung.class);
    private Vector<Kontakt> adressbuch = new Vector();
    private List<Email> erstellteNachrichten = new ArrayList<Email>();
    private List<Email> empfangeneNachrichten = new ArrayList<Email>();
    private List<Email> gesendeteNachrichten = new ArrayList<Email>();
    /** deleted mails (inbox or sent) until they are deleted in the trash, too */
    private transient List<Email> papierkorb = new ArrayList<Email>();
    private IMAPClient imapClient;
    private SMTPClient smtpclient;
    private EmailKonto konto;

    @Override
    public void starten() {
        super.starten();
        this.imapClient = new IMAPClient(this);
        this.imapClient.starten();
        this.smtpclient = new SMTPClient(this);
        this.smtpclient.starten();
    }

    @Override
    public void beenden() {
        super.beenden();
        if (this.imapClient != null) {
            this.imapClient.beenden();
        }
        if (this.smtpclient != null) {
            this.smtpclient.beenden();
        }
    }

    public void versendeEmail(String remoteServerIP, Email email, String absender) {
        Object rcpts = EmailUtils.addressEntryListToString(email.getEmpfaenger());
        if (email.getCc().size() > 0) {
            rcpts = (String)rcpts + ",";
            rcpts = (String)rcpts + EmailUtils.addressEntryListToString(email.getCc());
        }
        if (email.getBcc().size() > 0) {
            rcpts = (String)rcpts + ",";
            rcpts = (String)rcpts + EmailUtils.addressEntryListToString(email.getBcc());
        }
        this.smtpclient.versendeEmail(remoteServerIP, email, absender, (String)rcpts);
    }

    /** IMAP port; accounts of the original Filius still have the POP3 port 110 */
    public static String imapPort(String port) {
        return port == null || port.trim().isEmpty() || "110".equals(port.trim()) ? "143" : port.trim();
    }

    /** "Retrieve emails": reads the mailbox on the server with IMAP (the parameters keep their old order) */
    public void emailsAbholenEmails(String benutzername, String passwort, String imapPort, String imapServer) {
        String port = imapPort(imapPort);
        if (this.konto != null && !port.equals(this.konto.getPop3port())) {
            this.konto.setPop3port(port);
        }
        this.imapClient.emailsHolen(imapServer, port, benutzername, passwort);
    }

    /** the inbox after reading the mailbox: exactly the mails on the server */
    public void posteingangUebernehmen(List<Email> eingang) {
        this.empfangeneNachrichten = new ArrayList<Email>(eingang);
    }

    public boolean kontaktHinzufuegen(String name, String vorname, String strasse, int hausnr, int plz, String wohnort, String email, String telefon) {
        if (EingabenUeberpruefung.isGueltig(name, EingabenUeberpruefung.musterMindEinZeichen) && EingabenUeberpruefung.isGueltig(vorname, EingabenUeberpruefung.musterMindEinZeichen) && EingabenUeberpruefung.isGueltig(email, EingabenUeberpruefung.musterEmailAdresse)) {
            try {
                Kontakt kontaktNeu = new Kontakt();
                kontaktNeu.setName(name);
                kontaktNeu.setVorname(vorname);
                kontaktNeu.setStrasse(strasse);
                kontaktNeu.setHausnr(hausnr);
                kontaktNeu.setPlz(plz);
                kontaktNeu.setWohnort(wohnort);
                kontaktNeu.setEmail(email);
                kontaktNeu.setTelefon(telefon);
                this.getAdressbuch().add(kontaktNeu);
            }
            catch (Exception e) {
                LOG.debug("", e);
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    public boolean kontaktLoeschen(String name, String vorname, String email) {
        if (EingabenUeberpruefung.isGueltig(name, EingabenUeberpruefung.musterMindEinZeichen) && EingabenUeberpruefung.isGueltig(vorname, EingabenUeberpruefung.musterMindEinZeichen) && EingabenUeberpruefung.isGueltig(email, EingabenUeberpruefung.musterEmailAdresse)) {
            for (Kontakt kontakt : this.adressbuch) {
                if (!email.equalsIgnoreCase(kontakt.getEmail()) || !name.equals(kontakt.getName()) || !vorname.equals(kontakt.getVorname())) continue;
                this.adressbuch.remove(kontakt);
                return true;
            }
        }
        return false;
    }

    public void speichern() {
        Datei datei = new Datei();
        datei.setDateiInhalt(this.konto.toString());
        datei.setName("konten.txt");
        datei.setDateiTyp("text/txt");
        this.getSystemSoftware().getDateisystem().speicherDatei(this.getSystemSoftware().getDateisystem().getRoot(), datei);
    }

    public void laden() {
        Datei datei = this.getSystemSoftware().getDateisystem().holeDatei(this.getSystemSoftware().getDateisystem().getRoot(), "konten.txt");
        if (datei != null) {
            String kontenString = datei.getDateiInhalt();
            String[] konten = kontenString.split("\n");
            for (int i = 0; i < konten.length; ++i) {
                this.konto = new EmailKonto(konten[i]);
                this.konto.setPop3port(imapPort(this.konto.getPop3port()));
            }
        }
    }

    public IMAPClient holeIMAPClient() {
        return this.imapClient;
    }

    public Vector<Kontakt> getAdressbuch() {
        return this.adressbuch;
    }

    public void setAdressbuch(Vector<Kontakt> adressbuch) {
        this.adressbuch = adressbuch;
    }

    public List<Email> getEmpfangeneNachrichten() {
        return this.empfangeneNachrichten;
    }

    /** deletes a mail of the inbox, here and (with IMAP) on the server */
    public void removeReceivedMail(int index) {
        if (this.empfangeneNachrichten.size() > index && 0 <= index) {
            Email email = this.empfangeneNachrichten.remove(index);
            this.papierkorb.add(email);
            if (this.konto != null && this.imapClient != null) {
                this.imapClient.emailLoeschen(this.konto.getPop3server(), imapPort(this.konto.getPop3port()), this.konto.getBenutzername(),
                        this.konto.getPasswort(), email);
            }
        }
    }

    public void setEmpfangeneNachrichten(List<Email> empfangeneNachrichten) {
        this.empfangeneNachrichten = empfangeneNachrichten;
    }

    public List<Email> getErstellteNachrichten() {
        return this.erstellteNachrichten;
    }

    public void setErstellteNachrichten(List<Email> erstellteNachrichten) {
        this.erstellteNachrichten = erstellteNachrichten;
    }

    public List<Email> getGesendeteNachrichten() {
        return this.gesendeteNachrichten;
    }

    public void addGesendeteNachricht(Email gesendeteNachricht) {
        if (!this.gesendeteNachrichten.contains(gesendeteNachricht)) {
            this.gesendeteNachrichten.add(gesendeteNachricht);
        }
    }

    public void removeSentMail(int index) {
        if (this.gesendeteNachrichten.size() > index && 0 <= index) {
            this.papierkorb.add(this.gesendeteNachrichten.remove(index));
        }
    }

    /** the folder Papierkorb (trash) of this computer */
    public List<Email> holePapierkorb() {
        return this.papierkorb;
    }

    /** deletes a mail of the trash for good */
    public void removeTrashMail(int index) {
        if (this.papierkorb.size() > index && 0 <= index) {
            this.papierkorb.remove(index);
        }
    }

    /** log out: the account and the mails shown are gone; a new account must be set up */
    public void abmelden() {
        this.konto = null;
        this.empfangeneNachrichten.clear();
        this.gesendeteNachrichten.clear();
        this.papierkorb.clear();
        Dateisystem ds = this.getSystemSoftware().getDateisystem();
        if (ds.holeDatei(ds.getRoot(), "konten.txt") != null) {
            String root = ds.holeRootPfad();
            ds.deleteFile((root.endsWith(Dateisystem.FILE_SEPARATOR) ? root : root + Dateisystem.FILE_SEPARATOR) + "konten.txt");
        }
    }

    public void setGesendeteNachrichten(List<Email> gesendeteNachrichten) {
        this.gesendeteNachrichten = gesendeteNachrichten;
    }

    public Map<String, EmailKonto> holeKontoListe() {
        if (this.konto != null) {
            return Collections.singletonMap(this.konto.getBenutzername(), this.konto);
        }
        return Collections.emptyMap();
    }

    public void setzeKonto(EmailKonto konto) {
        this.konto = konto;
    }

    public void setzeKontoListe(Map<String, EmailKonto> kontoListe) {
        if (!kontoListe.isEmpty()) {
            this.konto = kontoListe.values().toArray(new EmailKonto[kontoListe.size()])[0];
        }
    }
}

