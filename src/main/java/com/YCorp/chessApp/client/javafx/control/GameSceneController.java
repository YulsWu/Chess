package com.YCorp.chessApp.client.javafx.control;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.event.HyperlinkEvent.EventType;

import com.YCorp.chessApp.client.engine.callback.TickCall;
import com.YCorp.chessApp.client.engine.callback.TimeoutCall;
import com.YCorp.chessApp.client.javafx.classes.GUIEngine;
import com.YCorp.chessApp.client.javafx.classes.GUIPiece;
import com.YCorp.chessApp.client.javafx.classes.interfaces.Closeable;
import com.YCorp.chessApp.client.javafx.events.SceneTransitionEvent;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.application.Platform;


public class GameSceneController implements TickCall, TimeoutCall, Closeable{
    //#region FXML elements
    @FXML
    Pane gamePane; // Root
    @FXML
    GridPane boardPane;
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
    //#endregion
    
    // Nonfxml nodes
    private Pane gameOverCover;
    private Label gameOverLabel;
    private Button gameOverRestartButton;
    private Button gameOverExitButton;

    private Button inGameRestartButton;
    private Button inGameExitButton;


    // Class members

    private final Background SELECTION_BACKGROUND = new Background(new BackgroundFill[]{new BackgroundFill(Color.web("#FF7676"), null, null)});
    private final Background LIGHT_BACKGROUND = new Background(new BackgroundFill[]{new BackgroundFill(Color.web("#F0D9B5"), null, null)});
    private final Background DARK_BACKGROUND = new Background(new BackgroundFill[]{new BackgroundFill(Color.web("#B58863"), null, null)});
    private final Background HIGH_LIGHT_BACKGROUND = new Background(new BackgroundFill[]{new BackgroundFill(Color.web("#F4E288"), null, null)});
    private final Background HIGH_DARK_BACKGROUND = new Background(new BackgroundFill[]{new BackgroundFill(Color.web("#C7A64A"), null, null)});

    private Scene scene;
    private GUIEngine guiEngine;

    // 
    private TextFlow[] rankLabels;
    private TextFlow[] fileLabels;
    private String[] files = new String[]{"a", "b", "c", "d", "e", "f", "g", "h"};
    
    private double squareDim;
    private double dragOffsetX = -1;
    private double dragOffsetY = -1;
    
    private double[] cellBorders = new double[8];
    
    // Order depending on white vs black perspective
    private ArrayList<StackPane> allSquares;
    private boolean whitePlayer;


    // Currently selected piece + associated square
    private StackPane selectedSquare = null;
    private GUIPiece selectedPiece = null;

    // Squares of the last move
    private StackPane lastMoveOrigin = null;
    private StackPane lastMoveDestination = null;

    private boolean clearSelectionsOnRelease = false;

    // Player info
    private String player;
    private String playerElo;
    private String opponent;
    private String opponentElo;

    // // Restart trigger
    // SimpleBooleanProperty restart = new SimpleBooleanProperty(false);
    // public SimpleBooleanProperty restartProperty() {return restart;};

