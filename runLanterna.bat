@echo off

cd C:\\Users\Yulun\AppData\Local\Programs\Java\Chess

javac -d bin\lanterna -cp lib\lanterna-3.1.3.jar entry.java lanterna\LanternaChess.java engine\Board.java engine\Move.java exceptions/AlgebraicParseException.java parser/RegexParser.java

javaw -cp bin\lanterna\;lib\lanterna-3.1.3.jar entry.java
