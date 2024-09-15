package model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Getter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
public class History {

    private long id;
    private String whiteId;
    private String blackId;
    private String whiteName;
    private String blackName;
    private GameResult result;
    private String data;
    @EqualsAndHashCode.Exclude
    private Timestamp playedOn;

    public static History ofResult(ResultSet rs) throws SQLException {
        var id = rs.getLong("id");
        var whiteId = rs.getString("whiteId");
        var blackId = rs.getString("blackId");
        var whiteName = rs.getString("whiteName");
        var blackName = rs.getString("blackName");
        var result = rs.getInt("result");
        var data = rs.getString("data");
        var playedOn = rs.getTimestamp("playedOn");
        return new History(id, whiteId, blackId, whiteName, blackName, GameResult.fromInt(result), data, playedOn);
    }

    public static History oneOfResults(ResultSet rs) throws SQLException {
        try (rs) {
            if (rs.next()) {
                return History.ofResult(rs);
            }
            return null;
        }
    }

    public static List<History> manyOfResults(ResultSet rs) throws SQLException {
        try (rs) {
            List<History> historyList = new ArrayList<>();
            while (rs.next()) {
                var history = ofResult(rs);
                historyList.add(history);
            }
            return historyList;
        }
    }
}