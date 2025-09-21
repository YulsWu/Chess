import java.util.Arrays;

import com.YCorp.chessApp.client.engine.StockfishClient;

public class StockfishTest {
    // Cramling v Botez
    public static String testFEN_0 = "2r2rk1/3q1pp1/p1n1bn1p/1p2p3/3pPP2/1P1P2N1/P2BB1PP/2RQ1RK1 b - f3 0 18";
    public static String testFEN_1 = "r1bqkb1r/pp3ppp/2n2n2/2ppp3/8/3PP1N1/PPPN1PPP/R1BQKB1R w KQkq - 2 6";
    public static String testFEN_2 = "r1bq1r1k/ppp3pp/2N1p3/3p1p2/1bPPn3/2NBP3/PPQ2PPP/R1B2RK1 b - - 0 10";

    public static void moveExtractionTest(){
        StockfishClient sf = new StockfishClient();

        for (String s : new String[]{testFEN_0, testFEN_1, testFEN_2}){

            sf.sendCommand("position fen " + s);
            sf.sendCommand("go depth 5");
    
            try {
                Thread.sleep(50);
            }
            catch (Exception e){
                e.printStackTrace();
            }
            System.out.println(Arrays.toString(sf.getBestMove()));
        }
    }

    
}
