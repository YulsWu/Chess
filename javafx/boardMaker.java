package javafx;

import engine.Board;

import javafx.application.Application;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.Label;
import javafx.geometry.Insets;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.RadioButton;

import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.scene.text.TextAlignment;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import javafx.scene.text.TextAlignment;

import javafx.geometry.Pos;

import javafx.application.Platform;
import javafx.event.Event;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;


<<<<<<< HEAD
public class boardMaker extends Application{
=======
public class BoardMaker extends Application{
    ArrayList<int[][]> returnPieceboards = new ArrayList<>();
    ArrayList<Long> returnBitboards = new ArrayList<>();
    ArrayList<Button> boardButtons;
    ArrayList<Button> menuButtons;

    Button doneButton;
    Button resetButton;
    Button addButton;

    ToggleGroup bitToggleGroup;;
    RadioButton bitboardToggle;
    RadioButton pieceboardToggle;

    // Non-javafx class variable initialization
    long bitboard = 0L;
    int[][] board = new int[8][8];
    int windowHeight = 400;
    int windowWidth = 600;
    int squareHeightWidth = 50;
    boolean bitboardMode = true;
    int currentPiece = 1;

    private static final Map<String, Integer> PIECE_VALUES = new HashMap<>(){{
        put("wP", 1);
        put("wN", 2);
        put("wB", 3); 
        put("wR", 4);
        put("wQ", 5);
        put("wK", 6);
        put("", 0);
        put("bP", -1);
        put("bN", -2);
        put("bB", -3);
        put("bR", -4);
        put("bQ", -5);
        put("bK", -6);
    }};

    private static final Map<Integer, String> PIECE_NAMES = new HashMap<>(){{
        put(1, "wP");
        put(2, "wN");
        put(3, "wB"); 
        put(4, "wR");
        put(5, "wQ");
        put(6, "wK");
        put(0, "");
        put(-1, "bP");
        put(-2, "bN");
        put(-3, "bB");
        put(-4, "bR");
        put(-5, "bQ");
        put(-6, "bK");
    }};

    public BoardMaker(){
        boardButtons = new ArrayList<Button>();
    }
>>>>>>> 1fe44a71f61078c30834d5891a6eeec711ad199b
    
    @Override
    public void start(Stage primaryStage){
        createBoardButtons();
        createMenuButtons();
        primaryStage.setTitle("Board maker");
        primaryStage.setResizable(false);

        //Gridpane for chessboard layout
        GridPane gridBoard = new GridPane();
        gridBoard.setGridLinesVisible(true);

        gridBoard.setMaxHeight(windowHeight);
        gridBoard.setMinHeight(windowHeight);
        gridBoard.setMaxWidth(windowHeight);
        gridBoard.setMinWidth(windowHeight);
        // Add empty corner labels
        gridBoard.add(new Label(""), 0, 0);
        gridBoard.add(new Label(""), 0, 9);
        gridBoard.add(new Label(""), 9, 0);
        gridBoard.add(new Label(""), 9, 9);


        // Add labels
        String[] files = new String[]{"A","B","C","D","E","F","G","H"};
        for (int j = 0; j < 8; j++){
            Label botFileLabel = new Label(files[j]);
            Label topFileLabel = new Label(files[j]);
            Label leftRankLabel = new Label(String.valueOf(j));
            Label rightRankLabel = new Label(String.valueOf(j));

            botFileLabel.setAlignment(Pos.CENTER);
            topFileLabel.setAlignment(Pos.CENTER);
            leftRankLabel.setAlignment(Pos.CENTER);
            rightRankLabel.setAlignment(Pos.CENTER);

            botFileLabel.setTextAlignment(TextAlignment.CENTER);
            topFileLabel.setTextAlignment(TextAlignment.CENTER);
            leftRankLabel.setTextAlignment(TextAlignment.CENTER);
            rightRankLabel.setTextAlignment(TextAlignment.CENTER);

            gridBoard.add(botFileLabel, j + 1, 0);
            gridBoard.add(topFileLabel, j + 1, 9);
            gridBoard.add(leftRankLabel, 0, 7 - j + 1);
            gridBoard.add(rightRankLabel, 9, 7 - j + 1);
        }
        // Add buttons to gridpane in correct places
        for (int i = 0; i < 64; i++){
            int row = i / 8;
            int col = i % 8;

            Button temp = boardButtons.get(i);
            gridBoard.add(temp, col + 1, (7 - row) + 1);
            GridPane.setHgrow(temp, Priority.ALWAYS);
            GridPane.setVgrow(temp, Priority.ALWAYS);
        }

        GridPane gridMenu = new GridPane();
        gridMenu.setGridLinesVisible(true);

        for (int i = 0; i < 12; i ++){
            int col = i / 6;
            int row = i % 6;

            gridMenu.add(menuButtons.get(i), col, row);
        }
        VBox radioBox = new VBox();
        radioBox.getChildren().addAll(bitboardToggle, pieceboardToggle);

        VBox menuBox = new VBox();
        menuBox.getChildren().addAll(resetButton, addButton, doneButton);

        gridMenu.add(radioBox, 0, 6);
        gridMenu.add(menuBox, 1, 6);



        // Assign borderpane elements
        BorderPane borderPane = new BorderPane();
        borderPane.setTop(null);
        borderPane.setLeft(null);
        borderPane.setBottom(null);
        borderPane.setCenter(gridBoard);
        borderPane.setRight(gridMenu);

        Paint color = Color.BLACK;
        BorderStroke bs = new BorderStroke(color, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(5));
        gridBoard.setBorder(new Border(bs));
        gridMenu.setBorder(new Border(bs));
        


        Scene scene = new Scene(borderPane, windowWidth, windowHeight);
        primaryStage.setScene(scene);

        primaryStage.show();
    }

