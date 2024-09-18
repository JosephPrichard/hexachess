package models;

public enum GameResult {
    WIN, LOSS, DRAW; // from white's perspective

    public static GameResult fromInt(int result) {
        return switch (result) {
            case 0 -> WIN;
            case 1 -> LOSS;
            case 2 -> DRAW;
            default -> throw new IllegalStateException("Failed to map a result code to a game result enum");
        };
    }

    public int toInt() {
        return switch (this) {
            case WIN -> 0;
            case LOSS -> 1;
            case DRAW -> 2;
        };
    }
}