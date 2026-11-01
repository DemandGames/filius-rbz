/*
 * Filius (RBZ Wirtschaft Kiel Version): based on GUIApplicationWebBrowserWindow of Filius 2.9.3 (GPL).
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel, 2026: Waterwolf shows web pages with a real
 * browser engine (WebKit from JavaFX), so current HTML, CSS and JavaScript work. All requests of the
 * engine travel through the simulated network of this computer (see filius.modern.web.SimHttp).
 * New: back, forward and reload buttons. If the engine is not available (or switched off in
 * config/filius.ini), the classic page view of Filius is used exactly as before.
 * HTTPS: a lock left of the address shows whether the page came encrypted (green), with a certificate
 * that is not trusted (red) or unencrypted (grey); a click on it shows the certificate.
 * The start page of Waterwolf (only it, not the web pages) follows the dark mode: dark background, light text.
 */
package filius.gui.anwendungssicht;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Observable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.FormSubmitEvent;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.htmlparser.Parser;
import org.htmlparser.Tag;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import filius.modern.Texts;
import filius.modern.Theme;
import filius.modern.tls.Tls;
import filius.modern.web.BrowserView;
import filius.modern.web.FxSupport;
import filius.modern.web.SimHttp;
import filius.rahmenprogramm.EingabenUeberpruefung;
import filius.rahmenprogramm.Information;
import filius.rahmenprogramm.SzenarioVerwaltung;
import filius.software.system.InternetKnotenBetriebssystem;
import filius.software.www.HTTPNachricht;
import filius.software.www.WebBrowser;

