package engine;
import java.util.HashMap;
import java.util.Map;
/**
 * Holds all important information pertaining to a chess move by either player.<br>
 * 
 * <p>
 * Data structure class that is only instantiated once a move is determined to be entirely valid<br>
 * for the current move, defined by the state of the board at the current move ply. No validity checks<br>
 * on an instantiated Move object are required, and can just be taken 'as is' assuming it is the correct<br>
 * order in relation to other played moves.
 * </p>
 */
public class Move {
    /**
     * Describes the type of move for a Move object.<br>
     * 
     * <p>
     * Used in {@link Board#playMove(Move)} and {@link Board#updateState(int)} to inform changes <br>
     * made to the pieces on the board, and the state of the board. Each entry in this enum describes <br>
     * a different behaviour required to play the move on to the board.
     * </p>
     */
    public enum MOVE_TYPE {
        MOVE, ATTACK, CASTLE_LONG, CASTLE_SHORT, EN_PASSENT, PROMOTE_MOVE, PROMOTE_ATTACK
    }
    private int piece;
    private int promotionPiece;
    private MOVE_TYPE type;
    private int originBit;
    private int destBit;
    
    private int originRank;
    private int originFile;
    private int destRank;
    private int destFile;

    public static final Map<Integer, String> PIECE_INT_TO_STRING = new HashMap<>(){{
        put(1, "");
        put(-1, "");
        put(2, "N");
        put(-2, "N");
        put(3, "B");
        put(-3, "B");
        put(4, "R");
        put(-4, "R");
        put(5, "Q");
        put(-5, "Q");
        put(6, "K");
        put(-6, "K");
    }};

    public Move(int pieceValue, int originBitIndex, int destinationBitIndex, MOVE_TYPE type){
        originRank = originBitIndex / 8;
        originFile = originBitIndex % 8;
        destRank = destinationBitIndex / 8;
        destFile = destinationBitIndex % 8;

        originBit = originBitIndex;
        destBit = destinationBitIndex;

        piece = pieceValue; 

        this.type = type;
    };

    
    public Move(int pieceValue, int originBitIndex, int destinationBitIndex, MOVE_TYPE type, int promotionPiece){
        originRank = originBitIndex / 8;
        originFile = originBitIndex % 8;
        destRank = destinationBitIndex / 8;
        destFile = destinationBitIndex % 8;

        originBit = originBitIndex;
        destBit = destinationBitIndex;

        piece = pieceValue; 

        this.type = type;

        this.promotionPiece = promotionPiece;
    }

    public int getOriginRank(){
        return this.originRank;
    }

    public int getOriginFile(){
        return this.originFile;
    }

    public int getDestinationRank(){
        return this.destRank;
    }

    public int getDestinationFile(){
        return this.destFile;
    }

    public int getOriginBit(){
        return this.originBit;
    }

    public int getDestBit(){
        return this.destBit;
    }

    public int getPiece(){
        return this.piece;
    }

    public MOVE_TYPE getType(){
        return this.type;
    }

    public int getPromotionPiece(){
        if (this.promotionPiece == 0){
            System.out.println("getPromotionPiece() invoked for a non-initialized promotion piece");
        }
        
        if (!(this.type == MOVE_TYPE.PROMOTE_ATTACK || this.type == MOVE_TYPE.PROMOTE_MOVE)){
            System.out.println("getPromotionPiece() invoked for a non-promotion type move");
        }
        return this.promotionPiece;
    }

    public void setPromotionPiece(int promotionPiece){
        if (promotionPiece > 6 || promotionPiece < -6){
            System.out.println("setPromotionPiece(): Invalid piece identifier provided");
        }
        else {
            this.promotionPiece = promotionPiece;
        }
    }

    @Override
    public boolean equals(Object obj){
        Move temp;
        if (obj instanceof Move){
            temp = (Move)obj;
            if (
                (this.piece == temp.getPiece()) &&
                (this.originBit == temp.getOriginBit()) &&
                (this.destBit == temp.getDestBit()) &&
                (this.type == temp.getType())
            ){
                return true;
            }
            else {
                return false;
            }
        }
        else {
            return false;
        }
    }

    @Override
    public String toString(){
        return "Piece: " + piece + "\nOrigin: " + originBit + "\nDestination: " + destBit + "\nType: " + type;
    }

    @Override
    public Move clone(){
        return new Move(this.piece, this.originBit, this.destBit, this.type);
    }
}
