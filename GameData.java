// Using this as a data structure to associate each individual game's data together
import java.util.ArrayList;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.Map;
import java.text.ParseException;
import java.text.DecimalFormat;

public class GameData {
    public ArrayList<String[]> moves;
    public String stringID;
    public Integer calculatedPlyCount; 

    public String event;
    public String site;
    public Date date;
    public Float round;
    public String whitePlayer;
    public String blackPlayer;
    public String result;
    public Integer whiteElo;
    public Integer blackElo;
    public String eco;
    public String whiteTitle;
    public String blackTitle;
    public Integer whiteFideId;
    public Integer blackFideId;
    public String opening;
    public String variation;
    public String timeControl;
    public String termination;
    public String mode;
    public Integer plyCount;
    public String eventType;
    public Float eventRounds;
    public String stage;
    public String annotator;
    public String source;
    public Date sourceDate;
    public String fen;
    public Boolean setUp;
    public String variant;

    public GameData(Map<String,String> metaMap, ArrayList<String[]> moves){
        this.moves = moves;

        
        
        // Set all metadata attributes 
        event = metaMap.get("Event");
        site = metaMap.get("Site");
        date = metaFormatDate(metaMap.get("Date"));
        round = metaFormatFloat(metaMap.get("Round"));
        whitePlayer = metaMap.get("White");
        blackPlayer = metaMap.get("Black");
        result = metaMap.get("Result");
        whiteElo = metaFormatInteger(metaMap.get("WhiteElo"));
        blackElo = metaFormatInteger(metaMap.get("BlackElo"));
        eco = metaMap.get("ECO");
        whiteTitle = metaMap.get("WhiteTitle");
        blackTitle = metaMap.get("BlackTitle");
        whiteFideId = metaFormatInteger(metaMap.get("WhiteFideId"));
        blackFideId = metaFormatInteger(metaMap.get("BlackFideId"));
        opening = metaMap.get("Opening");
        variation = metaMap.get("Variation");
        timeControl = metaMap.get("TimeControl");
        termination = metaMap.get("Termination");
        mode = metaMap.get("Mode");
        plyCount = metaFormatInteger(metaMap.get("PlyCount"));
        eventType = metaMap.get("EventType");
        eventRounds = metaFormatFloat(metaMap.get("EventRounds"));
        stage = metaMap.get("Stage");
        annotator = metaMap.get("Annotator");
        source = metaMap.get("Source");
        sourceDate = metaFormatDate(metaMap.get("SourceDate"));
        fen = metaMap.get("FEN");
        
        if (metaMap.get("SetUp") == null){
            setUp = null;
        }
        else if (metaFormatInteger(metaMap.get("SetUp")) == 1){
            setUp = true;
        }
        else {
            setUp = false;
        }
        variant = metaMap.get("Variant");
        
        // Calculate ply count from the moves arraylist. Different from plycount supplied from optional
        // metadata fields. Its possible the supplied plycount may be different, and I don't want to overwrite
        // the supplied count to ensure consistent string IDs across games read from PGN and read from Database
        int numTurns = moves.size();
        String lastMove;
        String middleMove;
    
        if (moves.get(numTurns - 1)[1].equals("")){
            calculatedPlyCount = (numTurns * 2) - 1;
            lastMove = moves.get(numTurns - 1)[0];
        }
        else {
            calculatedPlyCount = numTurns * 2;
            lastMove = moves.get(numTurns - 1)[1];
        }
        // Ceiling divison for the index middle turn, safe for numTurns == 1;
        int middleIndex = ((numTurns + 2 - 1) / 2) - 1;
        middleMove = moves.get(middleIndex)[0];


        // site, date, round, white, black, whiteelo, blackelo, eco
        Object[] targetKeys = new Object[]{this.site, this.date, this.round, this.whitePlayer, this.blackPlayer, this.whiteElo, this.blackElo, this.eco};
        StringBuilder sb = new StringBuilder();
        for (Object s : targetKeys){
            sb.append(String.valueOf(s));
            sb.append('-');
        }
        // Append additional metadata to increase uniqueness for each game with the same metadata labels
        sb.append(String.valueOf(middleMove));
        sb.append('-');
        sb.append(String.valueOf(lastMove));
        sb.append('-');
        sb.append(String.valueOf(calculatedPlyCount));
        sb.append('-');
        stringID = sb.toString();
    }
    
