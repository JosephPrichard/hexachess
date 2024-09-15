package model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Getter
@EqualsAndHashCode
@ToString
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

    public static Stats oneOfResults(ResultSet rs) throws SQLException {
        try (rs) {
            if (rs.next()) {
                return Stats.ofResult(rs);
            }
            return null;
        }
    }

    public static List<Stats> manyOfResults(ResultSet rs) throws SQLException {
        try (rs) {
            List<Stats> statList = new ArrayList<>();
            while (rs.next()) {
                var stats = Stats.ofResult(rs);
                statList.add(stats);
            }
            return statList;
        }
    }
}