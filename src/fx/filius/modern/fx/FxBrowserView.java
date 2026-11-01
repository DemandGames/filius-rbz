package filius.modern.fx;

import java.awt.Component;
import java.io.File;
import java.util.Optional;

import javax.swing.SwingUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import filius.modern.web.BrowserView;
import filius.modern.web.SimHttp;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;

/**
 * Waterwolf's page view with a real browser engine: JavaFX WebView (WebKit) in a Swing JFXPanel.
 * All network access of the engine goes through {@link SimHttp} and therefore through the simulated
 * network of the computer the browser belongs to.
 */
public final class FxBrowserView implements BrowserView {

    private final long browserId;
    private final JFXPanel panel;
    private WebEngine engine;
    private volatile Listener listener;
    private volatile Runnable pending;
    private boolean busy;

    public FxBrowserView(long browserId) throws Exception {
        this.browserId = browserId;
        JFXPanel[] created = new JFXPanel[1];
        Runnable make = () -> created[0] = new JFXPanel();
        if (SwingUtilities.isEventDispatchThread()) {
            make.run();
        } else {
            SwingUtilities.invokeAndWait(make);
        }
        this.panel = created[0];
        Platform.setImplicitExit(false);
        Platform.runLater(this::init);
    }

    private void init() {
        WebView view = new WebView();
        view.setContextMenuEnabled(false);
        this.engine = view.getEngine();
        this.engine.setJavaScriptEnabled(true);
        this.engine.setUserAgent(this.engine.getUserAgent() + " Waterwolf/2.9 " + SimHttp.UA_MARK + this.browserId);
        try {
            File data = new File(System.getProperty("java.io.tmpdir"), "filius-waterwolf");
            data.mkdirs();
            this.engine.setUserDataDirectory(data);
        } catch (Exception e) {
            // default directory
        }
        // standard dialogs of JavaScript
        this.engine.setOnAlert(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, e.getData(), ButtonType.OK);
            a.setHeaderText(null);
            a.setTitle("Waterwolf");
            a.showAndWait();
        });
        this.engine.setConfirmHandler(message -> {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
            a.setHeaderText(null);
            a.setTitle("Waterwolf");
            Optional<ButtonType> r = a.showAndWait();
            return r.isPresent() && r.get() == ButtonType.OK;
        });
        this.engine.setPromptHandler(p -> {
            TextInputDialog d = new TextInputDialog(p.getDefaultValue());
            d.setHeaderText(null);
            d.setContentText(p.getMessage());
            d.setTitle("Waterwolf");
            return d.showAndWait().orElse(null);
        });
        // links with target="_blank" and window.open() open in the same view
        this.engine.setCreatePopupHandler(features -> this.engine);

        this.engine.locationProperty().addListener((obs, old, url) -> {
            if (url != null && !url.isEmpty() && !url.startsWith("http:") && !url.startsWith("https:") && !url.startsWith("about:")
                    && !url.startsWith("data:")) {
                // the browser of a simulated computer must not open files of the real computer
                Platform.runLater(() -> this.engine.loadContent("<html><body></body></html>"));
                return;
            }
            this.swing(l -> l.locationChanged(url));
        });
        this.engine.titleProperty().addListener((obs, old, title) -> this.swing(l -> l.titleChanged(title)));
        WebHistory history = this.engine.getHistory();
        history.currentIndexProperty().addListener((obs, old, idx) -> this.fireHistory());
        history.getEntries().addListener((javafx.collections.ListChangeListener<WebHistory.Entry>) c -> this.fireHistory());
        this.engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            boolean nowBusy = state == Worker.State.SCHEDULED || state == Worker.State.RUNNING;
            if (nowBusy != this.busy) {
                this.busy = nowBusy;
                this.swing(l -> l.busyChanged(nowBusy));
            }
            if (state == Worker.State.SUCCEEDED) {
                String url = this.engine.getLocation();
                boolean error = isErrorPage(this.engine.getDocument());
                this.swing(l -> l.loaded(url, error));
            }
        });

        this.panel.setScene(new Scene(view));
        Runnable p = this.pending;
        this.pending = null;
        if (p != null) {
            p.run();
        }
    }

    private static boolean isErrorPage(Document doc) {
        if (doc == null) {
            return true;
        }
        NodeList metas = doc.getElementsByTagName("meta");
        for (int i = 0; i < metas.getLength(); i++) {
            if (metas.item(i) instanceof Element && "filius-error".equalsIgnoreCase(((Element) metas.item(i)).getAttribute("name"))) {
                return true;
            }
        }
        return false;
    }

    private void fireHistory() {
        WebHistory h = this.engine.getHistory();
        int idx = h.getCurrentIndex();
        int size = h.getEntries().size();
        boolean back = idx > 0;
        boolean forward = idx >= 0 && idx < size - 1;
        this.swing(l -> l.historyChanged(back, forward));
    }

    private void swing(java.util.function.Consumer<Listener> call) {
        Listener l = this.listener;
        if (l != null) {
            SwingUtilities.invokeLater(() -> call.accept(l));
        }
    }

    /** Runs on the JavaFX thread once the engine exists. */
    private void fx(Runnable r) {
        Platform.runLater(() -> {
            if (this.engine == null) {
                this.pending = r;
            } else {
                r.run();
            }
        });
    }

    @Override
    public Component component() {
        return this.panel;
    }

    @Override
    public void load(String url) {
        SimHttp.markActive(this.browserId);
        this.fx(() -> this.engine.load(url));
    }

    @Override
    public void loadContent(String html) {
        this.fx(() -> this.engine.loadContent(html));
    }

    @Override
    public void back() {
        SimHttp.markActive(this.browserId);
        this.fx(() -> {
            if (this.engine.getHistory().getCurrentIndex() > 0) {
                this.engine.getHistory().go(-1);
            }
        });
    }

    @Override
    public void forward() {
        SimHttp.markActive(this.browserId);
        this.fx(() -> {
            WebHistory h = this.engine.getHistory();
            if (h.getCurrentIndex() < h.getEntries().size() - 1) {
                h.go(1);
            }
        });
    }

    @Override
    public void reload() {
        SimHttp.markActive(this.browserId);
        this.fx(() -> this.engine.reload());
    }

    @Override
    public void dispose() {
        this.listener = null;
        this.fx(() -> this.engine.load(null));
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }
}
