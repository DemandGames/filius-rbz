/*
 * Filius (RBZ Wirtschaft Kiel Version): based on JExtendedTable of Filius 2.9.3 (decompiled, GPL).
 * Changed (version 31): the cells of these tables (static DHCP assignments, DNS records, port forwarding,
 * firewall rules, mail accounts, virtual hosts …) were drawn by Filius with a cyan background for the selected
 * row and white for all others, so a selected entry was hard to read and the rows were white in the dark mode.
 * Now: the surface of the theme, a soft blue tint for the selected row, a little room left and right of the
 * text. Colours that Filius sets for single cells (setColor) stay. Everything else is unchanged.
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel, 2026.
 */
package filius.gui;

import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

public class JExtendedTable extends JTable {
    private static final long serialVersionUID = 1L;
    private boolean editable;
    protected Object parentGUI;

    public JExtendedTable(TableModel model, boolean editable) {
        super(model);
        this.setEditable(editable);
        for (int i = 0; i < this.getColumnCount(); ++i) {
            this.getColumnModel().getColumn(i).setCellRenderer(new ColorTableCellRenderer());
        }
    }

    public void setParentGUI(Object parent) {
        this.parentGUI = parent;
    }

    public boolean isEditable() {
        return this.editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return this.editable;
    }

    public class ColorTableCellRenderer implements TableCellRenderer {
        private HashMap<Integer, Color> cellColor = new HashMap<>();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            JLabel label = new JLabel(value != null ? value.toString() : "");
            int key = (row + 1) * 1000 + column;
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
            if (isSelected) {
                label.setBackground(filius.modern.Theme.AUSWAHL);
                label.setForeground(filius.modern.Theme.INK);
            } else if (this.cellColor.containsKey(key)) {
                label.setBackground(this.cellColor.get(key));
            } else {
                label.setBackground(filius.modern.Theme.SURFACE);
                label.setForeground(filius.modern.Theme.INK);
            }
            return label;
        }

        public void setColor(int row, int column, Color color) {
            int key = (row + 1) * 1000 + column;
            this.cellColor.put(key, color);
        }

        public void resetColor(int row, int column) {
            int key = (row + 1) * 1000 + column;
            this.cellColor.remove(key);
        }

        public void resetColors() {
            this.cellColor.clear();
        }
    }
}
