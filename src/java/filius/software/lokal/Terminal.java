/*
 * Filius (RBZ Wirtschaft Kiel Version): based on Terminal of Filius 2.9.3 (GPL).
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel, 2026: with a connected VPN client, ipconfig also shows the VPN
 * adapter (VPN address and the network in the tunnel) and route the route through the tunnel. All commands work as
 * before.
 */
package filius.software.lokal;

import filius.exception.TimeOutException;
import filius.rahmenprogramm.EingabenUeberpruefung;
import filius.rahmenprogramm.I18n;
import filius.rahmenprogramm.Information;
import filius.rahmenprogramm.nachrichten.Lauscher;
import filius.software.clientserver.ClientAnwendung;
import filius.software.dns.DNSNachricht;
import filius.software.dns.Resolver;
import filius.software.dns.ResourceRecord;
import filius.software.system.Betriebssystem;
import filius.software.system.Datei;
import filius.software.system.Dateisystem;
import filius.software.system.InternetKnotenBetriebssystem;
import filius.software.transportschicht.ServerSocket;
import filius.software.transportschicht.Socket;
import filius.software.transportschicht.SocketSchnittstelle;
import filius.software.transportschicht.TransportProtokoll;
import filius.software.vermittlungsschicht.ARP;
import filius.software.vermittlungsschicht.ArpPaket;
import filius.software.vermittlungsschicht.IP;
import filius.software.vermittlungsschicht.IcmpPaket;
import filius.software.vermittlungsschicht.Route;
import filius.software.vermittlungsschicht.RouteNotFoundException;
import filius.software.vermittlungsschicht.VermittlungsProtokoll;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import javax.swing.tree.DefaultMutableTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Terminal
extends ClientAnwendung
implements I18n {
    private static Logger LOG = LoggerFactory.getLogger(Terminal.class);
    private Map<String, String> usageInfo;
    boolean abfrageVar;
    private DefaultMutableTreeNode aktuellerOrdner;
    private boolean interrupted = false;

    public void initUsageInfo() {
        if (this.usageInfo == null) {
            this.usageInfo = new HashMap<String, String>();
            this.usageInfo.put("arp", messages.getString("sw_terminal_arp") + "\n" + messages.getString("sw_terminal_usage_arp"));
            this.usageInfo.put("arpsend", messages.getString("sw_terminal_arpsend") + "\n" + messages.getString("sw_terminal_usage_arpsend"));
            this.usageInfo.put("cat", messages.getString("sw_terminal_cat") + "\n" + messages.getString("sw_terminal_usage_cat"));
            this.usageInfo.put("cd", messages.getString("sw_terminal_cd") + "\n" + messages.getString("sw_terminal_usage_cd"));
            this.usageInfo.put("copy", messages.getString("sw_terminal_copy") + "\n" + messages.getString("sw_terminal_usage_copy"));
            this.usageInfo.put("cp", messages.getString("sw_terminal_copy") + "\n" + messages.getString("sw_terminal_usage_copy"));
            this.usageInfo.put("del", messages.getString("sw_terminal_del") + "\n" + messages.getString("sw_terminal_usage_del"));
            this.usageInfo.put("dir", messages.getString("sw_terminal_dir") + "\n" + messages.getString("sw_terminal_usage_dir"));
            this.usageInfo.put("help", messages.getString("sw_terminal_help") + "\n" + messages.getString("sw_terminal_usage_help"));
            this.usageInfo.put("host", messages.getString("sw_terminal_host") + "\n" + messages.getString("sw_terminal_usage_host"));
            this.usageInfo.put("ipconfig", messages.getString("sw_terminal_ipconfig") + "\n" + messages.getString("sw_terminal_usage_ipconfig"));
            this.usageInfo.put("ls", messages.getString("sw_terminal_dir") + "\n" + messages.getString("sw_terminal_usage_dir"));
            this.usageInfo.put("mkdir", messages.getString("sw_terminal_mkdir") + "\n" + messages.getString("sw_terminal_usage_mkdir"));
            this.usageInfo.put("move", messages.getString("sw_terminal_move") + "\n" + messages.getString("sw_terminal_usage_move"));
            this.usageInfo.put("mv", messages.getString("sw_terminal_move") + "\n" + messages.getString("sw_terminal_usage_move"));
            this.usageInfo.put("netstat", messages.getString("sw_terminal_netstat") + "\n" + messages.getString("sw_terminal_usage_netstat"));
            this.usageInfo.put("nslookup", messages.getString("sw_terminal_nslookup") + "\n" + messages.getString("sw_terminal_usage_nslookup"));
            this.usageInfo.put("ping", messages.getString("sw_terminal_ping") + "\n" + messages.getString("sw_terminal_usage_ping"));
            this.usageInfo.put("pwd", messages.getString("sw_terminal_pwd") + "\n" + messages.getString("sw_terminal_usage_pwd"));
            this.usageInfo.put("rm", messages.getString("sw_terminal_del") + "\n" + messages.getString("sw_terminal_usage_del"));
            this.usageInfo.put("route", messages.getString("sw_terminal_route") + "\n" + messages.getString("sw_terminal_usage_route"));
            this.usageInfo.put("tcpdump", messages.getString("sw_terminal_tcpdump") + "\n" + messages.getString("sw_terminal_usage_tcpdump"));
            this.usageInfo.put("touch", messages.getString("sw_terminal_touch") + "\n" + messages.getString("sw_terminal_usage_touch"));
            this.usageInfo.put("traceroute", messages.getString("sw_terminal_traceroute") + "\n" + messages.getString("sw_terminal_usage_traceroute"));
            this.usageInfo.put("type", messages.getString("sw_terminal_cat") + "\n" + messages.getString("sw_terminal_usage_cat"));
        }
    }

    @Override
    public void setSystemSoftware(InternetKnotenBetriebssystem bs) {
        super.setSystemSoftware(bs);
        this.aktuellerOrdner = this.getSystemSoftware().getDateisystem().getRoot();
    }

    public String move(String[] args) {
        return this.mv(args);
    }

    public String mv(String[] args) {
        LOG.trace("INVOKED (" + this.hashCode() + ", T" + this.getId() + ") " + String.valueOf(this.getClass()) + " (Terminal), mv(");
        for (int i = 0; i < args.length; ++i) {
            LOG.debug(i + "='" + args[i] + "' ");
        }
        LOG.debug(")");
        if (!this.numParams(args, 2)) {
            return this.usage("mv");
        }
        if (this.pureCopy(args)) {
            this.getSystemSoftware().getDateisystem().deleteFile(Dateisystem.absoluterPfad(this.getAktuellerOrdner()) + "/" + args[0]);
            this.benachrichtigeBeobachter(messages.getString("sw_terminal_msg35"));
            return messages.getString("sw_terminal_msg35");
        }
        this.benachrichtigeBeobachter(messages.getString("sw_terminal_msg36"));
        return messages.getString("sw_terminal_msg36");
    }

    public String rm(String[] args) {
        return this.del(args);
    }

    public String del(String[] args) {
        LOG.trace("INVOKED (" + this.hashCode() + ", T" + this.getId() + ") " + String.valueOf(this.getClass()) + " (Terminal), del(");
        for (int i = 0; i < args.length; ++i) {
            LOG.debug(i + "='" + args[i] + "' ");
        }
        LOG.debug(")");
        if (!this.numParams(args, 1)) {
            return this.usage("del");
        }
        if (this.getSystemSoftware().getDateisystem().deleteFile(Dateisystem.absoluterPfad(this.getAktuellerOrdner()) + "/" + args[0])) {
            this.benachrichtigeBeobachter(messages.getString("sw_terminal_msg37"));
            return messages.getString("sw_terminal_msg37");
        }
        this.benachrichtigeBeobachter(messages.getString("sw_terminal_msg38"));
        return messages.getString("sw_terminal_msg38");
    }

    private boolean pureCopy(String[] args) {
        LOG.trace("INVOKED (" + this.hashCode() + ", T" + this.getId() + ") " + String.valueOf(this.getClass()) + " (Terminal), pureCopy(");
        for (int i = 0; i < args.length; ++i) {
            LOG.debug(i + "='" + args[i] + "' ");
        }
        LOG.debug(")");
        this.getSystemSoftware().getDateisystem().printTree();
        String srcString = args[0];
        srcString = srcString.length() > 0 && srcString.substring(0, 1).equals("/") ? Dateisystem.evaluatePathString(srcString) : Dateisystem.evaluatePathString(Dateisystem.absoluterPfad(this.getAktuellerOrdner()) + "/" + srcString);
        String destString = args[1];
        destString = destString.length() > 0 && destString.substring(0, 1).equals("/") ? Dateisystem.evaluatePathString(destString) : Dateisystem.evaluatePathString(Dateisystem.absoluterPfad(this.getAktuellerOrdner()) + "/" + destString);
        String destDir = Dateisystem.getDirectory(destString);
        String destFile = Dateisystem.getBasename(destString);
        Datei sfile = this.getSystemSoftware().getDateisystem().holeDatei(srcString);
        if (sfile == null) {
            return false;
        }
        Datei dfile = new Datei(destFile, sfile.getDateiTyp(), sfile.getDateiInhalt());
        return this.getSystemSoftware().getDateisystem().speicherDatei(destDir, dfile);
    }

    public String copy(String[] args) {
        return this.cp(args);
    }

    public String cp(String[] args) {
        LOG.trace("INVOKED (" + this.hashCode() + ", T" + this.getId() + ") " + String.valueOf(this.getClass()) + " (Terminal), cp(");
        for (int i = 0; i < args.length; ++i) {
            LOG.debug(i + "='" + args[i] + "' ");
        }
        LOG.debug(")");
        if (!this.numParams(args, 2)) {
            return this.usage("cp");
        }
        if (this.pureCopy(args)) {
            this.benachrichtigeBeobachter(messages.getString("sw_terminal_msg33"));
            return messages.getString("sw_terminal_msg33");
        }
        this.benachrichtigeBeobachter(messages.getString("sw_terminal_msg34"));
        return messages.getString("sw_terminal_msg34");
    }

    public String ipconfig(String[] args) {
        LOG.trace("INVOKED (" + this.hashCode() + ", " + this.getId() + ") " + String.valueOf(this.getClass()) + " (Terminal), ipconfig(");
        for (int i = 0; i < args.length; ++i) {
            LOG.debug(i + "='" + args[i] + "' ");
        }
        LOG.debug(")");
        if (!this.numParams(args, 0)) {
            return this.usage("ipconfig");
        }
        Betriebssystem bs = (Betriebssystem)this.getSystemSoftware();
        Object ausgabe = "";
        ausgabe = (String)ausgabe + messages.getString("sw_terminal_msg4") + " " + bs.primaryIPAdresse() + "\n";
        ausgabe = (String)ausgabe + messages.getString("sw_terminal_msg5") + " " + bs.primarySubnetMask() + "\n";
        ausgabe = (String)ausgabe + messages.getString("sw_terminal_msg6666") + " " + bs.primaryMACAddress() + "\n";
        ausgabe = (String)ausgabe + messages.getString("sw_terminal_msg6") + " " + bs.getStandardGateway() + "\n";
        ausgabe = (String)ausgabe + messages.getString("sw_terminal_msg27") + " " + bs.getDNSServer() + "\n";
        ausgabe = (String)ausgabe + filius.software.vpn.Vpn.ipconfig(bs);
        this.benachrichtigeBeobachter(ausgabe);
        return (String)ausgabe;
    }

    public String route(String[] args) {
        LOG.trace("INVOKED (" + this.hashCode() + ", " + this.getId() + ") " + String.valueOf(this.getClass()) + " (Terminal), route(");
        for (int i = 0; i < args.length; ++i) {
            LOG.debug(i + "='" + args[i] + "' ");
        }
        LOG.debug(")");
        if (!this.numParams(args, 0)) {
            return this.usage("route");
        }
        Object ausgabe = messages.getString("sw_terminal_msg7");
        LinkedList<String[]> routingTabelle = this.getSystemSoftware().getWeiterleitungstabelle().holeTabelle();
        ListIterator it = routingTabelle.listIterator();
        while (it.hasNext()) {
            String[] eintrag = (String[])it.next();
            ausgabe = (String)ausgabe + "| ";
            for (int i = 0; i < eintrag.length; ++i) {
                ausgabe = (String)ausgabe + eintrag[i] + this.stringFuellen(15 - eintrag[i].length(), " ") + " | ";
            }
            ausgabe = (String)ausgabe + "\n";
        }
        ausgabe = (String)ausgabe + filius.software.vpn.Vpn.route(this.getSystemSoftware(), this::stringFuellen);
        this.benachrichtigeBeobachter(ausgabe);
        return (String)ausgabe;
    }

    public String ls(String[] args) {
        return this.dir(args);
    }

    public String dir(String[] args) {
        String currPath;
        List<Object> liste;
        LOG.trace("INVOKED (" + this.hashCode() + ", " + this.getId() + ") " + String.valueOf(this.getClass()) + " (Terminal), dir(");
        for (int i = 0; i < args.length; ++i) {
            LOG.debug(i + "='" + args[i] + "' ");
        }
        LOG.debug(")");
        if (!this.numParams(args, 0, 1)) {
            return this.usage("dir");
        }
        if (args[0].isEmpty()) {
            liste = this.getSystemSoftware().getDateisystem().listeVerzeichnis(this.aktuellerOrdner);
            currPath = Dateisystem.absoluterPfad(this.aktuellerOrdner);
        } else if (args[0].length() > 0 && args[0].substring(0, 1).equals("/")) {
            liste = this.getSystemSoftware().getDateisystem().listeVerzeichnis(this.getSystemSoftware().getDateisystem().verzeichnisKnoten(args[0]));
            currPath = Dateisystem.evaluatePathString(args[0]);
        } else {
            liste = this.getSystemSoftware().getDateisystem().listeVerzeichnis(Dateisystem.verzeichnisKnoten(this.aktuellerOrdner, args[0]));
            currPath = Dateisystem.evaluatePathString(Dateisystem.absoluterPfad(this.aktuellerOrdner) + "/" + args[0]);
        }
        if (liste == null || liste.size() == 0) {
            this.benachrichtigeBeobachter(messages.getString("sw_terminal_msg8"));
            return messages.getString("sw_terminal_msg8");
        }
        StringBuffer inhalt = new StringBuffer();
        inhalt.append(messages.getString("sw_terminal_msg9") + " " + currPath + ":");
        int anzahlVerzeichnisse = 0;
        int anzahlDateien = 0;
        for (Object tmp : liste) {
            inhalt.append("\n");
            if (tmp instanceof Datei) {
                ++anzahlDateien;
                Datei tmpDatei = (Datei)tmp;
                int leerzeichen = 40 - tmpDatei.getName().length();
                inhalt.append(tmpDatei.getName() + this.stringFuellen(leerzeichen, ".") + tmpDatei.holeGroesse());
                continue;
            }
            ++anzahlVerzeichnisse;
            inhalt.append("[" + String.valueOf(tmp) + "]");
        }
        inhalt.append("\n");
        inhalt.append(messages.getString("sw_terminal_msg10") + anzahlVerzeichnisse);
        inhalt.append(messages.getString("sw_terminal_msg11") + anzahlDateien);
        this.benachrichtigeBeobachter(inhalt.toString());
        return inhalt.toString();
    }

    public String touch(String[] args) {
        LOG.trace("INVOKED (" + this.hashCode() + ", " + this.getId() + ") " + String.valueOf(this.getClass()) + " (Terminal), touch(");
        for (int i = 0; i < args.length; ++i) {
            LOG.debug(i + "='" + args[i] + "' ");
        }
        LOG.debug(")");
        if (!this.numParams(args, 1)) {
            return this.usage("touch");
        }
        String ergebnis = messages.getString("sw_terminal_msg12");
        String absPath = args[0].length() > 0 && args[0].substring(0, 1).equals("/") ? Dateisystem.evaluatePathString(args[0]) : Dateisystem.evaluatePathString(Dateisystem.absoluterPfad(this.aktuellerOrdner) + "/" + args[0]);
        String filePath = Dateisystem.getDirectory(absPath);
        String dateiName = Dateisystem.getBasename(absPath);
        if (!dateiName.equals("")) {
            if (!this.getSystemSoftware().getDateisystem().dateiVorhanden(filePath, dateiName)) {
                this.getSystemSoftware().getDateisystem().speicherDatei(filePath, new Datei(dateiName, "text/txt", ""));
                ergebnis = messages.getString("sw_terminal_msg13");
            } else {
                ergebnis = messages.getString("sw_terminal_msg14");
            }
        } else {
            ergebnis = messages.getString("sw_terminal_msg15");
        }
        this.benachrichtigeBeobachter(ergebnis);
        return ergebnis;
    }

    public String mkdir(String[] args) {
        LOG.trace("INVOKED (" + this.hashCode() + ", " + this.getId() + ") " + String.valueOf(this.getClass()) + " (Terminal), mkdir(");
        for (int i = 0; i < args.length; ++i) {
            LOG.debug(i + "='" + args[i] + "' ");
        }
        LOG.debug(")");
        if (!this.numParams(args, 1)) {
            return this.usage("mkdir");
        }
        String ergebnis = messages.getString("sw_terminal_msg16");
        String absPath = args[0].length() > 0 && args[0].substring(0, 1).equals("/") ? Dateisystem.evaluatePathString(args[0]) : Dateisystem.evaluatePathString(Dateisystem.absoluterPfad(this.aktuellerOrdner) + "/" + args[0]);
        String filePath = Dateisystem.getDirectory(absPath);
        String dateiName = Dateisystem.getBasename(absPath);
        ergebnis = !dateiName.equals("") ? (!this.getSystemSoftware().getDateisystem().dateiVorhanden(filePath, dateiName) && this.getSystemSoftware().getDateisystem().erstelleVerzeichnis(filePath, dateiName) ? messages.getString("sw_terminal_msg17") : messages.getString("sw_terminal_msg18")) : messages.getString("sw_terminal_msg19");
        this.benachrichtigeBeobachter(ergebnis);
        return ergebnis;
    }

    public String cd(String[] args) {
        LOG.trace("INVOKED (" + this.hashCode() + ", " + this.getId() + ") " + String.valueOf(this.getClass()) + " (Terminal), cd(");
        for (int i = 0; i < args.length; ++i) {
            LOG.debug(i + "='" + args[i] + "' ");
        }
        LOG.debug(")");
        String ergebnis = "";
        if (!this.numParams(args, 0, 1)) {
            return this.usage("cd");
        }
        if (this.numParams(args, 1)) {
            DefaultMutableTreeNode newDir = args[0].charAt(0) == '/' ? this.getSystemSoftware().getDateisystem().changeDirectory(args[0]) : this.getSystemSoftware().getDateisystem().changeDirectory(Dateisystem.absoluterPfad(this.aktuellerOrdner), args[0]);
            if (newDir != null) {
                this.aktuellerOrdner = newDir;
            } else {
                ergebnis = messages.getString("sw_terminal_msg20");
            }
        } else {
            ergebnis = Dateisystem.absoluterPfad(this.aktuellerOrdner);
        }
        this.benachrichtigeBeobachter(ergebnis);
        return ergebnis;
    }

    public String pwd(String[] args) {
        LOG.trace("INVOKED (" + this.hashCode() + ", " + this.getId() + ") " + String.valueOf(this.getClass()) + " (Terminal), pwd(");
        for (int i = 0; i < args.length; ++i) {
            LOG.debug(i + "='" + args[i] + "' ");
        }
        LOG.debug(")");
        if (!this.numParams(args, 0)) {
            return this.usage("pwd");
        }
        String ergebnis = Dateisystem.absoluterPfad(this.aktuellerOrdner);
        this.benachrichtigeBeobachter(ergebnis);
        return ergebnis;
    }

    public String netstat(String[] args) {
        StringBuffer ergebnis = new StringBuffer();
        ergebnis.append(messages.getString("sw_terminal_msg49"));
        ergebnis.append("--------------------------------------------------------------------------\n");
        TransportProtokoll transport = this.getSystemSoftware().holeTcp();
        String protocol = "TCP";
        this.processSockets(ergebnis, transport, protocol);
        transport = this.getSystemSoftware().holeUdp();
        protocol = "UDP";
        this.processSockets(ergebnis, transport, protocol);
        this.benachrichtigeBeobachter(ergebnis);
        return ergebnis.toString();
    }

    private void processSockets(StringBuffer ergebnis, TransportProtokoll transport, String protocol) {
        for (SocketSchnittstelle socket : transport.holeAktiveSockets()) {
            for (SocketInformation info : this.getSocketInfo(socket)) {
                ergebnis.append(String.format("| %-7s ", protocol));
                ergebnis.append(String.format("| %15s:%-5s ", info.localHost, info.localPort));
                ergebnis.append(String.format("| %15s:%-5s ", info.remoteHost, info.remotePort));
                ergebnis.append(String.format("| %-12s |\n", info.state));
            }
        }
    }

    private List<SocketInformation> getSocketInfo(SocketSchnittstelle socket) {
        ArrayList<SocketInformation> infoList = new ArrayList<SocketInformation>();
        if (socket instanceof Socket) {
            infoList.add(this.getSocketInfo((Socket)socket));
        } else if (socket instanceof ServerSocket) {
            ServerSocket temp = (ServerSocket)socket;
            infoList.add(this.getSocketInfo(temp));
            for (String port : temp.getSockets().keySet()) {
                infoList.add(this.getSocketInfo(temp.getSockets().get(port)));
            }
        }
        return infoList;
    }

    private SocketInformation getSocketInfo(ServerSocket serverSocket) {
        SocketInformation info = new SocketInformation();
        info.remoteHost = "-";
        info.remotePort = "-";
        info.localHost = "0.0.0.0";
        info.localPort = String.valueOf(serverSocket.getLocalPort());
        info.state = "LISTEN";
        return info;
    }

    private SocketInformation getSocketInfo(Socket socket) {
        SocketInformation info = new SocketInformation();
        info.remoteHost = socket.holeZielIPAdresse();
        info.remotePort = String.valueOf(socket.holeZielPort());
        try {
            Route routingEntry = this.getSystemSoftware().determineRoute(info.remoteHost);
            info.localHost = routingEntry.getInterfaceIpAddress();
        }
        catch (RouteNotFoundException e) {
            info.localHost = "<unknown>";
        }
        info.localPort = String.valueOf(socket.holeLokalenPort());
        info.state = socket.getStateAsString();
        return info;
    }

    public String test(String[] args) {
        String ergebnis = messages.getString("sw_terminal_msg23");
        if (this.getSystemSoftware().getDateisystem().speicherDatei(this.aktuellerOrdner, new Datei("test.txt", "txt", "blaaa"))) {
            ergebnis = messages.getString("sw_terminal_msg24");
        }
        this.benachrichtigeBeobachter(ergebnis);
        return ergebnis;
    }

    public String help(String[] args) {
        return args.length >= 1 ? this.usage(args[0]) : this.usage(null);
    }

    private String usage(String cmd) {
        this.initUsageInfo();
        if (this.usageInfo.containsKey(cmd)) {
            this.benachrichtigeBeobachter(this.usageInfo.get(cmd) + "\n");
        } else {
            this.benachrichtigeBeobachter(messages.getString("sw_terminal_msg25"));
        }
        return messages.getString("sw_terminal_msg25");
    }

    public String type(String[] args) {
        return this.cat(args);
    }

    public String cat(String[] args) {
        StringBuilder result = new StringBuilder();
        if (args == null || args.length < 1 || args[0] == null || "".equals(args[0])) {
            this.usage("cat");
        } else {
            Datei file = this.getSystemSoftware().getDateisystem().holeDatei(this.aktuellerOrdner, args[0]);
            if (null != file) {
                result.append(file.getDateiInhalt());
            } else {
                result.append(messages.getString("sw_terminal_msg54"));
            }
        }
        this.benachrichtigeBeobachter(result.toString());
        return result.toString();
    }

    public String arp(String[] args) {
        StringBuilder ergebnis = new StringBuilder();
        ARP arp = this.getSystemSoftware().holeARP();
        String filterAddress = null;
        if (this.numParams(args, 2) && EingabenUeberpruefung.isValidIpAddress(args[1])) {
            if ("-d".contentEquals(args[0])) {
                arp.removeARPTableEntry(args[1]);
            } else if ("-a".contentEquals(args[0])) {
                filterAddress = args[1];
            }
        } else if (this.numParams(args, 1) && "-d".contentEquals(args[0])) {
            arp.resetArpTable();
        }
        ergebnis.append(messages.getString("sw_terminal_msg50"));
        ergebnis.append("----------------------------------------\n");
        Map<String, String> arpTable = null == filterAddress ? arp.holeARPTabelle() : arp.holeARPTabelle(filterAddress);
        for (String ipAddress : arpTable.keySet()) {
            ergebnis.append(String.format("| %-15s  ", ipAddress));
            ergebnis.append(String.format("| %-17s |\n", arpTable.get(ipAddress)));
        }
        ergebnis.append("----------------------------------------\n");
        this.benachrichtigeBeobachter(ergebnis.toString());
        return ergebnis.toString();
    }

    public String host(String[] args) {
        this.benachrichtigeBeobachter(Boolean.TRUE);
        if (!this.numParams(args, 1)) {
            this.usage("host");
        } else {
            Resolver res = this.getSystemSoftware().holeDNSClient();
            try {
                DNSNachricht result = res.resolveA(args[0]);
                int resultCount = 0;
                for (ResourceRecord rr : result.holeResourceRecords()) {
                    if (!"A".equals(rr.getType())) continue;
                    this.benachrichtigeBeobachter(rr.getDomainname() + " " + messages.getString("sw_terminal_msg28") + " " + rr.getRdata() + "\n");
                    ++resultCount;
                }
                result = res.resolveMX(args[0]);
                for (ResourceRecord rr : result.holeResourceRecords()) {
                    if (!"MX".equals(rr.getType())) continue;
                    this.benachrichtigeBeobachter(rr.getDomainname() + " " + messages.getString("sw_terminal_msg58") + " " + rr.getRdata() + "\n");
                    ++resultCount;
                }
                if (resultCount == 0) {
                    LOG.debug("Terminal 'host': result is empty!");
                    this.benachrichtigeBeobachter(messages.getString("sw_terminal_msg30"));
                }
            }
            catch (TimeOutException e) {
                this.benachrichtigeBeobachter(messages.getString("sw_terminal_msg31"));
            }
            catch (Exception e) {
                LOG.debug("", e);
                this.benachrichtigeBeobachter(messages.getString("sw_terminal_msg29"));
            }
        }
        this.benachrichtigeBeobachter(Boolean.FALSE);
        return null;
    }

    public String nslookup(String[] args) {
        if (!this.numParams(args, 1)) {
            return this.usage("nslookup");
        }
        Betriebssystem bs = (Betriebssystem)this.getSystemSoftware();
        Resolver res = bs.holeDNSClient();
        if (res == null) {
            this.benachrichtigeBeobachter(messages.getString("sw_terminal_msg30"));
            return messages.getString("sw_terminal_msg30");
        }
        this.benachrichtigeBeobachter(Boolean.TRUE);
        try {
            DNSNachricht response = res.resolveA(args[0], bs.getDNSServer());
            String serverAddress = response.isLocal() ? "127.0.0.1" : bs.getDNSServer();
            this.benachrichtigeBeobachter("Server:  " + serverAddress + "\n");
            this.benachrichtigeBeobachter("Address: " + serverAddress + "\n");
            this.benachrichtigeBeobachter("\nNon-authoritative Answer:\n");
            for (ResourceRecord rr : response.holeAntwortResourceRecords()) {
                this.benachrichtigeBeobachter("Name:    " + rr.getDomainname() + "\n");
                this.benachrichtigeBeobachter("Address: " + rr.getRdata() + "\n");
            }
        }
        catch (TimeOutException e) {
            this.benachrichtigeBeobachter(messages.getString("sw_terminal_msg31") + "\n");
        }
        catch (Exception e) {
            LOG.debug("", e);
            this.benachrichtigeBeobachter(messages.getString("sw_terminal_msg29") + "\n");
        }
        this.benachrichtigeBeobachter(Boolean.FALSE);
        return null;
    }

    public String ping(String[] args) {
        int num;
        String targetIp;
        LOG.debug("ping with params: {}", (Object)args.toString());
        String target = null;
        boolean enableBroadcast = false;
        if (this.numParams(args, 1)) {
            target = args[0];
        } else if (this.numParams(args, 2)) {
            target = args[1];
            enableBroadcast = true;
        } else {
            return this.usage("ping");
        }
        Resolver res = this.getSystemSoftware().holeDNSClient();
        if (res == null) {
            LOG.debug("ERROR (" + this.hashCode() + "): Terminal 'host': Resolver is null!");
            this.benachrichtigeBeobachter(messages.getString("sw_terminal_msg30"));
            return messages.getString("sw_terminal_msg30");
        }
        try {
            targetIp = IP.ipCheck(target);
            if (targetIp == null) {
                targetIp = res.holeIPAdresse(target);
            }
            if (targetIp == null) {
                LOG.debug("ERROR (" + this.hashCode() + "): Terminal 'host': result is null!");
                this.benachrichtigeBeobachter(messages.getString("sw_terminal_msg30"));
                return messages.getString("sw_terminal_msg30");
            }
        }
        catch (TimeOutException e) {
            this.benachrichtigeBeobachter(messages.getString("sw_terminal_msg31") + " (DNS)");
            return messages.getString("sw_terminal_msg31 (DNS)");
        }
        catch (Exception e) {
            LOG.debug("", e);
            this.benachrichtigeBeobachter(messages.getString("sw_terminal_msg29"));
            return messages.getString("sw_terminal_msg29");
        }
        try {
            Route route = this.getSystemSoftware().determineRoute(targetIp);
            if (!enableBroadcast && VermittlungsProtokoll.isBroadcast(targetIp, route.getInterfaceIpAddress(), route.getNetMask())) {
                this.benachrichtigeBeobachter(messages.getString("sw_terminal_msg53"));
                return messages.getString("sw_terminal_msg53");
            }
        }
        catch (RouteNotFoundException e1) {
            this.benachrichtigeBeobachter(messages.getString("sw_terminal_msg52"));
            return messages.getString("sw_terminal_msg52");
        }
        this.benachrichtigeBeobachter(Boolean.TRUE);
        this.benachrichtigeBeobachter("PING " + target + " (" + targetIp + ")");
        int receivedReplies = 0;
        int loopNumber = Information.isPosixCommandLineToolBehaviour() ? 10 : 4;
        for (num = 0; !this.interrupted && num < loopNumber; ++num) {
            try {
                long timeStart = System.currentTimeMillis();
                IcmpPaket pingResponse = this.getSystemSoftware().holeICMP().startSinglePing(targetIp, num + 1);
                long timeDiff = 1000L - (System.currentTimeMillis() - timeStart);
                if (pingResponse.getTtl() >= 0) {
                    this.benachrichtigeBeobachter("\nFrom " + pingResponse.getSender() + ": icmp_seq=" + (num + 1) + " ttl=" + pingResponse.getTtl() + " time=" + (System.currentTimeMillis() - timeStart) + "ms");
                    ++receivedReplies;
                }
                if (timeDiff <= 0L) continue;
                try {
                    LOG.debug("Terminal waits for " + timeDiff + " ms");
                    Thread.sleep(timeDiff);
                }
                catch (InterruptedException interruptedException) {}
                continue;
            }
            catch (TimeoutException e) {
                this.benachrichtigeBeobachter("\nFrom " + args[0] + " (" + targetIp + "): icmp_seq=" + (num + 1) + "   -- Timeout!");
                continue;
            }
            catch (Exception e) {
                LOG.debug("", e);
            }
        }
        this.benachrichtigeBeobachter("\n--- " + args[0] + " " + messages.getString("sw_terminal_msg45") + " ---\n" + num + " " + messages.getString("sw_terminal_msg46") + ", " + receivedReplies + " " + messages.getString("sw_terminal_msg47") + ", " + (int)Math.round((1.0 - (double)receivedReplies / (double)num) * 100.0) + "% " + messages.getString("sw_terminal_msg48") + "\n");
        this.benachrichtigeBeobachter(Boolean.FALSE);
        return "";
    }

    public String tcpdump(String[] args) {
        LOG.trace("INVOKED (" + this.hashCode() + ", " + this.getId() + ") " + String.valueOf(this.getClass()) + " (Terminal), tcpdump(");
        for (int i = 0; i < args.length; ++i) {
            LOG.debug(i + "='" + args[i] + "' ");
        }
        LOG.debug(")");
        this.benachrichtigeBeobachter(Boolean.TRUE);
        this.benachrichtigeBeobachter(messages.getString("sw_terminal_msg55"));
        Lauscher lauscher = Lauscher.getLauscher();
        String localMacAddress = ((Betriebssystem)this.getSystemSoftware()).primaryMACAddress();
        int offset = lauscher.getOffsetByTimestamp(localMacAddress, System.currentTimeMillis());
        while (!this.interrupted) {
            Object[][] data = lauscher.getDaten(localMacAddress, true, offset);
            for (int i = 0; i < data.length; ++i) {
                int nextFrameSerialNumber;
                Object[] packetData = data[i];
                int currentFrameSerialNumber = Integer.parseInt(packetData[0].toString());
                int n = nextFrameSerialNumber = data.length > i + 1 ? Integer.parseInt(data[i + 1][0].toString()) : Integer.MAX_VALUE;
                if (currentFrameSerialNumber >= nextFrameSerialNumber) continue;
                String message = String.valueOf(packetData[1]) + " " + String.valueOf(packetData[4]) + " " + String.valueOf(packetData[2]) + " > " + String.valueOf(packetData[3]) + " " + String.valueOf(packetData[6]);
                this.benachrichtigeBeobachter("\n  " + message.trim().replace("\n", "  "));
                ++offset;
            }
            try {
                Thread.sleep(100L);
            }
            catch (InterruptedException interruptedException) {}
        }
        this.benachrichtigeBeobachter(Boolean.FALSE);
        return "";
    }

    public String arpsend(String[] args) {
        String targetIP = "0.0.0.0";
        String targetMAC = "FF:FF:FF:FF:FF:FF";
        String senderIP = null;
        Betriebssystem os = (Betriebssystem)this.getSystemSoftware();
        if (this.numParams(args, 2)) {
            if (EingabenUeberpruefung.isGueltig(args[1], EingabenUeberpruefung.musterIpAdresse)) {
                targetIP = args[1];
                targetMAC = os.holeARP().holeARPTabellenEintrag(targetIP, 2, true);
            }
            if (EingabenUeberpruefung.isGueltig(args[0], EingabenUeberpruefung.musterIpAdresse)) {
                senderIP = args[0];
            }
        }
        if (null != targetMAC && null != senderIP) {
            ArpPaket arpPacket = os.holeARP().sendArpReply(os.primaryMACAddress(), senderIP, targetMAC, targetIP, null);
            this.benachrichtigeBeobachter("  >>> " + String.valueOf(arpPacket));
        } else {
            this.usage("arpsend");
        }
        return null;
    }

    public String traceroute(String[] args) {
        int ttl;
        if (!this.numParams(args, 1)) {
            this.usage("traceroute");
            return null;
        }
        int maxHops = 20;
        String destIP = IP.ipCheck(args[0]);
        if (destIP == null) {
            Resolver res = this.getSystemSoftware().holeDNSClient();
            try {
                destIP = res.holeIPAdresse(args[0]);
            }
            catch (TimeOutException e) {
                this.benachrichtigeBeobachter(messages.getString("sw_terminal_trace_msg2"));
                return null;
            }
        }
        if (destIP == null) {
            this.benachrichtigeBeobachter(messages.getString("sw_terminal_trace_msg3"));
            return null;
        }
        this.benachrichtigeBeobachter(Boolean.TRUE);
        if (destIP.equals(args[0])) {
            this.benachrichtigeBeobachter(String.format(messages.getString("sw_terminal_trace_msg4") + "\n", args[0], maxHops));
        } else {
            this.benachrichtigeBeobachter(String.format(messages.getString("sw_terminal_trace_msg5") + "\n", args[0], destIP, maxHops));
        }
        IcmpPaket recv = null;
        int seqNr = 966;
        int fehler = 0;
        for (ttl = 1; ttl <= maxHops && !this.interrupted; ++ttl) {
            this.benachrichtigeBeobachter(" " + ttl + "    ");
            for (int i = 0; i < 3 && !this.interrupted; ++i) {
                recv = this.getSystemSoftware().holeICMP().sendProbe(destIP, ttl, ++seqNr);
                if (recv != null && recv.getSeqNr() == seqNr) {
                    fehler = 0;
                    break;
                }
                ++fehler;
                this.benachrichtigeBeobachter("* ");
            }
            if (fehler == 0) {
                this.benachrichtigeBeobachter(recv.getSender());
                if (recv.getIcmpType() != 11) {
                    break;
                }
            } else if (fehler > 5) break;
            this.benachrichtigeBeobachter("\n");
        }
        this.benachrichtigeBeobachter(Boolean.FALSE);
        if (ttl >= maxHops) {
            this.benachrichtigeBeobachter("\n\n" + String.format(messages.getString("sw_terminal_trace_msg6"), args[0], maxHops));
        } else if (this.interrupted) {
            this.benachrichtigeBeobachter("\n\n" + messages.getString("sw_terminal_trace_msg7"));
        } else if (recv != null && recv.getIcmpType() == 3) {
            switch (recv.getIcmpCode()) {
                case 0: {
                    this.benachrichtigeBeobachter("\n\n" + String.format(messages.getString("sw_terminal_trace_msg8"), recv.getSender()));
                    break;
                }
                case 1: {
                    this.benachrichtigeBeobachter("\n\n" + String.format(messages.getString("sw_terminal_trace_msg9"), recv.getSender()));
                    break;
                }
                default: {
                    this.benachrichtigeBeobachter("\n\n" + String.format(messages.getString("sw_terminal_trace_msg10"), recv.getSender()));
                    break;
                }
            }
        } else if (fehler == 0) {
            if (ttl == 1) {
                this.benachrichtigeBeobachter("\n\n" + String.format(messages.getString("sw_terminal_trace_msg11"), args[0]));
            } else {
                this.benachrichtigeBeobachter("\n\n" + String.format(messages.getString("sw_terminal_trace_msg12"), args[0], ttl));
            }
        } else {
            this.benachrichtigeBeobachter("\n\n" + messages.getString("sw_terminal_trace_msg13"));
        }
        return null;
    }

    public void setInterrupt(boolean val) {
        this.interrupted = val;
    }

    @Override
    public void beenden() {
        this.setInterrupt(true);
        super.beenden();
    }

    public void terminalEingabeAuswerten(String enteredCommand, String[] enteredParameters) {
        LOG.trace("INVOKED (" + this.hashCode() + ", " + this.getId() + ") " + String.valueOf(this.getClass()) + " (Terminal), terminalEingabeAuswerten(" + enteredCommand + "," + String.valueOf(enteredParameters) + ")");
        Object[] args = new Object[]{enteredParameters};
        try {
            this.getClass().getDeclaredMethod(enteredCommand, enteredParameters.getClass());
            this.setInterrupt(false);
            this.ausfuehren(enteredCommand, args);
        }
        catch (NoSuchMethodException e) {
            this.benachrichtigeBeobachter(messages.getString("terminal_msg2") + "\n" + messages.getString("terminal_msg3"));
        }
        catch (Exception e) {
            LOG.debug("", e);
        }
    }

    public DefaultMutableTreeNode getAktuellerOrdner() {
        return this.aktuellerOrdner;
    }

    public void setAktuellerOrdner(DefaultMutableTreeNode aktuellerOrdner) {
        this.aktuellerOrdner = aktuellerOrdner;
    }

    private String stringFuellen(int a, String fueller) {
        LOG.trace("INVOKED (" + this.hashCode() + ", " + this.getId() + ") " + String.valueOf(this.getClass()) + " (Terminal), stringFuellen(" + a + "," + fueller + ")");
        Object tmp = "";
        for (int i = 0; i < a; ++i) {
            tmp = (String)tmp + fueller;
        }
        return (String)tmp;
    }

    private int countParams(String[] args) {
        LOG.trace("INVOKED (" + this.hashCode() + ", " + this.getId() + ") " + String.valueOf(this.getClass()) + " (Terminal), countParams(" + String.valueOf(args) + ")");
        int count = 0;
        for (int i = 0; i < args.length; ++i) {
            if (!args[i].isEmpty()) {
                ++count;
                continue;
            }
            return count;
        }
        return count;
    }

    private boolean numParams(String[] args, int exactNum) {
        return exactNum == this.countParams(args);
    }

    private boolean numParams(String[] args, int minNum, int maxNum) {
        int count = this.countParams(args);
        return count >= minNum && count <= maxNum;
    }

    private class SocketInformation {
        String localHost;
        String localPort;
        String remoteHost;
        String remotePort;
        String state;

        private SocketInformation() {
        }
    }
}

