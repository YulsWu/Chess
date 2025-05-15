import com.YCorp.chessApp.server.db.RegexDatabase;
import com.YCorp.chessApp.client.parser.RegexParser;
import com.YCorp.chessApp.server.db.RegexGameData;
import java.util.ArrayList;
import java.util.Map;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class testMain {
    public static void main(String[] args){
        writeAllGames();
        
    }
    
    public static void writeAllGames(){
        String filePath = "C:/Users/Yulun/AppData/Local/Programs/Java/Chess/src/main/resources/pgn/";
        
        String[] players = new String[]{
            "andreikin.pgn", "aronian.pgn", "ashley.pgn", "carlsen.pgn",
            "ding.pgn", "nakamura.pgn", "polgarj.pgn", "tatamast25.pgn"
        };

        ArrayList<RegexGameData> gdArray;
        for (String p : players){
            System.out.println("Writing " + p);
            gdArray = RegexParser.extractPGN(filePath + p);
            RegexDatabase.writeDB(gdArray, 500);
            System.out.println("DONE");
        }
        
    }

    public static void printToFile(String filepath, String msg){
        try (PrintStream fileOut = new PrintStream(new FileOutputStream(filepath))){
            fileOut.print(msg);
        }
        catch(IOException e){
            System.out.println("printToFile() Error: Exception occurred printing to file " + e);
        }
    }
}
