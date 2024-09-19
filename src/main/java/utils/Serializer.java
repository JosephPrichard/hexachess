package utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import domain.ChessBoard;
import domain.ChessGame;
import domain.Hexagon;
import models.Duel;
import services.BroadcastService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class Serializer {

    private static final ThreadLocal<Kryo> KRYO = ThreadLocal.withInitial(() -> {
        var kryo = new Kryo();
        kryo.register(Hexagon.class);
        kryo.register(Hexagon.PieceMoves.class);
        kryo.register(ChessBoard.class);
        kryo.register(ChessGame.class);
        kryo.register(Duel.class);
        kryo.register(Duel.Player.class);
        kryo.register(BroadcastService.Message.class);
        return kryo;
    });

    public static Kryo get() {
        return KRYO.get();
    }

    public static <T> byte[] serialize(T obj) {
        var rawBytes = new ByteArrayOutputStream();
        try (var output = new Output(rawBytes)) {
            var kryo = Serializer.get();
            kryo.writeObject(output, obj);
        }
        return rawBytes.toByteArray();
    }

    public static <T> T deserialize(byte[] bytes, Class<T> clazz) {
        var rawBytes = new ByteArrayInputStream(bytes);
        try (var input = new Input(rawBytes)) {
            var kryo = Serializer.get();
            return kryo.readObject(input, clazz);
        }
    }
}
