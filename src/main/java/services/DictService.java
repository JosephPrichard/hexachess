package services;

import lombok.AllArgsConstructor;
import lombok.Getter;
import models.Duel;
import redis.clients.jedis.JedisPooled;
import utils.Serializer;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

public class DictService {

    private static final Random RANDOM = new Random();
    private final JedisPooled jedis;

    @Getter
    @AllArgsConstructor
    public static class ScanResult {
        private String cursor;
        private List<Duel> results;
    }

    public DictService(JedisPooled jedis) {
        this.jedis = jedis;
    }

    public Duel getDuel(String id) {
        var bytes = jedis.get(id.getBytes());
        if (bytes == null) {
            return null;
        }
        return Serializer.deserialize(bytes, Duel.class);
    }

    public void setDuel(String id, Duel duel) {
        var bytes = Serializer.serialize(duel);
        jedis.set(id.getBytes(), bytes);
    }

    public ScanResult scanDuels(String cursor) {
        var result = jedis.scan(cursor != null ? cursor.getBytes() : "0".getBytes());
        var duelResults = result.getResult()
            .stream()
            .map((bytes) -> Serializer.deserialize(bytes, Duel.class))
            .toList();
        return new ScanResult(result.getCursor(), duelResults);
    }

    public Duel.Player getSession(String sessionId) {
        var bytes = jedis.get(sessionId.getBytes());
        if (bytes == null) {
            return null;
        }
        return Serializer.deserialize(bytes, Duel.Player.class);
    }

    public void setSession(String sessionId, Duel.Player player) {
        var bytes = Serializer.serialize(player);
        // expires within 4 hours of inactivity
        jedis.setex(sessionId.getBytes(), 60 * 60 * 4, bytes);
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

    private static final String PLAYERS_KEY = "players";
    private static final byte[] PLAYERS_KEY_BYTES = PLAYERS_KEY.getBytes();

    // get all players and lazily update as we need (we expect this sorted set to be small)
    public List<Duel.Player> getThenAddPlayers(Duel.Player player) {
        var unixTimeNow = System.currentTimeMillis() / 1000d;

        // remove all keys with a unix timestamp smaller than the unix timestamp 15 minutes ago
        var unixTimeExpire = unixTimeNow - 900000; // current time minus 15 minutes in milliseconds
        jedis.zremrangeByScore(PLAYERS_KEY, "-INF", Double.toString(unixTimeExpire));

        // perform another operation to actually get the results
        var results = jedis.zrange(PLAYERS_KEY_BYTES, 0, -1);
        var players = results.stream()
            .map((bytes) -> Serializer.deserialize(bytes, Duel.Player.class))
            .collect(Collectors.toCollection(ArrayList::new));

        // only add the player if it doesn't exist yet...
        if (player != null && !players.contains(player)) {
            var bytes = Serializer.serialize(player);
            jedis.zadd(PLAYERS_KEY_BYTES, unixTimeNow, bytes);
            players.add(player);
        }
        return players;
    }
}