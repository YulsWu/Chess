public class Move {
    private int originRank;
    private int originFile;
    private int destRank;
    private int destFile;
    private int piece;

    public Move(int pieceValue, int originBitIndex, int destinationBitIndex){
        originRank = originBitIndex / 8;
        originFile = originBitIndex % 8;
        destRank = destinationBitIndex / 8;
        destFile = destinationBitIndex % 8;

        piece = pieceValue; 
    };
}
