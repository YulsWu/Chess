package engine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.concurrent.ThreadLocalRandom;

import engine.Move.MOVE_TYPE;


public class Board {
    // Any state with W_ or B_ signifies they LOST due to the reason
    // ie W_MATE means that White was Mated
    public enum BOARD_STATE {
        IN_PLAY, W_CHECK, B_CHECK, W_MATE, B_MATE, REPEAT_DRAW, MUTUAL_DRAW, STALEMATE, MATERIAL_DRAW, FIFTY_DRAW, W_RESIGN, B_RESIGN, W_TIME, B_TIME
    }

        public static final Map<Integer, String> CHESS_EMOJI = new HashMap<>(){{
        put(-6, "\u2654");
        put(-5, "\u2655");
        put(-4, "\u2656");
        put(-3, "\u2657");
        put(-2, "\u2658");
        put(-1, "\u2659");
        put(0, " ");
        put(1, "\u265F");
        put(2, "\u265E");
        put(3, "\u265D");
        put(4, "\u265C");
        put(5, "\u265B");
        put(6, "\u265A");
    }};
    // 0 - empty
    // 1 - Pawn
    // 2 - Knight
    // 3 - Bishop
    // 4 - Rook
    // 5 - Queen
    // 6 - King

    //#region Board state variables
    /*
     * Game loop:
     * - Initialize
     * - Check board state (Check, checkmate, turn, Insuff. Mat., 50 move rule, 
     * - Play move
     * - Check board state
     * - Play move, etc
     * 
     * Insufficient Material:
     * - King vs King
     * - King, Knight vs King
     * - King, Bishop vs King
     * - King, knight vs King, knight
     */
    

    private Long bitState; // Current occupancy map for the whole board
    private int[][] boardState; // True board representation
    private long[][] zobristHash; // Zobrist hash
    private ArrayDeque<Move> playedMoves;

    private boolean whitesTurn = true;
    private BOARD_STATE state = BOARD_STATE.IN_PLAY;

    public static final ArrayList<Long> W_PAWN_MOVE;
    public static final ArrayList<Long> B_PAWN_MOVE;
    public static final ArrayList<Long> W_PAWN_ATTACK;
    public static final ArrayList<Long> B_PAWN_ATTACK;
    public static final ArrayList<Long> KING_MOVE;
    public static final ArrayList<Long> KNIGHT_MOVE;
    
    public static final ArrayList<Long> HORIZONTAL_RAY;
    public static final ArrayList<Long> VERTICAL_RAY;
    public static final ArrayList<Long> DIAGONAL_MOVE;
    public static final ArrayList<Long> ANTI_MOVE;

    // Static initialization of move and ray masks
    static {
        W_PAWN_MOVE = generateWhitePawnMoveMask();
        B_PAWN_MOVE = generateBlackPawnMoveMask();
        W_PAWN_ATTACK = generateWhitePawnAttackMask();
        B_PAWN_ATTACK = generateBlackPawnAttackMask();
        KING_MOVE = generateKingMoveMask();
        KNIGHT_MOVE = generateKnightMoveMask();

        HORIZONTAL_RAY = generateHorizontalRayMask();
        VERTICAL_RAY = generateVerticalRayMask();
        DIAGONAL_MOVE = generateDiagonalRayMask();
        ANTI_MOVE = generateAntiRayMask();
    }

    public final long W_CASTLE_SHORT = 0b0000011000000000000000000000000000000000000000000000000000000000L;
    public final long B_CASTLE_SHORT = 0b0000000000000000000000000000000000000000000000000000000000000110L;
    public final long W_CASTLE_LONG = 0b0011000000000000000000000000000000000000000000000000000000000000L;
    public final long B_CASTLE_LONG = 0b0000000000000000000000000000000000000000000000000000000000110000L;
    //#endregion
    public Board(){
        // Generate fresh moves queue
        playedMoves = new ArrayDeque<>();

        // Generate new zobrist hash
        zobristHash = generateRandomZobrist();

        // Fresh bitboard
        bitState = 0xFFFF00000000FFFFL;

        // Populate freshboard
        boardState = generateFreshBoard();
    }

    //#region Base ray generation
    // Generate non-sliding piece masks
    public static ArrayList<Long> generateWhitePawnMoveMask(){
        
        ArrayList<Long> retMask = new ArrayList<>();
        
        for (int i = 0; i < 64; i++){
            int rank = i / 8;
            Long board = 0L;

            // Calculating for white pawns
            // Pawns cannot start on the first rank, have no valid moves on the last rank (Would have promoted)
            if (rank == 0 || rank == 7){
                // Do nothing, add empty board at end
            }
            else if (rank == 1) {
                int bitShiftSingle = 63 - (i + 8);
                int bitShiftDouble = 63 - (i + 16);

                board |= (1L << bitShiftSingle);
                board |= (1L << bitShiftDouble);
            }
            else {
                int bitShift = 63 - (i + 8);
                
                board |= (1L << bitShift);
            } // end if

            retMask.add(board);
        } // end for

        return retMask;
    }
    
    public static ArrayList<Long> generateWhitePawnAttackMask(){
        ArrayList<Long> retMasks = new ArrayList<>();

        for (int i = 0; i < 64; i++){
            int rank = i / 8;
            int file = i % 8;
            Long board = 0L;

            // Pawns cannot exist on rank 0 or 7 (promotion)
            if (rank == 0 || rank == 7){
                // Do nothing, add empty mask at the end
            }
            // On the left edge, file A, can only attack right
            else if (file == 0){
                int bitShift = 63 - (i + 8 + 1);
                board |= (1L << bitShift);  // One rank forward (+8), one file to the right (+1)
            }
            else if (file == 7){
                int bitShift = 63 - (i + 8 - 1); // One rank forward (+8), one file to the left (-1);
                board |= (1L << bitShift);
            }
            else {
                int bitShiftLeft = 63 - (i + 8 - 1);
                int bitShiftRight = 63 - (i + 8 + 1);
                board |= (1L << bitShiftLeft);
                board |= (1L << bitShiftRight);
            }

            retMasks.add(board);
        }
        return retMasks;
    }

    public static ArrayList<Long> generateBlackPawnMoveMask(){
        ArrayList<Long> retMasks = new ArrayList<>();

        for (int i = 0; i < 64; i++){
            int rank = i / 8;
            Long board = 0L;

            // Pawns still don't have a move on the first (promotion) or last (never exists here) rank
            if (rank == 0 || rank == 7){
                // Do nothing, empty board added at end of loop
            }
            // First Single or double pawn move
            else if (rank == 6){
                int bitShiftSingle = 63 - (i - 8);
                int bitShiftDouble = 63 - (i - 16);

                board |= (1L << bitShiftSingle);
                board |= (1L << bitShiftDouble);
            }
            else {
                int bitShift = 63 - (i - 8);
                board |= (1L << bitShift);
            }

            retMasks.add(board);
        }

        return retMasks;
    }

