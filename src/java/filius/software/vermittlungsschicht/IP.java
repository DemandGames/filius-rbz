/*
 * Filius (RBZ Wirtschaft Kiel Version): based on IP of Filius 2.9.3 (GPL).
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel, 2026: two hooks for VPN tunnels
 * (filius.software.vpn.Vpn). On a computer with a connected VPN client, packets for the network
 * behind the tunnel go into the tunnel instead of onto the cable. On a VPN server, answers to
 * connections that came through the tunnel go back into the tunnel. Without VPN everything works
 * exactly as before.
 */
package filius.software.vermittlungsschicht;

import filius.hardware.NetzwerkInterface;
import filius.hardware.knoten.InternetKnoten;
import filius.rahmenprogramm.I18n;
import filius.software.system.InternetKnotenBetriebssystem;
import filius.software.transportschicht.Segment;
import filius.software.transportschicht.TcpSegment;
import filius.software.transportschicht.UdpSegment;
import filius.software.vermittlungsschicht.IPThread;
import filius.software.vermittlungsschicht.IpPaket;
import filius.software.vermittlungsschicht.Route;
import filius.software.vermittlungsschicht.RouteNotFoundException;
import filius.software.vermittlungsschicht.VermittlungsProtokoll;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IP
extends VermittlungsProtokoll
implements I18n {
    private static final Pattern REGEX_CLIENT_MAC_ADDRESS_IN_DHCP = Pattern.compile("chaddr=(.{17})");
    private static Logger LOG = LoggerFactory.getLogger(IP.class);
    public static final String CURRENT_NETWORK = "0.0.0.0";
    public static final String LOCALHOST = "127.0.0.1";
    private LinkedList<IpPaket> ipPaketListeTCP = new LinkedList();
    private LinkedList<IpPaket> ipPaketListeUDP = new LinkedList();
    private IPThread thread;

    public IP(InternetKnotenBetriebssystem systemsoftware) {
        super(systemsoftware);
        LOG.trace("INVOKED-2 (" + this.hashCode() + ") " + String.valueOf(this.getClass()) + " (IP), constr: IP(" + String.valueOf(systemsoftware) + ")");
    }

    public boolean isIPForwardingEnabled() {
        return ((InternetKnotenBetriebssystem)this.holeSystemSoftware()).isIpForwardingEnabled();
    }

    public static long inetAton(String ipStr) {
        long ipAddr = 0L;
        StringTokenizer ipToken = new StringTokenizer(ipStr, ".");
        try {
            for (int i = 0; i < 4; ++i) {
                int octet;
                try {
                    octet = Integer.parseInt(ipToken.nextToken());
                }
                catch (NumberFormatException e) {
                    return -1L;
                }
                if (0 > octet || octet > 255) {
                    return -1L;
                }
                ipAddr += (long)octet;
                if (i >= 3) continue;
                ipAddr <<= 8;
            }
        }
        catch (NoSuchElementException e) {
            return -1L;
        }
        if (ipToken.hasMoreTokens()) {
            return -1L;
        }
        return ipAddr;
    }

    public static String inetNtoa(long ipAddr) {
        String ipStr = "." + (ipAddr & 0xFFL);
        ipStr = "." + ((ipAddr >>= 8) & 0xFFL) + ipStr;
        ipStr = "." + ((ipAddr >>= 8) & 0xFFL) + ipStr;
        ipStr = ((ipAddr >>= 8) & 0xFFL) + ipStr;
        return ipStr;
    }

    public static String ipCheck(String ip) {
        long ipAddr = IP.inetAton(ip);
        if (ipAddr == -1L) {
            return null;
        }
        return IP.inetNtoa(ipAddr);
    }

    private void sendeBroadcast(IpPaket ipPaket) {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + String.valueOf(this.getClass()) + " (IP), sendeBroadcast(" + ipPaket.toString() + ")");
        InternetKnoten knoten = (InternetKnoten)this.holeSystemSoftware().getKnoten();
        for (NetzwerkInterface nic : knoten.getNetzwerkInterfaces()) {
            this.sendBroadcastOverNic(ipPaket, nic);
        }
    }

    private void sendBroadcastOverNic(IpPaket ipPaket, NetzwerkInterface useNic) {
        Matcher dhcpMACMatcher;
        ipPaket.setTtl(1);
        String macAddressFilter = null;
        if ((ipPaket.getSegment().getDaten().startsWith("DHCPDISCOVER") || ipPaket.getSegment().getDaten().startsWith("DHCPREQUEST")) && (dhcpMACMatcher = REGEX_CLIENT_MAC_ADDRESS_IN_DHCP.matcher(ipPaket.getSegment().getDaten())).find()) {
            macAddressFilter = dhcpMACMatcher.group(1);
        }
        InternetKnotenBetriebssystem bs = (InternetKnotenBetriebssystem)this.holeSystemSoftware();
        if (CURRENT_NETWORK.equals(ipPaket.getSender()) && (null == macAddressFilter || useNic.getMac().equalsIgnoreCase(macAddressFilter)) || IP.gleichesRechnernetz(ipPaket.getSender(), useNic.getIp(), useNic.getSubnetzMaske())) {
            bs.holeEthernet().senden(ipPaket, useNic.getMac(), "FF:FF:FF:FF:FF:FF", "0x800", useNic);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void benachrichtigeTransportschicht(IpPaket paket) {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + String.valueOf(this.getClass()) + " (IP), benachrichtigeTransportschicht(" + paket.toString() + ")");
        // VPN server: an answer for a computer behind the tunnel
        if (filius.software.vpn.Vpn.eingehend((InternetKnotenBetriebssystem)this.holeSystemSoftware(), paket)) {
            return;
        }
        this.zustellen(paket);
    }

    /** hands a packet to TCP or UDP of this computer (also used for packets that came out of a VPN tunnel) */
    public void zustellen(IpPaket paket) {
        if (paket.getSegment() instanceof TcpSegment) {
            LinkedList<IpPaket> linkedList = this.ipPaketListeTCP;
            synchronized (linkedList) {
                this.ipPaketListeTCP.add(paket);
                this.ipPaketListeTCP.notify();
            }
        }
        if (paket.getSegment() instanceof UdpSegment) {
            LinkedList<IpPaket> linkedList = this.ipPaketListeUDP;
            synchronized (linkedList) {
                this.ipPaketListeUDP.add(paket);
                this.ipPaketListeUDP.notify();
            }
        }
    }

    private void sendeUnicast(IpPaket paket, Route route) throws RouteNotFoundException {
        NetzwerkInterface nic = ((InternetKnoten)this.holeSystemSoftware().getKnoten()).getNetzwerkInterfaceByIp(route.getInterfaceIpAddress());
        String netzmaske = nic.getSubnetzMaske();
        if (IP.gleichesRechnernetz(paket.getEmpfaenger(), route.getInterfaceIpAddress(), netzmaske)) {
            this.sendeUnicastLokal(paket, paket.getEmpfaenger(), nic);
        } else {
            this.sendeUnicastLokal(paket, route.getGateway(), nic);
        }
    }

    private void sendeUnicastLokal(IpPaket paket, String ziel, NetzwerkInterface useNic) {
        InternetKnotenBetriebssystem bs = (InternetKnotenBetriebssystem)this.holeSystemSoftware();
        String zielMacAdresse = bs.holeARP().holeARPTabellenEintrag(ziel, 2, true);
        if (zielMacAdresse != null) {
            bs.holeEthernet().senden(paket, useNic.getMac(), zielMacAdresse, "0x800", useNic);
        } else {
            bs.holeICMP().sendeICMP(3, 1, paket.getSender());
        }
    }

    public void senden(String zielIp, String quellIp, int protokoll, int ttl, Segment segment) {
        IpPaket paket = new IpPaket(protokoll, -1L);
        paket.setEmpfaenger(zielIp);
        paket.setTtl(ttl);
        paket.setSegment(segment);
        if (this.isLocalAddress(zielIp)) {
            paket.setSender(LOCALHOST);
            this.benachrichtigeTransportschicht(paket);
        } else if (zielIp.equals("255.255.255.255")) {
            if (quellIp == null) {
                quellIp = ((InternetKnotenBetriebssystem)this.holeSystemSoftware()).primaryIPAdresse();
            }
            paket.setSender(quellIp);
            this.sendeBroadcast(paket);
        } else {
            try {
                InternetKnotenBetriebssystem bs = (InternetKnotenBetriebssystem)this.holeSystemSoftware();
                // VPN client: packets for the network behind the tunnel go into the tunnel
                if (filius.software.vpn.Vpn.ausgehend(bs, paket)) {
                    return;
                }
                Route route = bs.determineRoute(paket.getEmpfaenger());
                paket.setSender(route.getInterfaceIpAddress());
                this.sendeUnicast(paket, route);
            }
            catch (RouteNotFoundException routeNotFoundException) {
                // empty catch block
            }
        }
    }

    public void weiterleitenPaket(IpPaket paket) {
        InternetKnotenBetriebssystem bs = (InternetKnotenBetriebssystem)this.holeSystemSoftware();
        if (paket.getTtl() <= 1) {
            bs.holeICMP().sendeICMP(11, 0, paket.getSender());
        } else {
            try {
                Route route = bs.determineRoute(paket.getEmpfaenger());
                IpPaket packetClone = paket.clone();
                packetClone.decrementTtl();
                this.sendeUnicast(packetClone, route);
            }
            catch (RouteNotFoundException e) {
                bs.holeICMP().sendeICMP(3, 0, paket.getSender());
                bs.benachrichtigeBeobacher(messages.getString("sw_ip_msg4") + " \"" + bs.getKnoten().getName() + "\"!\n" + messages.getString("sw_ip_msg5") + " " + paket.getEmpfaenger() + " " + messages.getString("sw_ip_msg6"));
            }
        }
    }

    public LinkedList<IpPaket> holePaketListe(int protokollTyp) {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + String.valueOf(this.getClass()) + " (IP), holePaketListe(" + protokollTyp + ")");
        if (protokollTyp == 6) {
            return this.ipPaketListeTCP;
        }
        if (protokollTyp == 17) {
            return this.ipPaketListeUDP;
        }
        return null;
    }

    @Override
    public void starten() {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + String.valueOf(this.getClass()) + " (IP), starten()");
        if (this.ipPaketListeTCP.size() > 0) {
            LOG.debug("Clear TCP buffer. Still data left from last run.");
            this.ipPaketListeTCP.clear();
        }
        if (this.ipPaketListeUDP.size() > 0) {
            LOG.debug("Clear UDP buffer. Still data left from last run.");
            this.ipPaketListeUDP.clear();
        }
        this.thread = new IPThread(this);
        this.thread.starten();
    }

    @Override
    public void beenden() {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + String.valueOf(this.getClass()) + " (IP), beenden()");
        this.thread.beenden();
    }

    public IPThread getIPThread() {
        return this.thread;
    }
}

