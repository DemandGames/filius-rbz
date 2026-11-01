package filius.software.email;

import filius.software.clientserver.TCPServerAnwendung;
import filius.software.transportschicht.Socket;
import filius.software.transportschicht.TCPSocket;

/**
 * IMAP service of the mail server (Filius RBZ Wirtschaft Kiel Version). It replaces the POP3 service of
 * Filius 2.9.3: an e-mail program reads the mailbox on the server, the mails stay there.
 */
public class IMAPServer extends TCPServerAnwendung {

    private final EmailServer emailServer;

    public IMAPServer(int port, EmailServer emailServer) {
        this.port = port;
        this.emailServer = emailServer;
    }

    public EmailServer holeEmailServer() {
        return this.emailServer;
    }

    @Override
    protected void neuerMitarbeiter(Socket socket) {
        IMAPMitarbeiter m = new IMAPMitarbeiter((TCPSocket) socket, this);
        m.starten();
        this.mitarbeiter.add(m);
    }
}
