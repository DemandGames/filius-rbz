; Filius (RBZ Wirtschaft Kiel Version) - exam edition (Klausurversion) as ONE program file
; "Filius-RBZ-Klausur.exe" that needs no installation and no Java on the computer.
;
; The exe contains the complete program folder of the exam edition (with Java runtime). On the first start
; it unpacks it into the temp folder of the user (%TEMP%\Filius-RBZ-Klausur-<build>); later starts use that
; folder directly. Filius then starts from there. "Open" and "Save" show the folder of the exe first, so it
; can lie in the task folder together with the project files.
; If Windows does not allow programs in the temp folder (AppLocker, software restriction policies), the
; program folder is unpacked next to the exe instead (hidden folder "Filius-Klausur-Dateien") and started
; from there.
;
; Built by installer/build-klausur.sh with: makensis -DQUELLE=... -DKENNUNG=... -DSTAND=... -DOUT=... -DICON=...
Unicode true
ManifestDPIAware true
RequestExecutionLevel user
SilentInstall silent
SetCompressor /SOLID lzma
SetCompressorDictSize 64
Name "Filius (RBZ Wirtschaft Kiel Version) - Klausur"
Caption "Filius (RBZ Wirtschaft Kiel Version) - Klausur"
OutFile "${OUT}"
Icon "${ICON}"
VIProductVersion "2.9.3.0"
VIAddVersionKey "ProductName" "Filius (RBZ Wirtschaft Kiel Version) - Klausur"
VIAddVersionKey "FileDescription" "Filius (RBZ Wirtschaft Kiel Version) - Klausurversion ohne Lernpfad"
VIAddVersionKey "CompanyName" "RBZ Wirtschaft Kiel"
VIAddVersionKey "LegalCopyright" "GPL v2/v3 - Filius: Universitaet Siegen u. a.; RBZ-Version: Thorben Demand"
VIAddVersionKey "FileVersion" "2.9.3 (${STAND}) Klausur"
VIAddVersionKey "ProductVersion" "2.9.3 (${STAND}) Klausur"
VIAddVersionKey "Comments" "Quellcode: https://github.com/DemandGames/filius-rbz"

!include "FileFunc.nsh"
!include "LogicLib.nsh"

; -XX:-UsePerfData: no monitoring file hsperfdata_<user> in the temp folder (trouble on terminal servers)
!define JAVAOPT "-XX:-UsePerfData"
!define PREFIX "Filius-RBZ-Klausur-"
!define MARKE ".vollstaendig-${KENNUNG}"

Var ziel
Var parameter
Var ordner
; result of bereitstellen and starten: 1 = ok. (Not the error flag: the LogicLib test ${Errors} clears it.)
Var ok

Section
  ${GetParameters} $parameter
  ; folder for "Open" and "Save": the folder of the exe. A drive root (H:) gets "\." - a backslash right
  ; before the closing quote would escape the quote on the command line.
  StrCpy $ordner "$EXEDIR"
  StrLen $0 $ordner
  ${If} $0 == 2
    StrCpy $ordner "$ordner\."
  ${EndIf}

  ; a second double click while the first start is still unpacking: the first one starts Filius
  System::Call 'kernel32::CreateMutex(p 0, i 1, t "FiliusRBZKlausur-${KENNUNG}") p .r1 ?e'
  Pop $0
  ${If} $0 == 183
    Quit
  ${EndIf}

  Call altesAufraeumen

  StrCpy $ziel "$TEMP\${PREFIX}${KENNUNG}"
  Call bereitstellen
  ${If} $ok == 1
    Call starten
  ${EndIf}
  ${If} $ok != 1
    ; the temp folder cannot be used or programs there are blocked: next to the exe
    StrCpy $ziel "$EXEDIR\Filius-Klausur-Dateien"
    Call bereitstellen
    ${If} $ok == 1
      SetFileAttributes "$ziel" HIDDEN
      Call starten
    ${EndIf}
    ${If} $ok != 1
      MessageBox MB_ICONSTOP|MB_OK "Filius konnte nicht gestartet werden.$\r$\n$\r$\nWindows erlaubt hier offenbar keine Programme im Temp-Ordner ($TEMP) und auch nicht im Ordner dieser Datei ($EXEDIR), oder dort ist kein Platz. Bitte die Administration bitten, Filius-RBZ-Klausur.exe freizugeben.$\r$\n$\r$\nFilius could not be started: Windows blocks programs in the temp folder and in the folder of this file, or there is no space left."
    ${EndIf}
  ${EndIf}
SectionEnd

; Unpacks the program folder into $ziel unless it is complete already. $ok = 1 if it is complete.
Function bereitstellen
  StrCpy $ok 0
  ${If} ${FileExists} "$ziel\${MARKE}"
  ${AndIf} ${FileExists} "$ziel\java-runtime\bin\javaw.exe"
  ${AndIf} ${FileExists} "$ziel\filius-rbz.jar"
    StrCpy $ok 1
    Return
  ${EndIf}
  RMDir /r "$ziel"
  ; can we write there at all? (otherwise straight to the next place, without a message)
  ClearErrors
  CreateDirectory "$ziel"
  FileOpen $0 "$ziel\.probe" w
  IfErrors 0 +2
    Return
  FileClose $0
  Delete "$ziel\.probe"
  Banner::show /set 76 "Preparing Filius (exam) ..." "Filius (Klausur) wird vorbereitet ..."
  ; "try": a file that cannot be written is skipped with the error flag instead of a message box
  SetOverwrite try
  ClearErrors
  SetOutPath "$ziel"
  File /r "${QUELLE}\*.*"
  IfErrors unvollstaendig
  ${If} ${FileExists} "$ziel\java-runtime\bin\javaw.exe"
  ${AndIf} ${FileExists} "$ziel\filius-rbz.jar"
  ${AndIf} ${FileExists} "$ziel\filius.jar"
    ClearErrors
    FileOpen $0 "$ziel\${MARKE}" w
    FileWrite $0 "${STAND} ${KENNUNG}"
    FileClose $0
    IfErrors +2
      StrCpy $ok 1
  ${EndIf}
  unvollstaendig:
  Banner::destroy
FunctionEnd

; Starts Filius from $ziel. $ok = 0 if Windows does not start it (e.g. blocked by AppLocker).
Function starten
  StrCpy $ok 0
  SetOutPath "$ziel"
  ClearErrors
  Exec '"$ziel\java-runtime\bin\javaw.exe" ${JAVAOPT} "-Dfilius.speicherort=$ordner" -jar "$ziel\filius-rbz.jar" $parameter'
  IfErrors +2
    StrCpy $ok 1
FunctionEnd

; Removes folders of other builds of the exam edition from the temp folder. A folder still in use (an older
; version is running) cannot be renamed and is left alone.
Function altesAufraeumen
  FindFirst $0 $1 "$TEMP\${PREFIX}*"
  ${DoWhile} $1 != ""
    ${If} $1 != "${PREFIX}${KENNUNG}"
    ${AndIf} ${FileExists} "$TEMP\$1\*.*"
      ${If} $1 == "${PREFIX}alt"
        RMDir /r "$TEMP\$1"
      ${Else}
        ClearErrors
        Rename "$TEMP\$1" "$TEMP\${PREFIX}alt"
        ${IfNot} ${Errors}
          RMDir /r "$TEMP\${PREFIX}alt"
        ${EndIf}
      ${EndIf}
    ${EndIf}
    FindNext $0 $1
  ${Loop}
  FindClose $0
  ClearErrors
FunctionEnd
