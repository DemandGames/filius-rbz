/*
 * Filius (RBZ Wirtschaft Kiel Version): based on WebServer of Filius 2.9.3 (GPL).
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel, 2026: a newly installed web server gets a start
 * page in the style of the RBZ Wirtschaft . Kiel website (index.html with CSS and JavaScript, plus
 * the logo rbz-logo.png, transparent). With modern-startseite=original in config/filius.ini it gets the original
 * Filius page instead. Existing pages in saved projects are never replaced. The start page has an
 * internal class area (folder intern) with a login; the web server checks name and password against
 * intern/zugang.txt and never hands out that file.
 * HTTPS: besides HTTP on port 80 the server can offer HTTPS on port 443 (filius.modern.tls.Tls) with a
 * certificate for the names in the settings, issued by the Filius certificate authority or self-signed.
 * HTTP can be switched off, then port 80 is closed.
 */
package filius.software.www;

import filius.modern.Prefs;
import filius.rahmenprogramm.Base64;
import filius.rahmenprogramm.Information;
import filius.rahmenprogramm.ResourceUtil;
import filius.software.clientserver.TCPServerAnwendung;
import filius.software.system.Datei;
import filius.software.system.Dateisystem;
import filius.software.system.InternetKnotenBetriebssystem;
import filius.software.transportschicht.Socket;
import filius.software.transportschicht.TCPSocket;
import filius.software.www.WebServerMitarbeiter;
import filius.software.www.WebServerPlugIn;
import filius.exception.ServerSocketException;
import filius.modern.tls.Tls;
import filius.software.transportschicht.ServerSocket;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.StringTokenizer;
import javax.swing.tree.DefaultMutableTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServer
extends TCPServerAnwendung {
    private static Logger LOG = LoggerFactory.getLogger(WebServer.class);
    private DefaultMutableTreeNode verzeichnis;
    private HashMap<String, WebServerPlugIn> plugins = new HashMap();
    private String[][] vHostArray = new String[5][2];
    private boolean useVHost = false;

    public WebServer() {
        this.port = 80;
        this.resetVHosts();
    }

    public void resetVHosts() {
        for (int i = 0; i < this.vHostArray.length; ++i) {
            this.vHostArray[i][0] = "";
            this.vHostArray[i][1] = "";
        }
        this.benachrichtigeBeobachter();
    }

    @Override
    public void setSystemSoftware(InternetKnotenBetriebssystem betriebssystem) {
        LOG.trace("INVOKED (" + this.hashCode() + ", T" + this.getId() + ") " + String.valueOf(this.getClass()) + " (WebServer), setSystemSoftware(" + String.valueOf(betriebssystem) + ")");
        super.setSystemSoftware(betriebssystem);
        this.erzeugeStandardVerzeichnis();
    }

    public void setzePlugIn(WebServerPlugIn pi) {
        this.plugins.put(pi.getPfad(), pi);
    }

    public void erzeugeIndexDatei(String dateipfad) {
        LOG.trace("INVOKED (" + this.hashCode() + ", T" + this.getId() + ") " + String.valueOf(this.getClass()) + " (WebServer), erzeugeIndexDatei(" + dateipfad + ")");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader((InputStream)new FileInputStream(dateipfad), Charset.forName("UTF-8")));){
            String input;
            StringBuffer fullFile = new StringBuffer();
            while ((input = reader.readLine()) != null) {
                fullFile.append(input + "\n");
            }
            this.erzeugeDatei("index", "html", fullFile.toString());
        } catch (Exception e) {
            LOG.debug("", e);
        }
    }

    public byte[] inputStreamToBytes(InputStream in) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        byte[] buffer = new byte[1024];
        try {
            int len;
            while ((len = in.read(buffer)) >= 0) {
                out.write(buffer, 0, len);
            }
            in.close();
            out.close();
        } catch (IOException e) {
            LOG.debug("EXCEPTION (" + this.hashCode() + ", T" + this.getId() + ") " + String.valueOf(this.getClass()) + " (WebServer):  IOException in conversion of InputStream to Byte[]");
            LOG.debug("", e);
        }
        return out.toByteArray();
    }

    private void erzeugeStandardVerzeichnis() {
        LOG.trace("INVOKED (" + this.hashCode() + ", T" + this.getId() + ") " + String.valueOf(this.getClass()) + " (WebServer), erzeugeStandardVerzeichnis()");
        Dateisystem dateisystem = this.getSystemSoftware().getDateisystem();
        dateisystem.erstelleVerzeichnis(dateisystem.getRoot(), "webserver");
        this.verzeichnis = dateisystem.verzeichnisKnoten(dateisystem.holeRootPfad() + "/webserver");
        try {
            if (!dateisystem.dateiVorhanden(this.verzeichnis, "index.html") && !this.erzeugeModerneStartseite()) {
                this.erzeugeIndexDatei(ResourceUtil.getResourcePath("tmpl/webserver_index_" + String.valueOf(Information.getInformation().getLocaleOrDefault()) + ".txt"));
                this.erzeugeDatei("splashscreen-mini", "png", Base64.encodeBytes(this.inputStreamToBytes(this.getClass().getResourceAsStream("/gfx/allgemein/splashscreen-mini.png"))));
            }
        } catch (Exception e) {
            LOG.debug("", e);
        }
    }

    /** Start page of Filius (RBZ Wirtschaft Kiel Version); false if it is switched off or its files are missing. */
    private boolean erzeugeModerneStartseite() {
        if (!Prefs.modernStartPage()) {
            return false;
        }
        InputStream html = filius.modern.Texts.englisch() ? WebServer.class.getResourceAsStream(STARTSEITE + "index_en.html") : null;
        if (html == null) {
            html = WebServer.class.getResourceAsStream(STARTSEITE + "index.html");
        }
        InputStream logo = WebServer.class.getResourceAsStream(STARTSEITE + "rbz-logo.png");
        if (html == null || logo == null) {
            return false;
        }
        String quelltext = new String(this.inputStreamToBytes(html), Charset.forName("UTF-8"));
        this.erzeugeDatei("index", "html", quelltext);
        this.erzeugeDatei("rbz-logo", "png", Base64.encodeBytes(this.inputStreamToBytes(logo)));
        this.erzeugeInternenBereich();
        return true;
    }

    /** folder intern: login page, the internal class area and the list of registered users */
    private void erzeugeInternenBereich() {
        String sprache = filius.modern.Texts.englisch() ? "_en" : "";
        Dateisystem dateisystem = this.getSystemSoftware().getDateisystem();
        dateisystem.erstelleVerzeichnis(this.verzeichnis, "intern");
        DefaultMutableTreeNode intern = dateisystem.verzeichnisKnoten(dateisystem.holeRootPfad() + "/webserver/intern");
        if (intern == null) {
            return;
        }
        String[][] dateien = { { "index.html", "intern/index" + sprache + ".html" }, { "bereich.html", "intern/bereich" + sprache + ".html" },
                { "zugang.txt", "intern/zugang.txt" } };
        for (String[] d : dateien) {
            InputStream in = WebServer.class.getResourceAsStream(STARTSEITE + d[1]);
            if (in == null || dateisystem.dateiVorhanden(intern, d[0])) {
                continue;
            }
            String inhalt = new String(this.inputStreamToBytes(in), Charset.forName("UTF-8"));
            String endung = d[0].substring(d[0].lastIndexOf('.') + 1);
            dateisystem.speicherDatei(intern, new Datei(d[0], endung, inhalt));
        }
    }

    private static final String STARTSEITE = "/filius/modern/web/startseite/";

    private void erzeugeDatei(String dateiname, String endung, String quellcode) {
        LOG.trace("INVOKED (" + this.hashCode() + ", T" + this.getId() + ") " + String.valueOf(this.getClass()) + " (WebServer), erzeugeDatei(" + dateiname + "," + endung + "," + quellcode + ")");
        String kompletteDateiName = dateiname + "." + endung;
        Datei datei = new Datei(kompletteDateiName, endung, quellcode);
        this.getSystemSoftware().getDateisystem().speicherDatei(this.verzeichnis, datei);
    }

    protected Datei dateiLiefern(String relativerPfad) {
        LOG.trace("INVOKED (" + this.hashCode() + ", T" + this.getId() + ") " + String.valueOf(this.getClass()) + " (WebServer), dateiLiefern(" + relativerPfad + ")");
        Datei tmpDatei = this.getSystemSoftware().getDateisystem().holeDatei(this.verzeichnis, relativerPfad);
        return tmpDatei;
    }

    public WebServerPlugIn holePlugin(String key) {
        LOG.trace("INVOKED (" + this.hashCode() + ", T" + this.getId() + ") " + String.valueOf(this.getClass()) + " (WebServer), holePlugin(" + key + ")");
        WebServerPlugIn plugIn = null;
        if (this.plugins.containsKey(key)) {
            plugIn = this.plugins.get(key);
        }
        return plugIn;
    }

    public HashMap<String, WebServerPlugIn> getPlugins() {
        return this.plugins;
    }

    public void setPlugins(HashMap<String, WebServerPlugIn> plugins) {
        this.plugins = plugins;
    }

    public String printVHostTable() {
        String result = "";
        for (int i = 0; i < this.vHostArray.length; ++i) {
            result = result + i + ":\t";
            result = this.vHostArray[i][0] == null ? result + "<null>\t" : result + this.vHostArray[i][0] + "\t";
            result = this.vHostArray[i][1] == null ? result + "<null>\n" : result + this.vHostArray[i][1] + "\n";
        }
        return result;
    }

    public void changeVHostTable(int row, int col, String val) {
        LOG.trace("INVOKED (" + this.hashCode() + ", T" + this.getId() + ") " + String.valueOf(this.getClass()) + " (WebServer), changeVHostTable(" + row + "," + col + "," + val + ")");
        this.vHostArray[row][col] = val != null ? val : "";
        this.saveVHosts();
    }

    public String vhostPrefix(String vhost) {
        LOG.trace("INVOKED (" + this.hashCode() + ", T" + this.getId() + ") " + String.valueOf(this.getClass()) + " (WebServer), vhostPrefix(" + vhost + ")");
        for (int i = 0; this.useVHost && i < this.vHostArray.length; ++i) {
            if (this.vHostArray[i][0] == null || !this.vHostArray[i][0].equalsIgnoreCase(vhost) || this.vHostArray[i][1] == null) continue;
            return this.vHostArray[i][1];
        }
        return "";
    }

    public String[][] getVHostArray() {
        return this.vHostArray;
    }

    @Override
    protected void neuerMitarbeiter(Socket socket) {
        LOG.trace("INVOKED (" + this.hashCode() + ", T" + this.getId() + ") " + String.valueOf(this.getClass()) + " (WebServer), neuerMitarbeiter(" + String.valueOf(socket) + ")");
        if (socket instanceof TCPSocket) {
            WebServerMitarbeiter wsMitarbeiter = new WebServerMitarbeiter(this, (TCPSocket)socket);
            wsMitarbeiter.starten();
            this.mitarbeiter.add(wsMitarbeiter);
        }
    }

    @Override
    public void starten() {
        LOG.debug("INVOKED (" + this.hashCode() + ", T" + this.getId() + ") " + String.valueOf(this.getClass()) + " (WebServer), starten()");
        super.starten();
        if (!this.getSystemSoftware().getDateisystem().dateiVorhanden("www.conf", "vhosts")) {
            this.getSystemSoftware().getDateisystem().erstelleVerzeichnis("root", "www.conf");
        }
        this.initialisiereVHosts();
        if (this.tlsAnnahme == null) {
            this.tlsAnnahme = new TlsAnnahme(this);
        }
        this.tlsAnnahme.setSystemSoftware(this.getSystemSoftware());
        this.tlsAnnahme.starten();
        this.tlsAbgleichen();
    }

    @Override
    public void beenden() {
        LOG.trace("INVOKED (" + this.hashCode() + ", T" + this.getId() + ") " + String.valueOf(this.getClass()) + " (WebServer), beenden()");
        super.beenden();
        if (this.tlsAnnahme != null) {
            this.tlsAnnahme.leise(false);
            this.tlsAnnahme.beenden();
        }
    }

    private void saveVHosts() {
        LOG.trace("INVOKED (" + this.hashCode() + ", T" + this.getId() + ") " + String.valueOf(this.getClass()) + " (DNSServer), schreibeRecordListe()");
        Dateisystem dateisystem = this.getSystemSoftware().getDateisystem();
        StringBuffer text = new StringBuffer();
        for (int i = 0; i < this.vHostArray.length; ++i) {
            String host = this.vHostArray[i][0];
            String directory = this.vHostArray[i][1];
            text.append((host != null ? host : "") + "\n" + (directory != null ? directory : "") + "\n");
        }
        Datei vhosts = new Datei();
        vhosts.setDateiInhalt(text.toString());
        vhosts.setName("vhosts");
        dateisystem.erstelleVerzeichnis(dateisystem.holeRootPfad(), "www.conf");
        dateisystem.speicherDatei(dateisystem.holeRootPfad() + "/www.conf", vhosts);
    }

    private void initialisiereVHosts() {
        this.vHostArray = new String[5][2];
        int row = 0;
        int col = 0;
        Dateisystem dateisystem = this.getSystemSoftware().getDateisystem();
        Datei vhosts = dateisystem.holeDatei(dateisystem.holeRootPfad() + "/www.conf/vhosts");
        if (vhosts != null) {
            this.useVHost = true;
            StringTokenizer tokenizer = new StringTokenizer(vhosts.getDateiInhalt(), "\n");
            while (tokenizer.hasMoreTokens()) {
                String line;
                this.vHostArray[row][col] = line = tokenizer.nextToken().trim();
                if (col == 0) {
                    ++col;
                    continue;
                }
                col = 0;
                ++row;
            }
        }
        while (row < 5) {
            this.vHostArray[row][0] = "";
            this.vHostArray[row][1] = "";
            ++row;
        }
    }

    public boolean isUseVHost() {
        return this.useVHost;
    }

    // ------------------------------------------------------------------ HTTPS (Filius RBZ Wirtschaft Kiel Version)

    private boolean https = false;
    private boolean http = true;
    private String zertifikatNamen = "";
    private boolean selbstSigniert = false;
    private transient Tls.Identitaet identitaet;
    private transient TlsAnnahme tlsAnnahme;

    public boolean isHttps() {
        return this.https;
    }

    public void setHttps(boolean https) {
        this.https = https;
        this.tlsAbgleichen();
        this.benachrichtigeBeobachter();
    }

    /** HTTP on port 80; off means port 80 is closed */
    public boolean isHttp() {
        return this.http;
    }

    public synchronized void setHttp(boolean http) {
        this.http = http;
        if (!http && this.socket != null) {
            this.socket.schliessen();
        }
        this.notifyAll();
        this.benachrichtigeBeobachter();
    }

    /**
     * names the certificate is made for, e.g. "192.168.10.10, www.rbz-wirtschaft-kiel.de". Empty = a certificate
     * without a name, so every browser warns (version 31; before, an empty field meant the own IPv4 address, and
     * pupils got a working certificate without entering anything)
     */
    public String getZertifikatNamen() {
        return this.zertifikatNamen;
    }

    public void setZertifikatNamen(String namen) {
        this.zertifikatNamen = namen == null ? "" : namen.trim();
        this.benachrichtigeBeobachter();
    }

    public boolean isSelbstSigniert() {
        return this.selbstSigniert;
    }

    public void setSelbstSigniert(boolean selbstSigniert) {
        this.selbstSigniert = selbstSigniert;
        this.benachrichtigeBeobachter();
    }

    /** key pair and certificate for the current settings */
    public synchronized Tls.Identitaet tlsIdentitaet() {
        this.identitaet = Tls.identitaet(this.identitaet, Tls.namen(this.zertifikatNamen, null), this.selbstSigniert);
        return this.identitaet;
    }

    /** the certificate the server shows now (without making a new key) */
    public Tls.Zertifikat zertifikat() {
        return this.tlsIdentitaet().zertifikat;
    }

    private void tlsAbgleichen() {
        if (this.tlsAnnahme != null) {
            this.tlsAnnahme.leise(this.aktiv && this.https);
        }
    }

    @Override
    public void setAktiv(boolean flag) {
        super.setAktiv(flag);
        this.tlsAbgleichen();
    }

    /** the server accepts HTTP connections on port 80 only while HTTP is switched on */
    @Override
    public void annehmenVerbindungen() {
        while (this.running) {
            if (this.aktiv && this.http) {
                if (this.socket == null) {
                    this.socket = new ServerSocket(this.getSystemSoftware(), this.port, this.transportProtokoll, this.gebundeneKarten());
                }
                try {
                    Socket transportSocket = ((ServerSocket) this.socket).oeffnen();
                    if (transportSocket == null || transportSocket.holeZielIPAdresse() == null) {
                        continue;
                    }
                    this.neuerMitarbeiter(transportSocket);
                    this.benachrichtigeBeobachter(messages.getString("sw_serveranwendung_msg4") + " " + transportSocket.holeZielIPAdresse() + ":"
                            + transportSocket.holeZielPort() + " " + messages.getString("sw_serveranwendung_msg5"));
                } catch (ServerSocketException e) {
                    this.benachrichtigeBeobachter(messages.getString("sw_serveranwendung_msg6"));
                    this.setAktiv(false);
                    LOG.debug("tried to establish connection to client - but failed.", e);
                } catch (Exception e) {
                    LOG.debug("", e);
                }
                continue;
            }
            if (this.socket != null) {
                this.socket.schliessen();
                if (!this.aktiv) {
                    this.benachrichtigeBeobachter(messages.getString("sw_serveranwendung_msg3"));
                }
            }
            this.socket = null;
            synchronized (this) {
                if (this.running && !(this.aktiv && this.http)) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        // go on
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private java.util.List<filius.hardware.NetzwerkInterface> gebundeneKarten() {
        try {
            java.lang.reflect.Field f = filius.software.clientserver.ServerAnwendung.class.getDeclaredField("boundNICs");
            f.setAccessible(true);
            return (java.util.List<filius.hardware.NetzwerkInterface>) f.get(this);
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    /** Accepts HTTPS connections on port 443 for this web server; its messages go to the log of the web server. */
    static final class TlsAnnahme extends filius.software.clientserver.TCPServerAnwendung {
        private final WebServer server;
        private boolean still;

        TlsAnnahme(WebServer server) {
            this.server = server;
            this.port = Tls.PORT;
        }

        /** switches without the "server started/stopped" messages (the web server shows its own) */
        void leise(boolean flag) {
            if (flag == this.aktiv) {
                return;
            }
            this.still = true;
            try {
                this.setAktiv(flag);
            } finally {
                this.still = false;
            }
        }

        @Override
        public void benachrichtigeBeobachter(Object daten) {
            if (!this.still) {
                this.server.benachrichtigeBeobachter(daten);
            }
        }

        @Override
        protected void neuerMitarbeiter(Socket socket) {
            if (socket instanceof TCPSocket) {
                WebServerMitarbeiter m = new WebServerMitarbeiter(this.server, (TCPSocket) socket, new Tls.ServerSitzung(this.server.tlsIdentitaet()));
                m.starten();
                this.mitarbeiter.add(m);
            }
        }
    }

    public void setUseVHost(boolean useVHost) {
        this.useVHost = useVHost;
    }
}

