package models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    String id;
    String username;
    String country;
    float elo;
    float highestElo;
    int wins;
    int losses;
    int rank;
    @EqualsAndHashCode.Exclude
    Timestamp joinedOn;

    public int getWinRate() {
        var total = getTotal();
        return total == 0 ? 0 : wins * 100 / total;
    }

    public int getTotal() {
        return  wins + losses;
    }

    public int getRoundedElo() {
        return Math.round(elo);
    }

    public int getRoundedHighestElo() {
        return Math.round(highestElo);
    }

    public String getFormattedJoinedOn() {
        return joinedOn.toLocalDateTime().format(DATE_FORMATTER);
    }

    public String getWinRateColor() {
        var winRate = getWinRate();
        if (winRate > 50) {
            return "green-color";
        } else if (winRate < 50) {
            return "red-color";
        } else {
            return "yellow-color";
        }
    }

    public UserEntity(String id, String username, String country, float elo, int wins, int losses) {
       this(id, username, country, elo, elo, wins, losses, 0, null);
    }

    public UserEntity(String id, String username, String country, float elo, int wins, int losses, int rank) {
        this(id, username, country, elo, elo, wins, losses, rank, null);
    }

    public void roundElo() {
        elo = Math.round(elo);
        highestElo = Math.round(highestElo);
    }
}