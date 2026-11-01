package filius.modern.web;

import java.awt.Component;

/**
 * A web page view backed by a real browser engine (WebKit from JavaFX). The implementation lives in
 * lib/javafx/filius-rbz-fx.jar and is loaded only when the engine is available, see
 * {@link FxSupport}.
 */
public interface BrowserView {

    /** The Swing component that shows the page. */
    Component component();

    /** Loads an http URL through the simulated network. */
    void load(String url);

    /** Shows an HTML page that does not come from the network (start page). */
    void loadContent(String html);

    void back();

    void forward();

    void reload();

    /** Frees the engine; the view must not be used afterwards. */
    void dispose();

    void setListener(Listener listener);

    /** Callbacks; they arrive on the Swing event thread. */
    interface Listener {
        void locationChanged(String url);

        void titleChanged(String title);

        void historyChanged(boolean canGoBack, boolean canGoForward);

        void busyChanged(boolean busy);

        /** A page finished loading. errorPage is true for Filius' own error pages. */
        void loaded(String url, boolean errorPage);
    }
}
