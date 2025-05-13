package com.YCorp.chessApp.server.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.HashMap;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Blob;

import com.YCorp.chessApp.server.db.RegexGameData;



import com.YCorp.chessApp.client.exceptions.ChessServiceDoesNotExistException;

public class RegexDatabase {
    //#region Static SQL class attributes
    public static final Map<String, String> PGN_TO_SQL_META_LABELS = Map.ofEntries(
        Map.entry("Event", "chess_event"),
        Map.entry("Site", "site"),
        Map.entry("Date", "game_date"),
        Map.entry("Round", "round"),
        Map.entry("White", "white_player"),
        Map.entry("Black", "black_player"),
        Map.entry("Result", "result")
    );

    public static final Map<String, String> SQL_TO_PGN_META_LABELS = Map.ofEntries(
        Map.entry("chess_event", "Event"),
        Map.entry("site", "Site"),
        Map.entry("game_date", "Date"),
        Map.entry("round", "Round"),
        Map.entry("white_player", "White"),
        Map.entry("black_player", "Black"),
        Map.entry("result", "Result")
    );

    public static final String SQL_GAMES_TABLE_DDL = 
    "CREATE TABLE pgn_database.games (" + 
    "id BINARY(16) PRIMARY KEY," +	
    "chess_event VARCHAR(50)," +
    "site VARCHAR(50)," +
    "game_date DATE," +
    "round FLOAT," +
    "white_player VARCHAR(35)," +
    "black_player VARCHAR(35)," +
    "result VARCHAR(7)," +
    "moves MEDIUMBLOB," +
    "optional_meta MEDIUMBLOB)";

    public static final String SQL_INSERT_QUERY = 
    "INSERT INTO games " + 
        "(id, chess_event, site, game_date, round, white_player, black_player, result, moves, optional_meta)" + 
    "VALUES " + 
        "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
    ;
    //#endregion

    //#region Database Credentials

    public static final String SERVER_URL = "jdbc:mysql://localhost:3306";
    public static final String DB_NAME = "pgn_database";
    public static final String DB_URL = SERVER_URL + "/" + DB_NAME;
    public static final String DB_USERNAME = "root";
    public static final String DB_PASSWORD = "fUZ8&ejS4]";
    public static final String DB_TABLE_NAME = "games";

    //#endregion

    public static ArrayList<RegexGameData> writeDB(ArrayList<RegexGameData> games, int batchSize){
        ArrayList<RegexGameData> unwrittenGames = new ArrayList<>();

        PreparedStatement stmt;

        try(Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD))
        {   // Ensure successful connection
            if (!(conn == null)){
                System.out.println("writeDB(): Connection successful");
            }
            else {
                System.out.println("writeDB() ERROR: Null connection detected, returning.");
                return unwrittenGames;
            }

            // Remove duplicates from games list
            if (removeDuplicateGames(games, conn) == 0){
                System.out.println("writeDB() Error: Duplicate detection failed, aborting write.");
                return games;
            }
            
            if (games.size() == 0){
                System.out.println("writeDB(): All provided games already exist in database.");
                return games;
            }

            // Set autocommit to false for batch processing of games
            conn.setAutoCommit(false);

            stmt = conn.prepareStatement(SQL_INSERT_QUERY);

            int i = 0;
            for (RegexGameData gd : games){
                try{
                    // Inner try block to differentiate between exceptions arising from setValues() and ones from writing to the db
                    try{
                        setValues(gd, stmt);
                    }
                    catch(SQLException e){
                        System.out.println("writeDB() ERROR: Exception occurred setting values on statement on game " + gd.ID);
                        throw new SQLException("Exception occurred setting values");
                    }
                    catch (IOException e){
                        System.out.println("writeDB() ERROR: Exception occurred setting values on statement on game " + gd.ID);
                        throw new IOException ("Exception occurred opening streams");
                    }
                    
                    stmt.addBatch();
                    
                    if (((i + 1) % batchSize == 0) || (i == games.size() - 1)){
                        stmt.executeBatch();
                        conn.commit();
                        System.out.print("\rAdded " + (i + 1) + " games...");
                    }
    
                    i++;
                }
                catch (SQLException | IOException e){
                    System.out.println("writeDB() Error: Exception thrown while writing to database: " + e);
                    conn.rollback();

                    // If any errors occur duing the current "batch", then we return the current batch + rest of the list but not previous committed batches
                    // If batchSize == 50, and 0 <= i <= 48, then the whole current batch must be returned to be re-input
                    // If i == 49, then commit() could have failed, so we still need to return the whole batch, so we use batchSize--
                    int batchIndex = (i + 1) / batchSize;

                    // Special behaviour for if an exception occurs on a commit loop, then we return the entire last batch
                    // This is due to the fact that commit() may throw an error, and if it does then the entire batch is not commited
                    // But on commit loops i + 1 would be treated as the 'last batch'.
                    if ((i + 1) % batchSize == 0){
                        // If exception occurs on a commit loop on the first batch, set to 0, otherwise it would be -1
                        if (i <= 49){
                            batchIndex = 0;
                        }
                        // If not the first batch, just minus 1 to return the entire last batch
                        else {
                            batchIndex--;
                        }
                    }
                    // If not on a commit loop, we can just use batchIndex to return the current batch
                    unwrittenGames = new ArrayList<RegexGameData>(games.subList(batchIndex * batchSize, games.size()));
                    return unwrittenGames;
                }
            }
        }
        catch (SQLException e){
            System.out.println("writeDB() ERROR: Issue connecting to database");
            System.out.println(e);
        }