    public static ArrayList<Long> generateBlackPawnAttackMask(){
        ArrayList<Long> retMasks = new ArrayList<>();

        for (int i = 0; i < 64; i ++){
            int rank = i / 8;
            int file = i % 8;
            Long board = 0L;

            if (rank == 0 || rank == 7){
                // Do nothing
            }
            else if (file == 0){
                // Right attack only
                int bitShift = 63 - (i - 8 + 1);
                board |= (1L << bitShift);
            }
            else if (file == 7){
                // Left attack only
                int bitShift = 63 - (i - 8 - 1);
                board |= (1L << bitShift);
            }
            else {
                // Both attacks
                int bitShiftLeft = 63 - (i - 8 - 1);
                int bitShiftRight = 63 - (i - 8 + 1);

                board |= (1L << bitShiftLeft);
                board |= (1L << bitShiftRight);
            }

            retMasks.add(board);
        }

        return retMasks;
    }
    
    public static ArrayList<Long> generateKingMoveMask(){
        ArrayList<Long> retMasks = new ArrayList<>();
        
        for (int i = 0; i < 64; i++){
            int rank = i / 8;
            int file = i % 8;
            Long board = 0L;

            // Precomputed bit shifts
            int forwardBitShift = 63 - (i + 8);
            int rightBitShift = 63 - (i + 1);
            int forwardRightBitShift = 63 - (i + 8 + 1);
            int backBitShift = 63 - (i - 8);
            int leftBitShift = 63 - (i - 1);
            int forwardLeftBitShift = 63 - (i + 8 - 1);
            int backRightBitShift = 63 - (i - 8 + 1);
            int backLeftBitShift = 63 - (i - 8 - 1);

            // LITERAL edge cases
            // King is on rank 1
            if (rank == 0){
                if (file == 0){
                    // Only forward, right, forward-right diagonal
                    board |= ((1L << forwardBitShift) | (1L << rightBitShift) | (1L << forwardRightBitShift));
                }
                else if (file == 7){
                    // Only forward, left, forward-left diagonal
                    board |= ((1L << forwardBitShift) | (1L << leftBitShift) | (1L << forwardLeftBitShift));
                }
                else {
                    // Only forward and sidewards, and forward diagonals
                    board |= ((1L << forwardBitShift) | (1L << leftBitShift) | (1L << rightBitShift) |
                     (1L << forwardLeftBitShift) | (1L << forwardRightBitShift));
                }
            }
            // King is on rank 7
            else if (rank == 7){
                if (file == 0){
                    // Only back, right, backright diagonal
                    board |= ((1L << backBitShift) | (1L << rightBitShift) | (1L << backRightBitShift));
                }
                else if (file == 7){
                    // Only back, left, backleft diagonal
                    board |= ((1L << backBitShift) | (1L << leftBitShift) | (1L << backLeftBitShift));
                }
                else {
                    // Side to side, backwards, back side diagonals
                    board |= ((1L << backBitShift) | (1L << leftBitShift) | (1L << rightBitShift) |
                     (1L << backLeftBitShift) | (1L << backRightBitShift));
                }
            }
            // King is on ranks other than 0 or 7, still have to check for sides
            else {
                if (file == 0){
                    // Only forward, back, right, rightUpper and rightLower diagonals
                    board |= ((1L << forwardBitShift) | (1L << backBitShift) | (1L << forwardRightBitShift) |
                     (1L << backRightBitShift) | (1L << rightBitShift));
                }
                else if (file == 7){
                    // Only forward, back, left, leftUpper, and leftLower diagonals
                    board |= ((1L << forwardBitShift) | (1L << backBitShift) | (1L << forwardLeftBitShift) |
                     (1L << backLeftBitShift) | (1L << leftBitShift));
                }
                else {
                    // All other board positions not corresponding to an edge case
                    board |= ((1L << forwardBitShift) | (1L << backBitShift) | (1L << leftBitShift) |
                     (1L << rightBitShift) | (1L << forwardLeftBitShift) | (1L << forwardRightBitShift) |
                      (1L << backLeftBitShift) | (1L << backRightBitShift));
                }

            }
            retMasks.add(board);
        }

        return retMasks;
    }

