package com.YCorp.chessApp.client.javafx.classes;

import java.util.HashMap;
import java.util.Map;

import javafx.event.EventHandler;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

public class GUIPiece extends ImageView {
    private static Image whitePawn = new Image(GUIPiece.class.getResource("/graphics/icons/white-pawn.png").toExternalForm());
    private Image whiteKnight = new Image(GUIPiece.class.getResource("/graphics/icons/white-knight.png").toExternalForm());
    private Image whiteBishop = new Image(GUIPiece.class.getResource("/graphics/icons/white-bishop.png").toExternalForm());
    private Image whiteRook = new Image(GUIPiece.class.getResource("/graphics/icons/white-rook.png").toExternalForm());
    private Image whiteQueen = new Image(GUIPiece.class.getResource("/graphics/icons/white-queen.png").toExternalForm());
    private Image whiteKing = new Image(GUIPiece.class.getResource("/graphics/icons/white-king.png").toExternalForm());

    private Image blackPawn = new Image(GUIPiece.class.getResource("/graphics/icons/black-pawn.png").toExternalForm());
    private Image blackKnight = new Image(GUIPiece.class.getResource("/graphics/icons/black-knight.png").toExternalForm());
    private Image blackBishop = new Image(GUIPiece.class.getResource("/graphics/icons/black-bishop.png").toExternalForm());
    private Image blackRook = new Image(GUIPiece.class.getResource("/graphics/icons/black-rook.png").toExternalForm());
    private Image blackQueen = new Image(GUIPiece.class.getResource("/graphics/icons/black-queen.png").toExternalForm());
    private Image blackKing = new Image(GUIPiece.class.getResource("/graphics/icons/black-king.png").toExternalForm());

    private final Map<Integer, Image> PIECE_TO_IMAGE = new HashMap<>(){{
        put(-6, blackKing);
        put(-5, blackQueen);
        put(-4, blackRook);
        put(-3, blackBishop);
        put(-2, blackKnight);
        put(-1, blackPawn);
        put(1, whitePawn);
        put(2, whiteKnight);
        put(3, whiteBishop);
        put(4, whiteRook);
        put(5, whiteQueen);
        put(6, whiteKing);
    }};

    int piece;

    // public GUIPiece(int piece, double squareDim, EventHandler<MouseEvent> pressedHandler, EventHandler<MouseEvent> draggedHandler, EventHandler<MouseEvent> releasedHandler){
    //     this.piece = piece;

    //     this.setImage(this.PIECE_TO_IMAGE.get(piece));
    //     this.setPreserveRatio(true);
    //     this.setFitWidth(squareDim);
    //     this.setFitHeight(squareDim);
    //     this.setMouseTransparent(true);

    //     this.setOnMousePressed(pressedHandler);
    //     this.setOnMouseDragged(draggedHandler);
    //     this.setOnMouseReleased(releasedHandler);
    // }

    public GUIPiece(int piece, double squareDim){
        this.piece = piece;

        this.setImage(this.PIECE_TO_IMAGE.get(piece));
        this.setPreserveRatio(true);
        this.setFitWidth(squareDim);
        this.setFitHeight(squareDim);
        this.setMouseTransparent(true);
    }

    public int getPieceInt(){
        return this.piece;
    }

    
}