    // Dummy node
    Pane dummy = new Pane();

    
    // Init StackPane squares, labels, arrange squares in array, set labels, depending on player perspective, eventNode, event handlers
    // Sets initial time on clocks if they exist
    public void init(boolean whitePerspective){
        // Migrated from initialize()

        squareDim = boardPane.getPrefHeight()/8;

        double cellDim = boardPane.getPrefHeight() / 8;

        for (int i = 1; i <= 8; i++){
            cellBorders[i - 1] = i * cellDim;
        }


        if (this.guiEngine == null){
            System.out.println("initBoard(): No GUIEngine assigned to controller");
            return;
        }
        
    

        this.player = "Player";
        this.playerElo = "1";
        this.opponent = "Opponent";
        this.opponentElo = "9999";

        playerTextFlow.getChildren().add(new Text(this.player));
        playerTextFlow.getChildren().add(new Text("\n" + this.playerElo));
        opponentTextFlow.getChildren().add(new Text(this.opponent));
        opponentTextFlow.getChildren().add(new Text("\n" + this.opponentElo));

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
        
        // Migrate node element initialization to separate method and invoke here?
        // Create in-game buttons first so they're rendered behind the game over screens
        
        inGameRestartButton = new Button("Restart");
        gamePane.getChildren().add(inGameRestartButton);
        inGameRestartButton.setAlignment(Pos.CENTER);
        inGameRestartButton.setPrefSize(100, 25);
        inGameRestartButton.setLayoutX(gamePane.getPrefWidth()/8 - inGameRestartButton.getPrefWidth()/2);
        inGameRestartButton.setLayoutY(3*gamePane.getPrefHeight()/4);
        inGameRestartButton.addEventHandler(ActionEvent.ACTION, this::restartButtonHandler);

        inGameExitButton = new Button("Exit");
        gamePane.getChildren().add(inGameExitButton);
        inGameExitButton.setAlignment(Pos.CENTER);
        inGameExitButton.setPrefSize(100, 25);
        inGameExitButton.setLayoutX(gamePane.getPrefWidth()/8 - inGameExitButton.getPrefWidth()/2);
        inGameExitButton.setLayoutY(3 * gamePane.getPrefHeight()/4 + inGameRestartButton.getPrefHeight());
        inGameExitButton.addEventHandler(ActionEvent.ACTION, this::exitButtonHandler);


        // Format game over screen
        // Format cover
        gameOverCover = new Pane();
        gamePane.getChildren().add(gameOverCover);
        // Set dims equal to parent
        gameOverCover.setPrefWidth(gamePane.getPrefWidth());
        gameOverCover.setPrefHeight(gamePane.getPrefHeight());
        // Cover should cover all of parent
        gameOverCover.setLayoutX(0);
        gameOverCover.setLayoutY(0);
        // Grey slightly transparent background, blocking further interaction with board
        gameOverCover.setBackground(new Background(new BackgroundFill(Color.web("#808080", 0.8), CornerRadii.EMPTY, Insets.EMPTY)));
        // Invisible until end of game, settings affect children
        gameOverCover.setVisible(false);
        gameOverCover.setMouseTransparent(true);
        gameOverCover.toFront();

        // Format the game over 'box' where the label will reside
        //gameOverBox = new Pane();
        gameOverLabel = new Label();
        gameOverLabel.setAlignment(Pos.CENTER);
        gameOverCover.getChildren().add(gameOverLabel);
        // Box should be 1/3 dim of parent, centered vertically and horizontally
        gameOverLabel.setPrefWidth(gameOverCover.getPrefWidth()/3);
        gameOverLabel.setPrefHeight(gameOverCover.getPrefHeight()/3);
        gameOverLabel.setLayoutX(gameOverCover.getPrefWidth()/2 - gameOverLabel.getPrefWidth()/2);
        gameOverLabel.setLayoutY(gameOverCover.getPrefHeight()/2 - gameOverLabel.getPrefHeight()/2);
        // Fully opaque box, dark background
        gameOverLabel.setBackground(this.DARK_BACKGROUND); // or set text file
        
        gameOverLabel.setText("Get gud");
        gameOverLabel.toFront();

        gameOverExitButton = new Button("Exit");
        gameOverExitButton.setAlignment(Pos.CENTER);
        
        gameOverRestartButton = new Button("Restart");
        gameOverRestartButton.setAlignment(Pos.CENTER);

        gameOverExitButton.setBackground(this.HIGH_DARK_BACKGROUND);
        gameOverRestartButton.setBackground(this.HIGH_DARK_BACKGROUND);

        gameOverCover.getChildren().addAll(gameOverExitButton, gameOverRestartButton);

        for (Button b : new Button[]{gameOverExitButton, gameOverRestartButton}){
            b.setPrefSize(150, 75);
        }

        gameOverRestartButton.setLayoutX(gameOverLabel.getLayoutX());
        gameOverRestartButton.setLayoutY(gameOverLabel.getLayoutY() + gameOverLabel.getPrefHeight() + 10);

        gameOverExitButton.setLayoutX(gameOverLabel.getLayoutX() + gameOverLabel.getPrefWidth() - gameOverExitButton.getPrefWidth());
        gameOverExitButton.setLayoutY(gameOverLabel.getLayoutY() + gameOverLabel.getPrefHeight() + 10);

        gameOverRestartButton.setOnAction(this::restartButtonHandler);
        gameOverExitButton.setOnAction(this::exitButtonHandler);

        updateBoardPieces();

        // Include dummy node in scene graph so events fired from it can be captured by stage
        gamePane.getChildren().add(dummy);
        dummy.setVisible(false);
        dummy.setMouseTransparent(true);

        // Draw light and dark squares
        drawBoard();

        // Attach handlers to squares
        for (StackPane sq : allSquares){
            sq.setOnMousePressed(this::mousePressHandler);
            sq.setOnMouseDragged(this::mouseDragHandler);
            sq.setOnMouseReleased(this::mouseReleaseHandler);
        }

        scene = gamePane.getScene();
        // Add Event filter to catch right click before it propagates down, for cancelling moves
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.SECONDARY){
                clearSelections();
            }
        });

        updateInfo();

        // Set visibility and initial values on player clocks, depending on if its a timed game
        if (this.guiEngine.isTimed()){
            this.playerClock.setText(formatTimeMillis(this.guiEngine.getTimeMillis(true)));
            this.opponentClock.setText(formatTimeMillis(this.guiEngine.getTimeMillis(false)));
        }
        else {
            this.playerClock.setVisible(false);
            this.opponentClock.setVisible(false);
        }

        // Force a layout pass before stage.show() to prevent pop-in UI elements
        gamePane.layout();
    }

    //#region GUI

    // Mouse press is initiated during a drag and click
    // NO SELECTION or SAME SQUARE: Click will select the piece/square, and snap to mouse, works for both piece selection and piece dragging
    // SELECTION: If we click with a piece selection on a different square, we attempt to make the move:
    //      If valid, move is processed
    //      If invalid, piece snaps back to original square
    private void mousePressHandler(MouseEvent e){
        if (e.getButton() == MouseButton.PRIMARY){

            StackPane square = (StackPane) e.getTarget();

            // Have selections
            if (this.selectedPiece != null && this.selectedSquare != null){

                // Select same square
                if (this.selectedSquare == square){
                    snapPieceToMouse(this.selectedPiece, e);
                }
                // Different square, attempt MOVE
                else {
                    int origin = this.allSquares.indexOf(this.selectedSquare);
                    int destination = this.allSquares.indexOf(square);
                    
                    // If move is valid, play the move, highlight the squares that were just played, and update board info
                    int endCode = this.guiEngine.attemptMove(new int[]{origin, destination});
                    if (endCode >= 0){
                        movePiece(square, this.selectedPiece, true);
                        highlightMove(origin, destination);
                        updateInfo();
                        updateClock();

                        if (endCode > 0){
                            endGame(endCode);
                            return;
                        }
                    }
                    // If not valid move, snap piece back to origin and clear selections
                    else {
                        movePiece(this.selectedSquare, this.selectedPiece, true);
                    }
                    this.clearSelectionsOnRelease = false;
                }
            }
            // NO selections
            else {
                // Square is occupied
                if (square.getChildren().size() == 1){
                    setSelections(square, (GUIPiece) square.getChildren().get(0));
                    snapPieceToMouse(this.selectedPiece, e);
                }
                else {
                }
            }
        }
    }

    // Just drags the selected piece, ensuring it is an anchorpane child
    private void mouseDragHandler(MouseEvent e){
        // If the selected piece
        if (this.selectedPiece != null && anchorPane.getChildren().contains(this.selectedPiece)){
            GUIPiece piece = this.selectedPiece;
            
            piece.setLayoutX(e.getSceneX() - this.dragOffsetX);
            piece.setLayoutY(e.getSceneY() - this.dragOffsetY);
        }
    }

    private void mouseReleaseHandler(MouseEvent e){
        // Left mouse button
        if (e.getButton() == MouseButton.PRIMARY){
            // Have selections
            if (this.selectedPiece != null && this.selectedSquare != null){

                Point2D temp = boardPane.sceneToLocal(e.getSceneX(), e.getSceneY());
                int newSquareInd = gridCoordToSquareIndex(temp);

                // Releasing over valid square
                if (newSquareInd >= 0){
                    StackPane square = allSquares.get(newSquareInd);

                    // If we click and release over the same square, we want to 'select' that piece and square
                    // So we use a flag, for the first click+release we keep the selections. For the second we clear the selections
                    if (this.selectedSquare == square){
    
                        if (this.clearSelectionsOnRelease){
                            movePiece(this.selectedSquare, this.selectedPiece, true);
                            this.clearSelectionsOnRelease = false;
                        }
                        else {
                            movePiece(this.selectedSquare, this.selectedPiece, false);
                            this.clearSelectionsOnRelease = true;
                        }
                    }

                    // Releasing over different square/MAKE MOVE
                    // If we release over a different square WITH selections, it means we want to make the move
                    // Attempt the move, if valid then execute, if invalid, snap piece back to origin

                    else {
                        int origin = this.allSquares.indexOf(this.selectedSquare);

                        // if valid move, execute move on to new square
                        int endCode = this.guiEngine.attemptMove(new int[]{origin, newSquareInd});
                        if (endCode >= 0){
                            movePiece(square, this.selectedPiece, true);
                            highlightMove(origin, newSquareInd);
                            updateInfo();
                            updateClock();

                            if (endCode > 0){
                                endGame(endCode);
                                return;
                            }

                        }
                        // if invalid move snap back to original square
                        else {
                            movePiece(this.selectedSquare, this.selectedPiece, true);
                        }
                        this.clearSelectionsOnRelease = false; // After an attempted move the selection/deselection cycle is reset, probably can move this line outside of the ifs
                    }
                }
                // Release over invalid square - Snap back to original square, clear selections
                else{
                    movePiece(this.selectedSquare, this.selectedPiece, true);
                    this.clearSelectionsOnRelease = false;
                }
                
            }
        }
    }

    // public void onPiecePressed(MouseEvent e){
    //     // Left click on Piece
    //     if (e.getTarget() instanceof GUIPiece && e.getButton() == MouseButton.PRIMARY){
    //         GUIPiece piece = (GUIPiece) e.getTarget();
    //         if (piece.getParent() instanceof StackPane){
    //             StackPane square = (StackPane) piece.getParent();          
    //             if (this.selectedPiece != null && this.selectedSquare != null){
    //                 int origin = this.allSquares.indexOf(this.selectedSquare);
    //                 int destination = this.allSquares.indexOf(square);
    //                 if (this.guiEngine.attemptMove(new int[]{origin, destination}) >= 0){
    //                     movePiece(square, piece, true);
    //                     highlightMove(origin, destination);
    //                     updateInfo();
    //                 };
    //             }
    //             else {
    //                 setSelections(square, piece);
    //                 // Relative to Stackpane                  
    //                 this.anchorPane.getChildren().add(piece);     
    //                 Point2D anchorPoint = anchorPane.sceneToLocal(e.getSceneX(), e.getSceneY());
    //                 piece.setLayoutX(anchorPoint.getX() - piece.getFitWidth()/2);
    //                 piece.setLayoutY(anchorPoint.getY() - piece.getFitHeight()/2);
    //                 this.dragOffsetX = e.getSceneX() - piece.getLayoutX();
    //                 this.dragOffsetY = e.getSceneY() - piece.getLayoutY();
    //             }
    //     }
    //     }
    // }

    // public void onPieceDragged(MouseEvent e){
    //     if (e.getTarget() instanceof ImageView && e.getButton() == MouseButton.PRIMARY){
    //         ImageView piece = (ImageView) e.getTarget();
    //         // This method transforms coordinates each event
    //         // Point2D localPoint = anchorPane.sceneToLocal(e.getSceneX(), e.getSceneY());
    //         // piece.setLayoutX(localPoint.getX() - piece.getFitWidth()/2);
    //         // piece.setLayoutY(localPoint.getY() - piece.getFitHeight()/2);
    //         // More lightweight method based on the offset between two coordinate systems
    //         piece.setLayoutX(e.getSceneX() - this.dragOffsetX);
    //         piece.setLayoutY(e.getSceneY() - this.dragOffsetY);
    //         //System.out.println("Piece Dragged");
    //     }
    // }

    // public void onPieceReleased(MouseEvent e){
    //     if (e.getTarget() instanceof GUIPiece && e.getButton() == MouseButton.PRIMARY){
    //         GUIPiece piece = (GUIPiece) e.getTarget();
    //         // Y is inverted
    //         Point2D temp = boardPane.sceneToLocal(e.getSceneX(), e.getSceneY());
    //         int newSquare = gridCoordToSquareIndex(temp);
    //         // If over valid square
    //         if (newSquare >= 0){
    //             StackPane square = allSquares.get(newSquare);
    //             // If different square as the selected square
    //             if (square != this.selectedSquare){
    //                 int origin = Arrays.asList(this.allSquares).indexOf(this.selectedSquare);
    //                 int destination = newSquare;
    //                 // Attempt move on Board
    //                 if (this.guiEngine.attemptMove(new int[]{origin, destination}) >= 0){
    //                     movePiece(square, piece, true);
    //                     highlightMove(origin, destination);
    //                 }
    //                 else {
    //                     movePiece(this.selectedSquare, piece, true);
    //                 }
    //             }
    //             else {
    //                 movePiece(this.selectedSquare, piece, true);
    //             }
    //         }
    //         else {
    //             movePiece(this.selectedSquare, piece, true);
    //         }
    //     }
    // }

    // public void onPieceClicked(MouseEvent e){
    //     if (e.getTarget() instanceof ImageView){
    //         System.out.println("Piece clicked");
    //         this.selectedPiece = (ImageView) e.getTarget();
    //         this.selectedSquare = (StackPane) this.selectedPiece.getParent();
    //     }
    //     else if (e.getTarget() instanceof StackPane){
    //         this.selectedSquare = (StackPane) e.getTarget();
    //         if (this.selectedSquare.getChildren().size() > 0){
    //             this.selectedPiece = (ImageView) this.selectedSquare.getChildren().get(0);
    //         }
    //         else {
    //             clearSelections();
    //         }
    //     }
    // }

    // public void onSquareClicked(MouseEvent e){
    //     // If clicking on a square with left click
    //     if (e.getTarget() instanceof StackPane && e.getButton() == MouseButton.PRIMARY){
    //         System.out.println("Square clicked");
    //         StackPane square = (StackPane) e.getTarget();
    //         // Clicking on a DIFFERENT Square with a selected piece/square
    //         if (this.selectedPiece != null && this.selectedSquare != square){
    //             int origin = Arrays.asList(this.allSquares).indexOf(this.selectedSquare);
    //             int destination = Arrays.asList(this.allSquares).indexOf(square);
    //             if (this.guiEngine.attemptMove(new int[]{origin, destination}) >= 0){
    //                 System.out.println("onSquareClicked(): Good move");
    //                 this.updateBoardPieces();
    //                 // if (square.getChildren().size() == 0){
    //                 //     square.getChildren().add(this.selectedPiece);
    //                 //     this.selectedPiece.setLayoutX(square.getWidth()/2 - this.selectedPiece.getFitWidth()/2);
    //                 //     this.selectedPiece.setLayoutY(square.getHeight()/2 - this.selectedPiece.getFitHeight()/2);
    //                 // }
    //                 // else if (square.getChildren().size() == 1){
    //                 //     square.getChildren().clear();
    //                 //     square.getChildren().add(this.selectedPiece);
    //                 // }
    //             }
    //             clearSelections();
    //         }
    //         // Clicking on square with no selections
    //         else {
    //             if (square.getChildren().size() == 1){
    //                 setSelections(square, (GUIPiece) square.getChildren().get(0));
    //             }
    //         }        
    //     }
    // }

    private void clearSelections(){
        if (this.selectedSquare != null && this.selectedPiece != null){
            restoreSquareBackground(this.selectedSquare);
            movePiece(this.selectedSquare, this.selectedPiece, false);
            this.clearSelectionsOnRelease = false;

            this.selectedPiece = null;
            this.selectedSquare = null;
        }
    }

    private void setSelections(StackPane square, GUIPiece piece){
        this.selectedPiece = piece;
        this.selectedSquare = square;

        square.setBackground(this.SELECTION_BACKGROUND);
    }

    private boolean isLightSquare(int squareInd){
        int row = squareInd/8;
        int col = squareInd%8;

        boolean temp = (row + col) % 2 == 1 ? false : true;     
        return this.whitePlayer ? temp : !temp;
    }

    private boolean isLightSquare(StackPane square){
        return isLightSquare(this.allSquares.indexOf(square));
    }

    private void movePiece(StackPane destination, GUIPiece piece, boolean clear){
        // Clear and set destination square
        destination.getChildren().clear();
        destination.getChildren().add(piece);

        // Align piece in new square (stackpane)
        piece.setLayoutX(destination.getWidth() - piece.getFitWidth()/2);
        piece.setLayoutY(destination.getHeight() - piece.getFitHeight()/2);


        if (clear){
            this.clearSelections();
        }

        // Update GUI display to reflect played move
        this.updateBoardPieces();
    }

    private void restoreSquareBackground(StackPane square){
        if (isLightSquare(square)){
            square.setBackground(this.LIGHT_BACKGROUND);
        }
        else {
            square.setBackground(this.DARK_BACKGROUND);
        }
    }

    private void highlightMove(int orig, int dest){
        if (this.lastMoveDestination != null && this.lastMoveOrigin != null){
            restoreSquareBackground(this.lastMoveDestination);
            restoreSquareBackground(this.lastMoveOrigin);
        }

        // Set "Last move" squares
        this.lastMoveOrigin = this.allSquares.get(orig);
        this.lastMoveDestination = this.allSquares.get(dest);

        // Highlight last move squares
        if (isLightSquare(this.lastMoveOrigin)){
            this.lastMoveOrigin.setBackground(this.HIGH_LIGHT_BACKGROUND);
        }
        else {
            this.lastMoveOrigin.setBackground(this.HIGH_DARK_BACKGROUND);
        }

        if (isLightSquare(this.lastMoveDestination)){
            this.lastMoveDestination.setBackground(this.HIGH_LIGHT_BACKGROUND);
        }
        else {
            this.lastMoveDestination.setBackground(this.HIGH_DARK_BACKGROUND);
        }
    
    }

    private void snapPieceToMouse(GUIPiece piece, MouseEvent e){
        if (!this.anchorPane.getChildren().contains(piece)){
            this.anchorPane.getChildren().add(piece);
        }

        Point2D anchorPoint = anchorPane.sceneToLocal(e.getSceneX(), e.getSceneY());
        piece.setLayoutX(anchorPoint.getX() - piece.getFitWidth()/2);
        piece.setLayoutY(anchorPoint.getY() - piece.getFitHeight()/2);

        this.dragOffsetX = e.getSceneX() - piece.getLayoutX();
        this.dragOffsetY = e.getSceneY() - piece.getLayoutY();
    }

    // Displays end game screen, win/lose/draw, method
    // Run this BEFORE toggling turn (if white checkmates, turn never toggles to black since game is over)
    // <ol>
    // * <li> 0 = No game end condition, game continues</li>
    // * <li> 1 = Checkmate </li>
    // * <li> 2 = Stalemate draw </li>
    // * <li> 3 = Insufficient material draw </li>
    // * <li> 4 = 75 move rule draw </li>
    // * <li> 5 = 5-fold repeat draw </li>
    // 6 - additional state not from evaluateGameEndConditions(), used for timeout callback from ChessClock
    private void endGame(int endCode){
        StringBuilder sb = new StringBuilder();
    
        switch (endCode){
            case 1:
                // Checkmate endgame
                // When checkmate occurs, the next set of valid moves is required to determine this (no valid moves + in check = checkmate)
                // Therefore the isWhitesTurn() returns the losing player instead, account for this here
                sb.append(this.guiEngine.isWhitesTurn() ? "Black" : "White");
                sb.append(" wins by checkmate!");
                break;
            case 2: 
                sb.append("Draw by Stalemate!");
                break;
            case 3:
                sb.append("Draw by Insufficient Material!");
                break;
            case 4:
                sb.append("Draw by 75-move rule!");
                break;
            case 5:
                sb.append("Draw by 5-fold repetition!");
                break;
            case 6: 
                // Player whose turn it is during timeout LOSES, so opposite player wins
                // Win condition is more simple here vs checkmate, the turn never toggles over so the player whose turn it was during the timeout
                // loses on time
                sb.append(this.guiEngine.isWhitesTurn() ? "Black" : "White");
                sb.append(" wins on time!");
                break;
        }

        this.gameOverLabel.setText(sb.toString());

        this.gameOverLabel.setVisible(true);
        this.gameOverLabel.setMouseTransparent(false);
        this.gameOverCover.setVisible(true);
        this.gameOverCover.setMouseTransparent(false);
    }
    
    private void exitButtonHandler(ActionEvent e){
        this.dummy.fireEvent(new SceneTransitionEvent(SceneTransitionEvent.TO_MENU));
    }

    private void restartButtonHandler(ActionEvent e){
        restart();
    }

    private void restart(){
        // No-settings constructor for SceneTransitionEvent
        this.dummy.fireEvent(new SceneTransitionEvent(SceneTransitionEvent.TO_GAME));
    }
    //#endregion

    //#region Utility

    public int gridCoordToSquareIndex(double x, double y){
        int row = -1;
        int col = -1;

        // Javafx coordinates have Y ascending from top to bottom, inverting this to fit with board square encoding better
        y = boardPane.getPrefHeight() - y;

        //System.out.println("[GTI]: Detected (" + x + ", " + y + ")");

        if ((x > 0 && y > 0) && 
            (x < boardPane.getPrefWidth() && y < boardPane.getPrefHeight()))
        {
            for (int i = 0; i < cellBorders.length; i++){
                if (y < cellBorders[i]){
                    row = i;
                    break;
                }
            }

            for (int j = 0; j < cellBorders.length; j++){
                if (x < cellBorders[j]){
                    col = j;
                    break;
                }
            }

            if (row >= 0 && col >= 0){
                int temp = (row * 8) + col;
                if (this.whitePlayer){
                    return temp;
                }
                else {
                    return 63 - temp;
                }
            }
            else {
                //System.out.println("[GTI] Error: Row or Col was never set");
                return -1;
            }


        }
        else {
            //System.out.println("[GTI] Error: Provided coordinates not in bounds");
            return -1;
        }
    }

    public int gridCoordToSquareIndex(Point2D pt){
        return gridCoordToSquareIndex(pt.getX(), pt.getY());
    }

    public void setGUIEngine(GUIEngine guiEngine){
        this.guiEngine = guiEngine;
    }

    //#endregion

    //#region Re/Drawing
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
    // Update board based on Board object
    // DEBUG INFO
    private void updateInfo(){
        this.infoTextFlow.getChildren().clear();

        for (String[] strArr : this.guiEngine.getStatus()){
            this.infoTextFlow.getChildren().add(new Text(strArr[0] + ": "));
            this.infoTextFlow.getChildren().add(new Text(strArr[1] + "\n"));
        }
    }
    
    private void updateBoardPieces(){
        int[][] board = this.guiEngine.getBoard();
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

    private void updateClock(){
        if (this.whitePlayer){
            this.playerClock.setText(this.formatTimeMillis(this.guiEngine.getTimeMillis(true)));
            this.opponentClock.setText(this.formatTimeMillis(this.guiEngine.getTimeMillis(false)));
        }
        else {
            this.playerClock.setText(this.formatTimeMillis(this.guiEngine.getTimeMillis(false)));
            this.opponentClock.setText(this.formatTimeMillis(this.guiEngine.getTimeMillis(true)));
        }
    }
    //#endregion
    
    //#region NonFXThreads
    public void tickCall(boolean whitePlayer, long ms){
        Label clock = (this.whitePlayer == whitePlayer) ? playerClock : opponentClock;
        
        // Requred to run platform.runLater() as this method would be called by non-FX threads
        Platform.runLater(() -> { 
                clock.setText(formatTimeMillis(ms));
            }
        );
    }

    // Callback from ChessClock signalling a timeout. Calls endGame(6) through Platform.runLater() since it affects nodes in scene graph
    public void timeoutCall(boolean whitePlayer){
        Platform.runLater(() -> {
            endGame(6);
        });
    }

    private String formatTimeMillis(long ms){
        int totalSeconds = (int)ms/1000;
        int hours = totalSeconds/3600;
        totalSeconds %= 3600;
        int minutes = totalSeconds/60;
        totalSeconds %= 60;


        return String.format("%02d:%02d:%02d", hours, minutes, totalSeconds);
    }

    // Essentally propagates ON_WINDOW_CLOSE to back-end non-FX threads so they can shutdown gracefully, with no hanging threads
    public void cleanup(){
        this.guiEngine.cleanup();
    }
    //#endregion
}
