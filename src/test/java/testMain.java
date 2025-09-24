import java.util.ArrayList;
import java.util.Map;

import com.YCorp.chessApp.client.engine.Board;
import com.YCorp.chessApp.client.engine.StockfishClient;
import com.YCorp.chessApp.client.javafx.classes.GUIEngine;
import com.YCorp.chessApp.client.parser.RegexParser;
import com.YCorp.chessApp.server.db.RegexDatabase;
import com.YCorp.chessApp.server.db.RegexGameData;

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import clockTests.ChessClockTests;

public class testMain {
    public static void main(String[] args){
        ArrayList<int[]> moves = new ArrayList<>();
        moves.add(new int[]{8, 24});
        moves.add(new int[]{55, 39});
        moves.add(new int[]{24, 32});
        moves.add(new int[]{39, 31});
        moves.add(new int[]{32, 40});
        moves.add(new int[]{31, 23});
        moves.add(new int[]{40, 49});
        moves.add(new int[]{23, 14});
        moves.add(new int[]{49, 56});
        moves.add(new int[]{14, 7});

        GUIEngine engine = new GUIEngine();

        for (int[] mv : moves){
            if (engine.isPromotionMove(mv)){
                engine.attemptMove(mv, engine.isWhitesTurn() ? 5 : -5);
            }
            else {
                engine.attemptMove(mv);
            }

            System.out.println(engine.boardVisualize());

        }

    }
}
    