    public void createBoardButtons(){
        for (int i = 0; i < 64; i++){
            Button temp = new Button();

            // temp.setMinHeight(squareHeightWidth);
            // temp.setMaxHeight(squareHeightWidth);
            // temp.setMinWidth(squareHeightWidth);
            // temp.setMaxWidth(squareHeightWidth);
            temp.setPrefSize((double)squareHeightWidth, (double)squareHeightWidth);
            temp.setPadding(Insets.EMPTY);
            temp.setTextAlignment(TextAlignment.CENTER);
            temp.setText("0");
            temp.setOnMouseClicked(this::boardButtonHandler);

            boardButtons.add(temp);
        }
    }

    public void createMenuButtons(){
        this.bitToggleGroup = new ToggleGroup();
        this.menuButtons = new ArrayList<Button>();
        String[] pieces = new String[] {"wP", "wN", "wB", "wR", "wQ", "wK", "bP", "bN", "bB", "bR", "bQ", "bK"};

        for (String label : pieces){
            Button temp = new Button(label);
            temp.setOnAction(this::menuButtonHandler);
            this.menuButtons.add(temp);
        }

        this.bitboardToggle = new RadioButton("bits");
        this.pieceboardToggle = new RadioButton("pieces");

        bitboardToggle.setToggleGroup(this.bitToggleGroup);
        pieceboardToggle.setToggleGroup(this.bitToggleGroup);

        this.addButton = new Button("add board");
        this.doneButton = new Button("done");
        this.resetButton = new Button("reset");

        this.addButton.setOnAction(this::addButtonHandler);
        this.resetButton.setOnAction(this::resetButtonHandler);
        this.doneButton.setOnAction(this::doneButtonHandler);
        

        pieceboardToggle.setSelected(false);
        bitboardToggle.setSelected(true);

        pieceboardToggle.setOnAction(this::radioHandler);
        bitboardToggle.setOnAction(this::radioHandler);

    }

    public void resetBoard(){
        if (bitboardMode){
            this.bitboard = 0L;
        }
        else {
            this.board = new int[8][8];
        }
    }

    public void toggleSquareMarkers(){
        if (bitboardMode){
            for (int i = 0; i < 8; i++){
                for (int j = 0; j < 8; j++){
                    int ind = (i * 8) + j;
                    this.boardButtons.get(ind).setText(PIECE_NAMES.get(this.board[i][j]));
                }
            }
        }
        else {
            char[] bitArray = Board.longToString(this.bitboard).toCharArray();
            for (int i = 0; i < 64; i++){
                this.boardButtons.get(i).setText(String.valueOf(bitArray[i]));
            }
        }
    }
    
