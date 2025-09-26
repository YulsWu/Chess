import com.YCorp.chessApp.server.db.RegexDatabase;
import com.YCorp.chessApp.server.db.RegexGameData;

import java.util.ArrayList;
import java.util.Arrays;

import com.YCorp.chessApp.client.engine.Board;
import com.YCorp.chessApp.client.parser.RegexParser;

public class PGNTest {
    

    public static void readTest_0(){
        ArrayList<RegexGameData> rgd = RegexParser.extractPGN("C:\\Users\\yulun\\AppData\\Local\\Programs\\Java\\Chess\\src\\main\\resources\\pgn\\Andreikin.pgn");
        System.out.println("Extracted " + rgd.size() + " games from PGN");

        RegexGameData game = rgd.get(0);
        for (int i = 0; i < game.moves.size(); i++){
            System.out.println(i + ". " + game.moves.get(i));
        }
    }
}
