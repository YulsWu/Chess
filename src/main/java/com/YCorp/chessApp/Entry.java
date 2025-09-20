package com.YCorp.chessApp;
import com.YCorp.chessApp.client.javafx.BoardMaker;
import com.YCorp.chessApp.client.lanterna.LanternaChess;


public class Entry{
    public static void main(String[] args){
        // LanternaChess.lanternaLoop();
        // System.out.println("Exiting");
        BoardMaker.launch(BoardMaker.class, args);
    }
}

