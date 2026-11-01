/*
 * Filius (RBZ Wirtschaft Kiel Version): new in Filius (RBZ Wirtschaft Kiel Version), not part of Filius 2.9.3.
 * Written by Thorben Demand @ RBZ Wirtschaft Kiel, 2026. Licensed like Filius under the GPL.
 */
package filius.hardware.knoten;

import filius.hardware.Port;

/** Finds out whether a port belongs to a device (Knoten.hasPort is only visible inside this package). */
public final class Anschlusssuche {

    private Anschlusssuche() {}

    public static boolean gehoertZu(Port port, Knoten knoten) {
        try {
            return port != null && knoten != null && knoten.hasPort(port);
        } catch (RuntimeException e) {
            return false;
        }
    }
}
