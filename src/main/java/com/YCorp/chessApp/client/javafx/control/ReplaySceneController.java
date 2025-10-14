package com.YCorp.chessApp.client.javafx.control;

import java.util.ArrayList;
import java.util.Arrays;

import com.YCorp.chessApp.client.engine.Move;
import com.YCorp.chessApp.client.javafx.classes.GUIPiece;
import com.YCorp.chessApp.client.javafx.classes.ReplayEngine;
import com.YCorp.chessApp.client.javafx.classes.interfaces.Closeable;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class ReplaySceneController implements Closeable{
    //#region FXML Injection
    @FXML
    private Pane gamePane; // Root
    @FXML
    private GridPane boardPane;
    @FXML
    AnchorPane anchorPane;

    //#region Squares
    @FXML
    StackPane square0;
    @FXML
    StackPane square1;
    @FXML
    StackPane square2;
    @FXML
    StackPane square3;
    @FXML
    StackPane square4;
    @FXML
    StackPane square5;
    @FXML
    StackPane square6;
    @FXML
    StackPane square7;
    @FXML
    StackPane square8;
    @FXML
    StackPane square9;
    @FXML
    StackPane square10;
    @FXML
    StackPane square11;
    @FXML
    StackPane square12;
    @FXML
    StackPane square13;
    @FXML
    StackPane square14;
    @FXML
    StackPane square15;
    @FXML
    StackPane square16;
    @FXML
    StackPane square17;
    @FXML
    StackPane square18;
    @FXML
    StackPane square19;
    @FXML
    StackPane square20;
    @FXML
    StackPane square21;
    @FXML
    StackPane square22;
    @FXML
    StackPane square23;
    @FXML
    StackPane square24;
    @FXML
    StackPane square25;
    @FXML
    StackPane square26;
    @FXML
    StackPane square27;
    @FXML
    StackPane square28;
    @FXML
    StackPane square29;
    @FXML
    StackPane square30;
    @FXML
    StackPane square31;
    @FXML
    StackPane square32;
    @FXML
    StackPane square33;
    @FXML
    StackPane square34;
    @FXML
    StackPane square35;
    @FXML
    StackPane square36;
    @FXML
    StackPane square37;
    @FXML
    StackPane square38;
    @FXML
    StackPane square39;
    @FXML
    StackPane square40;
    @FXML
    StackPane square41;
    @FXML
    StackPane square42;
    @FXML
    StackPane square43;
    @FXML
    StackPane square44;
    @FXML
    StackPane square45;
    @FXML
    StackPane square46;
    @FXML
    StackPane square47;
    @FXML
    StackPane square48;
    @FXML
    StackPane square49;
    @FXML
    StackPane square50;
    @FXML
    StackPane square51;
    @FXML
    StackPane square52;
    @FXML
    StackPane square53;
    @FXML
    StackPane square54;
    @FXML
    StackPane square55;
    @FXML
    StackPane square56;
    @FXML
    StackPane square57;
    @FXML
    StackPane square58;
    @FXML
    StackPane square59;
    @FXML
    StackPane square60;
    @FXML
    StackPane square61;
    @FXML
    StackPane square62;
    @FXML
    StackPane square63;
    //#endregion

    @FXML
    Label playerClock;
    @FXML
    Label opponentClock;
    @FXML
    TextFlow playerTextFlow;
    @FXML
    TextFlow opponentTextFlow;
    @FXML
    TextFlow infoTextFlow;
    //#region Rank and File labels
    @FXML
    TextFlow rankLabel0;
    @FXML
    TextFlow rankLabel1;
    @FXML
    TextFlow rankLabel2;
    @FXML
    TextFlow rankLabel3;
    @FXML
    TextFlow rankLabel4;
    @FXML
    TextFlow rankLabel5;
    @FXML
    TextFlow rankLabel6;
    @FXML
    TextFlow rankLabel7;
    @FXML
    TextFlow fileLabel0;
    @FXML
    TextFlow fileLabel1;
    @FXML
    TextFlow fileLabel2;
    @FXML
    TextFlow fileLabel3;
    @FXML
    TextFlow fileLabel4;
    @FXML
    TextFlow fileLabel5;
    @FXML
    TextFlow fileLabel6;
    @FXML
    TextFlow fileLabel7;
    //#endregion

    @FXML 
    private ScrollPane movesPane;
    //#endregion

    // Class members
    
    private final Background MOVE_BACKGROUND = new Background(new BackgroundFill[]{new BackgroundFill(Color.GREY, null, null)});
    private final Background LIGHT_BACKGROUND = new Background(new BackgroundFill[]{new BackgroundFill(Color.web("#F0D9B5"), null, null)});
    private final Background DARK_BACKGROUND = new Background(new BackgroundFill[]{new BackgroundFill(Color.web("#B58863"), null, null)});
    private final Background HIGH_LIGHT_BACKGROUND = new Background(new BackgroundFill[]{new BackgroundFill(Color.web("#F4E288"), null, null)});
    private final Background HIGH_DARK_BACKGROUND = new Background(new BackgroundFill[]{new BackgroundFill(Color.web("#C7A64A"), null, null)});

    private TextFlow[] rankLabels;
    private TextFlow[] fileLabels;
    private String[] files = new String[]{"a", "b", "c", "d", "e", "f", "g", "h"};

    private ReplayEngine replayEngine;

    private String playerName;
    private String playerElo;
    private String opponentName;
    private String opponentElo;

    private double squareDim;

    // Order depending on white vs black perspective
    private ArrayList<StackPane> allSquares;
    private boolean whitePlayer;

    private Pane dummy = new Pane();

    private ArrayList<StackPane> highlightedSquares = new ArrayList<>();

    private GridPane movesGrid;

    private ArrayList<Label> moveLabels = new ArrayList<>();

    public void init(boolean whitePerspective){

        if (this.replayEngine == null){
            System.out.println("initBoard(): No ReplayEngine assigned to controller");
            return;
        }

        squareDim = boardPane.getWidth()/8;
        
        // Perspective-based board drawing
        if (whitePerspective){
            this.whitePlayer = true;
            
            rankLabels = new TextFlow[]{rankLabel0, rankLabel1, rankLabel2, rankLabel3, rankLabel4, rankLabel5, rankLabel6, rankLabel7};
            fileLabels = new TextFlow[]{fileLabel0, fileLabel1, fileLabel2, fileLabel3, fileLabel4, fileLabel5, fileLabel6, fileLabel7};
            
            StackPane[] temp = new StackPane[]{
                square0, square1, square2, square3, square4, square5, square6, square7,
                square8, square9, square10, square11, square12, square13, square14, square15,
                square16, square17, square18, square19, square20, square21, square22, square23,
                square24, square25, square26, square27, square28, square29, square30, square31,
                square32, square33, square34, square35, square36, square37, square38, square39,
                square40, square41, square42, square43, square44, square45, square46, square47,
                square48, square49, square50, square51, square52, square53, square54, square55,
                square56, square57, square58, square59, square60, square61, square62, square63
            };
            
            allSquares = new ArrayList<StackPane>(Arrays.asList(temp));
        }
        else {
            this.whitePlayer = false;
            
            rankLabels = new TextFlow[]{
                rankLabel7, rankLabel6, rankLabel5, rankLabel4,
                rankLabel3, rankLabel2, rankLabel1, rankLabel0
            };
            
            fileLabels = new TextFlow[]{
                fileLabel7, fileLabel6, fileLabel5, fileLabel4,
                fileLabel3, fileLabel2, fileLabel1, fileLabel0
            };
            
            StackPane[] temp = new StackPane[]{
                square63, square62, square61, square60, square59, square58, square57, square56,
                square55, square54, square53, square52, square51, square50, square49, square48,
                square47, square46, square45, square44, square43, square42, square41, square40,
                square39, square38, square37, square36, square35, square34, square33, square32,
                square31, square30, square29, square28, square27, square26, square25, square24,
                square23, square22, square21, square20, square19, square18, square17, square16,
                square15, square14, square13, square12, square11, square10, square9, square8,
                square7, square6, square5, square4, square3, square2, square1, square0
            };
            
            allSquares = new ArrayList<StackPane>(Arrays.asList(temp));
        }

        // Draw light and dark squares
        drawBoard();
        //#endregion

        //#region Node Set-up

        this.playerName = "Player";
        this.playerElo = "1";
        this.opponentName = "Opponent";
        this.opponentElo = "9999";

        playerTextFlow.getChildren().add(new Text(this.playerName));
        playerTextFlow.getChildren().add(new Text("\n" + this.playerElo));
        opponentTextFlow.getChildren().add(new Text(this.opponentName));
        opponentTextFlow.getChildren().add(new Text("\n" + this.opponentElo));


        updateBoardPieces();

        // Include dummy node in scene graph so events fired from it can be captured by stage
        gamePane.getChildren().add(dummy);
        dummy.setVisible(false);
        dummy.setMouseTransparent(true);

        // Event filters
        gamePane.addEventFilter(KeyEvent.KEY_PRESSED, this::keyPressHandler);
        gamePane.setFocusTraversable(true);
        gamePane.requestFocus();

        // Algebraic Moves Display
        drawMovePane();        

        // Force a layout pass before stage.show() to prevent pop-in UI elements
        gamePane.layout();
    }

    private void drawMovePane(){
        movesGrid = new GridPane();
        movesGrid.setPrefWidth(movesPane.getPrefWidth());

        ArrayList<String> algebraicMoves = replayEngine.getAlgebraicMoves();
        int numRows = (algebraicMoves.size() + 2 - 1) / 2; //Ceiling division

        // Create rows
        for (int i = 0; i < numRows; i++){
            RowConstraints rc = new RowConstraints();
            rc.setPrefHeight(25);

            movesGrid.getRowConstraints().add(rc);
        }
        // Create columns
        ColumnConstraints moveCount = new ColumnConstraints();
        moveCount.setPercentWidth(20);
        movesGrid.getColumnConstraints().add(moveCount);
        for (int j = 0; j < 2; j++){
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(40);

            movesGrid.getColumnConstraints().add(cc);
        }

        // Populate rows with moves
        for (int k = 0; k < algebraicMoves.size(); k++){
            int row = k/2;
            int col = k % 2 + 1; // + 1 to skip the first move count column

            Label temp = new Label(algebraicMoves.get(k));
            moveLabels.add(temp); 
            movesGrid.add(temp, col, row);
        }

        // Align the move count labels to the center
        for (int l = 0; l < numRows; l++){
            Label temp = new Label(l + 1 + ". ");
            GridPane.setHalignment(temp, HPos.CENTER);

            movesGrid.add(temp, 0, l);
        }

        // Ensure no growing for labels
        for (Node n : movesGrid.getChildren()){
            GridPane.setHgrow(n, Priority.NEVER);
            GridPane.setVgrow(n, Priority.NEVER);
        }

        movesPane.setFitToWidth(true);
        movesPane.setFitToHeight(false);
        this.movesPane.setContent(movesGrid);
        
    }

    private void drawBoard(){
        Background bkgrnd1;
        Background bkgrnd2;

        if (this.whitePlayer){
            bkgrnd1 = this.DARK_BACKGROUND;
            bkgrnd2 = this.LIGHT_BACKGROUND;
        }
        else {
            bkgrnd1 = this.LIGHT_BACKGROUND;
            bkgrnd2 = this.DARK_BACKGROUND;
        }

        // Draw squares accordingly
        for (int j = 0; j < this.allSquares.size(); j++){
            int row = j/8;
            int col = j % 8;

            if ((row + col) % 2 == 1){
                this.allSquares.get(j).setBackground(bkgrnd1);
            }
            else {
                this.allSquares.get(j).setBackground(bkgrnd2);
            }

        }

        // Label files and ranks
        for (int i = 0; i < fileLabels.length; i++){
            Text txt = new Text(files[i]);
            txt.setFill(Color.GREY);
            txt.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
            fileLabels[i].getChildren().add(txt);
        }

        for (int i = 0; i < rankLabels.length; i++){
            Text txt = new Text(String.valueOf(i + 1));
            txt.setFill(Color.GREY);
            txt.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
            rankLabels[i].getChildren().add(txt);
        }
    }

    private void updateBoardPieces(){
        int[][] board = this.replayEngine.getBoard();
        int indexOffset;

        // Determine indexOffset dependent on whether the board is from whites or blacks perspective
        if (this.whitePlayer){
            indexOffset = 0;
        }
        else {
            indexOffset = 63;
        }

        // Iterate through all squares 0 --> 63
        for (int i = 0; i < 64; i++){
            int j = Math.abs(indexOffset - i);
            int row = j / 8;
            int file = j % 8;

            int boardOcc = board[row][file];
            int sceneOcc;

            StackPane square = allSquares.get(j);
            if (square.getChildren().size() == 0){
                sceneOcc = 0;
            }
            else {
                GUIPiece temp = (GUIPiece) square.getChildren().get(0);
                sceneOcc = temp.getPieceInt();
            }

            if (boardOcc == 0 && sceneOcc == 0){
                continue;
            }
            else if (boardOcc != 0 && sceneOcc == 0){
                square.getChildren().add(new GUIPiece(boardOcc, squareDim));
            }
            else if (boardOcc == 0 && sceneOcc != 0){
                square.getChildren().clear();
            }
            else {
                if (boardOcc != sceneOcc){
                    square.getChildren().clear();
                    square.getChildren().add(new GUIPiece(boardOcc, squareDim));
                }
            }
        }
    }

    private void keyPressHandler(KeyEvent event){
        KeyCode keyCode = event.getCode();

        // Unhighlight the previous algebraic move, before iterating
        int ind;
        if ((ind = replayEngine.getPointerIndex()) >= 0){
            moveLabels.get(ind).setBackground(Background.EMPTY);
        }

        boolean forwardIteration = true;
        // Progress game by one move
        if (keyCode.equals(KeyCode.RIGHT)){
            this.replayEngine.forward();
        }
        // Reverse by one move
        else if (keyCode.equals(KeyCode.LEFT)){
            this.replayEngine.back();
            forwardIteration = false;
        }
        
        // Now ind is increment/decremented
        if ((ind = replayEngine.getPointerIndex()) >= 0){

            Label thisLabel = moveLabels.get(ind);
            thisLabel.setBackground(MOVE_BACKGROUND);

            if (!nodeInBounds(moveLabels.get(ind), movesPane)){
                Bounds labelScenePt = thisLabel.localToScene(thisLabel.getLayoutBounds());
                double labelY = forwardIteration ? labelScenePt.getMaxY() : labelScenePt.getMinY();

                Bounds scrollBounds = movesPane.localToScene(movesPane.getLayoutBounds());
                double scrollY = forwardIteration ? scrollBounds.getMaxY() : scrollBounds.getMinY();

                // Will be negative if the label is higher than the scroll box, positive if label is below
                double pixelDiff = labelY - scrollY;
                double scaleDiff = pixelDiff / scrollBounds.getHeight();

                movesPane.setVvalue(movesPane.getVvalue() + scaleDiff);
            }

            
            // movesPane.layout();
            // final int lambdaInd = ind; // Duplicate for lambda
            // Platform.runLater(
            //     () -> {
            //         System.out.println("Label in viewport: " + nodeInBounds(moveLabels.get(lambdaInd), movesPane));
            //     }
            // );
        }
        // Highlight new algebraic move according to pointer

        restoreSquareColor();
        highlightLastMove();
        updateBoardPieces();
        event.consume();

    }

    public boolean nodeInBounds(Node node, ScrollPane pane){
        Bounds paneBounds = pane.localToScene(pane.getLayoutBounds());
        Bounds nodeBounds = node.localToScene(node.getBoundsInLocal());
        Point2D nodePoint = new Point2D(nodeBounds.getMaxX(), nodeBounds.getMaxY()); // Node was already converted to scene

        return paneBounds.contains(nodePoint);
    }
   

    private void highlightLastMove(){
        Move lastMove = this.replayEngine.getLastMove();
        
        if (lastMove == null) return;

        int[] indices = new int[]{lastMove.getOriginBit(), lastMove.getDestBit()};

        Background bkgrnd1;
        Background bkgrnd2;

        if (this.whitePlayer){
            bkgrnd1 = this.HIGH_DARK_BACKGROUND;
            bkgrnd2 = this.HIGH_LIGHT_BACKGROUND;
        }
        else {
            bkgrnd1 = this.HIGH_LIGHT_BACKGROUND;
            bkgrnd2 = this.HIGH_DARK_BACKGROUND;
        }


        for (int sq : indices){
            int row = sq / 8;
            int col = sq % 8;

            if ((row + col) % 2 == 1){
                this.allSquares.get(sq).setBackground(bkgrnd1);
            }
            else {
                this.allSquares.get(sq).setBackground(bkgrnd2);
            }

            this.highlightedSquares.add(this.allSquares.get(sq));

        }
    }

    private void restoreSquareColor(){
        if (this.highlightedSquares.size() > 0){

            Background bkgrnd1;
            Background bkgrnd2;

            if (this.whitePlayer){
                bkgrnd1 = this.DARK_BACKGROUND;
                bkgrnd2 = this.LIGHT_BACKGROUND;
            }
            else {
                bkgrnd1 = this.LIGHT_BACKGROUND;
                bkgrnd2 = this.DARK_BACKGROUND;
            }



            for (StackPane sq : this.highlightedSquares){
                int row = this.allSquares.indexOf(sq) / 8;
                int col = this.allSquares.indexOf(sq) % 8;

                if ((row + col) % 2 == 1){
                    sq.setBackground(bkgrnd1);
                }
                else {
                    sq.setBackground(bkgrnd2);
                }    
            }
            
            this.highlightedSquares.clear();
        }
    }
    

    public void setReplayEngine(ReplayEngine rpe){
        this.replayEngine = rpe;
    }

    public void cleanup(){};
}
