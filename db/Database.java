package db;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.sql.PreparedStatement;
import java.io.ObjectOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.sql.ResultSet;
import java.util.Set;

import exceptions.ChessServiceDoesNotExistException;
import parser.PgnParser;

import java.util.HashSet;
import java.sql.Types;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Map;


public class Database {
    //#region Static class variables
    // Excludes stringID and moves, 29 total meta fields
    public static final Map<String, String> PGN_TO_SQL_META_LABELS = Map.ofEntries(
        Map.entry("Event", "chess_event"),
        Map.entry("Site", "site"),
        Map.entry("Date", "game_date"),
        Map.entry("Round", "round"),
        Map.entry("White", "white_player"),
        Map.entry("Black", "black_player"),
        Map.entry("Result", "result"),
        Map.entry("WhiteElo", "white_elo"),
        Map.entry("BlackElo", "black_elo"),
        Map.entry("ECO", "eco"),
        Map.entry("WhiteTitle", "white_title"),
        Map.entry("BlackTitle", "black_title"),
        Map.entry("WhiteFideId", "white_fide_id"),
        Map.entry("BlackFideId", "black_fide_id"),
        Map.entry("Opening", "opening"),
        Map.entry("Variation", "variation"),
        Map.entry("TimeControl", "time_control"),
        Map.entry("Termination", "termination"),
        Map.entry("Mode", "game_mode"),
        Map.entry("PlyCount", "ply_count"),
        Map.entry("EventType", "event_type"),
        Map.entry("EventRounds", "event_rounds"),
        Map.entry("Stage", "stage"),
        Map.entry("Annotator", "annotator"),
        Map.entry("Source", "pgn_source"),
        Map.entry("SourceDate", "source_date"),
        Map.entry("FEN", "fen"),
        Map.entry("SetUp", "set_up"),
        Map.entry("Variant", "variant")
    );

    public static final Map<String, String> SQL_TO_PGN_META_LABELS = Map.ofEntries(
        Map.entry("chess_event", "Event"),
        Map.entry("site", "Site"),
        Map.entry("game_date", "Date"),
        Map.entry("round", "Round"),
        Map.entry("white_player", "White"),
        Map.entry("black_player", "Black"),
        Map.entry("result", "Result"),
        Map.entry("white_elo", "WhiteElo"),
        Map.entry("black_elo", "BlackElo"),
        Map.entry("eco", "ECO"),
        Map.entry("white_title", "WhiteTitle"),
        Map.entry("black_title", "BlackTitle"),
        Map.entry("white_fide_id", "WhiteFideId"),
        Map.entry("black_fide_id", "BlackFideId"),
        Map.entry("opening", "Opening"),
        Map.entry("variation", "Variation"),
        Map.entry("time_control", "TimeControl"),
        Map.entry("termination", "Termination"),
        Map.entry("game_mode", "Mode"),
        Map.entry("ply_count", "PlyCount"),
        Map.entry("event_type", "EventType"),
        Map.entry("event_rounds", "EventRounds"),
        Map.entry("stage", "Stage"),
        Map.entry("annotator", "Annotator"),
        Map.entry("pgn_source", "Source"),
        Map.entry("source_date", "SourceDate"),
        Map.entry("fen", "FEN"),
        Map.entry("set_up", "SetUp"),
        Map.entry("variant", "Variant")
    );

    public static final String SQL_GAMES_TABLE_DDL = 
        "CREATE TABLE pgn_database.games (" + 
        "id VARCHAR(200) PRIMARY KEY," +	
        "chess_event VARCHAR(50)," +
        "site VARCHAR(50)," +
        "game_date DATE," +
        "round FLOAT," +
        "white_player VARCHAR(35)," +
        "black_player VARCHAR(35)," +
        "result VARCHAR(7)," +
        "white_elo SMALLINT," +
        "black_elo SMALLINT," +
        "eco VARCHAR(3)," +
        "moves MEDIUMBLOB," +
        "white_title VARCHAR(3)," +
        "black_title VARCHAR(3)," +
        "white_fide_id INT," +
        "black_fide_id INT," +
        "opening VARCHAR(100)," +
        "variation VARCHAR(100)," +
        "time_control VARCHAR(15)," +
        "termination VARCHAR(30)," +
        "game_mode VARCHAR(10)," +
        "ply_count SMALLINT," +
        "event_type VARCHAR(15)," +
        "event_rounds FLOAT," +
        "stage VARCHAR(15)," +
        "annotator VARCHAR(20)," +
        "pgn_source VARCHAR(20)," +
        "source_date DATE," +
        "fen VARCHAR(90)," +
        "set_up BOOLEAN," +
        "variant VARCHAR(50)" +
        ")";
    
