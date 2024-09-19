package services;

import domain.ChessBoard;
import models.Duel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPooled;

public class TestDictService {

    @Test
    public void testUpdates() {
        try (var jedis = new JedisPooled("localhost", 6379)) {
            var dictService = new DictService(jedis);

            var id = "test-id";
            dictService.setDuel(id, Duel.start());

            var firstMatch = dictService.getDuel(id);
            Assertions.assertEquals(Duel.start(), firstMatch);

            firstMatch.getGame().getBoard().setPiece("a1", ChessBoard.BLACK_QUEEN);
            dictService.setDuel(id, firstMatch);

            var secondMatch = dictService.getDuel(id);

            Assertions.assertEquals(firstMatch, secondMatch);
        }
    }
}
