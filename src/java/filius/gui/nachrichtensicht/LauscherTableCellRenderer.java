/*
 * Filius (RBZ Wirtschaft Kiel Version): based on LauscherTableCellRenderer of Filius 2.9.3 (GPL). The layer colours of the data
 * exchange table are kept (they carry meaning). New: readable fonts (addresses in a monospaced face),
 * cell padding, a patch-blue selection instead of black, and in the OSI view the layer column shows the
 * OSI layer (number and name).
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel, 2026.
 */
package filius.gui.nachrichtensicht;

import filius.modern.LayerModel;
import filius.modern.Theme;
import filius.rahmenprogramm.nachrichten.Lauscher;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class LauscherTableCellRenderer
extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;
    private static final Font TEXT = Theme.font(Font.PLAIN, 12.5f);
    private static final Font TEXT_BOLD = Theme.font(Font.BOLD, 12.5f);
    private static final Font MONO = Theme.mono(Font.PLAIN, 12f);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Color color1;
        int schicht = 5;
        int tblCols = table.getColumnCount();
        int tblRows = table.getRowCount();
        String schichtString = tblRows > row && tblCols > 5 && table.getValueAt(row, 5) != null ? table.getValueAt(row, 5).toString() : "";
        for (int i = 0; i < Lauscher.PROTOKOLL_SCHICHTEN.length; ++i) {
            if (!schichtString.equals(Lauscher.PROTOKOLL_SCHICHTEN[i])) continue;
            schicht = i;
        }
        Color color2 = switch (schicht) {
            case 0 -> {
                color1 = AggregatedMessageTable.LayerColorVar.LINK.foregroundColor;
                yield AggregatedMessageTable.LayerColorVar.LINK.backgroundColor;
            }
            case 1 -> {
                color1 = AggregatedMessageTable.LayerColorVar.INTERNET.foregroundColor;
                yield AggregatedMessageTable.LayerColorVar.INTERNET.backgroundColor;
            }
            case 2 -> {
                color1 = AggregatedMessageTable.LayerColorVar.TRANSPORT.foregroundColor;
                yield AggregatedMessageTable.LayerColorVar.TRANSPORT.backgroundColor;
            }
            case 3 -> {
                color1 = AggregatedMessageTable.LayerColorVar.APPLICATION.foregroundColor;
                yield AggregatedMessageTable.LayerColorVar.APPLICATION.backgroundColor;
            }
            default -> {
                color1 = AggregatedMessageTable.LayerColorVar.DEFAULT.foregroundColor;
                yield AggregatedMessageTable.LayerColorVar.DEFAULT.backgroundColor;
            }
        };
        if (!isSelected && Theme.dunkel()) {
            // dark mode: the colour of the layer as a tint of the dark surface
            this.setForeground(Theme.INK);
            this.setBackground(Theme.mix(new Color(Theme.SURFACE.getRGB()), color2, 0.16));
        } else if (!isSelected) {
            this.setForeground(Color.BLACK.equals(color1) ? Theme.INK : color1);
            this.setBackground(color2);
        } else {
            this.setForeground(Color.WHITE);
            this.setBackground(Theme.PATCH_BLUE);
        }
        switch (column) {
            case 2:
            case 3:
            case 6: {
                this.setFont(MONO);
                break;
            }
            case 4: {
                this.setFont(TEXT_BOLD);
                break;
            }
            default: {
                this.setFont(TEXT);
            }
        }
        this.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        if (value != null && column == 5) {
            this.setText(LayerModel.tableLabel(value));
        } else if (value != null) {
            this.setText(value.toString().replace('\n', ' '));
        } else {
            this.setText("");
        }
        return this;
    }
}
