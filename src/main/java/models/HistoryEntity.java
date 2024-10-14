package models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import utils.Constants;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoryEntity {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    public static final int WHITE_WIN = 0;
    public static final int BLACK_WIN = 1;
    public static final int DRAW = 2;

    long id;
    String whiteId;
    String blackId;
    String whiteName;
    String blackName;
    String whiteCountry;
    String blackCountry;
    int result;
    float winElo;
    float loseElo;
    @EqualsAndHashCode.Exclude
    Timestamp playedOn;

    public String getFormattedResult() {
        return switch (result) {
            case WHITE_WIN -> "White Victory";
            case BLACK_WIN -> "Black Victory";
            case DRAW -> "Draw";
            default -> throw new IllegalStateException("Invalid result state " + result);
        };
    }

    private String formatElo(float elo) {
        return (elo >= 0 ? "+" : "") + elo;
    }

    public String getWhiteEloDiff() {
        return formatElo(switch (result) {
            case WHITE_WIN -> winElo;
            case BLACK_WIN -> loseElo;
            case DRAW -> 0;
            default -> throw new IllegalStateException("Invalid result state " + result);
        });
    }

    public String getBlackEloDiff() {
        return formatElo(switch (result) {
            case WHITE_WIN -> loseElo;
            case BLACK_WIN -> winElo;
            case DRAW -> 0;
            default -> throw new IllegalStateException("Invalid result state " + result);
        });
    }

    public String getWhiteEloColor() {
        return switch (result) {
            case WHITE_WIN -> Constants.GREEN_COLOR;
            case BLACK_WIN -> Constants.RED_COLOR;
            case DRAW -> Constants.YELLOW_COLOR;
            default -> throw new IllegalStateException("Invalid result state " + result);
        };
    }

    public String getBlackEloColor() {
        return switch (result) {
            case WHITE_WIN -> Constants.RED_COLOR;
            case BLACK_WIN -> Constants.GREEN_COLOR;
            case DRAW -> Constants.YELLOW_COLOR;
            default -> throw new IllegalStateException("Invalid result state " + result);
        };
    }

    public String getFormattedPlayedOn() {
        return playedOn.toLocalDateTime().format(DATE_FORMATTER);
    }
}