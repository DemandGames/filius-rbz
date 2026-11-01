package filius.modern.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import filius.modern.tls.Tls;
import filius.rahmenprogramm.Base64;
import filius.rahmenprogramm.I18n;
import filius.rahmenprogramm.Information;
import filius.rahmenprogramm.ResourceUtil;
import filius.software.system.InternetKnotenBetriebssystem;
import filius.software.transportschicht.TCPSocket;
import filius.software.www.HTTPNachricht;

/**
 * Network bridge of the Waterwolf browser engine: every http/https request that WebKit makes is
 * sent as a real HTTP request over a simulated TCP connection from the computer the browser runs on.
 * So pages, stylesheets, scripts, images and fetch() calls all travel through the simulated network
 * (DNS, routing, firewall) and appear in the data exchange window. Nothing reaches the real network.
 *
 * The bridge is a {@code http}/{@code https} URL handler for the whole JVM. It only intercepts
 * connections opened by WebKit's loader; every other caller gets the JDK's own handler.
 * https addresses go to port 443 and use a TLS session of the simulation (filius.modern.tls.Tls); a
 * certificate that is not trusted gives a warning page with a link to open the page anyway.
 */
public final class SimHttp {

    private SimHttp() {}

    /** Marker in the user agent of a browser: "FiliusHost/<id>". */
    public static final String UA_MARK = "FiliusHost/";
    private static final Pattern UA_PATTERN = Pattern.compile("FiliusHost/(\\d+)");
    private static final String WEBKIT_LOADER = "com.sun.webkit.network.URLLoader";
    private static final long TIMEOUT_MS = 120_000;

    private static volatile boolean installed;
    private static URL defaultHttp;
    private static URL defaultHttps;

    private static final AtomicLong ids = new AtomicLong(1);
    /**
     * One lock per target server. A Filius server accepts connections one at a time (its server
     * socket has a single listening socket), so parallel connection attempts of the engine (WebKit
     * opens several at once for stylesheets, scripts and images) would disturb each other. Requests to
     * the same server therefore run one after the other; requests to different servers still run in
     * parallel.
     */
    private static final Map<String, Object> serverLocks = new ConcurrentHashMap<>();
    private static final Map<Long, WeakReference<InternetKnotenBetriebssystem>> browsers = new ConcurrentHashMap<>();
    private static volatile long lastActive = -1;

    /** Installs the URL handler. Must run before any other code creates http URLs. */
    public static synchronized boolean install() {
        if (installed) {
            return true;
        }
        try {
            // capture the JDK's own handlers before the factory replaces them
            defaultHttp = new URL("http://localhost/");
            defaultHttps = new URL("https://localhost/");
            URL.setURLStreamHandlerFactory(protocol -> {
                String p = protocol.toLowerCase(Locale.ROOT);
                return "http".equals(p) || "https".equals(p) ? new Handler(p) : null;
            });
            installed = true;
        } catch (Throwable t) {
            System.err.println("Filius RBZ: web bridge not available: " + t);
            installed = false;
        }
        return installed;
    }

    public static boolean isInstalled() {
        return installed;
    }

    /** Registers a browser window that belongs to a simulated computer; returns its id for the user agent. */
    public static long register(InternetKnotenBetriebssystem system) {
        long id = ids.getAndIncrement();
        browsers.put(id, new WeakReference<>(system));
        lastActive = id;
        return id;
    }

    public static void unregister(long id) {
        browsers.remove(id);
    }

    /** The browser the user worked with last; used if a request carries no browser id. */
    public static void markActive(long id) {
        lastActive = id;
    }

    // ------------------------------------------------------------------ handler

    private static final class Handler extends URLStreamHandler {
        private final String protocol;

        Handler(String protocol) {
            this.protocol = protocol;
        }

        @Override
        protected int getDefaultPort() {
            return "https".equals(this.protocol) ? 443 : 80;
        }

        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            return this.openConnection(u, null);
        }

