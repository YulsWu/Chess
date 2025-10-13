package com.YCorp.chessApp.client.javafx.classes;

import java.util.ArrayList;
import java.util.ListIterator;

import com.YCorp.chessApp.client.engine.Board;
import com.YCorp.chessApp.client.engine.Move;
import com.YCorp.chessApp.client.parser.RegexParser;
import com.YCorp.chessApp.server.db.RegexGameData;

public class ReplayEngine {
    private ArrayList<Move> moves;
    private ArrayList<String> algebraicMoves;
    private ListIterator<Move> moveIterator;
    private Board board;
    private ArrayList<int[]> validMoves;
    private int currentIndex = -1;


    public ReplayEngine(RegexGameData rgd){
        board = new Board();
        validMoves = board.generateValidMoves();
        moves = RegexParser.PGNMoveValidator(rgd.getMoves());
        moveIterator = moves.listIterator();
        algebraicMoves = rgd.getMoves();
    }
    

    public void forward(){    
        if (moveIterator.hasNext()){
            Move mv = moveIterator.next();
            board.playMove(mv);
            validMoves = board.updateState(validMoves);
            currentIndex++;
        }
    };
    public void back(){
        if (moveIterator.hasPrevious()){
            moveIterator.previous();
            board.undoLastMove();
            currentIndex--;
        }
    };

    public Move getLastMove(){
        return board.peekMove() == null ? null : board.peekMove();
    }

    public int[][] getBoard(){
        return board.getBoard();
    }
    
    public ArrayList<String> getAlgebraicMoves(){
        return (ArrayList<String>) algebraicMoves.clone();
    }

    public int getPointerIndex(){
        return currentIndex;
    }



}
