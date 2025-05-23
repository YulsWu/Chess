import java.util.ArrayList;
import java.util.Arrays;
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
import java.io.PrintStream;
import java.io.FileOutputStream;


import db.Database;
import db.GameData;
import engine.Board;
import parser.PgnParser;
import parser.RegexParser;
import exceptions.AlgebraicParseException;

import java.nio.file.*;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.Scanner;

import engine.Move;
import engine.Move.MOVE_TYPE;


public class test {

    public static final Map<Integer, String> CHESS_EMOJI = new HashMap<>(){{
        put(-6, "\u2654");
        put(-5, "\u2655");
        put(-4, "\u2656");
        put(-3, "\u2657");
        put(-2, "\u2658");
        put(-1, "\u2659");
        put(0, " ");
        put(1, "\u265F");
        put(2, "\u265E");
        put(3, "\u265D");
        put(4, "\u265C");
        put(5, "\u265B");
        put(6, "\u265A");
    }};

    public static Move[] foolsOnWhite = new Move[]{
        new Move(1, 13, 21, MOVE_TYPE.MOVE),
        new Move(-1 , 52, 36, MOVE_TYPE.MOVE),
        new Move(1, 14, 30, MOVE_TYPE.MOVE),
        new Move(-5, 59, 31, MOVE_TYPE.MOVE)
    };

    public static Move[] foolsOnBlack = new Move[]{
        new Move(1, 12, 20, MOVE_TYPE.MOVE),
        new Move(-1, 53, 45, MOVE_TYPE.MOVE),
        new Move(1, 13, 29, MOVE_TYPE.MOVE),
        new Move(-1, 54, 38, MOVE_TYPE.MOVE),
        new Move(5, 3, 39, MOVE_TYPE.MOVE)
    };

    public static ArrayList<int[]> primFoolsOnBlack = new ArrayList<>();
    public static ArrayList<int[]> primFoolsOnWhite = new ArrayList<>();


    static {
        primFoolsOnBlack.add(new int[]{2, 52, 36});
        primFoolsOnBlack.add(new int[]{2, 52, 36}); // 2: Pawn, 52: e7, 36: e5
    
        // White moves pawn from f2 to f3
        primFoolsOnBlack.add(new int[]{1, 53, 45}); // 1: Pawn, 53: f2, 45: f3
    
        // Black moves queen from d8 to h4 (checkmate)
        primFoolsOnBlack.add(new int[]{5, 4, 60}); // 5: Queen, 4: d8, 60: h4

        // White moves pawn from f2 to f3
        primFoolsOnWhite.add(new int[]{1, 53, 45}); // 1: Pawn, 53: f2, 45: f3

        // Black moves pawn from e7 to e5
        primFoolsOnWhite.add(new int[]{2, 52, 36}); // 2: Pawn, 52: e7, 36: e5

        // White moves pawn from g2 to g4
        primFoolsOnWhite.add(new int[]{1, 54, 38}); // 1: Pawn, 54: g2, 38: g4

        // Black moves queen from d8 to h4 (checkmate)
        primFoolsOnWhite.add(new int[]{5, 4, 60}); // 5: Queen, 4: d8, 60: h4

    }


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

    // occMask DOES NOT INCLUDE the piece in question
    // rayMask is the specific ray we're calculating at the moment
    // pieceMask is the location of the piece
    // Provide RLERF encoded parameters, returns RLERF encoded mask
    public static Long hypQuint(Long occMask, Long rayMask, Long pieceMask){
        Long retBits;
        // Reverse all provide bitmasks from RLERF --> LERF
        occMask = Long.reverse(occMask);
        rayMask = Long.reverse(rayMask);
        pieceMask = Long.reverse(pieceMask);

        // occMask becomes only the pieces in the path of the ray
        occMask = occMask & rayMask;

        //retBits = ((rayOcc & rayMask) - (2 * pieceMask)) ^ (Long.reverse(Long.reverse(rayOcc & rayMask) - 2 * Long.reverse(pieceMask))) & rayMask;
        //retBits = ((rayOcc & rayMask) - (2 * pieceMask)) ^ (Long.reverse(Long.reverse(rayOcc & rayMask) - Long.reverse(2 *pieceMask))) & rayMask;
        //retBits = ((rayMask & occMask) - (2 * pieceMask)) ^ Long.reverse(Long.reverse(rayMask & occMask) - Long.reverse(pieceMask * 2)) & rayMask;
        Long forward = rayMask & occMask;
        Long reverse = Long.reverse(forward);
        forward = forward - (2 * pieceMask);
        reverse = reverse - (2 * Long.reverse(pieceMask));

        // System.out.println("rayMask: " + test.longToString(rayMask));
        // System.out.println("occMask: " + test.longToString(occMask));
        // System.out.println("pieceMask: " + test.longToString(pieceMask));

        retBits = (forward ^ Long.reverse(reverse)) & rayMask;
       
        // Reverse final bitmask from LERF --> RLERF
        return Long.reverse(retBits);
        
    }

    // NEED TO OPTIMIZE if using every time we calculate moves, invokes stringbuilder, which may defeat the benefit from using bitboards
    public static Long transposeBitboard(Long board){
        StringBuilder sb = new StringBuilder();
        String longString = String.format("%64s", Long.toBinaryString(board)).replace(" ", "0");
        char[] boardArray = longString.toCharArray();

        for (int i = 0; i < 8; i++){
            for (int j = 0; j < 8; j++){
                sb.append(boardArray[j * 8 + i]);
            }
        }

        return Long.parseUnsignedLong(sb.toString(), 2);
    }
    // Optimised version of transposeBitboard using bitwise operations and bitmasks
    public static Long transpose(Long board){
        Long retBoard = 0L;
        int iteration = 0;
        for (int i = 0; i < 8; i++){
            for (int j = 0; j < 8; j++){
                // Shift down to current index
                int index = (j * 8) + i;
                Long indexBit = (1L << index);

                // Read board at index, 1 for something 0 for nothing
                indexBit &= board;

                // If we read a 1 at the index, shift down by that index and OR it, to add to retBoard
                // If not, then we skip the index as it is already 0
                if (indexBit != 0){
                    retBoard |= (1L << iteration);
                }
                iteration ++;
            }
        }
        return retBoard;
    }

    public static Long queenMoveTest(){
        Long retMask = 0L;
        Long vertMask = 0b0001000000010000000100000001000000010000000100000001000000010000L;
        Long horzMask = 0b0000000000000000000000001111111100000000000000000000000000000000L;
        Long diagMask = 0x8040201008040201L;
        Long antiDMask = 0b0000001000000100000010000001000000100000010000001000000000000000L;
        Long pieceMask = (1L << 63 - 27);
        Long occMask = 0b1111111111111111000000000000000000000000000000001111111111111111L;
        Long[] allMasks = new Long[]{vertMask, horzMask, diagMask, antiDMask};

        for (Long ray : allMasks){
            Long temp = test.hypQuint(occMask, ray, pieceMask);
            retMask |= temp;
        }
        return retMask;
        
    }
   
    public static void transpositionTest(int batchSize){
        ArrayList<Long> longList = new ArrayList<>();
        Random rand = new Random();

        Long oldTime = 0L;
        Long newTime = 0L;
        
        for (int i = 0; i < batchSize; i++){
            longList.add(rand.nextLong());
        }
        Long startTime = System.nanoTime();
        
        for (Long bit : longList){
            test.transpose(bit);
            newTime += System.nanoTime() - startTime;
        }
        
        startTime = System.nanoTime();
        
        for (Long bit : longList){
            test.transposeBitboard(bit);
            oldTime += System.nanoTime() - startTime;
        }
        
       

        System.out.println("transposeBitboard: Finished with avg " + oldTime/batchSize + "ms per call");
        System.out.println("transpose: Finished with avg " + newTime/batchSize + "ms per call");

        if (oldTime > newTime){
            System.out.println("transpose is approx " + oldTime / newTime + " times faster than transposeBitboard");
        }
        else {
            System.out.println("transposeBitboard is approx " + newTime/oldTime + " times faster than transpose");
        }
    }

    // Expensive generation of masks at runtime
    public static void rayCombinationTest(int position){
        ArrayList<Long> vertMasks = Board.generateVerticalRayMask();
        ArrayList<Long> horzMasks = Board.generateHorizontalRayMask();
        ArrayList<Long> diagMasks = Board.generateDiagonalRayMask();
        ArrayList<Long> antiMasks = Board.generateAntiRayMask();

        Long combinedRays = 0L;
        combinedRays |= vertMasks.get(position%8);
        combinedRays |= horzMasks.get(position/8);
        combinedRays |= diagMasks.get(position);
        combinedRays |= antiMasks.get(position);

        test.bitboardVisualize(combinedRays);
    }

    public static void boardVisualize(int[][] board){
        StringBuilder sbInner = new StringBuilder();
        StringBuilder sbOuter = new StringBuilder();


        for (int i = 0; i < 8; i++){
            sbInner.setLength(0);
            sbInner.append((i + 1) + " ");
            for (int j = 0; j < 8; j++){
                sbInner.append("[" + CHESS_EMOJI.get(board[i][j]) + " ]");
            }
            sbInner.append("\n");
            sbOuter.insert(0, sbInner.toString());
            String temp = sbOuter.toString();
        }
        sbOuter.append("   A   B   C   D   E   F   G   H");
        System.out.print(sbOuter.toString());
    }

    public static void countCheckTest(){
        Board myBoard = new Board();
        int[][] temp = new int[][]{
            {0,-6, 0, 0, 0, 0, 0, 0},
            {1, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 5, 0, 0, 0, 0},
            {0, 0, 0, 0,-5, 0, 0, 0},
            {0, 0, 0, 0, 0, 0,-3, 0},
            {0, 0, 0, 0, 0,-1, 0, 0},
            {0, 0, 0, 0, 6, 0, 0, 0},
            {0, 4, 0, 0, 0, 0, 0, 0},
        };

        int[][] newBoard = pieceBoardMaker(temp);
        myBoard.setBoard(newBoard);
        myBoard.setOcc(Board.boardToBitboard(newBoard));

        System.out.println();
        test.boardVisualize(myBoard.getBoard());
        System.out.println();
        System.out.println("Checks on white should be 2: Computed value is: " + myBoard.getOpponentChecks(1));
        System.out.println("Checks on black should be 3: Computed value is: " + myBoard.getOpponentChecks(-1));
        
    }

