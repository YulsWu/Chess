package com.YCorp.chessApp.client.javafx.classes;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import com.YCorp.chessApp.client.parser.RegexParser;
import com.YCorp.chessApp.server.db.RegexDatabase;
import com.YCorp.chessApp.server.db.RegexGameData;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class BrowserImportDialog extends Dialog<Boolean> {
    private DialogPane dialogPane;
    private TextField inputField;
    private Button browseButton;
    private Button cancelButton; 
    private int rowsAffected = 0;
    private Stage stage;
    private DatabaseWriteService dbService = new DatabaseWriteService();

    private ObservableList<String> status = FXCollections.observableArrayList();

    public BrowserImportDialog(){
        super();

        dialogPane = this.getDialogPane();
        dialogPane.setPrefSize(600, 350);

        stage = (Stage) dialogPane.getScene().getWindow();
        stage.setOnCloseRequest(event -> {
            if (dbService.isRunning()){
                event.consume();
                dbService.interruptTask();
            }
        });
  

        // Set CONTENT
        String prompt = "Type filepath here...";
        HBox hbox = new HBox();
        browseButton = new Button("Browse");
        browseButton.disableProperty().bind(dbService.runningProperty());
        cancelButton = new Button ("Cancel");
        cancelButton.disableProperty().bind(dbService.runningProperty().not());
        inputField = new TextField();
        inputField.setPromptText(prompt);
        inputField.setPrefWidth(dialogPane.getPrefWidth() * (3.0 / 4.0)); // If calculating this way, must specify 3/4 is DOUBLE
        inputField.disableProperty().bind(dbService.runningProperty());
        
        hbox.getChildren().addAll(inputField, browseButton, cancelButton);
        hbox.setAlignment(Pos.CENTER);

        // VBox wrapper = new VBox(hbox);
        // wrapper.setAlignment(Pos.BOTTOM_CENTER);
        // wrapper.setPrefHeight(dialogPane.getPrefHeight() * (1/4));

        dialogPane.setContent(hbox);
        
        // Set HEADER
        ListView<String> statusView = new ListView<String>(status);
        statusView.setEditable(false);
        statusView.setItems(status);
        statusView.setPrefHeight(dialogPane.getPrefHeight() * (3.0/4.0)); 
        statusView.setPadding(Insets.EMPTY);
        statusView.setFixedCellSize(17);
        statusView.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px;");
        status.add("STATUS: Awaiting input...");
        dialogPane.setHeader(statusView);

        // Set BUTTONS TYPE
        ButtonType importButton = new ButtonType("Import", ButtonBar.ButtonData.LEFT);
        ButtonType exit = new ButtonType("Exit", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialogPane.getButtonTypes().addAll(importButton, exit);
        dialogPane.lookupButton(importButton).addEventFilter(ActionEvent.ACTION, this::importButtonHandler);
        dialogPane.lookupButton(importButton).disableProperty().bind(dbService.runningProperty());
        dialogPane.lookupButton(exit).disableProperty().bind(dbService.runningProperty());
        

        this.setResultConverter(buttonType -> {
            if (this.rowsAffected > 0){
                return true;
            }
            else {
                return false;
            }
        });

        // Set non-dialog button
        browseButton.addEventHandler(ActionEvent.ACTION, this::browseButtonHandler);
        cancelButton.addEventHandler(ActionEvent.ACTION, this::cancelButtonHandler);


    };

    private void importButtonHandler(ActionEvent e){
        e.consume(); // Event shouldn't need to leave this handler
        PrintStream origOut = System.out;
        BrowserPrintStream browserStream = new BrowserPrintStream(System.out, status);
        System.setOut(browserStream);

        // Load file (From real file)
        String pgn = "";

        String input;
        if ((input = inputField.getText()) != ""){
            Path path = Paths.get(input);
            try {
                pgn = Files.readString(path, StandardCharsets.UTF_8);
            }
            catch (IOException ioex){
                System.out.println("Exception " + ioex + " occurred while reading file, check filepath");
                return;
            }
        }
        else {
            System.out.println("Invalid path provided");
            return;
        }

        if (pgn == null || pgn.equals("")){
            System.out.println("Empty file provided, try again");
            return;
        }
        else {
            System.out.println("Found file, extracting");
        }

        // "//R" matches any unicode linebreak sequence
        pgn = pgn.replaceAll("\\R", "\n");

        dbService.setPgn(pgn);
        dbService.start();

        // Should always exit as succeeded, use interrupts rather than cancelled flag
        dbService.setOnSucceeded(ev -> {
            rowsAffected += dbService.getValue();
            dbService.reset();
            System.setOut(origOut);
        });
    }

    private void browseButtonHandler(ActionEvent e){
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import PGN");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PGN Files (*.pgn)", "*.pgn", "*.txt"));
        try{
            chooser.setInitialDirectory(new File(this.getClass().getClassLoader().getResource("").toURI()));
        }
        catch (Exception ex){
            System.out.println("browserButtonHandler(): Exception occurred setting initial directory");
        }

        File selectedFile = chooser.showOpenDialog(stage);

        if (selectedFile != null){
            inputField.setText(selectedFile.getAbsolutePath());
        }

    }

    private void cancelButtonHandler(ActionEvent e){
        if (dbService.isRunning()){
            dbService.interruptTask();
        }
    }
    
}
