package parser;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import engine.Board;
import engine.Move;
import engine.Move.MOVE_TYPE;

public class RegexParser {
    
    public static ArrayList<Move> moveValidator(String movesString){
        //#region Map initialization
        Map<String, Integer> PIECE_ID = new HashMap<>();
        PIECE_ID.put(null, 1);
        PIECE_ID.put("N", 2);
        PIECE_ID.put("B", 3);
        PIECE_ID.put("R", 4);
        PIECE_ID.put("Q", 5);
        PIECE_ID.put("K", 6);

        Map<String, Integer> FILE_INDEX = new HashMap<>();
        FILE_INDEX.put("a", 0);
        FILE_INDEX.put("b", 1);
        FILE_INDEX.put("c", 2);
        FILE_INDEX.put("d", 3);
        FILE_INDEX.put("e", 4);
        FILE_INDEX.put("f", 5);
        FILE_INDEX.put("g", 6);
        FILE_INDEX.put("h", 7);
        //#endregion

        ArrayList<Move> retArray = new ArrayList<>();

        //#region Regex initialization
        // Group 0: Full move
        // Group 1: Piece identifier, if null then pawn
        // Group 2: Optional rank disambig.
        // Group 3: Optional file disambig.
        // Group 4: Optional capture
        // Group 5: Destination rank
        // Group 6: Destination file
        // Group 7: Optional promotion indicator
        // Group 8: Optional Check or Mate
        // Group 9: Short/Kingside castle
        // Group 10: Long/Queenside castle

        String regex = "([KQRBN])?([a-hA-H])?([1-8])?(x)?([a-hA-H])([1-8])(=[KQRBN])?([+#])?|(O-O-O)|(O-O)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(movesString);
        //#endregion

        // Initial board state
        Board board = new Board();
        ArrayList<int[]> validMoves = board.generateValidMoves(1);

        //#region Move Inference
        int count = 0;
        while(matcher.find()){
            boolean whitesTurn = (count % 2 == 0) ? true : false;
            int turnInt = whitesTurn ? 1 : -1;
            int piece;
            int origin;
            int destination;
            MOVE_TYPE moveType;
            int promotionPiece = 0;
            
            // Short castle
            String debugMatch = matcher.group(0);
            if (matcher.group(0).equals("O-O")){
                if (whitesTurn){
                    piece = 6;
                    origin = 4;
                    destination = 6;
                }
                else {
                    piece = -6;
                    origin = 60;
                    destination = 62;
                }

                int[] temp = new int[]{piece, origin, destination};

                if (moveInMoveset(temp, validMoves)){
                    retArray.add(new Move(piece, origin, destination, MOVE_TYPE.CASTLE_SHORT));
                }
                else {
                    System.out.println("moveValidator(): Invalid move detected for index " + count + ", returning all valid moves until this point.");
                    return retArray;
                }
            }
            // Long castle
            else if (matcher.group(0).equals("O-O-O")){
            
                if (whitesTurn){
                    piece = 6;
                    origin = 4;
                    destination = 2;
                }
                else {
                    piece = -6;
                    origin = 60;
                    destination = 58;
                }

                int[] temp = new int[]{piece, origin, destination};

                if (moveInMoveset(temp, validMoves)){
                    retArray.add(new Move(piece, origin, destination, MOVE_TYPE.CASTLE_LONG));
                }
                else {
                    System.out.println("moveValidator(): Invalid move detected for index " + count + ", returning all valid moves until this point.");
                    return retArray;
                }
            }
            // All other moves
            else{
                //#region PieceID, Destination square, Move Type detection
                // Determine piece ID from string identifier
                // Throws exception if identifier is not found as primitive int cannot be Null
                try{
                    piece = PIECE_ID.get(matcher.group(1));
                }
                catch (NullPointerException e){
                    System.out.println("moveValidator(): Invalid piece identifier detected for index " + count + ", returning validated moves");
                    return retArray;
                }
                // Consider turn for piece
                if (!whitesTurn){
                    piece *= -1;
                }

                // Get destination
                destination = ((Integer.valueOf(matcher.group(6)) - 1) * 8) + FILE_INDEX.get(matcher.group(5)); // Subtract 1 as file index = file lablel - 1

                // Determine move type
                // Promotion?
                if (matcher.group(7) != null){
                    //Capture?
                    if (matcher.group(4) != null){
                        moveType = MOVE_TYPE.PROMOTE_ATTACK;
                    }
                    else {
                        moveType = MOVE_TYPE.PROMOTE_MOVE;
                    }
                    promotionPiece = PIECE_ID.get(String.valueOf(matcher.group(7).charAt(1))) * turnInt;
                }
                // Not promotion, but capture?
                else if (matcher.group(4) != null){
                    // If Pawn capture on an empty square
                    if ((Math.abs(piece) == 1) && (board.getBoard()[destination/8][destination % 8] == 0)){
                        moveType = MOVE_TYPE.EN_PASSENT;
                    }
                    else {
                        moveType = MOVE_TYPE.ATTACK;
                    }
                }
                else {
                    moveType = MOVE_TYPE.MOVE;
                }
                //#endregion

                //#region Move validation
                // No disambig
                if (matcher.group(2) == null && matcher.group(3) == null){
                    int[] temp = getMoveInMoveset(piece, destination, validMoves);

                    if (temp.length > 0){
                        retArray.add(new Move(temp[0],temp[1], temp[2], moveType));
                    }
                    else {
                        System.out.println("moveValidator(): No valid move match found for index " + count);
                        return retArray;
                    }
                }
                // Rank disambig
                else if (matcher.group(3) != null){
                    int[] temp = getMoveInMoveset(piece, destination, validMoves, Integer.valueOf(matcher.group(3)) - 1, -1); // No relationship between the sole -1 flag vs current[2] - 1

                    if (temp.length > 1){
                        retArray.add(new Move(temp[0], temp[1], temp[2], moveType));
                    }
                    else {
                        System.out.println("moveValidator(): No valid move match found for index " + count);
                        return retArray;
                    }
                }
                // File disambig
                else if (matcher.group(2) != null){
                    int[] temp = getMoveInMoveset(piece, destination, validMoves, -1, FILE_INDEX.get(matcher.group(2)));

                    if (temp.length > 0){
                        retArray.add(new Move(temp[0], temp[1], temp[2], moveType));
                    }
                    else {
                        System.out.println("moveValidator(): No valid move match found for index " + count);
                        return retArray;
                    }
                }
                // Rank and File disambig
                else {
                    int [] temp = getMoveInMoveset(piece, destination, validMoves, Integer.valueOf(matcher.group(3)) - 1, Integer.valueOf(matcher.group(2)));
                    
                    if (temp.length > 0){
                        retArray.add(new Move(temp[0], temp[1], temp[2], moveType));
                    }
                    else {
                        System.out.println("moveValidator(): No valid move match found for index " + count);
                        return retArray;
                    }
                }
                //#endregion
            }

            // Add promotion piece if the move is a promotion, promotion piece is not a factor for the move validity
            Move lastMove = retArray.get(retArray.size() - 1);
            if (lastMove.getType() == MOVE_TYPE.PROMOTE_ATTACK || lastMove.getType() == MOVE_TYPE.PROMOTE_MOVE){
                lastMove.setPromotionPiece(promotionPiece);
            }

            //#region Update validator board
            // Play the last move and update state
            board.playMove(lastMove);
            board.updateState(turnInt);
            validMoves = board.generateValidMoves(turnInt * -1);

            // if (validMoves.size() == 0){
            //     System.out.println("moveValidator(): No more valid moves left");
            //     return retArray;
            // }
            //#endregion

            count++;
        }
        //#endregion

        System.out.println("moveValidator(): All moves validated!");
        return retArray;
    }