    //#endregion
    
    // Currently unsuitable for hash collision handling
    public static ArrayList<Integer> generate_hashcode(ArrayList<GameData> games){
        ArrayList<Integer> return_hashes = new ArrayList<>();

        for (GameData g : games){
            return_hashes.add(g.getStringMoves().hashCode());
        }
        
        return return_hashes;
        /*
        // Old implementation using the metadata string and SHA-256 hashes
        // ArrayList<String> return_hashes = new ArrayList<>();

        // for (game_data g : games){
        //     ArrayList<String> meta = g.meta;
        //     StringBuilder meta_stringbuilder = new StringBuilder();
        //     String meta_string;

        //     for (String m : meta){
        //         meta_stringbuilder.append(m);
        //     }

        //     // Release the StringBuilder to release memory during the rest of execution
        //     meta_string = meta_stringbuilder.toString();
        //     meta_stringbuilder = null;

        //     try{
        //         byte[] temp;
        //         StringBuilder hex_string = new StringBuilder();
        //         MessageDigest msg = MessageDigest.getInstance("SHA-256");
        //         temp = msg.digest(meta_string.getBytes());

        //         for (byte b : temp){
        //             String hex = Integer.toHexString(0xff & b);
        //             if (hex.length() == 1){
        //                 hex_string.append('0');
        //             }
        //             hex_string.append(hex);
        //         }

        //         return_hashes.add(hex_string.toString());
            
        //     } catch (NoSuchAlgorithmException e){
        //         System.out.println("Error generating SQL hash: " + e);
        //     }
        // }
        // return return_hashes;
        */
    }

    // Overloaded method signature for a single game
    public static ArrayList<Integer> generate_hashcode(GameData game){
        ArrayList<GameData> temp = new ArrayList<>();
        temp.add(game);

        return generate_hashcode(temp);
    }


    public static ArrayList<GameData> writeDB(ArrayList<GameData> games, String database, String username, String password, int batch_size){
        ArrayList<GameData> unwritten = new ArrayList<>();
        PreparedStatement stmt;

        int i = 0;
        try(Connection conn = DriverManager.getConnection(database, username, password);
            ByteArrayOutputStream byte_stream = new ByteArrayOutputStream();
            ObjectOutputStream obj_stream = new ObjectOutputStream(byte_stream)){
            
            try{
                if (!(conn == null)){
                    System.out.println("Connection successful");
                }
                conn.setAutoCommit(false); // Batch processing of data

                // Remove duplicate entries
                if ((removeDuplicates(games, conn) == 0)){
                    System.out.println("Error detecting duplicates, aborting write.");
                    return games;
                }

                if (games.size() == 0){
                    System.out.println("All provided games already exist in the database.");
                    return games;
                }

                // Build SQL query with the information stored in game_data
                // 29 metadata fields + ID + moveset = 31 total database fields
                String query = "INSERT INTO games " + 
                                "(id, chess_event, site, game_date, round, white_player, black_player," + 
                                " result, white_elo, black_elo, eco, moves, white_title, black_title, " + 
                                "white_fide_id, black_fide_id, opening, variation, time_control, termination," + 
                                " game_mode, ply_count, event_type, event_rounds, stage, annotator, pgn_source," + 
                                " source_date, fen, set_up, variant) VALUES " + 
                                " (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                // Loop to create a query for each game that is to be written to the database
                // Begins by building the query string iteratively
                stmt = conn.prepareStatement(query);
                i = 0;
                for (GameData gd : games){
                    setValues(gd, stmt, byte_stream, obj_stream);

                    stmt.addBatch();

                    if (((i + 1) % batch_size == 0) || (i == games.size() - 1)){
                        stmt.executeBatch();
                        conn.commit();
                        System.out.print("\rAdded " + (i + 1) + " games...");
                    }

                    i++;
                }
                System.out.println();
            }
            catch (SQLException ex){
                System.out.println("SQLException: " + ex.getMessage());
                System.out.println("SQLState: " + ex.getSQLState());
                System.out.println("VendorError: " + ex.getErrorCode());
                
                conn.rollback();
                unwritten = new ArrayList<GameData>(games.subList((int)(i / batch_size) * batch_size, games.size()));
                return unwritten;
            }
            catch (IOException e){
                System.out.println("Stream error: " + e);

                conn.rollback();
                unwritten = new ArrayList<GameData>(games.subList((int)(i / batch_size) * batch_size, games.size()));
                return unwritten;
            }
        }
        catch (Exception e) {
            System.out.println("Error rolling back database: " + e);
        }

        System.out.println("Successfully added games to database");

        return unwritten; // Should be empty if it reaches this point
    }

