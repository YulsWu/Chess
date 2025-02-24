
import java.util.ArrayList;

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
        ArrayList<Long> bb = Board.generateRookMoveMask();

        Long diagonalBit = 0x8040201008040201L;

        test.bitboardVisualize(diagonalBit);

        char[] diagonalArray = Long.toBinaryString(diagonalBit).toCharArray();

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 8; i++){
            for (int j = 0; j < 8; j++){
                sb.append(diagonalArray[(j * 8) + i]);
            }
        }

        Long diagonalNew = Long.parseLong(sb.toString(), 2);

        test.longToString(diagonalBit);
        test.longToString(diagonalNew);



        



    }
}