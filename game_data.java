// Using this as a data structure to associate each individual game's data together
import java.util.ArrayList;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Objects;

public class game_data {
    public ArrayList<String> meta;
    public ArrayList<String[]> moves;
    public String stringID; 

    public String event;
    public String site;
    public Date date;
    public float round;
    public String white_player;
    public String black_player;
    public String result;
    public int white_elo;
    public int black_elo;
    public String eco;

    public game_data(ArrayList<String> meta, ArrayList<String[]> moves){
        this.meta = meta;
        this.moves = moves;
        
        int[] idx = {2, 3, 4, 5, 7, 8};

        // Construct StringID out of metadata fields
        StringBuilder sb = new StringBuilder();
        for (int i : idx){
            sb.append(meta.get(i));
            sb.append("-");
        }

        stringID = sb.toString();

        // Set all metadata attributes 
        event = meta.get(0);
        site = meta.get(1);  
        round = meta.get(3).equals("?") ? null : Float.valueOf(meta.get(3));
        white_player = meta.get(4);
        black_player = meta.get(5);
        result = meta.get(6);
        
        if (meta.get(7) == ""){
            white_elo = -1;
        }
        else{
            white_elo = Integer.valueOf(meta.get(7));
        }

        if (meta.get(8) == ""){
            black_elo = -1;
        }
        else {
            black_elo = Integer.valueOf(meta.get(8));
        }

        eco = meta.get(9);

        // Convert date into a Timestamp object
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
        Long milli_date;
        String str_date = meta.get(2);

        // Some dates have unspecified days and months, substitute with null
        if (str_date.contains("?")){
            date = null;
        }
        else{
            try{
                milli_date = sdf.parse(str_date).getTime();
                date = new Date(milli_date);
            }
            catch (Exception e){
                System.out.println("Error converting date string into Date: " + e);
            }
        }
    }

    public String getStringMeta(){
        StringBuilder retStr = new StringBuilder();

        for (String m : this.meta){
            retStr.append(m);
            retStr.append('_');
        }

        return retStr.toString();
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

    @Override
    public String toString(){
        Object[] meta_list = new Object[]{
                                        this.event,
                                        this.site,
                                        this.date,
                                        this.round,
                                        this.white_player, 
                                        this.black_player,
                                        this.result,
                                        this.white_elo == -1 ? "unspecified" : this.white_elo, 
                                        this.black_elo == -1 ? "unspecified" : this.black_elo,
                                        this.eco
                                    };
                                    
        String meta_string = String.format(
            "%s event at %s on %tF.\nRound %.1f between %s as white, and %s as black.\nThe result is %s. White elo is %s, Black elo is %s. ECO code: %s.",
            meta_list
        );

        String moves_string = "";
        int count = 1;
        for (String[] s : this.moves){
            moves_string += count + ". " + s[0] + " " + s[1] + "\n";
            count++;
        }


        String returnString = meta_string + "\n" + moves_string;
        return returnString;
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

        if (obj instanceof game_data){
            game_data gd = (game_data) obj;

            if ((gd.stringID.equals(this.stringID)) && (gd.moves.equals(this.moves))){
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
