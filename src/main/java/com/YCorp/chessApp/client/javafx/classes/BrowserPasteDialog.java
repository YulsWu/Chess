package com.YCorp.chessApp.client.javafx.classes;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;

import com.YCorp.chessApp.client.parser.RegexParser;
import com.YCorp.chessApp.server.db.RegexDatabase;
import com.YCorp.chessApp.server.db.RegexGameData;

import javafx.scene.control.ButtonType;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

public class BrowserPasteDialog extends Dialog<Boolean>{
    private DialogPane dialogPane;
    private TextArea inputArea;
    private int rowsAffected = 0;


    private static String sampleText;
    private ObservableList<String> status = FXCollections.observableArrayList();
    
    
        static{
            StringBuilder sb = new StringBuilder("---------EXAMPLE PGN---------\n");
            URL url = BrowserPasteDialog.class.getResource("/pgn/browserExample.pgn");
            try(
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            )
            {
                while (reader.ready()){
                    sb.append(reader.readLine()).append('\n');
                }
            }
            catch (Exception e){
                System.out.println("Exception occurred in static initialization of DialogFactory");
                e.printStackTrace();
            }
    
            sampleText = sb.toString();
        }
    
        public BrowserPasteDialog(){
            super();
            dialogPane = this.getDialogPane();
            dialogPane.setPrefSize(450, 550);
            this.setTitle("Import PGN from text");
    
            ButtonType apply = new ButtonType("Apply", ButtonBar.ButtonData.LEFT);
            ButtonType exit = new ButtonType("Exit", ButtonBar.ButtonData.CANCEL_CLOSE);
            
            dialogPane.getButtonTypes().addAll(apply, exit);
            dialogPane.lookupButton(apply).addEventFilter(ActionEvent.ACTION, this::applyButtonHandler); // FILTER action and consume to prevent window closing
    
            inputArea = new TextArea();
            inputArea.setText(sampleText);
            inputArea.setWrapText(false);
    
            dialogPane.setContent(inputArea);
    
            ListView<String> statusView = new ListView<String>();
            statusView.setEditable(false);
            statusView.setItems(status);
            statusView.setPrefHeight(dialogPane.getPrefHeight()/4);
            statusView.setPadding(Insets.EMPTY);
            statusView.setFixedCellSize(17);
            statusView.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px;");

       

            status.add("STATUS: Awaiting input...");
    
            dialogPane.setHeader(statusView);

            // Return true if any changes have been made to the DB, which would require a refresh of the DB screen
            this.setResultConverter(buttonType -> {
                if (this.rowsAffected > 0){
                    return true;
                }
                else {
                    return false;
                }
            });
        }

        private void applyButtonHandler(ActionEvent e){
            PrintStream origOut = System.out;
            BrowserPrintStream browserStream = new BrowserPrintStream(System.out, this.status);
            try
            {
                int unwritten;
                ArrayList<RegexGameData> games;
                System.setOut(browserStream);
                games = RegexParser.extractPGN(inputArea.getText());
                System.out.println("STATUS: " + games.size() + " games extacted...");
                unwritten = RegexDatabase.writeDB(games,50).size();
                System.out.println("STATUS: Finished with " + unwritten + " errors");
                System.out.println("DONE: Exit or go again");

                // track rows affected to determine if DB was affected during Dialog
                rowsAffected += (games.size() - unwritten);
            }
            catch (Exception ex){
                System.out.println("Exception " + ex + " while trying to parse PGN");
            }
            finally{
                System.setOut(origOut);
                e.consume();
            }
        }
    
}
