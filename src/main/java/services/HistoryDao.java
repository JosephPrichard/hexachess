package services;

import models.HistoryEntity;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HistoryDao {

    private static final ResultSetHandler<HistoryEntity> HIST_MAPPER = new BeanHandler<>(HistoryEntity.class);
    private static final ResultSetHandler<List<HistoryEntity>> HIST_LIST_MAPPER = new BeanListHandler<>(HistoryEntity.class);

    private final QueryRunner runner;

    public HistoryDao(DataSource ds) {
        runner = new QueryRunner(ds);
    }

    public void createTable() {
        try {
            var sql = """
            BEGIN;
                CREATE TABLE histories (
                    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                    whiteId VARCHAR NOT NULL,
                    blackId VARCHAR NOT NULL,
                    result INTEGER NOT NULL,
                    data BYTEA NOT NULL,
                    playedOn TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    winElo NUMERIC NOT NULL,
                    loseElo NUMERIC NOT NULL);
                    
                CREATE INDEX idxWhiteId ON histories(whiteId);
                CREATE INDEX idxBlackId ON histories(blackId);
            END;""";
            runner.execute(sql);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void insert(String whiteId, String blackId, int result, byte[] data, double winElo, double loseElo) {
        try {
            var sql = "BEGIN; INSERT INTO histories (whiteId, blackId, result, data, winElo, loseElo) VALUES (?, ?, ?, ?, ?, ?); END";
            runner.execute(sql, whiteId, blackId, result, data, winElo, loseElo);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public HistoryEntity getHistory(long id) {
        try {
            var sql = """
            SELECT h.id, h.whiteId, h.blackId,
                u1.username as whiteName, u2.username as blackName,
                h.result, h.data, h.playedOn, h.winElo, h.loseElo
            FROM histories as h
            INNER JOIN users as u1
            ON u1.id = h.whiteId
            INNER JOIN users as u2
            ON u2.id = h.blackId
            WHERE h.id = ?""";
            return runner.query(sql, HIST_MAPPER, id);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<HistoryEntity> getHistories(String whiteId, String blackId, String cursor) {
        try {
            var sql = new StringBuilder("""
            SELECT h.id, h.whiteId, h.blackId,
                u1.username as whiteName, u2.username as blackName,
                h.result, h.data, h.playedOn, h.winElo, h.loseElo
            FROM histories as h
            INNER JOIN users as u1
            ON u1.id = h.whiteId
            INNER JOIN users as u2
            ON u2.id = h.blackId
            WHERE 1 = 1""");
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

            return runner.query(sql.toString(), HIST_LIST_MAPPER, params.toArray());
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<HistoryEntity> getHistories(String accountId, String cursor) {
        try {
            assert accountId != null;

            // this query assumes that whiteId and blackId will be the same value, so we only need to join from whiteId
            var sql = new StringBuilder("""
            SELECT h.id, h.whiteId, h.blackId,
                u1.username as whiteName, u2.username as blackName,
                h.result, h.data, h.playedOn, h.winElo, h.loseElo
            FROM histories as h
            INNER JOIN users as u1
            ON u1.id = h.whiteId
            INNER JOIN users as u2
            ON u2.id = h.blackId
            WHERE h.whiteId = ? OR h.blackId = ?
            ORDER BY h.id DESC LIMIT 20""");
            List<Object> params = new ArrayList<>();

            params.add(accountId);
            params.add(accountId);
            if (cursor != null) {
                sql.append(" AND h.id < ? ");
                params.add(cursor);
            }

            return runner.query(sql.toString(), HIST_LIST_MAPPER, params.toArray());
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
}