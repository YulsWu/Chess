
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import engine.Board;
import javafx.application.Application;
import javafx.BoardMaker;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Arrays;
import java.util.regex.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.StringTokenizer;

import engine.Move;


public class entry{
    public static void main(String[] args){
        // try(PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8.name())){
        //     System.setOut(out);
        // }
        // catch (Exception e){
        //     System.out.println("Exception occurred setting system OUT encoding: " + e);
        // }
        // chcp 65001
        int batch_size = 100;

        String mysqlServer = "jdbc:mysql://localhost:3306";
        
        String url = "jdbc:mysql://localhost:3306";
        String DBName = "pgn_database";
        String database = url + "/" + DBName;
        String tableName = "games";
        String serviceName = "mysql84";
        String username = "root";
        String password = "fUZ8&ejS4]";
        String filepath = "pgn/polgarj.pgn";
        String dirPath = "pgn/";
        
        Long diagMask = 0x8040201008040201L;
        Long antiDMask = 0b0000001000000100000010000001000000100000010000001000000000000000L;
        // Long eFileMask = 0x1010101010101010L;

        // Long moveMask = eFileMask;

        Long vertMask = 0b0001000000010000000100000001000000010000000100000001000000010000L;
        Long horzMask = 0b0000000000000000000000001111111100000000000000000000000000000000L;
        Long occMask = Board.boardToBitboard(Board.generateFreshBoard());
        Long pieceMask = (1L << 63 - 27);
        
        // test.generateCheckEvasionTest(0);
        // test.generateEnPassentMaskTest();
        // test.generateValidMovesTest();
        // test.generateValidCastlingMovesTest();


        // test.createMoveTest();
        //BoardMaker.launch(BoardMaker.class, args);
        // Board.bitboardVisualize(0b000000000000000000000000000000000000000000000100000000000000000L);
        // System.out.println();
        // Board.boardVisualize(new int[][] {{4, 2, 3, 5, 6, 0, 0, 4}, {1, 1, 1, 0, 0, 1, 1, 1}, {0, 0, 0, 1, 0, 2, 0, 0}, {0, 0, 0, 0, 1, 0, 0, 0}, {0, 3, -3, -1, -1, 0, 0, 0}, {0, 0, -2, 0, 0, -3, 0, 0}, {-1, -1, -1, 0, -2, -1, -1, -1}, {-4, 0, 0, -5, -6, 0, 0, -4}});
        // test.playTest(test.foolsOnWhite, "Fools mate on white", 0);
        // test.playTest(test.foolsOnBlack, "Fools mate on black", 0);

        ArrayList<String[]> temp = test.extractPGN("pgn/sample.pgn");
        int count = 0;
        for (String[] tmv : temp){
            System.out.println("Count " + count);
            ArrayList<Move> tempMove = test.moveValidator(tmv[1]);
            System.out.println(tempMove.size() + " moves present");
            count ++;
        }


        //ArrayList<Move> tempMove = test.moveValidator(temp.get(1)[1]);
        //System.out.println(temp.get(1)[1]);

        // Errors:
        // Count 800, index 50
        // Count 867, index 40

        //Board.bitboardVisualize(0b1001110000100100000100011100010000000010000010001110110001010010L);

    }

}

