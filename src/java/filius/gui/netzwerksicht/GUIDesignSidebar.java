/*
 * Filius (RBZ Wirtschaft Kiel Version): based on GUIDesignSidebar of Filius 2.9.3 (GPL).
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel, 2026: the modem is no longer offered in the
 * component bar. Projects that already contain a modem still open and work as before.
 */
package filius.gui.netzwerksicht;

import filius.gui.GUIContainer;
import filius.gui.GUIEvents;
import filius.hardware.Kabel;
import filius.hardware.knoten.Gateway;
import filius.hardware.knoten.Notebook;
import filius.hardware.knoten.Rechner;
import filius.hardware.knoten.Switch;
import filius.hardware.knoten.Vermittlungsrechner;
import filius.rahmenprogramm.IconMode;
import filius.rahmenprogramm.Information;
import java.awt.event.MouseEvent;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.event.MouseInputAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GUIDesignSidebar
extends GUISidebar {
    private static Logger LOG = LoggerFactory.getLogger(GUIDesignSidebar.class);
    public static final String KABEL = "gfx/hardware/kabel.png";
    public static final String RECHNER = "gfx/hardware/server";
    public static final String SWITCH = "gfx/hardware/switch";
    public static final String SWITCH_CLOUD = "gfx/hardware/cloud.png";
    public static final String VERMITTLUNGSRECHNER = "gfx/hardware/router";
    public static final String NOTEBOOK = "gfx/hardware/laptop";
    public static final String MODEM = "gfx/hardware/modem";
    public static final String GATEWAY = "gfx/hardware/gateway";
    private JLabel newCableCursor;
    private static GUIDesignSidebar sidebar;

    public static GUIDesignSidebar getGUIDesignSidebar() {
        if (sidebar == null) {
            sidebar = new GUIDesignSidebar();
        }
        return sidebar;
    }

    private void addCableItemToSidebar() {
        this.newCableCursor = new JLabel(new ImageIcon(this.getClass().getResource("/gfx/hardware/kabel.png")));
        this.newCableCursor.setText(Kabel.TYPE);
        this.newCableCursor.setVerticalTextPosition(3);
        this.newCableCursor.setHorizontalTextPosition(0);
        this.newCableCursor.setAlignmentX(0.5f);
        this.newCableCursor.setVerticalTextPosition(3);
        this.newCableCursor.setHorizontalTextPosition(0);
        this.newCableCursor.setToolTipText("<Alt>+1");
        this.leistenpanel.add(this.newCableCursor);
        this.newCableCursor.addMouseListener(new MouseInputAdapter(){

            @Override
            public void mousePressed(MouseEvent e) {
                GUIEvents.getGUIEvents().resetAndShowCablePreview(e.getX() - GUIContainer.getGUIContainer().getSidebarScrollpane().getWidth(), e.getY());
            }
        });
    }

    @Override
    protected void addItemsToSidebar() {
        this.addCableItemToSidebar();
        this.addComponentItemsToSidebar();
    }

    private void addComponentItemsToSidebar() {
        IconMode mode = Information.getInformation().getIconMode();
        this.addHardwareComponent(RECHNER + mode.suffix(), Rechner.TYPE);
        this.addHardwareComponent(NOTEBOOK + mode.suffix(), Notebook.TYPE);
        this.addHardwareComponent(SWITCH + mode.suffix(), Switch.TYPE);
        if (Information.getInformation().isGatewayAvailable()) {
            this.addHardwareComponent(GATEWAY + mode.suffix(), Gateway.TYPE);
        }
        this.addHardwareComponent(VERMITTLUNGSRECHNER + mode.suffix(), Vermittlungsrechner.TYPE);
    }

    private void addHardwareComponent(String imageResourcePath, String hardwareType) {
        ImageIcon icon = new ImageIcon(this.getClass().getResource("/" + imageResourcePath));
        // labels as used in the learning path: clients are laptops, servers are PCs (the type keys stay)
        String text = hardwareType;
        if (hardwareType.equals(Rechner.TYPE)) {
            text = filius.rahmenprogramm.I18n.messages.getString("hw_rechner_msg2");
        } else if (hardwareType.equals(Notebook.TYPE)) {
            text = filius.rahmenprogramm.I18n.messages.getString("hw_notebook_msg2");
        }
        JSidebarButton newLabel = new JSidebarButton(text, (Icon)icon, hardwareType);
        this.buttonList.add(newLabel);
        this.leistenpanel.add(newLabel);
    }
}

