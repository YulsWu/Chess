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

    public static void databaseInterfaceTest(String url, String database, String DBName, String tableName, String serviceName, String username, String password){
        // Does service exist
        try{
            if (Database.doesServiceExist(serviceName)){
                System.out.println("Found installed service " + serviceName);
            }
            else {
                System.out.println("Service \"" + serviceName + "\" does not exist");
                return;
            }
        }
        catch (Exception ex){
            System.out.println("Unexpected error occured: " + ex);
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
            if (Database.doesTableExist(database, tableName, username, password)){
                System.out.println("Found table \"" + tableName + "\"");
            }
            else {
                System.out.println("Table \"" + tableName + "\" not found");

                if (Database.createTable(database, tableName, username, password)){
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

                if (Database.createTable(database, tableName, username, password)){
                    System.out.println("Table \"" + tableName + "\" successfully created");
                }
                else {
                    System.out.println("Unexpected error creating table \"" + tableName + "\"");
                    return;
                }
            }
            else{
                System.out.println("Unexpected error creating database \"" + DBName + "\"");
                return;
            }
        }

        // Check table existence

    }   

    public static void serviceQueryExceptionTest(String serviceName){
        boolean exists = false;

        System.out.println("\nTEST ::: CHECK IF SERVICE EXISTS, THEN CHECK IF RUNNING");
        try{
            if (Database.doesServiceExist(serviceName)){
                System.out.println("Service " + "\"" + serviceName + "\"" + " EXISTS in this system.");
                exists = true;
                try {
                    if (Database.isServiceRunning(serviceName)){
                        System.out.println("Service " + "\"" + serviceName + "\"" + " is currently RUNNING.");

                    }
                    else {
                        System.out.println("Service " + "\"" + serviceName + "\"" + " is currently STOPPED.");
                    }
                }
                catch(ChessServiceDoesNotExistException e){
                    System.out.println("ERROR: Attempted to query a non-existent service");
                }
            }
            else {
                System.out.println("Service " + "\"" + serviceName + "\"" + " DOES NOT EXIST in this system.");
            }
        }
        catch (ChessServiceException ex){
            System.out.println("ERROR: Unexpected return code from querying service " + serviceName + ".");
        }

        if (!exists){
            try {
            System.out.println("\n\nTEST:::CHECKING IF NON-EXISTENT SERVICE IS RUNNING");
            Database.isServiceRunning(serviceName);
    
            }
            catch (ChessServiceException e){
                e.printStackTrace();
            }
        }
    }

    public static void serviceStartTest(String serviceName){
        System.out.println("TEST ::: STARTING SERVICE:");
        try{
            Database.startService(serviceName);
            System.out.println("SUCCESSFULLY STARTED " + serviceName + " SERVICE");
        }
        catch (ChessServiceDoesNotExistException e){
            System.out.println("ERROR: SERVICE DOES NOT EXIST");
        }
        catch (ChessServiceException ex){
            System.out.println("ERROR: UNEXPECTED ERROR ENCOUNTERED STARTING SERVICE.");
            ex.printStackTrace();
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

    public static void tableExistsTest(String database, String tableName, String username, String password){
        if (Database.doesTableExist(database, tableName, username, password)){
            System.out.println("Table \"" + tableName + "\" EXISTS in database");
        }
        else {
            System.out.println("Table \"" + tableName + "\" does NOT EXIST in database");
        }
    }
}
