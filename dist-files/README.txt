FILIUS (RBZ WIRTSCHAFT KIEL VERSION)
====================================

This version is Filius 2.9.3 with a redesigned user interface and a
learning path for lessons. All functions, examples and saved projects
(.fls) of Filius work as usual. The simulation is extended: HTTPS at the
web server, IMAP instead of POP3 and the programs VPN server and VPN
client; projects that use them only open in this version.


STARTING
--------

Windows:  double-click "Filius starten.bat"
Linux:    ./filius-rbz.sh

On Windows this version brings its own Java runtime (folder
"java-runtime", Java 21). No installation is needed; the folder can also
be on a network drive or a USB stick. If the folder is missing, the Java
runtime of an installed Filius (C:\Program Files\Filius) or Java on the
system is used - the web browser then shows pages in the classic view
(see below).

Filius always starts with an empty work area. To open a project
directly, drag an .fls file onto "Filius starten.bat".

For a shortcut on the desktop or a published app on a terminal server,
the program icon is included as Filius.ico.

The normal Filius stays installed and can still be used at any time. Both
versions share the user profile (~/.filius).


LANGUAGE
--------

Filius (RBZ Wirtschaft Kiel Version) is available in German and English,
including the learning path and the help. At the first start it runs in
German. The switch "DE | EN" at the top right changes it: a click slides
it to the other language, Filius asks first, offers "Save", "Don't save"
or "Cancel" if there are unsaved changes, and then restarts in the other
language. The choice is stored in the user profile.

If config/filius.ini sets a fixed language (locale=de_DE or
locale=en_GB), it applies to everyone; the switch then says so.
French is no longer offered in this version.


SAVING
------

Results are only kept in the project file (.fls) that students save
themselves, for example in their own folder on the server. Filius does
not keep anything by itself:

- Filius always starts empty. In the next lesson, students open their own
  .fls file with "Open"; a learning path that has been started then
  opens by itself at the place where it continues.
- When Filius is closed with unsaved changes, it asks: "Save", "Don't
  save" or "Cancel". Progress in the learning path counts as a change,
  too.
- The original Filius silently kept unsaved work in the user profile
  when it closed (~/.filius/edited_scenario.fls) and opened it, or the
  last project, again at the next start. This version does not do that
  any more; it deletes such old files when it starts.
- The user profile (~/.filius) only keeps the window size, the language,
  light/dark and the folder used last.
- When the language is switched, Filius opens the saved project again
  after the restart.

On a terminal server, "Open" and "Save" show the right folder at once with
the setting modern-speicherort (see below).


THE LEARNING PATH
-----------------

