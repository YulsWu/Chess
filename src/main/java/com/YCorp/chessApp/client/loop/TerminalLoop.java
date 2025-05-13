package com.YCorp.chessApp.client.loop;

import java.util.Scanner;
import java.util.ArrayList;

import com.YCorp.chessApp.client.exceptions.AlgebraicParseException;
import com.YCorp.chessApp.client.parser.RegexParser;
import com.YCorp.chessApp.client.engine.Board;
import com.YCorp.chessApp.client.engine.Move;

public class TerminalLoop {
    public static void startTerminalLoop(){
        boolean gameOn = true;
        Scanner scanner = new Scanner(System.in);
        String turnString = "White";

        Board board = new Board();
        ArrayList<int[]> validMoves = board.generateValidMoves(1);

        String playerInput;
        Move currentMove;
        
        while(gameOn){
            currentMove = null;
            
            gameLoop:
            while (currentMove == null){
                board.boardVisualize();
                System.out.println("Halfclock: " + board.getHalfClock());
                turnString = board.getTurnInt() > 0 ? "White" : "Black";
                
                if (board.getThreeFold() || board.getFiftyMove()){
                    String drawType = board.getThreeFold() ? "Three fold repetition" : "Fifty move rule";
                    System.out.println("Claim " + drawType + " draw?");
                    
                    boolean validDrawInput = false;
                    while (!validDrawInput){
                        String drawInput = scanner.nextLine();
                        if (drawInput.equalsIgnoreCase("yes")){
                            System.out.println(turnString + " claims " + drawType + " draw!");
                            return;
                        }
                        else if (drawInput.equalsIgnoreCase("no")){
                            validDrawInput = true;
                        }
                        else {
                            board.boardVisualize();
                            System.out.println("Invalid input, either 'yes' or 'no'");
                            System.out.println("Please try again:");
                        }
                    }
                }

                System.out.println(turnString + "'s turn, please input move");


                boolean moveInput = false;
                while (!moveInput){
                    playerInput = scanner.nextLine();
                    if (playerInput.equalsIgnoreCase("undo")){
                        board.undoLastMove();
                        continue gameLoop;
                    }
                    try {
                        currentMove = RegexParser.validateMove(playerInput, board);
                        moveInput = true;
        
                    }
                    catch (AlgebraicParseException e){
                        System.out.println(e);
                        System.out.println("Invalid move input, try again");
                    }

                }
            }
            
            board.playMove(currentMove);
            board.updateState(board.getTurnInt(), validMoves);

            // This invokes another valid move generation, perhaps create a local variable for Board
            // That holds the current valid moves to avoid re-generation of valid moves
            // which is quite expensive
           
            validMoves = board.generateValidMoves(board.getTurnInt());
            int endGame = board.evaluateGameEndConditions(validMoves);
           
            // "Forced" end game states
            if(endGame == 1){
                board.boardVisualize();
                System.out.println(turnString + " wins by Checkmate!");
                return;
            }
            else if(endGame >= 2) {
                board.boardVisualize();
                System.out.println("Draw!");
                return;
            }

        }
    }

    
}
