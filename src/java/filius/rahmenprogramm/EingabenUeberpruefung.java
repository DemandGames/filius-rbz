/*
 * Filius (RBZ Wirtschaft Kiel Version) - based on Filius 2.9.3 (GPL), www.lernsoftware-filius.de
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel
 *
 * Patterns for checking inputs (addresses, ports, names) and the colours of a valid and an invalid entry.
 *
 * Changed: the two colours follow the light and the dark mode (ThemeFarbe). In the original a valid
 * entry was always written in pure black, which cannot be read on the dark fields of the dark mode.
 * Otherwise unchanged.
 */
package filius.rahmenprogramm;

import filius.rahmenprogramm.I18n;
import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EingabenUeberpruefung
implements I18n {
    private static Logger LOG = LoggerFactory.getLogger(EingabenUeberpruefung.class);
    public static final Pattern musterIpAdresse = Pattern.compile("^(0*([1-9]?[0-9]?|1[0-9]{1,2}|2[0-4]?[0-9]|25[0-5]))\\.((0*(1?[0-9]{1,2}|2[0-4]?[0-9]|25[0-5]))\\.){2}(0*(1?[0-9]{1,2}|2[0-4]?[0-9]|25[0-5]))$");
    public static final Pattern musterIpAdresseAuchLeer = Pattern.compile("^((0*([1-9]?[0-9]?|1[0-9]{1,2}|2[0-4]?[0-9]|25[0-5]))\\.((0*(1?[0-9]{1,2}|2[0-4]?[0-9]|25[0-5]))\\.){2}(0*(1?[0-9]{1,2}|2[0-4]?[0-9]|25[0-5]))){0,1}$");
    public static final Pattern musterSubNetz = Pattern.compile("^(0*([1-9]|1?[0-9]{1,2}|2[0-4]?[0-9]|25[0-5]))\\.((0*([0-9]|1?[0-9]{1,2}|2[0-4]?[0-9]|25[0-5]))\\.){2}(0*([0-9]|1?[0-9]{1,2}|2[0-4]?[0-9]|25[0-5]))$");
    private static final String rawMailAddress = "[a-zA-Z0-9]([\\-_\\.]{0,1}[a-zA-Z0-9])*@[a-zA-Z0-9]([\\-_\\.]{0,1}[a-zA-Z0-9])*\\.{0,1}";
    public static final Pattern musterEmailAdresse = Pattern.compile("^[a-zA-Z0-9 \\-_\\.]* <[a-zA-Z0-9]([\\-_\\.]{0,1}[a-zA-Z0-9])*@[a-zA-Z0-9]([\\-_\\.]{0,1}[a-zA-Z0-9])*\\.{0,1}>|[a-zA-Z0-9]([\\-_\\.]{0,1}[a-zA-Z0-9])*@[a-zA-Z0-9]([\\-_\\.]{0,1}[a-zA-Z0-9])*\\.{0,1}$");
    public static final Pattern musterKlassenName = Pattern.compile("[A-Z]([a-zA-Z]{2,})?");
    public static final Pattern musterPort = Pattern.compile("([1-9]|[1-9][0-9]{1,3}|[1-5][0-9]{1,4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])");
    public static final Pattern musterPortAuchLeer = Pattern.compile("(^$|[1-9]|[1-9][0-9]{1,3}|[1-5][0-9]{1,4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])");
    public static final Pattern musterKeineLeerzeichen = Pattern.compile("[^\\s]*");
    public static final Pattern musterEmailBenutzername = Pattern.compile("([a-zA-Z0-9]|\\.|\\_|\\-)*");
    public static final Pattern musterMindEinZeichen = Pattern.compile("(.){1,}");
    public static final Pattern musterNurZahl = Pattern.compile("\\d");
    public static final Pattern musterNurZahlen = Pattern.compile("\\d*");
    public static final Pattern musterDomain = Pattern.compile("^([a-zA-Z]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9]){0,1}(\\.[a-zA-Z]([a-zA-Z0-9\\\\-]{0,61}[a-zA-Z0-9]){0,1})*){0,1}\\.{0,1}$");
    public static final Pattern musterSubnetBinary = Pattern.compile("^11*0*$");
    public static final Pattern musterMacAddress = Pattern.compile("^[0-9a-fA-f]{2}(:[0-9a-fA-f]{2}){5}$");
    public static final Pattern musterServiceSetIdentifier = Pattern.compile("^[ -~]+$");
    public static final Pattern musterProtocol = Pattern.compile("^[Tt][Cc][Pp]$|^[Uu][Dd][Pp]$|^6$|^17$");
    public static final Color farbeFalsch = filius.modern.Theme.SIGNAL_RED;
    public static final Color farbeRichtig = filius.modern.Theme.INK;

    public static boolean isGueltig(String zuPruefen, Pattern muster) {
        LOG.trace("INVOKED (EingabenUeberpruefung), isGueltig(" + zuPruefen + "," + String.valueOf(muster) + ")");
        Matcher m = muster.matcher(zuPruefen);
        return m.matches();
    }

    public static boolean isValidSubnetmask(String subnet) {
        LOG.trace("INVOKED (EingabenUeberpruefung), isValidSubnetmask(" + subnet + ")");
        String[] token = subnet.split("\\.");
        Object binary = "";
        LOG.debug("(EingabenUeberpruefung), '" + String.valueOf(token) + "', length=" + token.length);
        if (token.length != 4) {
            return false;
        }
        try {
            for (int i = 0; i < token.length; ++i) {
                Object currBin = Integer.toBinaryString(Integer.parseInt(token[i]));
                while (((String)currBin).length() < 8) {
                    currBin = "0" + (String)currBin;
                }
                binary = (String)binary + (String)currBin;
                LOG.debug("(EingabenUeberpruefung), '" + token[i] + "' ~~> binary (" + i + ") = '" + (String)binary + "'");
            }
        }
        catch (Exception e) {
            return false;
        }
        return ((String)binary).length() == 32 && EingabenUeberpruefung.isGueltig((String)binary, musterSubnetBinary);
    }

    public static boolean isValidIpAddress(String addressString) {
        return EingabenUeberpruefung.isGueltig(addressString, musterIpAdresse);
    }
}