public class GUIApplicationWebBrowserWindow
extends GUIApplicationWindow {
    private static Logger LOG = LoggerFactory.getLogger(GUIApplicationWebBrowserWindow.class);
    private static final long serialVersionUID = 1L;
    private JPanel browserPanel = new JPanel(new BorderLayout());
    private JTextField urlFeld;
    private JEditorPane anzeigeFeld;
    private JButton goButton;
    /** browser engine; null for the classic page view */
    private BrowserView engine;
    private long browserId = -1;
    private JButton backButton;
    private JButton forwardButton;
    private JButton reloadButton;
    /** lock in front of the address */
    private JButton schloss;
    private SchlossIcon schlossIcon;
    private InternetKnotenBetriebssystem system;
    /** protocol of the page shown in the classic view (for relative links) */
    private String protokoll = "http";

    public GUIApplicationWebBrowserWindow(GUIDesktopPanel desktop, String appName) {
        super(desktop, appName);
        if (desktop.getBetriebssystem() instanceof InternetKnotenBetriebssystem) {
            this.system = (InternetKnotenBetriebssystem) desktop.getBetriebssystem();
        }
        this.add((Component)this.browserPanel, "Center");
        if (FxSupport.isAvailable() && desktop.getBetriebssystem() instanceof InternetKnotenBetriebssystem) {
            this.browserId = SimHttp.register((InternetKnotenBetriebssystem)desktop.getBetriebssystem());
            this.engine = FxSupport.createView(this.browserId);
            if (this.engine == null) {
                SimHttp.unregister(this.browserId);
                this.browserId = -1;
            }
        }
        this.browserPanel.add((Component)this.createAddressBar(), "North");
        if (this.engine != null) {
            this.initEngine();
        } else {
            this.initClassic();
        }
        // the start page follows the dark mode (web pages keep their own colours)
        final java.lang.ref.WeakReference<GUIApplicationWebBrowserWindow> ref = new java.lang.ref.WeakReference<>(this);
        filius.modern.DunkelModus.addListener(() -> {
            GUIApplicationWebBrowserWindow w = ref.get();
            if (w != null && w.startseiteSichtbar) {
                w.startseiteZeigen();
            }
        });
    }

    /** the start page of Waterwolf is shown (no web page loaded yet) */
    private boolean startseiteSichtbar = true;

    private void startseiteZeigen() {
        if (this.engine != null) {
            this.engine.loadContent(startPage());
        } else if (this.anzeigeFeld != null) {
            this.klassischeStartseite();
        }
    }

    /** the start page in the classic view: logo in the middle, light or dark */
    private void klassischeStartseite() {
        boolean dunkel = filius.modern.Theme.dunkel();
        String datei = dunkel ? "browser_waterwolf_logo_dunkel.png" : "browser_waterwolf_logo.png";
        SzenarioVerwaltung.saveStream(this.getClass().getResourceAsStream("/gfx/desktop/" + datei), Information.getInformation().getTempPfad() + datei);
        String farbe = dunkel ? "#141C25" : "#FFFFFF";
        this.anzeigeFeld.setText("<html><head><base href=\"file:bilder\"></head><body margin=\"0\" bgcolor=\"" + farbe + "\"><center><img src=\"" + datei
                + "\" align=\"top\"></center></font></body></html>");
        try {
            ((HTMLDocument)this.anzeigeFeld.getDocument()).setBase(new URL("file:" + Information.getInformation().getTempPfad()));
        } catch (MalformedURLException e1) {
            LOG.debug("", e1);
        }
        this.anzeigeFeld.setBackground(new java.awt.Color(dunkel ? 0x141C25 : 0xFFFFFF));
    }

    // ------------------------------------------------------------------ address bar

    private Box createAddressBar() {
        Box topBox = Box.createHorizontalBox();
        if (this.engine != null) {
            this.backButton = this.navButton(new NavIcon(NavIcon.BACK), e -> this.engine.back());
            this.forwardButton = this.navButton(new NavIcon(NavIcon.FORWARD), e -> this.engine.forward());
            this.reloadButton = this.navButton(new NavIcon(NavIcon.RELOAD), e -> this.engine.reload());
            this.backButton.setEnabled(false);
            this.forwardButton.setEnabled(false);
            topBox.add(this.backButton);
            topBox.add(Box.createHorizontalStrut(2));
            topBox.add(this.forwardButton);
            topBox.add(Box.createHorizontalStrut(2));
            topBox.add(this.reloadButton);
            topBox.add(Box.createHorizontalStrut(6));
        }
        this.schlossIcon = new SchlossIcon();
        this.schloss = new JButton(this.schlossIcon);
        this.schloss.putClientProperty("JButton.buttonType", "toolBarButton");
        this.schloss.setFocusable(false);
        this.schloss.setPreferredSize(new Dimension(30, 28));
        this.schloss.setMaximumSize(new Dimension(30, 28));
        this.schloss.addActionListener(e -> this.zeigeZertifikat());
        this.schloss.setVisible(false);
        topBox.add(this.schloss);
        topBox.add(Box.createHorizontalStrut(2));
        this.urlFeld = new JTextField("http://");
        this.urlFeld.setVisible(true);
        topBox.add(this.urlFeld);
        topBox.add(Box.createHorizontalStrut(5));
        this.goButton = new JButton(messages.getString("webbrowser_msg2"));
        topBox.add(this.goButton);
        topBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        ActionListener al = new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent arg0) {
                GUIApplicationWebBrowserWindow.this.go();
            }
        };
        this.goButton.addActionListener(al);
        this.urlFeld.addKeyListener(new KeyListener(){

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == 10) {
                    GUIApplicationWebBrowserWindow.this.go();
                }
            }

            @Override
            public void keyReleased(KeyEvent arg0) {
            }

            @Override
            public void keyTyped(KeyEvent arg0) {
            }
        });
        return topBox;
    }

    private JButton navButton(Icon icon, ActionListener action) {
        JButton b = new JButton(icon);
        b.putClientProperty("JButton.buttonType", "toolBarButton");
        b.setFocusable(false);
        b.setPreferredSize(new Dimension(30, 28));
        b.setMaximumSize(new Dimension(30, 28));
        b.addActionListener(action);
        return b;
    }

    private void go() {
        URL url = this.erzeugeURL(this.urlFeld.getText());
        if (this.engine != null) {
            if (url != null) {
                this.engine.load(url.toExternalForm());
            } else {
                this.urlFeld.setText("http://");
            }
        } else {
            this.abrufenWebseite(url, null);
        }
    }

    // ------------------------------------------------------------------ browser engine

    private void initEngine() {
        Component view = this.engine.component();
        JPanel holder = new JPanel(new BorderLayout());
        holder.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.LINE));
        holder.add(view, BorderLayout.CENTER);
        this.browserPanel.add((Component)holder, "Center");
        view.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                SimHttp.markActive(GUIApplicationWebBrowserWindow.this.browserId);
            }
        });
        this.engine.setListener(new BrowserView.Listener() {
            @Override
            public void locationChanged(String url) {
                if (url != null && url.startsWith("http")) {
                    GUIApplicationWebBrowserWindow.this.urlFeld.setText(url);
                    GUIApplicationWebBrowserWindow.this.schlossAktualisieren(url);
                }
            }

            @Override
            public void titleChanged(String title) {
                if (title != null && !title.isBlank()) {
                    GUIApplicationWebBrowserWindow.this.setTitle(title);
                }
            }

            @Override
            public void historyChanged(boolean canGoBack, boolean canGoForward) {
                GUIApplicationWebBrowserWindow.this.backButton.setEnabled(canGoBack);
                GUIApplicationWebBrowserWindow.this.forwardButton.setEnabled(canGoForward);
            }

            @Override
            public void busyChanged(boolean busy) {
                GUIApplicationWebBrowserWindow.this.reloadButton.setEnabled(!busy);
            }

            @Override
            public void loaded(String url, boolean errorPage) {
                if (url == null || !url.startsWith("http")) {
                    return;
                }
                GUIApplicationWebBrowserWindow.this.startseiteSichtbar = false;
                GUIApplicationWebBrowserWindow.this.urlFeld.setText(url);
                GUIApplicationWebBrowserWindow.this.schlossAktualisieren(url);
            }
        });
        this.engine.loadContent(startPage());
    }

    /** Waterwolf start page with the browser logo. */
    private static String startPage() {
        String logo = "";
        boolean dunkel = filius.modern.Theme.dunkel();
        String datei = dunkel ? "/gfx/desktop/browser_waterwolf_logo_dunkel@2x.png" : "/gfx/desktop/browser_waterwolf_logo@2x.png";
        try (java.io.InputStream in = GUIApplicationWebBrowserWindow.class.getResourceAsStream(datei)) {
            if (in != null) {
                logo = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(in.readAllBytes());
            }
        } catch (Exception e) {
            logo = "";
        }
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>html,body{height:100%;margin:0}"
                + "body{display:flex;align-items:center;justify-content:center;background:" + (dunkel ? "#141C25" : "#fff") + "}"
                + "img{width:" + 280 + "px}</style></head><body>" + (logo.isEmpty() ? "" : "<img alt=\"Waterwolf\" src=\"" + logo + "\">")
                + "</body></html>";
    }

    // ------------------------------------------------------------------ classic page view (Filius 2.9.3)

    private void initClassic() {
        Box middleBox = Box.createHorizontalBox();
        middleBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        HTMLEditorKit ek = new HTMLEditorKit();
        ek.setAutoFormSubmission(false);
        this.anzeigeFeld = new JEditorPane();
        this.anzeigeFeld.setEditorKit(ek);
        this.anzeigeFeld.setContentType("text/html");
        this.anzeigeFeld.setEditable(false);
        this.anzeigeFeld.setBorder(null);
        // web pages bring their own colours: the page stays white also in the dark mode (not the start page)
        this.klassischeStartseite();
        this.anzeigeFeld.putClientProperty("filius.eigeneFarben", Boolean.TRUE);
        this.hyperLinkListener(this.anzeigeFeld);
        this.anzeigeFeld.setVisible(true);
        JScrollPane spAnzeige = new JScrollPane(this.anzeigeFeld);
        middleBox.add(spAnzeige);
        this.browserPanel.add((Component)middleBox, "Center");
    }

    private URL erzeugeURL(String ressource) {
        int i;
        URL url = null;
        String host = null;
        String pfad = "";
        String eingabe = ressource.trim();
        String[] teilstrings = eingabe.split("/");
        if (teilstrings.length > 0 && !teilstrings[0].equalsIgnoreCase("http:") && !teilstrings[0].equalsIgnoreCase("https:") && (EingabenUeberpruefung.isGueltig(hostOhnePort(teilstrings[0]), EingabenUeberpruefung.musterDomain) || EingabenUeberpruefung.isGueltig(hostOhnePort(teilstrings[0]), EingabenUeberpruefung.musterIpAdresse))) {
            host = teilstrings[0];
            for (i = 1; i < teilstrings.length; ++i) {
                pfad = pfad + "/" + teilstrings[i];
            }
        }
        String protokoll = "http";
        if (teilstrings.length > 2 && (teilstrings[0].equalsIgnoreCase("http:") || teilstrings[0].equalsIgnoreCase("https:"))) {
            if (EingabenUeberpruefung.isGueltig(hostOhnePort(teilstrings[2]), EingabenUeberpruefung.musterDomain) || EingabenUeberpruefung.isGueltig(hostOhnePort(teilstrings[2]), EingabenUeberpruefung.musterIpAdresse)) {
                host = teilstrings[2];
            }
            if (teilstrings[0].equalsIgnoreCase("https:")) {
                protokoll = "https";
            }
            for (i = 3; i < teilstrings.length; ++i) {
                pfad = pfad + "/" + teilstrings[i];
            }
        }
        if (eingabe.endsWith("/") && !pfad.endsWith("/")) {
            pfad = pfad + "/";
        }
        if (host != null) {
            if (pfad.equals("")) {
                pfad = "/";
            }
            try {
                url = new URL(protokoll + "://" + host + pfad);
            } catch (MalformedURLException e) {
                LOG.debug("", e);
            }
        }
        return url;
    }

    private static String hostOhnePort(String host) {
        return host.replaceFirst(":\\d+$", "");
    }

    private void abrufenWebseite(URL url, String postDaten) {
        if (url != null) {
            if (postDaten == null) {
                ((WebBrowser)this.holeAnwendung()).holeWebseite(url);
            } else {
                ((WebBrowser)this.holeAnwendung()).holeWebseite(url, postDaten);
            }
            String host = url.getHost() == null || url.getHost().equals("") ? ((WebBrowser)this.holeAnwendung()).holeHost() : url.getHost();
            if (url.getPort() > 0) {
                host = host + ":" + url.getPort();
            }
            this.protokoll = url.getProtocol();
            String pfad = Tls.ohneAusnahme(url.getPath()) != null ? Tls.ohneAusnahme(url.getPath()) : url.getPath();
            this.urlFeld.setText(url.getProtocol() + "://" + host + pfad);
            this.setTitle(url.getProtocol() + "://" + host + pfad);
            this.schlossAktualisieren(this.urlFeld.getText());
        } else {
            this.urlFeld.setText("http://");
        }
    }

    /**
     * The classic page view treats every background-image value as an image address. A gradient
     * with percent values (e.g. radial-gradient(circle at 50% 0%, ...)) then makes Swing wait
     * forever for an image that can never load, which freezes Filius. Such values are hidden from
     * the classic view; real images (url(...)) stay.
     */
    static String ohneVerlaufsbilder(String quelltext) {
        if (quelltext == null) {
            return null;
        }
        return quelltext.replaceAll("(?i)background-image(\\s*:)(?!\\s*url\\()", "x-filius-background-image$1");
    }

    private void initialisiereWebseite(String quelltext) {
        this.startseiteSichtbar = false;
        this.anzeigeFeld.setBackground(new java.awt.Color(0xFFFFFF));
        quelltext = ohneVerlaufsbilder(quelltext);
        this.anzeigeFeld.setContentType("text/html");
        this.anzeigeFeld.setText(quelltext);
        Parser parser = Parser.createParser(quelltext, null);
        try {
            Tag tag;
            NodeList liste = parser.parse(new TagNameFilter("title"));
            if (liste.size() > 0 && (tag = (Tag)liste.elementAt(0)).getChildren() != null && tag.getChildren().size() > 0) {
                this.setTitle(tag.getChildren().elementAt(0).toHtml());
            }
        } catch (Exception e) {
            LOG.debug("", e);
        }
    }

    private void hyperLinkListener(JEditorPane editorPane) {
        editorPane.addHyperlinkListener(new HyperlinkListener(){

            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                URL url = null;
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    URL tmp = ((HTMLDocument)GUIApplicationWebBrowserWindow.this.anzeigeFeld.getDocument()).getBase();
                    if (e.getURL().getProtocol().equals(tmp.getProtocol())) {
                        try {
                            String pfad = e.getURL().getFile().replace(tmp.getFile(), "/");
                            url = new URL(GUIApplicationWebBrowserWindow.this.protokoll, "", pfad);
                        } catch (MalformedURLException e1) {
                            LOG.debug("", (Object)e);
                        }
                    } else {
                        url = e.getURL();
                    }
                    if (e instanceof FormSubmitEvent) {
                        String postDatenteil = "";
                        FormSubmitEvent formSubmitEvent = (FormSubmitEvent)e;
                        if (formSubmitEvent.getMethod() == FormSubmitEvent.MethodType.POST) {
                            postDatenteil = formSubmitEvent.getData();
                        }
                        GUIApplicationWebBrowserWindow.this.abrufenWebseite(url, postDatenteil);
                    } else {
                        GUIApplicationWebBrowserWindow.this.abrufenWebseite(url, null);
                    }
                }
            }
        });
    }

    @Override
    public void update(Observable arg0, Object arg1) {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + String.valueOf(this.getClass()) + " (GUIApplicationWebBrowserWindow), update(" + String.valueOf(arg0) + "," + String.valueOf(arg1) + ")");
        if (this.engine != null) {
            return;
        }
        if (arg1 == null) {
            this.anzeigeFeld.updateUI();
        } else if (arg1 instanceof HTTPNachricht) {
            if (((HTTPNachricht)arg1).getDaten() == null) {
                this.anzeigeFeld.updateUI();
            } else {
                this.initialisiereWebseite(((HTTPNachricht)arg1).getDaten());
            }
            this.schlossAktualisieren(this.urlFeld.getText());
        }
    }

    // ------------------------------------------------------------------ lock and certificate

    /** shows the lock for this address: green = HTTPS with trusted certificate, red = not trusted, grey = HTTP */
    private void schlossAktualisieren(String adresse) {
        int art = SchlossIcon.KEINS;
        String tipp = null;
        try {
            URL u = new URL(adresse);
            if ("https".equalsIgnoreCase(u.getProtocol())) {
                Tls.Status st = Tls.status(this.system, u.getHost());
                if (st != null && st.vertraut) {
                    art = SchlossIcon.SICHER;
                    tipp = Texts.get("tls.lock.secure");
                } else if (st != null) {
                    art = SchlossIcon.UNSICHER;
                    tipp = Texts.get("tls.lock.untrusted");
                }
            } else if ("http".equalsIgnoreCase(u.getProtocol())) {
                art = SchlossIcon.OFFEN;
                tipp = Texts.get("tls.lock.plain");
            }
        } catch (Exception e) {
            art = SchlossIcon.KEINS;
        }
        this.schlossIcon.art = art;
        this.schloss.setVisible(art != SchlossIcon.KEINS);
        this.schloss.setToolTipText(tipp);
        this.schloss.repaint();
    }

    /** little window below the lock: is the connection encrypted, and the certificate */
    private void zeigeZertifikat() {
        JPanel inhalt = new JPanel(new java.awt.GridBagLayout());
        inhalt.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = java.awt.GridBagConstraints.WEST;
        c.insets = new java.awt.Insets(0, 0, 8, 0);
        Tls.Status st = null;
        String host = null;
        boolean https = false;
        try {
            URL u = new URL(this.urlFeld.getText());
            host = u.getHost();
            https = "https".equalsIgnoreCase(u.getProtocol());
            st = https ? Tls.status(this.system, host) : null;
        } catch (Exception e) {
            // no address
        }
        String titel = !https ? Texts.get("tls.cert.plain") : st != null && st.vertraut ? Texts.get("tls.cert.secure") : Texts.get("tls.cert.untrusted");
        javax.swing.JLabel t = new javax.swing.JLabel(titel);
        t.setFont(Theme.font(java.awt.Font.BOLD, 14f));
        t.setForeground(!https ? Theme.INK_2 : st != null && st.vertraut ? Theme.LED_GREEN : Theme.SIGNAL_RED);
        inhalt.add(t, c);
        if (https && st != null) {
            Tls.Zertifikat z = st.zertifikat;
            String[][] zeilen = { { Texts.get("tls.f.for"), z.namenText() }, { Texts.get("tls.f.issuer"), z.aussteller },
                    { Texts.get("tls.f.valid"), z.gueltigBis }, { Texts.get("tls.f.key"), z.schluesselKurz() } };
            for (String[] zeile : zeilen) {
                c.gridy++;
                c.gridwidth = 1;
                c.gridx = 0;
                c.insets = new java.awt.Insets(2, 0, 2, 12);
                javax.swing.JLabel l = new javax.swing.JLabel(zeile[0]);
                l.setForeground(Theme.INK_3);
                inhalt.add(l, c);
                c.gridx = 1;
                c.insets = new java.awt.Insets(2, 0, 2, 0);
                javax.swing.JLabel v = new javax.swing.JLabel(zeile[1]);
                v.setForeground(Theme.INK);
                inhalt.add(v, c);
            }
            c.gridx = 0;
            c.gridy++;
            c.gridwidth = 2;
            c.insets = new java.awt.Insets(10, 0, 0, 0);
            String hinweis = st.vertraut ? Texts.get("tls.cert.ok")
                    : (st.unbekannteStelle ? Texts.get("tls.cert.unknownIssuer") + " " : "") + (st.falscherName ? Texts.format("tls.cert.wrongName", host) : "");
            javax.swing.JLabel h = new javax.swing.JLabel("<html><div style='width:300px'>" + hinweis + "</div></html>");
            h.setForeground(Theme.INK_2);
            inhalt.add(h, c);
        } else {
            c.gridy++;
            String hinweis = https ? Texts.get("tls.cert.none") : Texts.get("tls.cert.plainText");
            javax.swing.JLabel h = new javax.swing.JLabel("<html><div style='width:300px'>" + hinweis + "</div></html>");
            h.setForeground(Theme.INK_2);
            inhalt.add(h, c);
        }
        javax.swing.JPopupMenu popup = new javax.swing.JPopupMenu();
        popup.add(inhalt);
        popup.show(this.schloss, 0, this.schloss.getHeight());
    }

    /** lock in the address bar */
    private static final class SchlossIcon implements Icon {
        static final int KEINS = 0;
        static final int SICHER = 1;
        static final int UNSICHER = 2;
        static final int OFFEN = 3;
        int art = KEINS;

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (this.art == KEINS) {
                return;
            }
            Graphics2D g2 = Theme.smooth(g.create());
            try {
                g2.translate(x, y);
                java.awt.Color farbe = this.art == SICHER ? Theme.LED_GREEN : this.art == UNSICHER ? Theme.SIGNAL_RED : Theme.INK_3;
                g2.setColor(farbe);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // shackle: closed for HTTPS, open for HTTP
                Path2D bogen = new Path2D.Double();
                if (this.art == OFFEN) {
                    bogen.moveTo(5, 7.5);
                    bogen.lineTo(5, 4.5);
                    bogen.curveTo(5, 1.2, 11, 1.2, 11, 4.0);
                } else {
                    bogen.moveTo(5, 7.5);
                    bogen.lineTo(5, 4.8);
                    bogen.curveTo(5, 1.4, 11, 1.4, 11, 4.8);
                    bogen.lineTo(11, 7.5);
                }
                g2.draw(bogen);
                g2.fill(new java.awt.geom.RoundRectangle2D.Double(3, 7, 10, 8, 2.5, 2.5));
                g2.setColor(Theme.SURFACE);
                if (this.art == UNSICHER) {
                    g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(8, 9, 8, 11);
                    g2.fillOval(7, 12, 2, 2);
                } else {
                    g2.fillOval(7, 9, 2, 3);
                }
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return 16;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }
    }

    /** Small line icons for back, forward and reload. */
    private static final class NavIcon implements Icon {
        static final int BACK = 0;
        static final int FORWARD = 1;
        static final int RELOAD = 2;
        private final int kind;

        NavIcon(int kind) {
            this.kind = kind;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = Theme.smooth(g.create());
            try {
                g2.setColor(c.isEnabled() ? Theme.INK_2 : Theme.alpha(Theme.INK_3, 0.6));
                g2.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.translate(x, y);
                Path2D p = new Path2D.Double();
                if (this.kind == BACK) {
                    p.moveTo(10, 3.5);
                    p.lineTo(5, 8);
                    p.lineTo(10, 12.5);
                    g2.draw(p);
                } else if (this.kind == FORWARD) {
                    p.moveTo(6, 3.5);
                    p.lineTo(11, 8);
                    p.lineTo(6, 12.5);
                    g2.draw(p);
                } else {
                    g2.draw(new Arc2D.Double(3, 3, 10, 10, 60, 280, Arc2D.OPEN));
                    p.moveTo(10.2, 1.8);
                    p.lineTo(10.8, 4.6);
                    p.lineTo(8.0, 5.2);
                    g2.draw(p);
                }
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return 16;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }
    }
}
