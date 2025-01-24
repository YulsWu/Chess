// Using this as a data structure to associate each individual game's data together
import java.util.ArrayList;

public class game_data {
    public ArrayList<String> meta;
    public ArrayList<String[]> moves;
    public String stringID;

    public game_data(ArrayList<String> meta, ArrayList<String[]> moves){
        this.meta = meta;
        this.moves = moves;
        
        int[] idx = {2, 3, 4, 5, 7, 8};

        StringBuilder sb = new StringBuilder();
        for (int i : idx){
            sb.append(meta.get(i));
            sb.append("-");
        }

        stringID = sb.toString();
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
        String meta_string = String.format(
            "%s event at %s on %s.\nRound %s between %s as white, and %s as black.\nThe result is %s. White elo is %s, Black elo is %s. ECO code: %s.",
            this.meta.toArray()
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
}
