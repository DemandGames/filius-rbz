package filius.modern.web;

import java.util.Locale;
import java.util.Map;

/** Content types of the files a Filius web server delivers, chosen by file extension. */
public final class MimeTypes {

    private MimeTypes() {}

    private static final Map<String, String> BY_EXTENSION = Map.ofEntries(
            Map.entry("html", "text/html"),
            Map.entry("htm", "text/html"),
            Map.entry("css", "text/css"),
            Map.entry("js", "text/javascript"),
            Map.entry("mjs", "text/javascript"),
            Map.entry("json", "application/json"),
            Map.entry("xml", "application/xml"),
            Map.entry("txt", "text/plain"),
            Map.entry("csv", "text/csv"),
            Map.entry("md", "text/markdown"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"),
            Map.entry("bmp", "image/bmp"),
            // Filius' own browser expects image/jpg; the engine bridge turns it into image/jpeg
            Map.entry("jpg", "image/jpg"),
            Map.entry("jpeg", "image/jpg"),
            Map.entry("webp", "image/webp"),
            Map.entry("ico", "image/x-icon"),
            Map.entry("woff", "font/woff"),
            Map.entry("woff2", "font/woff2"),
            Map.entry("ttf", "font/ttf"),
            Map.entry("otf", "font/otf"),
            Map.entry("mp3", "audio/mpeg"),
            Map.entry("wav", "audio/wav"),
            Map.entry("ogg", "audio/ogg"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("webm", "video/webm"),
            Map.entry("pdf", "application/pdf"));

    /** Content type for a file name or path, or null if the extension is unknown. */
    public static String forName(String name) {
        if (name == null) {
            return null;
        }
        String n = name;
        int q = n.indexOf('?');
        if (q >= 0) {
            n = n.substring(0, q);
        }
        int dot = n.lastIndexOf('.');
        int slash = n.lastIndexOf('/');
        if (dot < 0 || dot < slash) {
            return null;
        }
        return BY_EXTENSION.get(n.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    /** True for content that is text (and therefore must not be sent Base64-encoded). */
    public static boolean isText(String type) {
        if (type == null) {
            return false;
        }
        String t = type.toLowerCase(Locale.ROOT);
        return t.startsWith("text/") || t.startsWith("image/svg") || t.equals("application/json") || t.equals("application/xml")
                || t.equals("application/javascript");
    }

    /**
     * Filius stores imported files whose type it does not know as text Base64-encoded. For text
     * types (stylesheets, scripts, ...) such content is decoded before it is delivered.
     */
    public static String textContent(String content, String fileType) {
        if (content == null || "text".equals(fileType) || !looksLikeBase64(content)) {
            return content;
        }
        try {
            byte[] bytes = filius.rahmenprogramm.Base64.decode(content);
            if (bytes == null) {
                return content;
            }
            String text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            if (text.startsWith("﻿")) {
                text = text.substring(1);
            }
            return text;
        } catch (Exception e) {
            return content;
        }
    }

    private static boolean looksLikeBase64(String s) {
        String t = s.trim();
        if (t.length() < 4) {
            return false;
        }
        int letters = 0;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '\r' || c == '\n') {
                continue;
            }
            boolean ok = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '+' || c == '/' || c == '=';
            if (!ok) {
                return false;
            }
            letters++;
        }
        return letters % 4 == 0;
    }
}
