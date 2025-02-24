public class Move {
    private int[] origin;
    private int[] destination;
    private Piece target;

    public Move(Piece target, int[] origin, int[] destination){
        this.target = target;
        this.origin = origin;
        this.destination = destination;
    }
    public Move(Piece target, int xOrigin, int yOrigin, int xDest, int yDest){
        this.target = target;
        this.origin = new int[]{xOrigin, yOrigin};
        this.destination = new int[]{xDest, yDest};
    }
}
