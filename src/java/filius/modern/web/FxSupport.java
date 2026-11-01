package filius.modern.web;

import java.io.File;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import filius.modern.Prefs;

/**
 * Loads the browser engine (JavaFX WebKit) at run time from lib/javafx next to filius-rbz.jar.
 *
 * The engine needs Java 21 or newer with the modules jdk.jsobject, jdk.xml.dom and
 * jdk.unsupported.desktop (the Java runtime shipped with Filius (RBZ Wirtschaft Kiel Version) has them) and the web bridge
 * {@link SimHttp}. If anything is missing, the browser falls back to the classic page view, so
 * Filius always works. Teachers can switch the engine off with modern-browser=klassisch in
 * config/filius.ini.
 */
public final class FxSupport {

    private FxSupport() {}

    private static final String[] REQUIRED_JDK_MODULES = { "java.desktop", "jdk.jsobject", "jdk.xml.dom", "jdk.unsupported",
            "jdk.unsupported.desktop" };
    private static final String VIEW_CLASS = "filius.modern.fx.FxBrowserView";

    private static Boolean available;
    private static ClassLoader loader;
    private static String reason = "";

    /** Prepares the JVM; call once, as early as possible in main. */
    public static void prepareJvm() {
        // WebKit must use java.net.URLConnection (and with it the simulated network), not java.net.http
        System.setProperty("com.sun.webkit.useHTTP2Loader", "false");
        SimHttp.install();
    }

    public static synchronized boolean isAvailable() {
        if (available == null) {
            available = load();
            if (!available) {
                System.err.println("Filius RBZ: classic browser view (" + reason + ")");
            }
        }
        return available;
    }

    public static String reason() {
        return reason;
    }

    private static boolean load() {
        try {
            if (!Prefs.browserEngineAllowed()) {
                reason = "switched off in filius.ini";
                return false;
            }
            if (!SimHttp.isInstalled()) {
                reason = "web bridge not installed";
                return false;
            }
            if (Runtime.version().feature() < 21) {
                reason = "Java " + Runtime.version().feature() + " is too old";
                return false;
            }
            for (String m : REQUIRED_JDK_MODULES) {
                if (ModuleLayer.boot().findModule(m).isEmpty()) {
                    reason = "Java module " + m + " missing";
                    return false;
                }
            }
            Path dir = libDir();
            if (dir == null || !Files.isRegularFile(dir.resolve("filius-rbz-fx.jar"))) {
                reason = "lib/javafx not found";
                return false;
            }
            ModuleFinder finder = ModuleFinder.of(dir);
            ModuleLayer boot = ModuleLayer.boot();
            Configuration cf = boot.configuration().resolve(finder, ModuleFinder.of(),
                    Set.of("javafx.base", "javafx.graphics", "javafx.controls", "javafx.web", "javafx.swing", "filius.modern.fx"));
            ModuleLayer layer = boot.defineModulesWithOneLoader(cf, ClassLoader.getSystemClassLoader());
            loader = layer.findLoader("filius.modern.fx");
            Class.forName(VIEW_CLASS, true, loader);
            return true;
        } catch (Throwable t) {
            reason = t.toString();
            return false;
        }
    }

    /** Creates a view for the browser of one simulated computer (browser id from {@link SimHttp#register}). */
    public static BrowserView createView(long browserId) {
        if (!isAvailable()) {
            return null;
        }
        Thread current = Thread.currentThread();
        ClassLoader previous = current.getContextClassLoader();
        try {
            current.setContextClassLoader(loader);
            Class<?> c = Class.forName(VIEW_CLASS, true, loader);
            return (BrowserView) c.getConstructor(long.class).newInstance(browserId);
        } catch (Throwable t) {
            Throwable root = t;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            reason = root.toString();
            System.err.println("Filius RBZ: browser engine failed, using the classic page view: " + reason);
            // do not try again for every browser window
            available = false;
            return null;
        } finally {
            current.setContextClassLoader(previous);
        }
    }

    private static Path libDir() throws URISyntaxException {
        File jar = new File(FxSupport.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        File base = jar.isFile() ? jar.getParentFile() : jar;
        Path p = base.toPath().resolve("lib").resolve("javafx");
        if (Files.isDirectory(p)) {
            return p;
        }
        // development layout: skin/build/classes -> ../../lib/javafx is not used; allow an override
        String override = System.getProperty("filius.modern.javafx");
        return override == null ? null : Path.of(override);
    }
}
