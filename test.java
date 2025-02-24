import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.StringTokenizer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.sql.Date;
import java.sql.Blob;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.*;
import java.nio.file.*;

public class test {
    public static void test1 (){
        String filepath = "test_games.pgn";
        StringBuilder sb = new StringBuilder();
        String line;

        try(BufferedReader bf = new BufferedReader(new FileReader(filepath))){
            while (bf.ready()){
                line = bf.readLine();
                if (line.equals("")){
                    break;
                }
                sb.append(line);
                sb.append("_");
            }

            StringTokenizer tokenizer1 = new StringTokenizer(sb.toString(), "_");
            while (tokenizer1.hasMoreTokens()){
                String currentMeta = tokenizer1.nextToken();
                StringTokenizer tokenizer2 = new StringTokenizer(currentMeta, "\"[] ");
                
                if (tokenizer2.countTokens() < 2){
                    System.out.println("Blank field for " + tokenizer2.nextToken());
                    continue;
                }
                while (tokenizer2.hasMoreTokens()){
                    System.out.println(tokenizer2.nextToken());
                }

            }

        }
        catch (Exception e){
            System.out.println("Testing error: " + e);
        }
    }

    public static ArrayList<GameData> test2(String database, String username, String password){
        ArrayList<GameData> returnGames = new ArrayList<>();
        ArrayList<byte[]> binaryMovesList = new ArrayList<>();
        ArrayList<Map<String, String>> metaMapList = new ArrayList<>();
        
        // Attempting to first read the data from the database and store in local memory, then process after the
        // connections have been closed. May be inefficient/impossible for the whole database, however
        // I don't forsee a use case where a significant number of games would have to be retained in memory?
        // Perhaps in order to convert the whole database back into a pgn file.

        // Try-catch to handle SQL connection first
        try (Connection conn = DriverManager.getConnection(database, username, password)){
            String query = "SELECT * FROM pgn_database.games";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet results = stmt.executeQuery();
            
            while (results.next()){
                Map<String, String> metaMap = PgnParser.createMetaMapTemplate();
                // Iterate through the meta labels that we want to extract from ResultSet, skipping NULL
                for (String key : Database.SQL_TO_PGN_META_LABELS.keySet()){

                    Object currentObj = results.getObject(key);

                    if (!(currentObj == null)){

                        if (currentObj instanceof Date){
                            metaMap.put(Database.SQL_TO_PGN_META_LABELS.get(key), Database.dateToString((Date)currentObj));
                        }
                        else if (currentObj instanceof Boolean){
                            String strBool = String.valueOf(currentObj.equals(true)? 1 : 0);
                            metaMap.put(Database.SQL_TO_PGN_META_LABELS.get(key), strBool);
                        }
                        else {
                            String strObj = String.valueOf(currentObj);
                            metaMap.put(Database.SQL_TO_PGN_META_LABELS.get(key), strObj);
                        }
                        
                    }
                }
                binaryMovesList.add(results.getBytes("moves"));
                metaMapList.add(metaMap);
            }

        }
        catch (Exception e){
            System.out.println("Error reading database: " + e);
            e.printStackTrace();
        }
        // All game data should be now stored in metaMapList and binaryMovesList. Now we can process these into GameData
        // objects
        // Seems inefficient to open and close these streams for each game, however this seems unavoidable if I wanted to
        // separate database reading and data processing

        if (!(binaryMovesList.size() == metaMapList.size())){
            System.out.println("Error reading games: Inconsistent size between Metadata list and Moves list, aborting");
            return returnGames;
        }

        for (int i = 0; i < binaryMovesList.size(); i++){
            Object currentObj;
            try(ByteArrayInputStream byteStream = new ByteArrayInputStream(binaryMovesList.get(i));
            ObjectInputStream objectStream = new ObjectInputStream(byteStream)){
                
                currentObj = objectStream.readObject();

                if (currentObj instanceof ArrayList){
                    // Unchecked casting of inner arraylist objects to string[], should be fine since I know what I'm reading
                    // Notwithstanding data corruption of course
                    returnGames.add(new GameData(metaMapList.get(i), (ArrayList<String[]>)currentObj));
                }

            }
            catch (IOException | ClassNotFoundException e ){
                System.out.print("Error processing binary Moveset data: " + e);
                e.printStackTrace();
            }
            
        }
        return returnGames;
    }

    public static boolean writeReadConsistencyTest(ArrayList<GameData> games, String database, String username, String password){
        ArrayList<GameData> readGames;
        readGames = Database.readDB(database, username, password);

        if (games.size() == readGames.size()){
            // If this for loop finishes, it means 
            for (GameData originalGame : games){
                boolean found = false;
                for (GameData readGame : readGames){
                    // If all games read from database are iterated through without finding a match, return false
                    // If a match is found break out of inner loop to move on to the next game
                    if (originalGame.equals(readGame)){
                        found = true;
                        break;
                    }
                }
                if (!found){
                    System.out.println("Match not found");
                    return false;
                }
            }
        }
        else{
            System.out.println("Game comparison arrays have different length, aborting.");
            return false;
        }

        return true;
    }

