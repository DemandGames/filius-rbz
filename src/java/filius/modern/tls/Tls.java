package filius.modern.tls;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import filius.modern.Texts;
import filius.software.system.InternetKnotenBetriebssystem;
import filius.software.transportschicht.TCPSocket;

/**
 * TLS for the simulated network (Filius RBZ Wirtschaft Kiel Version): HTTPS between Waterwolf and the Filius web
 * server. The encryption is real (RSA for handing over the session key, AES for the data), the records are text so
 * that they can be read in the data exchange window:
 *
 * <pre>
 * Client                                Server
 * TLS ClientHello            ------&gt;
 *                            &lt;------   TLS ServerHello + certificate (names, issuer, public key, signature)
 * TLS ClientKeyExchange      ------&gt;    session key, locked with the public key of the server
 *                            &lt;------   TLS Finished (check value, locked with the session key)
 * TLS Application Data       &lt;-----&gt;   HTTP, locked with the session key
 * </pre>
 *
 * The handing over of the session key is asymmetric (only the server can open it with its private key), all data
 * after it is symmetric (both sides have the same session key) - the two procedures the learning path explains.
 * Certificates are signed by the built-in "Filius certificate authority", which every Waterwolf trusts; a
 * self-signed certificate or a certificate for another name leads to a warning page in the browser.
 */
public final class Tls {

    private Tls() {}

    public static final int PORT = 443;
    public static final String VERSION = "TLS 1.2";

    static final String CLIENT_HELLO = "TLS ClientHello";
    static final String SERVER_HELLO = "TLS ServerHello";
    static final String KEY_EXCHANGE = "TLS ClientKeyExchange";
    static final String FINISHED = "TLS Finished";
    static final String DATA = "TLS Application Data";
    static final String ALERT = "TLS Alert";
    private static final String CHECK = "finished";
    /** length of a TCP segment in Filius (TCPSocket.MSS) */
    private static final int MSS = 1460;

    private static final SecureRandom RANDOM = new SecureRandom();

    // ------------------------------------------------------------------ records

    /** true if the payload of a segment is (the start of) a TLS record */
    public static boolean istRecord(String payload) {
        return payload != null && payload.startsWith("TLS ");
    }

    /** type of a record ("ClientHello", "ServerHello", "ClientKeyExchange", "Finished", "Application Data", "Alert") */
    public static String art(String payload) {
        if (!istRecord(payload)) {
            return null;
        }
        String erste = payload.split("\n", 2)[0];
        if (erste.startsWith(SERVER_HELLO)) {
            return "ServerHello";
        }
        return erste.substring(4).trim();
    }

    private static String wert(String zeile) {
        int i = zeile.indexOf(": ");
        return i < 0 ? zeile.trim() : zeile.substring(i + 2).trim();
    }

    private static String[] zeilen(String record) {
        return record.split("\n");
    }

    // ------------------------------------------------------------------ certificate authority

    private static KeyPair zertifizierungsstelle;

    private static synchronized KeyPair ca() {
        if (zertifizierungsstelle == null) {
            zertifizierungsstelle = schluesselpaar();
        }
        return zertifizierungsstelle;
    }

    /** name of the built-in certificate authority */
    public static String caName() {
        return Texts.get("tls.ca");
    }

    public static KeyPair schluesselpaar() {
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
            g.initialize(1024, RANDOM);
            return g.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ------------------------------------------------------------------ certificate

    /** The ID card of a web server: for which names, issued by whom, valid until, public key and signature. */
    public static final class Zertifikat {
        public final List<String> namen;
        public final String aussteller;
        public final String gueltigBis;
        /** public key of the server, X.509 encoded, Base64 */
        public final String schluessel;
        /** signature of the issuer over names, issuer, date and key, Base64 */
        public final String unterschrift;

        public Zertifikat(List<String> namen, String aussteller, String gueltigBis, String schluessel, String unterschrift) {
            this.namen = Collections.unmodifiableList(new ArrayList<>(namen));
            this.aussteller = aussteller;
            this.gueltigBis = gueltigBis;
            this.schluessel = schluessel;
            this.unterschrift = unterschrift;
        }

        private String inhalt() {
            return String.join(",", namen) + "|" + aussteller + "|" + gueltigBis + "|" + schluessel;
        }

        public PublicKey oeffentlicherSchluessel() throws Exception {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(schluessel)));
        }

