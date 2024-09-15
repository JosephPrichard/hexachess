package services;

import chess.ChessBoard;
import model.Duel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPooled;

public class TestDuelService {

    @Test
    public void testUpdates() {
        try (var jedis = new JedisPooled("localhost", 6379)) {
            var gameStorage = new DuelDao(jedis);

            var id = "test-id";
            gameStorage.setDuel(id, Duel.start());

            var firstMatch = gameStorage.getDuel(id);
            Assertions.assertEquals(Duel.start(), firstMatch);

            firstMatch.getGame().getBoard().setPiece("a1", ChessBoard.BLACK_QUEEN);
            gameStorage.setDuel(id, firstMatch);

            var secondMatch = gameStorage.getDuel(id);

            Assertions.assertEquals(firstMatch, secondMatch);
        }
    }
}
