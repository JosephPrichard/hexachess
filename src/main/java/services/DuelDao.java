package services;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.AllArgsConstructor;
import lombok.Getter;
import model.Duel;
import redis.clients.jedis.JedisPooled;
import utils.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

public class DuelDao {

    @Getter
    @AllArgsConstructor
    public static class ScanResult {
        private String cursor;
        private List<Duel> results;
    }

    private final JedisPooled jedis;

    public DuelDao(JedisPooled jedis) {
        this.jedis = jedis;
    }

    public static Duel readDuel(byte[] bytes) {
        var rawBytes = new ByteArrayInputStream(bytes);
        try (var input = new Input(rawBytes)) {
            var kryo = Serializer.get();
            return kryo.readObject(input, Duel.class);
        }
    }

    public Duel getDuel(String id) {
        var bytes = jedis.get(id.getBytes());
        return readDuel(bytes);
    }

    public void setDuel(String id, Duel duel) {
        var rawBytes = new ByteArrayOutputStream();
        try (var output = new Output(rawBytes)) {
            var kryo = Serializer.get();
            kryo.writeObject(output, duel);

            jedis.set(id.getBytes(), rawBytes.toByteArray());
        }
    }

    public Duel.Player getPlayer(String sessionId) {
        var bytes = jedis.get(sessionId.getBytes());
        var rawBytes = new ByteArrayInputStream(bytes);
        try (var input = new Input(rawBytes)) {
            var kryo = Serializer.get();
            return kryo.readObject(input, Duel.Player.class);
        }
    }

    public void setPlayer(String sessionId, Duel.Player player) {
        var rawBytes = new ByteArrayOutputStream();
        try (var output = new Output(rawBytes)) {
            var kryo = Serializer.get();
            kryo.writeObject(output, player);

            // expires within 4 hours of inactivity
            jedis.setex(sessionId, 60 * 60 * 4, rawBytes.toString());
        }
    }

    public ScanResult scanDuels(String cursor) {
        var result = jedis.scan(cursor != null ? cursor.getBytes() : "0".getBytes());
        var duelResults = result.getResult()
            .stream()
            .map(DuelDao::readDuel)
            .toList();
        return new ScanResult(result.getCursor(), duelResults);
    }
}