        /** signed by the Filius certificate authority (and not changed since) */
        public boolean vonZertifizierungsstelle() {
            if (!caName().equals(aussteller)) {
                return false;
            }
            try {
                Signature s = Signature.getInstance("SHA256withRSA");
                s.initVerify(ca().getPublic());
                s.update(inhalt().getBytes(StandardCharsets.UTF_8));
                return s.verify(Base64.getDecoder().decode(unterschrift));
            } catch (Exception e) {
                return false;
            }
        }

        /** true if the certificate names this host (name or IPv4 address, upper/lower case does not matter) */
        public boolean gilt(String host) {
            for (String n : namen) {
                if (n.equalsIgnoreCase(host)) {
                    return true;
                }
            }
            return false;
        }

        public String namenText() {
            return namen.isEmpty() ? Texts.get("tls.f.noname") : String.join(", ", namen);
        }

        /** shortened public key for the certificate window */
        public String schluesselKurz() {
            return schluessel.length() > 40 ? schluessel.substring(0, 24) + " … " + schluessel.substring(schluessel.length() - 12) : schluessel;
        }
    }

    static Zertifikat ausstellen(List<String> namen, PublicKey schluessel, boolean selbstSigniert, PrivateKey eigener) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.YEAR, 1);
        String datum = new SimpleDateFormat(Texts.englisch() ? "yyyy-MM-dd" : "dd.MM.yyyy", Locale.ROOT).format(c.getTime());
        String aussteller = selbstSigniert ? Texts.format("tls.self.issuer", namen.isEmpty() ? "?" : namen.get(0)) : caName();
        String key = Base64.getEncoder().encodeToString(schluessel.getEncoded());
        Zertifikat ohne = new Zertifikat(namen, aussteller, datum, key, "");
        try {
            Signature s = Signature.getInstance("SHA256withRSA");
            s.initSign(selbstSigniert ? eigener : ca().getPrivate());
            s.update(ohne.inhalt().getBytes(StandardCharsets.UTF_8));
            return new Zertifikat(namen, aussteller, datum, key, Base64.getEncoder().encodeToString(s.sign()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** names of a certificate from the text of the web server settings ("192.168.10.10, www.example.de") */
    public static List<String> namen(String text, String standard) {
        List<String> l = new ArrayList<>();
        if (text != null) {
            for (String n : text.split("[,;\\s]+")) {
                if (!n.trim().isEmpty() && !l.contains(n.trim().toLowerCase(Locale.ROOT))) {
                    l.add(n.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        if (l.isEmpty() && standard != null && !standard.isEmpty()) {
            l.add(standard);
        }
        return l;
    }

    // ------------------------------------------------------------------ server identity

    /** Key pair and certificate of a web server; made new when its settings change. */
    public static final class Identitaet {
        final KeyPair schluessel;
        public final Zertifikat zertifikat;
        final String stand;

        Identitaet(KeyPair schluessel, Zertifikat zertifikat, String stand) {
            this.schluessel = schluessel;
            this.zertifikat = zertifikat;
            this.stand = stand;
        }

        /** the private key that belongs to the certificate (stays on the server) */
        public PrivateKey privaterSchluessel() {
            return schluessel.getPrivate();
        }
    }

    /** the identity for these settings: the old one if nothing changed, otherwise a new key pair and certificate */
    public static Identitaet identitaet(Identitaet alt, List<String> namen, boolean selbstSigniert) {
        String stand = String.join(",", namen) + "|" + selbstSigniert + "|" + caName();
        if (alt != null && alt.stand.equals(stand)) {
            return alt;
        }
        KeyPair kp = alt != null ? alt.schluessel : schluesselpaar();
        return new Identitaet(kp, ausstellen(namen, kp.getPublic(), selbstSigniert, kp.getPrivate()), stand);
    }

    // ------------------------------------------------------------------ symmetric encryption

    public static String verschluesseln(SecretKey key, String klartext) {
        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] ct = c.doFinal(klartext.getBytes(StandardCharsets.UTF_8));
            byte[] alles = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, alles, 0, iv.length);
            System.arraycopy(ct, 0, alles, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(alles);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String entschluesseln(SecretKey key, String geheimtext) throws Exception {
        byte[] alles = Base64.getDecoder().decode(geheimtext.trim());
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, alles, 0, 12));
        return new String(c.doFinal(alles, 12, alles.length - 12), StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------ records as text

    static String clientHello(String host) {
        return CLIENT_HELLO + "\n" + Texts.get("tls.f.version") + ": " + VERSION + "\n" + Texts.get("tls.f.server") + ": " + host + "\n"
                + Texts.get("tls.f.methods") + ": RSA, AES-128\n" + Texts.get("tls.f.random") + ": " + zufall();
    }

    static String serverHello(Zertifikat z) {
        return SERVER_HELLO + " + " + Texts.get("tls.certificate") + "\n" + Texts.get("tls.f.version") + ": " + VERSION + "\n"
                + Texts.get("tls.f.methods") + ": RSA, AES-128\n" + Texts.get("tls.f.for") + ": " + String.join(", ", z.namen) + "\n"
                + Texts.get("tls.f.issuer") + ": " + z.aussteller + "\n" + Texts.get("tls.f.valid") + ": " + z.gueltigBis + "\n"
                + Texts.get("tls.f.key") + ": " + z.schluessel + "\n" + Texts.get("tls.f.signature") + ": " + z.unterschrift;
    }

    static Zertifikat zertifikatAus(String serverHello) {
        String[] z = zeilen(serverHello);
        if (z.length < 8) {
            return null;
        }
        List<String> namen = new ArrayList<>();
        for (String n : wert(z[3]).split(",")) {
            if (!n.trim().isEmpty()) {
                namen.add(n.trim());
            }
        }
        return new Zertifikat(namen, wert(z[4]), wert(z[5]), wert(z[6]), wert(z[7]));
    }

    static String keyExchange(String verschluesselterSchluessel) {
        return KEY_EXCHANGE + "\n" + Texts.get("tls.f.sessionkey") + ": " + verschluesselterSchluessel;
    }

    static String finished(SecretKey key) {
        return FINISHED + "\n" + Texts.get("tls.f.check") + ": " + verschluesseln(key, CHECK);
    }

    static String daten(String geheimtext) {
        return DATA + "\n" + Texts.get("tls.f.data") + ": " + geheimtext;
    }

    static String alert(String grund) {
        return ALERT + "\n" + Texts.get("tls.f.reason") + ": " + grund;
    }

    public static String zufall() {
        byte[] b = new byte[16];
        RANDOM.nextBytes(b);
        StringBuilder s = new StringBuilder();
        for (byte x : b) {
            s.append(String.format("%02x", x));
        }
        return s.toString();
    }

    // ------------------------------------------------------------------ plain text of encrypted segments

    /** What an encrypted segment contains; only sender and receiver know it (they have the session key). */
    public static final class Klartext {
        public final String text;
        /** 1-based part of the record if the record needed several TCP segments */
        public final int teil;
        public final int teile;

        Klartext(String text, int teil, int teile) {
            this.text = text;
            this.teil = teil;
            this.teile = teile;
        }
    }

    private static final Map<String, Klartext> ARCHIV = Collections.synchronizedMap(new LinkedHashMap<String, Klartext>(256, 0.75f, false) {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Klartext> eldest) {
            return size() > 20000;
        }
    });

    /** remembers the plain text of an application data record, split like TCP splits the record into segments */
    static void merken(String record, String klartext) {
        int teile = Math.max(1, (int) Math.ceil(record.length() / (double) MSS));
        String rest = record;
        for (int i = 1; i <= teile; i++) {
            String stueck = rest.length() < MSS ? rest : rest.substring(0, MSS);
            rest = rest.length() < MSS ? "" : rest.substring(MSS);
            ARCHIV.put(stueck, new Klartext(klartext, i, teile));
        }
    }

    /** plain text of the segment with this payload, null if it is no encrypted application data of this session */
    public static Klartext klartext(String payload) {
        return payload == null ? null : ARCHIV.get(payload);
    }

    // ------------------------------------------------------------------ server side

    /** One TLS connection at the web server; handles the handshake and decrypts the data. */
    public static final class ServerSitzung {
        private final Identitaet identitaet;
        private SecretKey sitzungsschluessel;
        private boolean fertig;

        public ServerSitzung(Identitaet identitaet) {
            this.identitaet = identitaet;
        }

        public boolean fertig() {
            return fertig;
        }

        /** Result of a received record: what to send back (raw record) and the decrypted application data. */
        public static final class Ergebnis {
            public String antwort;
            public String klartext;
            public String protokoll;
            public boolean schliessen;
        }

        public Ergebnis empfangen(String record) {
            Ergebnis e = new Ergebnis();
            String art = art(record);
            if (art == null) {
                e.protokoll = Texts.get("tls.log.noTls");
                e.antwort = alert(Texts.get("tls.alert.noTls"));
                e.schliessen = true;
                return e;
            }
            try {
                if (art.startsWith("ClientHello")) {
                    e.antwort = serverHello(identitaet.zertifikat);
                    e.protokoll = Texts.get("tls.log.hello");
                } else if (art.startsWith("ClientKeyExchange")) {
                    String verschluesselt = wert(zeilen(record)[1]);
                    Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                    c.init(Cipher.DECRYPT_MODE, identitaet.schluessel.getPrivate());
                    byte[] key = c.doFinal(Base64.getDecoder().decode(verschluesselt));
                    sitzungsschluessel = new SecretKeySpec(key, "AES");
                    fertig = true;
                    e.antwort = finished(sitzungsschluessel);
                    e.protokoll = Texts.get("tls.log.key");
                } else if (art.startsWith("Application Data") && fertig) {
                    e.klartext = entschluesseln(sitzungsschluessel, wert(zeilen(record)[1]));
                    merken(record, e.klartext);
                } else if (art.startsWith("Alert")) {
                    e.protokoll = Texts.format("tls.log.alert", wert(zeilen(record).length > 1 ? zeilen(record)[1] : ""));
                    e.schliessen = true;
                }
            } catch (Exception ex) {
                e.protokoll = Texts.get("tls.log.error");
                e.schliessen = true;
            }
            return e;
        }

        /** an application data record with this plain text */
        public String verschluesseln(String klartext) {
            String record = daten(Tls.verschluesseln(sitzungsschluessel, klartext));
            merken(record, klartext);
            return record;
        }
    }

    // ------------------------------------------------------------------ client side

    /** The certificate is not trusted: self-signed (or unknown issuer) or made for another name. */
    public static final class ZertifikatProblem extends Exception {
        private static final long serialVersionUID = 1L;
        public final Zertifikat zertifikat;
        public final boolean falscherName;
        public final boolean unbekannteStelle;

        ZertifikatProblem(Zertifikat zertifikat, boolean falscherName, boolean unbekannteStelle) {
            super("certificate");
            this.zertifikat = zertifikat;
            this.falscherName = falscherName;
            this.unbekannteStelle = unbekannteStelle;
        }
    }

    /** A TLS connection of a browser over an open TCP connection. */
    public static final class ClientSitzung {
        private final TCPSocket socket;
        private final SecretKey sitzungsschluessel;
        public final Zertifikat zertifikat;

        private ClientSitzung(TCPSocket socket, SecretKey key, Zertifikat zertifikat) {
            this.socket = socket;
            this.sitzungsschluessel = key;
            this.zertifikat = zertifikat;
        }

        /**
         * Handshake over the connected socket. Throws {@link ZertifikatProblem} if the certificate is not trusted
         * and the user has not allowed this host for this computer; the connection is then closed with an alert.
         */
        public static ClientSitzung verbinden(TCPSocket socket, InternetKnotenBetriebssystem system, String host, long timeout)
                throws Exception {
            socket.senden(clientHello(host));
            String antwort = socket.empfangen(timeout);
            if (antwort == null || !antwort.startsWith(SERVER_HELLO)) {
                throw new java.io.IOException("no TLS");
            }
            Zertifikat z = zertifikatAus(antwort);
            if (z == null) {
                throw new java.io.IOException("no certificate");
            }
            boolean stelle = z.vonZertifizierungsstelle();
            boolean name = z.gilt(host);
            Status st = new Status(z, stelle && name, !stelle, !name);
            if (!st.vertraut && hatAusnahme(system, host)) {
                st.ausnahme = true;
            }
            status(system, host, st);
            if (!st.vertraut && !st.ausnahme) {
                try {
                    socket.senden(alert(Texts.get("tls.alert.certificate")));
                } catch (Exception e) {
                    // closed anyway
                }
                throw new ZertifikatProblem(z, !name, !stelle);
            }
            byte[] key = new byte[16];
            RANDOM.nextBytes(key);
            SecretKey sk = new SecretKeySpec(key, "AES");
            Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            c.init(Cipher.ENCRYPT_MODE, z.oeffentlicherSchluessel());
            socket.senden(keyExchange(Base64.getEncoder().encodeToString(c.doFinal(key))));
            String fin = socket.empfangen(timeout);
            if (fin == null || !fin.startsWith(FINISHED) || !CHECK.equals(entschluesseln(sk, wert(zeilen(fin)[1])))) {
                throw new java.io.IOException("TLS handshake failed");
            }
            return new ClientSitzung(socket, sk, z);
        }

        public void senden(String klartext) throws Exception {
            String record = daten(verschluesseln(sitzungsschluessel, klartext));
            merken(record, klartext);
            socket.senden(record);
        }

        /** the next application data, decrypted; null if the connection ended */
        public String empfangen(long timeout) throws Exception {
            String record = timeout > 0 ? socket.empfangen(timeout) : socket.empfangen();
            if (record == null || !record.startsWith(DATA)) {
                return null;
            }
            String klar = entschluesseln(sitzungsschluessel, wert(zeilen(record)[1]));
            merken(record, klar);
            return klar;
        }
    }

    // ------------------------------------------------------------------ what a browser knows about a host

    /** result of the last handshake of a computer with a host, for the lock in the address bar */
    public static final class Status {
        public final Zertifikat zertifikat;
        public final boolean vertraut;
        public final boolean unbekannteStelle;
        public final boolean falscherName;
        public boolean ausnahme;

        Status(Zertifikat zertifikat, boolean vertraut, boolean unbekannteStelle, boolean falscherName) {
            this.zertifikat = zertifikat;
            this.vertraut = vertraut;
            this.unbekannteStelle = unbekannteStelle;
            this.falscherName = falscherName;
        }
    }

    private static final Map<InternetKnotenBetriebssystem, Map<String, Status>> STATUS = new WeakHashMap<>();
    private static final Map<InternetKnotenBetriebssystem, List<String>> AUSNAHMEN = new WeakHashMap<>();

    static synchronized void status(InternetKnotenBetriebssystem system, String host, Status s) {
        if (system != null) {
            STATUS.computeIfAbsent(system, k -> new LinkedHashMap<>()).put(host.toLowerCase(Locale.ROOT), s);
        }
    }

    /** last handshake result of this computer with this host, null if there was none */
    public static synchronized Status status(InternetKnotenBetriebssystem system, String host) {
        Map<String, Status> m = system == null || host == null ? null : STATUS.get(system);
        return m == null ? null : m.get(host.toLowerCase(Locale.ROOT));
    }

    /** the user accepted the risk for this host on this computer ("open anyway") */
    public static synchronized void ausnahme(InternetKnotenBetriebssystem system, String host) {
        if (system != null && host != null) {
            List<String> l = AUSNAHMEN.computeIfAbsent(system, k -> new ArrayList<>());
            if (!l.contains(host.toLowerCase(Locale.ROOT))) {
                l.add(host.toLowerCase(Locale.ROOT));
            }
        }
    }

    public static synchronized boolean hatAusnahme(InternetKnotenBetriebssystem system, String host) {
        List<String> l = system == null || host == null ? null : AUSNAHMEN.get(system);
        return l != null && l.contains(host.toLowerCase(Locale.ROOT));
    }

    /** forgets exceptions and states, e.g. when the simulation stops */
    public static synchronized void vergessen() {
        STATUS.clear();
        AUSNAHMEN.clear();
    }

    /** marker in a URL: open this host anyway (link on the warning page) */
    public static final String AUSNAHME_PARAMETER = "filius-trotzdem=1";

    // ------------------------------------------------------------------ warning page

    /** the page Waterwolf shows instead of a site whose certificate is not trusted */
    public static String warnseite(ZertifikatProblem p, String host, String weiterUrl) {
        StringBuilder gruende = new StringBuilder();
        if (p.unbekannteStelle) {
            gruende.append("<li>").append(Texts.format("tls.warn.issuer", esc(p.zertifikat.aussteller))).append("</li>");
        }
        if (p.falscherName) {
            gruende.append("<li>").append(p.zertifikat.namen.isEmpty() ? Texts.format("tls.warn.noname", esc(host))
                    : Texts.format("tls.warn.name", esc(p.zertifikat.namenText()), esc(host))).append("</li>");
        }
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>" + Texts.get("tls.warn.title") + "</title>"
                + "<style>body{font-family:system-ui,sans-serif;background:#fff6f5;color:#3b1d1a;margin:0;padding:40px}"
                + ".box{max-width:620px;margin:auto;background:#fff;border:2px solid #db3b3b;border-radius:14px;padding:24px 28px}"
                + "h1{font-size:22px;color:#b42a2a;margin-top:0}a{color:#b42a2a}li{margin:6px 0}</style></head><body><div class=\"box\">"
                + "<h1>&#9888; " + Texts.get("tls.warn.title") + "</h1><p>" + Texts.format("tls.warn.text", esc(host)) + "</p><ul>"
                + gruende + "</ul><p>" + Texts.get("tls.warn.risk") + "</p><p><a href=\"" + esc(weiterUrl) + "\">"
                + Texts.get("tls.warn.continue") + "</a></p></div></body></html>";
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** the URL with the marker for "open anyway" */
    public static String mitAusnahme(String url) {
        return url + (url.contains("?") ? "&" : "?") + AUSNAHME_PARAMETER;
    }

    /** the path without the marker, null if it has none */
    public static String ohneAusnahme(String pfad) {
        if (pfad == null || !pfad.contains(AUSNAHME_PARAMETER)) {
            return null;
        }
        String p = pfad.replace("&" + AUSNAHME_PARAMETER, "").replace("?" + AUSNAHME_PARAMETER + "&", "?").replace("?" + AUSNAHME_PARAMETER, "");
        return p.isEmpty() ? "/" : p;
    }
}
