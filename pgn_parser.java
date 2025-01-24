import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class pgn_parser {
    
    public static ArrayList<game_data> parse(String filepath){
        ArrayList<String[]> games_list = new ArrayList<>();  

        // Load PGN data into memory, then close file reader
        try (BufferedReader f = new BufferedReader(new FileReader(filepath))) {
            System.out.println("Reading file...");

            // Iterate through all lines of the PGN file
            // Read and concatenate meta data lines as well as moveset lines, resulting in 2 large strings to be processed after the file is closed.
            while(f.ready()){
                String line;
                StringBuilder meta_string = new StringBuilder("");
                StringBuilder moves_string = new StringBuilder("");

                // Read and store meta data as one string block, separated by _ delimiter
                // There will always be 10 metadata fields in the PGN
                for (int i = 0; i < 10; i++){
                    meta_string.append("_");
                    meta_string.append(f.readLine());
                }

                // Skips whitespace between metadata and moves
                f.readLine();

                // Process the lines relating to the moveset - Breaking out of the while loop when the next newline is detected, or end of file
                line = f.readLine();
                // While loop breaks when the next empty newline after the moveset is reached, or the end of file is reached returning a null.
                while (line != null && !line.equals("")){ // Skips the object comparison when line == null, preventing null pointer exceptions
                    moves_string.append(line);
                    moves_string.append(" ");
                    line = f.readLine();
                }

                games_list.add(new String[]{meta_string.toString(), moves_string.toString()});
            }
            System.out.println("Read successful.");

        } catch (IOException e) {
            System.out.println("Error reading file: " + e);
        }

        System.out.println("File closed.");

        // Process PGN data stored in memory
        int num_games = games_list.size();
        ArrayList<game_data> retArray = new ArrayList<>(num_games);
        ArrayList<String> temp_meta;
        ArrayList<String[]> temp_moves;
        for (String[] g  : games_list){
            temp_meta = meta_parser(g[0]);
            temp_moves = moves_parser(g[1]);

            retArray.add(new game_data(temp_meta, temp_moves));
        }

        return retArray;
    }

    public static ArrayList<String[]> moves_parser (String s){
        int pInd;
        StringTokenizer st = new StringTokenizer(s, " ");
        int numTokens = st.countTokens();
        // Initialize empty array to hold moves, Rows = numMoves, col = 2
        ArrayList<String[]> returnArray = new ArrayList<>();
        for (int i = 0; i < ((int)(numTokens/2)); i++){
            returnArray.add(new String[]{"", ""});
        }

        String token;
        int row;
        int col;
        for (int i = 0; i < numTokens - 1; i++){
            row = (int) i / 2;
            col = i % 2;
            token = st.nextToken();
            pInd = token.indexOf(".");
            token = (pInd < 0) ? token : token.substring(pInd + 1); // Truncate the turn number from the front of white's moves if there is one.
            returnArray.get(row)[col] = token;
        }

        // If white wins and black has no move on the last turn
        if (returnArray.get(returnArray.size() - 1)[1] == null){
            returnArray.get(returnArray.size() - 1)[1] = "";
        }

        return returnArray;
    }

    public static ArrayList<String> meta_parser(String line){
        ArrayList<String> retArray = new ArrayList<>();
        // Split apart individual meta data fields
        StringTokenizer st1 = new StringTokenizer(line, "_");
        while (st1.hasMoreTokens()){
            StringTokenizer st2 = new StringTokenizer(st1.nextToken(), "[] \"");
            st2.nextToken(); // Skip the metadata field name
            
            // Tokenize each meta data field individually
            StringBuilder temp = new StringBuilder("");
            while (st2.hasMoreTokens()){
                temp.append(st2.nextToken());
                temp.append((st2.hasMoreTokens()) ? " " : ""); // If there are more tokens include a space afterwards
            }

            retArray.add(temp.toString());
        }
        return retArray;

    }
}
