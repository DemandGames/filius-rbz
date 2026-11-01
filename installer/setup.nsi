; Filius (RBZ Wirtschaft Kiel Version) - installer (NSIS 3, Modern UI 2). Two variants from one script:
;   Filius-RBZ-Setup.exe               current user, no administrator rights, %LOCALAPPDATA%\Programs\Filius RBZ
;   Filius-RBZ-Setup-AlleBenutzer.exe  all users (-DALLE), administrator rights, C:\Program Files\Filius RBZ;
;                                      for school computers and terminal servers (Netman and the like)
; Installs only the program (no readme, no examples, no sources folder). Another Filius installation stays
; untouched. Built by build-setup.sh with: makensis [-DALLE] -DSTAGE=... -DBILDER=... -DOUT=... -DSTAND=... setup.nsi
Unicode true
ManifestDPIAware true
!ifdef ALLE
  RequestExecutionLevel admin
  !define ROOT "HKLM"
!else
  RequestExecutionLevel user
  !define ROOT "HKCU"
!endif
SetCompressor /SOLID lzma
SetCompressorDictSize 64

!define NAME "Filius (RBZ Wirtschaft Kiel Version)"
!define KURZ "Filius RBZ"
!define FIRMA "RBZ Wirtschaft Kiel"
!define VERSION "2.9.3 (${STAND})"
!define APPKEY "Software\FiliusRBZ"
!define UNINSTKEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\FiliusRBZ"
!define PROGID "FiliusRBZ.Projekt"

Name "${NAME}"
Caption "${NAME} - Setup"
OutFile "${OUT}"
!ifdef ALLE
  InstallDir "$PROGRAMFILES64\Filius RBZ"
!else
  InstallDir "$LOCALAPPDATA\Programs\Filius RBZ"
!endif
InstallDirRegKey ${ROOT} "${APPKEY}" "InstallDir"
BrandingText "${NAME} ${VERSION}"
ShowInstDetails nevershow
ShowUninstDetails nevershow

VIProductVersion "2.9.3.0"
VIAddVersionKey /LANG=0 "ProductName" "${NAME}"
VIAddVersionKey /LANG=0 "FileDescription" "${NAME} - Setup"
VIAddVersionKey /LANG=0 "CompanyName" "${FIRMA}"
VIAddVersionKey /LANG=0 "LegalCopyright" "GPL v2/v3 - Filius: Universitaet Siegen u. a.; RBZ-Version: Thorben Demand"
VIAddVersionKey /LANG=0 "FileVersion" "${VERSION}"
VIAddVersionKey /LANG=0 "ProductVersion" "${VERSION}"

!include "MUI2.nsh"
!include "LogicLib.nsh"
!include "FileFunc.nsh"
!include "x64.nsh"
!include "WinVer.nsh"

!define MUI_ICON "${STAGE}\Filius.ico"
!define MUI_UNICON "${STAGE}\Filius.ico"
!define MUI_WELCOMEFINISHPAGE_BITMAP "${BILDER}\seite.bmp"
!define MUI_UNWELCOMEFINISHPAGE_BITMAP "${BILDER}\seite.bmp"
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_RIGHT
!define MUI_HEADERIMAGE_BITMAP "${BILDER}\kopf.bmp"
!define MUI_HEADERIMAGE_UNBITMAP "${BILDER}\kopf.bmp"
!define MUI_ABORTWARNING
!define MUI_COMPONENTSPAGE_NODESC
!define MUI_LANGDLL_REGISTRY_ROOT "${ROOT}"
!define MUI_LANGDLL_REGISTRY_KEY "${APPKEY}"
!define MUI_LANGDLL_REGISTRY_VALUENAME "Sprache"
!define MUI_LANGDLL_WINDOWTITLE "${NAME}"

!define MUI_WELCOMEPAGE_TITLE "$(TxtWillkommenTitel)"
!define MUI_WELCOMEPAGE_TEXT "$(TxtWillkommen)"
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_INSTFILES
!define MUI_FINISHPAGE_TITLE "$(TxtFertigTitel)"
!define MUI_FINISHPAGE_TEXT "$(TxtFertig)"
!define MUI_FINISHPAGE_RUN
!define MUI_FINISHPAGE_RUN_TEXT "$(TxtStarten)"
!define MUI_FINISHPAGE_RUN_FUNCTION FiliusStarten
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

!insertmacro MUI_LANGUAGE "German"
!insertmacro MUI_LANGUAGE "English"
!insertmacro MUI_RESERVEFILE_LANGDLL

