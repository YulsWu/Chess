import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;

public class database {
    // Currently unsuitable for hash collision handling
    public static ArrayList<Integer> generate_hashcode(ArrayList<game_data> games){
        ArrayList<Integer> return_hashes = new ArrayList<>();

        for (game_data g : games){
            return_hashes.add(g.getStringMoves().hashCode());
        }
        
        return return_hashes;

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
    }

    // Overloaded method signature for a single game
    public static ArrayList<Integer> generate_hashcode(game_data game){
        ArrayList<game_data> temp = new ArrayList<>();
        temp.add(game);

        return generate_hashcode(temp);
    }


    public static ArrayList<game_data> write_db(ArrayList<game_data> games){
        ArrayList<Integer> ids = new ArrayList<>();
        ArrayList<game_data> unwritten = new ArrayList<>();
        
        String database = "jdbc:mysql://localhost:3306/pgn_database";
        String username = "root";
        String password = "fUZ8&ejS4]";

        PreparedStatement stmt = null;
        ResultSet rs = null;

        try(Connection conn = DriverManager.getConnection(database, username, password);
            ByteArrayOutputStream byte_stream = new ByteArrayOutputStream();
            ObjectOutputStream obj_stream = new ObjectOutputStream(byte_stream)){

            if (!(conn == null)){
                System.out.println("Connection successful");
            }

            // Build SQL query with the information stored in game_data
            StringBuilder sb = new StringBuilder();
            byte[] byte_moves;
            String qTemplate = "INSERT INTO games " + 
                            "(id, chess_event, site, game_date, round, white_player, black_player," + 
                            " result, white_elo, black_elo, eco, moves) VALUES (";

            // Loop to create a query for each game that is to be written to the database
            // Begins by building the query string iteratively
            for (int i = 0; i < games.size(); i++){
                sb.setLength(0);
                sb.append(qTemplate);
                sb.append("'");
                sb.append(games.get(i).stringID);
                sb.append("'");
                
                // Insert metadata into query
                for (String s : games.get(i).meta){
                    sb.append(", ");
                    sb.append("'");
                    sb.append(s);
                    sb.append("'");  
                }

                // End query
                sb.append(", ?);");

                // Serialize the ArrayList<String[]> array holding the moves to be stored in sql
                obj_stream.writeObject(games.get(i).moves); // Serialize object and send to bytestream
                byte_moves = byte_stream.toByteArray(); // Convert bytestream to byte array

                // Creates the statement object from the query string
                stmt = conn.prepareStatement(sb.toString());
                // Set the first wildcard parameter to the byte representation of the moves array
                stmt.setBytes(1, byte_moves);
                
                int updated = stmt.executeUpdate();
                if (updated > 0){
                    System.out.print("Wrote " + (i + 1) + " files");
                }
                else {
                    System.out.println();
                    System.out.println("Write failed");
                }
            }
        }
        catch (SQLException ex){
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        catch (Exception e){
            System.out.println("Stream error: " + e);
        }

        return unwritten;


        /*
        try {
            // Load mysql connector
            URL jarURL = new URL("file:///" + connection_jar_path);
            URLClassLoader class_loader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            URLClassLoader url_class_loader = new URLClassLoader(new URL[]{jarURL}, class_loader);

            Thread.currentThread().setContextClassLoader(url_class_loader);
            // Done loading mysql connector
            // Connect to database

            

            Properties properties = new Properties();
            properties.put("user", username);
            properties.put("password", password);

            try (Connection pgn_connection = DriverManager.getConnection(url, properties)){
                if (!(pgn_connection == null)){
                    System.out.println("Successfully connected to " + url);
                }

                // Create statment object to execute SQL queries
                Statement stmt = pgn_connection.createStatement();
                String q = "SELECT * FROM pgn_database.games";

                // ResultSet rs = stmt.executeQuery(q);
            }
            catch (Exception e){
                System.out.println("Error connecting to database: " + e);
            }

        } catch (MalformedURLException e) {
            System.out.println("Error connecting to MySQL database: " + e);
        }
        */

    }
}
