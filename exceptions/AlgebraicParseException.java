package exceptions;
/**
 * Custom exception class thrown when errors are encountered when parsing moves in algebraic notation<br>
 * <p>
 * Parsing algebraic notation moves is integral to the Terminal/Lanterna game loop as the loop cannot<br>
 * continue reliably if a move cannot be generated from the user's input. Both the program state and <br>
 * chess game state become invalid and thus we must either handle this situation (by prompting the user again)<br>
 * or halt execution.
 */
public class AlgebraicParseException extends Exception {
    public AlgebraicParseException(String message){
        super(message);
    }
}
