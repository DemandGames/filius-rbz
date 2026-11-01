/*
 * Filius (RBZ Wirtschaft Kiel Version) - based on Filius 2.9.3 (GPL), www.lernsoftware-filius.de
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel
 *
 * Window with the source address table (SAT) of a switch.
 *
 * Changed: the table is updated on the event thread of Swing. In the original every port thread of the
 * switch changed the table model directly; when two frames arrived at the same time, one of the threads
 * died with an ArrayIndexOutOfBoundsException and that port of the switch stopped forwarding (e.g. no
 * DHCP answers any more while the table was open). The entries are read on the port thread and only
 * shown on the event thread; a stale picture is replaced by the next update. Otherwise unchanged.
 */
package filius.gui.anwendungssicht;

import filius.gui.JMainFrame;
import filius.hardware.knoten.Switch;
import filius.rahmenprogramm.I18n;
import filius.software.system.SwitchFirmware;
import java.awt.Frame;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;

public class SatViewer
extends JDialog
implements I18n,
PropertyChangeListener {
    private Switch sw;
    private DefaultTableModel dtm;

    public SatViewer(Switch sw) {
        super((Frame)JMainFrame.getJMainFrame(), messages.getString("guievents_msg8") + " " + sw.holeAnzeigeName());
        this.sw = sw;
        this.init();
        this.updateSat();
    }

    private void init() {
        this.setBounds(100, 100, 400, 280);
        ImageIcon icon = new ImageIcon(this.getClass().getResource("/gfx/hardware/switch.png"));
        this.setIconImage(icon.getImage());
        this.dtm = new DefaultTableModel(0, 3);
        JTable tableSATNachrichten = new JTable(this.dtm);
        DefaultTableColumnModel dtcm = (DefaultTableColumnModel)tableSATNachrichten.getColumnModel();
        dtcm.getColumn(0).setHeaderValue(messages.getString("guievents_msg9"));
        dtcm.getColumn(0).setPreferredWidth(150);
        dtcm.getColumn(1).setHeaderValue(messages.getString("guievents_msg10"));
        dtcm.getColumn(1).setPreferredWidth(100);
        dtcm.getColumn(2).setHeaderValue(messages.getString("guievents_msg27"));
        dtcm.getColumn(2).setPreferredWidth(150);
        final JPopupMenu menu = new JPopupMenu();
        JMenuItem resetMenuItem = new JMenuItem(messages.getString("guievents_msg28"));
        resetMenuItem.addMouseListener(new MouseInputAdapter(){

            @Override
            public void mousePressed(MouseEvent e) {
                menu.setVisible(false);
                ((SwitchFirmware)SatViewer.this.sw.getSystemSoftware()).loescheSAT();
            }
        });
        menu.add(resetMenuItem);
        tableSATNachrichten.setComponentPopupMenu(menu);
        JScrollPane spSAT = new JScrollPane(tableSATNachrichten);
        this.getContentPane().add(spSAT);
    }

    public Switch getSwitch() {
        return this.sw;
    }

    private void updateSat() {
        final Vector<Vector<String>> zeilen;
        try {
            zeilen = ((SwitchFirmware)this.sw.getSystemSoftware()).holeSAT();
        } catch (RuntimeException e) {
            // the table changed while it was read: the next entry brings the next update
            return;
        }
        Runnable zeigen = () -> {
            this.dtm.setRowCount(0);
            for (Vector<String> zeile : zeilen) {
                this.dtm.addRow(zeile);
            }
        };
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            zeigen.run();
        } else {
            javax.swing.SwingUtilities.invokeLater(zeigen);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("sat_entry".equals(evt.getPropertyName())) {
            this.updateSat();
        }
    }
}

