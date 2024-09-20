package models;

import lombok.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class Stats {
    private String id;
    private String username;
    private float elo;
    private int wins;
    private int losses;

    public static Stats ofResult(ResultSet rs) throws SQLException {
        var id = rs.getString("id");
        var username = rs.getString("username");
        var elo = rs.getFloat("elo");
        var wins = rs.getInt("wins");
        var losses = rs.getInt("losses");
        return new Stats(id, username, elo, wins, losses);
    }
}