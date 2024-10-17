package services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.AllArgsConstructor;
import lombok.Data;
import models.GameState;
import models.Player;
import models.RankedUser;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.resps.Tuple;
import utils.Serializer;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

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

    public List<GameState> getGames(Stream<String> ids) {
        expireGames();

        var fullIds = ids.map(id -> ("game:" + id).getBytes()).toList();

        var bytesList = jedis.mget(fullIds.toArray(byte[][]::new));
        if (bytesList == null) {
            return null;
        }

        return bytesList.stream().map((bytes) -> Serializer.deserialize(bytes, GameState.class)).toList();
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
    public static class GetGamesResult {
        Double nextCursor;
        List<GameState> gameStates;
    }

    public GetGamesResult getGames(Double cursor, int count) {
        expireGames();

        cursor = cursor != null ? cursor : 0;
        var tuples = jedis.zrangeByScoreWithScores(GAMES_ZSET, cursor, Double.POSITIVE_INFINITY, 0, count + 1);

        // discard the last element, if we know for sure we over fetched, and use it as the next cursor
        Double nextCursor = null;
        if (tuples.size() >= count + 1) {
            nextCursor = tuples.remove(tuples.size() - 1).getScore();
        }

        var gameStates = getGames(tuples.stream().map(Tuple::getElement));
        return new GetGamesResult(nextCursor, gameStates);
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
    public static class Leaderboard {
        List<RankedUser> users;
        int pageCount;
    }

    public Leaderboard getLeaderboard(int startRank, int count) {
        var p = jedis.pipelined();
        var idsResp = p.zrange(LEADERBOARD_ZSET, startRank, startRank - 1 + count);
        var elemCountResp = p.zcount(LEADERBOARD_ZSET, Integer.MIN_VALUE, Integer.MAX_VALUE);
        p.sync();

        var ids = idsResp.get();
        var totalCount = elemCountResp.get().intValue();
        var pageCount = totalCount / count;

        List<RankedUser> users = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            var id = ids.get(i);
            users.add(new RankedUser(id, startRank + i + 1));
        }
        return new Leaderboard(users, pageCount);
    }

    public Leaderboard getLeaderboardPage(int page, int perPage) {
        page = Math.max(page, 1);
        var offset = (page - 1) * perPage;
        return getLeaderboard(offset, perPage);
    }

    @Data
    @AllArgsConstructor
    public static class EloChangeSet {
        String id;
        double elo;
    }

    public void incrLeaderboardUser(EloChangeSet... changeSets) {
        var t = jedis.multi();
        for (var cs : changeSets) {
            t.zincrby(LEADERBOARD_ZSET, cs.elo, cs.id);
        }
        t.exec();
    }

    public void incrLeaderboardUser(String id, double elo) {
        jedis.zincrby(LEADERBOARD_ZSET, elo, id);
    }

    public void updateLeaderboardUser(EloChangeSet... changeSets) {
        var t = jedis.multi();
        for (var cs : changeSets) {
            t.zadd(LEADERBOARD_ZSET, cs.elo, cs.id);
        }
        t.exec();
    }
}
