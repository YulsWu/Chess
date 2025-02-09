
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;


public class entry {
    public static void main(String[] args){
        int batch_size = 100;
        
        String database = "jdbc:mysql://localhost:3306/pgn_database";
        String username = "root";
        String password = "fUZ8&ejS4]";
        String filepath = "carlsen.pgn";

        ArrayList<GameData> data = PgnParser.parse(filepath);
        //ArrayList<GameData> readData = Database.readDB(database, username, password);

        // System.out.println(data.get(0).stringID.equals(readData.get(0).stringID));
        // System.out.println(readData.get(0).moves.equals(readData.get(0).moves));

        Database.writeDB(data, database, username, password, batch_size);

        if (test.writeReadConsistencyTest(data, database, username, password)){
            System.out.println("Game consistency verified");
        }
        else {
            System.out.println("ERROR: Inconsistency between written/read games detected");
        }


        // readData = test.test2(database, username, password);

        // for (GameData gd : readData){
        //     System.out.println(gd);
        // }

        // for (int i = 0; i < 10; i++){
        //     System.out.println(data.get(i).stringID);
        // }

        // ArrayList<Integer> hashes = new ArrayList<>();

        //ArrayList<game_data> test_data = new ArrayList<>();
        //test_data.add(data.get(0));

        //game_data test_game = test_data.get(0);


        //test.test1();

        // String noSpaceStr = "1.e4 c6 2.d4 d5 3.e5 c5 4.Nf3 Nc6 5.c4 Bg4 6.dxc5 e6 7.cxd5 exd5 8.Nc3 Bxc5" + //
        //                 "9.Qxd5 Qxd5 10.Nxd5 O-O-O 11.Nc3 Nb4 12.Ne4 Nc2+ 13.Ke2 Bb6 14.Nd6+ Rxd6" + //
        //                 "15.exd6 Nxa1 16.Kd1 Nf6 17.Bd3 Rd8 18.Bf4 Nh5 19.Be5 f6 20.Bg3 Nxg3 21.fxg3 Rxd6" + //
        //                 "22.Ke2 Re6+ 23.Kd2 Be3+ 24.Kd1 Rc6 25.Re1 Rc1+ 26.Ke2 Rxe1+ 27.Kxe1 Be6 28.Ke2 Bc1" + //
        //                 "29.b3 g6 30.Kd1 Be3 31.Ke2 Bh6 32.Nd4 Bd5 33.Kf2 a6 34.h4 Kd7 35.g4 Bf8 36.h5 Bc5" + //
        //                 "37.Ke3 Bxb3 38.Bxg6 hxg6 39.h6 Bg8  0-1";
        // String spaceStr = "1. e4 e5 2. Nf3 Nf6 3. Nxe5 d6 4. Nf3 Nxe4 5. Bd3 Ng5 6. O-O d5 7. Re1+ Ne6 8." + //
        //                 "c4 d4 9. Qc2 g6 10. c5 Bg7 11. Bc4 O-O 12. d3 Nc6 13. a3 Nxc5 14. Bg5 Qd6 15." + //
        //                 "Nbd2 Bf5 16. b4 Ne6 17. Bh4 Rac8 18. Ne4 Bxe4 19. Rxe4 Ncd8 20. Rae1 a6 21. Ne5" + //
        //                 "Bxe5 22. Rxe5 Re8 23. Qa2 Kg7 24. Bb3 Nc6 25. R5e4 Qd7 26. Qe2 h6 27. Qf3 Ng5" + //
        //                 "28. Bxg5 hxg5 29. h3 Rxe4 30. Qxe4 Rh8 31. Ba4 Rh4 32. Qe2 b5 33. Bb3 Rf4 34." + //
        //                 "Rc1 Qd6 35. Rc5 Rf5 36. Qe8 Nd8 37. g3 c6 38. Kg2 Qc7 39. Qe4 Rxc5 40. bxc5 Qd7" + //
        //                 "41. Qe5+ f6 42. Qb8 a5 43. a4 bxa4 44. Bxa4 Nf7 45. Bb3 a4 46. Bxa4 Qd5+ 1/2-1/2";
        // System.out.println();
        // String[] strList = new String[]{noSpaceStr, spaceStr};

        // for (String s : strList){
        //     System.out.println(PgnParser.moveSetFormatter(s));
        // }
    }
}