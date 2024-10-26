package services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import domain.Hexagon;
import domain.Move;
import lombok.AllArgsConstructor;
import lombok.Data;
import models.GameState;
import models.HistEntity;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import utils.Globals;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static utils.Globals.JSON_MAPPER;

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
        Double winEloDiff;
        Double loseEloDiff;
        String data;
    }

    public void insert(HistoryInst historyInst) {
        insert(
            historyInst.getWhiteId(),
            historyInst.getBlackId(),
            historyInst.getResult(),
            historyInst.getWinEloDiff(),
            historyInst.getLoseEloDiff(),
            historyInst.getData());
    }

    public void insert(String whiteId, String blackId, int result, double winEloDiff, double loseEloDiff, String data) {
        var sql = """
            BEGIN;
            INSERT INTO game_histories (whiteId, blackId, result, data, winElo, loseElo) VALUES (?, ?, ?, ? ::json, ?, ?);
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
                h1.data,
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