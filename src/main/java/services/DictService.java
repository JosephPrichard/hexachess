package services;

import lombok.AllArgsConstructor;
import lombok.Data;
import models.Duel;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.resps.Tuple;
import utils.Serializer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

public class DictService {

    private static final Random RANDOM = new Random();
    private final JedisPooled jedis;

    private static final String DUELS_ZSET = "duels";
    private static final String PLAYERS_ZSET = "players";
    private static final byte[] PLAYERS_ZSET_BYTES = PLAYERS_ZSET.getBytes();
    private static final Duration GAME_EXPIRE_FINISHED = Duration.ofHours(1);
    private static final Duration PLAYER_EXPIRE = Duration.ofMinutes(15);

    public DictService(JedisPooled jedis) {
        this.jedis = jedis;
    }

    public Duel getDuel(String id) {
        var fullId = "duel:" + id;
        var bytes = jedis.get(fullId.getBytes());
        if (bytes == null) {
            return null;
        }
        return Serializer.deserialize(bytes, Duel.class);
    }

    public void setDuel(String id, Duel duel) {
        double timeMs = System.currentTimeMillis();
        duel.setTouch(timeMs);

        var bytes = Serializer.serialize(duel);
        var fullId = "duel:" + id;

        var transaction = jedis.multi();
        transaction.set(fullId.getBytes(), bytes);
        transaction.zadd(DUELS_ZSET, timeMs, fullId);
        transaction.exec();
    }

    public void expireDuels() {
        expireDuels(GAME_EXPIRE_FINISHED.toMillis());
    }

    public void expireDuels(long expireTimeMs) {
        long timeMs = System.currentTimeMillis();
        long unixTimeExpireMs = timeMs - expireTimeMs;
        var results = jedis.zrange(DUELS_ZSET, Long.MIN_VALUE, unixTimeExpireMs);
        var duelKeys = results.toArray(String[]::new);

        var transaction = jedis.multi();
        transaction.del(duelKeys);
        transaction.zrem(DUELS_ZSET, duelKeys);
        transaction.exec();
    }

    @Data
    @AllArgsConstructor
    public static class GetDuelKeysResult {
        private final Double nextCursor;
        private final List<String> duelKeys;
    }

    public GetDuelKeysResult getDuelKeys(Double cursor, int count) {
        cursor = cursor != null ? cursor : 0;
        var tuples = jedis.zrangeByScoreWithScores(DUELS_ZSET, cursor, Double.POSITIVE_INFINITY, 0, count + 1);

        // discard the last element, if we know for sure we over fetched, and use it as the next cursor
        Double nextCursor = null;
        if (tuples.size() >= count + 1) {
            nextCursor = tuples.remove(tuples.size() - 1).getScore();
        }

        var duelKeys = tuples.stream()
            .map(Tuple::getElement)
            .toList();

        return new GetDuelKeysResult(nextCursor, duelKeys);
    }

    public Duel.Player getSession(String sessionId) {
        var fullId = "session:" + sessionId;
        var bytes = jedis.get(fullId.getBytes());
        if (bytes == null) {
            return null;
        }
        return Serializer.deserialize(bytes, Duel.Player.class);
    }

    public void setSession(String sessionId, Duel.Player player, long expirySeconds) {
        var bytes = Serializer.serialize(player);
        var fullId = "session:" + sessionId;
        jedis.setex(fullId.getBytes(), expirySeconds, bytes);
    }

    public Duel.Player getSessionOrDefault(String sessionId) {
        Duel.Player player;
        if (sessionId != null) {
            player = getSession(sessionId);
        } else {
            var guestName = "Guest " + RANDOM.nextInt(1000);
            player = new Duel.Player(UUID.randomUUID().toString(), guestName);
        }
        return player;
    }

    // get all players and lazily update as we need (we expect this sorted set to be small)
    public List<Duel.Player> retrieveAndAppend(Duel.Player player, double maxAgeMs) {
        double timeMs = System.currentTimeMillis();

        // perform another operation to actually get the results
        var results = jedis.zrange(PLAYERS_ZSET_BYTES, 0, -1);
        var players = results.stream()
            .map((bytes) -> Serializer.deserialize(bytes, Duel.Player.class))
            .collect(Collectors.toCollection(ArrayList::new));

        var transaction = jedis.multi();

        // only add the player if it doesn't exist yet...
        if (player != null && !players.contains(player)) {
            var bytes = Serializer.serialize(player);
            transaction.zadd(PLAYERS_ZSET_BYTES, timeMs, bytes);
            players.add(player);
        }

        // remove all keys with a unix timestamp smaller than the unix timestamp 15 minutes ago
        var unixTimeExpireMs = timeMs - maxAgeMs;
        transaction.zremrangeByScore(PLAYERS_ZSET, "-INF", Double.toString(unixTimeExpireMs));

        // let's execute the transaction some other time
        transaction.exec();

        return players;
    }

    public List<Duel.Player> retrieveAndAppend(Duel.Player player) {
        return retrieveAndAppend(player, PLAYER_EXPIRE.toMillis());
    }
}