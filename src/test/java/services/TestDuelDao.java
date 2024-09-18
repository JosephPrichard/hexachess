package services;

import domain.ChessBoard;
import models.Duel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPooled;

public class TestDuelDao {

    @Test
    public void testUpdates() {
        try (var jedis = new JedisPooled("localhost", 6379)) {
            var duelDao = new DuelDao(jedis);

            var id = "test-id";
            duelDao.setDuel(id, Duel.start());

            var firstMatch = duelDao.getDuel(id);
            Assertions.assertEquals(Duel.start(), firstMatch);

            firstMatch.getGame().getBoard().setPiece("a1", ChessBoard.BLACK_QUEEN);
            duelDao.setDuel(id, firstMatch);

            var secondMatch = duelDao.getDuel(id);

            Assertions.assertEquals(firstMatch, secondMatch);
        }
    }
}