    public void updateBoards(){ 
        if (bitboardMode){
            StringBuilder sb = new StringBuilder();
            String[] labels = new String[64];
            for (int i = 0; i < 64; i++){
                sb.append(this.boardButtons.get(i).getText());
            }
            this.bitboard = Long.parseUnsignedLong(sb.toString(), 2);
        }
        else {
            for (int i = 0; i < 8; i ++){
                for (int j = 0; j < 8; j++){
                    int ind = (i * 8) + j;
                    this.board[i][j] = Integer.valueOf(PIECE_VALUES.get(this.boardButtons.get(ind).getText()));
                }
            }
        }

    }
    
    public String dumpBoards(){
        StringBuilder sb = new StringBuilder();

        sb.append("Bit boards as Longs:\n");
        for (int i = 0; i < this.returnBitboards.size(); i++){
            sb.append("long bitboard" + i + " = 0b");
            sb.append(Board.longToString(this.returnBitboards.get(i)));
            sb.append("L;\n");
            sb.append("int[] bitIndices" + i + " = new int[] ");
            sb.append(Arrays.toString(getBitIndices(this.returnBitboards.get(i))).replace("[", "{").replace("]", "}"));
            sb.append(";\n");
        }
        sb.append("\n");
        sb.append("Piece boards as int[][]\n");

        for (int i = 0; i < this.returnPieceboards.size(); i++){
            sb.append("int[][] pieceboard" + i + " = new int[][] ");
            sb.append(Arrays.deepToString(this.returnPieceboards.get(i)).replace("[", "{").replace("]", "}"));
            sb.append(";\n");
        }

        return sb.toString();
    }

    // Event Handlers
    public void menuButtonHandler(Event event){
        Button button = (event.getSource() instanceof Button) ? (Button)event.getSource() : null;
        
        this.currentPiece = PIECE_VALUES.get(button.getText());
    }

    public void radioHandler(Event event){
        toggleSquareMarkers();
        this.bitboardMode = !this.bitboardMode;
    }

    public void boardButtonHandler(MouseEvent event){
        MouseButton mouseButton = event.getButton();
        Button button = (event.getSource() instanceof Button) ? (Button) event.getSource() : null;
        if (bitboardMode){
            if (mouseButton == MouseButton.PRIMARY){
                button.setText("1");
            }
            else {
                button.setText("0");
            }
        }
        else {
            if (mouseButton == MouseButton.PRIMARY){
                button.setText(PIECE_NAMES.get(this.currentPiece));
            }
            else {
                button.setText("");
            }
        }
        updateBoards();
    }
    
    public void addButtonHandler(Event event){
        if (bitboardMode){
            Board.bitboardVisualize(this.bitboard);
            this.returnBitboards.add(this.bitboard);
            System.out.println("Added bitboard, " + this.returnBitboards.size() + " added so far.");
        }
        else {
            Board.boardVisualize(this.board);
            int[][] clone = new int[8][8];

            //deep clone
            for (int i = 0; i < 8; i++){
                clone[i] = this.board[i].clone();
            }

            this.returnPieceboards.add(clone);
            System.out.println("Added board, " + this.returnPieceboards.size() + " added so far.");
        }
    }

    public void resetButtonHandler(Event event){
        String newLabel = bitboardMode ? "0" : "";
        for (Button b : this.boardButtons){
            b.setText(newLabel);
        }
        updateBoards();
    }

    public void doneButtonHandler(Event event){
        System.out.println(dumpBoards());

        this.board = new int[8][8];
        this.bitboard = 0L;

        this.returnBitboards = new ArrayList<Long>();
        this.returnPieceboards = new ArrayList<int[][]>();

        refreshBoardDisplay();
    }

    public void refreshBoardDisplay(){
        this.bitboardMode = !this.bitboardMode;
        toggleSquareMarkers();
        this.bitboardMode = !this.bitboardMode;
    }

    public Integer[] getBitIndices(long bitboard){
        char[] charArray = Board.longToString(bitboard).toCharArray();
        ArrayList<Integer> indices = new ArrayList<>();
        
        for (int i = 0; i < charArray.length; i++){
            if (charArray[i] == '1') {indices.add(i);}
        }

        return indices.toArray(new Integer[0]);

    }


}
