package filius.modern;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.geom.RoundRectangle2D;
import java.util.Map;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

/**
 * Role tags for the programs of the software installation: does a program offer a service
 * (server), use one (client), do both (peer-to-peer) or neither (tool)? The role is looked up by
 * the program's class, so the tags work in every language Filius runs in. Programs that are not
 * listed here (e.g. own programs from the software wizard) simply get no tag.
 */
public final class AppRoles {

    private AppRoles() {}

    public enum Role {
        SERVER("server", Theme.farbe(0x6B3FC2, 0xB596FF)),
        CLIENT("client", Theme.farbe(0x2A5FC4, 0x7FAEFF)),
        PEER("p2p", Theme.farbe(0x0A6C66, 0x4FD1C5)),
        TOOL("tool", Theme.farbe(0x4A5B6C, 0xADBCCB));

        private final String key;
        private final Color color;

        Role(String key, Color color) {
            this.key = key;
            this.color = color;
        }

        /** "Server", "Client", "Peer-to-Peer", "Werkzeug" */
        public String label() {
            return Texts.get("role." + key);
        }

        /** short definition for the legend */
        public String shortHint() {
            return Texts.get("role." + key + ".short");
        }

        /** full sentence for the tooltip */
        public String hint() {
            return Texts.get("role." + key + ".hint");
        }

        public Color color() {
            return color;
        }

        public String tooltip() {
            return "<html><b>" + label() + "</b>: " + hint() + "</html>";
        }
    }

    private static final Map<String, Role> BY_CLASS = Map.ofEntries(
            Map.entry("filius.software.www.WebServer", Role.SERVER),
            Map.entry("filius.software.dns.DNSServer", Role.SERVER),
            Map.entry("filius.software.email.EmailServer", Role.SERVER),
            Map.entry("filius.software.clientserver.ServerBaustein", Role.SERVER),
            Map.entry("filius.software.vpn.VpnServer", Role.SERVER),
            Map.entry("filius.software.vpn.VpnClient", Role.CLIENT),
            Map.entry("filius.software.www.WebBrowser", Role.CLIENT),
            Map.entry("filius.software.email.EmailAnwendung", Role.CLIENT),
            Map.entry("filius.software.clientserver.ClientBaustein", Role.CLIENT),
            Map.entry("filius.software.dateiaustausch.PeerToPeerAnwendung", Role.PEER),
            Map.entry("filius.software.lokal.Terminal", Role.TOOL),
            Map.entry("filius.software.lokal.ImageViewer", Role.TOOL),
            Map.entry("filius.software.lokal.FileExplorer", Role.TOOL),
            Map.entry("filius.software.lokal.TextEditor", Role.TOOL),
            Map.entry("filius.software.firewall.Firewall", Role.TOOL));

    /** The role of a program class, or null if the program has none (or is unknown). */
    public static Role forClass(String programClass) {
        return programClass == null ? null : BY_CLASS.get(programClass);
    }

    /** Small rounded label in the colour of its role. */
    public static final class Tag extends JComponent {
        private static final long serialVersionUID = 1L;
        private Role role;
        private boolean onSelection;

        public Tag() {
            setOpaque(false);
            setFont(Theme.font(Font.BOLD, 11f));
        }

        public Tag(Role role) {
            this();
            set(role, false);
        }

        public void set(Role role, boolean onSelection) {
            this.role = role;
            this.onSelection = onSelection;
        }

        @Override
        public Dimension getPreferredSize() {
            if (role == null) {
                return new Dimension(0, 0);
            }
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(role.label()) + 14, fm.getAscent() + 7);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (role == null) {
                return;
            }
            Graphics2D g2 = Theme.smooth(g.create());
            try {
                Dimension d = getPreferredSize();
                double y = (getHeight() - d.height) / 2.0;
                double x = getWidth() - d.width;
                g2.setColor(onSelection ? Theme.alpha(Color.WHITE, 0.24) : Theme.alpha(role.color(), Theme.dunkel() ? 0.2 : 0.11));
                g2.fill(new RoundRectangle2D.Double(x, y, d.width, d.height, d.height, d.height));
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(onSelection ? Color.WHITE : role.color());
                g2.drawString(role.label(), (float) (x + 7), (float) (y + (d.height + fm.getAscent() - fm.getDescent()) / 2.0));
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * Renders a program name with its role tag at the right edge. The tooltip explains the role.
     * {@code classOf} maps the displayed program name to the program class.
     */
    public static final class ListRenderer extends JPanel implements ListCellRenderer<String> {
        private static final long serialVersionUID = 1L;
        private final Function<String, String> classOf;
        private final DefaultListCellRenderer name = new DefaultListCellRenderer();
        private final Tag tag = new Tag();

        public ListRenderer(Function<String, String> classOf) {
            super(new BorderLayout(8, 0));
            this.classOf = classOf;
            add(name, BorderLayout.CENTER);
            add(tag, BorderLayout.EAST);
            setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 6));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
                boolean isSelected, boolean cellHasFocus) {
            name.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            name.setOpaque(false);
            setOpaque(true);
            setBackground(name.getBackground());
            setForeground(name.getForeground());
            setComponentOrientation(list.getComponentOrientation());
            setEnabled(list.isEnabled());
            Role role = forClass(value == null ? null : classOf.apply(value));
            tag.set(role, isSelected);
            setToolTipText(role == null ? null : role.tooltip());
            return this;
        }
    }

    /** The four roles with a short definition each, in two columns. */
    public static JComponent legend() {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        Role[] roles = Role.values();
        for (int i = 0; i < roles.length; i++) {
            Role r = roles[i];
            JPanel cell = new JPanel();
            cell.setOpaque(false);
            cell.setLayout(new BoxLayout(cell, BoxLayout.X_AXIS));
            Tag t = new Tag(r);
            t.setAlignmentY(0.5f);
            JLabel text = new JLabel(r.shortHint());
            text.setFont(Theme.font(Font.PLAIN, 12f));
            text.setForeground(Theme.INK_2);
            text.setAlignmentY(0.5f);
            cell.add(t);
            cell.add(Box.createHorizontalStrut(6));
            cell.add(text);
            cell.setToolTipText(r.tooltip());
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = i % 2;
            c.gridy = i / 2;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(i < 2 ? 0 : 3, i % 2 == 0 ? 0 : 16, 0, 0);
            grid.add(cell, c);
        }
        return grid;
    }
}
