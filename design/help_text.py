"""Texts of the Filius (RBZ Wirtschaft Kiel Version) help pages (used by help.py).

Component and menu names follow the labels Filius shows in each language.
Inline HTML is allowed; every text is written on one line in the page.
"""

KEY = '<span class="key">&nbsp;{}&nbsp;</span>'

TEXT = {
    'de_DE': {
        'tip': 'Tipp:',
        'switch_h': 'Modus wechseln',
        'go_design': 'Netz aufbauen und Geräte einstellen',
        'go_sim': 'Netz in Betrieb nehmen und ausprobieren',
        'go_doc': 'Netz beschriften und als Bild oder Report ausgeben',

        'chip_design': 'ENTWURF',
        'design_title': 'Entwurfsmodus',
        'design_lead': 'Hier baust du dein Netz auf und stellst die Geräte ein.',
        'd_add_h': 'Geräte hinzufügen',
        'd_add_p': 'Ziehe ein Bauteil mit gedrückter Maustaste aus der Leiste links auf die Arbeitsfläche.',
        'd_add_cap': 'Einen PC auf die Fläche ziehen',
        'd_del_p': 'Gelöscht wird ein Gerät über sein Kontextmenü (rechte Maustaste auf das Gerät) oder mit der Taste '
                   + KEY.format('Entf') + ', wenn es ausgewählt ist.',
        'd_del_cap': 'Kontextmenü eines Geräts',
        'd_con_h': 'Geräte verbinden',
        'd_con_p': 'Wähle in der Leiste die <b>Verbindung</b> (Klick oder ' + KEY.format('Alt + 1')
                   + ') und klicke dann nacheinander die beiden Geräte an.',
        'd_con_cap': 'Die Verbindung folgt der Maus bis zum zweiten Gerät.',
        'd_con_p2': 'Entfernen lässt sich eine Verbindung über das Kontextmenü eines angeschlossenen Geräts '
                    '(<b>Verbindung entfernen</b>).',
        'd_con_wlan': 'Drahtlos geht es auch: Stelle am PC oder Laptop <b>Drahtlos (WLAN)</b> ein und wähle '
                      'den Netzwerknamen (SSID) eines Switch / WLAN.',
        'd_cfg_h': 'Geräte einstellen',
        'd_cfg_p': 'Ein Doppelklick auf ein Gerät öffnet unten seine Einstellungen. Der Name ist frei wählbar und '
                   'ändert nichts am Netz. Alle anderen Angaben bestimmen, wie die Daten fließen.',
        'd_cfg_cap': 'Einstellungen eines PCs',
        'n_host': 'PC und Laptop',
        'd_host': 'Brauchen eine IPv4-Adresse und eine Netzmaske, für andere Netze zusätzlich ein Gateway. '
                  'Die Einstellungen können sie auch per DHCP beziehen, wenn im Netz ein DHCP-Server läuft. '
                  'Im Lernpfad sind Laptops die Geräte, an denen jemand arbeitet und surft; PCs bieten als Server '
                  'anderen etwas an, z. B. Webseiten.',
        'n_switch': 'Switch / WLAN',
        'd_switch': 'Verbindet Geräte im selben Netz und braucht keine Einstellungen. Mit einem Netzwerknamen (SSID) '
                    'ist er zugleich WLAN-Zugangspunkt. <b>als Wolke darstellen</b> zeigt ihn als Wolke, etwa für das Internet.',
        'n_gateway': 'Heimrouter',
        'd_gateway': 'Verbindet ein Heimnetz (LAN) mit dem Internet (WAN), wie die Box zu Hause. Er übersetzt Adressen '
                     '(NAT), kann per DHCP Adressen verteilen und bietet Firewall und Portfreigaben.',
        'n_router': 'Vermittlungsrechner',
        'd_router': 'Verbindet verschiedene Netze. Jede Netzwerkkarte bekommt eine Adresse aus ihrem Netz. Die '
                    'Weiterleitungstabelle legt fest, wohin Pakete gehen: von Hand oder mit <b>Automatisches Routing</b>. '
                    'Eine Firewall lässt sich hier oder im Simulationsmodus einrichten.',
        'd_tip': 'Geräte in einem anderen Netz erreichst du nur, wenn als Gateway die Adresse des '
                 'Vermittlungsrechners in deinem eigenen Netz eingetragen ist.',
        'd_mis_h': 'Lernpfad',
        'd_mis_p': 'Der Knopf <b>Lernpfad starten</b> neben den Modi öffnet eine Leiste mit dem Lernpfad. Beim ersten Mal '
                   'wählst du zwischen <b>Standard</b> (8 Kapitel) und <b>Fortgeschritten</b> (12 Kapitel, mit HTTPS, '
                   'Heimnetz, WLAN, Portfreigabe, Firewall und VPN). Jedes Kapitel hat Lesen, Praxis, Quiz und Glossar. Filius prüft die '
                   'Praxis-Aufgaben im laufenden Netz und hakt sie ab, sobald sie wirklich erledigt sind. Der Ring zeigt, '
                   'wie viele Kapitel du geschafft hast. Der Fortschritt beider Lernpfade wird mit deinem Projekt gespeichert; '
                   'öffnest du das Projekt wieder, geht es an derselben Stelle weiter. In der Übersicht kannst du den Lernpfad wechseln.',
        'd_file_h': 'Neu, Laden, Speichern',
        'd_file_p': '<b>Neu</b> beginnt ein leeres Projekt, auch der Lernpfad beginnt dann von vorn. <b>Laden</b> öffnet ein '
                    'gespeichertes Projekt (.fls), <b>Speichern</b> sichert dein Netz zusammen mit dem Fortschritt im Lernpfad. '
                    'Filius merkt sich nichts von selbst: Es startet immer leer, und was nicht in deiner .fls-Datei steht, ist '
                    'nach dem Beenden weg. Beim Schließen mit ungespeicherten Änderungen fragt Filius deshalb nach.',
        'd_lang_h': 'Sprache',
        'd_lang_p': 'Filius gibt es auf Deutsch und Englisch, auch den Lernpfad. Ein Klick auf den Schalter <b>DE | EN</b> oben '
                    'rechts stellt auf Englisch um: Filius fragt nach, bietet an, ungespeicherte Arbeit zu sichern, und startet neu.',
        'd_dark_h': 'Hell und dunkel',
        'd_dark_p': 'Rechts neben <b>DE | EN</b> sitzt der Schalter mit Sonne und Mond. Ein Klick wechselt zwischen hellem und '
                    'dunklem Aussehen, ohne Neustart. Filius merkt sich deine Wahl für den nächsten Start. Die Hilfe und die Bilder '
                    'im Lernpfad und in der Hilfe passen sich an.',

        'chip_sim': 'SIMULATION',
        'sim_title': 'Simulationsmodus',
        'sim_lead': 'Jetzt läuft dein Netz: Du bedienst die Geräte und beobachtest, wie die Daten fließen.',
        's_use_h': 'PCs und Laptops bedienen',
        's_use_p': 'Ein Klick auf einen PC oder Laptop öffnet seinen Desktop. Mit der '
                   '<b>Software-Installation</b> oben links installierst und entfernst du Programme. Das Fenster '
                   'kannst du am Rand größer ziehen oder mit dem Knopf oben rechts auf Vollbild schalten.',
        's_tags_p': 'Jedes Programm trägt ein Etikett, das seine Rolle im Netz zeigt:',
        'tag_server': 'Server', 'tagd_server': 'bietet anderen Geräten einen Dienst an, z. B. Webseiten oder E-Mail-Postfächer.',
        'tag_client': 'Client', 'tagd_client': 'nutzt den Dienst eines Servers, z. B. ruft er Webseiten ab.',
        'tag_p2p': 'Peer-to-Peer', 'tagd_p2p': 'ist Client und Server zugleich.',
        'tag_tool': 'Werkzeug', 'tagd_tool': 'ist weder Client noch Server, z. B. zum Bearbeiten von Dateien oder zum Testen des Netzes.',
        's_tags_cap': 'Verfügbare Programme mit ihren Etiketten',
        's_use_p2': 'Ein Doppelklick auf ein Symbol öffnet das Programm. Ob sein Fenster offen oder geschlossen ist, '
                    'ändert nichts an seiner Arbeit. Das ist besonders für Server-Programme wichtig.',
        's_web_h': 'Eigene Webseiten',
        's_web_p': 'Der Webbrowser Waterwolf zeigt Webseiten wie ein aktueller Browser an: HTML5, CSS (z. B. Flexbox, Grid, '
                   'Variablen, Animationen) und JavaScript (z. B. Ereignisse, DOM, fetch, canvas, localStorage).',
        's_web_p2': 'Lege die Dateien in den Ordner <b>webserver</b> eines PCs mit Webserver: mit dem Text-Editor (Farben für '
                    'HTML, CSS und JavaScript, Zeilennummern, Suche mit <b>Strg+F</b>, Speichern mit <b>Strg+S</b>) oder '
                    'per Import im Datei-Explorer. Jede Datei, die die Seite nachlädt (Stylesheet, Skript, Bild), ist eine '
                    'eigene Anfrage im simulierten Netz.',
        's_web_tip': 'Öffne den Datenaustausch des Laptops mit dem Browser und lade die Seite neu: Du siehst jede einzelne Datei als HTTP-Anfrage.',
        's_https_h': 'Verschlüsselung mit HTTPS',
        's_https_p': 'Im Fenster des Webservers schaltest du unter <b>Verschlüsselung (HTTPS)</b> HTTPS an Port 443 ein und auf Wunsch '
                     'HTTP an Port 80 ab. Unter <b>Zertifikat für</b> stehen die Namen und Adressen, für die das Zertifikat gilt, '
                     'durch Kommas getrennt; steht dort nichts, gilt es für keinen Server. Die Filius-Zertifizierungsstelle unterschreibt es; mit <b>Selbst signiert</b> fehlt '
                     'die Unterschrift, und Browser warnen.',
        's_https_p2': 'Im Webbrowser zeigt ein Schloss vor der Adresse, ob die Seite verschlüsselt kam (grün), mit einem '
                      'Zertifikat, dem der Browser nicht vertraut (rot), oder unverschlüsselt (grau). Ein Klick zeigt das Zertifikat. '
                      'Im Datenaustausch heißen die Nachrichten TLS; Absender und Empfänger sehen dort zusätzlich den '
                      'entschlüsselten Inhalt, alle anderen Geräte nur Zeichensalat.',
        's_vpn_h': 'VPN: Homeoffice',
        's_vpn_p': 'Die Programme <b>VPN-Server</b> und <b>VPN-Client</b> bauen einen verschlüsselten Tunnel durchs Internet. '
                   'Im VPN-Server legst du Benutzer mit Passwort an und startest ihn; er wartet an UDP-Port 1194. Unter '
                   '<b>VPN-Adressen</b> steht das Netz für die Adressen im Tunnel (10.8.0.0), unter <b>Netz im Tunnel</b> das Netz, '
                   'das die Clients erreichen. Leer heißt: das Netz des VPN-Servers.',
        's_vpn_p2': 'Im VPN-Client trägst du die IPv4-Adresse des VPN-Servers, Benutzer und Passwort ein und klickst auf '
                    '<b>Verbinden</b>. Danach gehen alle Pakete für das Netz im Tunnel verschlüsselt an den VPN-Server, der sie '
                    'mit seiner eigenen Adresse weiterschickt. <b>ipconfig</b> zeigt den VPN-Adapter, im Datenaustausch heißen die '
                    'Pakete VPN. Beim Beenden der Simulation wird die Verbindung getrennt.',
        's_ip_h': 'IP-Konfiguration ansehen',
        's_ip_p': 'Das Symbol rechts unten in der Leiste des Desktops zeigt die IP-Konfiguration des Geräts. Ein zweiter Klick '
                  'auf das Symbol oder ein Klick auf den Desktop schließt sie wieder.',
        's_data_h': 'Datenaustausch beobachten',
        's_glow_p': 'Fließt ein Paket durch ein Kabel, füllt sich das Kabel vom Sender aus blau und leert sich dann in Richtung Empfänger – das Blau fließt also mit dem Paket. Im WLAN gehen vom Sender Funkwellen aus, und beim Empfänger leuchtet ein Signalpunkt auf, sobald das Paket angekommen ist.',
        's_glow_cap': 'Ein Paket fließt vom PC zum Switch.',
        's_speed_p': 'Mit dem Regler <b>Tempo</b> oben verlangsamst oder beschleunigst du den Datenaustausch.',
        's_ex_p': 'Alle Daten, die ein Gerät sendet und empfängt, zeigt <b>Datenaustausch anzeigen</b> in seinem '
                  'Kontextmenü (rechte Maustaste).',
        's_table_p': 'In der Tabelle steht jede Nachricht in einer oder mehreren Zeilen. Die Farben zeigen, zu welcher '
                     'Schicht die Angaben gehören. Ein Klick auf eine Zeile zeigt darunter alle Schichten dieser Nachricht.',
        's_model_h': 'TCP/IP- oder OSI-Modell',
        's_model_p': 'Oben rechts im Fenster Datenaustausch wählst du das Schichtenmodell. Zu Beginn ist das TCP/IP-Modell '
                     'mit vier Schichten eingestellt.',
        's_model_p2': 'Im OSI-Modell siehst du alle sieben Schichten mit Nummer. Daneben steht, zu welcher TCP/IP-Schicht '
                      'sie gehören. Für die Schichten 1, 5 und 6 gibt es in Filius keine eigenen Daten; dort steht eine Erklärung.',
        's_model_cap': 'Eine Webseiten-Anfrage im OSI-Modell',
        's_model_tip': 'Vergleiche beide Modelle an derselben Nachricht: Welche OSI-Schichten stecken in der TCP/IP-Schicht Anwendung?',
        's_fw_h': 'Firewall des Vermittlungsrechners',
        's_fw_p': 'Installiere auf einem angeschlossenen Laptop einen Webbrowser und gib in die Adresszeile eine '
                  'IPv4-Adresse des Vermittlungsrechners ein. Auf dieser Seite schaltest du die Firewall ein und aus, '
                  'stellst sie ein und siehst ihr Protokoll.',
        's_sat_h': 'Switch-Tabelle ansehen',
        's_sat_p': 'Ein Switch merkt sich, über welchen Anschluss er welche MAC-Adresse erreicht (Source Address '
                   'Table, SAT). Ein Klick auf den Switch zeigt diese Tabelle.',

        'chip_doc': 'DOKUMENTATION',
        'doc_title': 'Dokumentationsmodus',
        'doc_lead': 'Hier beschriftest und gliederst du dein Netz und gibst es als Bild oder Report aus.',
        'o_el_h': 'Werkzeuge in der Leiste',
        'n_text': 'Text',
        'o_text': 'Erklärungen und Beschriftungen. Aus der Leiste auf die Fläche ziehen; verschoben wird ein Textfeld an '
                  'der Leiste über dem Text, die Schrift formatierst du über das Kontextmenü.',
        'n_field': 'Gliederungsfläche',
        'o_field': 'Fläche, die mehrere Geräte sichtbar zusammenfasst. Aus der Leiste ziehen und am Rand in der Größe '
                   'anpassen; neue Flächen sind weiß, die Farbe änderst du über das Kontextmenü. Das kleine <b>+</b> oben '
                   'links fügt ein Etikett ein, das zur Fläche gehört und mit ihr wandert, z. B. den Namen des Netzes.',
        'n_export': 'Exportieren',
        'o_export': 'Speichert die Darstellung des Netzes als Bilddatei.',
        'n_report': 'Report',
        'o_report': 'Erzeugt eine PDF-Datei mit einem Bild des Netzes und den wichtigsten Einstellungen der Geräte.',
        'o_tip': 'Im Lernpfad bekommt jedes Netz eine Gliederungsfläche um alle seine Geräte. Oben links in der Fläche '
                 'steht das Etikett mit Name und Netzadresse, z. B. „Schulnetz 192.168.10.0/24“ – am schnellsten über das + der '
                 'Fläche, ein Textfeld geht auch. Router bleiben zwischen den Flächen.',
    },

    'en_GB': {
        'tip': 'Tip:',
        'switch_h': 'Switching modes',
        'go_design': 'build the network and configure the devices',
        'go_sim': 'run the network and try it out',
        'go_doc': 'label the network and export it as an image or report',

        'chip_design': 'DESIGN',
        'design_title': 'Design Mode',
        'design_lead': 'Here you build your network and configure the devices.',
        'd_add_h': 'Adding devices',
        'd_add_p': 'Drag a component with the mouse from the bar on the left onto the workspace.',
        'd_add_cap': 'Dragging a PC onto the workspace',
        'd_del_p': 'Delete a device with its context menu (right mouse button on the device) or with the '
                   + KEY.format('Del') + ' key while it is selected.',
        'd_del_cap': 'Context menu of a device',
        'd_con_h': 'Connecting devices',
        'd_con_p': 'Select <b>Connection</b> in the bar (click or ' + KEY.format('Alt + 1')
                   + ') and then click the two devices one after the other.',
        'd_con_cap': 'The connection follows the mouse to the second device.',
        'd_con_p2': 'Remove a connection with the context menu of a connected device (<b>Remove cable</b>).',
        'd_con_wlan': 'Wireless works too: set the PC or laptop to <b>Wireless (Wi-Fi)</b> and choose the '
                      'network name (SSID) of a Switch / Wi-Fi.',
        'd_cfg_h': 'Configuring devices',
        'd_cfg_p': 'Double-click a device to open its settings at the bottom. You can choose any name; it does not '
                   'change the network. All other settings decide how data flows.',
        'd_cfg_cap': 'Settings of a PC',
        'n_host': 'PC and Laptop',
        'd_host': 'Need an IPv4 address and a netmask, and a gateway to reach other networks. They can also get their '
                  'settings by DHCP if a DHCP server is running in the network. In the learning path, laptops are the '
                  'devices people work and browse on; PCs offer something to others as servers, e.g. web pages.',
        'n_switch': 'Switch / Wi-Fi',
        'd_switch': 'Connects devices in the same network and needs no settings. With a network name (SSID) it is also '
                    'a Wi-Fi access point. <b>Display as Cloud</b> shows it as a cloud, for example for the internet.',
        'n_gateway': 'Home Router',
        'd_gateway': 'Connects a home network (LAN) to the internet (WAN), like the router at home. It translates '
                     'addresses (NAT), can hand out addresses by DHCP and offers a firewall and port forwarding.',
        'n_router': 'Router',
        'd_router': 'Connects different networks. Each network card gets an address from its network. The forwarding '
                    'table decides where packets go: set it by hand or with <b>Automatic Routing</b>. A firewall can be '
                    'set up here or in simulation mode.',
        'd_tip': 'Devices in another network can only be reached if the gateway is set to the address of the router '
                 'in your own network.',
        'd_mis_h': 'Learning path',
        'd_mis_p': 'The <b>Start learning path</b> button next to the modes opens the learning path. The first time, you '
                   'choose between <b>Standard</b> (8 chapters) and <b>Advanced</b> (12 chapters, with HTTPS, '
                   'home network, Wi-Fi, port forwarding, firewall and VPN). Every chapter has reading, practice, quiz and glossary. Filius checks the '
                   'practice tasks in the running network and ticks them off as soon as they are really done. The ring shows '
                   'how many chapters you have finished. The progress of both learning paths is saved with your project; when '
                   'you open the project again, it continues at the same place. In the overview you can switch the learning path.',
        'd_file_h': 'New, Open, Save',
        'd_file_p': '<b>New</b> starts an empty project, and the learning path starts from the beginning. <b>Open</b> opens '
                    'a saved project (.fls), <b>Save</b> saves your network together with your progress in the learning path. '
                    'Filius does not keep anything by itself: it always starts empty, and what is not in your .fls file is lost '
                    'when Filius closes. So Filius asks first when you close it with unsaved changes.',
        'd_lang_h': 'Language',
        'd_lang_p': 'Filius is available in English and German, including the learning path. Click the <b>DE | EN</b> switch '
                    'at the top right to switch to German: Filius asks first, offers to save unsaved work and restarts.',
        'd_dark_h': 'Light and dark',
        'd_dark_p': 'To the right of <b>DE | EN</b> is the switch with sun and moon. A click toggles between the light and the '
                    'dark look, without a restart. Filius remembers your choice for the next start. The help and the pictures '
                    'in the learning path and in the help follow your choice.',

        'chip_sim': 'SIMULATION',
        'sim_title': 'Simulation Mode',
        'sim_lead': 'Now your network is running: you use the devices and watch how the data flows.',
        's_use_h': 'Using PCs and laptops',
        's_use_p': 'Click a PC or laptop to open its desktop. With <b>Software Installation</b> at the top left '
                   'you install and remove programs. You can drag the border of the window to make it larger or '
                   'maximize it with the button at the top right.',
        's_tags_p': 'Every program carries a tag that shows its role in the network:',
        'tag_server': 'Server', 'tagd_server': 'offers a service to other devices, e.g. web pages or mailboxes.',
        'tag_client': 'Client', 'tagd_client': 'uses the service of a server, e.g. it fetches web pages.',
        'tag_p2p': 'Peer-to-peer', 'tagd_p2p': 'is client and server at once.',
        'tag_tool': 'Tool', 'tagd_tool': 'is neither client nor server, e.g. for editing files or testing the network.',
        's_tags_cap': 'Available programs with their tags',
        's_use_p2': 'Double-click an icon to open the program. Whether its window is open or closed does not change '
                    'its work. This matters especially for server programs.',
        's_web_h': 'Your own web pages',
        's_web_p': 'The Waterwolf web browser shows pages like a current browser: HTML5, CSS (e.g. flexbox, grid, variables, '
                   'animations) and JavaScript (e.g. events, DOM, fetch, canvas, localStorage).',
        's_web_p2': 'Put the files into the <b>webserver</b> folder of a PC running a web server: with the text editor (colours '
                    'for HTML, CSS and JavaScript, line numbers, find with <b>Ctrl+F</b>, save with <b>Ctrl+S</b>) or '
                    'by importing them in the file explorer. Every file the page loads (stylesheet, script, image) is a request '
                    'of its own in the simulated network.',
        's_web_tip': 'Open the data exchange of the laptop with the browser and reload the page: you see every single file as an HTTP request.',
        's_https_h': 'Encryption with HTTPS',
        's_https_p': 'In the window of the web server you switch on HTTPS at port 443 under <b>Encryption (HTTPS)</b> and, if you '
                     'like, switch off HTTP at port 80. <b>Certificate for</b> holds the names and addresses the certificate is '
                     'valid for, separated by commas; if it is empty, the certificate is valid for no server. The Filius Certificate Authority signs it; with <b>Self-signed</b> the '
                     'signature is missing and browsers warn you.',
        's_https_p2': 'In the web browser a lock in front of the address shows whether the page came encrypted (green), with a '
                      'certificate the browser does not trust (red) or unencrypted (grey). A click shows the certificate. In the '
                      'data exchange the messages are called TLS; sender and receiver also see the decrypted content there, all '
                      'other devices only a jumble of characters.',
        's_vpn_h': 'VPN: home office',
        's_vpn_p': 'The programs <b>VPN server</b> and <b>VPN client</b> build an encrypted tunnel through the internet. In the '
                   'VPN server you add users with passwords and start it; it waits at UDP port 1194. <b>VPN addresses</b> holds the '
                   'network for the addresses in the tunnel (10.8.0.0), <b>Network in the tunnel</b> the network the clients can '
                   'reach. Empty means: the network of the VPN server.',
        's_vpn_p2': 'In the VPN client you enter the IPv4 address of the VPN server, user and password and click <b>Connect</b>. '
                    'After that all packets for the network in the tunnel go encrypted to the VPN server, which sends them on with '
                    'its own address. <b>ipconfig</b> shows the VPN adapter; in the data exchange the packets are called VPN. '
                    'Stopping the simulation disconnects.',
        's_ip_h': 'Viewing the IP configuration',
        's_ip_p': 'The icon at the bottom right of the desktop bar shows the IP configuration of the device. A second click '
                  'on the icon or a click on the desktop closes it again.',
        's_data_h': 'Watching the data exchange',
        's_glow_p': 'When a packet travels through a cable, the cable fills with blue from the sender and then empties towards the receiver – the blue flows along with the packet. On a Wi-Fi link, radio waves go out from the sender and a signal point lights up at the receiver as soon as the packet has arrived.',
        's_glow_cap': 'A packet flows from the PC to the switch.',
        's_speed_p': 'The <b>Speed</b> slider at the top slows down or speeds up the data exchange.',
        's_ex_p': 'All data a device sends and receives is shown by <b>Show data exchange</b> in its context menu '
                  '(right mouse button).',
        's_table_p': 'In the table each message takes one or more rows. The colours show which layer the data belongs '
                     'to. Click a row to see all layers of this message below.',
        's_model_h': 'TCP/IP or OSI model',
        's_model_p': 'At the top right of the data exchange window you choose the layer model. It starts with the TCP/IP '
                     'model and its four layers.',
        's_model_p2': 'The OSI model shows all seven layers with their numbers, and next to them the TCP/IP layer they '
                      'belong to. Filius has no data of its own for layers 1, 5 and 6; an explanation is shown instead.',
        's_model_cap': 'A web page request in the OSI model',
        's_model_tip': 'Compare both models for the same message: which OSI layers are part of the TCP/IP application layer?',
        's_fw_h': 'Firewall of the router',
        's_fw_p': 'Install a web browser on a connected laptop and enter an IPv4 address of the router in the address '
                  'bar. On that page you switch the firewall on and off, configure it and see its log.',
        's_sat_h': 'Viewing the switch table',
        's_sat_p': 'A switch remembers through which port it reaches which MAC address (Source Address Table, SAT). '
                   'Click the switch to see this table.',

        'chip_doc': 'DOCUMENTATION',
        'doc_title': 'Documentation Mode',
        'doc_lead': 'Here you label and structure your network and export it as an image or report.',
        'o_el_h': 'Tools in the bar',
        'n_text': 'Text',
        'o_text': 'Explanations and labels. Drag it from the bar onto the workspace; a text field is moved by the bar above '
                  'its text, and you format the text with the context menu.',
        'n_field': 'Structure Field',
        'o_field': 'A field that groups several devices visually. Drag it from the bar and change its size at the edge; '
                   'new fields are white, you change the colour with the context menu. The small <b>+</b> at its top left '
                   'adds a label that belongs to the field and moves with it, e.g. the name of the network.',
        'n_export': 'Export',
        'o_export': 'Saves the picture of the network as an image file.',
        'n_report': 'Report',
        'o_report': 'Creates a PDF file with a picture of the network and the most important settings of the devices.',
        'o_tip': 'In the learning path, every network gets a structure field around all its devices. At the top left '
                 'of the field there is the label with name and network address, e.g. "School network 192.168.10.0/24" – '
                 'quickest with the + of the field, a text field works too. Routers stay between the fields.',
    },

}
