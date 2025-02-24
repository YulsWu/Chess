
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

        Long diagonalMask = 0x8040201008040201L;
        Long transposed = test.toFileMajor(diagonalMask);
        
        System.out.println(test.longToString(diagonalMask));
        System.out.println(test.longToString(transposed));

        //test.databaseInterfaceTest(url, database, DBName, tableName, serviceName, username, password, filepath);
        //ArrayList<Long> bb = Board.generateRookMoveMask();

        // int[][] occupancy = new int[8][8];
        // for (int i = 0; i < 8; i++){
        //     for (int j = 0; j < 8; j++){
        //         occupancy[i][j] = 0;
        //     }
        // }
        // occupancy[2][2] = 1;
        // occupancy[6][6] = 1;
        
        
        // Long pieceMask = (1L << (63 - 36));
        // test.bitboardVisualize(diagonalMask);
        // test.bitboardVisualize(pieceMask);
        // Long occupancyMask = Board.boardToBitboard(occupancy);
        // test.bitboardVisualize(occupancyMask);

        // test.bitboardVisualize(test.hyperbolicQuintessence(occupancyMask, diagonalMask, pieceMask));



    }
}