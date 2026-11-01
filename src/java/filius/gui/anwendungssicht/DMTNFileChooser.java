/*
 * Filius (RBZ Wirtschaft Kiel Version): based on DMTNFileChooser of Filius 2.9.3 (decompiled, GPL).
 * Changed: a single click on a file puts its name into the field below (in the original only a double click
 * did that), and in the dialog "Open" a double click on a file opens it at once, as in Windows. A double
 * click on a folder opens the folder as before.
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel, 2026.
 */
package filius.gui.anwendungssicht;

import filius.gui.JMainFrame;
import filius.gui.anwendungssicht.OrdnerInhaltListRenderer;
import filius.rahmenprogramm.I18n;
import filius.software.system.Betriebssystem;
import filius.software.system.Datei;
import filius.software.system.Dateisystem;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

public class DMTNFileChooser
implements I18n {
    private JPanel pHaupt;
    private JList lVerzeichnisse;
    private DefaultMutableTreeNode aktuellerOrdner;
    private String aktuellerDateiname;
    private JButton btEbeneHoch;
    private JTextField tfDateiname;
    private JLabel lbDateiname;
    private JLabel lbAktuellerOrdner;
    private JButton btAktion;
    private JButton btAbbrechen;
    private Betriebssystem betriebssystem;
    private int rueckgabe = 0;
    private JDialog dialog = new JDialog();
    public static final int OK = 1;
    public static final int CANCEL = 2;

    public DMTNFileChooser(Betriebssystem bs) {
        this.dialog.setIconImage(JMainFrame.getJMainFrame().getIconImage());
        this.betriebssystem = bs;
        this.aktuellerOrdner = this.betriebssystem.getDateisystem().getRoot();
        this.pHaupt = new JPanel(new BorderLayout());
        Box boxHaupt = Box.createVerticalBox();
        Box tmpBox = Box.createHorizontalBox();
        tmpBox.add(Box.createHorizontalStrut(5));
        this.lbAktuellerOrdner = new JLabel(this.aktuellerOrdner.toString());
        this.lbAktuellerOrdner.setHorizontalAlignment(2);
        boxHaupt.add(Box.createVerticalStrut(10));
        tmpBox.add(this.lbAktuellerOrdner);
        tmpBox.add(Box.createHorizontalStrut(20));
        this.btEbeneHoch = new JButton(messages.getString("dmtnfilechooser_msg1"));
        this.btEbeneHoch.setActionCommand("ebeneHoch");
        this.btEbeneHoch.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals(DMTNFileChooser.this.btEbeneHoch.getActionCommand()) && DMTNFileChooser.this.aktuellerOrdner.getParent() != null) {
                    DMTNFileChooser.this.aktuellerOrdner = (DefaultMutableTreeNode)DMTNFileChooser.this.aktuellerOrdner.getParent();
                    DMTNFileChooser.this.ordnerInhaltAnzeigen(DMTNFileChooser.this.aktuellerOrdner);
                }
            }
        });
        tmpBox.add(Box.createHorizontalStrut(5));
        tmpBox.add(this.btEbeneHoch);
        boxHaupt.add(Box.createVerticalStrut(10));
        boxHaupt.add(tmpBox);
        DefaultListModel lmDateiListe = new DefaultListModel();
        this.lVerzeichnisse = new JList(lmDateiListe);
        this.lVerzeichnisse.setFixedCellHeight(16);
        JScrollPane dateiListenScrollPane = new JScrollPane(this.lVerzeichnisse);
        boxHaupt.add(Box.createVerticalStrut(10));
        boxHaupt.add(dateiListenScrollPane);
        this.lVerzeichnisse.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == 1) {
                    String[] teile;
                    int index = ((JList)e.getSource()).locationToIndex(e.getPoint());
                    DefaultListModel lm = (DefaultListModel)DMTNFileChooser.this.lVerzeichnisse.getModel();
                    int selektiert = DMTNFileChooser.this.selektierteZelle(index, e.getPoint());
                    if (selektiert > -1 && (teile = lm.getElementAt(index).toString().split(";")).length > 0) {
                        if (teile[0].equals("Ordner") && e.getClickCount() == 2) {
                            DefaultMutableTreeNode ordnerNode;
                            DMTNFileChooser.this.aktuellerOrdner = ordnerNode = Dateisystem.verzeichnisKnoten(DMTNFileChooser.this.aktuellerOrdner, teile[1]);
                            DMTNFileChooser.this.ordnerInhaltAnzeigen(ordnerNode);
                        }
                        if (teile[0].equals("Datei")) {
                            Datei datei = DMTNFileChooser.this.betriebssystem.getDateisystem().holeDatei(DMTNFileChooser.this.aktuellerOrdner, teile[1]);
                            DMTNFileChooser.this.tfDateiname.setText(datei.getName());
                            if (e.getClickCount() == 2 && "open".equals(DMTNFileChooser.this.btAktion.getActionCommand())) {
                                DMTNFileChooser.this.btAktion.doClick();
                            }
                        }
                    }
                }
            }
        });
        Box boxDateiname = Box.createHorizontalBox();
        this.lbDateiname = new JLabel(messages.getString("dmtnfilechooser_msg2"));
        boxDateiname.add(this.lbDateiname);
        this.tfDateiname = new JTextField();
        boxDateiname.add(Box.createHorizontalStrut(5));
        boxDateiname.add(this.tfDateiname);
        boxHaupt.add(Box.createVerticalStrut(10));
        boxHaupt.add(boxDateiname);
        Box boxButtons = Box.createHorizontalBox();
        this.btAktion = new JButton(messages.getString("dmtnfilechooser_msg3"));
        this.btAbbrechen = new JButton(messages.getString("dmtnfilechooser_msg4"));
        this.btAbbrechen.setActionCommand("cancel");
        boxButtons.add(Box.createHorizontalStrut(5));
        boxButtons.add(this.btAktion);
        boxButtons.add(Box.createHorizontalStrut(5));
        boxButtons.add(this.btAbbrechen);
        boxHaupt.add(Box.createVerticalStrut(10));
        boxHaupt.add(boxButtons);
        boxHaupt.add(Box.createVerticalStrut(10));
        this.btAbbrechen.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals(DMTNFileChooser.this.btAbbrechen.getActionCommand())) {
                    DMTNFileChooser.this.rueckgabe = 2;
                    DMTNFileChooser.this.dialog.setVisible(false);
                    DMTNFileChooser.this.dialog.dispose();
                }
            }
        });
        this.pHaupt.add((Component)boxHaupt, "Center");
        this.dialog.setModal(true);
        this.dialog.setBounds(100, 100, 320, 240);
        this.dialog.add(this.pHaupt);
    }

    public int saveDialog() {
        this.dialog.setTitle(messages.getString("dmtnfilechooser_msg5"));
        this.btAktion.setText(messages.getString("dmtnfilechooser_msg6"));
        this.btAktion.setActionCommand("save");
        this.btAktion.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals(DMTNFileChooser.this.btAktion.getActionCommand())) {
                    DMTNFileChooser.this.rueckgabe = 1;
                    DMTNFileChooser.this.aktuellerDateiname = DMTNFileChooser.this.tfDateiname.getText();
                    DMTNFileChooser.this.dialog.setVisible(false);
                    DMTNFileChooser.this.dialog.dispose();
                }
            }
        });
        this.ordnerInhaltAnzeigen(this.aktuellerOrdner);
        this.dialog.setVisible(true);
        return this.rueckgabe;
    }

    public int openDialog() {
        this.dialog.setTitle(messages.getString("dmtnfilechooser_msg7"));
        this.btAktion.setText(messages.getString("dmtnfilechooser_msg8"));
        this.btAktion.setActionCommand("open");
        this.btAktion.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals(DMTNFileChooser.this.btAktion.getActionCommand())) {
                    DMTNFileChooser.this.rueckgabe = 1;
                    DMTNFileChooser.this.aktuellerDateiname = DMTNFileChooser.this.tfDateiname.getText();
                    DMTNFileChooser.this.dialog.setVisible(false);
                    DMTNFileChooser.this.dialog.dispose();
                }
            }
        });
        this.ordnerInhaltAnzeigen(this.aktuellerOrdner);
        this.dialog.setVisible(true);
        return this.rueckgabe;
    }

    public DefaultMutableTreeNode getAktuellerOrdner() {
        return this.aktuellerOrdner;
    }

    public void setAktuellerOrdner(DefaultMutableTreeNode aktuellerOrdner) {
        this.aktuellerOrdner = aktuellerOrdner;
    }

    public void ordnerInhaltAnzeigen(DefaultMutableTreeNode node) {
        DefaultListModel lm = (DefaultListModel)this.lVerzeichnisse.getModel();
        lm.clear();
        this.lVerzeichnisse.setCellRenderer(new OrdnerInhaltListRenderer());
        Enumeration<TreeNode> e = node.children();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode enode = (DefaultMutableTreeNode)e.nextElement();
            if (enode.getUserObject().getClass().equals(Datei.class)) {
                Datei dat = (Datei)enode.getUserObject();
                lm.addElement("Datei;" + dat.getName());
                continue;
            }
            lm.addElement("Ordner;" + enode.toString());
        }
        this.lbAktuellerOrdner.setText(messages.getString("dmtnfilechooser_msg11") + " " + this.aktuellerOrdner.toString());
    }

    public int selektierteZelle(int index, Point punkt) {
        int ergebnis = -1;
        if (this.lVerzeichnisse.indexToLocation(index) != null && this.lVerzeichnisse.indexToLocation(index).getY() + (double)this.lVerzeichnisse.getFixedCellHeight() > punkt.getY()) {
            ergebnis = index;
        }
        return ergebnis;
    }

    public String getAktuellerDateiname() {
        return this.aktuellerDateiname;
    }

    public void setAktuellerDateiname(String aktuellerDateiname) {
        this.aktuellerDateiname = aktuellerDateiname;
    }
}

