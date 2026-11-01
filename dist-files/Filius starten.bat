@echo off
rem Filius (RBZ Wirtschaft Kiel Version) - startet Filius 2.9.3 mit neuer Oberflaeche und Lernpfad.
rem Java wird in dieser Reihenfolge gesucht:
rem   1. Ordner java-runtime neben dieser Datei
rem   2. java-runtime einer installierten Filius-Version (C:\Program Files\Filius)
rem   3. Java auf dem System (PATH)
setlocal
cd /d "%~dp0"
set "JAVA="
if exist "%~dp0java-runtime\bin\javaw.exe" set "JAVA=%~dp0java-runtime\bin\javaw.exe"
if not defined JAVA if exist "%ProgramFiles%\Filius\java-runtime\bin\javaw.exe" set "JAVA=%ProgramFiles%\Filius\java-runtime\bin\javaw.exe"
if not defined JAVA if exist "%ProgramFiles(x86)%\Filius\java-runtime\bin\javaw.exe" set "JAVA=%ProgramFiles(x86)%\Filius\java-runtime\bin\javaw.exe"
if not defined JAVA set "JAVA=javaw"
rem -XX:-UsePerfData: keine Java-Ueberwachungsdatei (hsperfdata) im Temp-Ordner, hilft auf Terminalservern
start "" "%JAVA%" -XX:-UsePerfData -jar "%~dp0filius-rbz.jar" %*
endlocal