    public static ArrayList<Long> generateKnightMoveMask(){
        // Knight move cases
        // Corner - Only 2 moves of opposing direction
        // On side - All of that side's moves are gone, only half the adjacent sides moves are valid
        // 1 from side - both that side's moves are invalid
        // 2 from side - All moves valid
        ArrayList<Long> retMasks = new ArrayList<>();

        for (int i = 0; i < 64; i++){
            Long board = 0L;

            int forwardLeft = 63 - (i + 16 - 1);
            int forwardRight = 63 - (i + 16 + 1);
            int rightForward = 63 - (i + 2 + 8);
            int rightBack = 63 - (i + 2 - 8);
            int backRight = 63 - (i - 16 + 1);
            int backLeft = 63 - (i - 16 - 1);
            int leftBack = 63 - (i - 2 - 8);
            int leftForward = 63 - (i - 2 + 8);

            int rank = i / 8;
            int file = i % 8;

            // If knight is on the first rank
            if (rank == 0){
                if (file == 0){
                    // Knight on A1, only two moves
                    board |= ((1L << forwardRight) | (1L << rightForward));
                }
                else if (file == 1){
                    // Knight on B1, only three moves
                    board |= ((1L << forwardLeft) | (1L << forwardRight) | (1L << rightForward));
                }
                else if (file == 7) {
                    // Knight on H1, only two moves
                    board |= ((1L << forwardLeft) | (1L << leftForward));
                }
                else if (file == 6){
                    // Knight on G1, only three moves
                    board |= ((1L << forwardLeft) | (1L << forwardRight) | (1L << leftForward));
                }
                else {
                    // Knight on first rank, only forward moves
                    board |= ((1L << forwardLeft) | (1L << forwardRight) | (1L << leftForward) | (1L << rightForward));
                }
            }
            // Knight on the last rank
            else if (rank == 7){
                if (file == 0){
                    // Knight on A8, 2 moves
                    board |= ((1L << rightBack) | (1L << backRight));
                }else if (file == 1){
                    // Knight on B8, 3 moves
                    board |= ((1L << rightBack) | (1L << backRight) | (1L << backLeft));
                }else if (file == 7){
                    // Knight on H8, 2 moves
                    board |= ((1L << leftBack) | (1L << backLeft));
                }else if (file == 6){
                    // Knight on G8, 3 moves
                    board |= ((1L << leftBack) | (1L << backLeft) | (1L << backRight));
                }else {
                    // Knight on last rank, only backwards moves
                    board |= ((1L << leftBack) | (1L << backLeft) | (1L << backRight) | (1L << rightBack));
                }
            }
            // Knight on 2nd rank
            else if (rank == 1){
                if (file == 0){
                    // Only forwardRight, rightForward, rightback
                    board |= ((1L << forwardRight) | (1L << rightForward) | (1L << rightBack));
                }
                else if (file == 1){
                    // only forwardleft, forward right, right forward, right back
                    board |= ((1L << forwardLeft) | (1L << forwardRight) | (1L << rightForward) | (1L << rightBack));

                }
                else if (file == 7){
                    // Only leftBack, leftForward, forwardLeft
                    board |= ((1L << leftBack) | (1L << leftForward) | (1L << forwardLeft));

                }
                else if (file == 6){
                    // only leftback, leftForward, forwardleft, forwardRight
                    board |= ((1L << leftBack) | (1L << leftForward) | (1L << leftForward) | (1L << forwardLeft));

                }
                else {
                    // leftBack, leftForward, forwardLeft, forwardRight, rightForward, rightBack
                    board |= ((1L << leftBack) | (1L << leftForward) | (1L << forwardLeft) | (1L << forwardRight) |
                     (1L << rightForward) | (1L << rightBack));
                }

            }
            // Knight on 7th rank
            else if (rank == 6){
                if (file == 0){
                    // rightForward, rightBack, backRight
                    board |= ((1L << rightBack) | (1L << rightForward) | (1L << backRight));

                }
                else if (file == 1){
                    // rightForward, rightBack, backRight, backLeft
                    board |= ((1L << rightBack) | (1L << rightForward) | (1L << backRight) | (1L << backLeft));

                }
                else if (file == 7){
                    // leftForward, leftBack, backLeft
                    board |= ((1L << leftForward) | (1L << leftBack) | (1L << backLeft));

                }
                else if (file == 6){
                    // leftForward, leftBack, backLeft, backRight
                    board |= ((1L << leftForward) | (1L << leftBack) | (1L << backLeft) | (1L << backRight));

                }
                else {
                    // leftForward, leftBack, backLeft, backRight, rightForward, rightBack
                    board |= ((1L << leftForward) | (1L << leftBack) | (1L << backLeft) | (1L << backRight) |
                     (1L << rightForward) | (1L << rightBack));

                }
            }
            // Knight on any other rank
            else {
                if (file == 0){
                    // Forward right, rightForward, rightBack, backRight
                    board |= ((1L << forwardRight) | (1L << rightForward) | (1L << rightBack) | (1L << backRight));

                }
                else if (file == 7){
                    // forwardLeft, leftForward, leftBack, backLeft
                    board |= ((1L << forwardLeft) | (1L << leftForward) | (1L << leftBack) | (1L << backLeft));
                }
                else if (file == 1){
                    // forwardLeft, forwardRight, rightForward, rightBack, backRight, backLeft
                    board |= ((1L << forwardLeft) | (1L << forwardRight) | (1L << rightForward) | (1L << rightBack) | (1L << backRight) | (1L << backLeft));

                }
                else if (file == 6){
                    // forwardLeft, forwardRight, leftForward, leftBack, backLeft, backRight
                    board |= ((1L << forwardLeft) | (1L << forwardRight) | (1L << leftForward) | (1L << leftBack) | (1L << backRight) | (1L << backLeft));

                }
                else {
                    // Full moveset
                    board |= ((1L << forwardLeft) | (1L << forwardRight) | (1L << rightForward) | (1L << rightBack) |
                     (1L << backRight) | (1L << backLeft) | (1L << leftForward) | (1L << leftBack));
                }



                

            }
         
            retMasks.add(board);
        }
        return retMasks;
    }
    // Generate sliding masks
    // These are simply rays that flag the entire path
    // Straight rays return 8 masks instead of 64
    // Easy to modify later, simply add 8 copies of each rank/file instead of 1
    public static ArrayList<Long> generateVerticalRayMask(){
        ArrayList<Long> retMasks = new ArrayList<>();

        for (int i = 0; i < 8; i++){
            Long board = 0L;
            for (int j = 0; j < 8; j++){
                board |= (1L << 63 - ((j * 8) + i));
            }
            retMasks.add(board);
        }

        return retMasks;
    }

    public static ArrayList<Long> generateHorizontalRayMask(){
        ArrayList<Long> retMasks = new ArrayList<>();

        for (int i = 0; i < 8; i++){
            Long board = (255L << (7 - i) * 8);
            retMasks.add(board);
        }

        return retMasks;
    }
    // Diagonals return 64 masks again as diagonals change with both rank and file
    public static ArrayList<Long> generateDiagonalRayMask(){
        ArrayList<Long> retMasks = new ArrayList<>();

        for (int i = 0; i < 64; i++){
            // Fill in piece position
            Long board = (1L << 63 - i);
            int rank = i / 8;
            int file = i % 8;

            // Northeast
            int NESquares;
            int SWSquares;
            if (rank < file){
                NESquares = 7 - file;
                SWSquares = rank;
            }
            else {
                NESquares = 7 - rank;
                SWSquares = file;
            }

            for (int j = 0; j < NESquares; j++){
                board |= (1L << 63 - (i + ((8 + 1) * (j + 1))));
            }
            for (int j = 0; j < SWSquares; j++){
                board |= (1L << 63 - (i + ((-8 - 1) * (j + 1))));
            }

            retMasks.add(board);
        }

        return retMasks;
    }
    