    public static boolean moveInMoveset(int[] move, ArrayList<int[]> moveset){
        for (int[] mv : moveset){
            if (Arrays.equals(move, mv)){
                return true;
            }
        }
        return false;
    }

    public static int[] getMoveInMoveset(int piece, int dest, ArrayList<int[]> moveset){
        int count = 0;
        int[] targetMove = new int[]{};
        for (int [] mv : moveset){
            if (piece == mv[0] && dest == mv[2]){
                targetMove = mv;
                count++;
            }
        }

        if (count == 1){
            return targetMove;
        }
        else {
            System.out.println("getMoveInMoveset(): Multiple moves detected, returning empty array");
            return new int[]{};
        }
    }

    public static int[] getMoveInMoveset(int piece, int dest, ArrayList<int[]> moveset, int dsRank, int dsFile){
        // boardState checking required for EP moves, in addition to basic validity
        // Files (columns)
        int[] fileA = {0, 8, 16, 24, 32, 40, 48, 56};
        int[] fileB = {1, 9, 17, 25, 33, 41, 49, 57};
        int[] fileC = {2, 10, 18, 26, 34, 42, 50, 58};
        int[] fileD = {3, 11, 19, 27, 35, 43, 51, 59};
        int[] fileE = {4, 12, 20, 28, 36, 44, 52, 60};
        int[] fileF = {5, 13, 21, 29, 37, 45, 53, 61};
        int[] fileG = {6, 14, 22, 30, 38, 46, 54, 62};
        int[] fileH = {7, 15, 23, 31, 39, 47, 55, 63};

        int[][] files = new int[][] {
            fileA, fileB, fileC, fileD, fileE, fileF, fileG, fileH
        };

        // Ranks (rows)
        int[] rank1 = {0, 1, 2, 3, 4, 5, 6, 7};
        int[] rank2 = {8, 9, 10, 11, 12, 13, 14, 15};
        int[] rank3 = {16, 17, 18, 19, 20, 21, 22, 23};
        int[] rank4 = {24, 25, 26, 27, 28, 29, 30, 31};
        int[] rank5 = {32, 33, 34, 35, 36, 37, 38, 39};
        int[] rank6 = {40, 41, 42, 43, 44, 45, 46, 47};
        int[] rank7 = {48, 49, 50, 51, 52, 53, 54, 55};
        int[] rank8 = {56, 57, 58, 59, 60, 61, 62, 63};

        int[][] ranks = new int[][]{
            rank1, rank2, rank3, rank4, rank5, rank6, rank7, rank8
        };
        
        int[] emptyMove = new int[]{};

        if (dsRank >= 0 && dsFile >= 0){
            int origin = (dsRank * 8) + dsFile;
            int[] temp = new int[]{piece, origin, dest};

            for (int[] mv : moveset){
                if (Arrays.equals(temp, mv)){
                    return mv;
                }
            }
            return emptyMove;
        }
        else if (dsRank >= 0){
            for (int rankInd : ranks[dsRank]){
                for (int[] mv : moveset){
                    if ((piece == mv[0]) && (rankInd == mv[1]) && (dest == mv[2])){
                        return mv;
                    }
                }
            }
            return emptyMove;
        }
        else if (dsFile >= 0){
            for (int fileInd : files[dsFile]){
                for (int[] mv : moveset){
                    if ((piece == mv[0]) && (fileInd == mv[1]) && (dest == mv[2])){
                        return mv;
                    }
                }
            }
            return emptyMove;
        }

        return emptyMove;
    }

