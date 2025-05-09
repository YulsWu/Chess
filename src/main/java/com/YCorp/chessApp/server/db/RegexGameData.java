package com.YCorp.chessApp.server.db;

import java.sql.Date;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.zip.CRC32;

import org.apache.commons.codec.digest.MurmurHash3;

import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;

public class RegexGameData {
    public ArrayList<String> moves;
    public byte[] ID;
    public HashMap<String, String> optionalMeta;

    public String event;
    public String site;
    public Date date;
    public Float round;
    public String whitePlayer;
    public String blackPlayer;
    public String result;

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd");
    public static final int HASH_SEED = 0;
    public static final Charset ID_CHARSET = StandardCharsets.UTF_8;

    public RegexGameData (HashMap<String, String> metaMap, ArrayList<String> moves){
        HashMap<String, String> tempMap =(HashMap<String, String>) metaMap.clone();
        this.moves = moves;

        event = tempMap.get("Event");
        site = tempMap.get("Site");
        date = tempMap.get("Date").contains("?") ? null : metaFormatDate(tempMap.get("Date"));
        round = tempMap.get("Round").contains("?") ? null : metaFormatFloat(tempMap.get("Round"));
        whitePlayer = tempMap.get("White");
        blackPlayer = tempMap.get("Black");
        result = tempMap.get("Result");
        
        tempMap.remove("Event");
        tempMap.remove("Site");
        tempMap.remove("Date");
        tempMap.remove("Round");
        tempMap.remove("White");
        tempMap.remove("Black");
        tempMap.remove("Result");
        
        optionalMeta = tempMap;
        
        ID = generateID();
    }

    public RegexGameData (byte[] ID,String event, String site, Date date, Float round, String whitePlayer, String blackPlayer, String result, HashMap<String, String> optionalMeta, ArrayList<String> moves){
        this.ID = ID;
        this.event = event;
        this.site = site;
        this.date = date;
        this.whitePlayer = whitePlayer;
        this.blackPlayer = blackPlayer;
        this.result = result;
        this.optionalMeta = optionalMeta;
        this.moves = moves;
    }

    public static Integer metaFormatInteger(String input){
        if (input == null){
            return null;
        }
        try {
            return Integer.valueOf(input);
        }
        catch (NumberFormatException e){
            System.out.println("Error converting " + input + " to integer: " + e);
            return null;
        }
    }

    public static Float metaFormatFloat(String input){
        if (input == null){
            return null;
        }

        try{
            return Float.valueOf(input);
        }
        catch (NumberFormatException e){
            System.out.println("Error converting " + input + " to float: " + e);
            return null;
        }
    }

    public static Date metaFormatDate(String input){
        Long milliDate;

        if (input == null || input.contains("?")){
            return null;
        }

        try {
            milliDate = DATE_FORMAT.parse(input).getTime();
            return new Date(milliDate);
        }
        catch (ParseException e){
            System.out.println("Error parsing " + input + " as date: " + e);
            return null;
        }
    }

    public static String floatToString(Float input){
        DecimalFormat df = new DecimalFormat("#.#");
        return df.format(input);
    }

    // String ID generated as whitePlayer + blackPlayer + date + Checksum on moves
    public byte[] generateID(){
        StringBuilder sb = new StringBuilder();
        long moveSum;

        // Concatenate moves with '|' delimiter
        for (int i = 0; i < this.moves.size(); i++){
            sb.append(this.moves.get(i));
            sb.append("|");
        }

        // Generate 128 bit hash from concatenated moves
        // Returned as 2 64-bit longs
        long[] hashArray = MurmurHash3.hash128(sb.toString().getBytes());

        // Combine 64 bit hashes and convert to byte[16] ID
        return longArrayToByteArray(hashArray);
        

        // sb.setLength(0);

        // int whiteComma = this.whitePlayer.indexOf(',');
        // int blackComma = this.blackPlayer.indexOf(',');
        // int whiteSpace = this.whitePlayer.indexOf(' ');
        // int blackSpace = this.blackPlayer.indexOf(' ');

        // if (whiteComma == -1 && whiteSpace == -1){
        //     sb.append(this.whitePlayer);
        // }
        // else if (whiteComma == -1){
        //     sb.append(this.whitePlayer.substring(0, whiteSpace));
        // }
        // else {
        //     sb.append(this.whitePlayer.substring(0, whiteComma));
        // }

        // if (blackComma == -1 && blackSpace == -1){
        //     sb.append(this.blackPlayer);
        // }
        // else if (blackComma == -1){
        //     sb.append(this.blackPlayer.substring(0, blackSpace));
        // }
        // else {
        //     sb.append(this.blackPlayer.substring(0, blackComma));
        // }

        // sb.append(this.date == null ? "null" : DATE_FORMAT.format(this.date));
        // sb.append(moveSum);
    }

    public static byte[] longArrayToByteArray(long[] la){
        byte[] retArray = new byte[16];

        ByteBuffer bbf = ByteBuffer.wrap(retArray);

        bbf.putLong(la[0]);
        bbf.putLong(la[1]);

        return retArray;
    }

    public ArrayList<String> getMoves(){
        return (ArrayList<String>) this.moves.clone();
    }

    public HashMap<String, String> getOptionalMeta(){
        return (HashMap<String, String>) this.optionalMeta.clone();
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        // Convert byte[16] into readable format, for debugging game data equality
        String strID = new String(this.ID, StandardCharsets.ISO_8859_1);
        sb.append("GameID: " + strID + "\n");
        sb.append("\tEvent: " + this.event + "\n");
        sb.append("\tSite: " + this.site + "\n");
        sb.append("\tDate: " + this.date + "\n");
        sb.append("\tRound: " + this.round + "\n");
        sb.append("\tWhite: " + this.whitePlayer + "\n");
        sb.append("\tBlack: " + this.blackPlayer + "\n");
        sb.append("\tResult: " + this.result + "\n");
        sb.append("\t" + this.optionalMeta.size() + " additional meta fields in optional:\n");

        for (Map.Entry<String,String> entry : this.optionalMeta.entrySet()){
            sb.append("\t" + entry.getKey() + ": " + entry.getValue() + "\n");
        }

        sb.append("\t" + this.moves.size() + " moves starting with ");
        if (this.moves.size() >= 8){
            for (int i = 0; i < 4; i++){
                sb.append(this.moves.get(i) + " ");
            }
            sb.append("......");
            for (int i = this.moves.size() - 4; i < this.moves.size(); i++){
                sb.append(this.moves.get(i) + " ");
            }
        }
        else {
            for (String mv : this.moves){
                sb.append(mv + " ");
            }
        }
        sb.append(this.result);

        return sb.toString();
    }


}