        return unwrittenGames;


    }

    // A new BAOS and OOS are created for each new game, preventing issues with clearing buffers etc
    private static void setValues(RegexGameData game, PreparedStatement stmt) throws IOException, SQLException{
        byte[] movesBytes;
        byte[] optionalMetaBytes;

        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);

        // Serialize moves
        // Must read in the same order as they're written
        objectOutput.writeObject(game.getMoves());
        objectOutput.writeObject(game.getOptionalMeta());

        movesBytes = byteOutput.toByteArray();
        optionalMetaBytes = byteOutput.toByteArray();

        stmt.setBytes(1, game.ID);
        stmt.setString(2, game.event);
        stmt.setString(3, game.site);
        stmt.setObject(4, game.date, Types.DATE);
        stmt.setObject(5, game.round, Types.FLOAT);
        stmt.setString(6, game.whitePlayer);
        stmt.setString(7, game.blackPlayer);
        stmt.setString(8, game.result);
        stmt.setBytes(9, movesBytes);
        stmt.setBytes(10, optionalMetaBytes);
    }
    
    private static int removeDuplicateGames(ArrayList<RegexGameData> rgd, Connection conn){
        int initSize = rgd.size();
        String q = "SELECT id FROM pgn_database.games";
        ResultSet results;
        Set<ByteBuffer> resultSet = new HashSet<>();

        try{
            PreparedStatement stmt = conn.prepareStatement(q);
            results = stmt.executeQuery();

            while(results.next()){
                resultSet.add(ByteBuffer.wrap(results.getBytes(1)));
            }
            results.close();
        }
        catch(SQLException e){
            System.out.println("removeDuplicateGames() Error: Exception occurred " + e);
            return 0;
        }
        // Using ByteBuffer to leverage HashSet O(1) contains() method
        // Easier than implementing custom class
        rgd.removeIf(obj -> resultSet.contains(ByteBuffer.wrap(obj.ID)));

        if (initSize == rgd.size()){
            System.out.println("No duplicates detected.");
        }
        else {
            System.out.println("Removed " + (initSize - rgd.size()) + " duplicate games.");
        }
        return 1;
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

    public static boolean doesServiceExist(String serviceName){
        try {
            Process myProcess = new ProcessBuilder("sc", "query", serviceName).start();
            int exitCode = myProcess.waitFor();

            if (exitCode == 0){
                System.out.println("Successfully started service " + serviceName);
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

    public static boolean doesDatabaseExist(){
        try(Connection conn = DriverManager.getConnection(SERVER_URL, DB_USERNAME, DB_PASSWORD)){
            String query = "SHOW DATABASES";
            PreparedStatement statement = conn.prepareStatement(query);
            ResultSet rs = statement.executeQuery();

            while(rs.next()){
                String db = rs.getString("Database");
                if (db.equals(DB_NAME)){
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

    public static boolean doesTableExist(){
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)){
            String query = "SHOW TABLES from pgn_database";
            PreparedStatement statement = conn.prepareStatement(query);
            ResultSet results = statement.executeQuery();

            while (results.next()){
                String table = results.getString("Tables_in_pgn_database");
                if (table.equals(DB_TABLE_NAME)){
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

    public static boolean createDatabase(){
        try (Connection conn = DriverManager.getConnection(SERVER_URL, DB_USERNAME, DB_PASSWORD)){
            PreparedStatement statement = conn.prepareStatement("CREATE DATABASE " + DB_NAME);

            // Return value is always 0 and is therefore useless
            statement.executeUpdate();

            return true;
        }
        catch (Exception e){
            System.out.println("Error encountered while creating new database \"" + DB_NAME + "\": " + e);
            return false;
        }
    }

    public static boolean createTable(){
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)){
            PreparedStatement statement = conn.prepareStatement(SQL_GAMES_TABLE_DDL);
            // Return value always 0 for DDL statements
            statement.executeUpdate();
            return true;
        }
        catch (SQLException e){
            System.out.println("Error encountered while creating new table \"" + DB_TABLE_NAME + "\": " + e);
            return false;
        }
    }

    public static boolean dropDatabase(){
        try(Connection conn = DriverManager.getConnection(SERVER_URL, DB_USERNAME, DB_PASSWORD)){
            PreparedStatement statement = conn.prepareStatement("DROP DATABASE " + DB_NAME);
            statement.executeUpdate();
            return true;
        }
        catch (Exception e){
            System.out.println("Error encountered while attempting to drop database \"" + DB_NAME + "\": " + e);
            return false;
        }
    }

    public static HashMap<byte[], String[]> queryGames(){
        HashMap<byte[], String[]> retMap = new HashMap<>();
        String statement = "SELECT id, white_player, black_player, game_date FROM pgn_database.games";

        try (
            Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            PreparedStatement stmt = conn.prepareStatement(statement);
            ResultSet results = stmt.executeQuery();
            )
            {
                while (results.next()){
                    retMap.put(results.getBytes("id"), new String[]{results.getString("white_player"),
                                                                                results.getString("black_player"),
                                                                                RegexGameData.DATE_FORMAT.format(results.getDate("date"))
                                                                            }
                    );
                }
            }
        catch(SQLException e){
            System.out.println("queryGames() Error: Exception occured reading games " + e);
        }

        return retMap;
    }

    public static ArrayList<Map.Entry<String, Integer>> readPlayerCounts(){
        ArrayList<Map.Entry<String, Integer>> retArray = new ArrayList<Map.Entry<String, Integer>>();
        String query = 
        "WITH all_players AS (\n" + 
            "\tSELECT white_player AS player FROM pgn_database.games\n"+
            "\tUNION ALL\n"+
            "\tSELECT black_player FROM pgn_database.games\n"+
        ")\n" +
        "SELECT player, COUNT(*) AS appearances\n"+
        "FROM all_players\n" + 
        "GROUP BY player\n";

        try(
            Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            PreparedStatement statement = conn.prepareStatement(query);
            ResultSet rs = statement.executeQuery();
            ){
                while(rs.next()){
                    retArray.add(new AbstractMap.SimpleEntry<String, Integer>(rs.getString("player"), (int) rs.getInt("appearances")));
                }
            }
            catch (SQLException e){
                System.out.println("readPlayerCounts() Error: Exception occurred while querying database " + e);
                return new ArrayList<Map.Entry<String, Integer>>();
            }



        retArray.sort(Map.Entry.comparingByValue());
        Collections.reverse(retArray);

        return retArray;
    }

    // Date, event, White, Black, whiteElo, blackElo, Result, round
    public static ArrayList<String[]> readDBPlayer(String player){
        ArrayList<String[]> retArray = new ArrayList<>();

        String query = "SELECT chess_event, game_date, white_player, black_player, result FROM " + DB_NAME + "." + DB_TABLE_NAME + " WHERE white_player = \"" + player + "\" OR black_player = \"" + player + "\"";

        try(
            Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            PreparedStatement statement = conn.prepareStatement(query);
            ResultSet resultSet = statement.executeQuery();
        ){
            while (resultSet.next()){

                retArray.add(new String[] {resultSet.getString("chess_event"),
                                            RegexGameData.DATE_FORMAT.format(resultSet.getDate("game_date")),
                                            resultSet.getString("white_player"),
                                            resultSet.getString("black_player"),
                                            resultSet.getString("result")
                });                    
            }



        }catch(Exception e){
            System.out.println("filterReadOnPlayer() Exception: " + e);
            return retArray;
        }

        return retArray;
    }

    
}
