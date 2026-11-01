/*
 * Recorder of the data exchange of Filius 2.9.3 (decompiled), changed for Filius (RBZ Wirtschaft Kiel
 * Version): IMAP (port 143) is shown as its own protocol, because mail servers offer IMAP instead of POP3.
 * HTTPS connections (port 443) are shown as TLS: handshake and encrypted application data.
 * VPN tunnels (UDP port 1194) are shown as VPN.
 */
package filius.rahmenprogramm.nachrichten;

import filius.hardware.NetzwerkInterface;
import filius.hardware.knoten.InternetKnoten;
import filius.rahmenprogramm.I18n;
import filius.rahmenprogramm.Information;
import filius.rahmenprogramm.nachrichten.LauscherBeobachter;
import filius.software.ProtocolDataUnit;
import filius.software.netzzugangsschicht.EthernetFrame;
import filius.software.system.SystemSoftware;
import filius.software.transportschicht.TcpSegment;
import filius.software.transportschicht.UdpSegment;
import filius.software.vermittlungsschicht.ArpPaket;
import filius.software.vermittlungsschicht.IcmpPaket;
import filius.software.vermittlungsschicht.IpPaket;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Lauscher
implements I18n {
    private static Logger LOG = LoggerFactory.getLogger(Lauscher.class);
    public static final String ETHERNET = "";
    public static final String ARP = "ARP";
    public static final String IP = "IP";
    public static final String ICMP = "ICMP";
    public static final String TCP = "TCP";
    public static final String UDP = "UDP";
    public static final String HTTP = "HTTP";
    public static final String SMTP = "SMTP";
    public static final String POP = "POP3";
    public static final String IMAP = "IMAP";
    public static final String DNS = "DNS";
    public static final String DHCP = "DHCP";
    public static final String TLS = "TLS";
    public static final String VPN = "VPN";
    public static final String[] SPALTEN = new String[]{messages.getString("rp_lauscher_msg1"), messages.getString("rp_lauscher_msg2"), messages.getString("rp_lauscher_msg3"), messages.getString("rp_lauscher_msg4"), messages.getString("rp_lauscher_msg5"), messages.getString("rp_lauscher_msg6"), messages.getString("rp_lauscher_msg7")};
    public static final String[] PROTOKOLL_SCHICHTEN = new String[]{messages.getString("rp_lauscher_msg8"), messages.getString("rp_lauscher_msg9"), messages.getString("rp_lauscher_msg10"), messages.getString("rp_lauscher_msg11")};
    public static final String DROPPED = "dropped packets";
    private NumberFormat numberFormatter = NumberFormat.getInstance(Information.getInformation().getLocaleOrDefault());
    private static Lauscher lauscher = null;
    private HashMap<String, LinkedList<LauscherBeobachter>> beobachter = new HashMap();
    private HashMap<String, LinkedList<Object[]>> datenEinheiten = new HashMap();
    private List<Object[]> droppedDataUnits = new LinkedList<Object[]>();
    private HashMap<String, NetzwerkInterface> InterfaceById = new HashMap();
    private HashMap<String, SystemSoftware> SystemSoftwareById = new HashMap();

    private Lauscher() {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + String.valueOf(this.getClass()) + ", constr: Lauscher()");
        this.reset();
    }

    public void reset() {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + String.valueOf(this.getClass()) + ", reset()");
        this.datenEinheiten.clear();
        this.droppedDataUnits.clear();
        this.benachrichtigeBeobachter(null);
    }

    public Collection<String> getInterfaceIDs() {
        return this.datenEinheiten.keySet();
    }

    public static Lauscher getLauscher() {
        if (lauscher == null) {
            lauscher = new Lauscher();
        }
        return lauscher;
    }

    public NetzwerkInterface getInterfaceByID(String interfaceId) {
        return this.InterfaceById.get(interfaceId);
    }

    public SystemSoftware getSystemSoftwareByID(String interfaceId) {
        return this.SystemSoftwareById.get(interfaceId);
    }

    public void removeIdentifier(String identifier) {
        this.datenEinheiten.remove(identifier);
        this.beobachter.remove(identifier);
    }

    public void addBeobachter(String id, LauscherBeobachter newObserver) {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + String.valueOf(this.getClass()) + ", addBeobachter(" + id + "," + String.valueOf(newObserver) + ")");
        LinkedList<LauscherBeobachter> liste = this.beobachter.get(id);
        if (liste == null) {
            liste = new LinkedList();
            this.beobachter.put(id, liste);
        }
        liste.add(newObserver);
    }

    private void benachrichtigeBeobachter(String id) {
        LinkedList<LauscherBeobachter> liste = new LinkedList<LauscherBeobachter>();
        if (id == null) {
            for (LinkedList<LauscherBeobachter> beobachterListe : this.beobachter.values()) {
                liste.addAll(beobachterListe);
            }
        } else if (null == this.beobachter.get(id)) {
            LOG.trace("no observer for {}", (Object)id);
        } else {
            liste.addAll((Collection)this.beobachter.get(id));
        }
        for (LauscherBeobachter beobachter : liste) {
            beobachter.update();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void addDatenEinheit(NetzwerkInterface networkInterface, SystemSoftware systemSoftware, EthernetFrame frame) {
        String interfaceId = networkInterface.getMac();
        LOG.trace("INVOKED (" + this.hashCode() + ") " + String.valueOf(this.getClass()) + ", addDatenEinheit(" + interfaceId + "," + String.valueOf(frame) + ")");
        if (!frame.isReadByLauscherForMac(interfaceId)) {
            Object[] frameMitZeitstempel = this.frameWithTimestamp(frame);
            LinkedList<Object[]> liste = this.datenEinheiten.get(interfaceId);
            if (liste == null) {
                liste = new LinkedList<Object[]>();
                this.datenEinheiten.put(interfaceId, liste);
            }
            synchronized (liste) {
                liste.addLast(frameMitZeitstempel);
            }
            frame.setReadByLauscherForMac(interfaceId);
            this.benachrichtigeBeobachter(interfaceId);
        }
        this.storeInterfaceById(interfaceId, networkInterface);
        this.storeSystemSoftwareById(interfaceId, systemSoftware);
    }

    public void addDroppedDataUnit(EthernetFrame frame) {
        Object[] frameMitZeitstempel = this.frameWithTimestamp(frame);
        this.droppedDataUnits.add(frameMitZeitstempel);
        this.benachrichtigeBeobachter(DROPPED);
    }

    protected Object[] frameWithTimestamp(EthernetFrame frame) {
        Object[] frameMitZeitstempel = new Object[]{System.currentTimeMillis(), frame.clone()};
        return frameMitZeitstempel;
    }

    public Object[][] getDaten(String interfaceId, boolean inheritAddress, int offset) {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + String.valueOf(this.getClass()) + ", getDaten(" + interfaceId + ")");
        Vector<Object[]> vector = this.datenVorbereiten(interfaceId, inheritAddress, offset);
        if (vector == null) {
            Object[][] daten = new Object[0][SPALTEN.length];
            return daten;
        }
        Object[][] daten = new Object[vector.size()][SPALTEN.length];
        for (int i = 0; i < vector.size(); ++i) {
            daten[i] = vector.elementAt(i);
        }
        return daten;
    }

    public void print(String interfaceId) {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + String.valueOf(this.getClass()) + ", print(" + interfaceId + ")");
        Object[][] daten = this.getDaten(interfaceId, false, 1);
        for (int i = 0; i < daten.length; ++i) {
            for (int j = 0; j < daten[i].length; ++j) {
                LOG.debug("\t" + String.valueOf(daten[i][j]));
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public int getOffsetByTimestamp(String interfaceId, long offsetTimestamp) {
        LinkedList<Object[]> liste = this.datenEinheiten.get(interfaceId);
        int offset = 1;
        if (liste != null) {
            LinkedList<Object[]> linkedList = liste;
            synchronized (linkedList) {
                Object[] frameMitZeitstempel;
                long timestamp;
                Iterator iterator = liste.iterator();
                while (iterator.hasNext() && (timestamp = ((Long)(frameMitZeitstempel = (Object[])iterator.next())[0]).longValue()) < offsetTimestamp) {
                    ++offset;
                }
            }
        }
        return offset;
    }

    private String formatTimestamp(long timestamp) {
        GregorianCalendar zeit = new GregorianCalendar();
        zeit.setTimeInMillis(timestamp);
        String timestampStr = String.valueOf(zeit.get(11) < 10 ? "0" + zeit.get(11) : Integer.valueOf(zeit.get(11))) + ":" + String.valueOf(zeit.get(12) < 10 ? "0" + zeit.get(12) : Integer.valueOf(zeit.get(12))) + ":" + String.valueOf(zeit.get(13) < 10 ? "0" + zeit.get(13) : Integer.valueOf(zeit.get(13))) + "." + String.valueOf(zeit.get(14) < 10 ? "00" + zeit.get(14) : (zeit.get(14) < 100 ? "0" + zeit.get(14) : Integer.valueOf(zeit.get(14))));
        return timestampStr;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private Vector<Object[]> datenVorbereiten(String interfaceId, boolean inheritAddress, int offset) {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + String.valueOf(this.getClass()) + ", datenVorbereiten(" + interfaceId + ")");
        TcpSegment tcpSeg = null;
        UdpSegment udpSeg = null;
        LinkedList<Object[]> liste = this.datenEinheiten.get(interfaceId);
        if (liste == null) {
            return null;
        }
        Vector<Object[]> daten = new Vector<Object[]>();
        LinkedList<Object[]> linkedList = liste;
        synchronized (linkedList) {
            ListIterator it = liste.listIterator();
            int i = 1;
            while (it.hasNext()) {
                Object[] frameMitZeitstempel = (Object[])it.next();
                if (i >= offset) {
                    Object[] neuerEintrag = new Object[SPALTEN.length];
                    neuerEintrag[0] = ETHERNET + i;
                    String timestampStr = this.formatTimestamp((Long)frameMitZeitstempel[0]);
                    neuerEintrag[1] = timestampStr;
                    EthernetFrame frame = (EthernetFrame)frameMitZeitstempel[1];
                    neuerEintrag[2] = frame.getQuellMacAdresse();
                    neuerEintrag[3] = frame.getZielMacAdresse();
                    neuerEintrag[4] = ETHERNET;
                    neuerEintrag[5] = PROTOKOLL_SCHICHTEN[0];
                    neuerEintrag[6] = frame.getTyp();
                    daten.addElement(neuerEintrag);
                    neuerEintrag = new Object[SPALTEN.length];
                    neuerEintrag[0] = ETHERNET + i;
                    neuerEintrag[1] = timestampStr;
                    if (frame.getTyp().equals("0x800") && !(frame.getDaten() instanceof IcmpPaket)) {
                        IpPaket ipPaket = (IpPaket)frame.getDaten();
                        neuerEintrag[2] = ipPaket.getSender();
                        neuerEintrag[3] = ipPaket.getEmpfaenger();
                        neuerEintrag[4] = IP;
                        neuerEintrag[5] = PROTOKOLL_SCHICHTEN[1];
                        neuerEintrag[6] = messages.getString("rp_lauscher_msg12") + ": " + ipPaket.getProtocol() + ", TTL: " + ipPaket.getTtl();
                        daten.addElement(neuerEintrag);
                        neuerEintrag = new Object[SPALTEN.length];
                        neuerEintrag[0] = ETHERNET + i;
                        neuerEintrag[1] = timestampStr;
                        String source = null;
                        String dest = null;
                        if (ipPaket.getProtocol() == 6) {
                            tcpSeg = (TcpSegment)ipPaket.getSegment();
                            if (inheritAddress) {
                                source = ipPaket.getSender() + ":" + tcpSeg.getQuellPort();
                                neuerEintrag[2] = source;
                                dest = ipPaket.getEmpfaenger() + ":" + tcpSeg.getZielPort();
                                neuerEintrag[3] = dest;
                            } else {
                                neuerEintrag[2] = tcpSeg.getQuellPort();
                                neuerEintrag[3] = tcpSeg.getZielPort();
                            }
                            neuerEintrag[4] = TCP;
                            neuerEintrag[5] = PROTOKOLL_SCHICHTEN[2];
                            if (tcpSeg.isSyn()) {
                                neuerEintrag[6] = "SYN";
                            } else if (tcpSeg.isFin()) {
                                neuerEintrag[6] = "FIN";
                            }
                            neuerEintrag[6] = (String)(neuerEintrag[6] == null ? ETHERNET : String.valueOf(neuerEintrag[6]) + ", ") + "SEQ: " + this.numberFormatter.format(tcpSeg.getSeqNummer());
                            if (tcpSeg.isAck()) {
                                neuerEintrag[6] = String.valueOf(neuerEintrag[6]) + ", ACK: " + this.numberFormatter.format(tcpSeg.getAckNummer());
                            }
                        } else if (ipPaket.getProtocol() == 17) {
                            udpSeg = (UdpSegment)ipPaket.getSegment();
                            if (inheritAddress) {
                                source = ipPaket.getSender() + ":" + udpSeg.getQuellPort();
                                neuerEintrag[2] = source;
                                dest = ipPaket.getEmpfaenger() + ":" + udpSeg.getZielPort();
                                neuerEintrag[3] = dest;
                            } else {
                                neuerEintrag[2] = udpSeg.getQuellPort();
                                neuerEintrag[3] = udpSeg.getZielPort();
                            }
                            neuerEintrag[4] = UDP;
                            neuerEintrag[5] = PROTOKOLL_SCHICHTEN[2];
                            neuerEintrag[6] = ETHERNET;
                        } else {
                            LOG.warn("Protokoll der Transportschicht (" + ipPaket.getProtocol() + ") nicht bekannt.");
                        }
                        daten.addElement(neuerEintrag);
                        neuerEintrag = new Object[SPALTEN.length];
                        neuerEintrag[0] = ETHERNET + i;
                        neuerEintrag[1] = timestampStr;
                        neuerEintrag[2] = source;
                        neuerEintrag[3] = dest;
                        neuerEintrag[5] = PROTOKOLL_SCHICHTEN[3];
                        if (ipPaket.getProtocol() == 6) {
                            neuerEintrag[6] = tcpSeg.getDaten();
                        } else if (ipPaket.getProtocol() == 17) {
                            neuerEintrag[6] = udpSeg.getDaten();
                        }
                        String payload = (String)neuerEintrag[6];
                        if (payload != null && !payload.trim().equals(ETHERNET)) {
                            neuerEintrag[4] = this.classifyApplicationLayerProtocol(payload, ipPaket.getProtocol(), ipPaket.getSegment().getQuellPort(), ipPaket.getSegment().getZielPort());
                            daten.addElement(neuerEintrag);
                        }
                    } else if (frame.getTyp().equals("0x806")) {
                        ArpPaket arpPaket = (ArpPaket)frame.getDaten();
                        neuerEintrag[2] = arpPaket.getSenderIP();
                        neuerEintrag[3] = arpPaket.getTargetIP();
                        neuerEintrag[4] = ARP;
                        neuerEintrag[5] = PROTOKOLL_SCHICHTEN[1];
                        neuerEintrag[6] = arpPaket.getOperation() == ArpPaket.REQUEST ? messages.getString("rp_lauscher_msg13") + " " + arpPaket.getTargetIP() : messages.getString("rp_lauscher_msg14") + " " + arpPaket.getSenderMAC();
                        neuerEintrag[6] = String.valueOf(neuerEintrag[6]) + " " + arpPaket.toString();
                        daten.addElement(neuerEintrag);
                    } else if (frame.getTyp().equals("0x800") && frame.getDaten() instanceof IcmpPaket) {
                        IcmpPaket icmpPaket = (IcmpPaket)frame.getDaten();
                        neuerEintrag[2] = icmpPaket.getSender();
                        neuerEintrag[3] = icmpPaket.getEmpfaenger();
                        neuerEintrag[4] = ICMP;
                        neuerEintrag[5] = PROTOKOLL_SCHICHTEN[1];
                        block2 : switch (icmpPaket.getIcmpType()) {
                            case 0: {
                                neuerEintrag[6] = "ICMP Echo Reply (pong)";
                                break;
                            }
                            case 3: {
                                switch (icmpPaket.getIcmpCode()) {
                                    case 0: {
                                        neuerEintrag[6] = "ICMP Network Unreachable";
                                        break block2;
                                    }
                                    case 1: {
                                        neuerEintrag[6] = "ICMP Host Unreachable";
                                        break block2;
                                    }
                                }
                                neuerEintrag[6] = "ICMP Destination Unreachable (code " + icmpPaket.getIcmpCode() + ")";
                                break;
                            }
                            case 8: {
                                neuerEintrag[6] = "ICMP Echo Request (ping)";
                                break;
                            }
                            case 11: {
                                neuerEintrag[6] = "ICMP Time Exeeded (poof)";
                                break;
                            }
                            default: {
                                neuerEintrag[6] = "ICMP unknown: " + icmpPaket.getIcmpType() + " / " + icmpPaket.getIcmpCode();
                            }
                        }
                        neuerEintrag[6] = String.valueOf(neuerEintrag[6]) + ", TTL: " + icmpPaket.getTtl() + ", Seq.-No.: " + icmpPaket.getSeqNr();
                        daten.addElement(neuerEintrag);
                    }
                }
                ++i;
            }
        }
        return daten;
    }

    private String classifyApplicationLayerProtocol(String payload, int transport, int sourcePort, int destPort) {
        String protocol = ETHERNET;
        if (transport == 6 && (sourcePort == 443 || destPort == 443)) {
            return TLS;
        }
        if (transport == 17 && (sourcePort == 1194 || destPort == 1194) && filius.software.vpn.Vpn.istRecord(payload)) {
            return VPN;
        }
        if (StringUtils.contains((CharSequence)payload, "//0x00//") || StringUtils.contains((CharSequence)payload, "//0x01//") || StringUtils.contains((CharSequence)payload, "//0x80//") || StringUtils.contains((CharSequence)payload, "//0x81//") || StringUtils.contains((CharSequence)payload, "//0x40//")) {
            protocol = "GNT";
        } else if (sourcePort == 110 && (StringUtils.startsWithIgnoreCase(payload, "+OK") || StringUtils.startsWithIgnoreCase(payload, "-ERR")) || destPort == 110 && (StringUtils.startsWithIgnoreCase(payload, "USER ") || StringUtils.startsWithIgnoreCase(payload, "PASS ") || StringUtils.startsWithIgnoreCase(payload, "STAT ") || StringUtils.startsWithIgnoreCase(payload, "LIST ") || StringUtils.startsWithIgnoreCase(payload, "RETR ") || StringUtils.startsWithIgnoreCase(payload, "DELE ") || StringUtils.startsWithIgnoreCase(payload, "NOOP") || StringUtils.startsWithIgnoreCase(payload, "RSET") || StringUtils.startsWithIgnoreCase(payload, "QUIT") || StringUtils.startsWithIgnoreCase(payload, "TOP"))) {
            protocol = POP;
        } else if (sourcePort == 143 && (StringUtils.startsWith((CharSequence)payload, "* ") || payload.matches("(?s)^[A-Za-z0-9]+ (OK|NO|BAD)\\b.*")) || destPort == 143 && payload.matches("(?s)^[A-Za-z0-9]+ [A-Za-z]+.*")) {
            protocol = IMAP;
        } else if (sourcePort == 25 && (StringUtils.startsWithIgnoreCase(payload, "1") || StringUtils.startsWithIgnoreCase(payload, "2") || StringUtils.startsWithIgnoreCase(payload, "3") || StringUtils.startsWithIgnoreCase(payload, "4") || StringUtils.startsWithIgnoreCase(payload, "5")) || destPort == 25) {
            protocol = SMTP;
        } else if (StringUtils.contains((CharSequence)payload, "ID=") && StringUtils.contains((CharSequence)payload, "QR=") && StringUtils.contains((CharSequence)payload, "RCODE=")) {
            protocol = DNS;
        } else if (StringUtils.contains((CharSequence)payload, DHCP)) {
            protocol = DHCP;
        } else if (sourcePort == 521 && destPort == 520 || sourcePort == 520 && destPort == 521) {
            protocol = "RIP";
        } else if (destPort == 80 && StringUtils.containsIgnoreCase(payload, "http/") || sourcePort == 80) {
            protocol = HTTP;
        }
        return protocol;
    }

    public String[] getHeader() {
        return SPALTEN;
    }

    public List<String> getDroppedDataUnits() {
        ArrayList<String> list = new ArrayList<String>();
        for (Object[] droppedDataUnit : this.droppedDataUnits) {
            list.add(String.format("%s : %s", this.formatTimestamp((Long)droppedDataUnit[0]), droppedDataUnit[1]));
        }
        return list;
    }

    public void resetDroppedDataUnits() {
        this.droppedDataUnits.clear();
    }

    public Object[] getCorrespondingFrame(String originalInterfaceId, int originalFrameNumber, InternetKnoten node) {
        Object[] interfaceIdAndFrameNumber = new Object[2];
        EthernetFrame originalFrame = this.getFrame(originalInterfaceId, originalFrameNumber);
        ProtocolDataUnit data = originalFrame.getDaten();
        if (data instanceof IpPaket && !this.packetOnlyReceivedOrOnlySent(originalFrame, node)) {
            List<NetzwerkInterface> netzwerkInterfaces = node.getNetzwerkInterfaces();
            boolean found = false;
            for (NetzwerkInterface netzwerkInterface : netzwerkInterfaces) {
                String interfaceId = netzwerkInterface.getMac();
                LinkedList<Object[]> framesMitZeitstempel = this.datenEinheiten.get(interfaceId);
                if (framesMitZeitstempel == null) continue;
                boolean sent = originalFrame.getQuellMacAdresse().equals(originalInterfaceId);
                boolean received = originalFrame.getZielMacAdresse().equals(originalInterfaceId);
                for (int frameNumber = 0; frameNumber < framesMitZeitstempel.size(); ++frameNumber) {
                    int originalTTL;
                    EthernetFrame frame = (EthernetFrame)framesMitZeitstempel.get(frameNumber)[1];
                    if ((!frame.samePayload(originalFrame, (originalTTL = ((IpPaket)data).getTtl()) + 1) || !sent) && (!frame.samePayload(originalFrame, originalTTL - 1) || !received)) continue;
                    found = true;
                    interfaceIdAndFrameNumber[0] = interfaceId;
                    interfaceIdAndFrameNumber[1] = Integer.valueOf(frameNumber) + 1;
                    break;
                }
                if (!found) continue;
                break;
            }
        }
        return interfaceIdAndFrameNumber;
    }

    public EthernetFrame getFrameWithSameContentAndSpecificTTL(EthernetFrame originalFrame, int ttl) {
        if (originalFrame.getDaten() instanceof IpPaket) {
            for (String interfaceId : this.getInterfaceIDs()) {
                LinkedList<Object[]> framesMitZeitstempel = this.datenEinheiten.get(interfaceId);
                if (framesMitZeitstempel == null) continue;
                for (Object[] frameMitZeitstempel : framesMitZeitstempel) {
                    EthernetFrame frame = (EthernetFrame)frameMitZeitstempel[1];
                    if (!frame.samePayload(originalFrame, ttl)) continue;
                    return frame;
                }
            }
        }
        return null;
    }

    private boolean packetOnlyReceivedOrOnlySent(EthernetFrame frame, InternetKnoten node) {
        ProtocolDataUnit daten = frame.getDaten();
        List<NetzwerkInterface> netzwerkInterfaces = node.getNetzwerkInterfaces();
        if (daten instanceof ArpPaket) {
            return true;
        }
        if (daten instanceof IpPaket) {
            for (NetzwerkInterface netzwerkInterface : netzwerkInterfaces) {
                if (!((IpPaket)daten).getEmpfaenger().equals(netzwerkInterface.getIp()) && !((IpPaket)daten).getSender().equals(netzwerkInterface.getIp()) && !((IpPaket)daten).getEmpfaenger().equals("255.255.255.255")) continue;
                return true;
            }
            return false;
        }
        return false;
    }

    public EthernetFrame getFrame(String interfaceId, int frameNumber) {
        Object[] frameMitZeitstempel = this.datenEinheiten.get(interfaceId).get(frameNumber - 1);
        return (EthernetFrame)frameMitZeitstempel[1];
    }

    public int getFrameNumber(String interfaceId, EthernetFrame frame) {
        LinkedList<Object[]> framesMitZeitstempel = this.datenEinheiten.get(interfaceId);
        if (framesMitZeitstempel != null) {
            for (int frameNumber = 0; frameNumber < framesMitZeitstempel.size(); ++frameNumber) {
                if (!((EthernetFrame)framesMitZeitstempel.get(frameNumber)[1]).sameContent(frame)) continue;
                return frameNumber + 1;
            }
        }
        return 0;
    }

    public void storeInterfaceById(String interfaceId, NetzwerkInterface networkInterface) {
        if (!this.InterfaceById.keySet().contains(interfaceId)) {
            this.InterfaceById.put(interfaceId, networkInterface);
        }
    }

    public void storeSystemSoftwareById(String interfaceId, SystemSoftware systemSoftware) {
        if (!this.SystemSoftwareById.keySet().contains(interfaceId)) {
            this.SystemSoftwareById.put(interfaceId, systemSoftware);
        }
    }
}