    public String getStringMoves(){
        StringBuilder retStr = new StringBuilder();
        for (String[] mv : moves){
            retStr.append(mv[0]);
            retStr.append(" ");
            retStr.append(mv[1]);
            retStr.append(" ");
        }

        return retStr.toString();
    }

    public Integer metaFormatInteger(String input){
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

    public Float metaFormatFloat(String input){
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

    public Date metaFormatDate(String input){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
        Long milliDate;

        if (input == null || input.contains("?")){
            return null;
        }

        try {
            milliDate = sdf.parse(input).getTime();
            return new Date(milliDate);
        }
        catch (ParseException e){
            System.out.println("Error parsing " + input + " as date: " + e);
            return null;
        }
    }

    public String floatToString(Float input){
        DecimalFormat df = new DecimalFormat("#.#");
        return df.format(input);
    }

    @Override
    public String toString(){
        String[] metaTitles = new String[]{
            "Event",
            "Site",
            "Date",
            "Round",
            "White",
            "Black",
            "Result",
            "WhiteElo",
            "BlackElo",
            "WhiteTitle",
            "BlackTitle",
            "WhiteFIDEId",
            "BlackFIDEId",
            "ECO",
            "Opening",
            "Variation",
            "TimeControl",
            "Termination",
            "Mode",
            "PlyCount",
            "EventType",
            "EventRounds",
            "Stage",
            "Annotator",
            "Source",
            "SourceDate",
            "FEN",
            "SetUp",
            "Variant"
        };

        Object[] metaValues = new Object[]{
            this.event,
            this.site,
            this.date,
            this.round,
            this.whitePlayer, 
            this.blackPlayer,
            this.result,
            this.whiteElo, 
            this.blackElo,
            this.whiteTitle,
            this.blackTitle,
            this.whiteFideId,
            this.blackFideId,
            this.eco,
            this.opening,
            this.variation,
            this.timeControl,
            this.termination,
            this.mode,
            this.plyCount,
            this.eventType,
            this.eventRounds,
            this.stage,
            this.annotator,
            this.source,
            this.sourceDate,
            this.fen,
            this.setUp,
            this.variant
        };
        
        StringBuilder metaSB = new StringBuilder();
        for (int i = 0; i < metaTitles.length; i++){
            metaSB.append("[");
            metaSB.append(metaTitles[i]);
            metaSB.append(": ");
            metaSB.append(metaValues[i]);
            metaSB.append("]");
            metaSB.append("\n");
        }

        String moveString = "";
        int count = 1;
        for (String[] s : this.moves){
            moveString += count + ". " + s[0] + " " + s[1] + "\n";
            count++;
        }


        String returnString = metaSB.toString() + "\n" + moveString;
        return returnString;
    }

    public boolean checkMovsetEquality(ArrayList<String[]> input){
        // Check length
        if (this.moves.size() != input.size()){
            return false;
        }

        // Check String[] subarray equality
        int i = 0;
        for (String[] sub : this.moves){
            if (!(sub.equals(input.get(i)))){
                return false;
            }
            i++;
        }
        return true;

    }
    // Simply comparing both the stringID and moveset should be sufficient to differentiate games
    @Override
    public boolean equals(Object obj){
        if (obj == null){
            return false;
        }
        else if (this == obj){
            return true;
        }

        if (obj instanceof GameData){
            GameData gd = (GameData) obj;

            boolean equalsID = gd.stringID.equals(this.stringID);
            boolean equalsMoves = checkMovsetEquality(this.moves);

            if (equalsID && equalsMoves){
                return true;
            }
            else {
                return false;
            }
        }

        return false;

    }

    @Override
    public int hashCode(){
        return Objects.hash(this.stringID, this.moves);
    }

}