    public static ArrayList<String[]> extractPGN(String filepath){
        ArrayList<String[]> retArray = new ArrayList<>();
        ArrayList<String> metaArray = new ArrayList<>();
        ArrayList<String> movesArray = new ArrayList<>();

        String pgnString;
        StringBuilder sb = new StringBuilder();

        try(BufferedReader bf = new BufferedReader(new FileReader(filepath))){

            while (bf.ready()){
                sb.append(bf.readLine() + "\n");
            }

        }
        catch (Exception e){
            System.out.println("extractMetaPGN(): Error reading file");
        }

        pgnString = sb.toString();

        String metaRegex = "((?:\\n?\\[.*\\]\\n)+)";
        String movesRegex = "(?s)\\n1\\..*?[ ]+[01][.\\/]?\\d?-[01][.\\/]?\\d?";
        Pattern metaPattern = Pattern.compile(metaRegex);
        Pattern movesPattern = Pattern.compile(movesRegex);
        Matcher metaMatcher = metaPattern.matcher(pgnString);
        Matcher movesMatcher = movesPattern.matcher(pgnString);

        while (metaMatcher.find()){
            metaArray.add(metaMatcher.group());
        }

        while (movesMatcher.find()){
            movesArray.add(movesMatcher.group());
        }

        if (metaArray.size() != movesArray.size()){
            System.out.println("extractPGN() Error: Number of metadata and moves blocks do not match, returning empty array");
            return retArray;
        }

        for (int i = 0; i < metaArray.size(); i++){
            retArray.add(new String[]{metaArray.get(i), movesArray.get(i)});
        }

        return retArray;
    }

    public static void moveValidatorLogger(String filePath, String logPath){
        System.out.println("moveValidatorLogger(): Beginning logging...");
        long startTime = System.currentTimeMillis();
        PrintStream origOut = System.out;
        int count = 0;

        try (PrintStream fileOut = new PrintStream(new FileOutputStream(logPath))){
            System.setOut(fileOut);

            ArrayList<String[]> temp = extractPGN(filePath);

            for (String[] tmv : temp){
                System.out.println("Count " + count);
                ArrayList<Move> tempMove = moveValidator(tmv[1]);
                System.out.println(tempMove.size() + " moves present");
                count ++;
            }

        }
        catch (Exception e){
            System.out.println("moveValidatorLogger() Error: " + e);
        }
        finally {
            System.setOut(origOut);
        }

        long endTime = System.currentTimeMillis();
        float difference = (endTime - startTime)/1000 ;

        String duration = String.format("%.1f", difference);

        System.out.println("moveValidatorLogger(): Logging done!");
        System.out.println("Logged " + (count + 1) + " games in " + duration + " seconds.");
    }

}