    public static ArrayList<Long> generateAntiRayMask(){
        ArrayList<Long> retMasks = new ArrayList<>();

        for (int i = 0; i < 64; i++){
            // Fill in piece position
            Long board = (1L << 63 - i);
            int rank = i / 8;
            int file = i % 8;

            // Northeast
            int NWSquares = ((7 - rank) < file) ? (7 - rank) : file;
            int SESquares = ((7 - file) < rank) ? (7 - file) : rank;
            

            for (int j = 0; j < NWSquares; j++){
                board |= (1L << 63 - (i + ((8 - 1) * (j + 1))));
            }
            for (int j = 0; j < SESquares; j++){
                board |= (1L << 63 - (i + ((-8 + 1) * (j + 1))));
            }

            retMasks.add(board);
        }

        return retMasks;
    }
    
    
    // White/black en pessant, White/black king castling
    // Unused, generate valid moves ray-by-ray with hypQuint()
    /*
    public static ArrayList<Long> generateQueenMoveMask(){
        ArrayList<Long> retMasks = new ArrayList<>();
        // For the rank and file, set 1s to the whole row and column
        
        for (int i = 0; i < 64; i++){
            int rank = i / 8;
            int file = i % 8;
            Long board = 0L;

            int numRightMoves = 7 - file;
            int numLeftMoves = file;
            int numForwardMoves = 7 - rank;
            int numBackMoves = rank;
            // Number of possible diagnoal moves is the miniumum of the two directions,
            // ie forward right moves is the lesser of forward or right moves
            int numForwardRightMoves = numForwardMoves < numRightMoves ? numForwardMoves : numRightMoves;
            int numForwardLeftMoves = numForwardMoves < numLeftMoves ? numForwardMoves : numLeftMoves;
            int numBackRightMoves = numBackMoves < numRightMoves ? numBackMoves : numRightMoves;
            int numBackLeftMoves = numBackMoves < numLeftMoves ? numBackMoves : numLeftMoves;

            // Iterate through each of the remaining moves and flip the corresponding bit on the bitboard
            for (int j = 0; j < numRightMoves; j++){
                board |= (1L << 63 - (i + (j + 1)));
            }
            for (int j = 0; j < numLeftMoves; j++){
                board |= (1L << 63 - (i - (j + 1)));
            }
            for (int j = 0; j < numForwardMoves; j++){
                board |= (1L << 63 - (i + (8 * (j + 1))));
            }
            for (int j = 0; j < numBackMoves; j++){
                board |= (1L << 63 - (i - (8 * (j + 1))));
            }

            for (int j = 0; j < numForwardRightMoves; j++){
                board |= (1L << 63 - (i + ((8 + 1) * (j + 1))));
            }
            for (int j = 0; j < numForwardLeftMoves; j++){
                board |= (1L << 63 - (i + ((8 - 1) * (j + 1))));
            }
            for (int j = 0; j < numBackRightMoves; j++){
                board |= (1L << 63 - (i + ((-8 + 1) * (j + 1))));
            }
            for (int j = 0; j < numBackLeftMoves; j++){
                board |= (1L << 63 - (i + ((-8 - 1) * (j + 1))));
            }

            retMasks.add(board);
        }
        return retMasks;
    }

    public static ArrayList<Long> generateBishopMoveMask(){
        ArrayList<Long> retMasks = new ArrayList<>();

        for (int i = 0; i < 64; i++){
            int rank = i / 8;
            int file = i % 8;

            Long board = 0L;

            // Compares the moves forward and to the right, takes the minimum of the two,
            // which is always the number of diagonal moves towards h8
            int forwardRightMoves = (7 - file) < (7 - rank) ? (7 - file) : (7 - rank);
            int forwardLeftMoves = (7 - rank) < file ? (7 - rank) : file;
            int backRightMoves = (7 - file) < rank ? (7 - file) : rank;
            int backLeftMoves = file < rank ? file : rank;

            // For each move left in the forward right direction, flip each bit corresponding to the position on the board
            // Multiply 8 + 1 by j + 1 because we also want to get the next diagonal, not just the first one
            // 8 for moving up a rank, 1 for moving over a file
            // i is our current position on the board, all calculations are done with that reference point
            // Get the bitshift amount by subtracting total 'move' value from 63 to get correct position on bitboard
            for (int j = 0; j < forwardRightMoves; j++){
                board |= (1L << (63 - (i + (8 + 1) * (j + 1))));
            }

            for (int j = 0; j < forwardLeftMoves; j++){
                board |= (1L << (63 - (i + (8 - 1) * (j + 1))));
            }

            for (int j = 0; j < backLeftMoves; j++){
                board |= (1L << (63 - (i + (- 8 - 1) * (j + 1))));
            }

            for (int j = 0; j < backRightMoves; j++){
                board |= (1L << (63 - (i + (- 8 + 1) * (j + 1))));
            }

            retMasks.add(board);
        }

        return retMasks;
    }

    public static ArrayList<Long> generateRookMoveMask(){
        ArrayList<Long> retMasks = new ArrayList<>();

        for (int i = 0; i < 64; i++){
            Long board = 0L;

            int rank = i / 8;
            int file = i % 8;

            for (int j = 0; j < (7 - file); j++){
                board |= (1L << (63 - (i + (1 * (j + 1)))));
            }
            for (int j = 0; j < file; j++){
                board |= (1L << (63 - (i + (- 1 * (j + 1)))));
            }

            for (int j = 0; j < (7 - rank); j++){
                board |= (1L << (63 - (i + (8 * (j + 1)))));
            }
            for (int j = 0; j < rank; j++){
                board |= (1L << (63 - (i + (- 8 * (j + 1)))));
            }
            retMasks.add(board);
        }


        return retMasks;
    }
     */
    //
    //#endregion

    //#region Mask generation
    // generate ATTACK masks accounting for blockers
    public long generatePlayerPieceMask(int piece){
        long retMask = 0L;
        //Rank
        for (int i = 0; i < 8; i++){
            //File
            for (int j = 0; j < 8; j++){
                if (piece < 0){
                    if (this.boardState[i][j] < 0){
                        int bitPos = (i * 8) + j;
                        retMask |= (1L << (63 - bitPos));
                    }
                }
                else {
                    if (this.boardState[i][j] > 0){
                        int bitPos = (i * 8) + j;
                        retMask |= (1L << (63 - bitPos));
                    }
                }
            }
        }

        return retMask;
    }

    public long generateValidStraightRayMask(int position){
        long retBoard = 0L;
        long pieceMask = (1L << (63 - position));

        retBoard |= hypQuint(this.bitState, HORIZONTAL_RAY.get(position / 8), pieceMask);
        retBoard |= hypQuint(this.bitState, VERTICAL_RAY.get(position % 8), pieceMask);

        return retBoard;
    }

    public long generateValidDiagonalRayMask(int position){
        long retBoard = 0L;
        long pieceMask = (1L << (63 - position));

        retBoard |= hypQuint(this.bitState, DIAGONAL_MOVE.get(position), pieceMask);
        retBoard |= hypQuint(this.bitState, ANTI_MOVE.get(position), pieceMask);

        return retBoard;
    }

    public long generateValidAllRayMask(int position){
        long retBoard = generateValidStraightRayMask(position);
        retBoard |= generateValidDiagonalRayMask(position);

        return retBoard;
    }

