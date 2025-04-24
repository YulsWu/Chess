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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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

    // Dimensions such as width and height are plain integers
    // Column or row indicies have 1 subtracted after calculating
    // as Lanterna rows and columns are 0-based indexing
    public static final int SCREEN_WIDTH = 57;
    public static final int SCREEN_HEIGHT = 20;
    public static final int INPUT_COL = 1;
    public static final int INPUT_ROW = 18;
    public static final int ERROR_COL = 1;
    public static final int ERROR_ROW = 19;
    public static final int MID_COL = (SCREEN_WIDTH / 2) - 1;
    public static final int MID_ROW = (SCREEN_HEIGHT / 2) - 1;
    public static final int SIDEBAR_COL = ((SCREEN_WIDTH / 3) * 2) + 1;
    public static final int SIDEBAR_WIDTH = SCREEN_WIDTH - (SIDEBAR_COL + 1);
    public static final int BOARDPANE_WIDTH = SCREEN_WIDTH - SIDEBAR_WIDTH;
    public static final int SIDEBAR_MID_COL = (SIDEBAR_COL + 1) + SIDEBAR_WIDTH/2;

    public static final String LINE_FLUSH = " ".repeat(BOARDPANE_WIDTH);

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
        PrintStream systemOut = System.out;

        try (PrintStream logOut = new PrintStream(new FileOutputStream("lanternaLog.txt"))){
            System.setOut(logOut);

            // Create terminal emulator
            DefaultTerminalFactory factory = new DefaultTerminalFactory()
                                                    .setTerminalEmulatorFontConfiguration(SwingTerminalFontConfiguration.getDefaultOfSize(24))
                                                    .setForceAWTOverSwing(false)
                                                    .setInitialTerminalSize(new TerminalSize(SCREEN_WIDTH, SCREEN_HEIGHT));

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


            // GUI Loop
            boolean flipBoard = true;
            terminal.setCursorPosition(new TerminalPosition(INPUT_COL, INPUT_ROW));
            while(true){
                drawMenu(textGraphics, errorGraphics, terminal);

                switch(queryMenuInput(textGraphics, errorGraphics, terminal)){
                    case 0:
                        terminal.close();
                        return;
                    case 1:
                        int restart = 1;
                        while(restart == 1){
                            terminal.clearScreen();
                            restart = gameLoop(textGraphics, errorGraphics, terminal, flipBoard); 
                        }
                        break;
                    case 2:
                        // Replay loop
                        break;
                }
              

                // loopValue = gameLoop(textGraphics, errorGraphics, terminal, true);
                // if (loopValue != 0){
                //     terminal.close();
                //     return;
                // }
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally {
            System.setOut(systemOut);
        }
    }

    public static void drawBoard(TextGraphics textGraphics, int[][] board, int playerInt){
        int colInd = 4;
        StringBuilder sb = new StringBuilder();
        int buffer = (2 * (SCREEN_WIDTH/3)) >= (28 + 2) ? ((2 * (SCREEN_WIDTH/3)) - 28)/2 : 0; // Not technically dead code as I may change constants
        colInd += buffer;
        String fileLabels = playerInt > 0 ? "  a b c d e f g h" : "  h g f e d c b a";

        textGraphics.putString(colInd, 0, fileLabels);
        textGraphics.putString(colInd, 1, "  ----------------");
        for (int i = 0; i < 8; i++){
            int rankLabel;
            int currentRank;

            if (playerInt > 0){
                rankLabel = 8 - i;
                currentRank = 7 - i;
            }
            else {
                rankLabel = i + 1;
                currentRank = i;
            }

            for (int j : board[currentRank]){
                
                sb.append(CHESS_EMOJI.get(j));
            }
            if (playerInt < 0){sb.reverse();}

            for (int k = 8; k >= 1; k--){
                sb.insert(k, " ");
            }

            sb.insert(0, rankLabel + "|");
            sb.append("|" + rankLabel);
            textGraphics.putString(colInd, 2 + i, sb.toString());
            sb.setLength(0);
        }
        textGraphics.putString(colInd, 10, "  ----------------");
        textGraphics.putString(colInd, 11, fileLabels);
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

    public static void debug(Board board){
        System.out.println("Zobrist History:");
        for (long l : board.getZobristHistory()){
            int checksum = (int)(l ^ (l >>> 32));
            System.out.println(String.format("0x%08X", checksum));
        }
        System.out.println("END");

        // System.out.println("whiteShort:" + board.getCastlingRights("whiteShort"));
        // System.out.println("whiteLong:" + board.getCastlingRights("whiteLong"));
        // System.out.println("blackShort:" + board.getCastlingRights("blackShort"));
        // System.out.println("blackLong:" + board.getCastlingRights("blackLong"));
    }
    // return 0 == exit/back to menu
    // return 1 == restart
    public static int gameLoop(TextGraphics textGraphics, TextGraphics errorGraphics, Terminal terminal, boolean flipBoard) throws IOException, InterruptedException{
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
                
                if (flipBoard){
                    drawBoard(textGraphics, board.getBoard(), board.getTurnInt());
                    terminal.flush();
                }
                else {
                    drawBoard(textGraphics, board.getBoard(), 1);
                }

                drawMoves(textGraphics, errorGraphics, terminal, board.getAlgebraicHistory());
               

                //debug(board);
                
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
                    return 1;
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
                        textGraphics.putString(1, 18, " ".repeat(BOARDPANE_WIDTH - (INPUT_COL + 1)));
                        terminal.flush();
                    }
                    else if (input.getKeyType().equals(KeyType.Backspace)){
                        if (sb.length() != 0){
                            sb.deleteCharAt(sb.length() - 1);
                            textGraphics.putString(1, 18, sb.toString() + " ".repeat(BOARDPANE_WIDTH - (INPUT_COL + 1) - sb.length()));
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
                else if (inputString.equalsIgnoreCase("undo")){
                    board.undoLastMove();
                    terminal.clearScreen();
                    continue;
                }
                else if (inputString.equalsIgnoreCase("exit")){
                    terminal.clearScreen();
                    return 0;
                }
                else if (inputString.equalsIgnoreCase("restart")){
                    terminal.clearScreen();
                    return 1;
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
            }// End input loop

            // Now we should have a valid move
            board.playMove(currentMove);
            validMoves = board.updateState(board.getTurnInt(), validMoves);// Provide validmoves before regenerating
            
            if (flipBoard){
                drawBoard(textGraphics, board.getBoard(), board.getTurnInt() * -1);
                terminal.flush(); 
                Thread.sleep(600);
                
                terminal.clearScreen();
            }
            
            if (board.getState() == Board.BOARD_STATE.B_MATE || board.getState() == Board.BOARD_STATE.W_MATE){
                checkmate = true;
            }    
            else if (board.getState() == Board.BOARD_STATE.STALEMATE){
                drawString = "Stalemate";
                forcedDraw = true;
            }
            else if (board.getState() == Board.BOARD_STATE.MATERIAL_DRAW){
                drawString = "Insufficient Material";
                forcedDraw = true;
            }
            else if (board.getState() == Board.BOARD_STATE.SEVENTY_FIVE_DRAW){
                drawString = "75 Move";
                forcedDraw = true;
            }
            else if (board.getState() == Board.BOARD_STATE.FIVE_REPEAT_DRAW){
                drawString = "Five-fold repetition";
                forcedDraw = true;
            }
            
        } // End while(true) game loop   
    }

    public static int menuLoop(TextGraphics textGraphics, TextGraphics errorGraphics, Terminal terminal){

        drawMenu(textGraphics, errorGraphics, terminal);

        return 0;
    }

    public static void drawMenu(TextGraphics textGraphics, TextGraphics errorGraphics, Terminal terminal){
        String title0 = "  _____ _    _ ______  _____ _____ ";
        String title1 = " / ____| |  | |  ____|/ ____/ ____|";
        String title2 = "| |    | |__| | |__  | (___| (___  ";
        String title3 = "| |    |  __  |  __|  \\___ \\\\___ \\ ";
        String title4 = "| |____| |  | | |____ ____) |___) |";
        String title5 = " \\_____|_|  |_|______|_____/_____/ ";
        int titleLength = 35;

        String welcome = "Welcome to";
        textGraphics.putString(MID_COL - (welcome.length()/2), 0, welcome);
        textGraphics.putString(MID_COL - (titleLength / 2), 1, title0);
        textGraphics.putString(MID_COL - (titleLength / 2), 2, title1);
        textGraphics.putString(MID_COL - (titleLength / 2), 3, title2);
        textGraphics.putString(MID_COL - (titleLength / 2), 4, title3);
        textGraphics.putString(MID_COL - (titleLength / 2), 5, title4);
        textGraphics.putString(MID_COL - (titleLength / 2), 6, title5);

        String main = "MAIN MENU";
        String type = "TYPE COMMAND AND ENTER";
        String play = "play";
        String replay = "replay";
        String exit = "exit";

        textGraphics.putString(MID_COL - (main.length()/2), 8, main);
       
        textGraphics.putString(MID_COL - (play.length()/2), 10, play);
        textGraphics.putString(MID_COL - (replay.length()/2), 11, replay);
        textGraphics.putString(MID_COL - (exit.length()/2), 12, exit);
        textGraphics.putString(INPUT_COL, INPUT_ROW - 1, type);

        try{
            terminal.flush();
        }
        catch(IOException e){
            System.out.println("drawMenu(): Exception occurred flushing terminal");
        }
    }

    public static void displayCoordinates(TextGraphics textGraphics, Terminal terminal){

        for (int i = 0; i < SCREEN_WIDTH; i ++){
            for (int j = 0; j < SCREEN_HEIGHT; j++){
                textGraphics.putString(i, j, "|");
            }
        }
    }

    // play = single player game
    // exit
    // replay
    public static int queryMenuInput(TextGraphics textGraphics, TextGraphics errorGraphics, Terminal terminal){
        // Input validation loop
        while(true){
            String input = buildInput(textGraphics, errorGraphics, terminal);

            if (input.equalsIgnoreCase("play")){
                return 1;
            }
            else if (input.equalsIgnoreCase("replay")){
                return 2;
            }
            else if (input.equalsIgnoreCase("exit")){
                return 0;
            }
            else {
                errorGraphics.putString(ERROR_COL, ERROR_ROW, "Invalid input, try again.");
                try{
                    terminal.flush();
                }
                catch (Exception e){
                    System.out.println("queryMenuInput(): Exception occurred flushing terminal");
                }
            }

        } 
    }

    public static String buildInput(TextGraphics textGraphics, TextGraphics errorGraphics, Terminal terminal){
        // Input building loop
        StringBuilder sb = new StringBuilder();

        while (true){
            try{
                terminal.flush();
                KeyStroke input = terminal.readInput();
                if (input.getKeyType().equals(KeyType.Backspace)){
                    if (!sb.isEmpty()){
                        sb.deleteCharAt(sb.length() - 1);
                    }
                    textGraphics.putString(1, 18, sb.toString() + LINE_FLUSH);
                    terminal.flush();
                }
                else if (input.getKeyType().equals(KeyType.Enter)){
                    textGraphics.putString(INPUT_COL, INPUT_ROW, LINE_FLUSH);
                    return sb.toString();
                }
                else if (input.getKeyType().equals(KeyType.Character)){
                    sb.append(input.getCharacter());
                    textGraphics.putString(1, 18, sb.toString());
                    terminal.flush();
                }
            }
            catch (IOException e){
                System.out.println("buildInput() Exception: " + e);
                sb.setLength(0);
                textGraphics.putString(INPUT_COL, INPUT_ROW, LINE_FLUSH);
                errorGraphics.putString(ERROR_COL, ERROR_ROW, "Error occurred processing input, try again");
            }
        }
    }

    public static void drawMoves(TextGraphics textGraphics, TextGraphics errorGraphics, Terminal terminal, ArrayList<String> moves){
        String title = "Played Moves";
        int numRows = SCREEN_HEIGHT;
        int labelCol = SIDEBAR_COL + 1;
        int col1 = labelCol + 3;
        int col2 = col1 + ((SIDEBAR_WIDTH - 3)/2) + 1;
        int moveColWidth = 7;
        
        int startInd = 0;
        if (moves.size() > 2*numRows){
            startInd = moves.size() - numRows;
        } 
        
        for (int i = 0; i < 20; i++){
            textGraphics.putString(SIDEBAR_COL, i, "|");
        }
        textGraphics.putString(SIDEBAR_MID_COL - (title.length()/2), 0, title);
        
        for (int i = startInd, j = 1; i < moves.size(); i++, j *= -1){
            int row = (i/2) + 1;
            String label = row + ".";
            String mv = moves.get(i);
            int centeringBuffer = (moveColWidth - mv.length())/2;
            if (j > 0){
                textGraphics.putString(labelCol + (3 - label.length()), row, label);
                textGraphics.putString(col1 + centeringBuffer, row, mv);
            }
            else {
                textGraphics.putString(col2 + centeringBuffer, row, mv);
            }
        }
        


        safeFlush(terminal);
    }

    public static void safeFlush(Terminal terminal){
        try{
            terminal.flush();
        }
        catch(IOException e){
            System.out.println("safeFlush(): Exception occurred flushing terminal: " + e);
        }
    }

    
}