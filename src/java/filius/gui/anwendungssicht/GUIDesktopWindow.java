/*
 * Filius (RBZ Wirtschaft Kiel Version): based on GUIDesktopWindow of Filius 2.9.3 (GPL).
 * Changed: a minimized desktop comes back when its computer is clicked in the simulation. The window can be
 * resized at its border and maximized; the desktop and the programs grow with it (at least the original size).
 * Brought to the front, an open command line or text editor shows its last lines (filius.modern.AnsEnde).
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel, 2026.
 */
package filius.gui.anwendungssicht;

import filius.gui.JMainFrame;
import filius.gui.anwendungssicht.GUIDesktopPanel;
import filius.hardware.knoten.Host;
import filius.hardware.knoten.Knoten;
import filius.hardware.knoten.Notebook;
import filius.hardware.knoten.Rechner;
import filius.rahmenprogramm.IconMode;
import filius.rahmenprogramm.Information;
import filius.software.system.Betriebssystem;
import java.util.Observable;
import java.util.Observer;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

public class GUIDesktopWindow
extends JFrame
implements Observer {
    private GUIDesktopPanel desktopPanel;

    public GUIDesktopWindow(Betriebssystem bs) {
        bs.addObserver(this);
        String imageFile = null;
        IconMode mode = Information.getInformation().getIconMode();
        Knoten hardware = bs.getKnoten();
        if (hardware instanceof Rechner) {
            imageFile = "gfx/hardware/server" + mode.suffix();
        } else if (hardware instanceof Notebook) {
            imageFile = "gfx/hardware/laptop" + mode.suffix();
        }
        ImageIcon icon = new ImageIcon(this.getClass().getResource("/" + imageFile));
        this.setIconImage(icon.getImage());
        int titleBarHeight = JMainFrame.getJMainFrame().getHeight() - JMainFrame.getJMainFrame().getContentPane().getHeight();
        int borderWidth = JMainFrame.getJMainFrame().getWidth() - JMainFrame.getJMainFrame().getContentPane().getWidth();
        this.setSize(640 + borderWidth, 460 + titleBarHeight);
        this.setResizable(true);
        this.setMinimumSize(this.getSize());
        this.desktopPanel = new GUIDesktopPanel(bs);
        this.getContentPane().add(this.desktopPanel);
        filius.modern.DesktopGroesse.einrichten(this.desktopPanel);
        // brought to the front (not by a click into it): the command line or text editor shows its last lines
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowActivated(java.awt.event.WindowEvent e) {
                if (filius.modern.AnsEnde.vonAussen(GUIDesktopWindow.this)) {
                    GUIDesktopWindow.this.offeneTexteAnsEnde(GUIDesktopWindow.this.getContentPane());
                }
            }
        });
    }

    private void offeneTexteAnsEnde(java.awt.Container c) {
        for (java.awt.Component k : c.getComponents()) {
            if (k instanceof GUIApplicationWindow && k.isShowing()) {
                String cls = k.getClass().getSimpleName();
                if (cls.contains("Terminal") || cls.contains("TextEditor")) {
                    filius.modern.AnsEnde.textAnsEnde((GUIApplicationWindow) k);
                }
            } else if (k instanceof java.awt.Container) {
                this.offeneTexteAnsEnde((java.awt.Container) k);
            }
        }
    }

    @Override
    public void setVisible(boolean flag) {
        if (flag && (this.getExtendedState() & java.awt.Frame.ICONIFIED) != 0) {
            // minimized: show it again in front of the main window
            this.setExtendedState(this.getExtendedState() & ~java.awt.Frame.ICONIFIED);
        }
        super.setVisible(flag);
        this.updateTitle();
        this.desktopPanel.updateInfo();
        if (flag) {
            this.toFront();
        }
    }

    private void updateTitle() {
        Host host = (Host)this.desktopPanel.getBetriebssystem().getKnoten();
        Object title = host.isUseIPAsName() ? this.desktopPanel.getBetriebssystem().getKnoten().holeAnzeigeName() : this.desktopPanel.getBetriebssystem().getKnoten().holeAnzeigeName() + " - " + this.desktopPanel.getBetriebssystem().primaryIPAdresse();
        this.setTitle((String)title);
    }

    public Betriebssystem getBetriebssystem() {
        return this.desktopPanel.getBetriebssystem();
    }

    @Override
    public void update(Observable o, Object arg) {
        this.desktopPanel.updateInfo();
        this.updateTitle();
    }

    public static enum Mode {
        ROW(0),
        COLUMN(1),
        STACK(2);

        private final int value;

        private Mode(int mode) {
            this.value = mode;
        }

        public static Mode getMode(int value) {
            if (value == Mode.ROW.value) {
                return ROW;
            }
            if (value == Mode.COLUMN.value) {
                return COLUMN;
            }
            return STACK;
        }
    }
}

