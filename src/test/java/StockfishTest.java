import java.util.Arrays;

import com.YCorp.chessApp.client.engine.StockfishClient;

public class StockfishTest {
    // Cramling v Botez
    public static String testFEN_0 = "2r2rk1/3q1pp1/p1n1bn1p/1p2p3/3pPP2/1P1P2N1/P2BB1PP/2RQ1RK1 b - f3 0 18";
    public static String testFEN_1 = "r1bqkb1r/pp3ppp/2n2n2/2ppp3/8/3PP1N1/PPPN1PPP/R1BQKB1R w KQkq - 2 6";
    public static String testFEN_2 = "r1bq1r1k/ppp3pp/2N1p3/3p1p2/1bPPn3/2NBP3/PPQ2PPP/R1B2RK1 b - - 0 10";

    // Carlsen v Theodorou
    public static String testFEN_3 = "2k1r3/ppp2Np1/7p/n2p4/3P1R2/2PP2PP/P5K1/8 b - - 1 25";
    public static String testFEN_4 = "r2qk2r/ppp1bppp/2n5/3pP3/3P2b1/2PB1N2/P1P3PP/R1BQK2R b KQkq - 2 9";
    public static String testFEN_5 = "2k1r1R1/1pp4P/8/3p2P1/8/8/3K4/q7 w - - 0 41";

    //Naroditsky vs Minh Le
    public static String testFEN_6 = "rnbqk2r/ppppp1bp/5np1/6B1/2BP4/2N2N2/PPP3PP/R2QK2R b KQkq - 2 7";
    public static String testFEN_7 = "4r2k/1R4np/2pB2pb/p2pP3/7P/3P4/PP2NKP1/8 w - - 1 27";
    public static String testFEN_8 = "3K2k1/3RP2p/6p1/6P1/p2pr2P/8/8/8 b - - 0 44";


    public static void moveExtractionTest(){
        StockfishClient sf = new StockfishClient();

        for (String s : new String[]{testFEN_0, testFEN_1, testFEN_2, testFEN_3, testFEN_4, testFEN_5, testFEN_6, testFEN_7, testFEN_8}){
        
            sf.sendCommand("position fen " + s);
            sf.sendCommand("go depth 5");
    
          
            System.out.println(Arrays.toString(sf.getBestMove()));
        }

        System.out.println("Sending new game command");
        if (sf.sendUciNewGameAndWait()){
            System.out.println("Uci new game successful");
        }
        else {
            System.out.println("ERROR: Uci new game unsuccessful");
        }
    }

    
}
