package services;

import lombok.AllArgsConstructor;
import lombok.Data;
import models.HistoryEntity;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static utils.Globals.LOGGER;

public class HistoryDao {

    private static final ResultSetHandler<HistoryEntity> HIST_MAPPER = new BeanHandler<>(HistoryEntity.class);
    private static final ResultSetHandler<List<HistoryEntity>> HIST_LIST_MAPPER = new BeanListHandler<>(HistoryEntity.class);

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
        Double winEloDiff;
        Double loseEloDiff;
        String data;
    }

    public void insert(String whiteId, String blackId, int result, double winEloDiff, double loseEloDiff, String data) {
        insert(new HistoryInst(whiteId, blackId, result, winEloDiff, loseEloDiff, data));
    }

    public void insert(HistoryInst historyInst) {
        var sql = """
            BEGIN;
            INSERT INTO game_histories (whiteId, blackId, result, data, winElo, loseElo) VALUES (?, ?, ?, ? ::json, ?, ?);
            END""";
        try {
            runner.execute(sql, historyInst.whiteId, historyInst.blackId,
                historyInst.result, historyInst.data, historyInst.winEloDiff, historyInst.loseEloDiff);
            LOGGER.info("Successfully inserted a history={}", historyInst);
        } catch (SQLException ex) {
            LOGGER.error("Failed to insert a history={}", historyInst);
            throw new RuntimeException(ex);
        }
    }

    public HistoryEntity getHistory(long id) {
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
                h1.data,
                h1.playedOn,
                h1.winElo,
                h1.loseElo
            FROM game_histories as h1
            INNER JOIN users as u1 ON u1.id = h1.whiteId
            INNER JOIN users as u2 ON u2.id = h1.blackId
            WHERE h1.id = ?""";
        try {
            var results = runner.query(sql, HIST_MAPPER, id);
            LOGGER.info("Successfully selected history for id={}", id);
            return results;
        } catch (SQLException ex) {
            LOGGER.error("Failed to select history for id={}", id);
            throw new RuntimeException(ex);
        }
    }

    public List<HistoryEntity> getUserHistories(String userId, Long afterId, int perPage) {
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
            var results = runner.query(sql, HIST_LIST_MAPPER, params.toArray());
            LOGGER.info("Successfully selected user histories page for userId={}, afterId={}", userId, afterId);
            return results;
        } catch (SQLException ex) {
            LOGGER.info("Failed to select user histories page for userId={}, afterId={}", userId, afterId);
            throw new RuntimeException(ex);
        }
    }
}