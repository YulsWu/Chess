package com.YCorp.chessApp.client.javafx.classes;

import java.util.ArrayList;

import com.YCorp.chessApp.client.parser.RegexParser;
import com.YCorp.chessApp.server.db.RegexDatabase;
import com.YCorp.chessApp.server.db.RegexGameData;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class DatabaseWriteService extends Service<Integer> {
    private String pgn;
    private Thread taskThread;

    public void setPgn(String pgn){
        this.pgn = pgn;
    }
    
    @Override
    protected Task<Integer> createTask(){
        String pgnStr = pgn;
        return new Task<>() {
            @Override
            protected Integer call() throws Exception {
                taskThread = Thread.currentThread();
                if (pgn == null || pgn == ""){
                    return -1;
                }
                //Do stuff
                int rowsAffected = 0;
                int unwritten;
                ArrayList<RegexGameData> games = RegexParser.extractPGN(pgn);
                
                if (Thread.interrupted()){
                    return -1;
                }

                System.out.println("STATUS: " + games.size() + " games extracted...");
                unwritten = RegexDatabase.writeDB(games, 50).size();
    
                System.out.println("STATUS: " + "Finished with " + unwritten + " errors");
                System.out.println("DONE: Exit or go again");
    
                rowsAffected += games.size() - unwritten;

                return rowsAffected;
            }
        };
    }

    public void interruptTask(){
        if (taskThread != null && taskThread.isAlive()){
            taskThread.interrupt();
        }
    }
}
