package com.YCorp.chessApp.client.engine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.concurrent.ThreadLocalRandom;


import com.YCorp.chessApp.client.engine.Move.MOVE_TYPE;
import com.YCorp.chessApp.client.parser.RegexParser;

/**
 * Board is the main class in this chess program, responsible for tracking and updating
 * the current board state with each played move.
 * 
 * <p>
 * Board tracks draw conditions through a constantly updated zobrist hash and half-clock counter.<br>
 * It also contains the bitmasks required for valid move generation (Perhaps this should be in {@link Move} instead)<br>
 * Currently the move masks are generated statically at the first instantiation of Board.<br>
 * Any manipulation of the boardState or bitState is handled by methods in Board.<br>
 * Additonally valid {@link Move} object creation is handled in Board, as move validity<br>
 * is fundamentally linked to the current board.<br>
 * </p>
 * <p>
 * <h2><a name="RLERF">Reverse Little Endian Rank-File (RLERF) encoding:</a></h2>
 * All bitmasks or bitboard representations are encoded in a RLERF format where the least significant bit <br>
 * represents the 64th square (h8) and index 63 and the most significant bit represents the 1st square (a1).<br>
 * Example:<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;a&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;b&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;c&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;d&nbsp;
 * &nbsp;&nbsp;&nbsp;&nbsp;e&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;f&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;g&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;h<br>
 * 8  [56] [57] [58] [59] [60] [61] [62] [63]<br>
 * 7  [48] [49] [50] [51] [52] [53] [54] [55]<br>
 * 6  [40] [41] [42] [43] [44] [45] [46] [47]<br>
 * 5  [32] [33] [34] [35] [36] [37] [38] [39]<br>
 * 4  [24] [25] [26] [27] [28] [29] [30] [31]<br>
 * 3  [16] [17] [18] [19] [20] [21] [22] [23]<br>
 * 2  [08] [09] [10] [11] [12] [13] [14] [15]<br>
 * 1  [00] [01] [02] [03] [04] [05] [06] [07]<br>
 * <br>
 * Thus the first 8 bits 0b0000 0000... would be 0b'a1''b1''c1''d1' 'e1''f1''g1''h1'
 * </p>
 */
public class Board {
    // Any state with W_ or B_ signifies they LOST due to the reason
    // ie W_MATE means that White was Mated
    public enum BOARD_STATE {
        IN_PLAY, CHECK, W_MATE, B_MATE, THREE_REPEAT_DRAW, FIVE_REPEAT_DRAW, MUTUAL_DRAW, STALEMATE, MATERIAL_DRAW, FIFTY_DRAW, SEVENTY_FIVE_DRAW, W_RESIGN, B_RESIGN, W_TIME, B_TIME
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

    //#region Board state variables
    private long bitState; // Current occupancy map for the whole board
    private int[][] board; // True board representation
    
    private ArrayDeque<Move> playedMoves;
    private ArrayDeque<Long> bitStateHistory;
    private ArrayDeque<int[][]> boardHistory;
    private ArrayDeque<BOARD_STATE> stateHistory;
    private ArrayDeque<Long> zobristHistory;
    private ArrayDeque<Long> epHashHistory;
    private ArrayDeque<boolean[]> castlingHistory;
    private ArrayDeque<Integer> halfClockHistory;
    private ArrayList<String> algebraicHistory;

    // These are not constants as they are unique to each Board instance, not consistent across all Boards
    private long zobristHash; // Zobrist hash
    private final long whiteLongHash;
    private final long whiteShortHash;
    private final long blackLongHash;
    private final long blackShortHash;

    private final long whiteTurnHash;
    
    /**
     * Maps file index to a unique corresponding hash value used for Zobrist hashing.
     */
    private final Map<Integer, Long> epHashTable;
    /**
     * Maps each of the 12 unqique pieces to a 64 length long[], containing 64 unique hash<br>
     * values for each board square, used for Zobrist hashing.
     */
    private final Map<Integer, long[]> zobristTable;

    // Castling rights
    // Toggled to false if KING moves, or a Castling move is PLAYED
    private boolean whiteLong = true;
    private boolean whiteShort = true;
    private boolean blackLong = true;
    private boolean blackShort = true;
    

    // Flags indicating caslting permissions have been flipped
    /**
     * Flag indicating that the whiteLong flag has been flipped, used by {@link updateZobrist}.
     */
    private boolean WLF = false;
    /**
     * Flag indicating that the whiteShort flag has been flipped, used by {@link updateZobrist}.
     */
    private boolean WSF = false;
    /**
     * Flag indicating that the blackLong flag has been flipped, used by {@link updateZobrist}.
     */
    private boolean BLF = false;
    /**
     * Flag indicating that the blackShort flag has been flipped, used by {@link updateZobrist}.
     */
    private boolean BSF = false;

    // Optional draw flags
    private boolean threeFoldDrawAvailable = false; // Also automatic 5-fold rule
    private boolean fiftyMoveDrawAvailable = false; // Also automatic 75 move rule

    // Halfclock
    /**
     * Tracks the current half-clock count.<br>
     * 
     * <p>
     * Any move that does not move a pawn or capture a piece will result in an increment<br>
     * of this value. A pawn or capture move resets this value to zero.<br>
     * </p>
     * <p>
     * Used in the determination of whether the fifty-move rule draw is claimable, and <br>
     * whether the seventy-five move forced draw should be enacted.
     * </p>
     * 
     */
    private int halfClock = 0;

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

    /**
     * Creates a "fresh" Board object whose state reflects that of a chess board at the start of a game.
     * <p>
     * Generates random long values for attributes required for zobrist hashing, including the initial zobrist hash.
     * boardState is initialized with the starting chess position, bitState is initialized similarly with full occupancy on ranks 1, 2, 7, 8
     * 
     * 
     * @return A Board object representative of a 'fresh' chess board
     */
    public Board(){
        // Generate fresh moves queue
        playedMoves = new ArrayDeque<>();
        zobristHistory = new ArrayDeque<>();
        
        boardHistory = new ArrayDeque<int[][]>();
        bitStateHistory = new ArrayDeque<Long>();
        castlingHistory = new ArrayDeque<boolean[]>();
        epHashHistory = new ArrayDeque<Long>();
        stateHistory = new ArrayDeque<BOARD_STATE>();
        halfClockHistory = new ArrayDeque<Integer>();
        algebraicHistory = new ArrayList<String>();


        // Generate new zobrist hash
        whiteLongHash = ThreadLocalRandom.current().nextLong();
        whiteShortHash = ThreadLocalRandom.current().nextLong();
        blackLongHash = ThreadLocalRandom.current().nextLong();
        blackShortHash = ThreadLocalRandom.current().nextLong();
        
        whiteTurnHash = ThreadLocalRandom.current().nextLong();
        epHashTable = generateEPHashTable();
        
        zobristTable = generateZobristTable();
        
        
        // Fresh bitboard
        bitState = 0xFFFF00000000FFFFL;
        
        // Populate freshboard
        board = generateFreshBoard();
        
        zobristHash = generateCurrentZobristHash();
        zobristHistory.push(zobristHash);
    }

    //#region Base ray generation -----------------------------------------------------------------------------------------------------------
    // Generate non-sliding piece masks

    /**
     * Generates 64 length {@link ArrayList} of {@link Long} objects, each representing a bitmask
     * of possible white pawn moves for each of the 64 chess squares.
     * 
     * <p>
     * Indices of the returned {@link ArrayList} correspond to a "Reverse little endian rank-file" 
     * chess board encoding, where a1 = 0, a8 = 7, h8 = 63
     * </p>
     * @return An ArrayList<Long> of valid white pawn move masks for each chess square
     */
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
    
    /**
     * Generates 64 length {@link ArrayList} of {@link Long} objects, each representing a bitmask
     * of possible white pawn attacks for each of the 64 chess squares.
     * 
     * <p>
     * Indices of the returned {@link ArrayList} correspond to a "Reverse little endian rank-file" 
     * chess board encoding, where a1 = 0, a8 = 7, h8 = 63
     * </p>
     * @return An ArrayList<Long> of valid white pawn attack masks for each chess square
     */
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

    /**
     * Generates 64 length {@link ArrayList} of {@link Long} objects, each representing a bitmask
     * of possible black pawn moves for each of the 64 chess squares.
     * 
     * <p>
     * Indices of the returned {@link ArrayList} correspond to a "Reverse little endian rank-file" 
     * chess board encoding, where a1 = 0, a8 = 7, h8 = 63
     * </p>
     * @return An ArrayList<Long> of valid black pawn move masks for each chess square
     */
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

    /**
     * Generates 64 length {@link ArrayList} of {@link Long} objects, each representing a bitmask
     * of possible black pawn attacks for each of the 64 chess squares.
     * 
     * <p>
     * Indices of the returned {@link ArrayList} correspond to a "Reverse little endian rank-file" 
     * chess board encoding, where a1 = 0, a8 = 7, h8 = 63
     * </p>
     * @return An ArrayList<Long> of valid white pawn attack masks for each chess square
     */
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
    
    /**
     * Generates 64 length {@link ArrayList} of {@link Long} objects, each representing a bitmask
     * of possible king moves for each of the 64 chess squares.
     * 
     * <p>
     * Indices of the returned {@link ArrayList} correspond to a "Reverse little endian rank-file" 
     * chess board encoding, where a1 = 0, a8 = 7, h8 = 63
     * </p>
     * @return An ArrayList<Long> of valid king move masks for each chess square
     */
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

    /**
     * Generates 64 length {@link ArrayList} of {@link Long} objects, each representing a bitmask
     * of possible knight moves for each of the 64 chess squares.
     * 
     * <p>
     * Indices of the returned {@link ArrayList} correspond to a "Reverse little endian rank-file" 
     * chess board encoding, where a1 = 0, a8 = 7, h8 = 63
     * </p>
     * @return An ArrayList<Long> of valid knight move masks for each chess square
     */
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
                    board |= ((1L << leftBack) | (1L << leftForward) | (1L << forwardRight) | (1L << forwardLeft));

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
    
    /**
     * Generates 8 length {@link ArrayList} of {@link Long} objects, each representing a vertical
     * raymask for each of the 8 files.
     * 
     * <p>
     * Used to generate sliding piece moves for the Rook and Queen as their paths can be blocked
     * by friendly or opponent pieces. Compared to move masks, these only return an Arraylist of 
     * length 8, one for each file where a = 0 -> h = 7
     * @return An ArrayList<Long> of raymasks corresponding to each file
     */
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

    /**
     * Generates 8 length {@link ArrayList} of {@link Long} objects, each representing a horizontal
     * raymask for each of the 8 ranks.
     * 
     * <p>
     * Used to generate sliding piece moves for the Rook and Queen as their paths can be blocked
     * by friendly or opponent pieces. Compared to move masks, these only return an Arraylist of 
     * length 8, one for each rank where 1 = 0 -> 8 = 7
     * @return An ArrayList<Long> of raymasks corresponding to each file
     */
    public static ArrayList<Long> generateHorizontalRayMask(){
        ArrayList<Long> retMasks = new ArrayList<>();

        for (int i = 0; i < 8; i++){
            Long board = (255L << (7 - i) * 8);
            retMasks.add(board);
        }

        return retMasks;
    }
    
    /**
     * Generates 64 length {@link ArrayList} of {@link Long} objects, each representing a diagonal
     * ray for each of the 64 board squares.
     * 
     * <p>
     * Used to generate sliding piece moves for the Bishop and Queen as their paths can be blocked
     * by friendly or opponent pieces.
     * @return An ArrayList<Long> of diagonal raymasks centered on each of the 64 squares
     */
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
    
    /**
     * Generates 64 length {@link ArrayList} of {@link Long} objects, each representing a anti-diagonal
     * ray for each of the 64 board squares.
     * 
     * <p>
     * Used to generate sliding piece moves for the Bishop and Queen as their paths can be blocked
     * by friendly or opponent pieces.
     * @return An ArrayList<Long> of anti-diagonal raymasks centered on each of the 64 squares
     */
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
    
    //#endregion-------------------------------------------------------------------------------------------------------------------------------

