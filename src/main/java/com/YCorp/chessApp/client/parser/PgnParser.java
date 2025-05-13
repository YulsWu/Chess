package com.YCorp.chessApp.client.parser;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import com.YCorp.chessApp.server.db.GameData;

import java.util.Map;
import java.util.HashMap;

public class PgnParser {
    
    public static ArrayList<GameData> parse(String filepath){
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
                line = f.readLine();
                //System.out.println(line);
                while (!(line.equals(""))){
                    meta_string.append(line);
                    meta_string.append("_");
                    line = f.readLine();
                }
                // for (int i = 0; i < 10; i++){
                //     meta_string.append("_");
                //     meta_string.append(f.readLine());
                // }

                // // Skips whitespace between metadata and moves
                // f.readLine();

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
        ArrayList<GameData> retArray = new ArrayList<>(num_games);
        Map<String, String> temp_meta;
        ArrayList<String[]> temp_moves;

        for (String[] g  : games_list){

            temp_meta = meta_parser(g[0]);
            temp_moves = moves_parser(g[1]);

            retArray.add(new GameData(temp_meta, temp_moves));
        }

        return retArray;
    }

    public static ArrayList<String[]> moves_parser (String s){

        // Reformat moveset if required, if not it just sets s = s;
        s = moveSetFormatter(s);
        StringTokenizer st = new StringTokenizer(s, " ");
        int numTokens = st.countTokens() - 1; // Exclude result token
        ArrayList<String[]> returnArray = new ArrayList<>();
        
        // Determine number of turn count labels through ceiling division
        int numSkip = (int) (numTokens + 3 - 1)/ 3; 
        int numMoves = numTokens - numSkip;
        
        for (int i = 0; i < ((int)((numMoves + 2 - 1)/2)); i++){
            returnArray.add(new String[]{"", ""});
        }

        int row;
        int col;
        int i = 0;
        // While loop with manually incremented counter so I can skip the tokens that represent move number
        // ie 1. e4 e5 --> [1., e4, e5] - we skip '1.'
        while (i < numMoves){
            String currentToken = st.nextToken();
            if (currentToken.indexOf('.') > 0){
                continue;
            }
            row = (int) i / 2;
            col = i % 2;       
            returnArray.get(row)[col] = currentToken;
            i++;
        }


        // If white wins and black has no move on the last turn
        // if (returnArray.get(returnArray.size() - 1)[1] == null){
        //     returnArray.get(returnArray.size() - 1)[1] = "";
        // }

        return returnArray;
    }

    public static Map<String, String> meta_parser(String line){
        Map<String, String> retMap = createMetaMapTemplate();
        StringTokenizer outer_tok = new StringTokenizer(line, "_");

        String meta_field;
        String meta_title;
        String meta_value;
        // While loop iterates tokens by 2, for each key-value pair of metadata
        // Currently will throw an exception if a metadata field has a blank string ie [Event ""], however this should not happen
        // and there should at least be a ? character in place of the field ie [Event "?"]
        while (outer_tok.hasMoreTokens()){
            meta_field = outer_tok.nextToken();
            StringTokenizer inner_tok = new StringTokenizer(meta_field, "\"[]");

            // Skip if the metadata field only has 1 token, meaning there was an empty string for the field, or nothing in the field at all.
            // The default values of the set are null to begin with.
            if (inner_tok.countTokens() < 2){
                continue;
            }
            
            meta_title = inner_tok.nextToken();
            meta_value = inner_tok.nextToken();
            
            meta_title = meta_title.trim();

            if (retMap.containsKey(meta_title)){
                // For empty strings or ? values, insert null instead
                if (meta_value.equals("?") || meta_value.equals(" ")){
                    retMap.put(meta_title, null);
                }
                else {
                    retMap.put(meta_title, meta_value);
                }
            }
        }

        // Split apart individual meta data fields
        // StringTokenizer st1 = new StringTokenizer(line, "_");
        // while (st1.hasMoreTokens()){
        //     StringTokenizer st2 = new StringTokenizer(st1.nextToken(), "[] \"");
        //     st2.nextToken(); // Skip the metadata field name
            
        //     // Tokenize each meta data field individually
        //     StringBuilder temp = new StringBuilder("");
        //     while (st2.hasMoreTokens()){
        //         temp.append(st2.nextToken());
        //         temp.append((st2.hasMoreTokens()) ? " " : ""); // If there are more tokens include a space afterwards
        //     }

        //     retArray.add(temp.toString());
        // }
        return retMap;

    }

    // Helper function to reformat moveset strings that don't fit the proper format.
    // Adds a space after each turn label (1., 2.) so the StringTokenizer in move_parser works properly
    public static String moveSetFormatter(String moveSet){
        
        if (requiresReformat(moveSet)){
            StringBuilder sb = new StringBuilder();
            
            for (char c : moveSet.toCharArray()){
                sb.append(c);
                if (c == '.'){
                    sb.append(" ");
                }
            }
            return sb.toString();
        }
        else {
            return moveSet;
        }


    }

    public static boolean requiresReformat(String moveSet){
        if (moveSet.charAt(moveSet.indexOf('.') + 1) == ' '){
            return false;
        }
        else {
            return true;
        }
    }

    public static Map<String, String> createMetaMapTemplate(){
        Map<String, String> retMap = new HashMap<String, String>(){{
            put("Event", null);
            put("Site", null);
            put("Date", null);
            put("Round", null);
            put("White", null);
            put("Black", null);
            put("Result", null);
            put("WhiteElo", null);
            put("BlackElo", null);
            put("WhiteTitle", null);
            put("BlackTitle", null);
            put("WhiteFideId", null);
            put("BlackFideId", null);
            put("ECO", null);
            put("Opening", null);
            put("Variation", null);
            put("TimeControl", null);
            put("Termination", null);
            put("Mode", null);
            put("PlyCount", null);
            put("EventType", null);
            put("EventRounds", null);
            put("Stage", null);
            put("Annotator", null);
            put("Source", null);
            put("SourceDate", null);
            put("FEN", null);
            put("SetUp", null);
            put("Variant", null);
        }};

        return retMap;
    }
}
