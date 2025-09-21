package com.YCorp.chessApp.client.engine;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.YCorp.chessApp.client.parser.RegexParser;

public class UciParser {
    public static final Pattern BEST_MOVE;

    static {
        BEST_MOVE = Pattern.compile("bestmove ([a-h][0-9][a-h][0-9])");
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

    // Provide length 4 string representing two chess squares, with origin first then destination after
    public static int[] convertMove(String move){
        // Convert all file labels to their index equivalent
        // Subtract 1 from all rank labels to get their index equivalent

        int originFile = RegexParser.FILE_INDEX.get(String.valueOf(move.charAt(0)));
        int originRank = Character.getNumericValue(move.charAt(1)) - 1; // Rank value to index value
        int destFile = RegexParser.FILE_INDEX.get(String.valueOf(move.charAt(2)));
        int destRank = Character.getNumericValue(move.charAt(3)) - 1; // Rank value to index value

        int origin = (originRank * 8) + originFile;
        int dest = (destRank * 8) + destFile;

        return new int[]{origin, dest};
    }
}
