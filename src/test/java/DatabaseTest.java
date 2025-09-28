import java.util.Arrays;

import com.YCorp.chessApp.server.db.RegexDatabase;

public class DatabaseTest {
    public static void test_0(){
        System.out.println("Unique players: " + RegexDatabase.getUniquePlayers().size() + " entries");
        System.out.println("Unique sites: " + RegexDatabase.getUniqueSites().size() + " entries");
        System.out.println("Unique events: " + RegexDatabase.getUniqueEvents().size() + " entries");
        System.out.println("Total rows: " + RegexDatabase.countAll());
    }
}
