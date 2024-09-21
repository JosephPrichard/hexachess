package services;

import lombok.AllArgsConstructor;
import lombok.Data;
import models.History;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HistoryDao {
    private final QueryRunner runner;
    private final ResultSetHandler<History> historyMapper = new BeanHandler<>(History.class);
    private final ResultSetHandler<List<History>> historyListMapper = new BeanListHandler<>(History.class);

    public HistoryDao(DataSource ds) {
        runner = new QueryRunner(ds);
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
        runner.execute(sql);
    }

    public void createIndices() throws SQLException {
        var sql = """
            CREATE INDEX idxWhiteId ON histories(whiteId);
            CREATE INDEX idxBlackId ON histories(blackId);
            """;
        runner.execute(sql);
    }

    @Data
    @AllArgsConstructor
    public static class HistoryInst {
        private String whiteId;
        private String blackId;
        private int result;
        private String data;
    }

    public void insert(HistoryInst historyInst) throws SQLException  {
        var sql = "INSERT INTO histories (whiteId, blackId, result, data) VALUES (?, ?, ?, ?)";
        runner.execute(sql, historyInst.getWhiteId(), historyInst.getBlackId(), historyInst.getResult(), historyInst.getData());
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
        return runner.query(sql, historyMapper, id);
    }

    public List<History> getHistories(String whiteId, String blackId, String cursor) throws SQLException {
        var sql = new StringBuilder("""
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
            WHERE 1 = 1
            """);
        List<Object> params = new ArrayList<>();

        if (whiteId != null) {
            sql.append(" AND h.whiteId = ? ");
            params.add(whiteId);
        }
        if (blackId != null) {
            sql.append(" AND h.blackId = ? ");
            params.add(blackId);
        }
        if (cursor != null) {
            sql.append(" AND h.id < ? ");
            params.add(cursor);
        }
        sql.append("ORDER BY h.id DESC LIMIT 20");

        return runner.query(sql.toString(), historyListMapper, params.toArray());
    }

    public List<History> getHistories(String accountId, String cursor) throws SQLException {
        assert accountId != null;

        // this query assumes that whiteId and blackId will be the same value, so we only need to join from whiteId
        var sql = new StringBuilder("""
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
            WHERE h.whiteId = ? OR h.blackId = ?
            ORDER BY h.id DESC LIMIT 20
            """);
        List<Object> params = new ArrayList<>();

        params.add(accountId);
        params.add(accountId);
        if (cursor != null) {
            sql.append(" AND h.id < ? ");
            params.add(cursor);
        }

        return runner.query(sql.toString(), historyListMapper, params.toArray());
    }
}