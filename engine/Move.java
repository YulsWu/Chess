package engine;
public class Move {
    public enum MOVE_TYPE {
        MOVE, ATTACK, CASTLE_LONG, CASTLE_SHORT, EN_PESSANT, PROMOTE_MOVE, PROMOTE_ATTACK
    }
    private int originBit;
    private int destBit;
    private int originRank;
    private int originFile;
    private int destRank;
    private int destFile;
    private int piece;
    private MOVE_TYPE type;

    private Move(int pieceValue, int originBitIndex, int destinationBitIndex, MOVE_TYPE type){
        originRank = originBitIndex / 8;
        originFile = originBitIndex % 8;
        destRank = destinationBitIndex / 8;
        destFile = destinationBitIndex % 8;

        originBit = originBitIndex;
        destBit = destinationBitIndex;

        piece = pieceValue; 
    };


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
}
