package services;

import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static utils.Log.LOGGER;

public class HistoryDao {
    private final HikariDataSource ds;

    public HistoryDao(HikariDataSource ds) {
        this.ds = ds;
    }

    public void createTable(Connection conn) throws SQLException {
        var sql = """
            CREATE TABLE histories
                (id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                    whiteId VARCHAR, blackId VARCHAR, result INTEGER,
                    data VARCHAR, playedOn TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
            """;
        try (var stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    public void createIndices(Connection conn) {

    }

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

    @Getter
    @AllArgsConstructor
    public static class HistoryInst {
        private String whiteId;
        private String blackId;
        private GameResult result;
        private String data;
    }

    public void insert(HistoryInst historyInst) throws SQLException  {
        try (var conn = ds.getConnection()) {
            var stmt = conn.prepareStatement("INSERT INTO histories VALUES (?, ?, ?, ?)");
            stmt.setString(1, historyInst.getWhiteId());
            stmt.setString(2, historyInst.getBlackId());
            stmt.setInt(3, historyInst.getResult().toInt());
            stmt.setString(4, historyInst.getData());
            var rs = stmt.executeQuery();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            throw e;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class History {
        private String whiteId;
        private String blackId;
        private String whiteName;
        private String blackName;
        private GameResult result;
        private String data;
        private Timestamp playedOn;

        // reads from a result set that gets white and black as two different players
        public static List<History> fromResultSetTwoAccounts(ResultSet rs) throws SQLException {
            try (rs) {
                List<History> historyList = new ArrayList<>();
                while (rs.next()) {
                    var whiteId = rs.getString("whiteId");
                    var blackId = rs.getString("blackId");
                    var whiteName = rs.getString("whiteName");
                    var blackName = rs.getString("blackName");
                    var result = rs.getInt("result");
                    var data = rs.getString("data");
                    var playedOn = rs.getTimestamp("playedOn");
                    historyList.add(new History(whiteId, blackId, whiteName,
                        blackName, GameResult.fromInt(result), data, playedOn));
                }
                return historyList;
            }
        }

        // reads from a result set that gets white and black as the same player
        public static List<History> fromResultSetOneAccount(ResultSet rs) throws SQLException {
            try (rs) {
                List<History> historyList = new ArrayList<>();
                while (rs.next()) {
                    var accountId = rs.getString("accountId");
                    var username = rs.getString("username");
                    var result = rs.getInt("result");
                    var data = rs.getString("data");
                    var playedOn = rs.getTimestamp("playedOn");
                    historyList.add(new History(accountId, accountId, username,
                        username, GameResult.fromInt(result), data, playedOn));
                }
                return historyList;
            }
        }
    }

    private static String createGetHistoriesQuery(String whiteId, String blackId, String cursor) {
        var sql = """
            SELECT h.whiteId as whiteId, h.blackId as blackId, a1.username as whiteId,
                a2.username as blackId, h.result as result, h.data as data, h.playedOn as playedOn FROM histories as h
            INNER JOIN accounts as a1
            ON a1.id = h.whiteId
            INNER JOIN accounts as a2
            ON a2.id = h.whiteId
            WHERE 1 = 1
            """;
        if (whiteId != null) {
            sql += "AND h.whiteId = ? ";
        }
        if (blackId != null) {
            sql += "AND h.blackId = ? ";
        }
        if (cursor != null) {
            sql += "AND h.id < ? ";
        }
        sql += "ORDER BY h.id DESC LIMIT 20";
        return sql;
    }

    public List<History> getHistories(String whiteId, String blackId, String cursor) throws SQLException {
        try (var conn = ds.getConnection()) {
            var sql = createGetHistoriesQuery(whiteId, blackId, cursor);

            try (var stmt = conn.prepareStatement(sql)) {
                int index = 1;
                if (whiteId != null) {
                    stmt.setString(index++, whiteId);
                }
                if (blackId != null) {
                    stmt.setString(index++, blackId);
                }
                if (cursor != null) {
                    stmt.setString(index, cursor);
                }

                var rs = stmt.executeQuery();
                return History.fromResultSetTwoAccounts(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            throw e;
        }
    }

    private static String createGetHistoriesQuery(String cursor) {
        var sql = """
            SELECT h.whiteId as accountId, a.username as username, h.result as result, h.data as data, h.playedOn as playedOn FROM histories
            INNER JOIN accounts as a
            ON a.id = h.whiteId
            WHERE h.whiteId = ? OR h.blackId = ?
            """;
        if (cursor != null) {
            sql += "AND h.id < ? ";
        }
        sql += "ORDER BY h.id DESC LIMIT 20";
        return sql;
    }

    public List<History> getHistories(String accountId, String cursor) throws SQLException {
        assert accountId != null;

        try (var conn = ds.getConnection()) {
            var sql = createGetHistoriesQuery(cursor);

            try (var stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, accountId);
                stmt.setString(2, accountId);
                if (cursor != null) {
                    stmt.setString(3, cursor);
                }

                var rs = stmt.executeQuery();
                return History.fromResultSetOneAccount(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            throw e;
        }
    }
}