    // A somewhat more 
    public static int[][] pieceBoardMaker(int[][] temp){
        int[][] retBoard = new int[8][8];
        

        for (int i = 0; i < 8; i++){
            retBoard[i] = temp[(7 - i)];
        }

        return retBoard;
    }

    public static long shift(long board, int direction){
        return (direction > 0) ? (board >>> direction) : (board << -direction);
    }

    public static long generateEvasionPath(int origin, int destination){
        int direction;

        int originRank = origin / 8;
        int originFile = origin % 8;
        int originDiag = origin % 9;
        int originAnti = origin% 7;
        int destRank = destination / 8;
        int destFile = destination % 8;
        int destDiag = destination % 9;
        int destAnti = destination % 7;

        // Ensure two points lie on the same ray
        if (!((originRank == destRank) || (originFile == destFile) || (originAnti == destAnti) || (originDiag == destDiag))){
            System.out.print("Invalid origin and destination squares for evasion path generation");
            return 0L;
        }

        // Straight path
        if (originRank == destRank){
            // Right straight path
            if (destination > origin){
                direction = 1;
            }
            // left straight path
            else {
                direction = -1;
            }
        }
        else if (originFile == destFile){
            // Straight up
            if (destination > origin){
                direction = 8;
            }
            // Straight down
            else {
                direction = -8;
            }
        }
        // Up-right diagonal
        else if ((destRank > originRank) && (destFile > originFile)){
            direction = 9;
        }
        // Up-left diagonal
        else if ((destRank > originRank) && (destFile < originFile)){
            direction = 7;
        }
        // Down-right diagonal
        else if ((destRank < originRank) && (destFile > originFile)){
            direction = -7;
        }
        // Down-left diagonal
        // Just an 'else' to satisfy the compiler
        else {
            direction = -9;
        }

        long pos = (1L << (63 - origin));
        long retMask = (1L << (63 - origin));
        long endMask = (1L << (63 - destination));

        while ((pos = shift(pos, direction)) != endMask){
            retMask |= pos;
        }

        return retMask;
    }

    public static void testCheckEvasion(){
        // 2 checks on white and black
        int[][] temp1 = new int[][]{
            {-6, 0, 0, 0,-4, 0, 0, 0},
            { 1, 0, 0, 0, 0, 0, 0, 0},
            { 0, 0, 0, 0, 0, 0, 0, 0},
            { 0, 0, 0, 0, 0, 0, 0, 0},
            { 0, 0, 0, 0, 0, 0, 0, 0},
            { 0, 0, 0,-1, 0, 0, 0, 0},
            { 0, 0, 0, 0, 6, 0, 0, 0},
            { 0, 0, 0, 0, 0, 0, 0, 3},
        };
        int temp1solution = 2;
        // Black check escape
        int[][] temp2 = new int[][]{
            {-6, 0, 0, 0, 0, 0, 0, 0},
            { 0, 0, 0, 0, 0, 0, 0, 0},
            { 0, 0, 0, 0, 0, 0, 0, 0},
            {-2, 0, 0, 0, 0, 0, 0, 0},
            { 0, 0, 0, 0, 0, 0, 0, 0},
            { 0, 0, 0, 0, 0, 0, 0, 0},
            { 0, 0, 0, 0, 0, 0, 0, 0},
            { 0, 0, 0, 0, 0, 0, 0, 6},
        };
        

        int[][] temp3 = new int[][]{
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
        };

        int[][] temp4 = new int[][]{
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
        };




    }