    //#region Mask generation-----------------------------------------------------------------------------------------------------------------
    /**
     * Generates an occupancy mask for the provided player's pieces.
     * 
     * @param piece Can be any int just so long as the sign of the int corresponds to the player;
     * positive for white, negative for black.
     * @return Primitive long value representing occupancy mask for the specified player's pieces.
     */
    public long generatePlayerPieceMask(int piece){
        long retMask = 0L;
        //Rank
        for (int i = 0; i < 8; i++){
            //File
            for (int j = 0; j < 8; j++){
                if (piece < 0){
                    if (this.board[i][j] < 0){
                        int bitPos = (i * 8) + j;
                        retMask |= (1L << (63 - bitPos));
                    }
                }
                else {
                    if (this.board[i][j] > 0){
                        int bitPos = (i * 8) + j;
                        retMask |= (1L << (63 - bitPos));
                    }
                }
            }
        }

        return retMask;
    }

    /**
     * Generates a bitmask representing a 'blocked capture' for the Rook move pattern (Horizontal + vertical) centered 
     * on the provided position.
     * <p>
     * Used in the generation of Queen and Rook moves. 'Blocked capture' means the mask allows the capture of the first 
     * encountered blocking piece regardless of ownership, but can move no further. Takes into account blocking piece locations on the Board object
     * calling the method, and blocks the rays accordingly. 
     * Uses {@link hypQuint} to calculate the blocked squares of the rays, and performs a bitwise OR
     * operation to combine the vertical and horizontal ray.
     * </p>
     * @param position denotes the specific square that the mask is generated for, corresponding to RLERF encoding (see <a href="#RLERF">RLERF encoding</a> in {@link Board})
     * @return Primitive long value representing the 'blocked capture' straight ray mask for provided square, in RLERF encoding.
     */
    public long generateValidStraightRayMask(int position){
        long retBoard = 0L;
        long pieceMask = (1L << (63 - position));

        retBoard |= hypQuint(this.bitState, HORIZONTAL_RAY.get(position / 8), pieceMask);
        retBoard |= hypQuint(this.bitState, VERTICAL_RAY.get(position % 8), pieceMask);

        return retBoard;
    }

    /**
     * Generates a bitmask representing a 'blocked capture' for the Bishop move pattern (diag + anti-diag) centered 
     * on the provided position.
     * <p>
     * Used in the generation of Queen and Bishop moves. 'Blocked capture' means the mask allows the capture of the first 
     * encountered blocking piece regardless of ownership, but can move no further. Takes into account blocking piece locations on the Board object
     * calling the method, and blocks the rays accordingly. 
     * Uses {@link hypQuint} to calculate the blocked squares of the rays, and performs a bitwise OR
     * operation to combine the diag and anti-diag ray.
     * </p>
     * @param position denotes the specific square that the mask is generated for, corresponding to RLERF encoding (see <a href="#RLERF">RLERF encoding</a> in {@link Board})
     * @return Primitive long value representing the 'blocked capture' diagonal ray mask for provided square, in RLERF encoding.
     */
    public long generateValidDiagonalRayMask(int position){
        long retBoard = 0L;
        long pieceMask = (1L << (63 - position));

        retBoard |= hypQuint(this.bitState, DIAGONAL_MOVE.get(position), pieceMask);
        retBoard |= hypQuint(this.bitState, ANTI_MOVE.get(position), pieceMask);

        return retBoard;
    }

    /**
     * Generates a bitmask representing a 'blocked capture' for the Queen move pattern (straight + diag) centered 
     * on the provided position.
     * <p>
     * Used in the generation of Queen moves. 'Blocked capture' means the mask allows the capture of the first 
     * encountered blocking piece regardless of ownership, but can move no further. Takes into account blocking piece locations on the Board object
     * calling the method, and blocks the rays accordingly. 
     * Uses {@link generateValidDiagonalRayMask} and {@link generateValidStraightRayMask} to generate bishop and rook masks,
     * and performs a bitwise OR operation to combine all four rays (vertical, horizontal, diagonal, antidiagonal)
     * </p>
     * 
     * @param position denotes the specific square that the mask is generated for, corresponding to RLERF encoding (see <a href="#RLERF">RLERF encoding</a> in {@link Board})
     * @return long value representing the 'blocked capture' all ray mask for provided square, in RLERF encoding.
     */
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
    /**
     * Generates an attack mask representing the valid 'moves' for a given piece on a given square.
     * 
     * <p>
     * Note: Pawn moves are handled separately in {@link generatePawnMoveMask}, and pawn attacks are generated here using 
     * {@link generatePawnAttackMask}, which handles the differing move/attack logic.
     * </p>
     * <p>
     * Primary usage of this method is for both movement AND attack/captures, as both move a piece from one square to another.
     * First the base mask corresponding with the provided piece and origin square is generated, and undergos an AND operation 
     * with an inversed friendly occupancy mask (mask &amp; ~occ) to invalidate self-captures (alternatively mask ^ (mask &amp; occ)). 
     * If generating the mask for a King, all king moves into opponent piece vision are invalidated (mask &amp; ~vision).
     * </p>
     * 
     * <p>
     * This method does not account for whether the moves would be legal under check, it does not invalidate
     * self-checking moves through discovered check, and it does not provide legal castling moves for the king.
     * These operations are done in {@link generateValidMoves}.
     * </p>
     *
     * @param piece primitive int representing piece value and ownership through signedness
     * @param origin primitive int representing the square on which the piece resides; an 
     * index refering to the bit position of the RLERF encoded bitboard representation (see <a href="#RLERF">RLERF encoding</a> in {@link Board}).
     * @return a primitive long value representing the bitmask of all available move destination
     * squares, including captures on the opponent. RLERF encoded.
     */
    public long generatePieceAttackMask(int piece, int origin){
        long retMask = 0L;
        long friendlyOcc = (piece < 0) ? generatePlayerPieceMask(-1) : generatePlayerPieceMask(1);
        long opponentVision = (piece < 0) ? generatePieceVision(1) : generatePieceVision(-1);
        long temp;
        switch (Math.abs(piece)){
            case 1:
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
                long kingPosMask = (1L << (63 - origin));
                // If king is currently in check, we must remove the king's position from the valid moves as XORing with opponent vision in this case
                // will flip the king position to 1
                if ((opponentVision & kingPosMask) != 0){
                    retMask |= (KING_MOVE.get(origin) & ~friendlyOcc) & ~opponentVision & ~kingPosMask;
                }
                else {
                    retMask |= (KING_MOVE.get(origin) & ~friendlyOcc) & ~opponentVision;
                }
                break;
        }
        return retMask;
    }

    /**
     * Generates the move mask of a pawn given its origin square and ownership.
     * 
     * <p>
     * Determines valid destination squares depending on the ownership of the pawn,
     * whether the destination square is occupied and if its the pawn's first move.
     * </p>
     * @param playerSign int representing piece value and ownership through signedness, positive for white, negative for black.
     * @param origin represents the position of the piece as the index of the square in a RLERF encoded bitboard (see <a href="#RLERF">RLERF encoding</a> in {@link Board}).
     * @return long representing valid move squares for the supplied piece, ownership, and origin square, in RLERF encoding.
     */
    public long generatePawnMoveMask(int playerSign, int origin){
        int rank = origin / 8;

        if ((playerSign > 0) && (rank == 1)){
            long above = (1L << (63 - (origin + 8)));
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
            long below = (1L << (63 - (origin - 8)));
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

    /**
     * Generates the attack/capture mask of a pawn given its origin square and ownership.
     * 
     * <p>
     * Determines valid destination squares depending on the ownership of the pawn,
     * whether the destination square is occupied with an enemy piece, and whether an en passent is possible.
     * </p>
     * @param playerSign int representing piece value and ownership through signedness, positive for white, negative for black.
     * @param origin represents the position of the piece as the index of the square in a RLERF encoded bitboard (see <a href="#RLERF">RLERF encoding</a> in {@link Board}).
     * @return long representing valid move squares for the supplied piece, ownership, and origin square, in RLERF encoding.
     */
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

    /**
     * Generates a bitmask representing the provided player's pieces' vision.
     * 
     * <p>
     * Similar to generatePieceAttackMask but with cut down logic. Scans the calling Board object's boardState
     * attribute for pieces belonging to the player, and generates their vision using the corresponding sliding
     * and non-sliding piece mask generators. This vision mask undergoes bitwise OR with the return value, and
     * after scanning the board the accumulated return value is returned.
     * </p>
     * 
     * <p>
     * Uses the basic rays and move masks without occupancy checking or king move vision checking since:
     * <ul>
     * <li> King cannot move/capture into friendly piece/enemy piece in vision</li>
     * <li> King cannot castle if any piece exists in castling path</li>
     * <li> Vision checking: King is not actually moving, just preventing enemy king from moving</li>
     * </ul>
     * </p>
     * @param playerSign int representing piece value and ownership through signedness, positive for white, negative for black.
     * @return long representing the player's piece vision, in RLERF encoding (see <a href="#RLERF">RLERF encoding</a> in {@link Board}).
     */
    public long generatePieceVision(int playerSign){
        long retMask = 0L;

        // Possible attack moves for all pieces constitute vision on the king and castling path
        // No self-check avoidance required
        // For king: No vision avoidance required
        for (int i = 0; i < 8; i++){
            for (int j = 0; j < 8; j++){
                int bitInd = (i * 8) + j;
                int piece = this.board[i][j];
                if ((playerSign > 0) && (piece > 0)){
                    switch (piece){
                        // Pawn attack
                        case 1: 
                            // No occupancy check required for vision
                            // If enemy piece exists, the enemy king cannot move there, if it doesn't exist, the square is in vision
                            // If a piece exists, they cannot castle anyways, if it doesn't exist, they can't castle as its in vision
                            // Additionally, if friendly piece is captured by enemy, it is now in vision (prevents king captures)
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
    /**
     * Generates a bitmask representing a straight ray between two squares on the chess board.
     * 
     * <p>
     * Determines what type of ray based on provided origin and destination, then "builds" a 
     * ray by setting each bit and incrementing the index according to the ray direction until
     * it reaches the destination, which is not set.
     * </p>
     * @param origin represents the origin square of the ray as the index of the square in a RLERF encoded bitboard (see <a href="#RLERF">RLERF encoding</a> in {@link Board}).
     * @param destination represents the destination square of the ray as the index of the square in a RLERF encoded bitboard.
     * @return long value representing the isolated raymask, including the origin square but not the destination square, in RLERF format.
     */
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
    /**
     * Generates a bitmask representing possible en-passent attacks for the given player and pawn square.
     * 
     * <p>
     * Mask is generated by checking each possible pawn attack square for the provided origin square for an enemy pawn
     * that had just moved last turn two squares forward.
     * </p>
     * @param playerSign int whose sign represents the player owning the pawn, positive for white, negative for black
     * @param origin represents the position of the piece as the index of the square in a RLERF encoded bitboard (see <a href="#RLERF">RLERF encoding</a> in {@link Board}).
     * @return long value representing a bit mask of available en passent moves, in RLERF format. Returns 0 if there are no 
     * eligble en passent moves
     */
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
            int rightPiece = this.board[rank][file + 1];
            int rightSquare = (rank * 8) + file + 1;
            //       If piece == pawn                  Check correct pawn,           2 squares 'forward'
            if ((rightPiece == opponentPawn) && (lastMoveDestBit == rightSquare) && (lastMoveRankDiff > 1)){
                retMask |= (1L << (63 - (origin + rightAttackShift))); // Return with the right-forward attack mask
            }
            
        }
        // Right border, only check left EP
        else if (file == 7){
            int leftPiece = this.board[rank][file - 1];
            int leftSquare = (rank * 8) + file - 1;

            if ((leftPiece == opponentPawn) && (lastMoveDestBit == leftSquare) && (lastMoveRankDiff > 1)){
                retMask |= (1L << (63 - (origin + leftAttackShift))); // Return with the left-forward attack mask
            }
        }
        // Middle, check both side EP
        else {
            int leftPiece = this.board[rank][file -1];
            int rightPiece = this.board[rank][file + 1];

            int leftSquare = (rank * 8) + file - 1;
            int rightSquare = leftSquare + 2;

            if ((leftPiece == opponentPawn) && (lastMoveDestBit == leftSquare) && (lastMoveRankDiff > 1)){
                retMask |= (1L << (63 - (origin + leftAttackShift))); // Add the left-forward attack mask
            }
            else if ((rightPiece == opponentPawn) && (lastMoveDestBit == rightSquare) && (lastMoveRankDiff > 1)){
                retMask |= (1L << (63 - (origin + rightAttackShift))); // Add the right-forward attack mask
            }

        }

        return retMask;
    }
    
    
    //#endregion--------------------------------------------------------------------------------------------------------------------------

    //#region Move list generation-----------------------------------------------------------------------------------------------------------
    // Move generation for FORCING check states
    // Must also account for Self-checking as this is a move generator
    // Returns list of valid piece moves as int[]{piece, origin, destination}
    // If no moves exist, then checkmate
    /**
     * Generates all possible check evasion moves including king moves, piece blocking, and piece capture <br>
     * of the checking piece.
     * 
     * <p>
     * First determines whether it is a single check or more than a single check. If only one check then the<br>
     * moves list for all pieces are returned. Otherwise only king moves are returned as king moves are the<br>
     * only way to escape double/triple(rare) checks. Possible king moves are generated using {@link generatePieceAttackMask}<br>
     * which already invalidates king moves into enemy vision. Piece check evasion moves are filtered by generating all<br>
     * possible moves for each piece, and selecting the moves whose destination square lies on the 'checking path'<br>
     * between the checking piece and the king, or the square on which the single checking piece resides. Finally<br>
     * the moves that result in a self-check through discovered check are removed from the returned move lists.
     * </p>
     * 
     * <p>
     * The format of the returned ArrayList&lt;int[]> is that each entry in the ArrayList is a separate move described by the <br>
     * contained int[]{piece, origin, destination}. This information is then later used to create a {@link Move} object.
     * </p>
     * 
     * @param playerSign int whose sign represents the player owning the pawn, positive for white, negative for black.
     * @return an ArrayList&lt;int[]> containing the information for each valid check evasion move.
     */
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
                    evasionPiece = this.board[i][j];
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
                        if (combinedMask != 0){
                            for (int pos : getSetBitPositions(combinedMask)){
                                retArray.add(new int[] {evasionPiece, square, pos});
                            }
                        }
                    }
                }
            }    
        }// endif
        
        // Remove self-checking positions
        ArrayList<int[]> retArrayFinal = removeSelfCheckingMoves(playerSign, retArray);
        return retArrayFinal;
    }

