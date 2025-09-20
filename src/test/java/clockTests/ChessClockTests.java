package clockTests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import com.YCorp.chessApp.client.engine.ChessClock;

public class ChessClockTests {
    public static void toggleConsistencyTest(){
        ArrayList<Long> userTimestamps = new ArrayList<>();
        boolean whitesTurn = true;

        int setTime = 5;
        long setTimeMillis = setTime * 1000;
        int increment = 0;
        try {
            Scanner scanner = new Scanner(System.in);
            ChessClock clock = new ChessClock(0, 0, setTime, increment, 50);
            System.out.println("toggleConsistencyTest(): Any key to begin...");
            scanner.nextLine();

            long temp = System.nanoTime();
            userTimestamps.add(temp);

            clock.start();
            while(clock.isRunning()){
                scanner.nextLine();
                long temp2 = System.nanoTime();
                clock.toggle();
                userTimestamps.add(temp2);

                whitesTurn = !whitesTurn;
                System.out.println("whileLoop");
            }
            scanner.close();
            
            long timeoutTime = System.nanoTime();

            
            userTimestamps.add(timeoutTime);
            System.out.println("User " + userTimestamps);


            long userWhiteTime = 0;
            long userBlackTime = 0;

            // Add up all user times
            boolean turn = true;
            for (int i = 1; i < userTimestamps.size(); i++){
                long elapsed = userTimestamps.get(i) - userTimestamps.get(i - 1);

                if (turn){
                    userWhiteTime += elapsed;
                }
                else {
                    userBlackTime += elapsed;
                }

                turn = !turn;
            }


            System.out.println("Set time: " + setTimeMillis);
            System.out.println("TIME LEFT: White - " + clock.getTimeLeft(true) + ", Black - " + clock.getTimeLeft(false));

            System.out.println("User white: " + userWhiteTime/1000000);
            System.out.println("User black: " + userBlackTime/1000000);

        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    
}
