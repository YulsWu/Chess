
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;

public class entry {
    public static void main(String[] args){
        String filepath = "test.pgn";

        ArrayList<game_data> data = pgn_parser.parse(filepath);

        // for (int i = 0; i < 10; i++){
        //     System.out.println(data.get(i).stringID);
        // }

        // ArrayList<Integer> hashes = new ArrayList<>();

        ArrayList<game_data> test_data = new ArrayList<>();
        test_data.add(data.get(0));
        database.write_db(test_data);

    }
}