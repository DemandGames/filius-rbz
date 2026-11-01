/*
 * Filius (RBZ Wirtschaft Kiel Version) - shadow of filius.gui.HelpPanel (Filius 2.9.3, GPL).
 *
 * Changed: in the dark mode the colours of the help pages (written for a light page) are replaced by dark-mode
 * colours, the pictures show their dark variants (name_dunkel.png), and an open help page is reloaded when the
 * mode is switched.
 */
package filius.gui;

import java.awt.Dimension;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import filius.rahmenprogramm.I18n;
import filius.rahmenprogramm.ResourceUtil;

public class HelpPanel extends ControlPanel implements I18n {
    private static Logger LOG = LoggerFactory.getLogger(HelpPanel.class);

    /** light colour of the help pages -> colour in the dark mode */
    private static final Map<String, String> DUNKEL = new LinkedHashMap<>();
    static {
        // text and lines
        DUNKEL.put("#1C2B3A", "#E6EDF4");
        DUNKEL.put("#4A5B6C", "#ADBCCB");
        DUNKEL.put("#8796A5", "#8D9EB0");
        DUNKEL.put("#DDE4EB", "#2D3B4A");
        DUNKEL.put("#EAEFF4", "#2A3A4C");
        DUNKEL.put("#F2F5F8", "#1C2835");
        // tinted boxes of the three modes
        DUNKEL.put("#EAF1FC", "#1A2A40");
        DUNKEL.put("#E6F5EC", "#16302A");
        DUNKEL.put("#FCF2DF", "#33291A");
        // role tags of the software installation
        DUNKEL.put("#6B3FC2", "#C2ACFF");
        DUNKEL.put("#EFEAF8", "#30264A");
        DUNKEL.put("#2A5FC4", "#9CC0FF");
        DUNKEL.put("#E8EDF9", "#1F2E4C");
        DUNKEL.put("#0A6C66", "#6ADBD1");
        DUNKEL.put("#E3EEED", "#173836");
        DUNKEL.put("#EBEDEF", "#2A3644");
    }

    private JEditorPane epHtml;
    private int letzterModus = -1;

    public HelpPanel() {
        super(0, 350);
        this.reInit();
        this.minimieren();
        filius.modern.DunkelModus.addListener(() -> {
            if (this.letzterModus >= 0) {
                Point p = this.scrollPane.getViewport().getViewPosition();
                this.loadContext(this.letzterModus);
                SwingUtilities.invokeLater(() -> this.scrollPane.getViewport().setViewPosition(p));
            }
        });
    }

    @Override
    protected void initContents() {
        this.epHtml = new JEditorPane("text/html;charset=UTF-8", null);
        this.epHtml.setText(messages.getString("guihilfe_msg2"));
        this.epHtml.setEditable(false);
        this.epHtml.setMinimumSize(new Dimension(1, 1));
        this.scrollPane.getViewport().removeAll();
        this.scrollPane.getViewport().add(this.epHtml);
    }

    static String farben(String html) {
        if (!filius.modern.Theme.dunkel()) {
            return html;
        }
        String s = html;
        for (Map.Entry<String, String> e : DUNKEL.entrySet()) {
            s = s.replace(e.getKey(), e.getValue()).replace(e.getKey().toLowerCase(), e.getValue());
        }
        return s;
    }

    /** dark mode: pictures that have a dark variant (name_dunkel.png, made with the dark program) show it */
    static String bilderDunkel(String html) {
        if (!filius.modern.Theme.dunkel()) {
            return html;
        }
        Matcher m = java.util.regex.Pattern.compile("hilfe/gfx/([^\"']+?)\\.(png|gif)").matcher(html);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String dunkel = m.group(1) + "_dunkel." + m.group(2);
            File f = ResourceUtil.getResourceFile("hilfe/gfx/" + dunkel);
            m.appendReplacement(out, Matcher.quoteReplacement(f != null && f.isFile() ? "hilfe/gfx/" + dunkel : m.group(0)));
        }
        m.appendTail(out);
        return out.toString();
    }

    public void loadContext(int mode) {
        this.letzterModus = mode;
        File file;
        switch (mode) {
        case 2:
            file = ResourceUtil.getResourceFile("hilfe/" + messages.getString("hilfedatei_simulation"));
            break;
        case 1:
            file = ResourceUtil.getResourceFile("hilfe/" + messages.getString("hilfedatei_entwurf"));
            break;
        default:
            file = ResourceUtil.getResourceFile("hilfe/" + messages.getString("hilfedatei_documentation"));
        }
        LOG.debug("Help file: " + file.getAbsolutePath());
        String gfxBaseUrl = "file:" + ResourceUtil.getResourceUrlEncodedPath("hilfe/gfx/");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader((InputStream) new FileInputStream(file), Charset.forName("UTF-8")))) {
            StringBuffer sb = new StringBuffer();
            String line = reader.readLine();
            while (line != null) {
                sb.append(line);
                line = reader.readLine();
            }
            String newText = bilderDunkel(farben(sb.toString()));
            String escapedGfxBaseUrl = Matcher.quoteReplacement(gfxBaseUrl);
            newText = newText.replaceAll("hilfe/gfx/", escapedGfxBaseUrl);
            this.epHtml.read(new StringReader(newText), null);
            this.epHtml.setCaretPosition(0);
        } catch (FileNotFoundException e) {
            LOG.warn("help contents could not be found", e);
        } catch (IOException e) {
            LOG.warn("error while reading help contents", e);
        }
    }
}
