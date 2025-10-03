package com.YCorp.chessApp.server.db;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TableView;

public class DatabaseClient {

    private int entriesPerPage;
    private String statement = "SELECT * FROM games";
    private TableView<BrowserEntry> tableView;
    private int numPages;
    private boolean empty;

    public DatabaseClient(int entriesPerPage, TableView<BrowserEntry> tableView){
        this.entriesPerPage = entriesPerPage;
        numPages = (RegexDatabase.countQuery(statement) + entriesPerPage - 1)/entriesPerPage; // Take ceiling quotient for num pages
        System.out.println(numPages);
        this.tableView = tableView;

        if (numPages == 0){
            numPages = 1;
            empty = true;
            ObservableList<BrowserEntry> list = FXCollections.observableArrayList();
            list.add(new BrowserEntry(null, "NO", "GAMES", "<3", ":(", "FOUND", ":P", ":L"));
            tableView.setItems(list);
        }
        else {
            empty = false;
        }
    };

    public DatabaseClient(int entriesPerPage, TableView<BrowserEntry> tableView, String statement){
        this.statement = statement;
        this.entriesPerPage = entriesPerPage;
        numPages = (RegexDatabase.countQuery(statement) + entriesPerPage - 1)/entriesPerPage; // Take ceiling quotient for num pages
        System.out.println(numPages);
        this.tableView = tableView;

        if (numPages == 0){
            numPages = 1;
            empty = true;
            ObservableList<BrowserEntry> list = FXCollections.observableArrayList();
            list.add(new BrowserEntry(null, "NO", "GAMES", ":((((", "<3", "FOUND", ":P", ":L"));
            tableView.setItems(list);
        }
        else {
            empty = false;
        }
    }

    public Node getPage(int page){
        // If the original statement is empty, just return the same empty tableView
        if (empty){
            return tableView;
        }

        String pageStatement = statement;

        // Add LIMIT and OFFSET depending on the page provided
        pageStatement += " LIMIT " + entriesPerPage + " ";
        if (page > 0){
            pageStatement += " OFFSET " + (entriesPerPage * page);
        }

        this.tableView.setItems(FXCollections.observableArrayList(RegexDatabase.readBrowserEntries(pageStatement)));
        return tableView;
    }

    public int getNumPages(){
        return numPages; // Primitive int
    }




}
