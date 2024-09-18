package utils;

import domain.ChessBoard;
import domain.ChessGame;
import domain.Hexagon;
import com.esotericsoftware.kryo.Kryo;
import services.BroadcastService;
import models.Duel;

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
}
