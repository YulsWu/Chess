package com.YCorp.chessApp.client.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UciParser {
    public static final Pattern BEST_MOVE;
    public static final Map<Character, Integer> LOWERCASE_PIECE_TO_ID = new HashMap<>();

    static {
        BEST_MOVE = Pattern.compile("bestmove ([a-h][0-9][a-h][0-9][nbrq]?)");
        LOWERCASE_PIECE_TO_ID.put('p', 1);
        LOWERCASE_PIECE_TO_ID.put('n', 2);
        LOWERCASE_PIECE_TO_ID.put('b', 3);
        LOWERCASE_PIECE_TO_ID.put('r', 4);
        LOWERCASE_PIECE_TO_ID.put('q', 5);
        LOWERCASE_PIECE_TO_ID.put('k', 6);
    }

    // Returns either the best move if found, or null denoting that the given line did not contain any regex hits
    public static String extractBestMove(String line){
        Matcher matcher = BEST_MOVE.matcher(line);

        if (matcher.find()){
            return matcher.group(1);
        }
        else {
            return null;
        }
    }

    // Takes as input either a 4 or 5 length string representing a UCI compliant move, <originFile><originRank><destFile><destRank><optionalPromotion>
    // Returns a 2-length int[] if the move is non-promotion, 3-length int[] if it is
    // The returned promotion piece is ABSOLUTE, uncoloured by allegiance 
    public static int[] convertMove(String move){
        // Convert all file labels to their index equivalent
        // Subtract 1 from all rank labels to get their index equivalent

        int originFile = RegexParser.FILE_INDEX.get(String.valueOf(move.charAt(0)));
        int originRank = Character.getNumericValue(move.charAt(1)) - 1; // Rank value to index value
        int destFile = RegexParser.FILE_INDEX.get(String.valueOf(move.charAt(2)));
        int destRank = Character.getNumericValue(move.charAt(3)) - 1; // Rank value to index value

        int origin = (originRank * 8) + originFile;
        int dest = (destRank * 8) + destFile;

        if (move.length() == 5){
            int promotion = LOWERCASE_PIECE_TO_ID.get(move.charAt(4));
            return new int[]{origin, dest, promotion};
        }
        else {
            return new int[]{origin, dest};
        }
    }
}
