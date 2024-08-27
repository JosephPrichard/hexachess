package chess;

import static chess.ChessBoard.*;

public record Hexagon(int file, int rank) {

    public static Hexagon fromIndex(int index) {
        var file = index / FILES;
        var rank = index % RANKS;
        return new Hexagon(file, rank);
    }

    public static Hexagon fromNotation(String notation) {
        var file = notation.charAt(0) - 'a';
        var rank = Integer.parseInt(notation.substring(1)) - 1;
        return new Hexagon(file, rank);
    }

    @Override
    public String toString() {
        char fileChar = (char) (file + 'a');
        return fileChar + Integer.toString(rank + 1);
    }

    public int toIndex() {
       return toIndex(file, rank);
    }

    // convert a hexagon to an index without allocating a hexagon record structure
    public static int toIndex(int file, int rank) {
        return file * FILES + rank;
    }
}