        @Override
        protected URLConnection openConnection(URL u, Proxy proxy) throws IOException {
            if (fromWebKit()) {
                return new Connection(u);
            }
            URL real = new URL("https".equals(this.protocol) ? defaultHttps : defaultHttp, u.toExternalForm());
            return proxy == null ? real.openConnection() : real.openConnection(proxy);
        }
    }

    private static boolean fromWebKit() {
        return StackWalker.getInstance().walk(frames -> frames.anyMatch(f -> WEBKIT_LOADER.equals(f.getClassName())));
    }

    // ------------------------------------------------------------------ connection

    /** One HTTP exchange over a simulated TCP connection. */
    static final class Connection extends HttpURLConnection {
        private final Map<String, List<String>> requestHeaders = new LinkedHashMap<>();
        private final ByteArrayOutputStream body = new ByteArrayOutputStream();
        private boolean exchanged;
        private byte[] responseBody = new byte[0];
        private final Map<String, List<String>> responseHeaders = new LinkedHashMap<>();

        Connection(URL url) {
            super(url);
        }

        @Override
        public void setRequestProperty(String key, String value) {
            super.setRequestProperty(key, value);
            if (key != null) {
                this.requestHeaders.put(key.toLowerCase(Locale.ROOT), new ArrayList<>(List.of(value == null ? "" : value)));
            }
        }

        @Override
        public void addRequestProperty(String key, String value) {
            super.addRequestProperty(key, value);
            if (key != null) {
                this.requestHeaders.computeIfAbsent(key.toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(value == null ? "" : value);
            }
        }

        private String requestHeader(String name) {
            List<String> v = this.requestHeaders.get(name);
            return v == null || v.isEmpty() ? null : v.get(v.size() - 1);
        }

        @Override
        public void connect() {
            this.connected = true;
        }

        @Override
        public void disconnect() {}

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public OutputStream getOutputStream() {
            return this.body;
        }

        private synchronized void exchange() {
            if (this.exchanged) {
                return;
            }
            this.exchanged = true;
            InternetKnotenBetriebssystem system = this.source();
            if (system == null) {
                this.fail(502, "sw_webbrowser_msg1");
                return;
            }
            String host = this.url.getHost();
            int port = this.url.getPort() > 0 ? this.url.getPort() : this.url.getDefaultPort();
            String path = this.url.getFile();
            if (path == null || path.isEmpty()) {
                path = "/";
            }
            boolean https = "https".equalsIgnoreCase(this.url.getProtocol());
            // link of the warning page: open this host anyway
            String ohne = Tls.ohneAusnahme(path);
            if (ohne != null) {
                Tls.ausnahme(system, host);
                path = ohne;
            }
            HTTPNachricht request = new HTTPNachricht(HTTPNachricht.CLIENT);
            request.setMethod("POST".equalsIgnoreCase(this.method) ? HTTPNachricht.POST : HTTPNachricht.GET);
            request.setPfad(path);
            request.setHost(host);
            if (HTTPNachricht.POST.equals(request.getMethod())) {
                request.setDaten(this.body.toString(StandardCharsets.UTF_8));
            }
            String answer = null;
            String warnung = null;
            Object serverLock = serverLocks.computeIfAbsent(host.toLowerCase(Locale.ROOT) + ":" + port, k -> new Object());
            synchronized (serverLock) {
                TCPSocket socket = null;
                try {
                    socket = new TCPSocket(system, host, port);
                    socket.verbinden();
                    if (https) {
                        Tls.ClientSitzung tls = Tls.ClientSitzung.verbinden(socket, system, host, TIMEOUT_MS);
                        tls.senden(request.toString());
                        answer = tls.empfangen(TIMEOUT_MS);
                    } else {
                        socket.senden(request.toString());
                        answer = socket.empfangen(TIMEOUT_MS);
                    }
                } catch (Tls.ZertifikatProblem p) {
                    String weiter = this.url.getProtocol() + "://" + host + (this.url.getPort() > 0 ? ":" + this.url.getPort() : "") + path;
                    warnung = Tls.warnseite(p, host, Tls.mitAusnahme(weiter));
                } catch (Exception e) {
                    answer = null;
                } finally {
                    if (socket != null) {
                        try {
                            socket.schliessen();
                        } catch (Exception e) {
                            // already closed
                        }
                    }
                }
            }
            if (warnung != null) {
                this.respond(200, "text/html; charset=utf-8", warnung.getBytes(StandardCharsets.UTF_8));
                return;
            }
            if (answer == null) {
                this.fail(502, "sw_webbrowser_msg1");
                return;
            }
            HTTPNachricht response = new HTTPNachricht(answer);
            int status = response.getStatusCode() > 0 ? response.getStatusCode() : 502;
            String type = normalizeType(response.getContentType(), path);
            String data = response.getDaten();
            byte[] bytes;
            if (data == null) {
                bytes = new byte[0];
            } else if (isBinary(type)) {
                bytes = decodeBase64(data);
            } else {
                bytes = data.getBytes(StandardCharsets.UTF_8);
            }
            if (status >= 400 && bytes.length == 0) {
                this.fail(status, null);
                return;
            }
            this.respond(status, type, bytes);
        }

        private InternetKnotenBetriebssystem source() {
            long id = -1;
            String ua = this.requestHeader("user-agent");
            if (ua != null) {
                Matcher m = UA_PATTERN.matcher(ua);
                if (m.find()) {
                    id = Long.parseLong(m.group(1));
                }
            }
            if (id < 0) {
                id = lastActive;
            }
            WeakReference<InternetKnotenBetriebssystem> ref = browsers.get(id);
            InternetKnotenBetriebssystem system = ref == null ? null : ref.get();
            return system != null && system.isStarted() ? system : null;
        }

        private void respond(int status, String type, byte[] bytes) {
            this.responseCode = status;
            this.responseMessage = HTTPNachricht.holeStatusNachricht(status);
            this.responseBody = bytes;
            this.responseHeaders.clear();
            this.responseHeaders.put(null, Collections.singletonList("HTTP/1.1 " + status + " " + this.responseMessage));
            this.responseHeaders.put("Content-Type", Collections.singletonList(type));
            this.responseHeaders.put("Content-Length", Collections.singletonList(Integer.toString(bytes.length)));
            this.responseHeaders.put("Cache-Control", Collections.singletonList("no-store"));
        }

        /** Error page in the style of Filius' own browser messages. */
        private void fail(int status, String messageKey) {
            String html;
            if (messageKey != null) {
                html = page(I18n.messages.getString(messageKey));
            } else {
                html = errorTemplate(status);
            }
            this.respond(status, "text/html; charset=utf-8", html.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public int getResponseCode() {
            this.exchange();
            return this.responseCode;
        }

        @Override
        public String getResponseMessage() {
            this.exchange();
            return this.responseMessage;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            this.exchange();
            if (this.responseCode >= 400) {
                throw new java.io.FileNotFoundException(this.url.toString());
            }
            return new ByteArrayInputStream(this.responseBody);
        }

        @Override
        public InputStream getErrorStream() {
            if (!this.exchanged || this.responseCode < 400) {
                return null;
            }
            return new ByteArrayInputStream(this.responseBody);
        }

        @Override
        public String getHeaderField(String name) {
            this.exchange();
            if (name == null) {
                return this.getHeaderField(0);
            }
            for (Map.Entry<String, List<String>> e : this.responseHeaders.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) {
                    return e.getValue().get(0);
                }
            }
            return null;
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            this.exchange();
            return Collections.unmodifiableMap(this.responseHeaders);
        }

        @Override
        public String getHeaderFieldKey(int n) {
            this.exchange();
            int i = 0;
            for (String k : this.responseHeaders.keySet()) {
                if (i++ == n) {
                    return k;
                }
            }
            return null;
        }

        @Override
        public String getHeaderField(int n) {
            this.exchange();
            int i = 0;
            for (List<String> v : this.responseHeaders.values()) {
                if (i++ == n) {
                    return v.get(0);
                }
            }
            return null;
        }

        @Override
        public String getContentType() {
            return this.getHeaderField("Content-Type");
        }

        @Override
        public String getContentEncoding() {
            return null;
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Content type for WebKit: fixes old Filius types and adds the character set for text. */
    static String normalizeType(String type, String path) {
        String t = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty() || "text/plain".equals(t)) {
            String byName = MimeTypes.forName(path);
            if (byName != null) {
                t = byName;
            }
        }
        if (t.isEmpty()) {
            t = "application/octet-stream";
        }
        if ("image/jpg".equals(t)) {
            t = "image/jpeg";
        }
        if (!isBinary(t) && !t.contains("charset")) {
            t += "; charset=utf-8";
        }
        return t;
    }

    /** Binary types are transported Base64-encoded by the Filius web server. */
    static boolean isBinary(String type) {
        String t = type.toLowerCase(Locale.ROOT);
        return (t.startsWith("image/") && !t.startsWith("image/svg")) || t.startsWith("audio/") || t.startsWith("video/")
                || t.startsWith("font/") || t.startsWith("application/octet-stream") || t.startsWith("application/pdf");
    }

    private static byte[] decodeBase64(String data) {
        try {
            return Base64.decode(data.trim());
        } catch (Exception e) {
            return data.getBytes(StandardCharsets.ISO_8859_1);
        }
    }

    private static String page(String message) {
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><meta name=\"filius-error\" content=\"1\">"
                + "<title>Waterwolf</title><style>body{font-family:system-ui,sans-serif;color:#1c2b3a;margin:40px}"
                + "h1{font-size:20px}</style></head><body>" + message + "</body></html>";
    }

    private static String errorTemplate(int status) {
        String text;
        try {
            String path = ResourceUtil.getResourcePath("tmpl/http_fehler_" + Information.getInformation().getLocaleOrDefault() + ".txt");
            text = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            text = "<html><body><h1>:code: :meldung:</h1></body></html>";
        }
        text = text.replace(":code:", Integer.toString(status)).replace(":meldung:", HTTPNachricht.holeStatusNachricht(status));
        return text.replaceFirst("(?i)<head>", "<head><meta charset=\"utf-8\"><meta name=\"filius-error\" content=\"1\">");
    }
}