LangString TxtWillkommenTitel ${LANG_GERMAN} "Filius (RBZ Wirtschaft Kiel Version) installieren"
LangString TxtWillkommenTitel ${LANG_ENGLISH} "Install Filius (RBZ Wirtschaft Kiel Version)"
!ifdef ALLE
LangString TxtWillkommen ${LANG_GERMAN} "Filius ist eine Lernsoftware zu Rechnernetzen. Diese Version hat eine neue Oberfläche und einen Lernpfad für den Unterricht.$\r$\n$\r$\nVersion ${VERSION}$\r$\n$\r$\nDiese Installation gilt für alle Benutzer dieses Computers (auch auf einem Terminalserver) und braucht Administratorrechte. Eine andere Filius-Installation bleibt unverändert.$\r$\n$\r$\nKlicken Sie auf Weiter, um fortzufahren."
LangString TxtWillkommen ${LANG_ENGLISH} "Filius is learning software about computer networks. This version has a new user interface and a learning path for lessons.$\r$\n$\r$\nVersion ${VERSION}$\r$\n$\r$\nThis installation is for all users of this computer (also on a terminal server) and needs administrator rights. Any other Filius installation stays unchanged.$\r$\n$\r$\nClick Next to continue."
!else
LangString TxtWillkommen ${LANG_GERMAN} "Filius ist eine Lernsoftware zu Rechnernetzen. Diese Version hat eine neue Oberfläche und einen Lernpfad für den Unterricht.$\r$\n$\r$\nVersion ${VERSION}$\r$\n$\r$\nDie Installation gilt nur für den angemeldeten Benutzer und braucht keine Administratorrechte. Eine andere Filius-Installation auf diesem Computer bleibt unverändert.$\r$\n$\r$\nKlicken Sie auf Weiter, um fortzufahren."
LangString TxtWillkommen ${LANG_ENGLISH} "Filius is learning software about computer networks. This version has a new user interface and a learning path for lessons.$\r$\n$\r$\nVersion ${VERSION}$\r$\n$\r$\nThe installation is for the signed-in user only and needs no administrator rights. Any other Filius installation on this computer stays unchanged.$\r$\n$\r$\nClick Next to continue."
!endif
LangString TxtFertigTitel ${LANG_GERMAN} "Filius ist installiert"
LangString TxtFertigTitel ${LANG_ENGLISH} "Filius is installed"
LangString TxtFertig ${LANG_GERMAN} "Filius (RBZ Wirtschaft Kiel Version) ist jetzt installiert. Sie finden es im Startmenü unter ${KURZ}."
LangString TxtFertig ${LANG_ENGLISH} "Filius (RBZ Wirtschaft Kiel Version) is now installed. You find it in the Start menu as ${KURZ}."
LangString TxtStarten ${LANG_GERMAN} "Filius jetzt starten"
LangString TxtStarten ${LANG_ENGLISH} "Start Filius now"
LangString TxtDesktop ${LANG_GERMAN} "Verknüpfung auf dem Desktop"
LangString TxtDesktop ${LANG_ENGLISH} "Shortcut on the desktop"
LangString TxtFls ${LANG_GERMAN} "Projektdateien (.fls) mit dieser Version öffnen"
LangString TxtFls ${LANG_ENGLISH} "Open project files (.fls) with this version"
LangString TxtProgramm ${LANG_GERMAN} "Filius (RBZ Wirtschaft Kiel Version)"
LangString TxtProgramm ${LANG_ENGLISH} "Filius (RBZ Wirtschaft Kiel Version)"
LangString TxtDateityp ${LANG_GERMAN} "Filius-Projekt"
LangString TxtDateityp ${LANG_ENGLISH} "Filius project"
LangString Txt64 ${LANG_GERMAN} "Filius (RBZ Wirtschaft Kiel Version) braucht Windows 10 oder 11 in der 64-Bit-Version."
LangString Txt64 ${LANG_ENGLISH} "Filius (RBZ Wirtschaft Kiel Version) needs the 64-bit version of Windows 10 or 11."

; ------------------------------------------------------------------ program files

!macro AlteTeileEntfernen
  ; parts of an earlier version (files that no longer exist must not stay behind)
  RMDir /r "$INSTDIR\java-runtime"
  RMDir /r "$INSTDIR\lib"
  RMDir /r "$INSTDIR\lernpfad"
  RMDir /r "$INSTDIR\hilfe"
  RMDir /r "$INSTDIR\config"
  RMDir /r "$INSTDIR\tmpl"
  RMDir /r "$INSTDIR\img"
  RMDir /r "$INSTDIR\lizenzen"
  Delete "$INSTDIR\filius-rbz.jar"
  Delete "$INSTDIR\filius.jar"
  Delete "$INSTDIR\Filius RBZ.exe"
  Delete "$INSTDIR\Filius.ico"
!macroend

!macro Kontext
  !ifdef ALLE
    SetShellVarContext all
    SetRegView 64
  !endif
!macroend

