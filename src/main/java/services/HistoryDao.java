package services;

import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import models.GameResult;
import models.History;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static utils.Log.LOGGER;

public class HistoryDao {
    private final HikariDataSource ds;

    public HistoryDao(HikariDataSource ds) {
        this.ds = ds;
    }

    public static void createTable(Connection conn) throws SQLException {
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

    public static void createIndices(Connection conn) throws SQLException {
        var sql = """
            CREATE INDEX idxWhiteId ON histories(whiteId);
            CREATE INDEX idxBlackId ON histories(blackId);
            """;
        try (var stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
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
            var sql = "INSERT INTO histories (whiteId, blackId, result, data) VALUES (?, ?, ?, ?)";
            var stmt = conn.prepareStatement(sql);
            stmt.setString(1, historyInst.getWhiteId());
            stmt.setString(2, historyInst.getBlackId());
            stmt.setInt(3, historyInst.getResult().toInt());
            stmt.setString(4, historyInst.getData());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            throw e;
        }
    }

    public History getHistory(long id) throws SQLException  {
        try (var conn = ds.getConnection()) {
            var sql = """
                SELECT h.id as id, h.whiteId as whiteId, h.blackId as blackId, a1.username as whiteName, a2.username as blackName,
                    h.result as result, h.data as data, h.playedOn as playedOn FROM histories as h
                INNER JOIN accounts as a1
                ON a1.id = h.whiteId
                INNER JOIN accounts as a2
                ON a2.id = h.blackId
                WHERE h.id = ?
                """;

            try (var stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, id);

                var rs = stmt.executeQuery();
                return History.oneOfResults(rs);
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            throw e;
        }
    }

    public List<History> getHistories(String whiteId, String blackId, String cursor) throws SQLException {
        try (var conn = ds.getConnection()) {
            var sql = """
                SELECT h.id as id, h.whiteId as whiteId, h.blackId as blackId, a1.username as whiteName, a2.username as blackName,
                    h.result as result, h.data as data, h.playedOn as playedOn FROM histories as h
                INNER JOIN accounts as a1
                ON a1.id = h.whiteId
                INNER JOIN accounts as a2
                ON a2.id = h.blackId
                WHERE 1 = 1
                """;
            if (whiteId != null) {
                sql += " AND h.whiteId = ? ";
            }
            if (blackId != null) {
                sql += " AND h.blackId = ? ";
            }
            if (cursor != null) {
                sql += " AND h.id < ? ";
            }
            sql += " ORDER BY h.id DESC LIMIT 20 ";

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
                return History.manyOfResults(rs);
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            throw e;
        }
    }

    public List<History> getHistories(String accountId, String cursor) throws SQLException {
        assert accountId != null;

        try (var conn = ds.getConnection()) {
            // this query assumes that whiteId and blackId will be the same value, so we only need to join from whiteId
            var sql = """
               SELECT h.id as id, h.whiteId as whiteId, h.blackId as blackId, a1.username as whiteName, a2.username as blackName,
                    h.result as result, h.data as data, h.playedOn as playedOn FROM histories as h
                INNER JOIN accounts as a1
                ON a1.id = h.whiteId
                INNER JOIN accounts as a2
                ON a2.id = h.blackId
                WHERE h.whiteId = ? OR h.blackId = ?
                """;
            if (cursor != null) {
                sql += " AND h.id < ? ";
            }
            sql += " ORDER BY h.id DESC LIMIT 20 ";

            try (var stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, accountId);
                stmt.setString(2, accountId);
                if (cursor != null) {
                    stmt.setString(3, cursor);
                }

                var rs = stmt.executeQuery();
                return History.manyOfResults(rs);
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            throw e;
        }
    }
}