        // Checks for:
    //  - Piece attack pattern given origin square
    //  - Sliding piece blocking (hypQuint)
    //  - Opponent occupancy in attacked square (Mask comparison for non-sliders, hypQuint for sliding pieces)
    //  - Friendly occupancy invalidating self-captures
    // Does not check for:
    //  - Forcing moves (Different method is invoked to find valid check escape moves)
    //  - Self-check (Should check right before Move object creation)
    public long generatePieceAttackMask(int piece, int origin){
        long retMask = 0L;
        long friendlyOcc = (piece < 0) ? generatePlayerPieceMask(-1) : generatePlayerPieceMask(1);
        long opponentVision = (piece < 0) ? generatePieceVision(1) : generatePieceVision(-1);
        long temp;
        switch (Math.abs(piece)){
            case 1:
                // Pawn vision vs Attack is also different like Kings
                // Attacks: Requires occupancy check at destination square
                // Vision: No occupancy check, Possible attack moves on a square next turn if king moves into that square
                retMask |= generatePawnAttackMask(piece, origin);
                break;
            case 2:
                // Invaldiate self captures
                retMask |= (KNIGHT_MOVE.get(origin) ^ (friendlyOcc & KNIGHT_MOVE.get(origin)));
                break;
            case 3:
                // Invalidate bishop self-captures
                temp = generateValidDiagonalRayMask(origin);
                retMask |= (temp ^ (friendlyOcc & temp));
                break;
            case 4: 
                // Invalidate rook self-capture
                temp = generateValidStraightRayMask(origin);
                retMask |= (generateValidStraightRayMask(origin) ^ (friendlyOcc & temp));
                break;
            case 5:
                // Queen
                temp = generateValidAllRayMask(origin);
                retMask |= (generateValidAllRayMask(origin) ^ (friendlyOcc & temp));
                break;
            case 6:
                // King - (Attack only, can never check king with king)
                // As attack mask: Vision avoidance, move pattern, friendly occupancy
                // As vision mask: NO vision avoidance, move pattern, no friendly occupancy (if friendly then not enemy king/castling)
                long kingPosMask = (1L << (63 - origin));
                // If king is in check, we must remove the king's position from the valid moves as XORing with opponent vision in this case
                // will flip the king position to 1
                if ((opponentVision & kingPosMask) > 0){
                    retMask |= (KING_MOVE.get(origin) & ~friendlyOcc) & ~opponentVision & ~kingPosMask;
                }
                else {
                    retMask |= (KING_MOVE.get(origin) & ~friendlyOcc) & ~opponentVision;
                }




                break;

        }
        return retMask;
    }

    public long generatePawnMoveMask(int playerSign, int origin){
        int rank = origin / 8;
        int file = origin % 8;

        if ((playerSign > 0) && (rank == 1)){
            long above = (1L << (63 - origin + 8));
            // if square above the pawn is blocked, it cannot move at all
            if ((above & this.bitState) > 0){
                return 0L;
            }
            else {
                // Pawn can move forward 1 even if the 2nd square is blocked
                return W_PAWN_MOVE.get(origin) & ~this.bitState;
            }
        }
        else if ((playerSign) < 0 && (rank == 6)){
            long below = (1L << (63 - origin - 8));
            if ((below & this.bitState) > 0){
                return 0L;
            }
            else {
                return B_PAWN_MOVE.get(origin) & ~this.bitState;
            }
        }
        else {
            return (playerSign > 0) ? (W_PAWN_MOVE.get(origin) & ~this.bitState) : (B_PAWN_MOVE.get(origin) & ~this.bitState);
        }

    }

    public long generatePawnAttackMask(int playerSign, int origin){
        long opponentOcc = (playerSign > 0) ? generatePlayerPieceMask(-1) : generatePlayerPieceMask(1);
        long retMask = 0L;
        if (playerSign > 0){
            // Valid attacks are ones with opponents on the destination square
            retMask |= (W_PAWN_ATTACK.get(origin) & opponentOcc);
        }
        else {
            retMask |= (B_PAWN_ATTACK.get(origin) & opponentOcc);
        }

        return retMask | generateEnPassentMask(playerSign, origin);
    }

    // Vision doesn't necessarily grant validity
    // Vision == threat of capture == attacks
    // Use for move validity (king move check evasion, castling, self-checking moves )
    public long generatePieceVision(int playerSign){
        long retMask = 0L;

        // Possible attack moves for all pieces constitute vision on the king and castling path
        // No self-check avoidance required
        // For king: No vision avoidance required
        for (int i = 0; i < 8; i++){
            for (int j = 0; j < 8; j++){
                int bitInd = (i * 8) + j;
                int piece = this.boardState[i][j];
                if ((playerSign > 0) && (piece > 0)){
                    switch (piece){
                        // Pawn attack
                        case 1: 
                            // No occupancy check required for vision
                            // If a piece exists, the enemy king will never be there, if it doesn't exist, the square is in vision
                            // If a piece exists, they cannot castle anyways, if it doesn't exist, they can't castle as its in vision
                            // Additionally, if friendly piece is capture by enemy, it is now in vision (prevents king captures)
                            retMask |= W_PAWN_ATTACK.get(bitInd);
                            break;
                        // Knight
                        case 2:
                            // No need for occupancy check once again
                            retMask |= KNIGHT_MOVE.get(bitInd);
                            break;
                        // Bishop
                        case 3:
                            // Sliding pieces still require ray blocker calculations
                            // However don't require piece checking at destination
                            retMask |= generateValidDiagonalRayMask(bitInd);
                            break;
                        // Rook
                        case 4:
                            retMask |= generateValidStraightRayMask(bitInd);
                            break;
                        // Queen
                        case 5:
                            retMask |= generateValidAllRayMask(bitInd);
                            break;
                        // King
                        case 6:
                            // No need to check for opponent vision, 
                            // no need to check for occupancy for same reasons
                            retMask |= KING_MOVE.get(bitInd);
                            break;
                    } 
                    // KINGS vision counts as vision
                }
                // Same logic for Black vision
                else if ((playerSign < 0) && (piece < 0)){
                    switch (piece){
                        case -1: 
                            retMask |= B_PAWN_ATTACK.get(bitInd);
                            break;
                        case -2:
                            retMask |= KNIGHT_MOVE.get(bitInd);
                            break;
                        case -3:
                            retMask |= generateValidDiagonalRayMask(bitInd);
                            break;
                        case -4:
                            retMask |= generateValidStraightRayMask(bitInd);
                            break;
                        case -5:
                            retMask |= generateValidAllRayMask(bitInd);
                            break;
                        case -6:
                            retMask |= KING_MOVE.get(bitInd);
                            break;
                    } 
                }
            }
        }
        return retMask;
    }