Section "$(TxtProgramm)" SecProgramm
  SectionIn RO
  !insertmacro Kontext
  SetOutPath "$INSTDIR"
  !insertmacro AlteTeileEntfernen
  File /r "${STAGE}\*.*"
  WriteUninstaller "$INSTDIR\uninstall.exe"
  CreateShortcut "$SMPROGRAMS\${KURZ}.lnk" "$INSTDIR\Filius RBZ.exe" "" "$INSTDIR\Filius.ico" 0 SW_SHOWNORMAL "" "${NAME}"
  WriteRegStr ${ROOT} "${APPKEY}" "InstallDir" "$INSTDIR"
  WriteRegStr ${ROOT} "${UNINSTKEY}" "DisplayName" "${NAME}"
  WriteRegStr ${ROOT} "${UNINSTKEY}" "DisplayVersion" "${VERSION}"
  WriteRegStr ${ROOT} "${UNINSTKEY}" "Publisher" "${FIRMA}"
  WriteRegStr ${ROOT} "${UNINSTKEY}" "DisplayIcon" "$INSTDIR\Filius.ico"
  WriteRegStr ${ROOT} "${UNINSTKEY}" "InstallLocation" "$INSTDIR"
  WriteRegStr ${ROOT} "${UNINSTKEY}" "UninstallString" '"$INSTDIR\uninstall.exe"'
  WriteRegStr ${ROOT} "${UNINSTKEY}" "QuietUninstallString" '"$INSTDIR\uninstall.exe" /S'
  WriteRegDWORD ${ROOT} "${UNINSTKEY}" "NoModify" 1
  WriteRegDWORD ${ROOT} "${UNINSTKEY}" "NoRepair" 1
  ${GetSize} "$INSTDIR" "/S=0K" $0 $1 $2
  IntFmt $0 "0x%08X" $0
  WriteRegDWORD ${ROOT} "${UNINSTKEY}" "EstimatedSize" "$0"
SectionEnd

Section "$(TxtDesktop)" SecDesktop
  !insertmacro Kontext
  SetOutPath "$INSTDIR"
  CreateShortcut "$DESKTOP\${KURZ}.lnk" "$INSTDIR\Filius RBZ.exe" "" "$INSTDIR\Filius.ico" 0 SW_SHOWNORMAL "" "${NAME}"
SectionEnd

Section "$(TxtFls)" SecFls
  ; the earlier choice is kept and comes back on uninstall
  !insertmacro Kontext
  ReadRegStr $0 ${ROOT} "Software\Classes\.fls" ""
  ${If} $0 != "${PROGID}"
    WriteRegStr ${ROOT} "${APPKEY}" "FlsVorher" "$0"
  ${EndIf}
  WriteRegStr ${ROOT} "Software\Classes\.fls" "" "${PROGID}"
  WriteRegStr ${ROOT} "Software\Classes\.fls\OpenWithProgids" "${PROGID}" ""
  WriteRegStr ${ROOT} "Software\Classes\${PROGID}" "" "$(TxtDateityp)"
  WriteRegStr ${ROOT} "Software\Classes\${PROGID}\DefaultIcon" "" "$INSTDIR\Filius.ico"
  WriteRegStr ${ROOT} "Software\Classes\${PROGID}\shell\open\command" "" '"$INSTDIR\Filius RBZ.exe" "%1"'
  WriteRegDWORD ${ROOT} "${APPKEY}" "Fls" 1
  System::Call 'shell32::SHChangeNotify(i 0x08000000, i 0, p 0, p 0)'
SectionEnd

Function .onInit
  !insertmacro Kontext
  ${IfNot} ${RunningX64}
  ${OrIfNot} ${AtLeastWin10}
    MessageBox MB_OK|MB_ICONSTOP "$(Txt64)"
    Abort
  ${EndIf}
  !insertmacro MUI_LANGDLL_DISPLAY
FunctionEnd

Function FiliusStarten
  SetOutPath "$INSTDIR"
  Exec '"$INSTDIR\Filius RBZ.exe"'
FunctionEnd

; ------------------------------------------------------------------ uninstall

Function un.onInit
  !insertmacro Kontext
  !insertmacro MUI_UNGETLANGUAGE
FunctionEnd

Section "Uninstall"
  !insertmacro Kontext
  Delete "$SMPROGRAMS\${KURZ}.lnk"
  Delete "$DESKTOP\${KURZ}.lnk"
  !insertmacro AlteTeileEntfernen
  Delete "$INSTDIR\uninstall.exe"
  RMDir "$INSTDIR"
  ReadRegStr $0 ${ROOT} "Software\Classes\.fls" ""
  ${If} $0 == "${PROGID}"
    ReadRegStr $1 ${ROOT} "${APPKEY}" "FlsVorher"
    ${If} $1 == ""
      DeleteRegValue ${ROOT} "Software\Classes\.fls" ""
    ${Else}
      WriteRegStr ${ROOT} "Software\Classes\.fls" "" "$1"
    ${EndIf}
  ${EndIf}
  DeleteRegValue ${ROOT} "Software\Classes\.fls\OpenWithProgids" "${PROGID}"
  DeleteRegKey /ifempty ${ROOT} "Software\Classes\.fls\OpenWithProgids"
  DeleteRegKey /ifempty ${ROOT} "Software\Classes\.fls"
  DeleteRegKey ${ROOT} "Software\Classes\${PROGID}"
  DeleteRegKey ${ROOT} "${UNINSTKEY}"
  DeleteRegKey ${ROOT} "${APPKEY}"
  System::Call 'shell32::SHChangeNotify(i 0x08000000, i 0, p 0, p 0)'
SectionEnd
