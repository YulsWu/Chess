import db.RegexDatabase;
import parser.RegexParser;
import db.RegexGameData;
import java.util.ArrayList;
import db.databaseTests;
import java.util.Map;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class testMain {
    public static void main(String[] args){

        int batch_size = 100;

        String mysqlServer = "jdbc:mysql://localhost:3306";
        
        String url = "jdbc:mysql://localhost:3306";
        String DBName = "pgn_database";
        String database = url + "/" + DBName;
        String tableName = "games";
        String serviceName = "mysql84";
        String username = "root";
        String password = "fUZ8&ejS4]";
        String dirPath = "pgn/";
        
        
        RegexDatabase.dropDatabase();
        writeAllGames();
        
    }
    
    public static void writeAllGames(){
        String filePath = "pgn/";
        
        String[] players = new String[]{
            "andreikin.pgn", "aronian.pgn", "ashley.pgn", "carlsen.pgn",
            "ding.pgn", "nakamura.pgn", "polgarj.pgn", "tatamast25.pgn"
        };

        if (!RegexDatabase.doesDatabaseExist()){
            RegexDatabase.createDatabase();
        }
        if (!(RegexDatabase.doesTableExist())){
            RegexDatabase.createTable();
        }
        
        ArrayList<RegexGameData> gdArray;
        for (String p : players){
            System.out.println("Writing " + p);
            gdArray = RegexParser.extractPGN(filePath + p);
            RegexDatabase.writeDB(gdArray, 50);
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