    // Isolates the specific ray between the checking piece and king, ignoring all other rays
    // Includes origin (capture)
    public static long generateEvasionPath(int origin, int destination){
        int direction;

        int originRank = origin / 8;
        int originFile = origin % 8;
        int originDiag = origin % 9;
        int originAnti = origin% 7;
        int destRank = destination / 8;
        int destFile = destination % 8;
        int destDiag = destination % 9;
        int destAnti = destination % 7;

        // Ensure two points lie on the same ray
        if (!((originRank == destRank) || (originFile == destFile) || (originAnti == destAnti) || (originDiag == destDiag))){
            System.out.print("Invalid origin and destination squares for evasion path generation");
            return 0L;
        }

        // Straight path
        if (originRank == destRank){
            // Right straight path
            if (destination > origin){
                direction = 1;
            }
            // left straight path
            else {
                direction = -1;
            }
        }
        else if (originFile == destFile){
            // Straight up
            if (destination > origin){
                direction = 8;
            }
            // Straight down
            else {
                direction = -8;
            }
        }
        // Up-right diagonal
        else if ((destRank > originRank) && (destFile > originFile)){
            direction = 9;
        }
        // Up-left diagonal
        else if ((destRank > originRank) && (destFile < originFile)){
            direction = 7;
        }
        // Down-right diagonal
        else if ((destRank < originRank) && (destFile > originFile)){
            direction = -7;
        }
        // Down-left diagonal
        // Just an 'else' to satisfy the compiler
        else {
            direction = -9;
        }

        long pos = (1L << (63 - origin));
        long retMask = (1L << (63 - origin));
        long endMask = (1L << (63 - destination));

        while ((pos = shift(pos, direction)) != endMask){
            retMask |= pos;
        }

        return retMask;
    }
    
    // Returns a one-bit enpassent mask
    // Doesn't check for destination square occupancy since EP is only vaid if a pawn has JUST moved
    // through those squares
    public long generateEnPassentMask(int playerSign, int origin){
        int rank = origin / 8;
        int file = origin % 8;
        Move lastMove = this.playedMoves.peek();
        // return 0L if move is null
        if (lastMove == null){return 0L;}
        int lastMovePiece = lastMove.getPiece();
        int lastMoveDestBit = lastMove.getDestBit();
        int lastMoveRankDiff = Math.abs(lastMove.getOriginRank() - lastMove.getDestinationRank());
        
        int opponentPawn = (playerSign > 0) ? -1 : 1;
        int rightAttackShift = (playerSign > 0) ? 9 : -7;
        int leftAttackShift = (playerSign > 0) ? 7 : -9;
        int epRank = (playerSign > 0) ? 4 : 3;
        
        // Return 0L if:
        // - Last move was not enemy pawn move
        // - Current rank is not eligible for EP
        if ((rank != epRank) || (Math.abs(lastMovePiece) != 1)){return 0L;}

        long retMask = 0L;

       
        // Left border, only check right EP
        if (file == 0){
            int rightPiece = this.boardState[rank][file + 1];
            int rightSquare = (rank * 8) + file + 1;
            //       If piece == pawn                  Check correct pawn,           2 squares 'forward'
            if ((rightPiece == opponentPawn) && (lastMoveDestBit == rightSquare) && (lastMoveRankDiff > 1)){
                retMask |= (1L << (63 - (origin + rightAttackShift))); // Return with the right-forward attack mask
            }
            
        }
        // Right border, only check left EP
        else if (file == 7){
            int leftPiece = this.boardState[rank][file - 1];
            int leftSquare = (rank * 8) + file - 1;

            if ((leftPiece == opponentPawn) && (lastMoveDestBit == leftSquare) && (lastMoveRankDiff > 1)){
                retMask |= (1L << (63 - (origin + leftAttackShift))); // Return with the left-forward attack mask
            }
        }
        // Middle, check both side EP
        else {
            int leftPiece = this.boardState[rank][file -1];
            int rightPiece = this.boardState[rank][file + 1];

            int leftSquare = (rank * 8) + file - 1;
            int rightSquare = leftSquare + 2;

            if ((leftPiece == opponentPawn) && (lastMoveDestBit == leftSquare) && (lastMoveRankDiff > 1)){
                retMask |= (1L << (63 - (origin + leftAttackShift))); // Add the left-forward attack mask
            }
            else if ((rightPiece == opponentPawn) && (lastMoveDestBit == rightSquare) && (lastMoveRankDiff > 1)){
                retMask |= (1L << (63 - (origin + rightAttackShift))); // Add the left-forward attack mask
            }

        }

        return retMask;
    }
    
    
    //#endregion

