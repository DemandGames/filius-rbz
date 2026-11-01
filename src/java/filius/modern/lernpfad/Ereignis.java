package filius.modern.lernpfad;

import java.util.ArrayList;
import java.util.List;

import filius.hardware.knoten.Knoten;

/**
 * Something that was observed in the running simulation: a ping, an HTTP response, a DNS answer,
 * a command typed into a command line, a window that was opened, ...
 */
public final class Ereignis {

    public enum Art {
        /** echo reply received by {@link #knoten}: quelle = pinging address, ziel = answering address */
        PING_ANTWORT,
        /** echo request from {@link #knoten} without reply (timeout or ICMP unreachable) */
        PING_OHNE_ANTWORT,
        /** echo request arrived at {@link #knoten}: quelle = sender, ziel = own address */
        PING_EMPFANGEN,
        /** ICMP time exceeded arrived at {@link #knoten} from router {@link #quelle} */
        ZEIT_ABGELAUFEN,
        /** HTTP response for {@link #knoten} as client: ziel/port = server, pfad, status, inhalt */
        HTTP,
        /** HTTP request arrived at {@link #knoten} as server: quelle = client address */
        HTTP_ANFRAGE,
        /** DNS answer received by {@link #knoten}: name, typ, werte = "name TYP rdata" */
        DNS,
        /** DNS query sent by {@link #knoten}: name, port = destination port, protokoll */
        DNS_ANFRAGE,
        /** DHCP message seen at {@link #knoten}: name = message type, werte = [chaddr, yiaddr] */
        DHCP,
        /** SMTP server at address {@link #ziel} accepted a mail from client {@link #quelle}: werte = [from, to...] */
        SMTP,
        /**
         * IMAP server {@link #ziel} answered client {@link #quelle} for account {@link #name}: typ LOGIN (logged in) or
         * FETCH (mails of the mailbox handed out, port = number of mails in the first part of the answer) or EXPUNGE
         * (mails deleted, port = number)
         */
        IMAP,
        /**
         * TLS handshake record seen at {@link #knoten} (sender or receiver): name = ClientHello, ServerHello,
         * ClientKeyExchange, Finished or Alert; typ = "gesendet" or "empfangen"; werte = names of the certificate
         * (ServerHello)
         */
        TLS,
        /**
         * VPN record seen at {@link #knoten} (client or server): name = ClientHello, ServerHello, Login, Accept, Reject or
         * Close; typ = "gesendet" or "empfangen"; werte = VPN address of the client (Accept)
         */
        VPN,
        /** TCP connection attempt from {@link #knoten} to ziel:port without SYN-ACK */
        SYN_OHNE_ANTWORT,
        /** TCP connection from {@link #knoten} to ziel:port established */
        TCP_VERBUNDEN,
        /**
         * command typed on {@link #knoten}: name = command, werte = parameters; quelle = own address, typ = gateway
         * and port = number of cables of the device at that moment
         */
        BEFEHL,
        /** data exchange window of {@link #knoten} shown */
        DATENAUSTAUSCH,
        /** switch table of {@link #knoten} shown */
        SAT,
        /** NAT table of {@link #knoten} shown while it had entries */
        NAT,
        /** the OSI view of the data exchange window was used */
        OSI
    }

    public final Art art;
    public long zeit;
    public final Knoten knoten;
    public String quelle = "";
    public String ziel = "";
    public int port;
    public int status;
    public String pfad = "";
    public String host = "";
    public String name = "";
    public String typ = "";
    public final List<String> werte = new ArrayList<>();
    /** body of an HTTP response, grows while further segments arrive */
    public final StringBuilder inhalt = new StringBuilder();

    public Ereignis(Art art, Knoten knoten, long zeit) {
        this.art = art;
        this.knoten = knoten;
        this.zeit = zeit;
    }

    @Override
    public String toString() {
        String k = knoten == null ? "-" : knoten.getName();
        return art + "@" + k + "[" + quelle + "->" + ziel + (port > 0 ? ":" + port : "") + (pfad.isEmpty() ? "" : " " + pfad)
                + (status > 0 ? " " + status : "") + (name.isEmpty() ? "" : " " + name) + (werte.isEmpty() ? "" : " " + werte) + "]";
    }
}