    public static void databaseInterfaceTest(String url, String database, String DBName, String tableName, String serviceName, String username, String password, String filepath){
        System.out.println("____ESTABLISHING CONNECTION____");
        // Does service exist
        if (Database.doesServiceExist(serviceName)){
            System.out.println("Found installed service " + serviceName);
        }
        else {
            System.out.println("Service \"" + serviceName + "\" does not exist");
            return;
        }
        

        // Is service running, else start service
        try {
            boolean running = false;

            while (!running){
                if (Database.isServiceRunning("mysql84")){
                    System.out.println("Service \"" + serviceName + "\" is running");
                    running = true;
                } else {
                    System.out.println("Attempting to start \"" + serviceName + "\"");
                    if (Database.startService(serviceName)){
                        System.out.println("Service \"" + serviceName + "\" successfully started");
                    }
                }
            }
        }
        catch (Exception e){
            System.out.println("ERROR occurred during service query");
            return;
        }
        // Does database exist?
        // If so, check if table exists
        if (Database.doesDatabaseExist(url, DBName, username, password)){
            System.out.println("Found database \"" + DBName + "\"");
            if (Database.doesTableExist(url, DBName, tableName, username, password)){
                System.out.println("Found table \"" + tableName + "\"");
            }
            else {
                System.out.println("Table \"" + tableName + "\" not found");

                if (Database.createTable(url, DBName, tableName, username, password)){
                    System.out.println("Successfully created table \"" + tableName + "\"");
                }
                else {
                    System.out.println("Unexpected error creating table \"" + tableName + "\"");
                    return;
                }

            }
        }
        // If database doesn't exist create both database and table
        else {
            System.out.println("Database \"" + DBName + "\" not found");
            if (Database.createDatabase(url, DBName, username, password)){
                System.out.println("Database \"" + DBName + "\" successfully created");

                if (Database.createTable(url, DBName, tableName, username, password)){
                    System.out.println("Table \"" + tableName + "\" successfully created");
                }
                else {
                    System.out.println("Unexpected error creating table \"" + tableName + "\"");
                    return;
                }
            }
            else {
                System.out.println("Unexpected error creating database \"" + DBName + "\"");
                return;
            }
        }
        System.out.println("____SERVICE AND DATABASE VERIFIED____");
        System.out.println("____BEGINNING READ/WRITE TEST____");

        // Write to DB
        ArrayList<GameData> gd = PgnParser.parse(filepath);
        int unwritten = Database.writeDB(gd, database, username, password, 100).size();

        if (unwritten != 0){
            System.out.println("SQL Detected " + unwritten + " duplicates which were unwritten");
        }
        else {
            System.out.println("All games written to database");
        }
        // Read DB and check consistency between records

        // Delete database and stop service
        System.out.println("____STOPPING SERVICE AND DELETING DATABASE____");
        if (test.dropDatabase(url, DBName, username, password)){
            System.out.println("Database deleted");
        }
        else {
            System.out.println("Error deleting database");
        }

        if (test.stopService(serviceName)){
            System.out.println("Service \'" + serviceName + "\' successfully stopped");
        }
        else {
            System.out.println("Error stopping service \'" + serviceName + "\'.");
        }

    }   


    public static void databaseExistsTest(String url, String DBName, String username, String password){
        if (Database.doesDatabaseExist(url, DBName, username, password)){
            System.out.println("Database " + "\"" + DBName + "\" EXISTS.");
        }
        else{
            System.out.println("Database " + "\"" + DBName + "\" does NOT EXIST.");
        }
    }


    public static boolean stopService(String serviceName){
        try{
            Process process = new ProcessBuilder("sc", "stop", serviceName).start();
            int exitCode = process.waitFor();

            if (exitCode == 0){
                return true;
            }
            else {
                System.out.println("Unexpected exit code attempting to stop service");
                return false;
            }

        }
        catch (Exception e){
            System.out.println("Error stopping service: " + e);
            return false;
        }
    }

