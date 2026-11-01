/*
 * Filius (RBZ Wirtschaft Kiel Version): based on WebBrowser of Filius 2.9.3 (GPL).
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel, 2026: images are saved under their own name
 * (before, an image could end up under the name of the previous, failed one), JPEG files with the
 * ending .jpeg are shown, and images in sub folders work. This class serves the classic page view,
 * which Filius (RBZ Wirtschaft Kiel Version) uses only if the browser engine cannot start.
 * HTTPS: https:// addresses use port 443 and a TLS session (filius.modern.tls.Tls); a certificate that is not
 * trusted leads to a warning page with a link to open the page anyway.
 */
package filius.software.www;

import filius.exception.VerbindungsException;
import filius.modern.tls.Tls;
import filius.rahmenprogramm.Base64;
import filius.rahmenprogramm.I18n;
import filius.rahmenprogramm.Information;
import filius.rahmenprogramm.ResourceUtil;
import filius.software.clientserver.ClientAnwendung;
import filius.software.transportschicht.TCPSocket;
import filius.software.www.HTTPNachricht;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.LinkedList;
import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.visitors.TagFindingVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebBrowser
extends ClientAnwendung
implements I18n {
    private static Logger LOG = LoggerFactory.getLogger(WebBrowser.class);
    private static final int ABRUF_HTML = 1;
    private static final int ABRUF_IMG = 2;
    private LinkedList<String> bilddateien = new LinkedList();
    private String host;
    private int port = 80;
    private boolean https;
    /** TLS session of the current HTTPS connection */
    private Tls.ClientSitzung tls;
    /** the page was replaced by a warning or error page: nothing more to fetch */
    private boolean abgebrochen;
    private static final long TLS_TIMEOUT = 120_000L;

    public void holeWebseite(URL url) {
        this.holeWebseite(url, "");
    }

    public void holeWebseite(URL url, String post) {
        LOG.trace("INVOKED (" + this.hashCode() + ", T" + this.getId() + ") " + String.valueOf(this.getClass()) + " (WebBrowser), holeWebseite(" + String.valueOf(url) + "," + post + ")");
        // link of the warning page: open this host anyway
        String ohne = Tls.ohneAusnahme(url.getFile());
        if (ohne != null) {
            String h = url.getHost() == null || url.getHost().isEmpty() ? this.host : url.getHost();
            Tls.ausnahme(this.getSystemSoftware(), h);
            try {
                url = new URL(url.getProtocol(), url.getHost(), url.getPort(), ohne);
            } catch (MalformedURLException e) {
                LOG.debug("", e);
            }
        }
        if (this.socket != null) {
            this.ausfuehren("closeConnection", new Object[0]);
        }
        this.ausfuehren("initConnection", new Object[]{url});
        this.ausfuehren("retrieveWebpage", new Object[]{url, post});
        this.ausfuehren("retrieveImages", new Object[0]);
        this.ausfuehren("closeConnection", new Object[0]);
    }

    void initConnection(URL url) {
        HTTPNachricht fehler;
        this.abgebrochen = false;
        this.tls = null;
        if (url.getHost() != null && !url.getHost().equals("")) {
            this.host = url.getHost();
            this.https = "https".equalsIgnoreCase(url.getProtocol());
            // a port in the address (http://203.0.113.3:8080) is used, e.g. for port forwarding
            this.port = url.getPort() > 0 ? url.getPort() : (this.https ? Tls.PORT : 80);
        } else {
            this.https = "https".equalsIgnoreCase(url.getProtocol());
        }
        if (this.socket == null) {
            try {
                this.socket = new TCPSocket(this.getSystemSoftware(), this.host, this.port);
            } catch (VerbindungsException e) {
                fehler = new HTTPNachricht(1);
                fehler.setDaten(this.erzeugeHtmlFehlermeldung(0));
                this.benachrichtigeBeobachter(fehler);
            }
        }
        if (this.socket != null && !this.socket.istVerbunden()) {
            try {
                this.socket.verbinden();
            } catch (Exception e) {
                fehler = new HTTPNachricht(1);
                fehler.setDaten(this.erzeugeHtmlFehlermeldung(0));
                this.benachrichtigeBeobachter(fehler);
                this.abgebrochen = true;
            }
        }
        if (this.https && !this.abgebrochen && this.socket instanceof TCPSocket && this.socket.istVerbunden()) {
            try {
                this.tls = Tls.ClientSitzung.verbinden((TCPSocket) this.socket, this.getSystemSoftware(), this.host, TLS_TIMEOUT);
            } catch (Tls.ZertifikatProblem p) {
                fehler = new HTTPNachricht(1);
                String weiter = "https://" + this.host + (this.port != Tls.PORT ? ":" + this.port : "") + url.getFile();
                fehler.setDaten(Tls.warnseite(p, this.host, Tls.mitAusnahme(weiter)));
                this.benachrichtigeBeobachter(fehler);
                this.abgebrochen = true;
            } catch (Exception e) {
                LOG.debug("TLS handshake failed", e);
                fehler = new HTTPNachricht(1);
                fehler.setDaten(this.erzeugeHtmlFehlermeldung(0));
                this.benachrichtigeBeobachter(fehler);
                this.abgebrochen = true;
            }
        }
    }

    /** sends a request over HTTP or HTTPS */
    private void sende(String nachricht) throws Exception {
        if (this.tls != null) {
            this.tls.senden(nachricht);
        } else {
            this.socket.senden(nachricht);
        }
    }

    /** the next answer over HTTP or HTTPS */
    private String empfange() throws Exception {
        if (this.tls != null) {
            return this.tls.empfangen(0);
        }
        return this.socket != null ? this.socket.empfangen() : null;
    }

    /** true if the last page came over HTTPS */
    public boolean istHttps() {
        return this.https;
    }

    void retrieveWebpage(URL url, String post) {
        HTTPNachricht nachricht = this.createRequest(url, post);
        if (!this.abgebrochen && this.socket != null && this.socket.istVerbunden()) {
            try {
                String responseData;
                this.sende(nachricht.toString());
                responseData = this.empfange();
                if (responseData == null) {
                    HTTPNachricht response = new HTTPNachricht(1);
                    response.setDaten(this.erzeugeHtmlFehlermeldung(0));
                    this.benachrichtigeBeobachter(response);
                } else {
                    HTTPNachricht response = new HTTPNachricht(responseData);
                    if (response.getStatusCode() != 200) {
                        response.setDaten(this.erzeugeHtmlFehlermeldung(response.getStatusCode()));
                    } else {
                        String contentType = response.getContentType();
                        if ("text/html".equalsIgnoreCase(contentType) && response.getDaten() != null) {
                            this.extractImagesReferences(response.getDaten(), response.getHost());
                        }
                    }
                    this.benachrichtigeBeobachter(response);
                }
            } catch (Exception e) {
                LOG.debug("Error while retrieving web page.", e);
            }
        }
    }

    private HTTPNachricht createRequest(URL url, String post) {
        HTTPNachricht nachricht = new HTTPNachricht(1);
        nachricht.setPfad(url.getFile());
        nachricht.setHost(this.host);
        if (nachricht.getHost() != null && !nachricht.getHost().equals("")) {
            if (post != null && !post.equals("")) {
                nachricht.setMethod("POST");
                nachricht.setDaten(post);
            } else {
                nachricht.setMethod("GET");
            }
        }
        return nachricht;
    }

    public String holeHost() {
        return this.host;
    }

    @Override
    public void starten() {
        super.starten();
        this.bilddateien = new LinkedList();
    }

    private String einlesenTextdatei(String datei) throws FileNotFoundException, IOException {
        LOG.trace("INVOKED (" + this.hashCode() + ", T" + this.getId() + ") " + String.valueOf(this.getClass()) + " (WebBrowser), einlesenTextdatei(" + datei + ")");
        StringBuffer fullFile = new StringBuffer();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader((InputStream)new FileInputStream(ResourceUtil.getResourcePath(datei)), Charset.forName("UTF-8")));){
            String input;
            while ((input = reader.readLine()) != null) {
                fullFile.append(input).append("\n");
            }
        }
        return fullFile.toString();
    }

    private String erzeugeHtmlFehlermeldung(int statusCode) {
        String quelltext;
        LOG.trace("INVOKED (" + this.hashCode() + ", T" + this.getId() + ") " + String.valueOf(this.getClass()) + " (WebBrowser), erzeugeHtmlFehlermeldung(" + statusCode + ")");
        if (statusCode == 0) {
            quelltext = messages.getString("sw_webbrowser_msg1");
        } else {
            String dateipfad = ResourceUtil.getResourcePath("tmpl/http_fehler_" + Information.getInformation().getLocaleOrDefault().toString() + ".txt");
            try {
                quelltext = this.einlesenTextdatei(dateipfad);
            } catch (Exception e) {
                quelltext = messages.getString("sw_webbrowser_msg2");
                LOG.debug("", e);
            }
            quelltext = quelltext.replace(":code:", "" + statusCode);
            String meldung = HTTPNachricht.holeStatusNachricht(statusCode);
            quelltext = quelltext.replace(":meldung:", meldung);
        }
        return quelltext;
    }

    private void extractImagesReferences(String quelltext, String host) {
        LOG.trace("INVOKED (" + this.hashCode() + ", T" + this.getId() + ") " + String.valueOf(this.getClass()) + " (WebBrowser), verarbeiteIMGTags(" + quelltext + "," + host + ")");
        Parser parser = Parser.createParser(quelltext, null);
        TagFindingVisitor nodeVisitor = new TagFindingVisitor(new String[]{"img"});
        try {
            parser.visitAllNodesWith(nodeVisitor);
            Node[] nodes = nodeVisitor.getTags(0);
            for (int i = 0; i < nodes.length; ++i) {
                ImageTag img;
                if (!(nodes[i] instanceof ImageTag) || null == (img = (ImageTag)nodes[i])) continue;
                LinkedList<String> linkedList = this.bilddateien;
                synchronized (linkedList) {
                    this.bilddateien.add(img.getImageURL());
                    continue;
                }
            }
        } catch (Exception e) {
            LOG.debug("", e);
        }
    }

    void retrieveImages() {
        LinkedList<String> dateien;
        synchronized (this.bilddateien) {
            dateien = new LinkedList<String>(this.bilddateien);
            this.bilddateien.clear();
        }
        for (String dateipfad : dateien) {
            try {
                HTTPNachricht request = this.createRequest(new URL("http", this.host, dateipfad.startsWith("/") ? dateipfad : "/" + dateipfad), "");
                if (this.abgebrochen || this.socket == null || !this.socket.istVerbunden()) continue;
                this.sende(request.toString());
                String responseData = this.empfange();
                if (responseData == null) continue;
                HTTPNachricht response = new HTTPNachricht(responseData);
                if (response.getStatusCode() != 200) {
                    this.benachrichtigeBeobachter();
                    continue;
                }
                String contentType = response.getContentType() == null ? "" : response.getContentType().toLowerCase();
                if (!contentType.equals("image/bmp") && !contentType.equals("image/gif") && !contentType.equals("image/jpg")
                        && !contentType.equals("image/jpeg") && !contentType.equals("image/png")) continue;
                java.io.File ziel = new java.io.File(Information.getInformation().getTempPfad() + dateipfad);
                if (ziel.getParentFile() != null) {
                    ziel.getParentFile().mkdirs();
                }
                Base64.decodeToFile(response.getDaten(), ziel.getPath());
                this.benachrichtigeBeobachter();
            } catch (MalformedURLException e) {
                LOG.debug("Could not retrieve image. Invalid URL.", e);
            } catch (Exception e) {
                LOG.debug("Unexpected error while retrieving images via http.", e);
            }
        }
    }

    void closeConnection() {
        this.tls = null;
        if (this.socket != null) {
            this.socket.schliessen();
            this.socket = null;
        }
    }
}

