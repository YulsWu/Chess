package com.YCorp.chessApp.client.engine;

import com.YCorp.chessApp.server.db.RegexGameData;

import java.util.ArrayList;

import com.YCorp.chessApp.client.parser.RegexParser;

// Cut-down version of GUIEngine, just plays moves and doesn't do any end-game checking as provided moves should already be valid
public class ReplayClient {
    private Board board;
    private RegexGameData gameData;
    private int nextMove;
    private ArrayList<Move> movesList;
    private ArrayList<int[]> validMoves;

    public ReplayClient(RegexGameData gd){
        this.gameData = gd;
        nextMove = 0;

        movesList = RegexParser.PGNMoveValidator(gameData.moves);
        board = new Board();
        validMoves = board.generateValidMoves();
    }

    public void forward(){
        Move mv = movesList.get(nextMove);
        board.playMove(mv);
        validMoves = board.updateState(validMoves);
        nextMove++;
    }

    public void back(){
        board.undoLastMove();
        nextMove--;
        validMoves = board.generateValidMoves();
    }

    public boolean ready(){
        if (nextMove < gameData.moves.size()){
            return true;
        }
        else {
            return false;
        }
    }

    public int getCurrentMoveInd(){
        return this.nextMove;
    }

    public String getBoardVisualization(){
        return board.boardVisualize();
    }
    
}
