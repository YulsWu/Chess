
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import engine.Board;
import javafx.application.Application;
import javafx.BoardMaker;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;


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
        
        //BoardMaker.launch(BoardMaker.class, args);
        test.generateCheckEvasionTest(1);
        //test.generateEnPassentMaskTest();
        //Board.bitboardVisualize(0b000000000000000000000000000000000000000000000100000000000000000L);

        // // Get occupancy along the vertical
        // Long rayOcc = occMask & vertMask;

        // // Transpose and calculate vertical rays
        // Long validVerticalPaths = test.hyperbolicQuintessence(test.transposeBitboard(rayOcc), test.transposeBitboard(vertMask), test.transposeBitboard(pieceMask));
        // validVerticalPaths = test.transposeBitboard(validVerticalPaths);
        // System.out.println("Valid vertical moves:");
        // test.bitboardVisualize(validVerticalPaths);

        // // Get occupancy along the horizontal ray
        // rayOcc = occMask & horzMask;

        // // Transpose and calculate horizontal rays
        // Long validHorizontalPaths = test.hyperbolicQuintessence(test.transposeBitboard(rayOcc), test.transposeBitboard(horzMask), test.transposeBitboard(pieceMask));
        // validHorizontalPaths = test.transposeBitboard(validHorizontalPaths);
        // System.out.println("Valid horizontal moves:");
        // test.bitboardVisualize(validHorizontalPaths);
        //======================================================================================================================================================================
        // // Occmask in custom order (reverse LERF)
        // System.out.println(test.longToString(occMask));

        // // Occmask in LittleEndianRankFile
        // System.out.println(test.longToString(Long.reverse(occMask)));

        // System.out.println(test.longToString(test.transposeBitboard(Long.reverse(occMask))));
        // System.out.println(test.longToString(test.transposeBitboard(test.transposeBitboard(Long.reverse(occMask)))));

        // System.out.println(test.longToString(test.transpose(Long.reverse(occMask))));
        // System.out.println(test.longToString(test.transpose(test.transpose(Long.reverse(occMask)))));

        
    

    }
}