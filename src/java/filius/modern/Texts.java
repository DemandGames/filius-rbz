package filius.modern;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import filius.rahmenprogramm.Information;

/** Texts of the modern layer in the language Filius is running in (German or English). */
public final class Texts {

    private static ResourceBundle bundle;
    private static Locale bundleLocale;

    private Texts() {}

    private static synchronized ResourceBundle bundle() {
        Locale locale;
        try {
            locale = Information.getInformation().getLocaleOrDefault();
        } catch (Throwable t) {
            locale = Locale.GERMANY;
        }
        if (bundle == null || !locale.equals(bundleLocale)) {
            bundleLocale = locale;
            bundle = ResourceBundle.getBundle("filius.modern.texts", locale,
                    ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
        }
        return bundle;
    }

    /** true if Filius runs in English, otherwise it runs in German */
    public static boolean englisch() {
        try {
            return "en".equals(Information.getInformation().getLocaleOrDefault().getLanguage());
        } catch (Throwable t) {
            return false;
        }
    }

    public static String get(String key) {
        try {
            return bundle().getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    public static String format(String key, Object... args) {
        return MessageFormat.format(get(key).replace("'", "''"), args);
    }
}
