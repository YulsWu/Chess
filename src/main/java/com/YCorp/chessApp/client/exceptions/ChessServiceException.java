package com.YCorp.chessApp.client.exceptions;
public class ChessServiceException extends Exception {
    public ChessServiceException(String message){
        super(message);
    }

    public ChessServiceException(String message, Throwable cause){
        super(message, cause);
    }
}