    public static void generateCheckEvasionTest(int verbose){
        // White/Black
        // 1 check with all pieces
        // 1+ check with all piecs
        // Checkmate conditions
        // Simple king check on white

        //#region bitboards
        long bitboard0 = 0b0000000000011100000101000001110000000000000000000000000000000000L;
        long bitboard1 = 0b0000000000000000000000000000000000000000001110000010100000111000L;
        long bitboard2 = 0b0000000000001010000001100000001000000000000000000000000000000000L;
        long bitboard3 = 0b0000000000000000000000000000000100000000000011000000000000000100L;
        long bitboard4 = 0b0011100000100000001100000000010000000010000000000000000000000000L;
        long bitboard5 = 0b0000000000000000000000000000001000000100001100000010000000111000L;
        long bitboard6_7 = 0b0000010100000101000000000000000000000000000000001010000010100000L;
        long bitboard8_9 = 0b0000001000000000000000000000000000000000000000000000000001000000L;
        long bitboard10_11 = 0L;
        long bitboard12_13 = 0b0100000000000000000000000000000000000000000000000000000000000010L;
        long bitboard14_15 = 0b0001010000000100000000000000000000000000000000000010000000101000L;
        long bitboard16_17 = 0b0000000000000000010000111100001001000011110000100000000000000000L;

        //#endregion
        
        //#region bitIndices
        int[] bitIndices0 = new int[] {11, 12, 13, 19, 21, 27, 28, 29};
        int[] bitIndices1 = new int[] {42, 43, 44, 50, 52, 58, 59, 60};
        int[] bitIndices2 = new int[] {12, 14, 21, 22, 30};
        int[] bitIndices3 = new int[] {31, 44, 45, 61};
        int[] bitIndices4 = new int[] {3, 4, 10, 18, 19, 29, 38};
        int[] bitIndices5 = new int[] {30, 37, 42, 43, 50, 59, 60};
        int[] bitIndices6_7 = new int[] {5, 7, 13, 15, 48, 50, 56, 58};
        int[] bitIndices8_9 = new int[] {6, 57};
        int[] bitIndices10_11 = new int[]{};
        int[] bitIndices12_13 = new int[] {1, 62};
        int[] bitIndices14_15 = new int[] {3, 5, 13, 50, 58, 60};
        int[] bitIndices16_17 = new int[] {17, 22, 23, 24, 25, 30, 33, 38, 39, 40, 41, 46};
        
        //#endregion

        //#region pieceboards
        int[][] pieceboard0 = new int[][] {{0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 6, 0, 0, 0}, {0, 0, 0, -1, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}};
        int[][] pieceboard1 = new int[][] {{0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0,}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 1, 0, 0, 0}, {0, 0, 0, -6, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}};
        int[][] pieceboard2 = new int[][] {{0, 0, 0, 0, 0, 0, 0, -4}, {0, 0, 0, 0, 0, 6, 0, 0}, {0, 0, 0, 0, 2, 0, 0, 1}, {0, 0, 0, 0, 0, 0, -2, 0}, {0, 0, -3, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}};
        int[][] pieceboard3 = new int[][] {{0, 0, 0, 4, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {-4, 0, 0, 0, 0, 0, 0, 3}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, -6, -1, 0, 0}, {0, 0, 0, 0, -5, 0, 0, 0}};
        int[][] pieceboard4 = new int[][] {{0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 6, 1, 0, 0, -4}, {0, 0, 0, 0, 0, 1, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 1}, {0, 0, 0, 0, 0, 0, -3, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}};
        int[][] pieceboard5 = new int[][] {{0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 3, 0}, {0, 0, 0, 0, 0, 0, 0, -1}, {0, 0, 0, 0, 0, -1, 0, 0}, {0, 0, 0, -6, -1, 0, 0, 4}, {0, 0, 0, 0, 0, 0, 0, 0}};
        int[][] pieceboard6_7 = new int[][] {{0, 4, 0, 0, 0, 0, 6, 0}, {0, 0, 0, 0, 0, 0, 1, 0}, {0, 0, 0, 0, 0, 0, 0, -2}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {2, 0, 0, 0, 0, 0, 0, 0}, {0, -1, 0, 0, 0, 0, 0, 0}, {0, -6, 0, 0, 0, 0, -4, 0}};
        int[][] pieceboard8_9 = new int[][] {{0, 0, 0, 0, 0, 0, 0, 0}, {-4, 0, 0, 0, 0, 0, 3, 6}, {0, 0, 0, 0, 0, 0, 0, -5}, {0, 0, 0, 0, 0, 0, 0, -4}, {4, 0, 0, 0, 0, 0, 0, 0}, {5, 0, 0, 0, 0, 0, 0, 0}, {-6, -3, 0, 0, 0, 0, 0, 4}, {0, 0, 0, 0, 0, 0, 0, 0}};
        int[][] pieceboard10_11 = new int[][] {{0, 0, 0, -4, 0, 0, 6, 0}, {0, 0, 0, 0, 0, 3, 1, 1}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, -3, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 3, 0, 0, 0, 0}, {-1, -1, -2, 0, 0, 0, 1, 0}, {0, -6, 0, 0, 0, 0, 0, 4}};
        int[][] pieceboard12_13 = new int[][] {{0, 0, 6, 2, -4, 0, 0, 0}, {0, 5, 0, 3, 0, 0, 0, 0}, {-3, 0, 0, 0, -3, 0, 0, 0}, {0, 0, -4, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 4, 0, 0}, {0, 0, 0, 3, 0, 0, 0, 3}, {0, 0, 0, 0, -3, 0, -5, 0}, {0, 0, 0, 4, -2, -6, 0, 0}};
        int[][] pieceboard14_15 = new int[][] {{0, 0, 0, 0, 6, 0, 0, 0}, {2, 0, 0, 0, 1, 0, 0, 0}, {0, 0, -3, 0, 0, -2, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 2, 0, 0, 3, 0, 0}, {0, 0, 0, -1, 0, 0, 0, -2}, {0, 0, 0, -6, 0, 0, 0, 0}};
        int[][] pieceboard16_17 = new int[][] {{0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 1, -1, 0, 0, 0, 0, 6}, {-6, 0, 0, 0, 0, 1, -1, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}};
        //#endregion

        // Test 0: White king check evasion moves
        // Test 1: Black king check evasion moves
        // Test 2: White king checked by knight, white knight could capture but is pinned by bishop, pawn can capture, king moves
        // Test 3: Black king checked by bishop, pawn move to block, rook capture to block, king moves
        // Test 4: White Pawn move & attack evasion, Pawn move evasion blocked by discovered check
        // Test 5: Test 4 for Black
        // Test 6/7: White + Black blocked pawn capture evasion test
        // Next: Iterate through white/black pieces check for pin blocked moves and actual moves
        // Test 8/9: White/black pinned bishop capture evasion, king capture prevention due to vision
        // Test 10/11 White/black checkmate test with pinned pieces
        // Test 12/13 White/black pinned queen, rook, knight check block, only king to move 1 square
        // Test 14/15 white/black double check with available captures (not resulting in new discovered check), only king can move
        // Test 16/17 White/black enpassent check evasion (Double pawn move check, EP evasion)

        //#region answer list creation
        ArrayList<int[]> test0ans = new ArrayList<>();
        for (int ind : bitIndices0){
            test0ans.add(new int[] {6, 20, ind});
        }

        ArrayList<int[]> test1ans = new ArrayList<>();
        for (int ind : bitIndices1){
            test1ans.add(new int[]{-6, 51, ind});
        }

        ArrayList<int[]> test2ans = new ArrayList<>();
        test2ans.add(new int[]{6, 13, 12});
        test2ans.add(new int[]{6, 13, 14});
        test2ans.add(new int[]{6, 13, 21});
        test2ans.add(new int[]{6, 13, 22});
        test2ans.add(new int[]{1, 23, 30});

        ArrayList<int[]> test3ans = new ArrayList<>();
        test3ans.add(new int[]{-6, 52, 44});
        test3ans.add(new int[]{-6, 52, 61});
        test3ans.add(new int[]{-1, 53, 45});
        test3ans.add(new int[]{-4, 24, 31});

        ArrayList<int[]> test4ans = new ArrayList<>();;
        test4ans.add(new int[]{6, 11, 3});
        test4ans.add(new int[]{6, 11, 4});
        test4ans.add(new int[]{6, 11, 10});
        test4ans.add(new int[]{6, 11, 18});
        test4ans.add(new int[]{6, 11, 19});
        test4ans.add(new int[]{1, 21, 29});
        test4ans.add(new int[]{1, 31, 38});

        ArrayList<int[]> test5ans = new ArrayList<>();
        test5ans.add(new int[]{-6, 51, 59});
        test5ans.add(new int[]{-6, 51, 60});
        test5ans.add(new int[]{-6, 51, 50});
        test5ans.add(new int[]{-6, 51, 42});
        test5ans.add(new int[]{-6, 51, 43});
        test5ans.add(new int[]{-1, 45, 37});
        test5ans.add(new int[]{-1, 39, 30});

        ArrayList<int[]> test6ans = new ArrayList<>();
        test6ans.add(new int[]{6, 6, 5});
        test6ans.add(new int[]{6, 6, 7});
        test6ans.add(new int[]{6, 6, 15});

        ArrayList<int[]> test7ans = new ArrayList<>();
        test7ans.add(new int[]{-6, 57, 56});
        test7ans.add(new int[]{-6, 57, 48});
        test7ans.add(new int[]{-6, 57, 58});

        ArrayList<int[]> test8ans = new ArrayList<>();
        test8ans.add(new int[]{6, 15, 6});

        ArrayList<int[]> test9ans = new ArrayList<>();
        test9ans.add(new int[]{-6, 48, 57});

        ArrayList<int[]> test10ans = new ArrayList<>();
        ArrayList<int[]> test11ans = new ArrayList<>();

        ArrayList<int[]> test12ans = new ArrayList<>();
        test12ans.add(new int[]{6, 2, 1});

        ArrayList<int[]> test13ans = new ArrayList<>();
        test13ans.add(new int[]{-6, 61, 62});

        ArrayList<int[]> test14ans = new ArrayList<>();
        test14ans.add(new int[]{6, 4, 3});
        test14ans.add(new int[]{6, 4, 5});
        test14ans.add(new int[]{6, 4, 13});

        ArrayList<int[]> test15ans = new ArrayList<>();
        test15ans.add(new int[]{-6, 59, 50});
        test15ans.add(new int[]{-6, 59, 58});
        test15ans.add(new int[]{-6, 59, 60});

        ArrayList<int[]> test16ans = new ArrayList<>();
        test16ans.add(new int[]{6, 31, 22});
        test16ans.add(new int[]{6, 31, 23});
        test16ans.add(new int[]{6, 31, 30});
        test16ans.add(new int[]{6, 31, 38});
        test16ans.add(new int[]{6, 31, 39});
        test16ans.add(new int[]{1, 37, 46});

        ArrayList<int[]> test17ans = new ArrayList<>();
        test17ans.add(new int[]{-6, 32, 24});
        test17ans.add(new int[]{-6, 32, 25});
        test17ans.add(new int[]{-6, 32, 33});
        test17ans.add(new int[]{-6, 32, 40});
        test17ans.add(new int[]{-6, 32, 41});
        test17ans.add(new int[]{-1, 26, 17});

        //#endregion

        //#region board initialization
        Board board0 = new Board();
        board0.setBoard(pieceboard0);
        board0.setOcc(Board.boardToBitboard(pieceboard0));

        Board board1 = new Board();
        board1.setBoard(pieceboard1);
        board1.setOcc(Board.boardToBitboard(pieceboard1));

        Board board2 = new Board();
        board2.setBoard(pieceboard2);
        board2.setOcc(Board.boardToBitboard(pieceboard2));

        Board board3 = new Board();
        board3.setBoard(pieceboard3);
        board3.setOcc(Board.boardToBitboard(pieceboard3));

        Board board4 = new Board();
        board4.setBoard(pieceboard4);
        board4.setOcc(Board.boardToBitboard(pieceboard4));

        Board board5 = new Board();
        board5.setBoard(pieceboard5);
        board5.setOcc(Board.boardToBitboard(pieceboard5));

        Board board6_7 = new Board();
        board6_7.setBoard(pieceboard6_7);
        board6_7.setOcc(Board.boardToBitboard(pieceboard6_7));

        Board board8_9 = new Board();
        board8_9.setBoard(pieceboard8_9);
        board8_9.setOcc(Board.boardToBitboard(pieceboard8_9));

        Board board10_11 = new Board();
        board10_11.setBoard(pieceboard10_11);
        board10_11.setOcc(Board.boardToBitboard(pieceboard10_11));

        Board board12_13 = new Board();
        board12_13.setBoard(pieceboard12_13);
        board12_13.setOcc(Board.boardToBitboard(pieceboard12_13));

        Board board14_15 = new Board();
        board14_15.setBoard(pieceboard14_15);
        board14_15.setOcc(Board.boardToBitboard(pieceboard14_15));

        Board board16 = new Board();
        board16.setBoard(pieceboard16_17);
        board16.setOcc(Board.boardToBitboard(pieceboard16_17));
        ArrayDeque<Move> temp = new ArrayDeque<>();
        temp.add(new Move(-1, 54, 38, MOVE_TYPE.MOVE));
        board16.setMoveQueue(temp);

        Board board17 = new Board();
        board17.setBoard(pieceboard16_17);
        board17.setOcc(Board.boardToBitboard(pieceboard16_17));
        temp = new ArrayDeque<>();
        temp.add(new Move(1, 9, 25, MOVE_TYPE.MOVE));
        board17.setMoveQueue(temp);
        
        //#endregion
        int[][][] pieceboards = new int[][][]{
            pieceboard0,
            pieceboard1,
            pieceboard2,
            pieceboard3,
            pieceboard4,
            pieceboard5, 
            pieceboard6_7, 
            pieceboard8_9, 
            pieceboard10_11,
            pieceboard12_13,
            pieceboard14_15,
            pieceboard16_17
        };
        if (verbose > 0){
            int ind = 0;
            for (int[][] board : pieceboards){
                System.out.println("Board: " + ind);
                Board.boardVisualize(board);
                ind++;
            }
        }
        ArrayList<int[]> results0 = board0.generateCheckEvasionMoves(1);
        ArrayList<int[]> results1 = board1.generateCheckEvasionMoves(-1);
        ArrayList<int[]> results2 = board2.generateCheckEvasionMoves(1);
        ArrayList<int[]> results3 = board3.generateCheckEvasionMoves(-1);
        ArrayList<int[]> results4 = board4.generateCheckEvasionMoves(1);
        ArrayList<int[]> results5 = board5.generateCheckEvasionMoves(-1);
        ArrayList<int[]> results6 = board6_7.generateCheckEvasionMoves(1);
        ArrayList<int[]> results7 = board6_7.generateCheckEvasionMoves(-1);
        ArrayList<int[]> results8 = board8_9.generateCheckEvasionMoves(1);
        ArrayList<int[]> results9 = board8_9.generateCheckEvasionMoves(-1);
        ArrayList<int[]> results10 = board10_11.generateCheckEvasionMoves(1);
        ArrayList<int[]> results11 = board10_11.generateCheckEvasionMoves(-1);
        ArrayList<int[]> results12 = board12_13.generateCheckEvasionMoves(1);
        ArrayList<int[]> results13 = board12_13.generateCheckEvasionMoves(-1);
        ArrayList<int[]> results14 = board14_15.generateCheckEvasionMoves(1);
        ArrayList<int[]> results15 = board14_15.generateCheckEvasionMoves(-1);
        ArrayList<int[]> results16 = board16.generateCheckEvasionMoves(1);
        ArrayList<int[]> results17 = board17.generateCheckEvasionMoves(-1);

        // White/Black check escape with king moves, pawn capture evasion, pawn move evasion, and a blocked pawn attack/move evasion

        // System.out.println("ANSWERS 1:");
        // for (int[] intArray : test1ans){
        //     System.out.println(Arrays.toString(intArray));
        // }
        // System.out.println("RESULTS 1:");
        // for (int[] intArray : results1){
        //     System.out.println(Arrays.toString(intArray));
        // }

        if (checkMovesEquality(0, test0ans, results0) &&
            checkMovesEquality(1, test1ans, results1) &&
            checkMovesEquality(2, test2ans, results2) &&
            checkMovesEquality(3, test3ans, results3) &&
            checkMovesEquality(4, test4ans, results4) &&
            checkMovesEquality(5, test5ans, results5) &&
            checkMovesEquality(6, test6ans, results6) &&
            checkMovesEquality(7, test7ans, results7) &&
            checkMovesEquality(8, test8ans, results8) &&
            checkMovesEquality(9, test9ans, results9) &&
            checkMovesEquality(10, test10ans, results10) &&
            checkMovesEquality(11, test11ans, results11) &&
            checkMovesEquality(12, test12ans, results12) &&
            checkMovesEquality(13, test13ans, results13) &&
            checkMovesEquality(14, test14ans, results14) &&
            checkMovesEquality(15, test15ans, results15) &&
            checkMovesEquality(16, test16ans, results16) &&
            checkMovesEquality(17, test17ans, results17)
            ){
                

                System.out.println("All tests succeeded");
                
            }
        else {
            System.out.println("One or more tests failed");
        };

    }

    public static boolean checkMovesEquality(int test, ArrayList<int[]> answers, ArrayList<int[]> results){
        int correct = 0;
        if (answers.size() == results.size()){
            for (int[] ans : answers){
                for (int[] res : results){
                    if (Arrays.equals(ans, res)){
                        correct++;
                    };
                }
            }
            if (correct != answers.size()){
                System.out.println("Test " + test + " failed on result contents");

                System.out.println("ANSWERS " + test + ":");
                for (int[] intArray : answers){
                    System.out.println(Arrays.toString(intArray));
                }
                System.out.println("RESULTS " + test + ":");
                for (int[] intArray : results){
                    System.out.println(Arrays.toString(intArray));
                }

                return false;
            }
        }
        else {
            System.out.println("Test " + test + " failed on result size difference: " + answers.size() + " test answers, " + results.size() + " results.");
            
            System.out.println("ANSWERS " + test + ":");
            for (int[] intArray : answers){
                System.out.println(Arrays.toString(intArray));
            }
            System.out.println("RESULTS " + test + ":");
            for (int[] intArray : results){
                System.out.println(Arrays.toString(intArray));
            }
            
            return false;
        }
        return true;
    }

    public static void generateEnPassentMaskTest(){
        // Single pieceboard for all EP tests
        int[][] pieceboard0 = new int[][] {{0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {-1, 1, 1, -1, 1, 0, 1, -1}, {1, -1, -1, 1, -1, 0, -1, 1}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}};
        
        // White left border EP
        long bitboard0 = 0b0000000000000000000000000000000000000000010000000000000000000000L;
        int[] bitIndices0 = new int[] {41};
        // White middle Left capture EP
        long bitboard1 = 0b0000000000000000000000000000000000000000001000000000000000000000L;
        int[] bitIndices1 = new int[] {42};
        // White middle right capture EP
        long bitboard2 = 0b0000000000000000000000000000000000000000000010000000000000000000L;
        int[] bitIndices2 = new int[] {44};
        // White right border EP
        long bitboard3 = 0b0000000000000000000000000000000000000000000000100000000000000000L;
        int[] bitIndices3 = new int[] {46};
        // Black left border EP
        long bitboard4 = 0b0000000000000000010000000000000000000000000000000000000000000000L;
        int[] bitIndices4 = new int[] {17};
        // Black middle left capture EP
        long bitboard5 = 0b0000000000000000001000000000000000000000000000000000000000000000L;
        int[] bitIndices5 = new int[] {18};
        // Black middle right capture EP
        long bitboard6 = 0b0000000000000000000010000000000000000000000000000000000000000000L;
        int[] bitIndices6 = new int[] {20};
        // Black right border EP
        long bitboard7 = 0b0000000000000000000000100000000000000000000000000000000000000000L;
        int[] bitIndices7 = new int[] {22};
        
        int[] friendlyPawnSquares = new int[]{32, 35, 35, 39, 24, 27, 27, 31};
        
        long[] answers = new long[]{
            bitboard0, bitboard1, bitboard2, bitboard3,
            bitboard4, bitboard5, bitboard6, bitboard7
        };
        // Origin and dest indices refer to the opponent pawn that JUST moved
        int[] destIndices = new int[] {33, 34, 36, 38, 25, 26, 28, 30};
        
        int[] originIndices = new int[destIndices.length];

        for (int i = 0; i < destIndices.length; i++){
            int shift = (i <= 3) ? 16 : -16;
            originIndices[i] = destIndices[i] + shift;
        }

        Board[] boards = new Board[]{
            new Board(),
            new Board(),
            new Board(),
            new Board(),
            new Board(),
            new Board(),
            new Board(),
            new Board(),
        };

        Move[] moves = new Move[8];

        for (int i = 0; i < destIndices.length; i++){
            int pawn = (i <= 3) ? -1 : 1;
            moves[i] = new Move(pawn, originIndices[i], destIndices[i], MOVE_TYPE.MOVE);
        }

        // Create boards with corresponding board states
        for (int i = 0; i < destIndices.length; i++){
            boards[i].setBoard(pieceboard0);
            boards[i].setOcc(Board.boardToBitboard(pieceboard0));
            ArrayDeque<Move> temp = new ArrayDeque<>();
            temp.add(moves[i]);
            boards[i].setMoveQueue(temp);
        }

        long[] results = new long[destIndices.length];

        for (int i = 0; i < destIndices.length; i++){
            int playerSign = (i <= 3) ? 1 : -1;
            results[i] = boards[i].generateEnPassentMask(playerSign, friendlyPawnSquares[i]);
        }

        // Check move equality
        boolean success = true;
        int i = 0;
        while (success && (i < answers.length)){
            success = answers[i] == results[i];
            i++;
        }
        

        if (!success){
            System.out.println("Test " + (i - 1) + " failed: ");
            System.out.println("Answers: ");
            System.out.println(answers[i-1]);
            System.out.println("Results: ");
            System.out.println(results[i-1]);
        }
        else {
            System.out.println("Pawn EP generation test succeeded!");
        }

        // Test EP failure due to empty move deque
        for (Board b : boards){
            b.setMoveQueue(new ArrayDeque<Move>());
        }

        long[] emptyAnswers = new long[]{0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L};
        long[] emptyResults = new long[boards.length];

        for (int j = 0; j < boards.length; j++){
            int playerSign = (i < 3) ? 1 : -1;
            emptyResults[j] = boards[j].generateEnPassentMask(playerSign, friendlyPawnSquares[j]);
        }

        success = true;
        int k = 0;
        while (success && (k < emptyAnswers.length)){
            success = emptyAnswers[k] == emptyResults[k];
            k++;
        }

        if (!success){
            System.out.println("EP failure test " + k + " failed: ");
            System.out.println("Answers: ");
            System.out.println(emptyAnswers[k]);
            System.out.println("Results: ");
            System.out.println(emptyResults[k]);
        }
        else {
            System.out.println("EP failure generation test succeeded!");
        }



    }

    public static void generateValidMovesTest(){
        // Test 0/1 white/black valid moves on a starting board state
        // Test 2/3 white/black valid moves including castling
        
        //#region Test0/1
        // Default piece initialization
        Board board0_1 = new Board();

        ArrayList<int[]> answers0 = new ArrayList<>();
        // White pawn moves
        answers0.add(new int[]{1, 8, 16});
        answers0.add(new int[]{1, 8, 24});
        answers0.add(new int[]{1, 9, 17});
        answers0.add(new int[]{1, 9, 25});
        answers0.add(new int[]{1, 10, 18});
        answers0.add(new int[]{1, 10, 26});
        answers0.add(new int[]{1, 11, 19});
        answers0.add(new int[]{1, 11, 27});
        answers0.add(new int[]{1, 12, 20});
        answers0.add(new int[]{1, 12, 28});
        answers0.add(new int[]{1, 13, 21});
        answers0.add(new int[]{1, 13, 29});
        answers0.add(new int[]{1, 14, 22});
        answers0.add(new int[]{1, 14, 30});
        answers0.add(new int[]{1, 15, 23});
        answers0.add(new int[]{1, 15, 31});

        // White knight moves
        answers0.add(new int[]{2, 1, 16});
        answers0.add(new int[]{2, 1, 18});
        answers0.add(new int[]{2, 6, 21});
        answers0.add(new int[]{2, 6, 23});
        
        ArrayList<int[]> answers1 = new ArrayList<>();
        // Black pawn moves
        answers1.add(new int[]{-1, 48, 40});
        answers1.add(new int[]{-1, 48, 32});
        answers1.add(new int[]{-1, 49, 41});
        answers1.add(new int[]{-1, 49, 33});
        answers1.add(new int[]{-1, 50, 42});
        answers1.add(new int[]{-1, 50, 34});
        answers1.add(new int[]{-1, 51, 43});
        answers1.add(new int[]{-1, 51, 35});
        answers1.add(new int[]{-1, 52, 44});
        answers1.add(new int[]{-1, 52, 36});
        answers1.add(new int[]{-1, 53, 45});
        answers1.add(new int[]{-1, 53, 37});
        answers1.add(new int[]{-1, 54, 46});
        answers1.add(new int[]{-1, 54, 38});
        answers1.add(new int[]{-1, 55, 47});
        answers1.add(new int[]{-1, 55, 39});
        // Black knight moves
        answers1.add(new int[]{-2, 57, 40});
        answers1.add(new int[]{-2, 57, 42});
        answers1.add(new int[]{-2, 62, 45});
        answers1.add(new int[]{-2, 62, 47});

        ArrayList<int[]> results0 = board0_1.generateValidMoves(1);
        ArrayList<int[]> results1 = board0_1.generateValidMoves(-1);

        //#endregion

        //#region Test2/3
        Board board2_3 = new Board();
        int[][] pieceboard2_3 = new int[][] {{4, 2, 3, 5, 6, 0, 0, 4}, {1, 1, 1, 0, 0, 1, 1, 1}, {0, 0, 0, 1, 0, 2, 0, 0}, {0, 0, 0, 0, 1, 0, 0, 0}, {0, 3, -3, -1, -1, 0, 0, 0}, {0, 0, -2, 0, 0, -3, 0, 0}, {-1, -1, -1, 0, -2, -1, -1, -1}, {-4, 0, 0, -5, -6, 0, 0, -4}};
        board2_3.setBoard(pieceboard2_3);
        board2_3.setOcc(Board.boardToBitboard(pieceboard2_3));

        // Test2 answers
        ArrayList<int[]> answers2 = new ArrayList<>();
        answers2.add(new int[]{1, 8, 16});
        answers2.add(new int[]{1, 8, 24});
        answers2.add(new int[]{1, 9, 17});
        answers2.add(new int[]{1, 9, 25});
        answers2.add(new int[]{1, 10, 18});
        answers2.add(new int[]{1, 10, 26});
        answers2.add(new int[]{1, 19, 27});
        answers2.add(new int[]{1, 28, 35});
        answers2.add(new int[]{1, 14, 22});
        answers2.add(new int[]{1, 14, 30});
        answers2.add(new int[]{1, 15, 23});
        answers2.add(new int[]{1, 15, 31});
        answers2.add(new int[]{3, 33, 42});
        answers2.add(new int[]{3, 33, 24});
        answers2.add(new int[]{3, 33, 26});
        answers2.add(new int[]{3, 33, 40});
        answers2.add(new int[]{5, 3, 11});
        answers2.add(new int[]{5, 3, 12});
        answers2.add(new int[]{6, 4, 12});
        answers2.add(new int[]{6, 4, 11});
        answers2.add(new int[]{6, 4, 5});
        answers2.add(new int[]{6, 4, 6}); // White king short castle
        answers2.add(new int[]{2, 21, 6});
        answers2.add(new int[]{2, 21, 11});
        answers2.add(new int[]{2, 21, 27});
        answers2.add(new int[]{2, 21, 36});
        answers2.add(new int[]{2, 21, 38});
        answers2.add(new int[]{2, 21, 31});
        answers2.add(new int[]{2, 1, 16});
        answers2.add(new int[]{2, 1, 18});
        answers2.add(new int[]{2, 1, 11});
        answers2.add(new int[]{3, 2, 11});
        answers2.add(new int[]{3, 2, 20});
        answers2.add(new int[]{3, 2, 29});
        answers2.add(new int[]{3, 2, 38});
        answers2.add(new int[]{3, 2, 47});
        answers2.add(new int[]{4, 7, 6});
        answers2.add(new int[]{4, 7, 5});

        ArrayList<int[]> answers3 = new ArrayList<>();
        //Rank 8
        answers3.add(new int[]{-4, 56, 57});
        answers3.add(new int[]{-4, 56, 58});
        answers3.add(new int[]{-5, 59, 57});
        answers3.add(new int[]{-5, 59, 58});
        answers3.add(new int[]{-5, 59, 51});
        answers3.add(new int[]{-5, 59, 43});
        answers3.add(new int[]{-6, 60, 51});
        answers3.add(new int[]{-6, 60, 61});
        answers3.add(new int[]{-6, 60, 62});
        answers3.add(new int[]{-4, 63, 62});
        answers3.add(new int[]{-4, 63, 61});
        // Rank 7
        answers3.add(new int[]{-1, 48, 40});
        answers3.add(new int[]{-1, 48, 32});
        answers3.add(new int[]{-1, 49, 41});
        answers3.add(new int[]{-2, 52, 58});
        answers3.add(new int[]{-2, 52, 37});
        answers3.add(new int[]{-2, 52, 46});
        answers3.add(new int[]{-2, 52, 62});
        answers3.add(new int[]{-1, 54, 46});
        answers3.add(new int[]{-1, 54, 38});
        answers3.add(new int[]{-1, 55, 47});
        answers3.add(new int[]{-1, 55, 39});
        // Rank 6
        // Knight on 42 is pinned
        answers3.add(new int[]{-3, 45, 38});
        answers3.add(new int[]{-3, 45, 31});
        //Rank 5
        answers3.add(new int[]{-3, 34, 41});
        answers3.add(new int[]{-3, 34, 43});
        answers3.add(new int[]{-3, 34, 25});
        answers3.add(new int[]{-3, 34, 16});
        answers3.add(new int[]{-3, 34, 27});
        answers3.add(new int[]{-3, 34, 20});
        answers3.add(new int[]{-3, 34, 13});
        answers3.add(new int[]{-1, 35, 27});
        answers3.add(new int[]{-1, 35, 28});

        ArrayList<int[]> results2 = board2_3.generateValidMoves(1);
        ArrayList<int[]> results3 = board2_3.generateValidMoves(-1);
        
        if (checkMovesEquality(0, answers0, results0) 
            && checkMovesEquality(1, answers1, results1)
            && checkMovesEquality(2, answers2, results2)
            && checkMovesEquality(3, answers3, results3)
            )
        {
            System.out.println("generateValidMovesTest() Passed all tests!");
        } 
        else {
            System.out.println("generateValidMovesTest() failed one or more tests.");
        }
        
    }

    public static void generateValidCastlingMovesTest(){
        int[] wCastleLong = new int[]{6, 4, 2};
        int[] wCastleShort = new int[]{6, 4, 6};
        int[] bCastleLong = new int[] {-6, 60, 58};
        int[] bCastleShort = new int[] {-6, 60, 62};

        int[][] pieceboard0_1 = new int[][] {{4, 0, 0, 0, 6, 0, 0, 4}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {-4, 0, 0, 0, -6, 0, 0, -4}};
        int[][] pieceboard2_3 = new int[][] {{0, 0, 0, 0, 6, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, -6, 0, 0, 0}};
        int[][] pieceboard4_5 = new int[][] {{4, 0, 0, 6, 0, 0, 0, 4}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {-4, 0, 0, -6, 0, 0, 0, -4}};
        int[][] pieceboard6_7 = new int[][] {{4, 0, 0, 0, 6, 0, 0, 4}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, -4, 0, 0, 0, 0, 0}, {0, 0, 4, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {-4, 0, 0, 0, -6, 0, 0, -4}};
        int[][] pieceboard8_9 = new int[][] {{4, 0, 0, 0, 6, 0, 0, 4}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, -4, 0}, {0, 0, 0, 0, 0, 0, 4, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {-4, 0, 0, 0, -6, 0, 0, -4}};
        int[][] pieceboard10_11 = new int[][] {{4, 0, 2, 0, 6, 0, 0, 4}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {-4, 0, -3, 0, -6, 0, 0, -4}};
        int[][] pieceboard12_13 = new int[][] {{4, 0, 0, 0, 6, 0, 3, 4}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {-4, 0, 0, 0, -6, 0, -2, -4}};
        int[][] pieceboard14_15 = new int[][] {{4, 0, 0, 0, 6, 3, 2, 4}, {1, 0, 1, 0, 0, 1, 1, 1}, {3, 1, 2, 5, 1, 0, 0, 0}, {0, 0, 0, 1, 0, 0, 0, 0}, {0, 0, 0, -1, -1, -3, 0, 0}, {0, 0, 0, 0, 0, -2, 0, 0}, {-1, -1, -1, -5, 0, -1, -1, -1}, {-4, -2, 0, 0, -6, 0, 0, -4}};
        int[][] pieceboard16_17 = new int[][] {{4, 3, 0, 0, 6, 0, 0, 4}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {-4, -3, 0, 0, -6, 0, 0, -4}};

        //Test0/1 plain white/black long/short castling with no blockers
        // Test2/3 White/black no rooks at all
        // Test4/5 White/black kings moved, rooks in proper positions
        // Test 6/7 White/black long castle blocked by opponent vision
        // Test 8/9 white/black short castle blocked by opponent vision
        // Test 10/11 White/black long castle blocked by friendly occupancy
        // Test 12/13 White/black short castle blocked by friendly occupancy
        // Test 14/15 White/black castling on a fuller more realistic board
        // Test 16/17 White/black explicit testing of occupied long-castle square not travelled by king (B1 for white, B8 for black)

        // Create boards for each test
        Board board0_1 = new Board();
        board0_1.setBoard(pieceboard0_1);
        board0_1.setOcc(Board.boardToBitboard(pieceboard0_1));

        Board board2_3 = new Board();
        board2_3.setBoard(pieceboard2_3);
        board2_3.setOcc(Board.boardToBitboard(pieceboard2_3));

        Board board4_5 = new Board();
        board4_5.setBoard(pieceboard4_5);
        board4_5.setOcc(Board.boardToBitboard(pieceboard4_5));

        Board board6_7 = new Board();
        board6_7.setBoard(pieceboard6_7);
        board6_7.setOcc(Board.boardToBitboard(pieceboard6_7));

        Board board8_9 = new Board();
        board8_9.setBoard(pieceboard8_9);
        board8_9.setOcc(Board.boardToBitboard(pieceboard8_9));

        Board board10_11 = new Board();
        board10_11.setBoard(pieceboard10_11);
        board10_11.setOcc(Board.boardToBitboard(pieceboard10_11));

        Board board12_13 = new Board();
        board12_13.setBoard(pieceboard12_13);
        board12_13.setOcc(Board.boardToBitboard(pieceboard12_13));

        Board board14_15 = new Board();
        board14_15.setBoard(pieceboard14_15);
        board14_15.setOcc(Board.boardToBitboard(pieceboard14_15));

        Board board16_17 = new Board();
        board16_17.setBoard(pieceboard16_17);
        board16_17.setOcc(Board.boardToBitboard(pieceboard16_17));

        // Create answers for each test
        ArrayList<int[]> answers0 = new ArrayList<>();
        answers0.add(wCastleLong); 
        answers0.add(wCastleShort);
        ArrayList<int[]> answers1 = new ArrayList<>();
        answers1.add(bCastleLong); 
        answers1.add(bCastleShort);

        ArrayList<int[]> answers2 = new ArrayList<>();
        ArrayList<int[]> answers3 = new ArrayList<>();

        ArrayList<int[]> answers4 = new ArrayList<>();
        ArrayList<int[]> answers5 = new ArrayList<>();

        ArrayList<int[]> answers6 = new ArrayList<>();
        answers6.add(wCastleShort);
        ArrayList<int[]> answers7 = new ArrayList<>();
        answers7.add(bCastleShort);

        ArrayList<int[]> answers8 = new ArrayList<>();
        answers8.add(wCastleLong);
        ArrayList<int[]> answers9 = new ArrayList<>();
        answers9.add(bCastleLong);

        ArrayList<int[]> answers10 = new ArrayList<>();
        answers10.add(wCastleShort);
        ArrayList<int[]> answers11 = new ArrayList<>();
        answers11.add(bCastleShort);

        ArrayList<int[]> answers12 = new ArrayList<>();
        answers12.add(wCastleLong);
        ArrayList<int[]> answers13 = new ArrayList<>();
        answers13.add(bCastleLong);

        ArrayList<int[]> answers14 = new ArrayList<>();
        answers14.add(wCastleLong);
        ArrayList<int[]> answers15 = new ArrayList<>();

        ArrayList<int[]> answers16 = new ArrayList<>();
        answers16.add(wCastleShort);
        ArrayList<int[]> answers17 = new ArrayList<>();
        answers17.add(bCastleShort);

        // Get the results for the test
        ArrayList<int[]> results0 = board0_1.generateValidCastlingMoves(1);
        ArrayList<int[]> results1 = board0_1.generateValidCastlingMoves(-1);
        ArrayList<int[]> results2 = board2_3.generateValidCastlingMoves(1);
        ArrayList<int[]> results3 = board2_3.generateValidCastlingMoves(-1);
        ArrayList<int[]> results4 = board4_5.generateValidCastlingMoves(1);
        ArrayList<int[]> results5 = board4_5.generateValidCastlingMoves(-1);
        ArrayList<int[]> results6 = board6_7.generateValidCastlingMoves(1);
        ArrayList<int[]> results7 = board6_7.generateValidCastlingMoves(-1);
        ArrayList<int[]> results8 = board8_9.generateValidCastlingMoves(1);
        ArrayList<int[]> results9 = board8_9.generateValidCastlingMoves(-1);
        ArrayList<int[]> results10 = board10_11.generateValidCastlingMoves(1);
        ArrayList<int[]> results11 = board10_11.generateValidCastlingMoves(-1);
        ArrayList<int[]> results12 = board12_13.generateValidCastlingMoves(1);
        ArrayList<int[]> results13 = board12_13.generateValidCastlingMoves(-1);
        ArrayList<int[]> results14 = board14_15.generateValidCastlingMoves(1);
        ArrayList<int[]> results15 = board14_15.generateValidCastlingMoves(-1);
        ArrayList<int[]> results16 = board16_17.generateValidCastlingMoves(1);
        ArrayList<int[]> results17 = board16_17.generateValidCastlingMoves(-1);

        if (
            checkMovesEquality(0, answers0, results0)
            && checkMovesEquality(1, answers1, results1) 
            && checkMovesEquality(2, answers2, results2) 
            && checkMovesEquality(3, answers3, results3)
            && checkMovesEquality(4, answers4, results4)
            && checkMovesEquality(5, answers5, results5)
            && checkMovesEquality(6, answers6, results6)
            && checkMovesEquality(7, answers7, results7)
            && checkMovesEquality(8, answers8, results8)
            && checkMovesEquality(9, answers9, results9)
            && checkMovesEquality(10, answers10, results10)
            && checkMovesEquality(11, answers11, results11)
            && checkMovesEquality(12, answers12, results12)
            && checkMovesEquality(13, answers13, results13)
            && checkMovesEquality(14, answers14, results14)
            && checkMovesEquality(15, answers15, results15)
            && checkMovesEquality(16, answers16, results16)
            && checkMovesEquality(17, answers17, results17)
        ){
            System.out.println("generateValidCastlingMovesTest() passed all tests!");
        }
        else {
            System.out.println("generateValidCastlingMovesTest() failed one or more tests");
        }

    }

    public static void createMoveTest(){
        // Test for White and Black: Move, Attack, CastleLong, CastleShort, EnPassent, PromoteMove, PromoteAttack

        ArrayList<int[]> answers0 = new ArrayList<>();
        ArrayList<int[]> answers1 = new ArrayList<>();
        ArrayList<MOVE_TYPE> types0 = new ArrayList<>();
        answers0.add(new int[]{6, 4, 6});
        answers0.add(new int[]{6, 4, 2});
        answers1.add(new int[]{-6, 60, 62});
        answers1.add(new int[]{-6, 60, 58});
        types0.add(MOVE_TYPE.CASTLE_SHORT);
        types0.add(MOVE_TYPE.CASTLE_LONG);

        answers0.add(new int[]{1, 54, 63});
        answers0.add(new int[]{1, 54, 62});
        answers1.add(new int[]{-1, 14, 7});
        answers1.add(new int[]{-1, 14, 6});       
        types0.add(MOVE_TYPE.PROMOTE_ATTACK);
        types0.add(MOVE_TYPE.PROMOTE_MOVE);

        answers0.add(new int[]{1, 38, 47});
        answers1.add(new int[]{-1, 30, 21});
        types0.add(MOVE_TYPE.EN_PASSENT);

        answers0.add(new int[]{2, 28, 34});
        answers0.add(new int[]{2, 28, 18});
        answers1.add(new int[]{-2, 34, 28});
        answers1.add(new int[]{-2, 34, 24});
        types0.add(MOVE_TYPE.ATTACK);
        types0.add(MOVE_TYPE.MOVE);

        // Generate results
        int[][] pieceboard0_1 = new int[][] {{4, 0, 0, 0, 6, 0, 0, 4}, {0, 0, 0, 0, 0, 0, -1, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 2, 0, -1, 0}, {0, 0, -2, 0, 0, 0, 1, 0}, {0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 1, 0}, {-4, 0, 0, 0, -6, 0, 0, -4}};
        Board board0_1 = new Board();
        board0_1.setBoard(pieceboard0_1);
        board0_1.setOcc(Board.boardToBitboard(pieceboard0_1));

        ArrayList<Move> results0 = new ArrayList<>();
        ArrayList<Move> results1 = new ArrayList<>();

        for (int i = 0; i < answers0.size(); i++){
            results0.add(board0_1.createMove(answers0.get(i)));
            results1.add(board0_1.createMove(answers1.get(i)));
        }

        //#region Test evaluation
        for (int i = 0; i < results0.size(); i++){
            Move currentMove = results0.get(i);
            int[] currentAnswer = answers0.get(i);
            MOVE_TYPE currentType = types0.get(i);
            
            if (!(
                (currentMove.getPiece() == currentAnswer[0]) &&
                (currentMove.getOriginBit() == currentAnswer[1]) &&
                (currentMove.getDestBit() == currentAnswer[2]) &&
                (currentMove.getType() == currentType))
            ){
                System.out.println("createMoveTest failed on test 0:");
                System.out.println(currentMove);
                System.out.println(Arrays.toString(currentAnswer));
                System.out.println(currentType);
                return;
            }
        }

        for (int i = 0; i < results1.size(); i++){
            Move currentMove = results1.get(i);
            int[] currentAnswer = answers1.get(i);
            MOVE_TYPE currentType = types0.get(i);
            
            if (!(
                (currentMove.getPiece() == currentAnswer[0]) &&
                (currentMove.getOriginBit() == currentAnswer[1]) &&
                (currentMove.getDestBit() == currentAnswer[2]) &&
                (currentMove.getType() == currentType))
            ){
                System.out.println("createMoveTest failed on test 1:");
                System.out.println(currentMove);
                System.out.println(Arrays.toString(currentAnswer));
                System.out.println(currentType);
                return;
            }

        }
        System.out.println("createMoveTest() passed all tests!");

        //#endregion

    }

    public static void foolsMateTest(){
        Move mv0 = new Move(1, 13, 21, MOVE_TYPE.MOVE);
        Move mv1 = new Move(-1 , 52, 36, MOVE_TYPE.MOVE);
        Move mv2 = new Move(1, 14, 30, MOVE_TYPE.MOVE);
        Move mv3 = new Move(-5, 59, 31, MOVE_TYPE.MOVE);

        Move[] foolsMate = new Move[]{mv0, mv1, mv2, mv3};

        Board board = new Board();
        
        // Play
        // Update state
        // Generate moves
        // Game end conditions (moves)
        // Play
        try(Scanner scanner = new Scanner(System.in)){
            int turn = 1;
            int count = 0;
            String turnString;
            System.out.println("Game started, white to play:");
            System.out.println("Turn " + count);
            ArrayList<int[]> validMoves = board.generateValidMoves(turn);
            System.out.println("White has " + validMoves.size() + " valid moves");
            System.out.println(board);
            scanner.nextLine();
            count++;
            
            for (Move mv : foolsMate){
                board.playMove(mv);
                turn *= -1;
                turnString = (turn > 0) ? "White" : "Black";
                board.updateState(board.peekMove().getPiece(), validMoves);
                validMoves = board.generateValidMoves(turn);
                board.evaluateGameEndConditions(validMoves);
                System.out.println(turnString + "s turn: " + validMoves.size() + " possible moves");
                System.out.println(board);
                scanner.nextLine();
                count++;
            }
        }
        catch (Exception e){
            System.out.println("foolsMateTest(): Error initializing scanner");
        }
        
        
    }

    public static void playTest(Move[] moves, String label, int verbosity){
        System.out.println("Play test for " + label + ":");
        try{
            Scanner scanner = new Scanner(System.in);   
            Board board = new Board();
            int turn = 1;
            ArrayList<int[]> validMoves;
            for (int i = 0; i < moves.length; i++){
                String turnString = (turn > 0) ? "White" : "Black";
                validMoves = board.generateValidMoves(turn);
                System.out.println(i + ". " + turnString + " to move, " + validMoves.size() + " valid moves");
                System.out.println(board);
                if (verbosity >= 1){
                    System.out.println(String.format("Board state: %s\nwhites turn: %b\nwhiteLong:%b\nwhiteShort: %b\nblackLong: %b\nblackShort: %b\nthreeFold: %b\nfiftyMove: %b\nhalfClock: %d\nnumPlayedMoves: %d\nnumZobristHistory: %d\n",
                    board.getState(),
                    board.getWhitesTurn(), 
                    board.getCastlingRights("whiteLong"), 
                    board.getCastlingRights("whiteShort"), 
                    board.getCastlingRights("blackLong"), 
                    board.getCastlingRights("blackShort"), 
                    board.getClaimableDraw("threeFold"), 
                    board.getClaimableDraw("fiftyMove"), 
                    board.getHalfClock(), 
                    board.getPlayedMovesLength(), 
                    board.getZobristHistoryLength()));

                    if (verbosity >= 2){
                        for (int[] mv : validMoves){
                            System.out.println(Arrays.toString(mv));
                        }
                    }
                }
                scanner.nextLine();
                board.playMove(moves[i]);
                board.updateState(board.peekMove().getPiece(), validMoves);
                turn *= -1;
                validMoves = board.generateValidMoves(turn);
                board.evaluateGameEndConditions(validMoves);
            }
            System.out.println(board);
            if (verbosity >= 1){
                System.out.println(String.format("Board state: %s\nwhites turn: %b\nwhiteLong:%b\nwhiteShort: %b\nblackLong: %b\nblackShort%b\nthreeFold: %b\nfiftyMove: %b\nhalfClock: %d\nnumPlayedMoves: %d\nnumZobristHistory: %d\n",
                board.getState(),
                board.getWhitesTurn(), 
                board.getCastlingRights("whiteLong"), 
                board.getCastlingRights("whiteShort"), 
                board.getCastlingRights("blackLong"), 
                board.getCastlingRights("blackShort"), 
                board.getClaimableDraw("threeFold"), 
                board.getClaimableDraw("fiftyMove"), 
                board.getHalfClock(), 
                board.getPlayedMovesLength(), 
                board.getZobristHistoryLength()));
            }
            scanner.nextLine();


        }
        catch (Exception e){
            System.out.println("playTest(): Error initializing scanner: ");
            e.printStackTrace();
        }
        
        System.out.println("Finished play testing " + label);
    }

    public static ArrayList<String[]> parseMoveTest(String input){
        // String input = "\r\n" + //
        //                 "1.d4 Nf6 2.Nf3 g6 3.c4 c5 4.d5 b5 5.cxb5 a6 6.bxa6 Bg7 7.Nc3 Bxa6 8.e4 Bxf1\r\n" + //
        //                 "9.Kxf1 d6 10.g3 O-O 11.Kg2 Nbd7 12.Re1 Qb6 13.Re2 Rfb8 14.Bf4 Ng4 15.Rc1 Nde5\r\n" + //
        //                 "16.Nxe5 Nxe5 17.b3 Qa6 18.Bxe5 Bxe5 19.Nb1 Rb4 20.Rec2 c4 21.bxc4 Rxc4 22.Qd3 Rxc2\r\n" + //
        //                 "23.Qxa6 Rxa6 24.Rxc2 f5 25.f3 Kf7 26.Kf2 h5 27.Ke3 h4 28.gxh4 fxe4 29.fxe4 Bf6\r\n" + //
        //                 "30.Kf4 Bxh4 31.Nd2 Ra4 32.Nc4 Bf6 33.Ke3 Ra8 34.Kd3 Rh8 35.Ke3 Bg5+ 36.Kf3 Ra8\r\n" + //
        //                 "37.a3 Ra4 38.Ke2 Bf4 39.h3 Ra8 40.Rc3 Kf6 41.Rf3 g5 42.Kd3 Ra6 43.Kc2 e6\r\n" + //
        //                 "44.Rd3 exd5 45.Rxd5 Ke6 46.Kb3 Kd7 47.e5 Bxe5 48.Nxe5+ Ke6 49.Nd3 Rxa3+ 50.Kxa3 Kxd5\r\n" +
        //                 "51.Nb4+ Ke4 52.Nc6 Kf4 53.Nd4 Kg3  1/2-1/2";

        Map<Integer, String> GROUP_NAMES = Map.ofEntries(
            Map.entry(0, "Full"),
            Map.entry(1, "Piece"),
            Map.entry(2, "dsRank"),
            Map.entry(3, "dsFile"),
            Map.entry(4, "Capture"),
            Map.entry(5, "destRank"),
            Map.entry(6, "destFile"),
            Map.entry(7, "Promotion"),
            Map.entry(8, "Chk/Mt"),
            Map.entry(9, "ShortCastle"),
            Map.entry(10, "LongCastle")
        );
        
        ArrayList<String[]> retArray = new ArrayList<>();

        // Group 0: Full move
        // Group 1: Piece identifier, if null then pawn
        // Group 2: Optional rank disambig.
        // Group 3: Optional file disambig.
        // Group 4: Optional capture
        // Group 5: Destination rank
        // Group 6: Destination file
        // Group 7: Optional promotion indicator
        // Group 8: Optional Check or Mate
        // Group 9: Short/Kingside castle
        // Group 10: Long/Queenside castle
        String regex = "([KQRBN])?([a-hA-H])?([1-8])?(x)?([a-hA-H])([1-8])(=[KQRBN])?([+#])?|(O-O)|(O-O-O)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        //StringBuilder sb = new StringBuilder();

        while(matcher.find()){
            String[] temp = new String[11];

            for (int i = 0; i < 11; i++){
                temp[i] = matcher.group(i);
            }

            retArray.add(temp);
        }

        return retArray;
    }

    public static ArrayList<Move> moveValidator(String movesString){
        //#region Map initialization
        Map<String, Integer> PIECE_ID = new HashMap<>();
        PIECE_ID.put(null, 1);
        PIECE_ID.put("N", 2);
        PIECE_ID.put("B", 3);
        PIECE_ID.put("R", 4);
        PIECE_ID.put("Q", 5);
        PIECE_ID.put("K", 6);

        Map<String, Integer> FILE_INDEX = new HashMap<>();
        FILE_INDEX.put("a", 0);
        FILE_INDEX.put("b", 1);
        FILE_INDEX.put("c", 2);
        FILE_INDEX.put("d", 3);
        FILE_INDEX.put("e", 4);
        FILE_INDEX.put("f", 5);
        FILE_INDEX.put("g", 6);
        FILE_INDEX.put("h", 7);
        //#endregion

        ArrayList<Move> retArray = new ArrayList<>();

        //#region Regex initialization
        // Group 0: Full move
        // Group 1: Piece identifier, if null then pawn
        // Group 2: Optional rank disambig.
        // Group 3: Optional file disambig.
        // Group 4: Optional capture
        // Group 5: Destination rank
        // Group 6: Destination file
        // Group 7: Optional promotion indicator
        // Group 8: Optional Check or Mate
        // Group 9: Short/Kingside castle
        // Group 10: Long/Queenside castle

        String regex = "([KQRBN])?([a-hA-H])?([1-8])?(x)?([a-hA-H])([1-8])(=[KQRBN])?([+#])?|(O-O-O)|(O-O)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(movesString);
        //#endregion

        // Initial board state
        Board board = new Board();
        ArrayList<int[]> validMoves = board.generateValidMoves(1);

        //#region Move Inference
        int count = 0;
        while(matcher.find()){
            boolean whitesTurn = (count % 2 == 0) ? true : false;
            int turnInt = whitesTurn ? 1 : -1;
            int piece;
            int origin;
            int destination;
            MOVE_TYPE moveType;
            int promotionPiece = 0;
            
            // Short castle
            String debugMatch = matcher.group(0);
            if (matcher.group(0).equals("O-O")){
                if (whitesTurn){
                    piece = 6;
                    origin = 4;
                    destination = 6;
                }
                else {
                    piece = -6;
                    origin = 60;
                    destination = 62;
                }

                int[] temp = new int[]{piece, origin, destination};

                if (moveInMoveset(temp, validMoves)){
                    retArray.add(new Move(piece, origin, destination, MOVE_TYPE.CASTLE_SHORT));
                }
                else {
                    System.out.println("moveValidator(): Invalid move detected for index " + count + ", returning all valid moves until this point.");
                    return retArray;
                }
            }
            // Long castle
            else if (matcher.group(0).equals("O-O-O")){
            
                if (whitesTurn){
                    piece = 6;
                    origin = 4;
                    destination = 2;
                }
                else {
                    piece = -6;
                    origin = 60;
                    destination = 58;
                }

                int[] temp = new int[]{piece, origin, destination};

                if (moveInMoveset(temp, validMoves)){
                    retArray.add(new Move(piece, origin, destination, MOVE_TYPE.CASTLE_LONG));
                }
                else {
                    System.out.println("moveValidator(): Invalid move detected for index " + count + ", returning all valid moves until this point.");
                    return retArray;
                }
            }
            // All other moves
            else{
                //#region PieceID, Destination square, Move Type detection
                // Determine piece ID from string identifier
                // Throws exception if identifier is not found as primitive int cannot be Null
                try{
                    piece = PIECE_ID.get(matcher.group(1));
                }
                catch (NullPointerException e){
                    System.out.println("moveValidator(): Invalid piece identifier detected for index " + count + ", returning validated moves");
                    return retArray;
                }
                // Consider turn for piece
                if (!whitesTurn){
                    piece *= -1;
                }

                // Get destination
                destination = ((Integer.valueOf(matcher.group(6)) - 1) * 8) + FILE_INDEX.get(matcher.group(5)); // Subtract 1 as file index = file lablel - 1

                // Determine move type
                // Promotion?
                if (matcher.group(7) != null){
                    //Capture?
                    if (matcher.group(4) != null){
                        moveType = MOVE_TYPE.PROMOTE_ATTACK;
                    }
                    else {
                        moveType = MOVE_TYPE.PROMOTE_MOVE;
                    }
                    promotionPiece = PIECE_ID.get(String.valueOf(matcher.group(7).charAt(1))) * turnInt;
                }
                // Not promotion, but capture?
                else if (matcher.group(4) != null){
                    // If Pawn capture on an empty square
                    if ((Math.abs(piece) == 1) && (board.getBoard()[destination/8][destination % 8] == 0)){
                        moveType = MOVE_TYPE.EN_PASSENT;
                    }
                    else {
                        moveType = MOVE_TYPE.ATTACK;
                    }
                }
                else {
                    moveType = MOVE_TYPE.MOVE;
                }
                //#endregion

                //#region Move validation
                // No disambig
                if (matcher.group(2) == null && matcher.group(3) == null){
                    int[] temp = getMoveInMoveset(piece, destination, validMoves);

                    if (temp.length > 0){
                        retArray.add(new Move(temp[0],temp[1], temp[2], moveType));
                    }
                    else {
                        System.out.println("moveValidator(): No valid move match found for index " + count);
                        return retArray;
                    }
                }
                // Rank disambig
                else if (matcher.group(3) != null){
                    int[] temp = getMoveInMoveset(piece, destination, validMoves, Integer.valueOf(matcher.group(3)) - 1, -1); // No relationship between the sole -1 flag vs current[2] - 1

                    if (temp.length > 1){
                        retArray.add(new Move(temp[0], temp[1], temp[2], moveType));
                    }
                    else {
                        System.out.println("moveValidator(): No valid move match found for index " + count);
                        return retArray;
                    }
                }
                // File disambig
                else if (matcher.group(2) != null){
                    int[] temp = getMoveInMoveset(piece, destination, validMoves, -1, FILE_INDEX.get(matcher.group(2)));

                    if (temp.length > 0){
                        retArray.add(new Move(temp[0], temp[1], temp[2], moveType));
                    }
                    else {
                        System.out.println("moveValidator(): No valid move match found for index " + count);
                        return retArray;
                    }
                }
                // Rank and File disambig
                else {
                    int [] temp = getMoveInMoveset(piece, destination, validMoves, Integer.valueOf(matcher.group(3)) - 1, Integer.valueOf(matcher.group(2)));
                    
                    if (temp.length > 0){
                        retArray.add(new Move(temp[0], temp[1], temp[2], moveType));
                    }
                    else {
                        System.out.println("moveValidator(): No valid move match found for index " + count);
                        return retArray;
                    }
                }
                //#endregion
            }

            // Add promotion piece if the move is a promotion, promotion piece is not a factor for the move validity
            Move lastMove = retArray.get(retArray.size() - 1);
            if (lastMove.getType() == MOVE_TYPE.PROMOTE_ATTACK || lastMove.getType() == MOVE_TYPE.PROMOTE_MOVE){
                lastMove.setPromotionPiece(promotionPiece);
            }

            //#region Update validator board
            // Play the last move and update state
            board.playMove(lastMove);
            board.updateState(turnInt, validMoves);
            validMoves = board.generateValidMoves(turnInt * -1);

            // if (validMoves.size() == 0){
            //     System.out.println("moveValidator(): No more valid moves left");
            //     return retArray;
            // }
            //#endregion

            count++;
        }
        //#endregion

        System.out.println("moveValidator(): All moves validated!");
        return retArray;
    }

    public static boolean moveInMoveset(int[] move, ArrayList<int[]> moveset){
        for (int[] mv : moveset){
            if (Arrays.equals(move, mv)){
                return true;
            }
        }
        return false;
    }

    public static int[] getMoveInMoveset(int piece, int dest, ArrayList<int[]> moveset){
        int count = 0;
        int[] targetMove = new int[]{};
        for (int [] mv : moveset){
            if (piece == mv[0] && dest == mv[2]){
                targetMove = mv;
                count++;
            }
        }

        if (count == 1){
            return targetMove;
        }
        else {
            System.out.println("getMoveInMoveset(): Multiple moves detected, returning empty array");
            return new int[]{};
        }
    }

    public static int[] getMoveInMoveset(int piece, int dest, ArrayList<int[]> moveset, int dsRank, int dsFile){
        // boardState checking required for EP moves, in addition to basic validity
        // Files (columns)
        int[] fileA = {0, 8, 16, 24, 32, 40, 48, 56};
        int[] fileB = {1, 9, 17, 25, 33, 41, 49, 57};
        int[] fileC = {2, 10, 18, 26, 34, 42, 50, 58};
        int[] fileD = {3, 11, 19, 27, 35, 43, 51, 59};
        int[] fileE = {4, 12, 20, 28, 36, 44, 52, 60};
        int[] fileF = {5, 13, 21, 29, 37, 45, 53, 61};
        int[] fileG = {6, 14, 22, 30, 38, 46, 54, 62};
        int[] fileH = {7, 15, 23, 31, 39, 47, 55, 63};

        int[][] files = new int[][] {
            fileA, fileB, fileC, fileD, fileE, fileF, fileG, fileH
        };

        // Ranks (rows)
        int[] rank1 = {0, 1, 2, 3, 4, 5, 6, 7};
        int[] rank2 = {8, 9, 10, 11, 12, 13, 14, 15};
        int[] rank3 = {16, 17, 18, 19, 20, 21, 22, 23};
        int[] rank4 = {24, 25, 26, 27, 28, 29, 30, 31};
        int[] rank5 = {32, 33, 34, 35, 36, 37, 38, 39};
        int[] rank6 = {40, 41, 42, 43, 44, 45, 46, 47};
        int[] rank7 = {48, 49, 50, 51, 52, 53, 54, 55};
        int[] rank8 = {56, 57, 58, 59, 60, 61, 62, 63};

        int[][] ranks = new int[][]{
            rank1, rank2, rank3, rank4, rank5, rank6, rank7, rank8
        };
        
        int[] emptyMove = new int[]{};

        if (dsRank >= 0 && dsFile >= 0){
            int origin = (dsRank * 8) + dsFile;
            int[] temp = new int[]{piece, origin, dest};

            for (int[] mv : moveset){
                if (Arrays.equals(temp, mv)){
                    return mv;
                }
            }
            return emptyMove;
        }
        else if (dsRank >= 0){
            for (int rankInd : ranks[dsRank]){
                for (int[] mv : moveset){
                    if ((piece == mv[0]) && (rankInd == mv[1]) && (dest == mv[2])){
                        return mv;
                    }
                }
            }
            return emptyMove;
        }
        else if (dsFile >= 0){
            for (int fileInd : files[dsFile]){
                for (int[] mv : moveset){
                    if ((piece == mv[0]) && (fileInd == mv[1]) && (dest == mv[2])){
                        return mv;
                    }
                }
            }
            return emptyMove;
        }

        return emptyMove;
    }

    public static void findInMovesetTest(){
        
        ArrayList<int[]> moveset = new ArrayList<int[]>();
        moveset.add(new int[]{2, 1, 12});    // Knight b1 to e2
        moveset.add(new int[]{2, 6, 12});    // Knight g1 to e2
        moveset.add(new int[]{-2, 29, 12});  // Black knight to e2
        moveset.add(new int[]{6, 4, 12});    // King to e2
        moveset.add(new int[]{4, 0, 16});    // Rook a1 to a3
        moveset.add(new int[]{4, 24, 16});   // Rook a4 to a3
        moveset.add(new int[]{-4, 19, 16});  // BRook to a3
        moveset.add(new int[]{5, 3, 12});    // Queen d1 to e2
        moveset.add(new int[]{5, 39, 12});   // Queen h5 to e2
        moveset.add(new int[]{-5, 28, 12});  // BQueen to e2

        boolean test1 = Arrays.equals(getMoveInMoveset(2, 12, moveset), new int[]{});
        boolean test2 = Arrays.equals(getMoveInMoveset(2, 12, moveset, -1, 6), new int[]{2, 6, 12}); // Knight move with FILE disamb.
        boolean test3 = Arrays.equals(getMoveInMoveset(-5, 12, moveset), new int[]{-5, 28, 12}); // Get black queen without disambig.
        boolean test4 = Arrays.equals(getMoveInMoveset(5, 12, moveset), new int[]{}); // White queen without disamb.
        boolean test5 = Arrays.equals(getMoveInMoveset(5, 12, moveset, 4, 7), new int[]{5, 39, 12}); // White queen with disamb.
        boolean test6 = Arrays.equals(getMoveInMoveset(4, 16, moveset, 0, -1), new int[]{4, 0, 16}); // Rook with RANK disamb.

        // Test moveInMoveset
        boolean test7 = moveInMoveset(new int[]{1, 8, 16}, moveset) == false;
        boolean test8 = moveInMoveset(new int[]{-4, 19, 59}, moveset) == false;
        boolean test9 = moveInMoveset(new int[]{4, 0, 16}, moveset) == true;
        boolean test10 = moveInMoveset(new int[]{-2, 29, 12}, moveset) == true;



        boolean[] tests = new boolean[]{test1, test2, test3, test4, test5, test6, test7, test8, test9, test10};

        int count = 1;
        for (boolean t : tests){
            String msg = t ? "Test " + count + " passed." : "Test " + count + " FAILED...";
            System.out.println(msg);
            count++;
        }

    }

    // Returns an arraylist containing 2-length string[] entries, which contain the metadata and moves of each game.
    public static ArrayList<String[]> extractPGN(String filepath){
        ArrayList<String[]> retArray = new ArrayList<>();
        ArrayList<String> metaArray = new ArrayList<>();
        ArrayList<String> movesArray = new ArrayList<>();

        String pgnString;
        StringBuilder sb = new StringBuilder();

        try(BufferedReader bf = new BufferedReader(new FileReader(filepath))){

            while (bf.ready()){
                sb.append(bf.readLine() + "\n");
            }

        }
        catch (Exception e){
            System.out.println("extractMetaPGN(): Error reading file");
        }

        pgnString = sb.toString();

        String metaRegex = "((?:\\n?\\[.*\\]\\n)+)";
        String movesRegex = "(?s)\\n1\\..*?[ ]+[01][.\\/]?\\d?-[01][.\\/]?\\d?";
        Pattern metaPattern = Pattern.compile(metaRegex);
        Pattern movesPattern = Pattern.compile(movesRegex);
        Matcher metaMatcher = metaPattern.matcher(pgnString);
        Matcher movesMatcher = movesPattern.matcher(pgnString);

        while (metaMatcher.find()){
            metaArray.add(metaMatcher.group());
        }

        while (movesMatcher.find()){
            movesArray.add(movesMatcher.group());
        }

        if (metaArray.size() != movesArray.size()){
            System.out.println("extractPGN() Error: Number of metadata and moves blocks do not match, returning empty array");
            return retArray;
        }

        for (int i = 0; i < metaArray.size(); i++){
            retArray.add(new String[]{metaArray.get(i), movesArray.get(i)});
        }

        return retArray;
    }

    public static ArrayList<Move> moveValidatorLogger(String filePath, String logPath){
        System.out.println("moveValidatorLogger(): Beginning logging...");
        long startTime = System.currentTimeMillis();
        PrintStream origOut = System.out;
        int count = 0;
        ArrayList<Move> retMoves = new ArrayList<>();

        try (PrintStream fileOut = new PrintStream(new FileOutputStream(logPath))){
            System.setOut(fileOut);

            ArrayList<String[]> temp = test.extractPGN(filePath);

            for (String[] tmv : temp){
                System.out.println("Count " + count);
                ArrayList<Move> tempMove = test.moveValidator(tmv[1]);
                System.out.println(tempMove.size() + " moves present");
                retMoves.addAll(tempMove);
                count ++;
            }

        }
        catch (Exception e){
            System.out.println("moveValidatorLogger() Error: " + e);
        }
        finally {
            System.setOut(origOut);
        }

        long endTime = System.currentTimeMillis();
        float difference = (endTime - startTime)/1000 ;

        String duration = String.format("%.1f", difference);

        System.out.println("moveValidatorLogger(): Logging done!");
        System.out.println("Logged " + (count + 1) + " games in " + duration + " seconds.");
        return retMoves;
    }
}
