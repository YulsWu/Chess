import db.RegexDatabase;
import parser.RegexParser;
import db.RegexGameData;
import java.util.ArrayList;
import db.databaseTests;

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
        
        String filePath = "pgn/polgarj.pgn";
        
        if (!RegexDatabase.doesDatabaseExist()){
            RegexDatabase.createDatabase();
        }
        if (!(RegexDatabase.doesTableExist())){
            RegexDatabase.createTable();
        }

        ArrayList<RegexGameData> gd = RegexParser.extractPGN(filePath);

        ArrayList<RegexGameData> subList = new ArrayList<>(gd.subList(0, 51));

        RegexDatabase.writeDB(gd, 50);


        //RegexDatabase.dropDatabase();

    }
}
