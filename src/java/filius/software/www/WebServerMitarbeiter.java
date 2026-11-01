/*
 * Filius (RBZ Wirtschaft Kiel Version): based on WebServerMitarbeiter of Filius 2.9.3 (GPL).
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel, 2026: the web server delivers every file with a
 * suitable content type (stylesheets, scripts, JSON, SVG, JPEG, fonts, ...), so browsers can use
 * them. Text files that Filius stored Base64-encoded during import (e.g. .css or .js) are sent as
 * text. HTML pages, images and the web interfaces of firewall and home router work as before.
 * New: HTTPS connections (filius.modern.tls.Tls): a worker with a TLS session first answers the
 * handshake, then decrypts every request and encrypts every answer. A login form that posts to a
 * file "anmelden" (e.g. intern/anmelden) is checked against zugang.txt in the same folder
 * (lines "name:password"); with the right password the server delivers bereich.html of that folder,
 * otherwise index.html with a message. zugang.txt itself is never delivered.
 */
package filius.software.www;

import filius.modern.web.MimeTypes;
import filius.modern.tls.Tls;
import filius.software.clientserver.ServerMitarbeiter;
import filius.software.system.Datei;
import filius.software.transportschicht.TCPSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServerMitarbeiter
extends ServerMitarbeiter {
    private static Logger LOG = LoggerFactory.getLogger(WebServerMitarbeiter.class);
    /** TLS session of an HTTPS connection, null for HTTP */
    private final Tls.ServerSitzung tls;

    public WebServerMitarbeiter(WebServer server, TCPSocket socket) {
        this(server, socket, null);
    }

    public WebServerMitarbeiter(WebServer server, TCPSocket socket, Tls.ServerSitzung tls) {
        super(server, socket);
        this.tls = tls;
    }

    private HTTPNachricht verarbeiteAnfrage(HTTPNachricht anfrage) {
        WebServerPlugIn plugin;
        String tmp = null;
        HTTPNachricht antwort = new HTTPNachricht(0);
        String dateipfad = anfrage.getPfad().equals("/") ? "index.html" : (anfrage.getPfad().startsWith("/") ? anfrage.getPfad().substring(1) : anfrage.getPfad());
        // a query (?a=b) does not belong to the file name
        if (dateipfad.contains("?")) {
            dateipfad = dateipfad.substring(0, dateipfad.indexOf('?'));
        }
        // a folder ("/seite/") delivers its index.html
        if (dateipfad.endsWith("/") || dateipfad.isEmpty()) {
            dateipfad = dateipfad + "index.html";
        }
        String vHostRelPath = ((WebServer)this.server).vhostPrefix(anfrage.getHost());
        if (!vHostRelPath.endsWith("/")) {
            vHostRelPath = vHostRelPath + "/";
        }
        if ((dateipfad = vHostRelPath + dateipfad).startsWith("/")) {
            dateipfad = dateipfad.substring(1);
        }
        if ((plugin = ((WebServer)this.server).holePlugin(dateipfad)) != null) {
            tmp = anfrage.getMethod().equals("POST") ? plugin.holeHtmlSeite(anfrage.getDaten()) : plugin.holeHtmlSeite(null);
            if (tmp != null) {
                antwort.setDaten(tmp);
                antwort.setContentType("text/html");
                antwort.setStatusCode(200);
            } else {
                antwort.setStatusCode(500);
            }
        } else if (dateipfad.equals("anmelden") || dateipfad.endsWith("/anmelden")) {
            this.anmelden(dateipfad, anfrage, antwort);
        } else if (dateipfad.equals("zugang.txt") || dateipfad.endsWith("/zugang.txt")) {
            // the list of passwords stays on the server
            antwort.setStatusCode(403);
        } else {
            Datei datei = ((WebServer)this.server).dateiLiefern(dateipfad);
            if (datei != null) {
                String typ = MimeTypes.forName(datei.getName());
                if (typ == null) {
                    typ = "text/plain";
                }
                String inhalt = datei.getDateiInhalt();
                if (MimeTypes.isText(typ) && !"text/html".equals(typ)) {
                    inhalt = MimeTypes.textContent(inhalt, datei.getDateiTyp());
                }
                antwort.setDaten(inhalt);
                antwort.setContentType(typ);
                antwort.setStatusCode(200);
            } else {
                antwort.setStatusCode(404);
            }
        }
        return antwort;
    }

    /** login: name and password of the form against zugang.txt of the same folder */
    private void anmelden(String dateipfad, HTTPNachricht anfrage, HTTPNachricht antwort) {
        String ordner = dateipfad.contains("/") ? dateipfad.substring(0, dateipfad.lastIndexOf('/') + 1) : "";
        WebServer ws = (WebServer) this.server;
        String daten = anfrage.getDaten() == null ? "" : anfrage.getDaten().trim();
        String name = feld(daten, "name");
        String passwort = feld(daten, "passwort");
        if (passwort.isEmpty()) {
            passwort = feld(daten, "password");
        }
        boolean richtig = false;
        Datei zugang = ws.dateiLiefern(ordner + "zugang.txt");
        if (zugang != null && !name.isEmpty()) {
            for (String zeile : zugang.getDateiInhalt().split("\r?\n")) {
                int i = zeile.indexOf(':');
                if (zeile.trim().startsWith("#")) {
                    continue;
                }
                if (i > 0 && zeile.substring(0, i).trim().equalsIgnoreCase(name) && zeile.substring(i + 1).trim().equals(passwort)) {
                    richtig = true;
                }
            }
        }
        Datei seite = ws.dateiLiefern(ordner + (richtig ? "bereich.html" : "index.html"));
        if (seite == null) {
            antwort.setStatusCode(404);
            return;
        }
        String inhalt = seite.getDateiInhalt();
        if (richtig) {
            inhalt = inhalt.replace("{name}", escape(name));
        } else {
            inhalt = inhalt.replace("<!--meldung-->", "<p class=\"fehler\">" + filius.modern.Texts.get("web.login.wrong") + "</p>");
        }
        antwort.setDaten(inhalt);
        antwort.setContentType("text/html");
        antwort.setStatusCode(200);
    }

    /** value of a form field in "name=anna&passwort=Schulfest" */
    private static String feld(String daten, String feld) {
        for (String teil : daten.split("&")) {
            int i = teil.indexOf('=');
            if (i > 0 && teil.substring(0, i).trim().equalsIgnoreCase(feld)) {
                try {
                    return java.net.URLDecoder.decode(teil.substring(i + 1), "UTF-8").trim();
                } catch (Exception e) {
                    return teil.substring(i + 1).trim();
                }
            }
        }
        return "";
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    protected void verarbeiteNachricht(String nachricht) {
        HTTPNachricht antwort;
        HTTPNachricht anfrage = new HTTPNachricht(nachricht);
        if (anfrage.getMethod() != null && (anfrage.getMethod().equals("GET") || anfrage.getMethod().equals("POST"))) {
            antwort = this.verarbeiteAnfrage(anfrage);
        } else {
            antwort = new HTTPNachricht(0);
            antwort.setStatusCode(501);
        }
        this.sendeNachricht(antwort.toString());
    }

    /** HTTPS: answers are encrypted with the session key */
    @Override
    protected void sendeNachricht(String nachricht) {
        if (this.tls == null) {
            super.sendeNachricht(nachricht);
            return;
        }
        try {
            this.socket.senden(this.tls.verschluesseln(nachricht));
            this.server.benachrichtigeBeobachter("<< [TLS] " + nachricht);
        } catch (Exception e) {
            this.server.benachrichtigeBeobachter(e.getMessage());
            LOG.debug("", e);
        }
    }

    /** HTTPS: like the loop of ServerMitarbeiter, with the TLS handshake and decryption in between */
    @Override
    public void run() {
        if (this.tls == null) {
            super.run();
            return;
        }
        while (this.running) {
            try {
                String record = null;
                if (this.socket.istVerbunden()) {
                    record = this.socket.empfangen(Long.MAX_VALUE);
                }
                if (record == null) {
                    if (this.socket != null) {
                        this.socket.schliessen();
                    }
                    this.running = false;
                    continue;
                }
                Tls.ServerSitzung.Ergebnis e = this.tls.empfangen(record);
                if (e.protokoll != null) {
                    this.server.benachrichtigeBeobachter("TLS: " + e.protokoll);
                }
                if (e.antwort != null) {
                    this.socket.senden(e.antwort);
                }
                if (e.klartext != null) {
                    this.server.benachrichtigeBeobachter(">> [TLS] " + e.klartext);
                    this.verarbeiteNachricht(e.klartext);
                }
                if (e.schliessen) {
                    this.socket.schliessen();
                    this.running = false;
                    this.server.entferneMitarbeiter(this);
                }
            } catch (Exception ex) {
                LOG.debug("", ex);
                this.socket.schliessen();
                this.running = false;
                this.server.entferneMitarbeiter(this);
            }
        }
    }
}
