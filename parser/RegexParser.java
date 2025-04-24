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
import exceptions.AlgebraicParseException;
import db.RegexGameData;

public class RegexParser {
    public static Map<String, Integer> PIECE_ID;
    public static Map<String, Integer> FILE_INDEX;
    public static Map<Integer, String> FILE_LABEL;

    public static String ALGEBRAIC_REGEX = "([KQRBN])?([a-h])?([1-8])?(x)?([a-h])([1-8])(=[QRBN])?([+#])?|(O-O-O)|(O-O)";
    public static String META_BLOCK_REGEX = "((?:\\n?\\[.*\\]\\n)+)";
    public static String MOVE_BLOCK_REGEX = "(?s)\\n1\\..*?[ ]+[01][.\\/]?\\d?-[01][.\\/]?\\d?";
    public static String META_FIELD_REGEX = "\n?\\[(.*) \"(.*)\"\\]";
    public static String GAME_BLOCK_REGEX = "(?s)(.*? +[01][.\\/]?\\d?-[01][.\\/]?\\d?)";

    static {
        PIECE_ID = new HashMap<>();
        PIECE_ID.put(null, 1);
        PIECE_ID.put("N", 2);
        PIECE_ID.put("B", 3);
        PIECE_ID.put("R", 4);
        PIECE_ID.put("Q", 5);
        PIECE_ID.put("K", 6);


        FILE_INDEX = new HashMap<>();
        FILE_INDEX.put("a", 0);
        FILE_INDEX.put("b", 1);
        FILE_INDEX.put("c", 2);
        FILE_INDEX.put("d", 3);
        FILE_INDEX.put("e", 4);
        FILE_INDEX.put("f", 5);
        FILE_INDEX.put("g", 6);
        FILE_INDEX.put("h", 7);

        FILE_LABEL = new HashMap<>();
        FILE_LABEL.put(0, "a");
        FILE_LABEL.put(1, "b");
        FILE_LABEL.put(2, "c");
        FILE_LABEL.put(3, "d");
        FILE_LABEL.put(4, "e");
        FILE_LABEL.put(5, "f");
        FILE_LABEL.put(6, "g");
        FILE_LABEL.put(7, "h");
    }
    
