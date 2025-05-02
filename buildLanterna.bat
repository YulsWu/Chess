@echo off

cd C:\\Users\Yulun\AppData\Local\Programs\Java\Chess

javac -d bin\lanterna -cp lib\lanterna-3.1.3.jar;lib\commons-codec-1.16.0.jar;lib\mysql-connector-j-9 entry.java lanterna\LanternaChess.java engine\Board.java engine\Move.java exceptions/*.java parser/RegexParser.java db/RegexGameData.java db/RegexDatabase.java

(
echo Manifest-Version: 1.0
echo Main-Class: entry
echo Class-Path: lib\lanterna-3.1.3.jar lib\commons-codec-1.16.0.jar lib\mysql-connector-j-9.1.0.jar
echo.
) > manifest.txt

jar cfvm LanternaChess.jar manifest.txt -C bin/lanterna .

if "%~1"=="nopause" goto skip
pause
:skip