    //#region Move generation
    // Move generation for FORCING check states
    // Must also account for Self-checking as this is a move generator
    // Returns list of valid piece moves as int[]{piece, origin, destination}
    // If no moves exist, then checkmate
    public ArrayList<int[]> generateCheckEvasionMoves(int playerSign){
        ArrayList<int[]> retArray = new ArrayList<>();
        int kingPos = findKingBitPosition(playerSign);
        ArrayList<int[]> checks = getOpponentChecks(playerSign);
        int playerKing = (playerSign > 0) ? 6 : -6;


        // Iterate through all possible king moves to find ones that evade check
        // (KingMove XOR FriendlyPieceMask) XOR OpponentVision
        // King moves already account for opponent vision and friendly occupancy
        long kingMoves = generatePieceAttackMask(playerKing, kingPos);
        ArrayList<Integer> validSquares = getSetBitPositions(kingMoves);
        for (int i : validSquares){
            retArray.add(new int[]{playerKing, kingPos, i});
        }

        // Add other piece moves for single check, otherwise skip
        if (!(checks.size() > 1)){
            // SINGLE check:
            // Find Enemy pieces and paths checking king
            // checks should only contain ONE check at this point
            int checkingPiece = checks.get(0)[0];
            int checkingPos = checks.get(0)[1];
    
            // Differentiate between Sliding vs non pieces.
                // Non-sliding pieces MUST be captured, their piecemask is the only valid evasion square
                // Sliding pieces can be blocked along their attack path or captured
                // Sliding pieces are > 2
            long evasionMask = (Math.abs(checkingPiece) > 2) ? generateEvasionPath(checkingPos, kingPos) : (1L << (63 - checkingPos));
    
            // Iterate through players pieces to find ones that can block or capture
            int evasionPiece;
            int square;
            long combinedMask;
            for (int i = 0; i < 8; i++){
                for (int j = 0; j < 8; j++){
                    evasionPiece = this.boardState[i][j];
                    square = (i * 8) + j;
    
                    // Skip king as we've already calculated it
                    if (Math.abs(evasionPiece) == 6){continue;}
    
                    // Ensuring we find the same players pieces
                    if ((evasionPiece * playerSign) > 0){
                        // Find where along the attack path a piece could block (incl capture)
                        // Since pawns move and attack differently, we must first combine the valid move and attack masks since either are eligible
                        // for check blocking
                        if (Math.abs(evasionPiece) == 1){
                            // Combine valid pawn moves and attacks, then filter using evasion mask
                            // generatePieceAttackMasks returns valid en passent as well
                            // generateEnPassentMask() will only return a non 0 value IF the checking move was an eligible double pawn forward.
                            //      - Pawn double move check: Will only return non-zero IF current pawn can en passent it
                            //      - Other checks: returns 0L;
                            combinedMask = (generatePieceAttackMask(evasionPiece, square) | generatePawnMoveMask(evasionPiece, square)) & (evasionMask | generateEnPassentMask(playerSign, square));
                        }
                        else {
                            combinedMask = generatePieceAttackMask(evasionPiece, square) & evasionMask;
                        }
                        if (combinedMask > 0){
                            for (int pos : getSetBitPositions(combinedMask)){
                                retArray.add(new int[] {evasionPiece, square, pos});
                            }
                        }
                    }
                }
            }    
        }// endif
        
        ArrayList<int[]> retArrayFinal = new ArrayList<>();
        // Remove moves resulting in self-check
        int[][] realBoard = getBoard();
        long realOcc = getOcc();

        for (int[] move : retArray){
            int evasionPiece = move[0];
            int originRank = move[1] / 8;
            int originFile = move[1] % 8;
            int destRank = move[2] / 8;
            int destFile = move[2] % 8;
            
            int[][] candidateBoard = deepCloneBoard(realBoard);
            candidateBoard[originRank][originFile] = 0; // Remove piece from origin
            candidateBoard[destRank][destFile] = evasionPiece; // Put piece in destination
            long candidateOcc = boardToBitboard(candidateBoard);
            
            // IF PAWN & ATTACK & ON EMPTY SQUARE, then en passent
            // If en passent, we have to 'capture' the pawn, set to 0 in board
            if ((Math.abs(evasionPiece) == 1) && (originFile != destFile) && (realBoard[destRank][destFile] == 0)){
                int epRank = (evasionPiece > 0) ? destRank - 1 : destRank + 1;
                candidateBoard[epRank][destFile] = 0; // Set pawn that was en-passented to 0
                candidateOcc = boardToBitboard(candidateBoard); // Overwrite occupancy
            }
            
            // Switch for checking
            this.boardState = candidateBoard;
            this.bitState = candidateOcc;
            // Ensure move doesn't result in discovered self-check
            if (getOpponentChecks(playerSign, candidateBoard).size() == 0){
                retArrayFinal.add(move);
            }

        }
        // Switch for real board state
        this.boardState = realBoard;
        this.bitState = realOcc;
        return retArrayFinal;
    }

    // Returns an ArrayList<int[]> with int[] containing pieceID, origin, destination
    // These are all existing checks against player with playerSign
    public ArrayList<int[]> getOpponentChecks(int playerSign){
        ArrayList<int[]> retChecks = new ArrayList<>();
        int kingPos = findKingBitPosition(playerSign);
        long kingMask = (1L << (63 - kingPos));
        // Iterate through board looking for opponent's pieces
        for (int i = 0; i < 8; i++){
            for (int j = 0; j < 8; j++){
                int square = (i * 8) + j;
                // If player and piece are opponents
                // Piece sign never changes throughout, so only one of these ORs will ever be true
                int piece = this.boardState[i][j];
                if (((piece < 0) && (playerSign > 0)) || ((piece > 0) && (playerSign < 0))){
                    if ((kingMask & generatePieceAttackMask(piece, square)) != 0){
                        retChecks.add(new int[]{piece, square, kingPos});
                    }
                }
            }
        }
        return retChecks;
    }

    // Overloaded parameters to check for checks in candidate board states, not the current board state
    public ArrayList<int[]> getOpponentChecks(int playerSign, int[][] boardState){
        ArrayList<int[]> retChecks = new ArrayList<>();
        int kingPos = findKingBitPosition(playerSign);
        long kingMask = (1L << (63 - kingPos));
        for (int i = 0; i < 8; i++){
            for (int j = 0; j < 8; j++){
                int square = (i * 8) + j;
                // If player and piece are opponents
                // Piece sign never changes throughout, so only one of these ORs will ever be true
                int piece = boardState[i][j];
                if (((piece < 0) && (playerSign > 0)) || ((piece > 0) && (playerSign < 0))){
                    if ((kingMask & generatePieceAttackMask(piece, square)) != 0){
                        retChecks.add(new int[]{piece, square, kingPos});
                    }
                }
            }
        }
        return retChecks;
    }

    //#endregion

    //#region Board utility
    public static String longToString(Long num){
        return String.format("%64s", Long.toBinaryString(num)).replace(' ', '0');
    }

    public static long boardToBitboard(int[][] board){
        if (board.length != 8 || board[0].length != 8){
            System.out.println("boardToBitboard ERROR: Incompatible board format provided.");
            return 0L;
        }

        long retBoard = 0L;
        int bitCount = 0;
        // Bitcount increments for each inner loop (8x8 = 64) allowing it to index the 64bit bitboard properly
        for (int i = 0; i < 8; i ++){
            for (int j = 0; j < 8; j++){
                // If there is any piece, shift a 1 down to that position in the bitboard
                if (board[i][j] != 0){
                    retBoard |= (1L << (63 - bitCount));
                }
                bitCount++;
            }
        }

        return retBoard;
    };

    public static long[][] generateRandomZobrist(){
        long[][] retArray = new long[8][8];

        for (int i = 0; i < 8; i++){
            for (int j = 0; j < 8; j++){
                retArray[i][j] = ThreadLocalRandom.current().nextLong();
            }
        }

        return retArray;
    };

