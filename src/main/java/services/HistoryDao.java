package services;

import lombok.AllArgsConstructor;
import lombok.Data;
import models.HistEntity;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HistoryDao {

    private static final ResultSetHandler<HistEntity> HIST_MAPPER = new BeanHandler<>(HistEntity.class);
    private static final ResultSetHandler<List<HistEntity>> HIST_LIST_MAPPER = new BeanListHandler<>(HistEntity.class);

    private final QueryRunner runner;

    public HistoryDao(DataSource ds) {
        runner = new QueryRunner(ds);
    }

    @Data
    @AllArgsConstructor
    public static class HistoryInst {
        String whiteId;
        String blackId;
        int result;
        byte[] data;
        Double winEloDiff;
        Double loseEloDiff;
    }

    public void insert(HistoryInst historyInst) {
        insert(
            historyInst.getWhiteId(), historyInst.getBlackId(),
            historyInst.getResult(), historyInst.getData(),
            historyInst.getWinEloDiff(), historyInst.getLoseEloDiff());
    }

    public void insert(String whiteId, String blackId, int result, byte[] data, double winEloDiff, double loseEloDiff) {
        var sql = """
            BEGIN;
            INSERT INTO
                game_histories (whiteId, blackId, result, data, winElo, loseElo)
                VALUES (?, ?, ?, ?, ?, ?);
            END""";
        try {
            runner.execute(sql, whiteId, blackId, result, data, winEloDiff, loseEloDiff);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public HistEntity getHistory(long id) {
        var sql = """
            SELECT
                h1.id,
                h1.whiteId,
                h1.blackId,
                u1.username as whiteName,
                u2.username as blackName,
                u1.country as whiteCountry,
                u2.country as blackCountry,
                h1.result,
                h1.playedOn,
                h1.winElo,
                h1.loseElo
            FROM game_histories as h1
            INNER JOIN users as u1 ON u1.id = h1.whiteId
            INNER JOIN users as u2 ON u2.id = h1.blackId
            WHERE h1.id = ?""";
        try {
            return runner.query(sql, HIST_MAPPER, id);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<HistEntity> getUserHistories(String userId, Long afterId, int perPage) {
        if (userId == null) {
            throw new RuntimeException("Expected userId to be non null");
        }

        var sql = """
            SELECT
                h1.id,
                h1.whiteId,
                h1.blackId,
                u1.username as whiteName,
                u2.username as blackName,
                u1.country as whiteCountry,
                u2.country as blackCountry,
                h1.result,
                h1.playedOn,
                h1.winElo,
                h1.loseElo
            FROM game_histories as h1
            INNER JOIN users as u1 ON u1.id = h1.whiteId
            INNER JOIN users as u2 ON u2.id = h1.blackId
            WHERE (h1.whiteId = ? OR h1.blackId = ?)""";
        List<Object> params = new ArrayList<>();

        params.add(userId);
        params.add(userId);

        if (afterId != null) {
            sql += " AND h1.id < ?";
            params.add(afterId);
        }
        sql += " ORDER BY h1.id DESC LIMIT ?";
        params.add(perPage);

        try {
            return runner.query(sql, HIST_LIST_MAPPER, params.toArray());
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
}