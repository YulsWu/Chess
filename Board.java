import java.util.ArrayList;

public class Board {
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
}
