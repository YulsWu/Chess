package lanterna;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorAutoCloseTrigger;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFrame;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

import com.googlecode.lanterna.terminal.swing.SwingTerminalFontConfiguration;
import com.googlecode.lanterna.terminal.swing.AWTTerminalFontConfiguration.BoldMode;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.io.FileOutputStream;
import java.awt.Font;
import java.util.ArrayList;
import javax.swing.JFrame;

import engine.*;
import parser.RegexParser;
import exceptions.AlgebraicParseException;

public class LanternaChess {
    public static final Map<Integer, String> CHESS_EMOJI = new HashMap<>(){{
    put(-6, "\u2654");
    put(-5, "\u2655");
    put(-4, "\u2656");
    put(-3, "\u2657");
    put(-2, "\u2658");
    put(-1, "\u2659");
    put(0, ".");
    put(1, "\u265F");
    put(2, "\u265E");
    put(3, "\u265D");
    put(4, "\u265C");
    put(5, "\u265B");
    put(6, "\u265A");
    }};

    public static final Map<Integer, String> CHESS_IDENTIFIER = new HashMap<>(){{
        put(-6, "k");
        put(-5, "q");
        put(-4, "r");
        put(-3, "b");
        put(-2, "n");
        put(-1, "p");
        put(0, ".");
        put(1, "P");
        put(2, "N");
        put(3, "B");
        put(4, "R");
        put(5, "Q");
        put(6, "K");
        }};

    public static int[][] testBoard = new int[][]{
        new int[]{-4, -2, -3, -5, -6, -3, -2, -4},
        new int[]{-1, -1, -1, -1, -1, -1, -1, -1},
        new int[]{0, 0, 0, 0, 0, 0, 0, 0},
        new int[]{0, 0, 0, 0, 0, 0, 0, 0},
        new int[]{0, 0, 0, 0, 0, 0, 0, 0},
        new int[]{0, 0, 0, 0, 0, 0, 0, 0},
        new int[]{1, 1, 1, 1, 1, 1, 1, 1},
        new int[]{4, 2, 3, 5, 6, 3, 2, 4}
    };

    public static void ScreenTest(){
        Screen screen = null;
        System.out.println("Hello World");
        PrintStream systemOut = System.out;
        
        try(PrintStream out = new PrintStream(new FileOutputStream("LanternaLog.txt"));){
            System.setOut(out);

            DefaultTerminalFactory factory = new DefaultTerminalFactory()
                                                    .setTerminalEmulatorFontConfiguration(SwingTerminalFontConfiguration.getDefaultOfSize(24))
                                                    .setForceAWTOverSwing(false)
                                                    .setInitialTerminalSize(new TerminalSize(20,20));

            Terminal terminal = factory.createTerminal();


            final TextGraphics textGraphics = terminal.newTextGraphics();
            textGraphics.setForegroundColor(TextColor.ANSI.WHITE);
            textGraphics.setBackgroundColor(TextColor.ANSI.BLACK);

            StringBuilder sb = new StringBuilder();
            textGraphics.putString(0, 0, "  a b c d e f g h");
            textGraphics.putString(0, 1, "  ----------------");
            for (int i = 0; i < 8; i++){
                int rank = 8 - i;
                sb.append(rank + "|");
                for (int j : testBoard[i]){

                    sb.append(CHESS_EMOJI.get(j) + " ");
                }
                sb.append("|" + rank);
                textGraphics.putString(0, 2 + i, sb.toString());
                sb.setLength(0);
            }
            textGraphics.putString(0, 10, "  ----------------");
            textGraphics.putString(0, 11, "  a b c d e f g h");

            TerminalPosition inputPosition = new TerminalPosition(0, 18);
            textGraphics.putString(0, 14, "White's turn");

            textGraphics.putString(0, 17, "Please enter move:");

            terminal.flush();



            
        }
        catch (Exception e){
            e.printStackTrace();
        }
        finally {
            System.setOut(systemOut);
        }
    }

