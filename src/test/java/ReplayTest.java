import java.util.ArrayList;

import com.YCorp.chessApp.client.engine.ReplayClient;
import com.YCorp.chessApp.client.parser.RegexParser;
import com.YCorp.chessApp.server.db.RegexGameData;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class ReplayTest {
    public static void test_0(){
        // Redirect logging
        try {
            PrintStream fileOut = new PrintStream(new FileOutputStream("C:\\Users\\yulun\\AppData\\Local\\Programs\\Java\\Chess\\build\\log\\replayTest_0.txt"));
            System.setOut(fileOut);
            System.setErr(fileOut);
        }
        catch (FileNotFoundException e){
            e.printStackTrace();
        }


        ArrayList<RegexGameData> rgd = RegexParser.extractPGN("C:\\Users\\yulun\\AppData\\Local\\Programs\\Java\\Chess\\src\\main\\resources\\pgn\\Andreikin.pgn");
        System.out.println("Extracted " + rgd.size() + " games from PGN");

        for (int i = 0; i < 20; i++){
            RegexGameData game = rgd.get(i);
            ReplayClient rc = new ReplayClient(game);
    
            // System.out.println("Starting pos:");
            // System.out.println(rc.getBoardVisualization());
            System.out.println("GAME " + (i + 1) + "-----------------------------------------------------------------------------------------");
    
            while (rc.ready()){
                rc.forward();
                //System.out.println(rc.getBoardVisualization());
            }
    
            if (rc.getCurrentMoveInd() == game.moves.size()){
                System.out.println("All moves played on Replay board " + (i + 1));
            }
            else {
                System.out.println("Error: Index only reached value of " + rc.getCurrentMoveInd() + " for " + game.moves.size() + " length moves list");
            }


        }

    }
}
