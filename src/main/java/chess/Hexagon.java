package chess;

import java.util.ArrayList;
import java.util.List;

import static chess.ChessBoard.FILES;
import static chess.ChessBoard.RANKS_PER_FILE;

public record Hexagon(int file, int rank) {
    public static final Hexagon[] ORDERED = getOrdered();
    public static final int MIDPOINT = 5;

    public enum Direction {
        UP,
        DOWN,
        DOWN_LEFT,
        DOWN_RIGHT,
        UP_LEFT,
        UP_RIGHT,
    }

    public static Hexagon of(int file, int rank) {
        return new Hexagon(file, rank);
    }

    public Hexagon addOffset(int file, int rank) {
        return Hexagon.of(file() + file, rank() + rank);
    }

    public Hexagon walk(Direction[] directions) {
        var hex = this;
        for (var direction : directions) {
            hex = hex.walk(direction);
        }
        return hex;
    }

    public Hexagon walk(Direction direction) {
        return switch(direction) {
            case UP -> addOffset(0, 1);
            case DOWN -> addOffset(0, -1);
            case UP_LEFT -> file() <= MIDPOINT ? addOffset(-1, 0) : addOffset(-1, 1);
            case DOWN_LEFT -> file() <= MIDPOINT ? addOffset(-1, -1) : addOffset(-1, 0);
            case UP_RIGHT -> file() < MIDPOINT ? addOffset(1, 1) : addOffset(1,0);
            case DOWN_RIGHT -> file() < MIDPOINT ? addOffset(1, 0) : addOffset(1, -1);
        };
    }

    private static Hexagon[] getOrdered() {
        List<Hexagon> hexagons = new ArrayList<>();
        for (int file = 0; file < FILES; file++) {
            for (int rank = 0; rank < RANKS_PER_FILE[file]; rank++) {
                hexagons.add(new Hexagon(file, rank));
            }
        }
        return hexagons.toArray(Hexagon[]::new);
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
        return file * FILES + rank;
    }
}
