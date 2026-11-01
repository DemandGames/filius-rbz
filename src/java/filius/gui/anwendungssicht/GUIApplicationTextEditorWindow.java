/*
 * Text editor of Filius 2.9.3 (decompiled), rebuilt for Filius (RBZ Wirtschaft Kiel Version): a code editor
 * with syntax colours for HTML, CSS and JavaScript (as in VS Code), line numbers, a search field
 * (Ctrl+F, Enter, magnifier), a status line and the shortcuts Ctrl+S, Ctrl+Z and Ctrl+Y.
 * Files, dialogs and the behaviour towards the file system are those of the original.
 */
package filius.gui.anwendungssicht;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Observable;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.tree.DefaultMutableTreeNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import filius.modern.Texts;
import filius.modern.Theme;
import filius.modern.editor.CodeFlaeche;
import filius.modern.editor.Suchleiste;
import filius.modern.editor.Syntax;
import filius.modern.editor.Zeilennummern;
import filius.software.system.Betriebssystem;
import filius.software.system.Datei;

public class GUIApplicationTextEditorWindow extends GUIApplicationWindow {
    private static Logger LOG = LoggerFactory.getLogger(GUIApplicationTextEditorWindow.class);
    private static final long serialVersionUID = 1L;
    private final CodeFlaeche editorField = new CodeFlaeche();
    private final Suchleiste suche = new Suchleiste(this.editorField);
    private final JLabel status = new JLabel(" ");
    private JPanel backPanel;
    private Datei aktuelleDatei = null;
    private String original = "";
    private DefaultMutableTreeNode arbeitsVerzeichnis;

