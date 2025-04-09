@echo off
cd C:\\Users\Yulun\AppData\Local\Programs\Java\Chess

(
echo Manifest-Version: 1.0
echo Main-class: entry
echo.
) > manifest.txt



jar cfvm TerminalChess.jar manifest.txt -C bin/terminal .

(
echo @echo off
echo.
echo chcp 65001
echo java -jar TerminalChess.jar
) > StartChess.bat

pause