    public static boolean dropDatabase(String url, String DBName, String username, String password){
        try(Connection conn = DriverManager.getConnection(url + "/" + DBName, username, password)){
            String query = "DROP DATABASE pgn_database";
            PreparedStatement statement = conn.prepareStatement(query);
            
            statement.executeUpdate();
            
            return true;
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }


    public static void databaseProfiler(String DBName, String serviceName, String tableName, String url, String username, String password){
        if (!Database.doesServiceExist(serviceName)){
            System.out.println("Service + \'" + serviceName + "\' not found on this system.");
            return;
        }

        try{
            if (!Database.isServiceRunning(serviceName)){
                System.out.println("Service \'" + serviceName + "\' is not currently running");
                if (Database.startService(serviceName)){
                    System.out.println("Successfully started service");
                }
                else {
                    System.out.println("Error starting service");
                    return;
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
            return;
        }
    }

    public static boolean pgnRegexTest(String filePath){
        // (?m)^\[.*\](?:\n\[.*\])*\n\n((?:\d+\.[^\[]+\n?)+)
        // String reg = "(?m)^\\[.*\\](?:\\r?\\n\\[.*\\])*[\\r?\\n]{2}((?:\\d+\\.[^\\[]+\\r?\\n?)+)";
        String reg = "(((\\[.+?\\]\r\n)+?\r\n(([^\\[]+?\r\n)+?))(\r\n)?)+?";

        Pattern pattern = Pattern.compile(reg);
        Path pathObj = Path.of(filePath);
        String pgnText = null;

        try {
            pgnText = Files.readString(pathObj);
        }
        catch (IOException e){
            System.out.println("Exception occurred:" + e.getClass().getSimpleName());
            return false;
        }
        
        if (!(pgnText == null)){
            
            if (pattern.matcher(pgnText).matches()){
                return true;
            }
            else {
                return false;
            }
            
        }
        else {
            System.out.println("Null text provided, aborting with false");
            return false;
        }

    }

    public static ArrayList<Path> getFilePaths(String dirPath){
        ArrayList<Path> returnPaths = new ArrayList<>();
        try{
            DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of(dirPath));
            for (Path pth : stream){
                if (Files.isRegularFile(pth)){
                    System.out.println(pth);
                    returnPaths.add(pth);
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return returnPaths;
    }

    public static String fileStringTest(String filepath){
        try{
            return Files.readString(Path.of(filepath));
        }
        catch (Exception e){
            e.printStackTrace();
            return "";
        }
    }

    public static ArrayList<Long> generateWhitePawnMoveMask(){
        
        ArrayList<Long> wPawnMoveMask = new ArrayList<>();
        int rank;
        int file;

        for (int i = 0; i < 64; i++){
            // Calculating for white pawns
            // Pawns cannot start on the first rank, have no valid moves on the last rank (Would have promoted)
            if (i < 8 || i > 56){
                wPawnMoveMask.add(0L);
                continue;
            }
            else {
                Long board = 0L;

                // Pawn move 1 forward
                int bitshift;
                bitshift = 63 - (i + 8); // Calculate how many to shift down for the correct square
                board |= (1L << bitshift);

                // Add first pawn move 2 forward
                if (8 <= i && i <= 15){
                    bitshift = 63 - (i + 16);
                    board |= (1L << (bitshift));
                }

                wPawnMoveMask.add(board);
            } // end if
        } // end for

        return wPawnMoveMask;
    }

    public static void bitboardVisualize(Long bitboard){
        StringBuilder sbRank = new StringBuilder();
        StringBuilder sbFile = new StringBuilder();
        String bitString = String.format("%64s", Long.toBinaryString(bitboard)).replace(' ', '0');

        for (int i = 0; i < 64; i++){

            if (i % 8 == 0){
                sbRank.reverse().append("\n");
                sbFile.append(sbRank.toString());
                sbRank.setLength(0);
            }

            sbRank.append(bitString.charAt(i) + " ");
        }
        sbFile.append(sbRank.reverse().append('\n').toString());
        
        sbFile.reverse();
        sbFile.append("\n");

        System.out.print(sbFile.toString());

    }

    public static String longToString(Long num){
        return String.format("%64s", Long.toBinaryString(num)).replace(' ', '0');
    }


    public static Long hyperbolicQuintessence(Long occupancy, Long moveMask, Long pieceMask){
        Long retBits;

        //retBits = ((occupancy & moveMask) - (2 * pieceMask)) ^ (Long.reverse(Long.reverse(occupancy & moveMask) - 2 * Long.reverse(pieceMask))) & moveMask;
        retBits = ((occupancy & moveMask) - (2 * pieceMask)) ^ (Long.reverse(Long.reverse(occupancy & moveMask) - Long.reverse(2 *pieceMask))) & moveMask;
        return retBits;
    }

    public static Long toFileMajor(Long rankMajorBitboard){
        StringBuilder sb = new StringBuilder();

        char[] longChars = test.longToString(rankMajorBitboard).toCharArray();

        for (int i = 0; i < 8; i++){
            for (int j = 0; j < 8; j++){
                sb.append();
            }
        }
    }

    public static Long toRankMajor(Long fileMajorBitboard){
        Long retBoard = 0L;

        for (int i = 0; i < 8; i++){
            long col = (fileMajorBitboard >> (i + 8)) & 0x0101010101010101L;
            retBoard |= col << i;
        }

        return retBoard;
    }
}
