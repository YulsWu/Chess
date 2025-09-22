package com.YCorp.chessApp.client.javafx.classes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.YCorp.chessApp.client.engine.Board;
import com.YCorp.chessApp.client.engine.ChessClock;
import com.YCorp.chessApp.client.engine.callback.TickCall;
import com.YCorp.chessApp.client.engine.callback.TimeoutCall;

public class GUIEngine{

    private Board board;
    private ChessClock clock;
    private ArrayList<int[]> currentValidMoves;

    // Creation of a game with no clock
    public GUIEngine(){
        board = new Board();
        currentValidMoves = board.generateValidMoves();
    }


    // Overloaded constructor for creating a GUIEngine with a clock, automatically sets Board as a timeout callback target
    public GUIEngine(int hours, int minutes, int seconds, int incrementSeconds, int milliTick){
        board = new Board();
        currentValidMoves = board.generateValidMoves();
        clock = new ChessClock(hours, minutes, seconds, incrementSeconds, milliTick);
        clock.addTimeoutCallbackTarget(board);
    }

    // Maybe implement this as a builder pattern instead, where we can create a GUIEngine and incrementally add clocks and clock times - Would require an "invalid" state before use (ie Added clock and white time, no black time, etc)
    public GUIEngine(int w_hours, int w_minutes, int w_seconds, int w_increment, int b_hours, int b_minutes, int b_seconds, int b_increment, int milliTick){
        board = new Board();
        currentValidMoves = board.generateValidMoves();
        clock = new ChessClock(w_hours, w_minutes, w_seconds, w_increment, b_hours, b_minutes, b_seconds, b_increment, milliTick);
        clock.addTimeoutCallbackTarget(board);
    }

    public int attemptMove(int[] move){
        if (move.length != 2) return -1;

        int moveInd = findMove(move, this.currentValidMoves);
        if (moveInd >= 0){
            board.playMove(board.createMove(this.currentValidMoves.get(moveInd)));
            this.currentValidMoves = board.updateState(this.currentValidMoves);
            int state = board.evaluateGameEndConditions(this.currentValidMoves);

            // Toggle clock if game continues, otherwise stop it.
            if (state == 0){
                toggleClock();
            }
            else {
                stopClock();
            }

            return state;
        }
        else {
            System.out.println("attemptMove(): Bad move " + Arrays.toString(move));
            return -1;
        }
    }

    private int findMove(int[] candidate, ArrayList<int[]> validMoves){
        for (int i = 0; i < validMoves.size(); i++){
            if (validMoves.get(i)[1] == candidate[0] && validMoves.get(i)[2] == candidate[1]){
                return i;
            }
        }
        return -1;
    }

    public int[][] getBoard(){
        return board.getBoard();
    }
    
    // DEBUG
    public ArrayList<String[]> getStatus(){
        return board.getBoardStatus();
    }

    private void printStatus(){
        System.out.println("---------------------------------------------------------------");
        for (String[] strArr : board.getBoardStatus()){
            System.out.println(strArr[0] + ": " + strArr[1]);
        }
        System.out.println("---------------------------------------------------------------");
    }

    public void addTickCallbackTarget(TickCall t){
        clock.addTickCallbackTarget(t);
    }

    public void addTimeoutCallbackTarget(TimeoutCall t){
        clock.addTimeoutCallbackTarget(t);
    }
    

    // Toggles the clock if it exists
    private void toggleClock(){
        if (clock != null){
            clock.toggle();
        }
    }

    private void stopClock(){
        if (clock != null){
            clock.stop();
        }
    }

    public void cleanup(){
        if (clock != null) clock.stop();
    }

    public boolean isTimed(){
        if (this.clock != null){
            return true;
        }
        else {
            return false;
        }
    }

    // Max value of INT in milliseconds is ~25 days, unlikely to ever need long capacity for this returned value for a chess game
    public long getTimeMillis(boolean whitePlayer){
        return this.clock.getTimeLeft(whitePlayer);
        
    }

    public boolean isWhitesTurn(){
        return this.board.getTurnBool();
    }

    
    public String getFEN(){
        return this.board.boardToFEN();
    }
}
