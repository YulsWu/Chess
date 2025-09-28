package com.YCorp.chessApp.server.db;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.TableView;

public class DatabaseClient {

    private int entriesPerPage;
    private String statement;
    private TableView<BrowserEntry> tableView;
    private int numPages;

    public DatabaseClient(int entriesPerPage, TableView<BrowserEntry> tableView){
        this.entriesPerPage = entriesPerPage;
        statement = "SELECT * FROM games ORDER BY game_date DESC";
        numPages = (RegexDatabase.countAll() + entriesPerPage - 1)/entriesPerPage; // Take ceiling quotient for num pages
        this.tableView = tableView;
    };

    public Node getPage(int page){
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


}