    public static ArrayList<GameData> readDB(String database, String username, String password){
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

    private static void setValues(GameData game, PreparedStatement stmt, ByteArrayOutputStream ba_OS, ObjectOutputStream o_OS) throws Exception{
        // Serialize moveset
        byte[] byte_moves;
        o_OS.writeObject(game.moves);
        byte_moves = ba_OS.toByteArray();

        // Set values of PreparedStatment
        stmt.setString(1, game.stringID);
        stmt.setString(2, game.event);
        stmt.setString(3, game.site);
        stmt.setObject(4, game.date, Types.DATE);
        stmt.setObject(5, game.round, Types.FLOAT);
        stmt.setString(6, game.whitePlayer);
        stmt.setString(7, game.blackPlayer);
        stmt.setString(8, game.result);
        stmt.setObject(9, game.whiteElo, Types.SMALLINT);
        stmt.setObject(10, game.blackElo, Types.SMALLINT);
        stmt.setString(11, game.eco);
        stmt.setBytes(12, byte_moves); // Should never be null
        stmt.setString(13, game.whiteTitle);
        stmt.setString(14, game.blackTitle);
        stmt.setObject(15, game.whiteFideId, Types.INTEGER);
        stmt.setObject(16, game.blackFideId, Types.INTEGER);
        stmt.setString(17, game.opening);
        stmt.setString(18, game.variation);
        stmt.setString(19, game.timeControl);
        stmt.setString(20, game.termination);
        stmt.setString(21, game.mode);
        stmt.setObject(22, game.plyCount, Types.SMALLINT);
        stmt.setString(23, game.eventType);
        stmt.setObject(24, game.eventRounds, Types.FLOAT);
        stmt.setString(25, game.stage);
        stmt.setString(26, game.annotator);
        stmt.setString(27, game.source);
        stmt.setObject(28, game.sourceDate, Types.DATE);
        stmt.setString(29, game.fen);
        // Specific handling of null as setBoolean() doesn't work well with null
        // Other set functions should work properly
        if (game.setUp != null){
            stmt.setBoolean(30, game.setUp);
        }
        else {
            stmt.setNull(30, Types.BOOLEAN);
        }
        stmt.setString(31, game.variant);
    }

    private static int removeDuplicates(ArrayList<GameData> games, Connection conn){
        int init_size = games.size();
        String q = "SELECT id FROM pgn_database.games";
        ResultSet results;
        Set<String> result_set = new HashSet<String>();

        try{
            PreparedStatement stmt = conn.prepareStatement(q);
            results = stmt.executeQuery();

            while (results.next()){
                result_set.add(results.getString(1));
            }
        }
        catch (SQLException e){
            System.out.println("Error occurred during duplicate checking: " + e);
            return 0;
        }

        games.removeIf(obj -> result_set.contains(obj.stringID));
        if (init_size == games.size()){
            System.out.println("No duplicates detected");
        }
        else {
            System.out.println("Removed " + (init_size - games.size()) + " duplicate games");
        }
        return 1;
    }

    public static String dateToString(Date d){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd"); // Maybe store format as a constant?
        return sdf.format(d);
    }

    public static boolean isServiceRunning(String serviceName) throws ChessServiceDoesNotExistException{
            try{
                Process myProcess = new ProcessBuilder("sc", "query", serviceName).start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(myProcess.getInputStream()));
                StringBuilder sb  = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null){
                    sb.append(line + '\n');
                }

                int exitCode = myProcess.waitFor();

                if (exitCode == 0){
                    if (sb.toString().contains("RUNNING")){
                        return true;
                    }
                }
                else if (exitCode == 1060){
                    throw new ChessServiceDoesNotExistException("Service does not exist.");
                }
                else{
                    System.out.println("Unexpected exit code from querying services");
                    return false;
                }
            }
            catch(IOException e){
                System.out.println("Error reading data from service query");
                return false;
            }
            catch (InterruptedException ex) {
                System.out.println("Error: Service interrupted while waiting to finish execution");
                return false;
            }
        return false;
    }

    public static boolean doesServiceExist(String serviceName){
        try {
            Process myProcess = new ProcessBuilder("sc", "query", serviceName).start();
            int exitCode = myProcess.waitFor();

            if (exitCode == 0){
                return true;
            }
            else if (exitCode == 1060){
                return false;
            }
            else {
                System.out.println("Unexpected error code returned from services query");
                return false;
            }
        
        }
        catch (IOException e){
            System.out.println("Error creating process to query services:");
            return false;
        }
        catch (InterruptedException ex){
            System.out.println("Error: Service interrupted while waiting to finish execution");
            return false;
        }

    }

    public static boolean startService(String serviceName) {
        if (!doesServiceExist(serviceName)){
            System.out.println("ERROR: Attempting to start a non-existent service.");
            return false;
        }
        
        try {
            Process process = new ProcessBuilder("sc", "start", serviceName).start();
            
            while (!isServiceRunning(serviceName)){};
            
            int exitCode = process.waitFor();

            if (exitCode == 0){
                return true;
            }
            // Service already running
            else if (exitCode == 1056){
                return true;
            }
        }
        catch (Exception e){ 
            System.out.println("ERROR: Attempt to start " + serviceName + " has failed.");
            return false;
        }
        return true;
    }
    
    public static boolean doesDatabaseExist(String url, String DBName, String username, String password){
        try(Connection conn = DriverManager.getConnection(url, username, password)){
            String query = "SHOW DATABASES";
            PreparedStatement statement = conn.prepareStatement(query);
            ResultSet rs = statement.executeQuery();

            while(rs.next()){
                String db = rs.getString("Database");
                if (db.equals(DBName)){
                    return true;
                }
            }
            return false;

        }
        catch (SQLException e){
            // Do something else??
            return false;
        }
    }

    public static boolean doesTableExist(String url, String DBName, String tableName, String username, String password){
        try (Connection conn = DriverManager.getConnection(url + "/" + DBName, username, password)){
            String query = "SHOW TABLES from pgn_database";
            PreparedStatement statement = conn.prepareStatement(query);
            ResultSet results = statement.executeQuery();

            while (results.next()){
                String table = results.getString("Tables_in_pgn_database");
                if (table.equals(tableName)){
                    return true;
                }
            }
            return false;
        }
        catch (Exception e){
            System.out.println("Error checking table existence in database" + e);
            return false;
        }
    }

    public static boolean createDatabase(String url, String DBName, String username, String password){
        try (Connection conn = DriverManager.getConnection(url, username, password)){
            PreparedStatement statement = conn.prepareStatement("CREATE DATABASE " + DBName);

            // Return value is always 0 and is therefore useless
            statement.executeUpdate();

            return true;
        }
        catch (Exception e){
            System.out.println("Error encountered while creating new database \"" + DBName + "\": " + e);
            return false;
        }
    }

    public static boolean createTable(String url, String DBName, String tableName, String username, String password){
        try (Connection conn = DriverManager.getConnection(url + "/" + DBName, username, password)){
            PreparedStatement statement = conn.prepareStatement(SQL_GAMES_TABLE_DDL);
            // Return value always 0 for DDL statements
            statement.executeUpdate();
            return true;
        }
        catch (SQLException e){
            System.out.println("Error encountered while creating new table \"" + tableName + "\": " + e);
            return false;
        }
    }

}