    public static ArrayList<Move> PGNMoveValidator(String movesString){
        //#region Map initialization
        // Map<String, Integer> PIECE_ID = new HashMap<>();
        // PIECE_ID.put(null, 1);
        // PIECE_ID.put("N", 2);
        // PIECE_ID.put("B", 3);
        // PIECE_ID.put("R", 4);
        // PIECE_ID.put("Q", 5);
        // PIECE_ID.put("K", 6);

        // Map<String, Integer> FILE_INDEX = new HashMap<>();
        // FILE_INDEX.put("a", 0);
        // FILE_INDEX.put("b", 1);
        // FILE_INDEX.put("c", 2);
        // FILE_INDEX.put("d", 3);
        // FILE_INDEX.put("e", 4);
        // FILE_INDEX.put("f", 5);
        // FILE_INDEX.put("g", 6);
        // FILE_INDEX.put("h", 7);
        //#endregion

        ArrayList<Move> retArray = new ArrayList<>();

        //#region Regex initialization
        // Group 0: Full move
        // Group 1: Piece identifier, if null then pawn
        // Group 2: Optional File disambig.
        // Group 3: Optional Rank disambig.
        // Group 4: Optional capture
        // Group 5: Destination File
        // Group 6: Destination Rank
        // Group 7: Optional promotion indicator
        // Group 8: Optional Check or Mate
        // Group 9: Short/Kingside castle
        // Group 10: Long/Queenside castle

        String regex = RegexParser.ALGEBRAIC_REGEX;
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
            board.updateState(turnInt, validMoves);
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
        else if (count == 0){
            System.out.println("getMoveInMoveset(): No matching moves found");
            return new int[]{};
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
        
        // All control paths should somewhere return a value but if not this is returned, which indicates something went wrong
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

    // Separates games first, then separates meta block from moves block, then separates each meta field and each move
    // May be easier to skip the meta/move block regex, and directly get the meta fields and moves from the game block
    public static ArrayList<RegexGameData> extractPGN(String filepath){
        ArrayList<RegexGameData> retArray = new ArrayList<>();
        ArrayList<String> metaArray = new ArrayList<>();
        ArrayList<String> movesArray = new ArrayList<>();

        String pgnString;
        StringBuilder sb = new StringBuilder();

        System.out.println("extractPGN(): Beginning PGN extraction from " + filepath);
        long startTime = System.currentTimeMillis();

        try(BufferedReader bf = new BufferedReader(new FileReader(filepath))){

            while (bf.ready()){
                sb.append(bf.readLine() + "\n");
            }

        }
        catch (Exception e){
            System.out.println("extractMetaPGN(): Error reading file");
        }

        pgnString = sb.toString();

        // Separate games
        Pattern gameBlockPattern = Pattern.compile(RegexParser.GAME_BLOCK_REGEX);

        // Separate metadata from moves
        Pattern metaBlockPattern = Pattern.compile(RegexParser.META_BLOCK_REGEX);
        Pattern movesBlockPattern = Pattern.compile(RegexParser.MOVE_BLOCK_REGEX);

        // Read metadata and moves
        Pattern algebraicPattern = Pattern.compile(RegexParser.ALGEBRAIC_REGEX);
        Pattern metaFieldPattern = Pattern.compile(RegexParser.META_FIELD_REGEX);

        Matcher gameBlockMatcher = gameBlockPattern.matcher(pgnString);
        Matcher metaBlockMatcher;
        Matcher movesBlockMatcher;
        Matcher metaFieldMatcher;
        Matcher algebraicMatcher;

        HashMap<String, String> tempMetaMap;
        ArrayList<String> tempMoveArray;
        // Separate each "game" in the PGN
        while (gameBlockMatcher.find()){
            tempMetaMap = new HashMap<String, String>();
            tempMoveArray = new ArrayList<String>();
            
            // Separate the meta block in each game
            metaBlockMatcher = metaBlockPattern.matcher(gameBlockMatcher.group());
            while (metaBlockMatcher.find()){
                metaFieldMatcher = metaFieldPattern.matcher(metaBlockMatcher.group());
                
                // Populate meta fields
                while (metaFieldMatcher.find()){
                    tempMetaMap.put(metaFieldMatcher.group(1), metaFieldMatcher.group(2));
                }
            }
            
            // Separate the moves block from each game
            movesBlockMatcher = movesBlockPattern.matcher(gameBlockMatcher.group());
            while (movesBlockMatcher.find()){
                algebraicMatcher = algebraicPattern.matcher(movesBlockMatcher.group());
                // Extract moves sequence
                while (algebraicMatcher.find()){
                    tempMoveArray.add(algebraicMatcher.group());
                }
            }

            // Add game
            retArray.add(new RegexGameData(tempMetaMap, tempMoveArray));
        }
        String timeString = String.format("%1.3f", (float)(System.currentTimeMillis() - startTime)/1000);
        System.out.println("extractPGN(): Finished extracting " + retArray.size() + " games from " + filepath + " in " + timeString + " seconds.");

        return retArray;
    }

    public static Move validateMove(String algebraicMove, Board board) throws AlgebraicParseException{
        // Regex setup
        String moveRegex = RegexParser.ALGEBRAIC_REGEX;
        Pattern movePattern = Pattern.compile(moveRegex);
        Matcher moveMatcher = movePattern.matcher(algebraicMove);

        // Invalid input detection
        if (algebraicMove.length() < 2 || algebraicMove.length() > 7){
            throw new AlgebraicParseException("validateMove() Error: Invalid length move provided");
        }
        
        if (moveMatcher.find()){
            if (// Determines if mandatory destination file and rank are present, takes into account castling
                !(moveMatcher.group().equals("O-O") || moveMatcher.group().equals("O-O-O")) && 
                (moveMatcher.group(5) == null || moveMatcher.group(6) == null)
                ){
                throw new AlgebraicParseException("validateMove() Error: Destination square not found for match " + moveMatcher.group());
            }
            // If the whole move in algebraic notation doesn't match
            else if (!moveMatcher.group().equals(algebraicMove)){
                throw new AlgebraicParseException("validateMove() Error: Invalid algebraic format");
            }
        }
        else {
            throw new AlgebraicParseException("validateMove() Error: No move match found");
        }

        // If castling, we know all information, return valid MOVE

        // If not castling, determine move:
        int turnInt = board.getTurnInt();
        int origin;
        int piece;
        Integer dsFile = null;
        Integer dsRank = null;
        Integer promotionPiece = null;
        int destRank;
        int destFile;
        MOVE_TYPE moveType;
        
        if (moveMatcher.group().equals("O-O")){
            if (turnInt > 0){
                piece = 6;
                destRank = 0;
            }
            else {
                piece = -6;
                destRank = 7;
            }

            destFile = 6;
            moveType = MOVE_TYPE.CASTLE_SHORT;
        }
        else if (moveMatcher.group().equals("O-O-O")){
            if (turnInt > 0){
                piece = 6;
                destRank = 0;
            }
            else {
                piece = -6;
                destRank = 7;
            }
            
            destFile = 2;
            moveType = MOVE_TYPE.CASTLE_LONG;
        }
        else {
            // Determine Piece
            piece = turnInt > 0 ? PIECE_ID.get(moveMatcher.group(1)) : PIECE_ID.get(moveMatcher.group(1)) * -1;
    
            // Destination rank and file
            destRank = Integer.valueOf(moveMatcher.group(6)) - 1;
            destFile = FILE_INDEX.get(moveMatcher.group(5));
    
            // Disambig
            if (moveMatcher.group(3) != null){
                dsRank = Integer.valueOf(moveMatcher.group(3)) - 1;
            }
            if (moveMatcher.group(2) != null){
                dsFile = FILE_INDEX.get(moveMatcher.group(2));
            }
    
            // Move type determination
            // Group 4 captures the presence of 'x' ie exf4
            if (moveMatcher.group(4) != null){
                // If capturing an unoccupied square
                if (!board.squareIsOccupied((destRank * 8) + destFile)){
                    moveType = MOVE_TYPE.EN_PASSENT;
                }
                else if (moveMatcher.group(7) != null){
                    moveType = MOVE_TYPE.PROMOTE_ATTACK;
                    promotionPiece = PIECE_ID.get(String.valueOf(moveMatcher.group(7).charAt(1))) * turnInt;

                    if (promotionPiece == null || Math.abs(promotionPiece) > 5){
                        throw new AlgebraicParseException("validateMove() Error: Invalid promotion piece");
                    }
                }
                else {
                    moveType = MOVE_TYPE.ATTACK;
                }
            }
            // If there is no 'x' capture identifier
            else {
                // Group 7 is "=[BNRQ]", move promotion of pawn
                if (moveMatcher.group(7) != null && Math.abs(piece) == 1){
                    moveType = MOVE_TYPE.PROMOTE_MOVE;
                    promotionPiece = PIECE_ID.get(String.valueOf(moveMatcher.group(7).charAt(1))) * turnInt;

                    if (promotionPiece == null || Math.abs(promotionPiece) > 5){
                        throw new AlgebraicParseException("validateMove() Error: Invalid promotion piece");
                    }
                }
                else {
                    if (board.getBoard()[destRank][destFile] == 0){ // If destination square has no occupants
                        moveType = MOVE_TYPE.MOVE;
                    }
                    else {
                        throw new AlgebraicParseException("validateMove() Error: Move to occupied square");
                    }
                }
            }
        }

        ArrayList<int[]> validMoves = board.generateValidMoves(turnInt);
        // Now find the origin and appropriate moves by scanning through valid generated moves, then create and return move
        int[] temp;
        Move retMove;
        if (dsRank != null && dsFile != null){
            temp = getMoveInMoveset(piece, (destRank * 8) + destFile, validMoves, dsRank, dsFile);
        }
        else if (dsRank != null){
            temp = getMoveInMoveset(piece, (destRank * 8) + destFile, validMoves, dsRank, -1);
        }
        else if (dsFile != null){
            temp = getMoveInMoveset(piece, (destRank * 8) + destFile, validMoves, -1, dsFile);
        }
        else {
            temp = getMoveInMoveset(piece, (destRank * 8) + destFile, validMoves);
        }

        if (temp.length == 0){
            throw new AlgebraicParseException("validateMove() Error: No matching valid moves found");
        }
        else {
            retMove = new Move(temp[0], temp[1], temp[2], moveType);
        }   

        if (promotionPiece != null){
            retMove.setPromotionPiece(promotionPiece);
        }
        // If promotionPiece == null
        else {
            if (
                (piece == 1 && destRank == 7) || 
                (piece == -1 && destRank == 0)
            ){
                throw new AlgebraicParseException("validateMove() Error: Pawn moves to the last rank must promote");
            }
        }

        return retMove;
    }
}
