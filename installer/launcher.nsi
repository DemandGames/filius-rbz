; Filius (RBZ Wirtschaft Kiel Version) - starter "Filius RBZ.exe" in the installation folder.
; Starts the Java runtime next to it with filius-rbz.jar. The working folder is always the installation
; folder (Filius finds its files from there), also when a project file (.fls) is opened by double click.
Unicode true
ManifestDPIAware true
RequestExecutionLevel user
SilentInstall silent
SetCompressor /SOLID lzma
Name "Filius (RBZ Wirtschaft Kiel Version)"
Caption "Filius (RBZ Wirtschaft Kiel Version)"
OutFile "${OUT}"
Icon "${ICON}"
VIProductVersion "2.9.3.0"
VIAddVersionKey "ProductName" "Filius (RBZ Wirtschaft Kiel Version)"
VIAddVersionKey "FileDescription" "Filius (RBZ Wirtschaft Kiel Version)"
VIAddVersionKey "CompanyName" "RBZ Wirtschaft Kiel"
VIAddVersionKey "LegalCopyright" "GPL v2/v3 - Filius: Universitaet Siegen u. a.; RBZ-Version: Thorben Demand"
VIAddVersionKey "FileVersion" "2.9.3 (${STAND})"
VIAddVersionKey "ProductVersion" "2.9.3 (${STAND})"

!include "FileFunc.nsh"

; -XX:-UsePerfData: Java does not create its monitoring file hsperfdata_<user> in the temp folder. On
; terminal servers and with restricted temp folders that file is a known source of trouble; Filius does not
; need it.
!define JAVAOPT "-XX:-UsePerfData"

Section
  ${GetParameters} $0
  SetOutPath "$EXEDIR"
  IfFileExists "$EXEDIR\java-runtime\bin\javaw.exe" 0 systemjava
    Exec '"$EXEDIR\java-runtime\bin\javaw.exe" ${JAVAOPT} -jar "$EXEDIR\filius-rbz.jar" $0'
    Goto ende
  systemjava:
    Exec 'javaw.exe ${JAVAOPT} -jar "$EXEDIR\filius-rbz.jar" $0'
  ende:
SectionEnd
