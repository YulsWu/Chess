package com.YCorp.chessApp.client.javafx.control;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.YCorp.chessApp.client.javafx.classes.interfaces.Closeable;
import com.YCorp.chessApp.client.javafx.events.SceneTransitionEvent;
import com.YCorp.chessApp.client.parser.RegexParser;
import com.YCorp.chessApp.server.db.BrowserEntry;
import com.YCorp.chessApp.server.db.DatabaseClient;
import com.YCorp.chessApp.server.db.RegexDatabase;
import com.YCorp.chessApp.server.db.RegexGameData;
import com.YCorp.chessApp.client.javafx.classes.BrowserPasteDialog;
import com.YCorp.chessApp.client.javafx.classes.BrowserImportDialog;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

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
    private DatePicker fromDatePicker;
    @FXML
    private DatePicker toDatePicker;
    @FXML
    private ComboBox<String> eventComboBox;
    @FXML
    private Pagination resultsPagination;
    @FXML
    private Button applyButton;
    @FXML 
    private Button clearButton;

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
        int[] columnWidthUnits = new int[]{2, 2, 2, 3, 3, 1, 1}; // sums to divisor


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

        // TableView Setup
        resultsTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Node setup
        whitePlayerComboBox.setId("white_player");
        blackPlayerComboBox.setId("black_player");
        siteComboBox.setId("site");
        eventComboBox.setId("chess_event");

        filters.add(whitePlayerComboBox);
        filters.add(blackPlayerComboBox);
        filters.add(siteComboBox);
        filters.add(eventComboBox);
        filters.trimToSize();

        //Handlers
        applyButton.addEventHandler(ActionEvent.ACTION, this::applyButtonHandler);
        backButton.addEventHandler(ActionEvent.ACTION, this::backButtonHandler);
        pasteButton.addEventHandler(ActionEvent.ACTION, this::pasteButtonHandler);
        importButton.addEventHandler(ActionEvent.ACTION, this::importButtonHandler);
        deleteButton.addEventHandler(ActionEvent.ACTION, this::deleteButtonHandler);
        clearButton.addEventHandler(ActionEvent.ACTION, this::clearButtonHandler);
        playButton.addEventHandler(ActionEvent.ACTION, this::playButtonHandler);

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
        // For each of the Filter comboboxes,
        for (int i = 0; i < filters.size(); i++){
            value = filters.get(i).getValue();
            // If there's a value input in those boxes
            if ((value != null) && (value != "")){
                ComboBox<String> valueBox = filters.get(i);
                // If we've started filtering, put AND, otherwise put WHERE
                if (filtering){
                    sb.append(" AND ").append(valueBox.getId()).append(" = \"").append(value).append("\"");
                }
                else {
                    sb.append(" WHERE ").append(valueBox.getId()).append(" = \"").append(value).append("\"");
                    filtering = true;
                }

            }
        }
        // Date filtering requires the specific identity of the DatePicker to determine whether we > or < the value against the records
        LocalDate fromDate = this.fromDatePicker.getValue();
        LocalDate toDate = this.toDatePicker.getValue();
        
      
        // Messy logic, basically only include if the date field is not null, then check if we're already filtering to determine
        // the appropriate statement string to add (AND vs WHERE)
        if (!(fromDate == null && toDate == null)){
            if (fromDate != null){
                long epoch = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
                if (filtering){
                    sb.append(" AND game_date >= ").append(epoch);
                }
                else {
                    filtering = true;
                    sb.append(" WHERE game_date >= ").append(epoch);
                }
            
            }
            
            if (toDate != null){
                long epoch = toDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

                if (filtering){
                    sb.append(" AND game_date <= ").append(epoch);
                }
                else {
                    filtering = true;
                    sb.append(" WHERE game_date <= ").append(epoch);
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

    private void pasteButtonHandler(ActionEvent e){
        Dialog<Boolean> d = new BrowserPasteDialog();
        d.showAndWait();

        // Refresh if DB changed
        if (d.getResult() != null && d.getResult() == true){
            dummy.fireEvent(new SceneTransitionEvent(SceneTransitionEvent.TO_BROWSER));
        }
    }

    private void importButtonHandler(ActionEvent e){
        Dialog<Boolean> d = new BrowserImportDialog();
        d.showAndWait();

        // Refresh if DB changed
        if (d.getResult() != null && d.getResult() == true){
            dummy.fireEvent(new SceneTransitionEvent(SceneTransitionEvent.TO_BROWSER));
        }

    }

    private void deleteButtonHandler(ActionEvent e){
        ArrayList<byte[]> ids = new ArrayList<>();
        for (BrowserEntry entry : this.resultsTableView.getSelectionModel().getSelectedItems()){
            ids.add(entry.getId());
        } 

        if (ids.size() == 0){
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        DialogPane alertPane = alert.getDialogPane();
        alertPane.setPrefSize(350, 110);

        alert.setTitle("Confirm deletion");

        Label content = new Label("Are you sure you want to delete " + ids.size() + (ids.size() == 1 ? " game?" : " games?\nThis action cannot be undone"));
        VBox contentBox = new VBox(content);
        contentBox.setAlignment(Pos.CENTER);

        // header.setTextAlignment(TextAlignment.CENTER);
        content.setTextAlignment(TextAlignment.CENTER);

        alertPane.setHeader(new VBox());
        alertPane.setContent(contentBox);

        Optional<ButtonType> result = alert.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK){
            RegexDatabase.deleteByID(ids);
            dummy.fireEvent(new SceneTransitionEvent(SceneTransitionEvent.TO_BROWSER)); // Refresh
        }
    }

    private void clearButtonHandler(ActionEvent e){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        DialogPane alertPane = alert.getDialogPane();
        alertPane.setPrefSize(350, 110);

        alert.setTitle("Wipe Database?");

        Label content = new Label("WARNING: ALL GAMES WILL BE DELETED\nThis action cannot be undone");
        VBox contentBox = new VBox(content);
        contentBox.setAlignment(Pos.CENTER);

        // header.setTextAlignment(TextAlignment.CENTER);
        content.setTextAlignment(TextAlignment.CENTER);

        alertPane.setHeader(new VBox());
        alertPane.setContent(contentBox);

        Optional<ButtonType> result = alert.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK){
            RegexDatabase.deleteAll();
            dummy.fireEvent(new SceneTransitionEvent(SceneTransitionEvent.TO_BROWSER)); // Refresh
        }
    }

    private void playButtonHandler(ActionEvent e){
        // Isolate game ID\
        ObservableList<BrowserEntry> selections = this.resultsTableView.getSelectionModel().getSelectedItems();

        if (selections.size() > 0){
            byte[] gameId = selections.get(0).getId();
            RegexGameData game = RegexDatabase.readGameById(gameId);
            dummy.fireEvent(new SceneTransitionEvent(SceneTransitionEvent.TO_REPLAY, game));
        }
        else return;
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
