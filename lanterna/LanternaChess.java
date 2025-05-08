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
import java.util.Collections;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;

import engine.*;
import parser.RegexParser;
import exceptions.AlgebraicParseException;
import db.RegexDatabase;

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

            final TextGraphics highlightGraphics = terminal.newTextGraphics();
            highlightGraphics.setForegroundColor(TextColor.ANSI.BLACK);
            highlightGraphics.setBackgroundColor(TextColor.ANSI.WHITE);


            // GUI Loop
            boolean flipBoard = true;
            terminal.setCursorPosition(new TerminalPosition(INPUT_COL, INPUT_ROW));
            while(true){
                drawMenu(textGraphics, errorGraphics, terminal);

                switch(queryMenuInput(textGraphics, errorGraphics, terminal)){
                    case 0:{
                        terminal.close();
                        return;
                    }
                    case 1: {
                        int restart = 1;
                        while(restart == 1){
                            terminal.clearScreen();
                            restart = gameLoop(textGraphics, errorGraphics, terminal, flipBoard); 
                        }
                        break;
                    }
                    case 2:{
                        // Replay loop
                        terminal.clearScreen();
                        terminal.flush();
                        drawLoadingScreen(textGraphics, terminal);
                        playerCountLoop(textGraphics, errorGraphics, highlightGraphics, terminal);
                            

                    }
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

    public static int replayLoop(TextGraphics textGraphics, TextGraphics errorGraphics, Terminal terminal){
        while(true){
            try{
                terminal.clearScreen();
            }
            catch (IOException e){
                System.out.println("replayLoop(): Exception occurred " + e);
                continue; // Retry loop
            }
        }
    }

    public static void drawStaticBrowserElements(TextGraphics textGraphics, Terminal terminal){
        int bottomPanelBorder = SCREEN_HEIGHT - ((SCREEN_HEIGHT)/4) - 1;
        int verticalBorderBuffer = 1;
        int horizontalBorderBuffer = 0;
        String leftArrow = "<- PREV. PAGE(q)";
        String rightArrow = "NEXT PAGE (e) ->";
        String navigation = "ARROW KEYS TO NAVIGATE";
        String exit = "(ESC) TO MAIN MENU";
        String enter = "(ENTER) TO SELECT GAME";
        String firstPage = "<-(f)irst";
        String lastPage = "(l)ast->";

        // Horizontal Borders--------------------------------------------
        for (int x : new int[]{horizontalBorderBuffer, SCREEN_HEIGHT - 1, bottomPanelBorder}){
            for (int i = 1; i < SCREEN_WIDTH - 1; i++){
                textGraphics.putString(i, x, "-");
            }
        }
        // Vertical borders|||||||||||||||||||||||||||||||||||||||||||||||
        for (int x : new int[]{verticalBorderBuffer, SCREEN_WIDTH - 2}){
            for (int i = 1; i < SCREEN_HEIGHT - 1; i++){
                textGraphics.putString(x, i, "|");
            }
        }

        textGraphics.putString(2, bottomPanelBorder + 1, leftArrow);
        textGraphics.putString(2, bottomPanelBorder + 2, firstPage);
        textGraphics.putString(SCREEN_WIDTH - 1 - 1 - rightArrow.length(), bottomPanelBorder + 1, rightArrow);
        textGraphics.putString(SCREEN_WIDTH - 1 - 1 - lastPage.length(), bottomPanelBorder + 2, lastPage);
        
        textGraphics.putString(MID_COL - (enter.length()/2), bottomPanelBorder + 2, enter);
        textGraphics.putString(MID_COL - (exit.length()/2), bottomPanelBorder + 3, exit);
        textGraphics.putString(MID_COL - (navigation.length()/2), bottomPanelBorder + 4, navigation);

        safeFlush(terminal);
    }

    public static int playerCountLoop(TextGraphics textGraphics, TextGraphics errorGraphics, TextGraphics highlightGraphics, Terminal terminal){        
        ArrayList<Map.Entry<String, Integer>> players = RegexDatabase.readPlayerCounts();
        safeClear(terminal);
        
        // Create list of all posititions the entries go into
        ArrayList<int[]> entryPositions = new ArrayList<>();
        for (int col = 2; col <= 29; col += 27){
            for (int row = 2; row <= 13; row++){
                entryPositions.add(new int[]{col, row});
            }
        }
        
        
        // Draw browser contents
        // Screen is 54x20
        // width = 50
        // height = 12 (20 - 3 borders - 5 bot pane)
        // name spans 20 col, number spans 5
        // 26 entries per page
        int horizontalBorderBuffer = 0;
        int bottomPanelBorder = SCREEN_HEIGHT - ((SCREEN_HEIGHT)/4) - 1;
        int startRow = horizontalBorderBuffer + 2;
        int endRow = bottomPanelBorder - 1;
        int numElementsPerPage = 2 * (endRow - startRow + 1);
        int numElementsPerCol = numElementsPerPage/2;
        int numPages = (players.size()/(numElementsPerPage)) + 1; //for ceiling div
        String sortedByNum = "Num. Games - (t)oggle sort";
        String sortedAlphabetic = "Alphabetic - (t)oggle sort";
        int currentPageInd = 0;
        int currentElement = 0;
        boolean numericSort = true;
        String activeTitle = sortedByNum;
        while(true){
            textGraphics.putString(MID_COL - (activeTitle.length()/2), 1, activeTitle);

            // Fill the menu with players and counts
            int counter = 0;
            int endElementInd = ((currentPageInd + 1) * numElementsPerPage) >= players.size() ? players.size() : ((currentPageInd + 1) * numElementsPerPage);
            for (Map.Entry<String, Integer> me : players.subList(currentPageInd * numElementsPerPage, endElementInd)){ // End index is exclusive
                int c = entryPositions.get(counter)[0];
                int r = entryPositions.get(counter)[1];
                
                String player = me.getKey();
                int numGames = me.getValue();
                
                if (player.length() > 20){
                    player = player.substring(0, 16) + "...";
                }
                
                if (counter == currentElement) {
                    highlightGraphics.putString(c, r, player);
                    highlightGraphics.putString(c + 20, r, String.valueOf(numGames));
                }
                else {
                    textGraphics.putString(c, r, player);
                    textGraphics.putString(c + 20, r, String.valueOf(numGames));
                }
                counter++;
            }
            String pageCounter = String.valueOf(currentPageInd + 1) + "/" + String.valueOf(numPages);
            textGraphics.putString(MID_COL - (pageCounter.length()/2), 15, pageCounter);
            
            drawStaticBrowserElements(textGraphics, terminal);
            safeFlush(terminal);

            switch (queryBrowserInput(textGraphics, errorGraphics, terminal)){
                // Exit
                case 0 :{
                    safeClear(terminal);
                    return 0;
                }
                // Scroll left
                case 1: {
                    if (currentElement >= numElementsPerCol) currentElement -= numElementsPerCol;
                    break;
                }
                // Scroll right
                case 2: {
                    // If last page
                    if (currentPageInd == (numPages - 1)){
                        if ((currentPageInd + numElementsPerCol) < (players.size() % numElementsPerPage)){
                            currentElement += numElementsPerCol;
                        }
                    }
                    // If not last page
                    else if (currentElement < numElementsPerCol){
                        currentElement += numElementsPerCol;
                    } 
                    break;
                }
                // Scroll up
                case 3: {
                    if ((currentElement % numElementsPerCol) > 0)currentElement--;
                    break;
                }
                // Scroll down
                case 4: {
                    if (currentPageInd == numPages - 1){
                        if (!(currentElement == (players.size() % numElementsPerPage) - 1)){
                            currentElement++;
                        }
                    
                    }
                    else if ((currentElement % numElementsPerCol) != (numElementsPerCol - 1))
                    {
                        currentElement++;
                    } 
                    break;
                }
                // Toggle sorting
                case 5: {
                    safeClear(terminal);
                    currentPageInd = 0;
                    currentElement = 0;
                    if (numericSort){
                        players.sort(Map.Entry.comparingByKey());
                        numericSort = false;
                        activeTitle = sortedAlphabetic;
                    }
                    else {
                        players.sort(Map.Entry.comparingByValue());
                        Collections.reverse(players);
                        numericSort = true;
                        activeTitle = sortedByNum;
                    }
                    break;
                }
                // Prev.Page
                case 6: {
                    if (currentPageInd > 0) currentPageInd--;
                    safeClear(terminal);
                    currentElement = 0;
                    break;
                }
                // Next Page
                case 7: {
                    if (currentPageInd < numPages) currentPageInd++;
                    safeClear(terminal);
                    currentElement = 0;
                    break;
                }
                // First page
                case 8: {
                    safeClear(terminal);
                    currentPageInd = 0;
                    break;
                }
                // Last page
                case 9: {
                    safeClear(terminal);
                    currentPageInd = numPages - 1;
                    break;
                }
                case 10: {
                    safeClear(terminal);
                    playerGameLoop(textGraphics, errorGraphics, highlightGraphics, terminal, "Nakamura,Hi");
                    currentPageInd = 0;
                    currentElement = 0;
                    break;
                }
            }
        }

    }

    // 0 - escape
    // 1 - left arrow
    // 2 - right arrow
    // 3 - up arrow
    // 4 - down arrow
    // 5 - toggle order
    // 6 - previous page
    // 7 - next page
    // 8 - first page
    // 9 - last page
    // 10 - enter
    public static  int queryBrowserInput(TextGraphics textGraphics, TextGraphics errorGraphics, Terminal terminal){
        while(true){
            try {
                KeyStroke input = terminal.readInput();
                KeyType type = input.getKeyType();

                if (type == KeyType.Escape){
                    return 0;
                }
                else if (type == KeyType.ArrowLeft){
                    return 1;
                }
                else if (type == KeyType.ArrowRight){
                    return 2;
                }
                else if (type == KeyType.ArrowUp){
                    return 3;
                }
                else if (type == KeyType.ArrowDown){
                    return 4;
                }
                else if (type == KeyType.Character){
                    char inChar = input.getCharacter();
                    if (inChar == 't'){
                        return 5;
                    }
                    else if (inChar == 'q'){
                        return 6;
                    }
                    else if (inChar == 'e'){
                        return 7;
                    }
                    else if (inChar == 'f'){
                        return 8;
                    }
                    else if (inChar == 'l'){
                        return 9;
                    }
                }
                else if(type == KeyType.Enter){
                    return 10;
                }
                else {
                    errorGraphics.putString(ERROR_COL, ERROR_ROW, "Invalid input, try again.");
                    terminal.flush();
                    
                }
            }
            catch (IOException e){
                System.out.println("queryReplayInput() Error: Exception occurred " + e);
            }

        } 
    }

    public static void safeClear(Terminal terminal){
        try{
            terminal.clearScreen();
        }
        catch (IOException e){
            System.out.println("safeClear(): Exception occurred" + e);
        }
    }

    public static void drawLoadingScreen(TextGraphics textGraphics, Terminal terminal){
        String s0 = "  _                     _ _                   ";
        String s1 = " | |                   | (_)                  ";
        String s2 = " | |     ___   __ _  __| |_ _ __   __ _       ";
        String s3 = " | |    / _ \\ / _` |/ _` | | '_ \\ / _` |      ";
        String s4 = " | |___| (_) | (_| | (_| | | | | | (_| |_ _ _ ";
        String s5 = " |______\\___/ \\__,_|\\__,_|_|_| |_|\\__, (_|_|_)";
        String s6 = "                                   __/ |      ";
        String s7 = "                                  |___/       ";

        String[] loading = new String[]{s0, s1, s2, s3, s4, s5, s6, s7};

        int i = 0;
        for (String line : loading){
            textGraphics.putString(MID_COL - (s4.length()/2), 5 + i, line);
            i++;
        }

        safeFlush(terminal);
    }

    public static void playerGameLoop(TextGraphics textGraphics, TextGraphics errorGraphics, TextGraphics highlightGraphics, Terminal terminal, String player){
        drawLoadingScreen(textGraphics, terminal);
        ArrayList<String[]> games = RegexDatabase.readDBPlayer(player);
        
        int leftEdgeInd = 2;
        int rightEdgeInd = SCREEN_WIDTH - 2 - 1;
        int topRowInd = 2;
        int botRowInd = 13;
        int titleRow = topRowInd - 1;
        
        int displayHeight = (botRowInd + 1) - topRowInd;
        
        int displayWidth = rightEdgeInd - leftEdgeInd; // 54 - (2 x 2 edge border) - 5 cols 4 dividers -> 46
        int dateColWidth = 10;
        int resColWidth = 7;
        int playerColWidth = 11;
        int eventColWidth = 7;
        
        int eventCol = leftEdgeInd;
        int dateCol = eventCol + eventColWidth + 1;
        int whiteCol = dateCol + dateColWidth + 1;
        int blackCol = whiteCol + playerColWidth + 1;
        int resCol = blackCol + playerColWidth + 1;
        
        int[] colIndices = new int[]{eventCol, dateCol, whiteCol, blackCol, resCol};
        int[] colWidths = new int[]{eventColWidth, dateColWidth, playerColWidth, playerColWidth, resColWidth};
        
        String eventTitle = "EVENT";
        String whiteTitle = "WHITE";
        String blackTitle = "BLACK";
        String dateTitle = "DATE";
        String resTitle = "RESULT";

        int numPages = (games.size() + displayHeight - 1) / displayHeight;
        int currentElementInd = 0;
        int currentPageInd = 0;

        // System.out.println("displayHeight = " + displayHeight);
        // System.out.println("displayWidth = " + displayWidth);
        // System.out.println("dateColWidth = " + dateColWidth);
        // System.out.println("resColWidth = " + resColWidth);
        // System.out.println("playerColWidth = " + playerColWidth);
        // System.out.println("eventColWidth = " + eventColWidth);

        // System.out.println("eventCol = " + eventCol);
        // System.out.println("dateCol = " + dateCol);
        // System.out.println("whiteCol = " + whiteCol);
        // System.out.println("blackCol = " + blackCol);
        // System.out.println("resCol = " + resCol);
                
        safeClear(terminal);
        while (true){
            int endIndex = ((currentPageInd + 1) * displayHeight) > games.size() ? games.size() : (currentPageInd + 1) * displayHeight;

            List<String[]> subList = games.subList(currentPageInd * displayHeight, endIndex);

            String pageCounter = String.valueOf(currentPageInd + 1) + "/" + String.valueOf(numPages);
            drawStaticBrowserElements(textGraphics, terminal);
            // Write titles
            textGraphics.putString(eventCol + (eventColWidth/2) - (eventTitle.length()/2), titleRow, eventTitle);
            textGraphics.putString(dateCol + (dateColWidth/2) - (dateTitle.length()/2), titleRow, dateTitle);
            textGraphics.putString(whiteCol + (playerColWidth/2) - (whiteTitle.length()/2), titleRow, whiteTitle);
            textGraphics.putString(blackCol + (playerColWidth/2) - (blackTitle.length()/2), titleRow, blackTitle);
            textGraphics.putString(resCol + (resColWidth/2) - (resTitle.length()/2), titleRow, resTitle);

            textGraphics.putString(MID_COL - pageCounter.length()/2, botRowInd + 2, pageCounter);

            for (int i = 0; i < subList.size(); i++){
                String[] game = subList.get(i);
                for (int j = 0; j < 5; j++){
                    // Truncate length to available width
                    String currentString = game[j];
                    if (currentString.length() >= colWidths[j]){
                        currentString = currentString.substring(0, colWidths[j]);
                    }

                    // Write to terminal using appropriate text graphics
                    if (currentElementInd == i){
                        highlightGraphics.putString(colIndices[j], i + topRowInd, currentString);
                    }
                    else{
                        textGraphics.putString(colIndices[j], i + topRowInd, currentString);
                    }
                }
            }
            
            
            safeFlush(terminal);

            // 0 - escape
            // 1 - left arrow
            // 2 - right arrow
            // 3 - up arrow
            // 4 - down arrow
            // 5 - toggle order
            // 6 - previous page
            // 7 - next page
            // 8 - first page
            // 9 - last page
            // 10 - enter
            switch (queryBrowserInput(textGraphics, errorGraphics, terminal)){
                case 0:{
                    return;
                }
                // Skip left + right arrow cases
                case 3:{
                    if (currentElementInd > 0) currentElementInd--;
                    break;
                }
                case 4: {
                    if ((currentElementInd < displayHeight) && (currentElementInd < subList.size() - 1)) currentElementInd++; // displayHeight == elements per page
                    break;
                }
                case 6: {
                    if (currentPageInd > 0) {
                        currentPageInd--;
                        currentElementInd = 0;
                    }
                    safeClear(terminal);
                    break;
                }
                case 7: {
                    if (currentPageInd < (numPages - 1)) {
                        currentPageInd++;
                        currentElementInd = 0;
                    }
                    safeClear(terminal);
                    break;
                }
                case 8: {
                    currentPageInd = 0;
                    currentElementInd = 0;
                    safeClear(terminal);
                    break;
                }
                case 9: {
                    currentPageInd = numPages - 1;
                    currentElementInd = 0;
                    safeClear(terminal);
                    break;
                }
                case 10: {
                    break;
                }
            }

        }
        
    }

}