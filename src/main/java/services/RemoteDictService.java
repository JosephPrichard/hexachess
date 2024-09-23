package services;


import models.Duel;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.resps.Tuple;
import utils.Serializer;

import java.time.Duration;
import java.util.Random;
import java.util.UUID;

public class RemoteDictService implements DictService {
    private static final Random RANDOM = new Random();
    private final JedisPooled jedis;

    private static final String DUELS_ZSET = "duels";
    private static final Duration GAME_EXPIRE_FINISHED = Duration.ofHours(1);

    public RemoteDictService(JedisPooled jedis) {
        this.jedis = jedis;
    }

    public Duel getDuel(String id) {
        expireDuels();

        var fullId = "duel:" + id;
        var bytes = jedis.get(fullId.getBytes());
        if (bytes == null) {
            return null;
        }
        return Serializer.deserialize(bytes, Duel.class);
    }

    public Duel setDuel(String id, Duel duel) {
        double timeNanos = System.nanoTime();
        duel.setTouch(timeNanos);

        var bytes = Serializer.serialize(duel);
        var fullId = "duel:" + id;

        var transaction = jedis.multi();
        transaction.set(fullId.getBytes(), bytes);
        transaction.zadd(DUELS_ZSET, timeNanos, fullId);
        transaction.exec();

        return duel;
    }

    public void expireDuels() {
        expireDuels(GAME_EXPIRE_FINISHED.toNanos());
    }

    public void expireDuels(long expireTimeNanos) {
        long timeNanos = System.nanoTime();
        long unixTimeExpireNanos = timeNanos - expireTimeNanos;
        var results = jedis.zrangeByScore(DUELS_ZSET, Double.NEGATIVE_INFINITY, unixTimeExpireNanos);
        var duelKeys = results.toArray(String[]::new);

        if (duelKeys.length > 0) {
            var transaction = jedis.multi();
            transaction.del(duelKeys);
            transaction.zrem(DUELS_ZSET, duelKeys);
            transaction.exec();
        }
    }

    public DictService.GetDuelKeysResult getDuelKeys(Double cursor, int count) {
        expireDuels();

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
        return new DictService.GetDuelKeysResult(nextCursor, duelKeys);
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
}
