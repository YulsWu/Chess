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

import com.YCorp.chessApp.client.exceptions.ChessServiceDoesNotExistException;
import com.YCorp.chessApp.server.db.RegexGameData;

import java.util.HashSet;
import java.util.Collections;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Blob;

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
    "CREATE TABLE games (" + 
    "id BINARY(16) PRIMARY KEY," +	
    "chess_event VARCHAR(50)," +
    "site VARCHAR(50)," +
    "game_date DATE," +
    "round VARCHAR(10)," +
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

    // public static final String SERVER_URL = "jdbc:mysql://localhost:3306";
    // public static final String DB_NAME = "pgn_database";
    // public static final String DB_URL = SERVER_URL + "/" + DB_NAME;
    // public static final String DB_USERNAME = "root";
    // public static final String DB_PASSWORD = "fUZ8&ejS4]";
    
    public static final String DB_TABLE_NAME = "games";
    public static final String DB_PATH = "jdbc:sqlite:C:/Users/yulun/AppData/Local/Programs/Java/Chess/src/main/resources/database/games.sqlite";
    public static final String DB_QUERY_TABLE = "SELECT name FROM sqlite_master WHERE type='table' AND name='games'";
    public static final String USER_PATH = System.getenv("USERPROFILE");

    //#endregion

    public static ArrayList<RegexGameData> writeDB(ArrayList<RegexGameData> games, int batchSize){
        ArrayList<RegexGameData> unwrittenGames = new ArrayList<>();
        if (games.size() == 0){
            return new ArrayList<RegexGameData>();
        }

        PreparedStatement stmt;

        try(Connection conn = DriverManager.getConnection(DB_PATH))
        {   // Ensure successful connection
            if (!(conn == null)){
                System.out.println("writeDB(): Connection successful");
            }
            else {
                System.out.println("writeDB() ERROR: Null connection detected, returning.");
                return unwrittenGames;
            }

            // Determine if table exists, create if not
            if (conn.createStatement().executeQuery(RegexDatabase.DB_QUERY_TABLE).next()){
                
            }
            else {
                System.out.println("writeDB(): Creating new table \'games\'");
                conn.createStatement().executeUpdate(RegexDatabase.SQL_GAMES_TABLE_DDL);
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
                // Don't clear?
                if (Thread.currentThread().isInterrupted()){
                    conn.rollback();
                    System.out.println("writeDB(): Interrupted, wrote " + ((i + 1)/batchSize) * batchSize + " games out of " + games.size());
                    return new ArrayList<RegexGameData>();
                }
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
                        System.out.print("Wrote " + (i + 1) + " games...\r");
                    }
                    System.out.println();
    
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

        ByteArrayOutputStream movesByteOutput = new ByteArrayOutputStream();
        ObjectOutputStream movesObjectOutput = new ObjectOutputStream(movesByteOutput);

        ByteArrayOutputStream metaByteOutput = new ByteArrayOutputStream();
        ObjectOutputStream metaObjectOutput = new ObjectOutputStream(metaByteOutput);

        // Serialize moves
        // Must read in the same order as they're written
        movesObjectOutput.writeObject(game.getMoves());
        metaObjectOutput.writeObject(game.getOptionalMeta());

        movesBytes = movesByteOutput.toByteArray();
        optionalMetaBytes = metaByteOutput.toByteArray();

        stmt.setBytes(1, game.ID);
        stmt.setString(2, game.event);
        stmt.setString(3, game.site);
        stmt.setObject(4, game.date, Types.DATE);
        stmt.setString(5, game.round);
        stmt.setString(6, game.whitePlayer);
        stmt.setString(7, game.blackPlayer);
        stmt.setString(8, game.result);
        stmt.setBytes(9, movesBytes);
        stmt.setBytes(10, optionalMetaBytes);
    }
    
    private static int removeDuplicateGames(ArrayList<RegexGameData> rgd, Connection conn){
        int initSize = rgd.size();
        String q = "SELECT id FROM games";
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

    public static HashMap<byte[], String[]> queryGames(){
        HashMap<byte[], String[]> retMap = new HashMap<>();
        String statement = "SELECT id, white_player, black_player, game_date FROM games";

        try (
            Connection conn = DriverManager.getConnection(DB_PATH);
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
            "\tSELECT white_player AS player FROM games\n"+
            "\tUNION ALL\n"+
            "\tSELECT black_player FROM games\n"+
        ")\n" +
        "SELECT player, COUNT(*) AS appearances\n"+
        "FROM all_players\n" + 
        "GROUP BY player\n";

        try(
            Connection conn = DriverManager.getConnection(DB_PATH);
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

        String query = "SELECT chess_event, game_date, white_player, black_player, result FROM " + DB_TABLE_NAME + " WHERE white_player = \"" + player + "\" OR black_player = \"" + player + "\"";

        try(
            Connection conn = DriverManager.getConnection(DB_PATH);
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

    public static ArrayList<String> getUniquePlayers(){
        ArrayList<String> retArray = new ArrayList<>();
        String query = "SELECT white_player as value FROM games UNION SELECT black_player as value FROM games";

        try(
            Connection conn = DriverManager.getConnection(DB_PATH);
            PreparedStatement statement = conn.prepareStatement(query);
            ResultSet rs = statement.executeQuery();
            ){
                while(rs.next()){
                    retArray.add(rs.getString("value"));
                }
            }
        catch (Exception e){
            e.printStackTrace();
        }

        return retArray;
    }

    public static ArrayList<String> getUniqueSites(){
        ArrayList<String> retArray = new ArrayList<>();
        String query = "SELECT DISTINCT site FROM games";

        try(
            Connection conn = DriverManager.getConnection(DB_PATH);
            PreparedStatement statement = conn.prepareStatement(query);
            ResultSet rs = statement.executeQuery();
            ){
                while(rs.next()){
                    retArray.add(rs.getString("site"));
                }
            }
        catch (Exception e){
            e.printStackTrace();
        }

        return retArray;
    }

    public static ArrayList<String> getUniqueEvents(){
        ArrayList<String> retArray = new ArrayList<>();
        String query = "SELECT DISTINCT chess_event FROM games";

        try(
            Connection conn = DriverManager.getConnection(DB_PATH);
            PreparedStatement statement = conn.prepareStatement(query);
            ResultSet rs = statement.executeQuery();
            ){
                while(rs.next()){
                    retArray.add(rs.getString("chess_event"));
                }
            }
        catch (Exception e){
            e.printStackTrace();
        }

        return retArray;
    }

    public static int countAll(){
        String query = "SELECT COUNT(*) AS count FROM games";
        try(
            Connection conn = DriverManager.getConnection(DB_PATH);
            PreparedStatement statement = conn.prepareStatement(query);
            ResultSet rs = statement.executeQuery();
            ){
                while(rs.next()){
                    return rs.getInt("count");
                }
            }
        catch (Exception e){
            e.printStackTrace();
            return -1;
        }

        System.out.println("countAll(): ERROR");
        return -1;
    }

    public static ArrayList<BrowserEntry> readBrowserEntries(String query){
        ArrayList<BrowserEntry> retArray = new ArrayList<>();
        try (
            Connection conn = DriverManager.getConnection(DB_PATH);
            PreparedStatement statement = conn.prepareStatement(query);
            ResultSet rs = statement.executeQuery();
        )
        {
            while (rs.next()){
                retArray.add(new BrowserEntry(rs.getBytes("id"), rs.getString("white_player"), rs.getString("black_player"), rs.getString("site"), rs.getString("chess_event"), epochMillisToString(rs.getLong("game_date")), rs.getString("round"), rs.getString("result")));
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return retArray;
    }

    public static boolean doesTableExist(String table){
        String query = "SELECT name FROM sqlite_master WHERE type='table' AND name='" + table + "'";
        try (
            Connection conn = DriverManager.getConnection(DB_PATH);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
        ){
            return rs.next();
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public static void createGamesTable(){
        try (
            Connection conn = DriverManager.getConnection(DB_PATH);
            Statement stmt = conn.createStatement();
        )
        {
            stmt.executeUpdate(SQL_GAMES_TABLE_DDL);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    // Ensures existence of db and tables
    public static void assertDB(){
        if (!doesTableExist("games")){
            createGamesTable();
            System.out.println("assertDB(): No table found, creating");
        }
        else {
            System.out.println("assertDB(): DB and table already exist");
        }
    }

    private static String epochMillisToString(long epoch){
            return DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneId.of("UTC")).format(Instant.ofEpochSecond(epoch/1000));
    }

    public static int countQuery(String query){
        StringBuilder sb = new StringBuilder(query);
        sb.insert(0, "SELECT COUNT(*) FROM (").append(")");

        try (
            Connection conn = DriverManager.getConnection(DB_PATH);
            PreparedStatement statement = conn.prepareStatement(sb.toString());
            ResultSet rs = statement.executeQuery();
        )
        {
            while (rs.next()){
                return rs.getInt(1);
            }
            return -1;
        }
        catch(Exception e){
            e.printStackTrace();
            return -1;
        }
    }

    public static int deleteByID(ArrayList<byte[]> ids){
        StringBuilder sb = new StringBuilder("DELETE FROM ").append(DB_TABLE_NAME).append(" WHERE id IN (");

        for (int i = 0; i < ids.size(); i++){
            if (i != 0){
                sb.append(", ");
            }

            sb.append("?");
        }
        sb.append(")");

        try (
            Connection conn = DriverManager.getConnection(DB_PATH);
            PreparedStatement statement = conn.prepareStatement(sb.toString());
        )
        {
            for (int i = 0; i < ids.size(); i++){
                statement.setBytes((i + 1), ids.get(i));
            }

            return statement.executeUpdate();
        }
        catch(Exception e){
            System.out.println("deleteByID(): Exception " + e + " occurred when attempting to delete from database");
            return -1;
        }

    }

    public static int deleteAll(){
        String stmt = "DELETE FROM games";

        try(
            Connection conn = DriverManager.getConnection(DB_PATH);
            PreparedStatement statement = conn.prepareStatement(stmt);
        )
        {
            return statement.executeUpdate();
        }
        catch(Exception e){
            e.printStackTrace();
            return -1;
        }
    }

    public static RegexGameData readGameById(byte[] id){
        try (
            Connection conn = DriverManager.getConnection(DB_PATH);
            PreparedStatement statement = conn.prepareStatement("SELECT * FROM games WHERE id = ?");
        )
        {
            statement.setBytes(1, id);
            ResultSet results = statement.executeQuery();
            if (results.next()){
                System.out.println("FOUND HITS");

                // Deserialize ArrayList<String> for moves, and HashMap<String, String> for optional meta
                byte[] moveBytes = results.getBytes("moves");
                byte[] metaBytes = results.getBytes("optional_meta");

                ArrayList<String> moves = null;
                HashMap<String, String> optionalMeta = null;

                try (
                    ByteArrayInputStream moveBI = new ByteArrayInputStream(moveBytes);
                    ObjectInputStream moveOI = new ObjectInputStream(moveBI);

                    ByteArrayInputStream metaBI = new ByteArrayInputStream(metaBytes);
                    ObjectInputStream metaOI = new ObjectInputStream(metaBI);
                    )
                    {
                        moves = (ArrayList<String>) moveOI.readObject();
                        optionalMeta = (HashMap<String, String>) metaOI.readObject();
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        return null;
                    }

                return new RegexGameData(id, results.getString("chess_event"), results.getString("site"), results.getDate("game_date"), results.getString("round"), results.getString("white_player"), results.getString("black_player"), results.getString("result"), optionalMeta, moves);
            }
            else {
                System.out.println("NO HITS");
                return null;
            }
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }

    }
}
