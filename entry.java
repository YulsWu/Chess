
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;

public class entry {
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
        String filepath = "pgn/polgarj.pgn";
        String dirPath = "pgn/";

        //test.databaseInterfaceTest(url, database, DBName, tableName, serviceName, username, password, filepath);
        try{
            System.out.println(test.pgnRegexTest(filepath));
        }
        catch (Throwable T){
            System.out.println("Unchecked exception ocurred:" + T);
        }
    
    }
}