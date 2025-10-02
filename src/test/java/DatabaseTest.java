import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import com.YCorp.chessApp.client.parser.RegexParser;
import com.YCorp.chessApp.server.db.RegexDatabase;

public class DatabaseTest {
    public static void test_0(){
        System.out.println("Unique players: " + RegexDatabase.getUniquePlayers().size() + " entries");
        System.out.println("Unique sites: " + RegexDatabase.getUniqueSites().size() + " entries");
        System.out.println("Unique events: " + RegexDatabase.getUniqueEvents().size() + " entries");
        System.out.println("Total rows: " + RegexDatabase.countAll());
    }

    public static void test_1(){
        boolean table = RegexDatabase.doesTableExist("games");
        if (!table){
            System.out.println("No table found, creating");
            RegexDatabase.createGamesTable();

            System.out.println("Table exists after creation: " + RegexDatabase.doesTableExist("games"));
        }
        else {
            System.out.println("Table exists");
        }
    }

    public static void test_2(){
        String pgn = "";
        try{
            pgn = Files.readString(Paths.get("C:\\Users\\yulun\\AppData\\Local\\Programs\\Java\\Chess\\src\\main\\resources\\pgn\\Carlsen.pgn"), StandardCharsets.UTF_8);
        }
        catch(Exception e){
            e.printStackTrace();
        }

        pgn = pgn.replaceAll("\\R", "\n");

        RegexDatabase.writeDB(RegexParser.extractPGN(pgn), 50);

    }
}
