package services;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import models.GameState;
import models.Player;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.resps.Tuple;
import utils.Serializer;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static utils.Log.LOGGER;

public class RemoteDict {

    private static final Random RANDOM = new Random();
    private final JedisPooled jedis;
    private final ObjectMapper jsonMapper;
    private final ObjectReader playerReader;

    private static final String GAMES_ZSET = "games";
    private static final String LEADERBOARD_ZSET = "leaderboard";
    private static final Duration GAME_EXPIRE_FINISHED = Duration.ofHours(1);

    public RemoteDict(JedisPooled jedis, ObjectMapper jsonMapper) {
        this.jedis = jedis;
        this.jsonMapper = jsonMapper;
        this.playerReader = jsonMapper.readerFor(Player.class);
    }

    public GameState getGame(String id) {
        expireGames();

        var fullId = "game:" + id;
        var bytes = jedis.get(fullId.getBytes());
        if (bytes == null) {
            return null;
        }
        return Serializer.deserialize(bytes, GameState.class);
    }

    public GameState setGame(String id, GameState gameState) {
        double timeNanos = System.nanoTime();
        gameState.setTouch(timeNanos);

        var bytes = Serializer.serialize(gameState);
        var fullId = "game:" + id;

        var t = jedis.multi();
        t.set(fullId.getBytes(), bytes);
        t.zadd(GAMES_ZSET, timeNanos, fullId);
        t.exec();

        return gameState;
    }

    public void expireGames() {
        expireGames(GAME_EXPIRE_FINISHED.toNanos());
    }

    public void expireGames(long expireTimeNanos) {
        long timeNanos = System.nanoTime();
        long unixTimeExpireNanos = timeNanos - expireTimeNanos;
        var results = jedis.zrangeByScore(GAMES_ZSET, Double.NEGATIVE_INFINITY, unixTimeExpireNanos);
        var gameKeys = results.toArray(String[]::new);

        if (gameKeys.length > 0) {
            var t = jedis.multi();
            t.del(gameKeys);
            t.zrem(GAMES_ZSET, gameKeys);
            t.exec();
        }
    }

    @Data
    @AllArgsConstructor
    public static class GetGameKeysResult {
        private final Double nextCursor;
        private final List<String> keys;
    }

    public GetGameKeysResult getGameKeys(Double cursor, int count) {
        expireGames();

        cursor = cursor != null ? cursor : 0;
        var tuples = jedis.zrangeByScoreWithScores(GAMES_ZSET, cursor, Double.POSITIVE_INFINITY, 0, count + 1);

        // discard the last element, if we know for sure we over fetched, and use it as the next cursor
        Double nextCursor = null;
        if (tuples.size() >= count + 1) {
            nextCursor = tuples.remove(tuples.size() - 1).getScore();
        }

        var gameKeys = tuples.stream()
            .map(Tuple::getElement)
            .toList();
        return new GetGameKeysResult(nextCursor, gameKeys);
    }

    public Player getSession(String sessionId) {
        var fullId = "session:" + sessionId;
        var str = jedis.get(fullId);
        if (str == null) {
            return null;
        }
        try {
            return playerReader.readValue(str, Player.class);
        } catch (IOException ex) {
            LOGGER.info("Failed to parse json object from the dictionary " + ex);
            return null;
        }
    }

    public void setSession(String sessionId, Player player, long expirySeconds) {
        var fullId = "session:" + sessionId;
        try {
            var str = jsonMapper.writeValueAsString(player);
            jedis.setex(fullId, expirySeconds, str);
        } catch (IOException ex) {
            LOGGER.info("Failed to serialize an input json object to dictionary " + ex);
        }
    }

    public Player getSessionOrDefault(String sessionId) {
        Player player;
        if (sessionId != null) {
            player = getSession(sessionId);
        } else {
            var guestName = "Guest " + RANDOM.nextInt(1000);
            player = new Player(UUID.randomUUID().toString(), guestName);
        }
        return player;
    }

    public int getLeaderboardRank(String id) {
        return jedis.zrank(LEADERBOARD_ZSET, id).intValue() + 1;
    }

    @Data
    @AllArgsConstructor
    public static class RankedUser {
        String id;
        float elo;
        int rank;
    }

    public List<RankedUser> getLeaderboard(int startRank, int count) {
        var results = jedis.zrangeWithScores(LEADERBOARD_ZSET, startRank - 1, startRank - 2 + count);

        List<RankedUser> leaderboard = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            var tuple = results.get(i);
            leaderboard.add(new RankedUser(tuple.getElement(), (float) tuple.getScore(), startRank + i));
        }
        return leaderboard;
    }

    @Data
    @AllArgsConstructor
    public static class EloChangeSet {
        String id;
        double elo;
    }

    public void updateLeaderboardUser(EloChangeSet... changeSets) {
        var t = jedis.multi();
        for (var cs : changeSets) {
            t.zincrby(LEADERBOARD_ZSET, cs.elo, cs.id);
        }
        t.exec();
    }

    public void updateLeaderboardUser(String id, double elo) {
        jedis.zincrby(LEADERBOARD_ZSET, elo, id);
    }
}
