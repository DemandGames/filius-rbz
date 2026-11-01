/*
 * Filius (RBZ Wirtschaft Kiel Version): based on GUIApplicationEmailAnwendungWindow of Filius 2.9.3
 * (decompiled, GPL). Changed: the window for writing an email is a clear form with visible input
 * fields (in the original, the fields for recipients, copy and subject had no border and could not
 * be seen on the white background); a wrong address marks the field red.
 * Version 30: tool bar with one symbol above its label for every action (Abrufen, Neu, Antworten, Löschen,
 * Konto einrichten), folders under the heading "Postfächer" (Posteingang, Gesendet, Spam, Papierkorb; deleted
 * mails go to the Papierkorb, Spam stays empty), "Angemeldet als: NAME" with "Abmelden" (removes the account,
 * a new one has to be set up), Senden/Abbrechen at the bottom right of the form, welcome text centred without
 * the letter symbol.
 */
/*
 * Decompiled with CFR 0.152.
 */
package filius.gui.anwendungssicht;

import filius.gui.JExtendedTable;
import filius.gui.anwendungssicht.GUIApplicationWindow;
import filius.gui.anwendungssicht.GUIDesktopPanel;
import filius.rahmenprogramm.EingabenUeberpruefung;
import filius.rahmenprogramm.I18n;
import filius.rahmenprogramm.ResourceUtil;
import filius.software.email.Email;
import filius.software.email.EmailAnwendung;
import filius.software.email.EmailKonto;
import filius.software.email.EmailUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Vector;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GUIApplicationEmailAnwendungWindow
extends GUIApplicationWindow {
    private static Logger LOG = LoggerFactory.getLogger(GUIApplicationEmailAnwendungWindow.class);
    private static final long serialVersionUID = 1L;
    private JTabbedPane tabbedPane;
    private JPanel gesendetPanel;
    private JPanel eingangPanel;
    private JScrollPane gesendetScroll;
    private JScrollPane eingangScroll;
    private JEditorPane emailVorschau;
    /** true while the preview shows the welcome text (its colour follows the dark mode) */
    private boolean zeigtWillkommen;

    private void willkommenZeigen() {
        String farbe = String.format("#%06X", filius.modern.Theme.INK.getRGB() & 0xFFFFFF);
        this.emailVorschau.setContentType("text/html");
        String fett = messages.getString("emailanwendung_msg4");
        this.emailVorschau.setText("<html><body><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr><td align=\"center\" valign=\"middle\" height=\""
                + Math.max(40, this.vorschauHoehe() - 30) + "\"><font face=\"" + filius.modern.Theme.FONT_FAMILY + "\" size=\"4\" color=\"" + farbe + "\">" + fett
                + "!</font></td></tr></table></body></html>");
        this.zeigtWillkommen = true;
    }
    /** height of the visible part of the preview (the welcome text stands in its middle) */
    private int vorschauHoehe() {
        java.awt.Container p = this.emailVorschau.getParent();
        return p instanceof javax.swing.JViewport ? p.getHeight() : this.emailVorschau.getHeight();
    }

    private JButton buttonMailsAbholen;
    private JButton buttonMailVerfassen;
    private JButton buttonMailAntworten;
    private JButton buttonKonten;
    private JButton buttonEmailLoeschen;
    private JButton buttonAbmelden;
    private JLabel angemeldetAls;
    private JLabel angemeldetName;
    private JPanel spamPanel;
    private JPanel papierkorbPanel;
    private DefaultTableModel spamModell = new DefaultTableModel(0, 2);
    private DefaultTableModel papierkorbModell = new DefaultTableModel(0, 2);
    private JTable papierkorbTable;
    private JProgressBar progressBar;
    private Box middleBox;
    private DefaultTableModel posteingangModell = new DefaultTableModel(0, 2);
    private DefaultTableModel gesendeteModell = new DefaultTableModel(0, 2);
    private JTable posteingangTable;
    private JTable gesendeteTable;
    private JTextField tfName;
    private JTextField tfEmailAdresse;
    /** IMAP server of the account (the account keeps the old field names pop3server/pop3port) */
    private JTextField tfPOP3Server;
    private JTextField tfPOP3Port;
    private JTextField tfSMTPServer;
    private JTextField tfSMTPPort;
    private JTextField tfBenutzername;
    private JPasswordField tfPasswort;
    private Email aktuelleMail = null;
    private int zeilenNummer;
    private int auswahlfuerloeschen;
    ListMode paa = ListMode.UNKNOWN;
    private JPanel progressPanel;
    private JPanel verfassenPanel;

    public GUIApplicationEmailAnwendungWindow(GUIDesktopPanel desktop, String appName) {
        super(desktop, appName);
        ((EmailAnwendung)this.holeAnwendung()).holeIMAPClient().hinzuBeobachter(this);
        this.initialisiereKomponenten();
        this.laden();
        this.posteingangAktualisieren();
        this.gesendeteAktualisieren();
        this.papierkorbAktualisieren();
        this.anmeldungZeigen();
    }

    private void initialisiereKomponenten() {
        this.tabbedPane = new JTabbedPane();
        this.gesendetPanel = new JPanel(new BorderLayout());
        this.eingangPanel = new JPanel(new BorderLayout());
        Box gesendetBox = Box.createHorizontalBox();
        gesendetBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        Box eingangBox = Box.createHorizontalBox();
        eingangBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        Box vorschauBox = Box.createHorizontalBox();
        vorschauBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        this.posteingangTable = new JExtendedTable((TableModel)this.posteingangModell, false);
        TableColumnModel tcm = this.posteingangTable.getColumnModel();
        tcm.getColumn(0).setHeaderValue(messages.getString("emailanwendung_msg1"));
        tcm.getColumn(1).setHeaderValue(messages.getString("emailanwendung_msg2"));
        this.eingangScroll = new JScrollPane(this.posteingangTable);
        this.posteingangTable.getSelectionModel().addListSelectionListener(new ListSelectionListener(){

            @Override
            public void valueChanged(ListSelectionEvent lse) {
                GUIApplicationEmailAnwendungWindow.this.auswahlfuerloeschen = GUIApplicationEmailAnwendungWindow.this.zeilenNummer = GUIApplicationEmailAnwendungWindow.this.posteingangTable.getSelectedRow();
                GUIApplicationEmailAnwendungWindow.this.paa = ListMode.INBOX;
                if (GUIApplicationEmailAnwendungWindow.this.zeilenNummer != -1) {
                    Email tmpEmail = ((EmailAnwendung)GUIApplicationEmailAnwendungWindow.this.holeAnwendung()).getEmpfangeneNachrichten().get(GUIApplicationEmailAnwendungWindow.this.zeilenNummer);
                    GUIApplicationEmailAnwendungWindow.this.emailVorschau.setContentType("text/plain");
                    GUIApplicationEmailAnwendungWindow.this.zeigtWillkommen = false;
                    GUIApplicationEmailAnwendungWindow.this.emailVorschau.setText(tmpEmail.getText());
                    GUIApplicationEmailAnwendungWindow.this.aktuelleMail = tmpEmail;
                    GUIApplicationEmailAnwendungWindow.this.emailVorschau.updateUI();
                }
            }
        });
        eingangBox.add(this.eingangScroll);
        this.eingangPanel.add((Component)eingangBox, "Center");
        this.gesendeteTable = new JExtendedTable((TableModel)this.gesendeteModell, false);
        TableColumnModel tcmGesendet = this.gesendeteTable.getColumnModel();
        tcmGesendet.getColumn(0).setHeaderValue(messages.getString("emailanwendung_msg3"));
        tcmGesendet.getColumn(1).setHeaderValue(messages.getString("emailanwendung_msg2"));
        this.gesendeteTable.getSelectionModel().addListSelectionListener(new ListSelectionListener(){

            @Override
            public void valueChanged(ListSelectionEvent lse) {
                GUIApplicationEmailAnwendungWindow.this.auswahlfuerloeschen = GUIApplicationEmailAnwendungWindow.this.zeilenNummer = GUIApplicationEmailAnwendungWindow.this.gesendeteTable.getSelectedRow();
                GUIApplicationEmailAnwendungWindow.this.paa = ListMode.OUTBOX;
                if (GUIApplicationEmailAnwendungWindow.this.zeilenNummer != -1) {
                    Email tmpEmail = ((EmailAnwendung)GUIApplicationEmailAnwendungWindow.this.holeAnwendung()).getGesendeteNachrichten().get(GUIApplicationEmailAnwendungWindow.this.zeilenNummer);
                    GUIApplicationEmailAnwendungWindow.this.emailVorschau.setContentType("text/plain");
                    GUIApplicationEmailAnwendungWindow.this.zeigtWillkommen = false;
                    GUIApplicationEmailAnwendungWindow.this.emailVorschau.setText(tmpEmail.getText());
                    GUIApplicationEmailAnwendungWindow.this.aktuelleMail = tmpEmail;
                    GUIApplicationEmailAnwendungWindow.this.emailVorschau.updateUI();
                }
            }
        });
        this.gesendetScroll = new JScrollPane(this.gesendeteTable);
        gesendetBox.add(this.gesendetScroll);
        this.gesendetPanel.add((Component)gesendetBox, "Center");
        this.emailVorschau = new JEditorPane();
        this.emailVorschau.setBackground(filius.modern.Theme.SURFACE);
        this.emailVorschau.setContentType("text/html");
        this.emailVorschau.setEditable(false);
        this.willkommenZeigen();
        final java.lang.ref.WeakReference<GUIApplicationEmailAnwendungWindow> selbst = new java.lang.ref.WeakReference<>(this);
        filius.modern.DunkelModus.addListener(() -> {
            GUIApplicationEmailAnwendungWindow w = selbst.get();
            if (w != null && w.zeigtWillkommen) {
                w.willkommenZeigen();
            }
        });
        JScrollPane vorschauScrollPane = new JScrollPane(this.emailVorschau);
        vorschauScrollPane.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            private int hoehe = -1;

            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int h = e.getComponent().getHeight();
                if (h != this.hoehe && GUIApplicationEmailAnwendungWindow.this.zeigtWillkommen) {
                    this.hoehe = h;
                    GUIApplicationEmailAnwendungWindow.this.willkommenZeigen();
                }
            }
        });
        vorschauScrollPane.setPreferredSize(new Dimension(300, 165));
        vorschauBox.add(vorschauScrollPane);
        this.eingangPanel.add((Component)vorschauBox, "South");
        // the folders under the heading "Postfächer": inbox, sent, spam (stays empty) and trash
        this.spamPanel = this.ordner(this.spamModell, null);
        this.papierkorbTable = new JExtendedTable((TableModel)this.papierkorbModell, false);
        this.papierkorbPanel = this.ordner(this.papierkorbModell, this.papierkorbTable);
        this.papierkorbTable.getSelectionModel().addListSelectionListener(lse -> {
            int zeile = this.papierkorbTable.getSelectedRow();
            this.auswahlfuerloeschen = this.zeilenNummer = zeile;
            this.paa = ListMode.TRASH;
            List<Email> korb = ((EmailAnwendung)this.holeAnwendung()).holePapierkorb();
            if (zeile >= 0 && zeile < korb.size()) {
                this.emailVorschau.setContentType("text/plain");
                this.zeigtWillkommen = false;
                this.emailVorschau.setText(korb.get(zeile).getText());
                this.aktuelleMail = korb.get(zeile);
            }
        });
        this.tabbedPane.addTab(filius.modern.Texts.get("mail.ordner.eingang"), this.symbol("email_ordner_posteingang"), this.eingangPanel);
        this.tabbedPane.addTab(filius.modern.Texts.get("mail.ordner.gesendet"), this.symbol("email_ordner_gesendet"), this.gesendetPanel);
        this.tabbedPane.addTab(filius.modern.Texts.get("mail.ordner.spam"), this.symbol("email_ordner_spam"), this.spamPanel);
        this.tabbedPane.addTab(filius.modern.Texts.get("mail.ordner.papierkorb"), this.symbol("email_ordner_papierkorb"), this.papierkorbPanel);
        this.tabbedPane.setTabPlacement(2);
        JLabel postfaecher = new JLabel(filius.modern.Texts.get("mail.postfaecher"));
        postfaecher.setFont(filius.modern.Theme.font(java.awt.Font.BOLD, 12.5f));
        postfaecher.setForeground(filius.modern.Theme.INK_2);
        postfaecher.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        this.tabbedPane.putClientProperty("JTabbedPane.leadingComponent", postfaecher);
        this.tabbedPane.putClientProperty("JTabbedPane.tabAreaAlignment", "leading");
        this.tabbedPane.putClientProperty("JTabbedPane.tabAlignment", "leading");
        this.tabbedPane.putClientProperty("JTabbedPane.tabWidthMode", "equal");
        this.tabbedPane.putClientProperty("JTabbedPane.tabHeight", 28);
        // one column of folders, as in a mail program (no second row)
        this.tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        Box topBox = Box.createHorizontalBox();
        this.buttonMailsAbholen = this.werkzeug("email_tb_abrufen", filius.modern.Texts.get("mail.tb.abrufen"));
        this.buttonMailsAbholen.setActionCommand("Abholen");
        this.buttonMailsAbholen.setToolTipText(messages.getString("emailanwendung_msg7"));
        topBox.add(this.buttonMailsAbholen);
        topBox.add(Box.createHorizontalStrut(2));
        this.buttonMailVerfassen = this.werkzeug("email_tb_neu", filius.modern.Texts.get("mail.tb.neu"));
        this.buttonMailVerfassen.setActionCommand("Verfassen");
        this.buttonMailVerfassen.setToolTipText(messages.getString("emailanwendung_msg8"));
        ActionListener al = new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (arg0.getActionCommand() == GUIApplicationEmailAnwendungWindow.this.buttonMailVerfassen.getActionCommand()) {
                    GUIApplicationEmailAnwendungWindow.this.emailVerfassen(null);
                }
                if (arg0.getActionCommand() == GUIApplicationEmailAnwendungWindow.this.buttonMailAntworten.getActionCommand()) {
                    GUIApplicationEmailAnwendungWindow.this.emailVerfassen(GUIApplicationEmailAnwendungWindow.this.aktuelleMail);
                }
                if (arg0.getActionCommand() == GUIApplicationEmailAnwendungWindow.this.buttonMailsAbholen.getActionCommand()) {
                    GUIApplicationEmailAnwendungWindow.this.emailsAbholen();
                }
                if (arg0.getActionCommand() == GUIApplicationEmailAnwendungWindow.this.buttonKonten.getActionCommand()) {
                    GUIApplicationEmailAnwendungWindow.this.kontoVerwalten();
                    GUIApplicationEmailAnwendungWindow.this.kontoAktualisieren();
                }
                if (arg0.getActionCommand() == GUIApplicationEmailAnwendungWindow.this.buttonEmailLoeschen.getActionCommand()) {
                    GUIApplicationEmailAnwendungWindow.this.emailLoeschen(GUIApplicationEmailAnwendungWindow.this.auswahlfuerloeschen, GUIApplicationEmailAnwendungWindow.this.paa);
                }
            }
        };
        this.buttonMailVerfassen.addActionListener(al);
        this.buttonMailsAbholen.addActionListener(al);
        topBox.add(this.buttonMailVerfassen);
        topBox.add(Box.createHorizontalStrut(2));
        this.buttonMailAntworten = this.werkzeug("email_tb_antworten", filius.modern.Texts.get("mail.tb.antworten"));
        this.buttonMailAntworten.addActionListener(al);
        this.buttonMailAntworten.setActionCommand("antworten");
        this.buttonMailAntworten.setToolTipText(messages.getString("emailanwendung_msg9"));
        topBox.add(this.buttonMailAntworten);
        topBox.add(Box.createHorizontalStrut(2));
        this.buttonEmailLoeschen = this.werkzeug("email_tb_loeschen", filius.modern.Texts.get("mail.tb.loeschen"));
        this.buttonEmailLoeschen.addActionListener(al);
        this.buttonEmailLoeschen.setActionCommand("loeschen");
        this.buttonEmailLoeschen.setToolTipText(messages.getString("emailanwendung_msg10"));
        topBox.add(this.buttonEmailLoeschen);
        topBox.add(Box.createHorizontalStrut(2));
        this.buttonKonten = this.werkzeug("email_tb_konto", filius.modern.Texts.get("mail.tb.konto"));
        this.buttonKonten.addActionListener(al);
        this.buttonKonten.setActionCommand("konten");
        this.buttonKonten.setToolTipText(messages.getString("emailanwendung_msg11"));
        topBox.add(this.buttonKonten);
        topBox.add(Box.createHorizontalGlue());
        // who is logged in, and log out
        this.angemeldetAls = new JLabel();
        this.angemeldetAls.setFont(filius.modern.Theme.font(java.awt.Font.PLAIN, 12.5f));
        this.angemeldetAls.setForeground(filius.modern.Theme.INK_2);
        this.angemeldetName = new JLabel();
        this.angemeldetName.setFont(filius.modern.Theme.font(java.awt.Font.BOLD, 12.5f));
        this.angemeldetName.setForeground(filius.modern.Theme.INK);
        this.buttonAbmelden = new JButton(filius.modern.Texts.get("mail.abmelden"));
        this.buttonAbmelden.setFocusable(false);
        this.buttonAbmelden.setMargin(new java.awt.Insets(2, 8, 2, 8));
        this.buttonAbmelden.addActionListener(e -> this.abmelden());
        Box anmeldung = Box.createVerticalBox();
        this.angemeldetAls.setAlignmentX(Component.RIGHT_ALIGNMENT);
        this.buttonAbmelden.setAlignmentX(Component.RIGHT_ALIGNMENT);
        Box zeileAngemeldet = Box.createHorizontalBox();
        zeileAngemeldet.setAlignmentX(Component.RIGHT_ALIGNMENT);
        zeileAngemeldet.add(this.angemeldetAls);
        zeileAngemeldet.add(Box.createHorizontalStrut(4));
        zeileAngemeldet.add(this.angemeldetName);
        anmeldung.add(Box.createVerticalGlue());
        anmeldung.add(zeileAngemeldet);
        anmeldung.add(Box.createVerticalStrut(4));
        anmeldung.add(this.buttonAbmelden);
        anmeldung.add(Box.createVerticalGlue());
        topBox.add(anmeldung);
        topBox.add(Box.createHorizontalStrut(6));
        topBox.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        this.anmeldungZeigen();
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.add((Component)this.tabbedPane, "Center");
        contentPane.add((Component)vorschauBox, "South");
        contentPane.add((Component)topBox, "North");
        this.add((Component)contentPane, "Center");
    }

    /** a folder with a table "sender / subject" (spam and trash) */
    private JPanel ordner(DefaultTableModel modell, JTable tabelle) {
        JTable t = tabelle != null ? tabelle : new JExtendedTable((TableModel)modell, false);
        TableColumnModel tcm = t.getColumnModel();
        tcm.getColumn(0).setHeaderValue(messages.getString("emailanwendung_msg1"));
        tcm.getColumn(1).setHeaderValue(messages.getString("emailanwendung_msg2"));
        Box box = Box.createHorizontalBox();
        box.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        box.add(new JScrollPane(t));
        JPanel p = new JPanel(new BorderLayout());
        p.add((Component)box, "Center");
        return p;
    }

    private javax.swing.Icon symbol(String name) {
        return filius.modern.HiDpiIcons.upgrade(new ImageIcon(this.getClass().getResource("/gfx/desktop/" + name + ".png")));
    }

    /** a button of the tool bar: symbol above, label below (like New, Open, Save of Filius) */
    private JButton werkzeug(String symbol, String text) {
        JButton b = new JButton(text, this.symbol(symbol));
        b.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        b.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.setFont(filius.modern.Theme.font(java.awt.Font.PLAIN, 11.5f));
        b.putClientProperty("JButton.buttonType", "toolBarButton");
        b.setMargin(new java.awt.Insets(3, 6, 3, 6));
        b.setIconTextGap(2);
        return b;
    }

    /** "logged in as ..." and the button for logging out, only with an account */
    private void anmeldungZeigen() {
        if (this.angemeldetAls == null) {
            return;
        }
        Map<String, EmailKonto> konten = ((EmailAnwendung)this.holeAnwendung()).holeKontoListe();
        if (konten.isEmpty()) {
            this.angemeldetAls.setText(filius.modern.Texts.get("mail.nicht.angemeldet"));
            this.angemeldetName.setText("");
            this.buttonAbmelden.setVisible(false);
        } else {
            this.angemeldetAls.setText(filius.modern.Texts.get("mail.angemeldet.als"));
            this.angemeldetName.setText(konten.values().iterator().next().getBenutzername());
            this.buttonAbmelden.setVisible(true);
        }
    }

    /** log out: the account is removed, a new one must be set up */
    private void abmelden() {
        ((EmailAnwendung)this.holeAnwendung()).abmelden();
        this.posteingangAktualisieren();
        this.gesendeteAktualisieren();
        this.papierkorbAktualisieren();
        this.aktuelleMail = null;
        this.paa = ListMode.UNKNOWN;
        this.willkommenZeigen();
        this.anmeldungZeigen();
    }

    private void papierkorbAktualisieren() {
        this.papierkorbModell.setRowCount(0);
        for (Email mail : ((EmailAnwendung)this.holeAnwendung()).holePapierkorb()) {
            Vector<String> v = new Vector<String>();
            String absender = mail.getAbsender() == null ? "" : (mail.getAbsender().getName() == null ? mail.getAbsender().getMailAddress() : mail.getAbsender().getName());
            v.add(absender);
            v.add(mail.getBetreff());
            this.papierkorbModell.addRow(v);
        }
    }

    public void emailLoeschen(int rowIndex, ListMode list) {
        if (ListMode.INBOX.equals((Object)list)) {
            ((EmailAnwendung)this.holeAnwendung()).removeReceivedMail(this.posteingangTable.getSelectedRow());
            --this.zeilenNummer;
            this.posteingangAktualisieren();
            this.papierkorbAktualisieren();
            this.emailVorschau.setText(" ");
            this.zeigtWillkommen = false;
            this.emailVorschau.updateUI();
        } else if (ListMode.OUTBOX.equals((Object)list)) {
            ((EmailAnwendung)this.holeAnwendung()).removeSentMail(this.gesendeteTable.getSelectedRow());
            --this.zeilenNummer;
            this.gesendeteAktualisieren();
            this.papierkorbAktualisieren();
            this.emailVorschau.setText(" ");
            this.zeigtWillkommen = false;
            this.emailVorschau.updateUI();
        } else if (ListMode.TRASH.equals((Object)list)) {
            ((EmailAnwendung)this.holeAnwendung()).removeTrashMail(this.papierkorbTable.getSelectedRow());
            --this.zeilenNummer;
            this.papierkorbAktualisieren();
            this.emailVorschau.setText(" ");
            this.zeigtWillkommen = false;
            this.emailVorschau.updateUI();
        } else {
            LOG.debug("============================================GuiAppl. Emailloeschen: Email konnte nicht geloescht werden=======================================");
        }
    }

    private String replyLayout(String text) {
        return "> " + text.replaceAll("\\n", "\n> ");
    }

    private void emailVerfassen(Email antwortAuf) {
        // Filius (RBZ Wirtschaft Kiel Version): the fields had no border and lay white on white; now a
        // form with labels on the left and visible input fields (recipients, copy, subject, text)
        this.verfassenPanel = new JPanel(new BorderLayout());
        Box topBox = Box.createHorizontalBox();
        JButton buttonSenden = new JButton(messages.getString("emailanwendung_msg13"));
        buttonSenden.setActionCommand("senden");
        buttonSenden.putClientProperty("JButton.buttonType", "default");
        buttonSenden.putClientProperty("FlatLaf.style", "background: #2E6BD8; foreground: #FFFFFF; borderWidth: 0; arc: 10");
        topBox.add(buttonSenden);
        topBox.add(Box.createHorizontalStrut(8));
        JButton cancelButton = new JButton(messages.getString("emailanwendung_msg37"));
        cancelButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent arg0) {
                GUIApplicationEmailAnwendungWindow.this.desktop.closeModularWindow();
            }
        });
        topBox.add(cancelButton);
        topBox.setBorder(BorderFactory.createEmptyBorder(2, 12, 10, 12));
        // the buttons sit at the bottom right, below the text of the mail
        Box untenBox = Box.createHorizontalBox();
        untenBox.add(Box.createHorizontalGlue());
        untenBox.add(topBox);
        this.verfassenPanel.add((Component)untenBox, "South");
        this.middleBox = Box.createVerticalBox();
        Vector<String> kontenVector = new Vector<String>();
        for (EmailKonto konto : ((EmailAnwendung)this.holeAnwendung()).holeKontoListe().values()) {
            kontenVector.addElement(konto.getBenutzername());
        }
        final JComboBox cbAbsender = new JComboBox(kontenVector);
        final JTextField anField = new JTextField();
        anField.putClientProperty("JTextField.placeholderText", filius.modern.Texts.get("mail.to.hint"));
        anField.addKeyListener(new KeyAdapter(){

            @Override
            public void keyReleased(KeyEvent e) {
                GUIApplicationEmailAnwendungWindow.this.mailPruefen(anField);
            }
        });
        final JTextField ccField = new JTextField();
        ccField.putClientProperty("JTextField.placeholderText", filius.modern.Texts.get("mail.optional"));
        ccField.addKeyListener(new KeyAdapter(){

            @Override
            public void keyReleased(KeyEvent e) {
                GUIApplicationEmailAnwendungWindow.this.mailPruefen(ccField);
            }
        });
        final JTextField bccField = new JTextField();
        bccField.putClientProperty("JTextField.placeholderText", filius.modern.Texts.get("mail.optional"));
        bccField.addKeyListener(new KeyAdapter(){

            @Override
            public void keyReleased(KeyEvent e) {
                GUIApplicationEmailAnwendungWindow.this.mailPruefen(bccField);
            }
        });
        final JTextField betreffszeile = new JTextField();
        JPanel formular = new JPanel(new java.awt.GridBagLayout());
        formular.setOpaque(false);
        formular.setAlignmentX(Component.LEFT_ALIGNMENT);
        zeile(formular, 0, filius.modern.Texts.get("mail.from"), cbAbsender);
        zeile(formular, 1, messages.getString("emailanwendung_msg14"), anField);
        zeile(formular, 2, messages.getString("emailanwendung_msg15"), ccField);
        zeile(formular, 3, messages.getString("emailanwendung_msg16"), bccField);
        zeile(formular, 4, messages.getString("emailanwendung_msg17"), betreffszeile);
        this.middleBox.add(formular);
        this.middleBox.add(Box.createVerticalStrut(8));
        final JTextArea inhaltField = new JTextArea();
        inhaltField.setLineWrap(true);
        inhaltField.setWrapStyleWord(true);
        inhaltField.setRows(5);
        this.middleBox.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
        JScrollPane inhaltScrollPane = new JScrollPane(inhaltField);
        inhaltScrollPane.setPreferredSize(new Dimension(520, 110));
        inhaltScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.middleBox.add(inhaltScrollPane);
        this.verfassenPanel.add(this.middleBox);
        if (antwortAuf != null) {
            betreffszeile.setText(messages.getString("emailanwendung_msg18") + " " + antwortAuf.getBetreff());
            inhaltField.setText("\n\n" + antwortAuf.getAbsender().toString() + " " + messages.getString("emailanwendung_msg19") + "\n" + this.replyLayout(antwortAuf.getText()));
            anField.setText(antwortAuf.getAbsender().toString());
        }
        inhaltField.requestFocus();
        inhaltField.grabFocus();
        inhaltField.setCaretPosition(0);
        ActionListener al = new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent arg0) {
                boolean eingabeFehler = false;
                Email mail = new Email();
                if (cbAbsender.getSelectedItem() == null) {
                    String msgNoAccountAvailable = messages.getString("emailanwendung_msg47");
                    GUIApplicationEmailAnwendungWindow.this.showMessageDialog(msgNoAccountAvailable);
                } else {
                    String kontoString = cbAbsender.getSelectedItem().toString();
                    EmailKonto versendeKonto = ((EmailAnwendung)GUIApplicationEmailAnwendungWindow.this.holeAnwendung()).holeKontoListe().get(kontoString);
                    mail.setAbsender(versendeKonto.getVorname() + (String)(!versendeKonto.getNachname().isEmpty() ? " " + versendeKonto.getNachname() : "") + " <" + versendeKonto.getEmailAdresse() + ">");
                    if (!GUIApplicationEmailAnwendungWindow.this.mailPruefen(anField)) {
                        eingabeFehler = true;
                    } else {
                        mail.setEmpfaenger(EmailUtils.stringToAddressEntryList(anField.getText()));
                    }
                    if (!GUIApplicationEmailAnwendungWindow.this.mailPruefen(ccField)) {
                        eingabeFehler = true;
                    } else {
                        mail.setCc(EmailUtils.stringToAddressEntryList(ccField.getText()));
                    }
                    if (!GUIApplicationEmailAnwendungWindow.this.mailPruefen(bccField)) {
                        eingabeFehler = true;
                    } else {
                        mail.setBcc(EmailUtils.stringToAddressEntryList(bccField.getText()));
                    }
                    if (eingabeFehler) {
                        GUIApplicationEmailAnwendungWindow.this.showMessageDialog(I18n.messages.getString("emailanwendung_msg20"));
                    } else if (mail.getEmpfaenger().size() == 0 && mail.getCc().size() == 0 && mail.getBcc().size() == 0) {
                        GUIApplicationEmailAnwendungWindow.this.showMessageDialog(I18n.messages.getString("emailanwendung_msg21"));
                    } else {
                        mail.setBetreff(betreffszeile.getText());
                        mail.setText(inhaltField.getText());
                        GUIApplicationEmailAnwendungWindow.this.progressBar = new JProgressBar(0, 100);
                        GUIApplicationEmailAnwendungWindow.this.progressBar.setValue(0);
                        GUIApplicationEmailAnwendungWindow.this.progressBar.setIndeterminate(true);
                        GUIApplicationEmailAnwendungWindow.this.progressBar.setStringPainted(true);
                        GUIApplicationEmailAnwendungWindow.this.middleBox.add(GUIApplicationEmailAnwendungWindow.this.progressBar);
                        GUIApplicationEmailAnwendungWindow.this.middleBox.invalidate();
                        GUIApplicationEmailAnwendungWindow.this.middleBox.validate();
                        GUIApplicationEmailAnwendungWindow.this.progressBar.setString(I18n.messages.getString("emailanwendung_msg22"));
                        ((EmailAnwendung)GUIApplicationEmailAnwendungWindow.this.holeAnwendung()).versendeEmail(versendeKonto.getSmtpserver(), mail, versendeKonto.getEmailAdresse());
                        GUIApplicationEmailAnwendungWindow.this.tabbedPane.setSelectedIndex(1);
                    }
                }
            }
        };
        buttonSenden.addActionListener(al);
        this.desktop.showModularWindow(messages.getString("emailanwendung_msg12"), this.verfassenPanel);
    }

    public void emailsAbholen() {
        if (!((EmailAnwendung)this.holeAnwendung()).holeKontoListe().isEmpty()) {
            this.progressBar = new JProgressBar(0, 100);
            this.progressBar.setValue(0);
            this.progressBar.setIndeterminate(true);
            this.progressBar.setStringPainted(true);
            this.progressPanel = new JPanel(new BorderLayout());
            this.progressPanel.add((Component)this.progressBar, "Center");
            this.progressPanel.setPreferredSize(new Dimension(500, 40));
            this.desktop.showModularWindow(messages.getString("emailanwendung_msg23"), this.progressPanel);
            for (EmailKonto aktuellesKonto : ((EmailAnwendung)this.holeAnwendung()).holeKontoListe().values()) {
                this.progressBar.setString(messages.getString("emailanwendung_msg24") + aktuellesKonto.getEmailAdresse() + ")");
                ((EmailAnwendung)this.holeAnwendung()).emailsAbholenEmails(aktuellesKonto.getBenutzername(), aktuellesKonto.getPasswort(), aktuellesKonto.getPop3port(), aktuellesKonto.getPop3server());
                this.tabbedPane.setSelectedIndex(0);
            }
        }
    }

    private void posteingangAktualisieren() {
        this.posteingangModell.setRowCount(0);
        List<Email> empfangeneNachrichten = ((EmailAnwendung)this.holeAnwendung()).getEmpfangeneNachrichten();
        for (Email neueMail : empfangeneNachrichten) {
            Vector<String> v = new Vector<String>();
            String absender = neueMail.getAbsender() == null ? "" : (neueMail.getAbsender().getName() == null ? neueMail.getAbsender().getMailAddress() : neueMail.getAbsender().getName());
            v.add(absender);
            v.add(neueMail.getBetreff());
            this.posteingangModell.addRow(v);
        }
    }

    public void gesendeteAktualisieren() {
        this.gesendeteModell.setRowCount(0);
        List<Email> gesendeteNachrichten = ((EmailAnwendung)this.holeAnwendung()).getGesendeteNachrichten();
        for (Email neueMail : gesendeteNachrichten) {
            Vector<String> v = new Vector<String>();
            v.add(neueMail.holeEmpfaengerListe());
            v.add(neueMail.getBetreff());
            this.gesendeteModell.addRow(v);
        }
    }

    private void kontoAktualisieren() {
        Map<String, EmailKonto> kontoListe = ((EmailAnwendung)this.holeAnwendung()).holeKontoListe();
        for (EmailKonto konto : kontoListe.values()) {
            this.tfName.setText(konto.getVorname() + " " + konto.getNachname());
            this.tfEmailAdresse.setText(konto.getEmailAdresse());
            this.tfPOP3Server.setText(konto.getPop3server());
            this.tfPOP3Port.setText(EmailAnwendung.imapPort(konto.getPop3port()));
            this.tfSMTPServer.setText(konto.getSmtpserver());
            this.tfSMTPPort.setText(konto.getSmtpport());
            this.tfBenutzername.setText(konto.getBenutzername());
            this.tfPasswort.setText(konto.getPasswort());
        }
        // long names: show their beginning
        for (JTextField f : new JTextField[] { this.tfName, this.tfEmailAdresse, this.tfPOP3Server, this.tfSMTPServer, this.tfBenutzername }) {
            if (f != null) {
                f.setCaretPosition(0);
            }
        }
    }

    private void kontoVerwalten() {
        Box vBox = Box.createVerticalBox();
        JLabel label = new JLabel(messages.getString("emailanwendung_msg45"));
        label.setPreferredSize(new Dimension(150, 25));
        this.tfName = new JTextField();
        this.tfName.setPreferredSize(new Dimension(260, 25));
        Box hBox = Box.createHorizontalBox();
        hBox.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        hBox.add(label);
        hBox.add(this.tfName);
        vBox.add(hBox);
        label = new JLabel(messages.getString("emailanwendung_msg26"));
        label.setPreferredSize(new Dimension(150, 25));
        this.tfEmailAdresse = new JTextField();
        this.tfEmailAdresse.setPreferredSize(new Dimension(260, 25));
        this.tfEmailAdresse.addKeyListener(new KeyAdapter(){

            @Override
            public void keyReleased(KeyEvent e) {
                GUIApplicationEmailAnwendungWindow.this.gueltigkeitPruefen(GUIApplicationEmailAnwendungWindow.this.tfEmailAdresse, EingabenUeberpruefung.musterEmailAdresse);
            }
        });
        hBox = Box.createHorizontalBox();
        hBox.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        hBox.add(label);
        hBox.add(this.tfEmailAdresse);
        vBox.add(hBox);
        label = new JLabel(messages.getString("emailanwendung_msg27"));
        label.setPreferredSize(new Dimension(150, 25));
        this.tfPOP3Server = new JTextField();
        this.tfPOP3Server.setPreferredSize(new Dimension(260, 25));
        hBox = Box.createHorizontalBox();
        hBox.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        hBox.add(label);
        hBox.add(this.tfPOP3Server);
        vBox.add(hBox);
        label = new JLabel(messages.getString("emailanwendung_msg28"));
        label.setPreferredSize(new Dimension(150, 25));
        this.tfPOP3Port = new JTextField();
        this.tfPOP3Port.setPreferredSize(new Dimension(260, 25));
        this.tfPOP3Port.setText("143");
        this.tfPOP3Port.addKeyListener(new KeyAdapter(){

            @Override
            public void keyReleased(KeyEvent e) {
                GUIApplicationEmailAnwendungWindow.this.gueltigkeitPruefen(GUIApplicationEmailAnwendungWindow.this.tfPOP3Port, EingabenUeberpruefung.musterPort);
            }
        });
        hBox = Box.createHorizontalBox();
        hBox.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        hBox.add(label);
        hBox.add(this.tfPOP3Port);
        vBox.add(hBox);
        label = new JLabel(messages.getString("emailanwendung_msg29"));
        label.setPreferredSize(new Dimension(150, 25));
        this.tfSMTPServer = new JTextField();
        this.tfSMTPServer.setPreferredSize(new Dimension(260, 25));
        hBox = Box.createHorizontalBox();
        hBox.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        hBox.add(label);
        hBox.add(this.tfSMTPServer);
        vBox.add(hBox);
        label = new JLabel(messages.getString("emailanwendung_msg30"));
        label.setPreferredSize(new Dimension(150, 25));
        this.tfSMTPPort = new JTextField();
        this.tfSMTPPort.setPreferredSize(new Dimension(260, 25));
        this.tfSMTPPort.setText("25");
        this.tfSMTPPort.addKeyListener(new KeyAdapter(){

            @Override
            public void keyReleased(KeyEvent e) {
                GUIApplicationEmailAnwendungWindow.this.gueltigkeitPruefen(GUIApplicationEmailAnwendungWindow.this.tfSMTPPort, EingabenUeberpruefung.musterPort);
            }
        });
        hBox = Box.createHorizontalBox();
        hBox.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        hBox.add(label);
        hBox.add(this.tfSMTPPort);
        vBox.add(hBox);
        label = new JLabel(messages.getString("emailanwendung_msg31"));
        label.setPreferredSize(new Dimension(150, 25));
        this.tfBenutzername = new JTextField();
        this.tfBenutzername.setPreferredSize(new Dimension(260, 25));
        this.tfBenutzername.addKeyListener(new KeyAdapter(){

            @Override
            public void keyReleased(KeyEvent e) {
                GUIApplicationEmailAnwendungWindow.this.gueltigkeitPruefen(GUIApplicationEmailAnwendungWindow.this.tfBenutzername, EingabenUeberpruefung.musterEmailBenutzername);
            }
        });
        hBox = Box.createHorizontalBox();
        hBox.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        hBox.add(label);
        hBox.add(this.tfBenutzername);
        vBox.add(hBox);
        label = new JLabel(messages.getString("emailanwendung_msg32"));
        label.setPreferredSize(new Dimension(150, 25));
        this.tfPasswort = new JPasswordField();
        this.tfPasswort.setPreferredSize(new Dimension(260, 25));
        hBox = Box.createHorizontalBox();
        hBox.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        hBox.add(label);
        hBox.add(this.tfPasswort);
        vBox.add(hBox);
        hBox = Box.createHorizontalBox();
        JButton button = new JButton(messages.getString("emailanwendung_msg33"));
        button.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                if (GUIApplicationEmailAnwendungWindow.this.kontoSpeichern()) {
                    GUIApplicationEmailAnwendungWindow.this.desktop.closeModularWindow();
                } else {
                    GUIApplicationEmailAnwendungWindow.this.showMessageDialog(I18n.messages.getString("emailanwendung_msg46"));
                }
            }
        });
        hBox.add(button);
        hBox.add(Box.createHorizontalStrut(5));
        button = new JButton(messages.getString("emailanwendung_msg37"));
        button.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                GUIApplicationEmailAnwendungWindow.this.desktop.closeModularWindow();
            }
        });
        hBox.add(button);
        vBox.add(Box.createVerticalStrut(10));
        vBox.add(hBox);
        JPanel panel = new JPanel();
        panel.add(vBox);
        JScrollPane scroller = new JScrollPane(panel);
        scroller.setPreferredSize(new Dimension(460, 330));
        scroller.setBorder(BorderFactory.createEmptyBorder());
        this.desktop.showModularWindow(messages.getString("emailanwendung_msg25"), scroller);
    }

    private boolean kontoSpeichern() {
        if (EingabenUeberpruefung.isGueltig(this.tfPOP3Port.getText(), EingabenUeberpruefung.musterPort) && EingabenUeberpruefung.isGueltig(this.tfSMTPPort.getText(), EingabenUeberpruefung.musterPort) && EingabenUeberpruefung.isGueltig(this.tfBenutzername.getText(), EingabenUeberpruefung.musterEmailBenutzername)) {
            EmailKonto konto = new EmailKonto();
            if (this.tfName.getText().trim().equals("")) {
                konto.setVorname("");
                konto.setNachname("");
            } else {
                String[] teilStrings = this.tfName.getText().split(" ");
                if (teilStrings.length == 1) {
                    konto.setVorname(this.tfName.getText().trim());
                } else if (teilStrings.length >= 2) {
                    konto.setNachname(teilStrings[teilStrings.length - 1]);
                    Object tmp = "";
                    for (int i = 0; i < teilStrings.length - 1; ++i) {
                        tmp = (String)tmp + teilStrings[i] + " ";
                    }
                    konto.setVorname(((String)tmp).trim());
                }
            }
            konto.setBenutzername(this.tfBenutzername.getText());
            konto.setPasswort(new String(this.tfPasswort.getPassword()));
            konto.setPop3port(this.tfPOP3Port.getText());
            konto.setPop3server(this.tfPOP3Server.getText());
            konto.setSmtpport(this.tfSMTPPort.getText());
            konto.setSmtpserver(this.tfSMTPServer.getText());
            konto.setEmailAdresse(this.tfEmailAdresse.getText());
            ((EmailAnwendung)this.holeAnwendung()).setzeKonto(konto);
            this.speichern();
            this.anmeldungZeigen();
            return true;
        }
        return false;
    }

    /** a row of the compose form: label on the left, the field fills the rest */
    private static void zeile(JPanel formular, int y, String text, Component feld) {
        java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
        c.gridx = 0;
        c.gridy = y;
        c.anchor = java.awt.GridBagConstraints.WEST;
        c.insets = new java.awt.Insets(2, 0, 2, 10);
        JLabel label = new JLabel(text.trim());
        label.setPreferredSize(new Dimension(64, label.getPreferredSize().height));
        formular.add(label, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = java.awt.GridBagConstraints.HORIZONTAL;
        c.insets = new java.awt.Insets(2, 0, 2, 0);
        formular.add(feld, c);
    }

    private void speichern() {
        ((EmailAnwendung)this.holeAnwendung()).speichern();
    }

    private void laden() {
        ((EmailAnwendung)this.holeAnwendung()).laden();
    }

    public void gueltigkeitPruefen(JTextField feld, Pattern pruefRegel) {
        if (EingabenUeberpruefung.isGueltig(feld.getText(), pruefRegel)) {
            feld.setForeground(EingabenUeberpruefung.farbeRichtig);
            JTextField temp = new JTextField();
            feld.setBorder(temp.getBorder());
        } else {
            feld.setForeground(EingabenUeberpruefung.farbeFalsch);
            feld.setBorder(BorderFactory.createLineBorder(EingabenUeberpruefung.farbeFalsch, 1));
        }
    }

    private boolean mailPruefen(JTextField feld) {
        boolean fehler = false;
        if (!feld.getText().trim().equals("")) {
            String[] adressen = feld.getText().split(",");
            for (int i = 0; i < adressen.length; ++i) {
                if (EingabenUeberpruefung.isGueltig(adressen[i].trim(), EingabenUeberpruefung.musterEmailAdresse)) continue;
                fehler = true;
            }
        }
        // the field keeps its normal border; a wrong address marks it red (FlatLaf outline)
        if (!fehler) {
            feld.setForeground(EingabenUeberpruefung.farbeRichtig);
            feld.putClientProperty("JComponent.outline", null);
        } else {
            feld.setForeground(EingabenUeberpruefung.farbeFalsch);
            feld.putClientProperty("JComponent.outline", "error");
        }
        return !fehler;
    }

    @Override
    public void update(Observable arg0, Object arg1) {
        this.posteingangAktualisieren();
        this.gesendeteAktualisieren();
        this.papierkorbAktualisieren();
        this.anmeldungZeigen();
        if (arg1 instanceof Exception) {
            this.showMessageDialog(((Exception)arg1).getMessage());
        }
        if (arg1 == null || arg1.equals("") || arg1 instanceof Exception) {
            this.desktop.closeModularWindow(this.progressPanel);
            this.desktop.closeModularWindow(this.verfassenPanel);
        }
    }

    private static enum ListMode {
        INBOX,
        OUTBOX,
        TRASH,
        UNKNOWN;

    }
}

