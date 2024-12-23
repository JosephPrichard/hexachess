package models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jsoup.Jsoup;
import utils.Globals;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

import static utils.Globals.HTML_SAFELIST;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    public static final float START_ELO = 1000f;

    String id;
    String username;
    String country;
    float elo;
    float highestElo;
    int wins;
    int losses;
    int rank;
    String bio;
    @EqualsAndHashCode.Exclude
    Timestamp joinedOn;

    public UserEntity(String id, String username, String country, float elo, int rank) {
        this(id, username, country, elo, elo, 0, 0, rank, null, null);
    }

    public int getWinRate() {
        var total = getTotal();
        return total == 0 ? 0 : wins * 100 / total;
    }

    public int getTotal() {
        return wins + losses;
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
            return Globals.GREEN_COLOR;
        } else if (winRate < 50) {
            return Globals.RED_COLOR;
        } else {
            return Globals.YELLOW_COLOR;
        }
    }

    public void roundElo() {
        elo = Math.round(elo);
        highestElo = Math.round(highestElo);
    }

    public void sanitize() {
        username = Jsoup.clean(username, HTML_SAFELIST);
        country = Jsoup.clean(country, HTML_SAFELIST);
    }
}