import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.sql.PreparedStatement;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class database {
    // Currently unsuitable for hash collision handling
    public static ArrayList<Integer> generate_hashcode(ArrayList<game_data> games){
        ArrayList<Integer> return_hashes = new ArrayList<>();

        for (game_data g : games){
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
    public static ArrayList<Integer> generate_hashcode(game_data game){
        ArrayList<game_data> temp = new ArrayList<>();
        temp.add(game);

        return generate_hashcode(temp);
    }


    public static ArrayList<game_data> write_db(ArrayList<game_data> games){
        ArrayList<game_data> unwritten = new ArrayList<>();
        int batch_size = 100;
        
        String database = "jdbc:mysql://localhost:3306/pgn_database";
        String username = "root";
        String password = "fUZ8&ejS4]";

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

                // Build SQL query with the information stored in game_data
                String query = "INSERT INTO games " + 
                                "(id, chess_event, site, game_date, round, white_player, black_player," + 
                                " result, white_elo, black_elo, eco, moves) VALUES " + 
                                " (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                // Loop to create a query for each game that is to be written to the database
                // Begins by building the query string iteratively
                stmt = conn.prepareStatement(query);
                i = 0;
                for (game_data gd : games){
                    set_values(gd, stmt, byte_stream, obj_stream);

                    stmt.addBatch();

                    i++;

                    if ((i % batch_size == 0) || (i == games.size() - 1)){
                        stmt.executeBatch();
                        conn.commit();
                        System.out.print("Added " + i + " games...");
                    }
                }
                System.out.println();
            }
            catch (SQLException ex){
                System.out.println("SQLException: " + ex.getMessage());
                System.out.println("SQLState: " + ex.getSQLState());
                System.out.println("VendorError: " + ex.getErrorCode());
                
                conn.rollback();
                unwritten = new ArrayList<game_data>(games.subList((int)(i / 50) * 50, games.size()));
                return unwritten;
            }
            catch (IOException e){
                System.out.println("Stream error: " + e);

                conn.rollback();
                unwritten = new ArrayList<game_data>(games.subList((int)(i / 50) * 50, games.size()));
                return unwritten;
            }
        }
        catch (Exception e) {
            System.out.println("Error rolling back database: " + e);
        }

        
        System.out.println("Successfully added games to database");
    

        return unwritten; // Should be empty if it reaches this point
    }

    public static void set_values(game_data game, PreparedStatement stmt, ByteArrayOutputStream ba_OS, ObjectOutputStream o_OS) throws Exception{
        // Serialize moveset
        byte[] byte_moves;
        o_OS.writeObject(game.moves);
        byte_moves = ba_OS.toByteArray();

        // Set values of PreparedStatment
        stmt.setString(1, game.stringID);
        stmt.setString(2, game.event);
        stmt.setString(3, game.site);
        stmt.setDate(4, game.date);
        stmt.setFloat(5, game.round);
        stmt.setString(6, game.white_player);
        stmt.setString(7, game.black_player);
        stmt.setString(8, game.result);
        stmt.setInt(9, game.white_elo);
        stmt.setInt(10, game.black_elo);
        stmt.setString(11, game.eco);
        stmt.setBytes(12, byte_moves);
    }

}

