package com.YCorp.chessApp.client.exceptions;
public class ChessServiceDoesNotExistException extends ChessServiceException{
    public ChessServiceDoesNotExistException(String message){
        super(message);
    }

    public ChessServiceDoesNotExistException(String message, Exception e){
        super(message, e);
    }
}
