package models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsEntity {
    String id;
    String username;
    String country;
    float elo;
    int wins;
    int losses;
    int rank;

    public StatsEntity(String id, String username, String country, float elo, int wins, int losses) {
       this(id, username, country, elo, wins, losses, 0);
    }

    public void roundElo() {
        elo = Math.round(elo);
    }
}