    public static void lanternaLoop(){
        Screen screen = null;
        PrintStream systemOut = System.out;

        try (PrintStream logOut = new PrintStream(new FileOutputStream("lanternaLog.txt"))){
            System.setOut(logOut);

            // Create terminal emulator
            DefaultTerminalFactory factory = new DefaultTerminalFactory()
                                                    .setTerminalEmulatorFontConfiguration(SwingTerminalFontConfiguration.getDefaultOfSize(24))
                                                    .setForceAWTOverSwing(false)
                                                    .setInitialTerminalSize(new TerminalSize(28,20));

            Terminal terminal = factory.createTerminal();

            // Set behaviour on "Close" (pressing x on window)
            if (terminal instanceof SwingTerminalFrame){
                SwingTerminalFrame frame = (SwingTerminalFrame) terminal;
                frame.setTitle("Lanterna Chess");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            }
            else {
                System.out.println("Detected non-swing terminal window, exiting.");
                terminal.close();
                return;
            };

            // Create text graphics object to write to console
            final TextGraphics textGraphics = terminal.newTextGraphics();
            textGraphics.setForegroundColor(TextColor.ANSI.WHITE);
            textGraphics.setBackgroundColor(TextColor.ANSI.BLACK);

            final TextGraphics errorGraphics = terminal.newTextGraphics();
            errorGraphics.setForegroundColor(TextColor.ANSI.RED_BRIGHT);
            errorGraphics.setBackgroundColor(TextColor.ANSI.BLACK);

            
            // Game-Restart loop
            while(true){
                Board board = new Board();

                boolean inputError = false;
                boolean claimedDraw = false;
                boolean forcedDraw = false;
                String drawString = null;
                boolean checkmate = false;
                String turnString = null;
                ArrayList<int[]> validMoves = board.generateValidMoves(board.getTurnInt());

                gameLoop:
                while(true){    
                    Move currentMove = null;
                    while (currentMove == null){
                        
                        drawBoard(textGraphics, board.getBoard());
                        terminal.flush();
                        
                        if (claimedDraw || forcedDraw || checkmate){
                            turnString = board.getTurnInt() > 0 ? "Black" : "White"; // Inverted turn string, we're looking for previous player to claim draw
                            
                            // Claimed draws break execution before the turn is flipped
                            if (claimedDraw){
                                turnString = board.getTurnInt() > 0 ? "White" : "Black";
                                textGraphics.putString(1, 13, drawString);
                                textGraphics.putString(1, 14, "Claimed by " + turnString + "               ");
                            }
                            else if (forcedDraw){
                                textGraphics.putString(1, 13, drawString);
                                textGraphics.putString(1, 14,  "draw!"  + "               ");
                            }
                            else if (checkmate){
                                textGraphics.putString(1, 13, turnString + " wins");
                                textGraphics.putString(1, 14,  "by Checkmate!                       ");
                            }
                            
                            textGraphics.putString(1, 15, "Press any key to restart");
                            terminal.flush();
                            terminal.readInput();
                            terminal.clearScreen();
                            break gameLoop;
                        }
    
                        drawUserPrompt(textGraphics, errorGraphics, board.getTurnInt(), inputError, board.getThreeFold(), board.getFiftyMove());
                        terminal.flush();
    
                        StringBuilder sb = new StringBuilder();
                        String inputString = null;
                        // Build input string character by character
                        while(inputString == null){
                            KeyStroke input = terminal.readInput();
            
                            if (input.getKeyType().equals(KeyType.Enter)){
                                inputString = sb.toString();
                                inputError = false;
                                textGraphics.putString(1, 18, "                                               ");
                                terminal.flush();
                            }
                            else if (input.getKeyType().equals(KeyType.Backspace)){
                                if (sb.length() != 0){
                                    sb.deleteCharAt(sb.length() - 1);
                                    textGraphics.putString(1, 18, sb.toString() + "                             ");
                                    terminal.flush();
                                }
                            }
                            else if (input.getKeyType().equals(KeyType.Character)){
                                sb.append(input.getCharacter());
                                textGraphics.putString(1, 18, sb.toString());
                                terminal.flush();
                            } 
                        }
                        
                        // Read input for special commands, then discern if its a valid move in else
                        if ((board.getFiftyMove() || board.getThreeFold()) && inputString.equalsIgnoreCase("claim draw")){
                            drawString = board.getFiftyMove() ? "Fifty move draw" : "Three fold repetition draw";
                            claimedDraw = true;
                            continue;
                        }
                        else if (inputString.equalsIgnoreCase("exit")){
                            terminal.close();
                            return;
                        }
                        else if (inputString.equalsIgnoreCase("restart")){
                            terminal.clearScreen();
                            break gameLoop;
                        }
                        else {
                            try{
                                currentMove = RegexParser.validateMove(inputString, board);
                                inputError = false;
                            }
                            catch (AlgebraicParseException e){
                                inputError = true;
                                continue;
                            }
                        }
                    }// End move input loop
    
                    // Now we should have a valid move
                    board.playMove(currentMove);
                    board.updateState(board.getTurnInt());
                    validMoves = board.generateValidMoves(board.getTurnInt());
                    terminal.clearScreen();
                    
                    int endGame = board.evaluateGameEndConditions(validMoves);
                    if (endGame == 1){
                        checkmate = true;
                    }
                    else if (endGame >= 2){
                        forcedDraw = true;
                        
                        if (endGame == 2){
                            drawString = "Stalemate";
                        }
                        else if (endGame == 3){
                            drawString = "Insufficient Material";
                        }
                        else if (endGame == 4){
                            drawString = "75 Move";
                        }
                        else if (endGame == 5){
                            drawString = "Five-fold repetition";
                        }
                    }
    
    
                } // End while(true) game loop            
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally {
            System.setOut(systemOut);
        }
    }

    public static void drawBoard(TextGraphics textGraphics, int[][] board){
        int colInd = 4;
        StringBuilder sb = new StringBuilder();
        textGraphics.putString(colInd, 0, "  a b c d e f g h");
        textGraphics.putString(colInd, 1, "  ----------------");
        for (int i = 0; i < 8; i++){
            int rank = 8 - i;
            sb.append(rank + "|");
            for (int j : board[7-i]){

                sb.append(CHESS_EMOJI.get(j) + " ");
            }
            sb.append("|" + rank);
            textGraphics.putString(colInd, 2 + i, sb.toString());
            sb.setLength(0);
        }
        textGraphics.putString(colInd, 10, "  ----------------");
        textGraphics.putString(colInd, 11, "  a b c d e f g h");
    }

    public static void drawUserPrompt(TextGraphics textGraphics, TextGraphics errorGraphics, int turnInt, boolean inputError, boolean threeFoldAvailable, boolean fiftyAvailable){
        int colInd = 1;
        String turnString = turnInt > 0 ? "White" : "Black";
        
        textGraphics.putString(colInd, 16,  turnString + " to move");
        textGraphics.putString(colInd, 17, "Please enter move:");
        
        if (inputError){
            errorGraphics.putString(colInd, 19, "Bad input, try again");
        }

        if (threeFoldAvailable){
            textGraphics.putString(colInd, 15, "3 fold draw available");
        }

        if (fiftyAvailable){
            textGraphics.putString(colInd, 14, "50 move draw available");
        }
    }

    public static void drawThreeFoldAvailable(TextGraphics textGraphics){
        textGraphics.putString(1, 14, "Three fold repetition draw available");
    }

    public static void drawFiftyDrawAvailable(TextGraphics textGraphics){
        textGraphics.putString(1, 15, "Fifty move draw available");
    }
}