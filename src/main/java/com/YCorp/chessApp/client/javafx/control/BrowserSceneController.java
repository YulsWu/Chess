package com.YCorp.chessApp.client.javafx.control;

import java.util.ArrayList;
import java.util.function.Function;

import com.YCorp.chessApp.client.javafx.classes.interfaces.Closeable;
import com.YCorp.chessApp.server.db.BrowserEntry;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class BrowserSceneController implements Closeable{
    //FXML Injection
    @FXML
    private TableView<BrowserEntry> resultsTableView;
    @FXML
    private Button playButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button importButton;
    @FXML
    private Button pasteButton;
    @FXML
    private Button backButton;

    // Dynamically attach cellValueFactories for TableColumns
    // We lose TableColumn references this way but they are not expected to be required
    String[] metaLabels = new String[]{"White", "Black", "Date", "Site", "Event", "Round", "Result"};
    ArrayList<Function<BrowserEntry, String>> getters = new ArrayList<>();
    {
        getters.add(BrowserEntry::getWhitePlayer);
        getters.add(BrowserEntry::getBlackPlayer);
        getters.add(BrowserEntry::getDate);
        getters.add(BrowserEntry::getSite);
        getters.add(BrowserEntry::getEvent);
        getters.add(BrowserEntry::getRound);
        getters.add(BrowserEntry::getResult);
    }
    

    public void init(){
        // Create TableColumns for each metaData label, attach a read-only property wrapper as we do not require
        // auto-updating functionality
        for (int i = 0; i < metaLabels.length; i++){
            // To satisfy lambda local referencing safety rules, create a final copy of the index at each loop
            // Should be safe to do as I am certainly referencing different method references at each index
            final int ind = i;
            TableColumn<BrowserEntry, String> col = new TableColumn<BrowserEntry, String>(metaLabels[i]);

            // Just wraps the static string from BrowserEntry as a property
            // Intended usage is on a real property to update and display the value of the property when it changes
            col.setCellValueFactory(cd -> 
                new ReadOnlyStringWrapper(getters.get(ind).apply(cd.getValue()))
            );

            resultsTableView.getColumns().add(col);
        }
    }

    public void setTableData(){};

    //debug
    public void setTableData(ObservableList<BrowserEntry> list){
        resultsTableView.setItems(list);
    }
    public void printTableDump(){
        System.out.println(resultsTableView.getColumns());
        System.out.println(resultsTableView.getItems());
    }
    
    // Scene doesn't require cleanup
    public void cleanup(){};
}
