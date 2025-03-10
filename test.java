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

import db.Database;
import db.GameData;
import engine.Board;
import parser.PgnParser;

import java.nio.file.*;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;


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

}
