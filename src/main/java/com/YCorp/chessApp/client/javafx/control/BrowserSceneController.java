package com.YCorp.chessApp.client.javafx.control;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.YCorp.chessApp.client.javafx.classes.interfaces.Closeable;
import com.YCorp.chessApp.client.javafx.events.SceneTransitionEvent;
import com.YCorp.chessApp.server.db.BrowserEntry;
import com.YCorp.chessApp.server.db.DatabaseClient;
import com.YCorp.chessApp.server.db.RegexDatabase;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;

public class BrowserSceneController implements Closeable{
    //FXML Injection

    @FXML
    private Pane root;
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
    @FXML
    private ComboBox<String> whitePlayerComboBox;
    @FXML
    private ComboBox<String> blackPlayerComboBox;
    @FXML
    private ComboBox<String> siteComboBox;
    @FXML
    private ComboBox<String> dateComboBox;
    @FXML
    private ComboBox<String> eventComboBox;
    @FXML
    private Pagination resultsPagination;
    @FXML
    private Button applyButton;

    private Pane dummy;
    private TableView<BrowserEntry> resultsTableView;

    private DatabaseClient client;

    private int entriesPerPage = 16;

    ArrayList<ComboBox<String>> filters = new ArrayList<>();

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
        getters.trimToSize();
    }
    

    public void init(){
        dummy = new Pane();
        dummy.setVisible(false);
        root.getChildren().add(dummy);
        
        resultsTableView = new TableView<BrowserEntry>();
        resultsTableView.setPrefSize(724, 24 * 17); // Set height to correspond to 16 (+ 1 header) rows of 24px height
        // Ordered list of column width units
        int divisor = 14;
        double cellUnit = (int) (resultsTableView.getPrefWidth())/divisor;// Each column has 1px right border
        int[] columnWidthUnits = new int[]{3, 3, 2, 2, 2, 1, 1}; // sums to divisor


        // Create TableColumns for each metaData label, attach a read-only property wrapper as we do not require
        // auto-updating functionality
        int cumulativeWidth = 0;
        for (int i = 0; i < metaLabels.length; i++){
            // To satisfy lambda local referencing safety rules, create a final copy of the index at each loop
            // Should be safe to do as I am certainly referencing different method references at each index
            final int ind = i;
            TableColumn<BrowserEntry, String> col = new TableColumn<BrowserEntry, String>(metaLabels[i]);

            // Just wraps the static string from BrowserEntry as a property
            // Intended usage is on a real property to update and display the value of the property when it changes
            col.setCellValueFactory(cd -> 
                new ReadOnlyStringWrapper(getters.get(ind).apply(cd.getValue())) // we .apply() on the method signature to provide an object that we want to invoke the method on, .apply() == takes parameters, returns value, .accept(T) just takes, .get() just returns (For the different funcitonal interfaces)
            );

            // Set column width using corresponding cell width units
            if (i != metaLabels.length - 1){
                col.setPrefWidth(columnWidthUnits[i] * cellUnit);
                cumulativeWidth += (columnWidthUnits[i] * cellUnit);
            }
            else {
                col.setPrefWidth(resultsTableView.getPrefWidth() - cumulativeWidth - 2); // Pixel counts are slightly off due to column borders
            }

            resultsTableView.getColumns().add(col);
        }

        // Populate filter combo boxes with current unique values found in database
        updateFilters();
        
        // Set up database client and Pagination widget
        client = new DatabaseClient(this.entriesPerPage, resultsTableView);
        resultsPagination.setPageCount(client.getNumPages());
        resultsPagination.setPageFactory(client::getPage);
        resultsPagination.setMaxPageIndicatorCount(8);

        // Node setup
        whitePlayerComboBox.setId("white_player");
        blackPlayerComboBox.setId("black_player");
        siteComboBox.setId("site");
        eventComboBox.setId("chess_event");
        dateComboBox.setId("game_date");
        filters.add(whitePlayerComboBox);
        filters.add(blackPlayerComboBox);
        filters.add(siteComboBox);
        filters.add(dateComboBox);
        filters.add(eventComboBox);
        filters.trimToSize();

        //Handlers
        applyButton.addEventHandler(ActionEvent.ACTION, this::applyButtonHandler);
        backButton.addEventHandler(ActionEvent.ACTION, this::backButtonHandler);

    }

    private void updateFilters(){
        ObservableList<String> players = FXCollections.observableArrayList(RegexDatabase.getUniquePlayers()); 
        whitePlayerComboBox.setItems(players);
        blackPlayerComboBox.setItems(players);
        siteComboBox.setItems(FXCollections.observableArrayList(RegexDatabase.getUniqueSites()));
        eventComboBox.setItems(FXCollections.observableArrayList(RegexDatabase.getUniqueEvents()));
    }

    private void applyButtonHandler(ActionEvent a){
        StringBuilder sb = new StringBuilder("SELECT * FROM games");
 
        // If there are filters we start with "WHERE", and then "AND" for each subsequent filter
        boolean filtering = false;
        String value;
        for (int i = 0; i < filters.size(); i++){
            value = filters.get(i).getValue();
            if ((value != null) && (value != "")){
                ComboBox<String> valueBox = filters.get(i);
                if (filtering){
                    sb.append(" AND ").append(valueBox.getId()).append(" = \"").append(value).append("\"");
                }
                else {
                    sb.append(" WHERE ").append(valueBox.getId()).append(" = \"").append(value).append("\"");
                    filtering = true;
                }
            }
        }

        // If there are no filters the statement stays as-is

        System.out.println(sb.toString());
        this.client = new DatabaseClient(this.entriesPerPage, this.resultsTableView, sb.toString());
        this.resultsPagination.setPageFactory(client::getPage);
        this.resultsPagination.setCurrentPageIndex(0);
        this.resultsPagination.setPageCount(client.getNumPages());
        
    }

    private void backButtonHandler(ActionEvent e){
        dummy.fireEvent(new SceneTransitionEvent(SceneTransitionEvent.TO_MENU));
    }

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