    // Returns an ArrayList<int[]> with int[] containing pieceID, origin, destination
    // These are all existing checks against player with playerSign
    /**
     * Generates an {@link ArrayList} of current checks for the provided player's opponent using the boardState <br>
     * of the calling {@link Board} object.
     * 
     * <p>
     * The returned moves aren't actually valid moves, they just denote which piece(s) are currently<br>
     * checking the provided player's king. This is done by isolating the player's king position and<br>
     * iterating through the associated board to search for opponents pieces whose attack path includes the <br>
     * king's position, resulting in a check. Self-check filtering is not required here as the moves can't <br>
     * actually be "played" on the board.
     * </p>
     * 
     * <p>
     * The format of the returned ArrayList&lt;int[]> is that each entry in the ArrayList is a separate move described by the <br>
     * contained int[]{piece, origin, destination}. This information is used to describe all current checks on the player by <br>
     * their opponent.
     * </p>
     * 
     * @param playerSign int whose sign represents the player being checked, whose opponents checking moves<br>
     * are returned in this method.
     * @return an ArrayList&lt;int[]> containing all current opponent checking moves (effectively checks).
     */
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
                int piece = this.board[i][j];
                if (((piece < 0) && (playerSign > 0)) || ((piece > 0) && (playerSign < 0))){
                    if ((kingMask & generatePieceAttackMask(piece, square)) != 0){
                        retChecks.add(new int[]{piece, square, kingPos});
                    }
                }
            }
        }
        return retChecks;
    }

    //#region Unused overload of getOpponentChecks
    // Overloaded parameters to check for checks in candidate board states, not the current board state
    // /**
    //  * An overloaded static method that generates an {@link ArrayList} of current checks for the provided player's<br>
    //  * opponent using the provided boardState parameter instead of the boardState of the calling {@link Board} object.
    //  * 
    //  * <p>
    //  * The returned moves aren't actually valid moves, they just denote which piece(s) are currently<br>
    //  * checking the provided player's king. This is done by isolating the player's king position and<br>
    //  * iterating through the provided board to search for opponents pieces whose attack path includes the <br>
    //  * king's position, resulting in a check. Self-check filtering is not required here as the moves can't <br>
    //  * actually be "played" on the board.
    //  * </p>
    //  * 
    //  * <p>
    //  * The format of the returned ArrayList<int[]> is that each entry in the ArrayList is a separate move described by the <br>
    //  * contained int[]{piece, origin, destination}. This information is used to describe all current checks on the player by <br>
    //  * their opponent.
    //  * </p>
    //  * 
    //  * @param playerSign int whose sign represents the player being checked, whose opponents checking moves<br>
    //  * are returned in this method.
    //  * @param boardState the board to use to check for king position and piece checks on the king. Consists of<br>
    //  * of a 2D primitive int array containing integer values -6 to 6 representing players' pieces, with 0 <br>
    //  * representing no piece.
    //  * @return an ArrayList<int[]> containing all current opponent checking moves based on the provided board
    //  */
    // public ArrayList<int[]> getOpponentChecks(int playerSign, int[][] boardState){
    //     ArrayList<int[]> retChecks = new ArrayList<>();
    //     int kingPos = findKingBitPosition(playerSign);
    //     long kingMask = (1L << (63 - kingPos));
    //     for (int i = 0; i < 8; i++){
    //         for (int j = 0; j < 8; j++){
    //             int square = (i * 8) + j;
    //             // If player and piece are opponents
    //             // Piece sign never changes throughout, so only one of these ORs will ever be true
    //             int piece = boardState[i][j];
    //             if (((piece < 0) && (playerSign > 0)) || ((piece > 0) && (playerSign < 0))){
    //                 if ((kingMask & generatePieceAttackMask(piece, square)) != 0){
    //                     retChecks.add(new int[]{piece, square, kingPos});
    //                 }
    //             }
    //         }
    //     }
    //     return retChecks;
    // }
    //#endregion

    // Returns zero length array, meaning checkmate or stalemate
    /**
     * Generates an {@link ArrayList} of int[] containing all valid moves for the associated {@link Board}<br>
     * object, for the given player.
     * 
     * <p>
     * This is the final validator for possible moves on a given board for a given player. Accounts for self-checking<br>
     * moves, forced check evasion moves, and valid castling moves. Returns a zero length ArrayList when no moves are<br>
     * possible, indicating either checkmate or stalemate. These conditions are accounted for in {@link evaluateGameEndConditions}.
     * </p>
     * <p>
     * The format of the returned ArrayList&lt;int[]> is that each entry in the ArrayList is a separate move described by the <br>
     * contained int[]{piece, origin, destination}.
     * </p>
     * 
     * @param playerSign int whose sign represents the player whose valid moves are being generated. Positive for white<br>
     * and negative for black.
     * @return an ArrayList&lt;int[]> containing all valid moves for the given player and associated Board object. Returns<br>
     * a zero length array in the case of checkmate or stalemate.
     */
    public ArrayList<int[]> generateValidMoves(int playerSign){
        ArrayList<int[]> retArray = new ArrayList<>();
        boolean shortCastleRights = (playerSign > 0) ? this.whiteShort : this.blackShort;
        boolean longCastleRights = (playerSign > 0) ? this.whiteLong : this.blackLong;
        
        if (this.state == BOARD_STATE.CHECK){
            // Already filters for self-checking moves
            return generateCheckEvasionMoves(playerSign);
        }
        else {
            // Generate possible moves using piece move masks, not checking for self-check
            for (int i = 0; i < 8; i++){
                for (int j = 0; j < 8; j++){
                    int square = (i * 8) + j;
                     int piece = this.board[i][j];

                    if ((piece * playerSign) > 0){
                        // Check if piece is a king, and if the player can castle at all
                        if ((piece == 6 || piece == -6) && (shortCastleRights || longCastleRights)){
                            // Add valid castling moves
                            for (int[] move : generateValidCastlingMoves(playerSign)){
                                retArray.add(move);
                            };
                        }

                        for (int pos : getSetBitPositions(generatePieceAttackMask(piece, square))){
                            retArray.add(new int[] {piece, square, pos});
                        }
    
                        // If pawn also add non-attack moves
                        if (Math.abs(piece) == 1){
                            for (int pos : getSetBitPositions(generatePawnMoveMask(piece,square))){
                                retArray.add(new int[] {piece, square, pos});
                            }
                        }
                    }

                }
            }

            // Filter out self-checking moves
            retArray = removeSelfCheckingMoves(playerSign, retArray);
        }

        return retArray;
    }

    // Returns valid castling based on:
    //  - Board.white/blackCanCastle boolean (King or rooks have moved);
    //  - Paths not blocked by vision or occupancy
    //  - Rooks are in the proper places
    //  - Does NOT check for previously moved king/rooks (Singified by boolean)
    /**
     * Generates an {@link ArrayList} of int[] representing valid castling moves on the associated {@link Board}<br>
     * object for the given player.
     * 
     * <p>
     * Basic king move patterns for long and short castles are defined as masks in {@link Board}. This method<br>
     * simply checks king position, castling rights, and enemy vision to determine whether these are valid for the<br>
     * current board position for the given player. If the king is not in the right spot, the castling right doesn't<br>
     * exist, or if enemy vision crosses the castling path, then that specific castling move is invalid and not <br>
     * included in the return value.
     * </p>
     * 
     * <p>
     * Castling rights are stored as boolean attributes in {@link Board} and maintained elsewhere in {@link updateState}
     * </p>
     * 
     * <p>
     * The format of the returned ArrayList&lt;int[]> is that each entry in the ArrayList is a separate move described by the <br>
     * contained int[]{piece, origin, destination}. This information is used to describe all current checks on the player by <br>
     * their opponent.
     * </p>
     * 
     * @param playerSign int whose sign represents the player that castling moves are generated for, positive for white,<br>
     * negative for black.
     * @return an ArrayList&lt;int[]> array containing all valid castling moves for the given player on the associated board.
     */
    public ArrayList<int[]> generateValidCastlingMoves(int playerSign){
        ArrayList<int[]> retArray = new ArrayList<>();
        // If the player whose turn it is can't castle, return an empty array (should not get here anyway)
        if (
            ((playerSign > 0) && !(whiteLong) && !(whiteShort)) || 
            ((playerSign < 0) && !(blackLong) && !(blackShort))){
            return retArray;
        }

        int opponentSign;       // The sign of the opponent player, -1 for white 1 for black
        int friendlyRook;       // Player-specific rook piece identifier
        int longRookSquare;     // Square where the rook on the long castle side resides 
        int shortRookSquare;    // Square where the rook on the short castle side resides
        int kingBitPos = findKingBitPosition(playerSign);   // Players' CURRENT king position
        int kingSquare;         // Player-specific king starting square
        int kingPiece;          // Player-specific king piece identifier
        int longCastleDest;     // King's destination square for long castle
        int shortCastleDest;    // King's destination square for short castle
        long shortCastleMask;   // King's path for short castle
        long longCastleMask; // King's path for long castle
        long longRookMask; // Represents the square to the right of the long rook, needs to be unoccupied but not invisible
        // Set values based on player sign
        if (playerSign > 0){
            opponentSign = -1;
            friendlyRook = 4;
            longRookSquare = 0;
            shortRookSquare = 7;
            kingPiece = 6;
            longCastleDest = 2;
            shortCastleDest = 6;
            kingSquare = 4;
            shortCastleMask = W_CASTLE_SHORT;
            longCastleMask = W_CASTLE_LONG;
            longRookMask = (1L << (63 - 1));
        }
        else {
            opponentSign = 1;
            friendlyRook = -4;
            longRookSquare = 56;
            shortRookSquare = 63;
            kingPiece = -6;
            longCastleDest = 58;
            shortCastleDest = 62;
            kingSquare = 60;
            shortCastleMask = B_CASTLE_SHORT;
            longCastleMask = B_CASTLE_LONG;
            longRookMask = (1L << (63 - 57));
        }


        // Check short castle validity
        long opponentVision = generatePieceVision(opponentSign);
        // Determine long/short castling validity
        // Check ROOK IS ON SQUARE && CASTLING PATH NOT IN OPPONENT VISION && CASTLING PATH NOT BLOCKED && KING ON PROPER SQUARE   
        boolean canShortCastle = (
            (this.board[shortRookSquare / 8][shortRookSquare % 8] == friendlyRook) && 
            ((opponentVision & shortCastleMask) == 0) && 
            ((this.bitState & shortCastleMask) == 0) &&
            (kingSquare == kingBitPos)
        );
        
        // Long castle must check also that the square to the right of the Long rook is NOT occupied by pieces
        // No need to check for vision as rooks can castle through enemy vision
        boolean canLongCastle = (
            (this.board[longRookSquare / 8][longRookSquare % 8] == friendlyRook) && 
            ((opponentVision & longCastleMask) == 0) && 
            ((this.bitState & (longCastleMask | longRookMask)) == 0) &&
            (kingSquare == kingBitPos)
        );

        if (canShortCastle) {retArray.add(new int[]{kingPiece, kingBitPos, shortCastleDest});}
        if (canLongCastle) {retArray.add(new int[] {kingPiece, kingBitPos, longCastleDest});}

        return retArray;
    }

    //#endregion-----------------------------------------------------------------------------------------------------------------

    //#region Move Object creation------------------------------------------------------------------------------------------------------------------------
    /**
     * Generates a valid {@link Move} object given the move information stored in the provided int[].
     * 
     * <p>
     * Utilizes the ArrayList&lt;int[]> move lists generated by {@link generateValidMoves} to create a valid Move object<br>
     * Determines the Move type (see {@MOVE_TYPE}) of the move by checking the associated {@link Board} object for destination<br>
     * square occupancy and the last played move.
     * </p>
     * 
     * <p>
     * The format of the returned ArrayList&lt;int[]> is that each entry in the ArrayList is a separate move described by the <br>
     * contained int[]{piece, origin, destination}. This information is then later used to create a {@link Move} object.
     * </p>
     * @param move int[] containing the {piece, origin, destination} of the move.
     * @return Fully qualified {@link Move} object ready to be 'played' on the board
     */
    public Move createMove(int[] move){
        // EP attack (Move to empty square but still taking piece)
        // Attack? (Move to enemy occupied square)
        // Move? (Move to empty square)
        //  - Promotion (Pawn move to 'end' rank)
        //  - Castle?   (King move of 2 spaces in either direction)
        int piece = move[0];
        int origin = move[1];
        int originFile = origin % 8;
        int dest = move[2];
        int destRank = dest / 8;
        int destFile = dest % 8;
        int destOccPiece = this.board[destRank][destFile];
        int promotionRank = (piece > 0) ? 7 : 0;

        // IF piece is pawn AND its a pawn attack AND the destination square is empty, then EN PASSENT
        if ((Math.abs(piece) == 1) && (originFile != destFile) && (destOccPiece == 0)){
            return new Move(piece, origin, dest, MOVE_TYPE.EN_PASSENT);
        }
        // Attack moves
        else if (destOccPiece != 0){
            // Pawn promotion attack
            if ((Math.abs(piece) == 1) && (destRank == promotionRank)){
                return new Move(piece, origin, dest, MOVE_TYPE.PROMOTE_ATTACK);
            }
            // Regular attack
            else {
                return new Move(piece, origin, dest, MOVE_TYPE.ATTACK);
            }
        }
        // Move moves
        else {
            // Castling
            if (Math.abs(piece) == 6){
                // Short castle
                if ((destFile - originFile) == 2){
                    return new Move(piece, origin, dest, MOVE_TYPE.CASTLE_SHORT);
                }
                // Long castle
                else if ((destFile - originFile) == -2){
                    return new Move(piece, origin, dest, MOVE_TYPE.CASTLE_LONG);
                }
            }
            // Pawn promotion move
            else if ((Math.abs(piece) == 1) && (destRank == promotionRank)){
                return new Move(piece, origin, dest, MOVE_TYPE.PROMOTE_MOVE);
            }
            // All other moves

            return new Move(piece, origin, dest, MOVE_TYPE.MOVE);
        }
    }
    //#endregion--------------------------------------------------------------------------------------------------------------------------------------------------

    //#region Board utility--------------------------------------------------------------------------------------------------------------------------------------
    
    /**
     * Converts a provided primitve long to a 64 length string representing the bit pattern of an int64.
     * 
     * <p>
     * First character of string represents the MSB, and last character represents the LSB.
     * </p>
     * @param num primitive long to be converted to a string representation.
     * @return a 64-length {@link String} representing the 64 bit pattern of the provided long as an int64.
     */
    public static String longToString(long num){
        return String.format("%64s", Long.toBinaryString(num)).replace(' ', '0');
    }

    /**
     * Converts a provided int[][] board array to a primitive long bitboard occupancy representation.
     * 
     * <p>
     * Iterates through the board in RLERF order (see <a href="#RLERF">RLERF encoding</a> in {@link Board}) and sets the associated bit in the long<br>
     * to 1 if a piece exists on that square, 0 otherwise.
     * </p>
     * 
     * @param board an int[][] representing a {@link boardState}, containing values -6 to 6 representing pieces.
     * @return a primitive long representing the occupancy mask/bitboard of the provided board.
     */
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

    /**
     * Generates an int[][] representing a chess board in its game start state with pieces in their starting positions.
     * 
     * @return int[][] representing a chess board with all pieces in their starting positions.
     */
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

    /**
     * Deep-clones a provided int[][] representation of a {@link boardState}.
     * @param board the int[][] board that is to be deep cloned.
     * @return a deep cloned int[][] copy of the provided board.
     */
    public static int[][] deepCloneBoard(int[][] board){
        int[][] retBoard = new int[8][8];

        for (int i = 0; i < 8; i++){
            retBoard[i] = board[i].clone();
        }

        return retBoard;
    }

    /**
     * Prints a visual represntation of a bitboard to System.out in proper orientation.
     * 
     * <p>
     * Proper orientation is a1 at the bottom left, and h8 at the top right. Prints to System.out. Mainly<br>
     * used for testing and debugging, but System.out can be redirected if needed for logging, etc.
     * </p>
     * @param bitboard primitive long representing a bitboard/occupancy mask to be printed to System.out.
     */
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

    /**
     * Prints a visual representation of a board to System.out using UTF-8 chess emojis to symbolize pieces,<br>
     * chess board squares, and file and rank labels.
     * 
     * <p>
     * Utilizes UTF chess emoji codes in {@link CHESS_EMOJI}. Prints properly to IDE consoles but native<br>
     * windows terminal does not support these codes. For IDE consoles may have to set the active code page by<br>
     * executing 'chcp 65001' for UTF-8 (on windows).
     * </p>
     * @param board the int[][] board to be visualized and printed to System.out
     */
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
        }
        sbOuter.append("   A   B   C   D   E   F   G   H");
        System.out.println();
        System.out.print(sbOuter.toString());
        System.out.println();
    }

    /**
     * A non-static version of {@link Board#boardVisualize(int[][])}. Prints a visual representation of the<br>
     * associated {@link boardState} to System.out using UTF-8 chess emojis to symbolize pieces, chess board<br>
     * squares, and file and rank labels.
     * 
     * <p>
     * Utilizes UTF chess emoji codes in {@link CHESS_EMOJI}. Prints properly to IDE consoles but native<br>
     * windows terminal does not support these codes. For IDE consoles may have to set the active code page by<br>
     * executing 'chcp 65001' for UTF-8 (on windows).
     * </p>
     */
    public void boardVisualize(){
        StringBuilder sbInner = new StringBuilder();
        StringBuilder sbOuter = new StringBuilder();


        for (int i = 0; i < 8; i++){
            sbInner.setLength(0);
            sbInner.append((i + 1) + " ");
            for (int j = 0; j < 8; j++){
                sbInner.append("[" + CHESS_EMOJI.get(this.board[i][j]) + " ]");
            }
            sbInner.append("\n");
            sbOuter.insert(0, sbInner.toString());
        }
        sbOuter.append("   A   B   C   D   E   F   G   H");
        System.out.println();
        System.out.print(sbOuter.toString());
        System.out.println();
    }

    /**
     * Filters out the self-checking moves in the provided {@link ArrayList} of int[], returns all remaining<br>
     * valid non self-checking moves.
     * 
     * <p>
     * First deep clones the associated calling {@link Board} object's {@link boardState} attribute. Then<br>
     * 'plays' the candidate move on that cloned board. An occupancy mask is then generated from this 'played' board.<br>
     * A 'fresh' Board object is created, and its {@link boardState} and {@link bitState} attributes <br>
     * are replaced with the 'played' cloned ones. We then invoke {@link getOpponentChecks} on this new board to <br>
     * detect whether the candidate move would result in a self check. If the returned list is lenghth zero, we <br>
     * know there are no checks and we add the move to the return list. If the returned list is not empty the move<br>
     * is discarded.
     * </p>
     * 
     * <p>
     * As we are only filtering for CHECKS, only the boardState and bitState need to be set on the fresh board.
     * </p>
     * 
     * <p>
     * The format of the returned ArrayList&lt;int[]> is that each entry in the ArrayList is a separate move described by the <br>
     * contained int[]{piece, origin, destination}.
     * </p>
     * 
     * @param playerSign int whose sign represents the player that we are filtering moves for. Moves resulting<br>
     * in checks on this player are invalidated.
     * @param movesArray ArrayList&lt;int[]> of candidate moves to be filtered for self-checking moves.
     * @return ArrayList&lt;int[]> containing all moves that do not result in self-check.
     */
    public ArrayList<int[]> removeSelfCheckingMoves(int playerSign, ArrayList<int[]> movesArray){
        ArrayList<int[]> retArray = new ArrayList<>();
        Board futureBoard = new Board();
        
        
        // Remove moves resulting in self-check
        int[][] realBoard = getBoard();

        for (int[] move : movesArray){
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
                int epRank = (evasionPiece > 0) ? destRank - 1 : destRank + 1; // Opponent pawn is either above or below the capturing pawn

                candidateBoard[epRank][destFile] = 0; // Set pawn that was en-passented to 0
                candidateOcc = boardToBitboard(candidateBoard); // Overwrite occupancy
            }
            
            // Switch for checking
            futureBoard.board = candidateBoard;
            futureBoard.bitState = candidateOcc;
            // Ensure move doesn't result in discovered self-check
            if (futureBoard.getOpponentChecks(playerSign).size() == 0){
                retArray.add(move);
            }

        }
        return retArray;
        
    
    }
    
    /**
     * Returns the position of the specified player's king on the associated {@link Board} object, in the form of an index <br>
     * of the corresponding bit in the RLERF encoded (see <a href="#RLERF">RLERF encoding</a> in {@link Board}) bitboard representation.
     *
     * <p>
     * Iterates through the associated {@link boardState} looking for the value 6 for white, or -6 for black.<br>
     * when found the method returns (rowIndex * 8) + fileIndex, which corresponds to the index of the square in <br>
     * the RLERF encoding of the board.
     * </p>
     * 
     * @param playerSign int whose sign denotes which player's king to locate.
     * @return int denoting the position of the king in the form of an index of the RLERF {@link Board} encoded board <br>
     * representation.
     */
    public int findKingBitPosition(int playerSign){
        int kingInt = (playerSign > 0) ? 6 : -6;
        for (int i = 0; i < 8; i++){
            for (int j = 0; j < 8; j++){
                if (this.board[i][j] == kingInt){
                    return (i * 8) + j;
                }
            }
        }
        // returns negative if not found
        return -1;
    }

    // Only one en passent destination square is possible
    /**
     * Generates a long RLERF board representation with any available en passent destination squares.
     * 
     * <p>
     * Differentiated from {@link generateEnPassentMask} in that this method looks for all possible <br>
     * en passent moves on all squares for the given player. Returned value will always be either zero<br>
     * if no en passent is possible, or a single bit flipped indicating the possible en passent. There<br>
     * can only ever be one en passent possible for either player.
     * </p>
     * @param playerSign int whose sign denotes which player's en passent moves are being generated.
     * @return long representing a bitmask of any en passent moves available for the provided player,<br>
     * in RLERF format (see <a href="#RLERF">RLERF encoding</a> in {@link Board}). Will only ever be 0 or have a single bit flipped.
     */
    public long getPlayerEPMask(int playerSign){
        int friendlyPawn = (playerSign > 0) ? 1 : -1;
        for (int i = 0; i < 8; i++){
            for (int j = 0; j < 8; j++){
                if (this.board[i][j] == friendlyPawn){
                    long temp = generateEnPassentMask(playerSign, ((i * 8) + j));
                    if (temp > 0){
                        return temp;
                    }
                }
            }
        }
        return 0L;
    }

    // Generates a random zobrist table using a Map to store piece ID values, corresponding to a 64 length 
    // long[] holding each unique hash for each corresponding board square bit 0 to 63
    /**
     * Generates a {@link Map} mapping each of the 12 piece values to a primitive long[64], whose contents<br>
     * represents a unique zobrist hash value for each chess board square for that specific piece.
     * 
     * <p>
     * Uses {@link ThreadLocalRandom} to generate the random values. The same piece with different owners counts<br>
     * as two unique pieces.
     * </p>
     * @return a 12 length Map&lt;Integer, long[64]> representing a unique hash value for each board square,<br>
     * for each piece. The order of the long[64] corresponds to the indices of a RLERF encoded board (see <a href="#RLERF">RLERF encoding</a> in {@link Board}).
     */
    public static Map<Integer, long[]> generateZobristTable(){
        // Each piece identifier is mapped to a 64 length int[], corresponding to each square on the board
        Map<Integer, long[]> retMap = new HashMap<>();

        int[] pieces = new int[]{-6, -5, -4, -3, -2, -1, 1, 2, 3, 4, 5, 6};

        for (int piece : pieces){
            // Generate fresh hashes for each square, for this current piece
            long[] temp = new long[64];
            for (int i = 0; i < 63; i++){
                temp[i] = ThreadLocalRandom.current().nextLong();
            }

            retMap.put(piece, temp);
        }

        return retMap;
    }

    /**
     * Generates an 8 length {@link Map} mapping each of the 8 possible EP destination files to a <br>
     * unique zobrist hash value.
     *
     * @return Map&lt;Integer, Long> mapping each file index Integer to a unique Long zobrist hash value.
     */
    public static Map<Integer, Long> generateEPHashTable(){
        Map<Integer,Long> retMap = new HashMap<>();
        int[] epFiles = new int[]{0, 1, 2, 3, 4, 5, 6, 7};

        for (int i : epFiles){
            retMap.put(i, ThreadLocalRandom.current().nextLong());
        }

        return retMap;
    }

    /**
     * Generates a zobrist hash using the {@link Board} attributes representing variables determining
     * a unique position (Piece position, turn, en passent, caslting rights).
     * 
     * <p>
     * Creates the hash by only 'adding' hashes that of states that are currently true, as if they turn<br>
     * false during the course of the game (ie castling) they would be removed at that time anyway from<br>
     * the zobrist hash anyway.
     * </p>
     * @return long value representing the zobrist hash of the current state of the current {@link Board}.
     */
    public long generateCurrentZobristHash(){
        long retLong = 0L;

        // Piece positions
        int piece;
        for (int i = 0; i < 8; i++){
            for (int j = 0; j < 8; j++){
                piece = this.board[i][j];
                if (piece != 0){
                    retLong ^= this.zobristTable.get(piece)[(i * 8) + j];
                }
            }
        }

        // Castling rights
        if (whiteShort){retLong ^= whiteShortHash;};
        if (whiteLong){retLong ^= whiteLongHash;};
        if (blackShort){retLong ^= blackShortHash;};

        if (blackLong){retLong ^= blackLongHash;};

        // En passent rights (If they exist)
        if (epHashHistory.size() > 0){
            retLong ^= epHashHistory.peek();
        }

        // Player turn
        if (whitesTurn){retLong ^= whiteTurnHash;};

        return retLong;
    }

    // Invoked after a player plays a move to calculate the starting board state for the OPPONENT
    // Updates zobrist hash based on affected squares of the last played move
    // For other pieces it looks at CURRENT board state, therefore this must be run AFTER moves are PLAYED
    /**
     * Updates the current zobrist hash based on the last played move. 
     * <p>
     * The moving piece is always removed from the origin square and the destination square behaviour depends<br>
     * on the type of move. 'Adding' or 'Removing' are done by XOR operations on the precomputed zobrist hashes<br>
     * for each piece/square combination.
     * </p>
     * <p>
     * This method is always called after {@link updateState} and also updates the zobrist hash <br>
     * with castling rights, en passent rights, and turn. Finally this zobrist is added to {@link zobristHistory}<br>
     * </p>
     * 
     * <p>
     * Note about castling rights: this method uses separate flags ({@link WSF}, {@link WLF}, {@link BSF}, {@link BLF}) <br>
     * that are maintained in {@link updateState} (which iteslf uses {@link setShortCastleRights} and <br>
     * {@link setLongCastleRights} to do so) to indicate whether the castling hash must be XOR'ed out of<br>
     * the zobrist hash.
     * <br>
     * This is necessary as simply checking if castling == false, would cause repeated adding and removing of the castling<br>
     * hash.<br>
     * ie. setting {@link whiteShort} = false can be done repeatedly and it is still false, however zobristHash ^= {@link whiteShortHash} <br>
     * repeatedly will not result in the same behaviour. Thus we need a flag that denotes the first time a castling right is set to false, <br>
     * at which point this method will XOR out the corresponding castling hash once and only once, as castling rights never come back in a game.
     * </p>
     * 
     * @see updateState
     * @see zobristHistory
     */
    public void updateZobrist(){
        Move lastMove = this.playedMoves.peek();
        int origin = lastMove.getOriginBit();
        int dest = lastMove.getDestBit();
        int piece = lastMove.getPiece();
        MOVE_TYPE lastType = lastMove.getType();
        int[][] lastBoard = this.boardHistory.peek();
        
        //#region Piece Position
        // Moves will always remove piece from origin
        this.zobristHash ^= this.zobristTable.get(piece)[origin];

        if (lastType == MOVE_TYPE.MOVE){
            this.zobristHash ^= this.zobristTable.get(piece)[dest]; // Place piece in destination
        }
        else if (lastType == MOVE_TYPE.ATTACK){
            int capturedPiece = lastBoard[dest/8][dest%8];
            this.zobristHash ^= this.zobristTable.get(capturedPiece)[dest]; // Remove captured piece from dest
            this.zobristHash ^= this.zobristTable.get(piece)[dest]; // Add new piece to dest

        }
        else if (lastType == MOVE_TYPE.EN_PASSENT){
            int rankAdjustment = (piece > 0) ? -8 : 8;
            int capturedPiece = (piece > 0) ? -1 : 1;

            this.zobristHash ^= this.zobristTable.get(capturedPiece)[dest + rankAdjustment]; // Remove captured pawn
            this.zobristHash ^= this.zobristTable.get(piece)[dest]; // Place moved piece
        }
        else if (lastType == MOVE_TYPE.CASTLE_LONG){
            int rook = (piece > 0) ? 4 : -4;
            // place king
            this.zobristHash ^= this.zobristTable.get(piece)[dest];

            // Remove and place rook
            this.zobristHash ^= this.zobristTable.get(rook)[dest - 2];
            this.zobristHash ^= this.zobristTable.get(rook)[dest + 1];
        }
        else if (lastType == MOVE_TYPE.CASTLE_SHORT){
            int rook = (piece > 0) ? 4 : -4;
            // place king
            this.zobristHash ^= this.zobristTable.get(piece)[dest];

            // Remove and place rook
            this.zobristHash ^= this.zobristTable.get(rook)[dest + 1];
            this.zobristHash ^= this.zobristTable.get(rook)[dest - 1];
        }
        else if (lastType == MOVE_TYPE.PROMOTE_ATTACK){
            int promotedPiece = this.board[dest/8][dest%8];
            int capturedPiece = lastBoard[dest/8][dest%8];
            this.zobristHash ^= this.zobristTable.get(capturedPiece)[dest];// remove captured piece
            this.zobristHash ^= this.zobristTable.get(promotedPiece)[dest];// add newly promoted piece
        }
        else if (lastType == MOVE_TYPE.PROMOTE_MOVE){
            int promotedPiece = this.board[dest/8][dest%8];
            this.zobristHash ^= this.zobristTable.get(promotedPiece)[dest]; // Place promoted piece
        }
        else{
            System.out.println("updateZorbist() ERROR: INVALID MOVE TYPE");
        }
        //#endregion
        //#region En Passent
        // En passent availability for next player
        long epMask = getPlayerEPMask(piece * -1);
        // If current player can EP
        long lastEpHash = (this.epHashHistory.size() != 0) ? this.epHashHistory.peek() : 0L;
        if (epMask > 0){
            int epFile = getSetBitPositions(epMask).get(0) % 8;
            long currentEpHash = epHashTable.get(epFile);
            // Incoming zobrist is not ep-enabled
            if (lastEpHash == 0){
                this.zobristHash ^= currentEpHash; // Add current
                this.epHashHistory.push(currentEpHash); // Add current ep hash to history
            }
            else {
                this.zobristHash ^= lastEpHash; // Remove last EP
                this.zobristHash ^= currentEpHash; // Add current EP
                this.epHashHistory.push(currentEpHash); // add to history
            }
        }
        // If no current EP possibility exists
        else {
            this.zobristHash ^= lastEpHash; // Either removes the last EP hash, or XOR's with 0 which doesn't matter
            this.epHashHistory.push(0L);
        }
        //#endregion     
        //#region Castling Rights
        if (this.WSF){
            this.zobristHash ^= this.whiteShortHash;
            this.WSF = false;
        }
        if (this.WLF){
            this.zobristHash ^= this.whiteLongHash;
            this.WLF = false;
        }
        if (this.BSF){
            this.zobristHash ^= this.blackShortHash;
            this.BSF = false;
        }
        if (this.BLF){
            this.zobristHash ^= this.blackLongHash;
            this.BLF = false;
        }
        //endregion        
        //#region Toggle turn
        this.zobristHash ^= this.whiteTurnHash;
        //#endregion
    
        // Add zobrist hash to zobrist history
        this.zobristHistory.push(this.zobristHash); // Should push a copy as it is a primitive long

    }
    
    // Checks the board to see if both players have insufficent material
    // Returns false if any pawns, rooks, or queens are detected
    // Returns false if either players material exceeds 9 points
    // Only returns true if after tallying all material, both players are still 9 or under
    /**
     * Checks current object for insufficent material on both sides.
     * 
     * <p>
     * Insufficient material is defined as:
     * <ol>
     * <li> King vs King </li>
     * <li> King + Knight vs King </li>
     * <li> King + Bishop vs King </li>
     * <li> King + Bishop vs King + Bishop </li>
     * <li> King + Bishop vs King + Knight </li>
     * </ol>
     * </p>
     * 
     * <p>
     * Programatically this is achieved by checking the presence of any Queens, Rooks, or Pawns first. If <br>
     * none are present, we determine if material for both sides is less than or equal to 9 (KB vs KB). Now the <br>
     * only sufficient material that is 9 pts or lower for both sides would be King+Knight vs King+Knight so if <br>
     * both players still have a knight then it is NOT insufficent, otherwise it is.
     * </p>
     * 
     * @return true if there is insuffient material, false otherwise.
     */
    public boolean checkInsufficientMaterial(){
        int whiteMat = 0;
        int blackMat = 0;
        boolean whiteKnight = false;
        boolean blackKnight = false;

        // Iterate through all board squares
        for (int i = 0; i < 8; i++){
            for (int j = 0; j < 8; j++){
                int piece = this.board[i][j];
                int pieceID = Math.abs(piece);
                // If any pawn, rook, or queen is detected return false
                // Detect knights for both sides
                if (pieceID == 1 || pieceID == 5 || pieceID == 4){
                    return false;
                }else if (piece == 2){
                    whiteKnight = true;
                }
                else if (piece == -2){
                    blackKnight = true;
                }
                // If its another piece, add to appropriate material count
                else {
                    if (piece < 0){
                        blackMat += piece;
                    }
                    else{
                        whiteMat += piece;
                    }

                    // After each addition, if white or black material reaches greater than 9, then its not insufficent material
                    if ((whiteMat > 9) && (blackMat < -9)){
                        return false;
                    }
                    
                }
            }
        }

        // If material for both is < 9, but both have knights, then technically its not insufficient material, otherwise it is
        if (whiteKnight && blackKnight){
            return false;
        }
        else {
            return true;
        }
    }
    
    // Basic halfclock updating method
    // Increments halfclock if non-capture, non-pawn move
    // resets halfclock otherwise
    // NO reverse functionality
    /**
     * Updates the current halfclock depending on the move that was just played on the current object.
     * 
     * <p>
     * Increments {@link halfClock} by one if the last played move was a non-pawn and non-capture move.<br>
     * Otherwise resets the halfclock to zero.
     * </p>
     */
    public void updateHalfClock(){
        MOVE_TYPE lastType = this.playedMoves.peek().getType();
        int lastPieceID = Math.abs(this.playedMoves.peek().getPiece());

        // If last move was a MOVE and NOT PAWN OR Castle
        if (((lastType == MOVE_TYPE.MOVE) && (lastPieceID != 1)) || (lastType == MOVE_TYPE.CASTLE_LONG) || (lastType == MOVE_TYPE.CASTLE_SHORT)){
            this.halfClock++;
        }
        else {
            this.halfClock = 0;
        }
    }

    /**
     * Checks whether the current half clock has reached the provided value.
     * 
     * <p>
     * Used for determining 50 move and 75 move rule trigger point.
     * </p>
     * 
     * @param n the half clock value at which this method will return true.
     * @return true if {@link halfClock} is greater than or equal to n, false otherwise
     */
    public boolean checkNMoveDraw(int n){
        return (this.halfClock >= n) ? true : false;
    }

    /**
     * Determines if the most recent zobrist hash has occured n or more times in the zobrist history,<br>
     * including the most recent occurence.
     * 
     * <p>
     * Used for determining the 3-fold and 5-fold repeat rule trigger point.
     * </p>
     * 
     * @param n the number of occurences at which this method will return true.
     * @return true if there are more than or equal to n occurences in the zobrist history, false otherwise.
     */
    public boolean checkNFoldRepeat(int n){
        if (this.zobristHistory.size() < n){
            return false;
        }
        else {
            int matches = 0;
            long current = this.zobristHistory.peek();
            
            for (long past : this.zobristHistory){
                if (current == past){
                    matches++;
                }
            }
            return (matches >= n) ? true : false;
        }
    }

    public boolean squareIsOccupied(int squareBit){
        long occ = this.bitState & (1L << (63 - squareBit));

        return occ != 0 ? true : false;
    }

    public int getPieceAtBitAddress(int bit){
        return this.board[bit/8][bit%8];
    }

    public void recordAlgebraicMove(ArrayList<int[]> lastValidMoves){
        //#region Extract move info and setup
        Move move = this.playedMoves.peek();
        MOVE_TYPE moveType = move.getType();
        int origin = move.getOriginBit();
        int destination = move.getDestBit();
        int piece = move.getPiece();
        StringBuilder sb = new StringBuilder();
        //#endregion

        if (moveType == MOVE_TYPE.CASTLE_LONG){
            this.algebraicHistory.add("O-O-O");
            return;
        }
        else if(moveType == MOVE_TYPE.CASTLE_SHORT){
            this.algebraicHistory.add("O-O");
            return;
        }
        
        //#region Piece determination
        sb.append(Move.PIECE_INT_TO_STRING.get(piece));
        //#endregion

        //#region Disambiguation determination
        // Determine if move requires disambiguation, if so rankDis and fileDis are set. If not they are left as empty strings
        // to be appended to the string builder.
        // Disambiguation possibilities:
        // Single: Two pieces on the same rank OR file can attack the same square
        // Double: Square can be attacked by pieces on the same RANK and FILE at the same time
        // Check for disambig
        // Only if >2 pieces of the same type can move to the target square
        // Pawn: SINGLE only
        // King: NO DISAMBIG
        // Bishop: Single/Double
        // Knight: Single/Double
        // Rook: SINGLE only
        // Queen: Single/Double
        String rankDis = "";
        String fileDis = "";
        boolean sharedFile = false;
        boolean sharedRank = false;
        // Count identical pieces that share the file and/or rank of the target piece

        // if not king, and (not (pawn and (move or promoteMove)))
        if (Math.abs(piece) != 6 && !(Math.abs(piece) == 1 && (moveType == MOVE_TYPE.MOVE || moveType == MOVE_TYPE.PROMOTE_MOVE))){
            
            // If pawn capture move just set file disambiguation every time
            if (Math.abs(piece) == 1 && (moveType == MOVE_TYPE.ATTACK || moveType == MOVE_TYPE.PROMOTE_ATTACK || moveType == MOVE_TYPE.EN_PASSENT)){
                fileDis = RegexParser.FILE_LABEL.get(origin%8);
            }
            else {
                for (int[] mv : lastValidMoves){
                    // if piece is the same AND dest squares are the same AND origin squares not the same
                    if (mv[0] == piece && mv[2] == destination && mv[1] != origin){
    
                        if (mv[1]/8 == origin/8){
                            sharedRank = true;
                        }
                        else if(mv[1]%8 == origin%8){
                            sharedFile = true;
                        }
                    }
                    if (sharedRank && sharedFile) break;
                }
            }
        }

        /* 
        // switch (Math.abs(piece)){
        //     case 1:{
        //         if (moveType == MOVE_TYPE.ATTACK || moveType == MOVE_TYPE.PROMOTE_ATTACK){
        //             int diff = destination - origin;
        //             int otherSquare;
        //             if (diff == 9){ // Black pawn attack from NE
        //                 otherSquare = origin + 7;
        //             }
        //             else if (diff == 7){// Black pawn attack from NW
        //                 otherSquare = origin + 9;
        //             }
        //             else if (diff == -7){ // White pawn attack from SE
        //                 otherSquare = origin - 9;
        //             }
        //             else if (diff == -9){ // White pawn attack from SW
        //                 otherSquare = origin - 7;
        //             }
        //             else {
        //                 // Should never get here, just to satisfy compiler, will throw out of bounds exception in the next 'if' if it reaches here
        //                 otherSquare = -1; 
        //             }
        //             // If another of the player's pawns exist on the other square, disambiguate current move
        //             if (this.board[otherSquare/8][otherSquare%8] == piece){
        //                 fileDis = RegexParser.FILE_LABEL.get(origin%8);
        //             }                    
        //         }
        //         break;
        //     }
        //     case 2:{
        //         // Check all 8 squares
        //         int[] otherSquares = new int[]{origin + 6, origin + 15, origin + 17, origin + 10, origin - 6, origin -15, origin - 17,
        //             origin - 10};
        //         sharedFile = 0;
        //         sharedRank = 0;
        //         for (int square : otherSquares){
        //             // Prevent out of bounds checks
        //             if (square < 0 || square > 63) continue;

        //             if (this.board[square/8][square%8] == piece){
        //                 if (square/8 == origin/8){
        //                     sharedRank++;
        //                 }
        //                 else if (square%8 == origin%8){
        //                     sharedFile++;
        //                 }
        //             }

        //             if (sharedFile > 0 && sharedRank > 0){
        //                 break;
        //             }
        //         }
        //         break;
        //     }
        //     case 3:{
        //         // Diagonal AND antidiagonal AND NOT destination square
        //         // Use generateValidDiagonalRayMask to get a 'blocked' bishop ray mask
        //         long candidateMask = generateValidDiagonalRayMask(destination);
        //         ArrayList<Integer> otherSquares = getSetBitPositions(candidateMask);

        //         // Single: Target piece shares rank OR file with another target piece
        //         // Double: Target piece shares rank with a second piece, shares file with a third piece
        //         sharedFile = 0;
        //         sharedRank = 0;

        //         for (int square : otherSquares){
        //             if (this.board[square/8][square%8] == piece){
        //                 if (square/8 == origin/8){
        //                     sharedRank++;
        //                 }
        //                 else if (square%8 == origin%8){
        //                     sharedFile++;
        //                 }
        //             }

        //             // Break loop as soon as double disambiguation is required
        //             if (sharedFile > 0 && sharedRank > 0){
        //                 break; // Doesn't matter if there are more, we already maximally disambiguate
        //             }
        //         }
        //         break;
        //     }
        //     case 4:{
        //         long candidateMask = generateValidStraightRayMask(destination);
        //         ArrayList<Integer> otherSquares = getSetBitPositions(candidateMask);

        //         for (int square : otherSquares){
        //             if (this.board[square/8][square%8] == piece){
        //                 if (square/8 == origin/8){
        //                     sharedRank++;
        //                 }
        //                 else if (square%8 == origin%8){
        //                     sharedFile++;
        //                 }
        //             }

        //             if (sharedFile > 0 && sharedRank > 0){
        //                 break;
        //             }
        //         }
        //         break;
        //     }
        //     case 5:{
        //         long candidateMask = generateValidAllRayMask(destination);
        //         ArrayList<Integer> otherSquares = getSetBitPositions(candidateMask);

        //         for (int square : otherSquares){
        //             if (this.board[square/8][square%8] == piece){
        //                 if (square/8 == origin/8){
        //                     sharedRank++;
        //                 }
        //                 else if (square%8 == origin%8){
        //                     sharedFile++;
        //                 }
        //             }

        //             if (sharedRank > 0 && sharedFile > 0){
        //                 break;
        //             }
        //         }
        //         break;
        //     }
        // }
        // // Determine required disambiguation dependent on if the target piece shares its rank/file with other pieces of the same type and owner
        */
        if (sharedRank) fileDis = RegexParser.FILE_LABEL.get(origin%8);
        if (sharedFile) rankDis = String.valueOf((origin/8) + 1);

        sb.append(fileDis);
        sb.append(rankDis);
        //#endregion

        //#region Capture determination
        if (moveType == MOVE_TYPE.ATTACK || moveType == MOVE_TYPE.PROMOTE_ATTACK || moveType == MOVE_TYPE.EN_PASSENT){
            sb.append('x');
        }
        //#endregion

        //#region Destination square
        sb.append(RegexParser.FILE_LABEL.get(destination%8));
        sb.append(String.valueOf((destination/8) + 1));
        //#endregion

        //#region Promotion and possible check
        if (moveType == MOVE_TYPE.PROMOTE_MOVE || moveType == MOVE_TYPE.PROMOTE_ATTACK){
            sb.append("=");
            sb.append(Move.PIECE_INT_TO_STRING.get(move.getPromotionPiece()));
        }

        if (this.state == BOARD_STATE.CHECK){
            sb.append("+");
        }
        else if (this.state == BOARD_STATE.W_MATE || this.state == BOARD_STATE.B_MATE){
            sb.append("#");
        }
        //#endregion

        this.algebraicHistory.add(sb.toString());
    }

    //#endregion--------------------------------------------------------------------------------------------------------------------------------------

    //#region Bit utility
    //#region unused transpose method
    // Optimized in terms of operation types, still high number of operations
    // public static long transpose(Long board){
    //     long retBoard = 0L;
    //     int iteration = 0;
    //     for (int i = 0; i < 8; i++){
    //         for (int j = 0; j < 8; j++){
    //             // Shift down to current index
    //             int index = (j * 8) + i;
    //             Long indexBit = (1L << index);

    //             // Read board at index, 1 for something 0 for nothing
    //             indexBit &= board;

    //             // If we read a 1 at the index, shift down by that index and OR it, to add to retBoard
    //             // If not, then we skip the index as it is already 0
    //             if (indexBit != 0){
    //                 retBoard |= (1L << iteration);
    //             }
    //             iteration ++;
    //         }
    //     }
    //     return retBoard;
    // }
    //#endregion

    // occMask DOES NOT INCLUDE the piece in question
    // rayMask is the specific ray we're calculating at the moment
    // pieceMask is the location of the piece
    // Provide RLERF encoded parameters, returns RLERF encoded mask
    /**
     * Calculate the blocked ray mask from the provided ray blocked by pieces in the provdied <br>
     * occupancy mask.
     * 
     * <p>
     * Uses Hyperbolic quintessence method to generate a blocked ray mask based on the provided<br>
     * ray mask and occupancy mask. Essentially leverages bit underflow to unset all indices after<br>
     * a blocker piece in the occupancy mask. The masks are then reversed to block the other<br>
     * side of the ray since underflow only works in one direction.
     * </p>
     * 
     * <p>
     * Formatting considerations:
     * The parameters are provided as RLERF encoded masks (see <a href="#RLERF">RLERF encoding</a> in {@link Board}). However the hyperbolic<br>
     * quintessence method utilises LERF (non-reversed) encoding so the provided parameters must first<br>
     * be reversed RLERF -> LERF, and the return value reversed from LERF -> RLERF.
     * </p>
     * @param occMask mask representing board occupancy in RLERF encoding (see <a href="#RLERF">RLERF encoding</a> in {@link Board}).
     * @param rayMask mask representing the specific ray to be blocked in RLERF encoding.
     * @param pieceMask mask representing the location of the piece that lies on the ray, <br>
     * in RLERF encoding. Should only have one set bit representing location.
     * @return mask representing the resulting blocked ray, in RLERF format.
     */
    public static long hypQuint(long occMask, long rayMask, long pieceMask){
        long retBits;
        // Reverse all provided bitmasks from RLERF --> LERF
        occMask = Long.reverse(occMask);
        rayMask = Long.reverse(rayMask);
        pieceMask = Long.reverse(pieceMask);

        // occMask becomes only the pieces in the path of the ray
        occMask = occMask & rayMask;

        // IDK what this is could be black magic for all I know, "It just works" - Todd Howard
        long forward = rayMask & occMask;
        long reverse = Long.reverse(forward);
        forward = forward - (2 * pieceMask);
        reverse = reverse - (2 * Long.reverse(pieceMask));

        retBits = (forward ^ Long.reverse(reverse)) & rayMask;
       
        // Reverse final bitmask from LERF --> RLERF
        return Long.reverse(retBits);
        
    }

    // Returns bit position in RLERF encoding
    /**
     * Examines the provided long value as an int64 and returns a list containing all of the indices of<br>
     * bits that are set to 1.
     * 
     * <p>
     * Uses {@link Long#numberOfTrailingZeros(long)} to count the zeroes from LSB -> MSB (left to right). That number<br>
     * would also be the LERF index of the first set bit from LSB -> MSB. We subtract this index from 63 to convert<br>
     * to a RLERF index (see <a href="#RLERF">RLERF encoding</a> in {@link Board}), then add to the return array. We then unset this counted bit by subtracting 1 from it and<br>
     * and perfoming an AND with the original board, eliminating the bits that were set with the subtract 1 operation.<br>
     * This process is then repeated until there are no more set bits in the bitboard.
     * </p>
     * 
     * <p>
     * <code>
     * bitboard --> 0b0010 1000<br>
     * bitboard - 1 --> 0b0010 0111<br>
     * bitboard &amp;= bitboard - 1 --> 0b0010 0000<br>
     * </code>
     * </p>
     * 
     * @param bitboard long representation of a RLERF (see {@Board}) encoded bitboard.
     * @return {@link ArrayList} of {@link Integer} representing all indicices in the provided bitboard that were <br>
     * set.
     */
    public static ArrayList<Integer> getSetBitPositions(long bitboard){
        ArrayList<Integer> retArray = new ArrayList<>();
        while (bitboard != 0){
            // Returns number of trailing zeroes starting from LSB
            int index = Long.numberOfTrailingZeros(bitboard);
            // 63 - index for RLERF
            retArray.add(63 - index);
            // bitboard - 1 would unset the least significant bit currently set, but flips the rest of the bits to the right as well
            // However bitboard &= bitboard - 1 would only keep the LSB set to 0, not the ones also flipped to 1
            bitboard &= bitboard - 1;
        }

        return retArray;
    }

    /**
     * Utility method used in {@link generateEvasionPath} that shifts the provided pos bitmask in the direction<br>
     * specified by the provided direction, and returns the result.
     * 
     * @param pos long RLERF bitmask (see {@Board}) with one flipped bit as the current 'position' to be shifted.
     * @param direction int representing the index shift required to shift the position in a specific direction in RLERF <br>
     * encoding (see <a href="#RLERF">RLERF encoding</a> in {@link Board}). ie 'up' is +8, 'down' is -8, 'up-right' would be +9, etc.
     * @return long RLERF bitmask (see {@Board}) representing the original pos mask shifted by the provided direction.
     */
    public static long shift(long pos, int direction){
        return (direction > 0) ? (pos >>> direction) : (pos << -direction);
    }

    public void setOccBit(int value, int bitIndex){
        long temp = (1L << (63 - bitIndex));

        if (!(value == 0 || value == 1)){
            System.out.println("setOccBit(): Invalid value provided, only 1 or 0 allowed.");
            return;
        }
        else if (value == 1){
            this.bitState |= temp; // Sets bit to 1 regardless of what it was before
        }
        else {
            this.bitState &= ~temp; // Sets the bit to 0 regardless of whether it was a 1 or 0
        }



    }
    //#endregion

    //#region Getters/Setters

    public long getBitState(){
        return this.bitState;
    }

    public int[][] getBoard(){
        int[][] retBoard = new int[8][8];

        for (int i = 0; i < 8; i++){
            retBoard[i] = this.board[i].clone();
        }
        return retBoard;
    }

    public long getOcc(){
        return this.bitState;
    }

    public void addMove(Move newMove){
        this.playedMoves.push(newMove);
    }

    public Move peekMove(){
        return this.playedMoves.peek().clone();
    }

    public BOARD_STATE getState(){
        return this.state;
    }

    public void setState(BOARD_STATE newState){
        this.state = newState;
    }
   
    /**
     * Mutator method used to set short castling rights for either player.
     * 
     * <p>
     * When invoked sets the short castling right for the provided player to the provided state<br>
     * on the current object. Additionally flips the zobrist associated flags {@link WSF} and {@link BSF}<br>
     * when the castling value changes from true to false or vice versa, so {@link updateZobrist} can <br>
     * update the castling hash only when the castling rights change values.
     * </p>
     * @param playerSign int whose sign represents which players castling rights to change, positive for white<br>
     * and negative for black.
     * @param bool the value to change the castling right to.
     */
    public void setShortCastleRights(int playerSign, boolean bool){
        if (playerSign > 0){
            if (this.whiteShort != bool){
                this.WSF = true;
            }
            this.whiteShort = bool;
            
        }
        else {
            if (this.blackShort != bool){
                this.BSF = true;
            }
            this.blackShort = bool;
        }
    }

    /**
     * Mutator method used to set long castling rights for either player.
     * 
     * <p>
     * When invoked sets the long castling right for the provided player to the provided state<br>
     * on the current object. Additionally flips the zobrist associated flags {@link WLF} and {@link BLF}<br>
     * when the castling value changes from true to false or vice versa, so {@link updateZobrist} can <br>
     * update the castling hash only when the castling rights change values.
     * </p>
     * @param playerSign int whose sign represents which players castling rights to change, positive for white<br>
     * and negative for black.
     * @param bool the value to change the castling right to.
     */
    public void setLongCastleRights(int playerSign, boolean bool){
        if (playerSign > 0){
            if (this.whiteLong != bool){
                this.WLF = bool;
            }
            this.whiteLong = bool;
        }
        else {
            if (this.blackLong != bool){
                this.BLF = true;
            }
            this.blackLong = bool;
        }
    }

    public boolean getCastlingRights(String label){
        if (label.equals("whiteShort")){
            return this.whiteShort;
        }
        else if(label.equals("whiteLong")){
            return this.whiteLong;
        }
        else if (label.equals("blackShort")){
            return this.blackShort;
        }
        else if (label.equals("blackLong")){
            return this.blackLong;
        }
        else {
            System.out.println("getCastlingRights(): Invalid rights label");
            return false;
        }
    }
    
    public boolean getClaimableDraw(String label){
        if (label.equals("fiftyMove")){
            return this.fiftyMoveDrawAvailable;
        }
        else if (label.equals("threeFold")){
            return this.threeFoldDrawAvailable;
        }
        else {
            System.out.println("getClaimableDraw(): Invalid label provided");
            return false;
        }
    }
    
    public boolean getWhitesTurn(){
        return this.whitesTurn;
    }

    public int getHalfClock(){
        return this.halfClock;
    }

    public int getPlayedMovesLength(){
        return this.playedMoves.size();
    }

    public int getZobristHistoryLength(){
        return this.zobristHistory.size();
    }
    
    public int getTurnInt(){
        return whitesTurn ? 1 : -1;
    }
   
    public long getZobrist(){
        return this.zobristHash;
    }

    public boolean getThreeFold(){
        return this.threeFoldDrawAvailable;
    }

    public boolean getFiftyMove(){
        return this.fiftyMoveDrawAvailable;
    }

    public void setHalfClock(int halfClock){
        if ((Object) halfClock instanceof Integer){
            this.halfClock = halfClock;
        }
    }
    
    public long[] getZobristHistory(){
        long[] retArray = new long[this.zobristHistory.size()];

        int i = this.zobristHistory.size() - 1;
        for (Long l : this.zobristHistory){
            retArray[i] = l;
            i--;
        }
        return retArray;
    }
    
    public ArrayList<String> getAlgebraicHistory(){
        @SuppressWarnings("unchecked")
        ArrayList<String> copy = (ArrayList<String>)this.algebraicHistory.clone();
        return copy;
    }
    
    // TEST FUNCTION REMOVE AFTER
    public void setBoard(int[][] newBoard){
        this.board = newBoard;
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

    //#region Game loop methods
    // Strictly plays the moves and modifies board state, no move validity checking here
    // Additionally updates the occupancy mask to reflect the board after move is played
    /**
     * Method responsible for playing a {@link Move} on to the current {@link Board}.
     * 
     * <p>
     * Reads the move and changes {@link boardState} according to the move information. Also modifies {@link bitState} <br>
     * according to the changes made by the move. Then adds the played move to {@link playedMoves}. Finally sets {@link lastBoardState}<br>
     * to a deep clone of the board before the move was played.
     * </p>
     * 
     * <p>
     * Proper game loop order:<br>
     * Depending on user input mode this may vary slightly, but the following describes the order required<br>
     * for moves to be played and the board to update its state accordingly. Move selection/validation require<br>
     * separate methods.
     * <ol>
     * <li> {@link playMove} to play the move on to the board</li>
     * <li> {@link updateState} to update the state based on the move just played, flips turn</li>
     * <li> {@link generateValidMoves} to generate moves for the next player
     * <li> {@link evaluateGameEndConditions} determines any forcing game-end states based on the current object<br>
     * and the provided valid moves array.</li>
     * </ol>
     * </p>
     * @param mv the {@link Move} to be played on to the current {@link Board}.
     */
    public void playMove(Move mv){
        MOVE_TYPE mvType = mv.getType();
        int origin = mv.getOriginBit();
        int originRank = origin / 8;
        int originFile = origin % 8;
        int dest = mv.getDestBit();
        int destRank = dest / 8;
        int destFile = dest % 8;
        int piece = mv.getPiece();

        //int[][] lastBoard = deepCloneBoard(this.boardState);
        this.boardHistory.push(deepCloneBoard(this.board));
        this.bitStateHistory.push(this.bitState);

        // PLAY the move
        // Move the piece first, which occurs with all types of moves
        this.board[originRank][originFile] = 0;
        this.board[destRank][destFile] = piece;

        // Update occupancy mask accordingly
        setOccBit(0, origin);
        setOccBit(1, dest);

        if (mvType == MOVE_TYPE.CASTLE_LONG){
            if (piece > 0){
                // Set previous rook square to 0, set rook destination according to type of castling
                this.board[0][0] = 0;
                this.board[destRank][destFile + 1] = 4;

                // Update occupancy mask accordingly
                setOccBit(0, 0);
                setOccBit(1, (destRank * 8) + (destFile + 1));
            }
            else{
                this.board[7][0] = 0;
                this.board[destRank][destFile + 1] = -4;

                setOccBit(0, 56);
                setOccBit(1, (destRank * 8) + (destFile + 1));
            }
        }
        else if (mvType == MOVE_TYPE.CASTLE_SHORT){
            // Move rooks to proper squares
            if (piece > 0){
                this.board[0][7] = 0;
                this.board[destRank][destFile - 1] = 4;

                setOccBit(0, 7);
                setOccBit(1, (destRank * 8) + (destFile - 1));
            }
            else{
                this.board[7][7] = 0;
                this.board[destRank][destFile - 1] = -4;

                setOccBit(0, 63);
                setOccBit(1, (destRank * 8) + (destFile - 1));
            }
        }
        else if (mvType == MOVE_TYPE.EN_PASSENT){
            // Depending on player, remove the piece above(black) or below (white) the en passent destination square
            if (piece > 0){
                this.board[destRank - 1][destFile] = 0;
                setOccBit(0, ((destRank - 1) * 8) + destFile);
            }
            else {
                this.board[destRank + 1][destFile] = 0;
                setOccBit(0, ((destRank + 1) * 8) + destFile);
            }
        }
        //Promotions
        else if ((mvType == MOVE_TYPE.PROMOTE_ATTACK) || (mvType == MOVE_TYPE.PROMOTE_MOVE)) {
            this.board[destRank][destFile] = mv.getPromotionPiece(); // Overwrite previous piece with selected promotion piece
        }

        // Add move to playedMoves
        this.playedMoves.push(mv);
    }
    // Out of date Javadoc
    // /**
    //  * Updates the state of the current {@link Board} to reflect changes caused by the most recently played {@link Move}.
    //  * 
    //  * <p>
    //  * Determines board state by generating opponent checks for opposing player. If no checks then game is <br>
    //  * {@link BOARD_STATE#IN_PLAY}, otherwise game is in {@link BOARD_STATE#CHECK}. Also updates {@link halfClock}<br>
    //  * , {@link zobristHash}, {@link threeFoldDrawAvailable}, and {@link fiftyMoveDrawAvailable}.
    //  * </p>
    //  * 
    //  * <p>
    //  * Updates castling rights based on last move:
    //  * <ol>
    //  * <li> If king moves both of that player's castling rights are invalidated</li>
    //  * <li> If either rook moves from its starting position, that side's castling is invalidated</li>
    //  * </ol>
    //  * <br>
    //  * Both castling flags as well as flags used to inform {@link updateZobrist} about the first time castling rights have changed are<br>
    //  * maintained here.<br>
    //  * 
    //  * These separate flags are necessary as {@link updateZobrist} only removes the castling hash from the zobrist hash the first time<br>
    //  * any of the castling rights are set to false.
    //  * </p>
    //  * 
    //  * <p>
    //  * Finally flips {@Board.whitesTurn} depending on whose turn it is.
    //  * </p>
    //  * 
    //  * <p>
    //  * Proper game loop order:<br>
    //  * 
    //  * Depending on user input mode this may vary slightly, but the following describes the order required<br>
    //  * for moves to be played and the board to update its state accordingly. Move selection/validation require<br>
    //  * separate methods.
    //  * <ol>
    //  * <li> {@link playMove} to play the move on to the board</li>
    //  * <li> {@link updateState} to update the state based on the move just played, flips turn</li>
    //  * <li> {@link generateValidMoves} to generate moves for the next player
    //  * <li> {@link evaluateGameEndConditions} determines any forcing game-end states based on the current object<br>
    //  * and the provided valid moves array.</li>
    //  * </ol>
    //  * </p>
    //  * 
    //  * @param lastPlayerSign int whose sign represents the player that played the last move. Positive for white negative for black.
    //  */
    public ArrayList<int[]> updateState(int lastPlayerSign, ArrayList<int[]> lastValidMoves){
        boolean shortCastleRights = (lastPlayerSign > 0) ? whiteShort : blackShort;
        boolean longCastleRights = (lastPlayerSign > 0) ? whiteLong : blackLong;
        int shortRookSquare = (lastPlayerSign > 0) ? 7 : 63;
        int longRookSquare = (lastPlayerSign > 0) ? 0 : 56;
        
        this.castlingHistory.push(new boolean[]{this.whiteLong, this.whiteShort, this.blackLong, this.blackShort});
        this.stateHistory.push(this.state);
        this.halfClockHistory.push(this.halfClock);

        // Check/Checkmate
        if (getOpponentChecks(lastPlayerSign * -1).size() > 0){
            setState(BOARD_STATE.CHECK);
        }
        else {
            setState(BOARD_STATE.IN_PLAY);
        }
        
        // Updating Castling rights
        // If any castling right is changed, its corresponding zobrist flag is set to true, causing updateZobrist() to update the hash accordingly
        if (shortCastleRights || longCastleRights){
            Move lastMove = peekMove();
            if (lastMove != null){
                int lastMovePiece = Math.abs(lastMove.getPiece());
                // If either castling right is active, and the move is a King, set both rights to false
                if ((Math.abs(lastMove.getPiece()) == 6)){
                    setShortCastleRights(lastPlayerSign, false);
                    setLongCastleRights(lastPlayerSign, false);
                }
                else {
                    int lastMoveOrigin = lastMove.getOriginBit();
                    // If either castling right is active, check if rook moved from corresponding origin square
                    if (lastMovePiece == 4){
                        if (shortCastleRights && (lastMoveOrigin == shortRookSquare)){
                            setShortCastleRights(lastPlayerSign, false);
                        }
                        else if(longCastleRights && (lastMoveOrigin == longRookSquare)){
                            setLongCastleRights(lastPlayerSign, false);
                        }
                    }
                }
            }
        } // End castling IF

        // Update halfClock based on previous turn
        updateHalfClock();

        // Update board, castling, turn, and EP zobrist based on flags set previously in UpdateBoard()
        updateZobrist();

        
        // Update optional draw flags
        this.fiftyMoveDrawAvailable = checkNMoveDraw(50);
        this.threeFoldDrawAvailable = checkNFoldRepeat(3);
        
        if (this.whitesTurn){
            this.whitesTurn = false;
        }
        else {
            this.whitesTurn = true;
        }

        // At this point board state reflects the starting state for the next player
        ArrayList<int[]> newValidMoves = generateValidMoves(getTurnInt());
        // We execute this here so that Board.state is set properly for algebraic move generation
        evaluateGameEndConditions(newValidMoves);
        // Record algebraic move, must occur after board.state is updated
        recordAlgebraicMove(lastValidMoves);

        return newValidMoves;
    }
    
    // We can generate valid moves for the next player before this method runs
    // Since all the state is done changing by this time, now we're just evaluating the state + moves
    // exit code == 0 : game on
    // exit code == 1 : Checkmate
    // exit code >= 2 : Forced Draw
    // 2 = Stalemate, 3 = Insufficient material, 4 = 75 move draw, 5 = five fold repeat draw
    /**
     * Determines if the current object has reached a forced game end state.
     * 
     * <p>
     * Determines checkmate vs stalemate by examining the length of the provided validMoves array<br>
     * generated by {@link generateValidMoves}. If there are valid moves, then goes on to check <br>
     * for forced draw conditions on the current object such as insufficient material, 5-fold repeat<br>
     * draw, and 75-move rule draw.
     * </p>
     * 
     * <p>
     * Proper game loop order:<br>
     * Depending on user input mode this may vary slightly, but the following describes the order required<br>
     * for moves to be played and the board to update its state accordingly. Move selection/validation require<br>
     * separate methods.
     * <ol>
     * <li> {@link playMove} to play the move on to the board</li>
     * <li> {@link updateState} to update the state based on the move just played, flips turn</li>
     * <li> {@link generateValidMoves} to generate moves for the next player
     * <li> {@link evaluateGameEndConditions} determines any forcing game-end states based on the current object<br>
     * and the provided valid moves array.</li>
     * </ol>
     * </p>
     * @param validMoves an {@link ArrayList} of valid moves generated by {@link generateValidMoves}.
     * @return return values depending on game-end states of the current object:<br>
     * <ol>
     * <li> 0 = No game end condition, game continues</li>
     * <li> 1 = Checkmate </li>
     * <li> 2 = Stalemate draw </li>
     * <li> 3 = Insufficient material draw </li>
     * <li> 4 = 75 move rule draw </li>
     * <li> 5 = 5-fold repeat draw </li>
     * </ol>
     */
    public int evaluateGameEndConditions(ArrayList<int[]> validMoves){
        int turn = this.getTurnInt();
        // Stalemate and Checkmate detection based on provided moves
        if (validMoves.size() == 0){
            if (this.state == BOARD_STATE.CHECK){
                System.out.println("Checkmate!"); // Replace with proper game-ending code
                this.state = turn > 0 ? BOARD_STATE.B_MATE : BOARD_STATE.W_MATE;
                return 1;
            }
            else {
                System.out.println("Stalemate!");
                this.state = BOARD_STATE.STALEMATE;
                return 2;
            }
        }

        // Check forced draw conditions
        if (checkInsufficientMaterial()){
            System.out.println("Insufficient materal!"); // Implement game-end method
            this.state = BOARD_STATE.MATERIAL_DRAW;
            return 3;
        }
        else if (this.fiftyMoveDrawAvailable && checkNMoveDraw(75)){
            this.state = BOARD_STATE.SEVENTY_FIVE_DRAW;
            System.out.println("75 move draw!");
            return 4;
        }
        else if (this.threeFoldDrawAvailable && checkNFoldRepeat(5)){
            System.out.println("Five fold repeat draw!");
            this.state = BOARD_STATE.FIVE_REPEAT_DRAW;
            return 5;
        }
        // If function exits without ending the game, then we can give control to next player
        return 0;
    }

    public void undoLastMove(){
        if (playedMoves.size() > 0){
            boolean[] lastCastling = castlingHistory.pop();
            playedMoves.pop();
            epHashHistory.pop();
            this.whiteLong = lastCastling[0];
            this.whiteShort = lastCastling[1];
            this.blackLong = lastCastling[2];
            this.blackShort = lastCastling[3];
            
            // At the beginning of a turn, zobristHistory.peek() == this.zobristHistory
            // Once it is updated it is then pushed into the zobristHistory
            // Thus the zobristHistory must be popped once, and this.zobristHistory set to the next occurence:
            // zHistory(0) -> play -> zHistory(1) -> play -> zHistory(2) -> undo -> zHistory(1) -> play -> zHistory(2)
            // Must restore the zobristHash to the point BEFORE the last move was played.
            zobristHistory.pop();
            this.zobristHash = zobristHistory.peek();
    
            this.board = boardHistory.pop();
            this.state = stateHistory.pop();
            this.bitState = bitStateHistory.pop();
            this.halfClock = halfClockHistory.pop();
    
            this.fiftyMoveDrawAvailable = checkNMoveDraw(50);
            this.threeFoldDrawAvailable = checkNFoldRepeat(3);

            this.algebraicHistory.remove(this.algebraicHistory.size() - 1);
    
            this.whitesTurn = whitesTurn ? false : true;
        }
    
    }
    //#endregion

    //#region Object method overrides
    @Override
    public String toString(){
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
        }
        sbOuter.append("   A   B   C   D   E   F   G   H");
        sbOuter.insert(0, '\n');
        sbOuter.append('\n');
        return sbOuter.toString();
        
    }



    //#endregion
}
