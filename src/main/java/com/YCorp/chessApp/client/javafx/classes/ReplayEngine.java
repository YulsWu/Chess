package com.YCorp.chessApp.client.javafx.classes;

import java.util.ArrayList;
import java.util.ListIterator;

import com.YCorp.chessApp.client.engine.Board;
import com.YCorp.chessApp.client.engine.Move;
import com.YCorp.chessApp.client.parser.RegexParser;
import com.YCorp.chessApp.server.db.RegexGameData;

public class ReplayEngine {
    private ArrayList<Move> moves;
    private ListIterator<Move> moveIterator;
    private Board board;
    private ArrayList<int[]> validMoves;

    public ReplayEngine(RegexGameData rgd){
        board = new Board();
        validMoves = board.generateValidMoves();
        moves = RegexParser.PGNMoveValidator(rgd.getMoves());
        moveIterator = moves.listIterator();
        System.out.println("Moves is " + moves.size() + " elements long");   
    }
    

    public void forward(){
        System.out.println("Moves is " + moves.size() + " elements long");   
        if (moveIterator.hasNext()){
            Move mv = moveIterator.next();
            board.playMove(mv);
            validMoves = board.updateState(validMoves);
        }
    };
    public void back(){
        if (moveIterator.hasPrevious()){
            moveIterator.previous();
            board.undoLastMove();
        }
    };

    public Move getLastMove(){
        return board.peekMove() == null ? null : board.peekMove();
    }

    public int[][] getBoard(){
        return board.getBoard();
    }
}