    public static int[][] generateFreshBoard(){
        int[][] retArray = new int[8][8];

        for (int i = 0; i < 8; i++){
            for (int j = 0; j < 8; j++){
                retArray[i][j] = 0;
            }
        }

        // Fill pawns
        for (int i = 0; i < 8; i++){
            retArray[1][i] = 1;
        }
        for (int i = 0; i < 8; i++){
            retArray[6][i] = -1;
        }

        // Set white major pieces
        retArray[0][0] = 4;
        retArray[0][1] = 2;
        retArray[0][2] = 3;
        retArray[0][3] = 5;
        retArray[0][4] = 6;
        retArray[0][5] = 3;
        retArray[0][6] = 2;
        retArray[0][7] = 4;

        // Set black major pieces
        retArray[7][0] = -4;
        retArray[7][1] = -2;
        retArray[7][2] = -3;
        retArray[7][3] = -5;
        retArray[7][4] = -6;
        retArray[7][5] = -3;
        retArray[7][6] = -2;
        retArray[7][7] = -4;

        return retArray;
    };

    public static int[][] deepCloneBoard(int[][] board){
        int[][] retBoard = new int[8][8];

        for (int i = 0; i < 8; i++){
            retBoard[i] = board[i].clone();
        }

        return retBoard;
    }

    public static void bitboardVisualize(long bitboard){
        StringBuilder sbRank = new StringBuilder();
        StringBuilder sbFile = new StringBuilder();
        String bitString = String.format("%64s", Long.toBinaryString(bitboard)).replace(' ', '0');

        for (int i = 0; i < 64; i++){

            if (i % 8 == 0){
                sbRank.reverse().append("\n");
                sbFile.append(sbRank.toString());
                sbRank.setLength(0);
            }

            sbRank.append(bitString.charAt(i) + " ");
        }
        sbFile.append(sbRank.reverse().append('\n').toString());
        
        sbFile.reverse();
        sbFile.append("\n");

        System.out.print(sbFile.toString());

    }

    public static void boardVisualize(int[][] board){
        StringBuilder sbInner = new StringBuilder();
        StringBuilder sbOuter = new StringBuilder();


        for (int i = 0; i < 8; i++){
            sbInner.setLength(0);
            sbInner.append((i + 1) + " ");
            for (int j = 0; j < 8; j++){
                sbInner.append("[" + CHESS_EMOJI.get(board[i][j]) + " ]");
            }
            sbInner.append("\n");
            sbOuter.insert(0, sbInner.toString());
            String temp = sbOuter.toString();
        }
        sbOuter.append("   A   B   C   D   E   F   G   H");
        System.out.println();
        System.out.print(sbOuter.toString());
        System.out.println();
    }

    //#endregion

    //#region Bit utility
    // Optimized in terms of operation types, still high number of operations
    public static long transpose(Long board){
        long retBoard = 0L;
        int iteration = 0;
        for (int i = 0; i < 8; i++){
            for (int j = 0; j < 8; j++){
                // Shift down to current index
                int index = (j * 8) + i;
                Long indexBit = (1L << index);

                // Read board at index, 1 for something 0 for nothing
                indexBit &= board;

                // If we read a 1 at the index, shift down by that index and OR it, to add to retBoard
                // If not, then we skip the index as it is already 0
                if (indexBit != 0){
                    retBoard |= (1L << iteration);
                }
                iteration ++;
            }
        }
        return retBoard;
    }

    // occMask DOES NOT INCLUDE the piece in question
    // rayMask is the specific ray we're calculating at the moment
    // pieceMask is the location of the piece
    // Provide RLERF encoded parameters, returns RLERF encoded mask
    public static long hypQuint(long occMask, long rayMask, long pieceMask){
        long retBits;
        // Reverse all provided bitmasks from RLERF --> LERF
        occMask = Long.reverse(occMask);
        rayMask = Long.reverse(rayMask);
        pieceMask = Long.reverse(pieceMask);

        // occMask becomes only the pieces in the path of the ray
        occMask = occMask & rayMask;

        //retBits = ((rayOcc & rayMask) - (2 * pieceMask)) ^ (Long.reverse(Long.reverse(rayOcc & rayMask) - 2 * Long.reverse(pieceMask))) & rayMask;
        //retBits = ((rayOcc & rayMask) - (2 * pieceMask)) ^ (Long.reverse(Long.reverse(rayOcc & rayMask) - Long.reverse(2 *pieceMask))) & rayMask;
        //retBits = ((rayMask & occMask) - (2 * pieceMask)) ^ Long.reverse(Long.reverse(rayMask & occMask) - Long.reverse(pieceMask * 2)) & rayMask;
        long forward = rayMask & occMask;
        long reverse = Long.reverse(forward);
        forward = forward - (2 * pieceMask);
        reverse = reverse - (2 * Long.reverse(pieceMask));

        // System.out.println("rayMask: " + test.longToString(rayMask));
        // System.out.println("occMask: " + test.longToString(occMask));
        // System.out.println("pieceMask: " + test.longToString(pieceMask));

        retBits = (forward ^ Long.reverse(reverse)) & rayMask;
       
        // Reverse final bitmask from LERF --> RLERF
        return Long.reverse(retBits);
        
    }

    public int findKingBitPosition(int playerSign){
        int kingInt = (playerSign > 0) ? 6 : -6;
        for (int i = 0; i < 8; i++){
            for (int j = 0; j < 8; j++){
                if (this.boardState[i][j] == kingInt){
                    return (i * 8) + j;
                }
            }
        }
        // returns negative if not found
        return -1;
    }

    // Returns bit position in RLERF encoding
    public static ArrayList<Integer> getSetBitPositions(long bitboard){
        ArrayList<Integer> retArray = new ArrayList<>();
        while (bitboard != 0){
            int index = Long.numberOfTrailingZeros(bitboard);
            // 63 - index for RLERF
            retArray.add(63 - index);
            bitboard &= bitboard - 1;
        }

        return retArray;
    }

    public static long shift(long board, int direction){
        return (direction > 0) ? (board >>> direction) : (board << -direction);
    }
    
    //#endregion

    //#region Getters/Setters

    public long getBitState(){
        return this.bitState;
    }

    public int[][] getBoard(){
        int[][] retBoard = new int[8][8];

        for (int i = 0; i < 8; i++){
            retBoard[i] = this.boardState[i].clone();
        }
        return retBoard;
    }

    public long getOcc(){
        return this.bitState;
    }

    // TEST FUNCTION REMOVE AFTER
    public void setBoard(int[][] newBoard){
        this.boardState = newBoard;
    }
    public void setOcc(long occ){
        this.bitState = occ;
    }
    public void addMoveToQueue(Move move){
        this.playedMoves.add(move);
    }
    public void setMoveQueue(ArrayDeque<Move> queue){
        this.playedMoves = queue;
    }
    
    //#endregion
}
