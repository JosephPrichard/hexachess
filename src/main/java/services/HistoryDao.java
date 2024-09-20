package services;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import models.GameResult;
import models.History;
import models.Stats;
import org.intellij.lang.annotations.Language;
import utils.Database;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

public class HistoryDao {
    private final DataSource ds;

    public HistoryDao(DataSource ds) {
        this.ds = ds;
    }

    public void createTable() throws SQLException {
        var sql = """
            CREATE TABLE histories (
                id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                whiteId VARCHAR,
                blackId VARCHAR,
                result INTEGER,
                data VARCHAR,
                playedOn TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
            """;
        Database.executeUpdate(ds, sql);
    }

    public void createIndices() throws SQLException {
        var sql = """
            CREATE INDEX idxWhiteId ON histories(whiteId);
            CREATE INDEX idxBlackId ON histories(blackId);
            """;
        Database.executeUpdate(ds, sql);
    }

    @Data
    @AllArgsConstructor
    public static class HistoryInst {
        private String whiteId;
        private String blackId;
        private GameResult result;
        private String data;
    }

    public void insert(HistoryInst historyInst) throws SQLException  {
        var sql = "INSERT INTO histories (whiteId, blackId, result, data) VALUES (?, ?, ?, ?)";
        Database.executeUpdate(ds, sql, (stmt) -> {
            stmt.setString(1, historyInst.getWhiteId());
            stmt.setString(2, historyInst.getBlackId());
            stmt.setInt(3, historyInst.getResult().toInt());
            stmt.setString(4, historyInst.getData());
        });
    }

    public History getHistory(long id) throws SQLException  {
        var sql = """
            SELECT
                h.id as id,
                h.whiteId as whiteId,
                h.blackId as blackId,
                a1.username as whiteName,
                a2.username as blackName,
                h.result as result,
                h.data as data,
                h.playedOn as playedOn
            FROM histories as h
            INNER JOIN accounts as a1
            ON a1.id = h.whiteId
            INNER JOIN accounts as a2
            ON a2.id = h.blackId
            WHERE h.id = ?
            """;
        return Database.executeQuery(ds, sql, (stmt) -> {
            stmt.setLong(1, id);

            var rs = stmt.executeQuery();
            return Database.oneOfResults(rs, History::ofResult);
        });
    }

    public List<History> getHistories(String whiteId, String blackId, String cursor) throws SQLException {
        var sql = String.format("""
            SELECT
                h.id as id,
                h.whiteId as whiteId,
                h.blackId as blackId,
                a1.username as whiteName,
                a2.username as blackName,
                h.result as result,
                h.data as data,
                h.playedOn as playedOn
            FROM histories as h
            INNER JOIN accounts as a1
            ON a1.id = h.whiteId
            INNER JOIN accounts as a2
            ON a2.id = h.blackId
            WHERE 1 = 1 %s %s %s
            ORDER BY h.id DESC LIMIT 20
            """,
            whiteId != null ? " AND h.whiteId = ? " : "",
            blackId != null ? " AND h.blackId = ? " : "",
            cursor != null ? " AND h.id < ? " : "");

        return Database.executeQuery(ds, sql, (stmt) -> {
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
            return Database.manyOfResults(rs, History::ofResult);
        });
    }

    public List<History> getHistories(String accountId, String cursor) throws SQLException {
        assert accountId != null;

        // this query assumes that whiteId and blackId will be the same value, so we only need to join from whiteId
        var sql = String.format("""
            SELECT
                h.id as id,
                h.whiteId as whiteId,
                h.blackId as blackId,
                a1.username as whiteName,
                a2.username as blackName,
                h.result as result,
                h.data as data,
                h.playedOn as playedOn
            FROM histories as h
            INNER JOIN accounts as a1
            ON a1.id = h.whiteId
            INNER JOIN accounts as a2
            ON a2.id = h.blackId
            WHERE h.whiteId = ? OR h.blackId = ? %s
            ORDER BY h.id DESC LIMIT 20
            """,
            cursor != null ? " AND h.id < ? " : "");

        return Database.executeQuery(ds, sql, (stmt) -> {
            stmt.setString(1, accountId);
            stmt.setString(2, accountId);
            if (cursor != null) {
                stmt.setString(3, cursor);
            }

            var rs = stmt.executeQuery();
            return Database.manyOfResults(rs, History::ofResult);
        });
    }
}