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
        numPages = (getQueryCount(statement) + entriesPerPage - 1)/entriesPerPage; // Take ceiling quotient for num pages
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
        numPages = (getQueryCount(statement) + entriesPerPage - 1)/entriesPerPage; // Take ceiling quotient for num pages
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
        ArrayList<BrowserEntry> entries = new ArrayList<>();

        // Add LIMIT and OFFSET depending on the page provided
        pageStatement += " LIMIT " + entriesPerPage + " ";
        if (page > 0){
            pageStatement += " OFFSET " + (entriesPerPage * page);
        }

        try{
            ResultSet rs = RegexDatabase.executeQuery(pageStatement);
            while (rs.next()){
                entries.add(new BrowserEntry(rs.getBytes("id"), rs.getString("white_player"), rs.getString("black_player"), rs.getString("site"), rs.getString("chess_event"), epochMillisToString(rs.getLong("game_date")), rs.getString("round"), rs.getString("result")));
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        this.tableView.setItems(FXCollections.observableArrayList(entries));
        return tableView;
    }

    public int getNumPages(){
        return numPages; // Primitive int
    }

    private String epochMillisToString(long epoch){
        return DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneId.of("UTC")).format(Instant.ofEpochSecond(epoch/1000));
    }

    // return negated entriesPerPage value to ensure negative number is propagated to GUI indicating failure
    private int getQueryCount(String statement){
        ResultSet rs;
        StringBuilder sb = new StringBuilder(statement);
        sb.insert(0, "SELECT COUNT(*) FROM (").append(")");
        try{
            if ((rs = RegexDatabase.executeQuery(sb.toString())).next()){
                return rs.getInt(1);
            }

        }
        catch (Exception e){
            e.printStackTrace();
            return -entriesPerPage;
        }
        return -entriesPerPage;
    }




}
