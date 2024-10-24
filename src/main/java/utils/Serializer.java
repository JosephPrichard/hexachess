package utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import domain.ChessBoard;
import domain.ChessGame;
import domain.Hexagon;
import domain.PieceMoves;
import models.GameState;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static utils.Log.LOGGER;

public class Serializer {

    private static final ThreadLocal<Kryo> KRYO = ThreadLocal.withInitial(() -> {
        var kryo = new Kryo();
        kryo.register(Hexagon.class);
        kryo.register(PieceMoves.class);
        kryo.register(ChessBoard.class);
        kryo.register(ChessGame.class);
        kryo.register(GameState.class);
        return kryo;
    });

    public static Kryo get() {
        return KRYO.get();
    }

    public static <T> byte[] serialize(T obj) {
        try {
            var rawBytes = new ByteArrayOutputStream();
            try (var output = new Output(rawBytes)) {
                var kryo = Serializer.get();
                kryo.writeObject(output, obj);
            }
            return rawBytes.toByteArray();
        } catch (Exception ex) {
            LOGGER.error("Error occurred while deserializing: " + ex);
            throw new RuntimeException(ex);
        }
    }

    public static <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try {
            var rawBytes = new ByteArrayInputStream(bytes);
            try (var input = new Input(rawBytes)) {
                var kryo = Serializer.get();
                return kryo.readObject(input, clazz);
            }
        } catch (Exception ex) {
            LOGGER.error("Error occurred while deserializing: " + ex);
            throw new RuntimeException(ex);
        }
    }

    public static <T> T deserialize(String str, Class<T> clazz) {
        return deserialize(str.getBytes(StandardCharsets.UTF_8), clazz);
    }
}