The button "Start learning path" next to the modes opens a panel with the
learning path (once it has started, the button is called "Continue
learning path"). There are two learning paths. The first time, students
choose one of them; later they can switch in the overview ("Switch
learning path"). The progress of both paths is kept separately in the
project file.

STANDARD (8 chapters): 8 quiz questions per chapter. The school, the RGG
and a DNS provider are connected by ONE router with three network cards
(192.168.0.0, 192.168.1.0, 192.168.2.0), without home network, Wi-Fi, port
mapping and firewall, without automatic routing.

   1  Two laptops, one cable              5  Names instead of numbers: DNS
   2  A network with a switch             6  E-mail: an order at the RGG
   3  Web server and website              7  Automatic addresses: DHCP
   4  Connecting networks: school, RGG    8  Final project: troubleshooting

ADVANCED (12 chapters): more background, two routers with automatic
routing, ARP, ports, encryption with HTTPS, IMAP, a home network with
Wi-Fi and NAT, firewall and VPN.

   1  Two laptops, one cable              7  Automatic addresses: DHCP
   2  A local network with a switch       8  The home network: Wi-Fi, router, NAT
   3  Web server and your own website     9  Email: Ordering from the RGG
   4  Encryption: HTTPS with TLS         10  Port mapping and firewall
   5  Connecting networks: school, RGG   11  Home office with VPN
   6  Names instead of numbers: DNS      12  Final project: troubleshooting

Since version 19 the advanced path has 12 chapters: chapter 4 (HTTPS) is
new, email (9) comes before port mapping and firewall (10). Since
version 20 chapter 11 (VPN) and the new final project (12) are included.
Progress from an older
version is moved to the new order automatically when the .fls file is
opened; only the old final project starts again. Project files from
before the two learning paths belong to the advanced path.

Teachers can set one learning path in config/filius.ini (see settings for
teachers); then there is no choice.

Every chapter has four parts that are unlocked one after the other:

- Read: short pages with pictures.
- Practice: tasks in your own network. Filius checks them in the running
  network and only ticks a task off when it is really done and correct
  (e.g. the reply to a ping arrived, the page was loaded by its name).
  Some tasks have a question whose answer is checked against the network.
- Quiz: 10 questions (Standard: 8); questions answered wrongly come again
  at the end.
- Glossary: the terms of the chapter; in the glossary of all chapters,
  terms that are not unlocked yet are hidden.

A chapter is unlocked when the one before is completed. Completed
chapters can be repeated. Then - and on request also for the current
chapter - Filius loads the start setup of the chapter (lernpfad/en/start).
Unsaved work can be saved first.

The progress is saved in the project file (.fls) together with the
network. Students save their project as usual and open it again in the
next lesson: if the file contains a learning path that has been started,
the learning path opens by itself at the place where it continues.
What is not saved is lost when Filius closes - Filius asks first (see
Saving). "New" starts an empty project with an empty learning path; the
progress of the previous project is not taken over.
The overview of the learning path also has "Reset progress". Files
without a learning path (e.g. example networks) do not change the
progress when they are opened.

In the learning path, devices that people work and browse on are
laptops; devices that offer something to others (web pages, DNS, DHCP,
email) are PCs. This is why the component bar shows "PC" and "Laptop".

Changing the texts: the English chapters are plain text files in the
folder lernpfad/en (advanced: kapitel-01.txt to kapitel-12.txt) and
lernpfad/standard/en (standard: kapitel-01.txt to kapitel-08.txt), the
German ones in lernpfad and lernpfad/standard. They can be edited with
any editor (UTF-8). Bold words in the reading text are in the glossary
of the chapter; labels of Filius are in quotes. Tasks can contain bullet
points ("- ...") and tables ("| Device | IP address |", the first row is
the header). In the quiz, every wrong answer is followed by a line
"Wrong: ...": Filius shows it first when somebody picks this answer,
then the "Explanation:" of the right answer. Pictures are in
lernpfad/en/bilder and lernpfad/standard/en/bilder, start setups in
lernpfad/en/start and lernpfad/standard/en/start. Every picture NAME.png
can have a dark variant NAME_dunkel.png of the same size; it is shown in
the dark mode. If you change a chapter, change both languages. The
checking rules of the practice tasks are part of the program, so task
numbers and their order should stay the same. Device names may be German
or English (e.g. "Switch Schule" or "School switch") - Filius recognises
both. If a task gives a name, the name is part of the task: the tick
only comes when the device has it.

The final project chooses one of three variants with faults at random:
three per variant in the standard path (chapter 8), eight per variant in
the advanced path (chapter 12), as tickets to the IT support. A tick
only comes when the cause is fixed and the matching end-to-end test has
worked.


WHAT ELSE IS NEW?
-----------------

- Header with a labelled mode switch: Design / Simulation /
  Documentation. Each mode has its own colour (blue / green / yellow),
  visible as a strip below the header. New, Open and Save are labelled
  below their icons. Next to the modes: the button "Start learning path".
- The work area always reaches the edge of the window, also on wide
  screens; scroll bars and folded panels have the colour of the work
  area (no more white edges).
- A minimised desktop window of a PC or laptop comes back to the front
  when you click the device.
- A logo of its own for Filius (RBZ Wirtschaft Kiel Version): the
  networked water tower with radio waves and network cables. It appears
  in the header, the splash screen, the info window, on the start page
  of new web servers, as window and taskbar icon and as Filius.ico. At
  small sizes (16 to 32 pixels) it shows a compact form without the
  plugs, so the tower stays easy to recognise.
- The splash screen shows the packet effects of Filius: radio waves and
  a signal point on Wi-Fi and a cable filling with blue.
- New info window (i at the top right). It is never open twice; a second
  click on i closes it. A second click on the question mark hides the
  help again.
- Learning path: tasks with several devices, addresses or steps are
  structured with bullet points and tables.
- Quiz: after a wrong answer, Filius first explains why it is not right,
  then why the right answer is right.
- Glossary: every term is explained in several sentences, with examples
  from the practice network.
- Text editor like a code editor: colours for HTML, CSS and JavaScript
  (as in VS Code), line numbers, the current line highlighted, indenting
  with Tab and Shift+Tab. A search field at the top: Ctrl+F jumps into it,
  all matches are marked, Enter or the magnifier goes to the next match,
  Shift+Enter back, Esc back into the text (the match is selected and can
  be overwritten right away). Ctrl+S saves, Ctrl+Z/Ctrl+Y undo and redo
  word by word. The bottom line shows language, line and whether the file
  is saved; a dot in the title marks unsaved changes.
- Email with IMAP instead of POP3: the email program reads the mailbox
  with IMAP (port 143), the mail server waits on port 25 (SMTP) and 143.
  The emails stay on the server: every device with the same account shows
  the same emails, and "Delete email" also deletes the email on the
  server. In the data exchange window the protocol is called IMAP (LOGIN,
  SELECT, FETCH, LOGOUT). Chapter 9 explains IMAP and has the new task
  9.10 (one account on two devices). In older projects, port 110 in the
  account becomes 143 automatically.
- Waterwolf web browser: new logo on the start page of the browser.
- Learning path overview: every chapter shows its number in a circle,
  grey = still locked, blue = current chapter, green = completed (with
  "Completed" below the title).
- Chapter 7 with Wi-Fi: the Home switch sends out the Wi-Fi network
  "Home-WiFi", the Home laptop and the Tablet are connected by radio
  (dashed line). New section in the reading text: at home, router, switch
  and Wi-Fi are in one box, in Filius the Home router and the switch are
  separate - the switch forwards by MAC addresses in the home network,
  the Home router routes to the outside. New in the glossary: Wi-Fi,
  access point, SSID. Task 7.3 now checks the SSID and the Wi-Fi
  connection.
- IPv4: in the learning path, addresses are now always called "IPv4
  address". Chapter 1 briefly explains the difference to IPv6 (longer
  addresses, because IPv4 with about 4.3 billion addresses is not
  enough; both are used side by side today) and that Filius only works
  with IPv4. New in the glossary: IPv6. Filius itself now also says
  IPv4: fields "IPv4 Address" (PC, laptop, router, home router, DHCP,
  DNS server), ipconfig shows "IPv4 address", the firewall "Source IPv4"
  and "Dest. IPv4". "Netmask" everywhere instead of "subnet mask".
- Windows of PCs and laptops in the simulation: can be resized at the
  border and maximized (like normal Windows windows). The home screen
  and the programs grow with them; they cannot get smaller than the old
  size. The background stays sharp at every size: the soft blue gradient
  (a little darker at the top left, a little lighter at the bottom right)
  is calculated for the size of the window, and the network symbol always
  has the same size in the lower right corner (before, a small picture
  was enlarged and became blurred).
- Text editor with the font Consolas (as in VS Code on Windows); if
  Consolas is missing, it uses JetBrains Mono.
- Dark mode: the switch with sun and moon at the top right (next to DE/EN)
  toggles between light and dark with a short cross-fade. Dark are the
  header, the component bar, the work area (dark dot grid, dark green in
  the simulation), the settings, the learning path, the help, the data
  exchange and the windows of the PCs and laptops (command line, text
  editor, DNS server, e-mail etc.). Web pages in the browser stay light,
  as in a real browser; only the start page of Waterwolf turns dark. The
  pictures in the learning path and in the help exist in light and dark
  and switch along. The choice is stored per user and kept for the next
  start.
- Two learning paths: new is the standard learning path with 8 chapters
  (one router for all networks). The previous learning path is called
  "Advanced". Choice when the learning path is opened for the first time,
  switching in the overview, a fixed setting for teachers in
  config/filius.ini (modern-lernpfad-auswahl).
- HTTPS and TLS: the web server can offer HTTPS at port 443 (area
  "Encryption (HTTPS)": HTTPS on/off, HTTP on/off, certificate for names
  and addresses, self-signed). The Filius Certificate Authority signs the
  certificates; the browser shows a lock in front of the address and warns
  about self-signed certificates or a wrong name. The encryption is real
  (RSA for the session key, AES for the data). In the data exchange the
  messages are called TLS; sender and receiver also see the decrypted
  content, in the OSI model TLS is in layer 6.
- Internal class area: the home page of new web servers has an area
  "Internal" with a sign-in (name anna, password Schulfest). The accounts
  are in webserver/intern/zugang.txt (one line name:password); the web
  server never hands out this file.
- Network plan: the term is introduced in chapter 2, the documentation
  tasks start with "Complete the network plan".
- VPN: two new programs, "VPN server" and "VPN client". The VPN server
  waits at UDP port 1194, knows users with passwords and gives out VPN
  addresses (10.8.0.2, ...). The VPN client builds an encrypted tunnel
  (handshake like TLS, data with AES). Packets for the "Network in the
  tunnel" then go encrypted to the VPN server, which sends them on with
  its own address (NAT) and returns the answers through the tunnel.
  ipconfig shows the VPN adapter, route the route through the tunnel. In
  the data exchange the packets are called VPN; both ends of the tunnel
  also see the inner packet. Stopping the simulation disconnects.
- HTTPS: an exception "open anyway" for an untrusted certificate only
  lasts until the simulation is stopped (like restarting a browser).
- New pictures for Switch / Wi-Fi, Router, Notebook and Connection in the
  style of the PC and the home router: the switch as a flat box with the
  switch arrows and the Wi-Fi sign, the router as a blue disc with the four
  router arrows (as in network plans), the notebook with a keyboard that is
  no longer wider than the screen, and a thinner cable with RJ-45 plugs.
  In light mode the switch at the top right shows a sun with rays and a
  small cloud (the counterpart of the moon and the stars).
- Check boxes that are ticked but locked (e.g. "IP forwarding" of a
  router) now show a grey tick; before, they looked empty.
- DNS server: in the input fields, the text cursor no longer jumps to
  the end on every key (arrow left works again).
- When a router reports "no entry in the forwarding table", it keeps
  working while the message is open (in the original the router stopped
  until you clicked OK; after that, even correct addresses failed).
- Email program: when you write an email, the fields for recipients, copy
  and subject have a visible border (in the original they were white on
  white); a wrong address is marked red. Send and Cancel are at the bottom
  right. One tool bar with symbol and label: Fetch, New, Reply, Delete, Set
  up account. On the left under "Mailboxes": Inbox, Sent, Spam and Trash
  (deleted mails go into the trash and are only gone when they are deleted
  there). At the top right you see who is logged in ("Logged in as: anna");
  "Log out" removes the account, then you set up a new one. The welcome
  text stands in the middle.
- The heading "Components" of the bar is larger and bold.
- Documentation mode: new structure fields are light (dark in the dark
  mode) instead of grey. In the learning path, every network gets a
  structure field around all its devices, with a label (name and network
  address) at the top left.
- Removed: the software wizard (own programs in Java) and the settings
  dialog. Filius always uses the standard pictures of the devices; the
  language is changed with the DE | EN switch.
- New, clear graphics for all devices, desktop programs and icons.
- Easy-to-read font "Atkinson Hyperlegible" (tells 1, l, I and 0, O
  apart - important for IP and MAC addresses); command line and
  addresses in "JetBrains Mono".
- Every packet is visible, in its direction: a cable fills with blue from
  the sender up to the receiver and then empties in the same direction, so
  the blue flows along with the packet. On a Wi-Fi link radio waves go out
  from the sender towards the receiver, and a signal point lights up at the
  receiver as soon as the packet has arrived. Even at high speed every
  packet stays visible for at least about a third of a second.
- When a network card is selected in the configuration of a router or
  home router, the cable of this card glows blue and pulses slowly (in the
  design mode).
- Input fields: an invalid entry (e.g. a wrong IPv4 address) stays in the
  field in red, also when you click into another field or press Enter, so
  the mistake stays visible. The device keeps its old value until the entry
  is corrected. (Before, the field jumped back to the old value.) In dark
  mode checked values are now written in a light colour (before: black),
  and the input line of the command line keeps its height when switching
  from light to dark.
- Bug of the original fixed: when the table of a switch (right click, show
  SAT) was open during traffic, a port of the switch could stop working
  (e.g. no more DHCP addresses).
- Empty work area: a welcome text with the logo briefly explains what
  Filius is. Completed tasks and chapters are celebrated with a short
  animation and confetti.
- Command line: "tracert" works as another name for "traceroute", as on
  Windows.
- Home router: when the simulation starts, the NAT table is empty, as
  after a restart. A changed port mapping therefore works at once (the
  original kept old entries for up to 5 minutes).
- Automatic routing (RIP): after switching to the simulation, routers
  know their routes again within a few seconds. In the original this
  often took up to half a minute after every change of mode, and ping
  failed in the meantime.
- The modem has been removed from the component bar. Older projects that
  contain a modem can still be opened.
- Help (question mark at the top right) in the new design with current
  pictures, in English and German.
- Data exchange: switch "TCP/IP | OSI" at the top right. The default is
  the TCP/IP model (4 layers); the OSI model shows all 7 layers with their
  numbers and the matching TCP/IP layer. Layers 1, 5 and 6 have no data of
  their own in Filius and are explained. Clicking a message shows its
  layers as a stack.
- Web browser "Waterwolf" with a real browser engine (WebKit): your own
  web pages may use current HTML5, CSS (flexbox, grid, variables,
  gradients, animations) and JavaScript (events, DOM, fetch, canvas,
  localStorage) - without extra libraries. Every file (page, CSS, JS,
  image, fetch request) is sent as an HTTP request through the simulated
  network and appears in the data exchange. There are also buttons for
  back, forward and reload. Addresses with a port
  (e.g. http://203.0.113.3:8080) work in both views. The log page of the
  firewall (home router address/log) had an HTML error in the original
  and stayed empty in the engine; this is fixed.
- Web server: delivers every file with the right file type (.css, .js,
  .json, .svg, .jpeg, fonts and more); a folder delivers its index.html.
  CSS and JS files can be created with the text editor or imported in
  the file explorer.
- Start page: a newly installed web server gets an example page in the
  style of RBZ Wirtschaft . Kiel (index.html with CSS and JavaScript, plus
  the Filius RBZ logo as rbz-logo.png; in English an English version).
  It explains how the page travels through the network and invites you
  to change it.
  Pages in existing projects stay as they are.
- Software installation: every program carries a tag "Server", "Client",
  "Peer-to-peer" or "Tool". A legend explains the four roles; hovering
  shows a short explanation.
- Documentation mode: a structure field shows a small "+" at its top left.
  A click on it adds a label that belongs to the field and moves with it,
  e.g. "School network 192.168.0.0". Outside the documentation mode the "+"
  is hidden. Other labels are text fields as before; a text field is moved
  by the bar above its text (before: below). In the learning path a label
  with the "+" counts just like one in a text field.
- Open file (e.g. in the text editor): a click on a file puts it into the
  field below, a double click opens it at once - as in Windows.
- Text editor, command line and data exchange show their last lines (the
  newest messages) when they are opened or brought to the front; a click
  into an open window does not move anything.
- Network window of the desktop (symbol at the bottom right): the addresses
  are easy to read without frames; a second click on the symbol or a click
  on the desktop closes it.
- Start page of new web servers: the logo stands free (no white box) in the
  dark head of the page. The start page of the Waterwolf browser follows
  the dark mode (dark background, light text).
- Help (question mark) in the dark mode with dark pictures.
- Learning path: a new chapter starts in a window of its own with title,
  goal and the choice "With my network", "Load start setup" or "Cancel".
  A second click on a picture closes its enlarged view again, also while a
  settings window (e.g. of the DHCP server) is open.
- Lists and tables (static DHCP assignments, DNS records, port
  forwarding, firewall rules, mail accounts): the selected row has a soft
  blue tint instead of cyan and is easy to read in light and dark. In
  port forwarding and firewall rules one click into a cell edits it.
- Static DHCP assignments (DHCP server and home router): adding and
  removing count at once, also when the window is closed with the cross.
- Firewall of a router: the default action names the approach:
  "accept (blacklist)", "drop (whitelist)".
- Web server with HTTPS: the certificate is only valid for the names and
  addresses entered at "Certificate for". An empty field is valid for no
  server, and the browser warns.
- Text editor: a click anywhere into the text (also below the last line
  or on the line numbers) gives the keyboard to the text; Enter only jumps
  to the next match while you are in the search field. While you write in
  the text, the view stays where you are, even if the search field still
  holds a term (before, every new line made the view jump back to the
  match).
- School website of the start setups: without the icons in the header
  (they repeated the menu bar); the logo has a shadow in its own shape
  instead of a box.
- Two screens on Windows: Filius draws without Direct3D and redraws all
  windows when one is dragged to the other screen (before, the view could
  fall apart).
- Learning path: a chapter that has not been started shows only the
  button "Start chapter" in the bar; text and choice are in the start
  window, which adapts to the length of the text. Steps such as the TLS
  handshake appear as a numbered list.
- Learning path in full screen: pages that are almost exactly as tall as
  the learning path panel no longer shake back and forth (the scroll bar
  always gets its space, even when it is not needed).
- Start setups of the learning path and the example
  private_and_public_network.fls: the name and the network address of a
  network are in the label of its structure field (the "+" at the top
  left), e.g. "School network 192.168.10.0/24". Additional notes such as
  "DHCP: ...", "HTTPS, port 443" or "private, behind NAT" are in text
  fields.
- Design mode: new icon, a wrench in the style of the two other mode
  icons.
- Info window: version with the date of this version, "2.9.3 (21.10.2026)"
  (also in the splash screen); editor Thorben Demand with the e-mail
  address thorben.demand@rbz-wirtschaft-kiel.de; "Maintenance and Further
  Development (original Filius)".
- Larger start window, sharp icons on high-resolution screens, modern
  dialogs, tables and input fields.


SETTINGS FOR TEACHERS
---------------------

In config/filius.ini, features can be set for all users of this
installation (add a line):

  modern-lernpfad=0    hide the learning path
  modern-lernpfad-auswahl=standard
                       only the standard learning path (no choice);
                       =fortgeschritten only the advanced one,
                       =beide students choose (default)
  modern-schichtmodell=osi
                       the data exchange starts with the OSI model
                       instead of TCP/IP
  modern-browser=klassisch
                       the web browser uses the classic Filius view
                       (HTML 3.2 only, no JavaScript) instead of the engine
  modern-startseite=original
                       new web servers get the original Filius start page
                       instead of the RBZ page
  modern-speicherort=H:\Filius
                       folder that "Open" and "Save" show first (it is
                       created if it is missing). Environment variables
                       are replaced, e.g.
                       modern-speicherort=%HOMESHARE%\Filius or
                       modern-speicherort=D:\Students\%USERNAME%

The browser engine is available for Windows (64 bit). On Linux and macOS,
and with a Java runtime other than the one included, the web browser
automatically shows pages in the classic view.


CONTENTS OF THIS FOLDER
-----------------------

Filius starten.bat     start on Windows
Filius.ico             program icon for shortcuts
filius-rbz.jar         new user interface and learning path (starts the
                       original filius.jar)
LIESMICH.txt           this description in German
lernpfad/              chapter texts, pictures and start setups (German,
                       advanced learning path)
lernpfad/standard/     the same for the standard learning path
lernpfad/en/, lernpfad/standard/en/
                       the same in English
filius.jar, lib/, ...  unchanged program files of Filius 2.9.3
lib/flatlaf-3.7.2.jar  FlatLaf (modern Swing design)
lib/javafx/            browser engine (OpenJFX/WebKit, Windows)
java-runtime/          Java 21 for Windows (Eclipse Temurin, reduced)
beispiele/             example networks
lizenzen/              licence texts and the source code of the changes
                       (quellcode-filius-rbz.zip)


LICENCES
--------

Filius:   GNU GPL v2 or v3 - https://www.lernsoftware-filius.de
          Filius (RBZ Wirtschaft Kiel Version) is a further development
          and is also licensed under GPL v2/v3. The source code comes as
          lizenzen/quellcode-filius-rbz.zip and is available at
          https://github.com/DemandGames/filius-rbz
          Edited by Thorben Demand @ RBZ Wirtschaft Kiel
FlatLaf:  Apache License 2.0 - https://www.formdev.com/flatlaf/
OpenJFX and Java runtime: GPL v2 with Classpath Exception, details in
          lizenzen/OpenJFX-und-Java-Laufzeit.txt
Fonts:    Atkinson Hyperlegible (Braille Institute) and JetBrains Mono,
          both SIL Open Font License 1.1
Logo:     The logo and the icon of Filius (RBZ Wirtschaft Kiel Version) -
          in the header, the splash screen, as window icon, as Filius.ico
          and on the start page of new web servers (rbz-logo.png) - are not
          protected according to their publisher and may be used and
          passed on with the program. The
          logo of RBZ Wirtschaft . Kiel (the school) is no longer included.
          The logo of the Waterwolf web browser (start page of the
          browser) comes from the publisher.
