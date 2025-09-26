package com.YCorp.chessApp.server.db;

public class BrowserEntry {
    private byte[] id;
    private String white;
    private String black;
    private String site;
    private String event;
    private String date;
    private String round;
    private String result;


    public BrowserEntry(byte[] id, String white, String black, String site, String event, String date, String round, String result){
        this.id = id;
        this.white = white;
        this.black = black;
        this.site = site;
        this.event = event;
        this.date = date;
        this.round = round;
        this.result = result;
    }

    //#region Accessors
    public byte[] getId(){
        return id.clone();
    }

    public String getWhitePlayer(){
        return this.white;
    }
    
    public String getBlackPlayer(){
        return this.black;
    }

    public String getSite(){
        return this.site;
    }

    public String getEvent(){
        return this.event;
    }

    public String getDate(){
        return this.date;
    }

    public String getRound(){
        return this.round;
    }

    public String getResult(){
        return this.result;
    }
    //#endregion
    
}
