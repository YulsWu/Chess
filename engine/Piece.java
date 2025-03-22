package engine;


public abstract class Piece {
    public enum PieceType {WPAWN, BPAWN, KING, QUEEN, KNIGHT, BISHOP, ROOK}

    private PieceType type;
    protected abstract Long getValidMoves();
    protected abstract Long getValidAttacks();
}