    public GUIApplicationTextEditorWindow(GUIDesktopPanel desktop, String appName) {
        super(desktop, appName);
        this.editorField.setEditable(true);
        this.arbeitsVerzeichnis = this.holeAnwendung().getSystemSoftware().getDateisystem().getRoot();
        String dateiName = this.holeParameter()[0];
        if (!dateiName.equals("")) {
            Datei datei;
            if (this.arbeitsVerzeichnis == null) {
                this.arbeitsVerzeichnis = this.holeAnwendung().getSystemSoftware().getDateisystem().getRoot();
            }
            if ((datei = this.holeAnwendung().getSystemSoftware().getDateisystem().holeDatei(this.arbeitsVerzeichnis, dateiName)) != null) {
                this.aktuelleDatei = datei;
            }
            this.updateFromFile();
        }

        JScrollPane tpPane = new JScrollPane(this.editorField);
        tpPane.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Theme.LINE));
        tpPane.setRowHeaderView(new Zeilennummern(this.editorField));
        tpPane.getViewport().setBackground(Theme.farbe(0xFFFFFF, 0x141C25));
        tpPane.getVerticalScrollBar().setUnitIncrement(18);
        tpPane.getHorizontalScrollBar().setUnitIncrement(18);

        this.backPanel = new JPanel(new BorderLayout());
        this.backPanel.setBackground(Theme.SURFACE_2);
        this.backPanel.add((Component) tpPane, BorderLayout.CENTER);
        this.add((Component) this.backPanel, BorderLayout.CENTER);

        JPanel dateiKnoepfe = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        dateiKnoepfe.setOpaque(false);
        dateiKnoepfe.add(this.knopf(messages.getString("texteditor_msg3"), this::neu));
        dateiKnoepfe.add(this.knopf(messages.getString("texteditor_msg4"), this::oeffnen));
        dateiKnoepfe.add(this.knopf(messages.getString("texteditor_msg5"), this::speichern));
        dateiKnoepfe.add(this.knopf(messages.getString("texteditor_msg6"), this::speichernUnter));
        JPanel menubar = new JPanel(new BorderLayout(6, 0));
        menubar.setOpaque(false);
        menubar.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 6));
        menubar.add(dateiKnoepfe, BorderLayout.WEST);
        menubar.add(this.suche, BorderLayout.EAST);
        this.backPanel.add((Component) menubar, BorderLayout.NORTH);

        this.status.setFont(Theme.font(Font.PLAIN, 11.5f));
        this.status.setForeground(Theme.INK_2);
        this.status.setBorder(BorderFactory.createEmptyBorder(3, 10, 4, 10));
        this.backPanel.add(this.status, BorderLayout.SOUTH);
        this.editorField.addCaretListener(e -> this.statusZeigen());
        this.editorField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                GUIApplicationTextEditorWindow.this.geaendert();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                GUIApplicationTextEditorWindow.this.geaendert();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });

        int strg = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        this.kurzbefehl(KeyStroke.getKeyStroke(KeyEvent.VK_F, strg), "suchen", this.suche::oeffnen);
        this.kurzbefehl(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "weiter", () -> this.springe(true));
        this.kurzbefehl(KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK), "zurueck", () -> this.springe(false));
        this.kurzbefehl(KeyStroke.getKeyStroke(KeyEvent.VK_S, strg), "speichern", this::speichern);
        this.statusZeigen();
        SwingUtilities.invokeLater(() -> this.editorField.requestFocusInWindow());
    }

    private JButton knopf(String text, Runnable r) {
        JButton b = new JButton(text);
        b.setMargin(new java.awt.Insets(2, 8, 2, 8));
        b.setFocusable(false);
        b.addActionListener(e -> r.run());
        return b;
    }

    private void kurzbefehl(KeyStroke taste, String name, Runnable r) {
        this.backPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(taste, name);
        this.backPanel.getActionMap().put(name, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                r.run();
            }
        });
    }

    private void springe(boolean vor) {
        this.suche.springe(vor);
    }

    /** language, line and column, saved or not */
    private void statusZeigen() {
        if (this.status == null || this.editorField == null) {
            return;
        }
        String sprache;
        switch (this.editorField.getSprache()) {
        case HTML:
            sprache = "HTML";
            break;
        case CSS:
            sprache = "CSS";
            break;
        case JS:
            sprache = "JavaScript";
            break;
        default:
            sprache = "Text";
        }
        int zeile = 1;
        int spalte = 1;
        try {
            int pos = this.editorField.getCaretPosition();
            zeile = this.editorField.getLineOfOffset(pos) + 1;
            spalte = pos - this.editorField.getLineStartOffset(zeile - 1) + 1;
        } catch (BadLocationException e) {
            // start of the text
        }
        boolean englisch = Texts.englisch();
        String ort = englisch ? "Ln " + zeile + ", Col " + spalte : "Zeile " + zeile + ", Spalte " + spalte;
        String gespeichert = this.aktuelleDatei == null || this.istGeaendert() ? Texts.get("editor.changed") : Texts.get("editor.saved");
        this.status.setText(sprache + "   ·   " + ort + "   ·   " + gespeichert);
        this.status.setForeground(this.aktuelleDatei == null || this.istGeaendert() ? Theme.farbe(0xB0640A, 0xE39B4A) : Theme.INK_2);
    }

    private boolean istGeaendert() {
        return this.original == null || !this.original.equals(this.editorField.getText());
    }

    private void geaendert() {
        if (this.aktuelleDatei == null) {
            this.editorField.setSprache(Syntax.sprache(null, this.editorField.getText()));
        }
        this.titelZeigen();
        this.statusZeigen();
    }

    private void titelZeigen() {
        if (this.aktuelleDatei != null) {
            this.setTitle(this.anwendung, this.aktuelleDatei.getName() + (this.istGeaendert() ? " •" : ""));
        } else {
            this.setTitle(this.anwendung);
        }
    }

    public void speichern() {
        if (this.aktuelleDatei != null) {
            this.original = this.editorField.getText();
            this.aktuelleDatei.setDateiInhalt(this.original);
            this.titelZeigen();
            this.statusZeigen();
        } else {
            this.speichernUnter();
        }
    }

    public void speichernUnter() {
        DMTNFileChooser fc = new DMTNFileChooser((Betriebssystem) this.holeAnwendung().getSystemSoftware());
        int rueckgabe = fc.saveDialog();
        if (rueckgabe == 1) {
            String dateiNameNeu = fc.getAktuellerDateiname();
            Datei tmpFile = new Datei(dateiNameNeu, messages.getString("texteditor_msg8"), this.editorField.getText());
            this.holeAnwendung().getSystemSoftware().getDateisystem().speicherDatei(fc.getAktuellerOrdner(), tmpFile);
            this.changeCurrentFile(tmpFile);
        }
    }

    public void changeCurrentFile(Datei tmpFile) {
        if (this.aktuelleDatei != null) {
            this.aktuelleDatei.deleteObserver(this);
        }
        this.aktuelleDatei = tmpFile;
        this.updateFromFile();
        if (this.aktuelleDatei != null) {
            this.aktuelleDatei.addObserver(this);
        }
    }

    public void oeffnen() {
        DMTNFileChooser fc = new DMTNFileChooser((Betriebssystem) this.holeAnwendung().getSystemSoftware());
        int rueckgabe = fc.openDialog();
        if (rueckgabe == 1) {
            String aktuellerDateiname = fc.getAktuellerDateiname();
            Datei tmpFile = this.holeAnwendung().getSystemSoftware().getDateisystem().holeDatei(fc.getAktuellerOrdner(), aktuellerDateiname);
            this.changeCurrentFile(tmpFile);
        } else {
            LOG.debug("Fehler beim oeffnen einer Datei");
        }
    }

    private void updateFromFile() {
        if (this.aktuelleDatei != null) {
            this.original = this.aktuelleDatei.getDateiInhalt();
            if (this.original == null) {
                this.original = "";
            }
            this.editorField.setSprache(Syntax.sprache(this.aktuelleDatei.getName(), this.original));
            this.editorField.neuerText(this.original);
            this.titelZeigen();
            // an opened file shows its end, like the window when it comes to the front
            filius.modern.AnsEnde.textAnsEnde(this);
        } else {
            this.setTitle(this.anwendung);
            LOG.debug("Fehler beim oeffnen einer Datei: keine Datei ausgewaehlt");
        }
        this.statusZeigen();
    }

    @Override
    public void starten(String[] param) {
        String dateiName = this.holeParameter()[0];
        if (!dateiName.equals("")) {
            Datei datei = this.holeAnwendung().getSystemSoftware().getDateisystem().holeDatei(this.arbeitsVerzeichnis, dateiName);
            if (datei != null) {
                this.aktuelleDatei = datei;
            }
            this.updateFromFile();
        }
    }

    public void neu() {
        this.editorField.neuerText("");
        this.editorField.setSprache(Syntax.Sprache.TEXT);
        this.original = "";
        this.setTitle(this.anwendung);
        this.changeCurrentFile(null);
    }

    public void updateUnchangedTextFromFile() {
        if (this.original != null && this.editorField != null && this.aktuelleDatei != null && this.original.equals(this.editorField.getText())) {
            this.original = this.aktuelleDatei.getDateiInhalt();
            int pos = this.editorField.getCaretPosition();
            this.editorField.neuerText(this.original);
            this.editorField.setCaretPosition(Math.min(pos, this.editorField.getDocument().getLength()));
        }
    }

    @Override
    public void update(Observable observable, Object arg1) {
        if (observable == this.aktuelleDatei) {
            this.updateUnchangedTextFromFile();
        }
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(400, 200);
    }
}
