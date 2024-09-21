package services;

import domain.ChessBoard;
import models.Duel;
import org.junit.jupiter.api.*;
import redis.clients.jedis.JedisPooled;
import redis.embedded.RedisServer;

import java.time.Duration;
import java.util.List;

// this is an integration test that runs against a real redis instance,
// so it is disabled as to not be run automatically with all other tests
@Disabled
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestDictService {

    private RedisServer redisServer;
    private JedisPooled jedis;
    private DictService dictService;

    @BeforeAll
    public void beforeAll() {
        redisServer = new RedisServer(6379);
        redisServer.start();
        jedis = new JedisPooled("localhost", 6379);
        dictService = new DictService(jedis);
    }

    @BeforeEach
    public void beforeEach() {
        jedis.flushAll();
    }

    @AfterAll
    public void afterAll() {
        redisServer.stop();
    }

    @Test
    public void testDuelUpdate() {
        var id = "test-id";
        dictService.setDuel(id, Duel.start());

        var firstMatch = dictService.getDuel(id);
        Assertions.assertEquals(Duel.start(), firstMatch);

        firstMatch.getGame().getBoard().setPiece("a1", ChessBoard.BLACK_QUEEN);
        dictService.setDuel(id, firstMatch);

        var secondMatch = dictService.getDuel(id);

        Assertions.assertEquals(firstMatch, secondMatch);
    }

    @Test
    public void testDuelScan() {
        // given
        var id1 = "test-id1";
        var id2 = "test-id2";
        var id3 = "test-id3";
        var id4 = "test-id4";

        var player1 = new Duel.Player("id1", "name1");
        var player2 = new Duel.Player("id2", "name2");
        var player3 = new Duel.Player("id3", "name3");

        var duel1 = Duel.ofPlayers(player1, player2);
        var duel2 = Duel.ofPlayers(player2, player3);
        var duel3 = Duel.ofPlayers(player3, player1);
        var duel4 = Duel.ofPlayers(player2, player1);

        // when
        dictService.setDuel(id1, duel1);
        dictService.setDuel(id2, duel2);
        dictService.setDuel(id3, duel3);
        dictService.setDuel(id4, duel4);

        var scanResult1 = dictService.getDuelKeys(null, 2);
        var scanResult2 = dictService.getDuelKeys(scanResult1.getNextCursor(), 2);

        // then
        Assertions.assertEquals(2, scanResult1.getDuelKeys().size());
        Assertions.assertEquals(2, scanResult2.getDuelKeys().size());
        Assertions.assertNull(scanResult2.getNextCursor());
    }

    @Test
    public void testSessions() throws InterruptedException {
        // given
        var player1 = new Duel.Player("test-id1", "test-name1");
        var player2 = new Duel.Player("test-id2", "test-name2");

        // when
        dictService.setSession("session1", player1, 100);
        dictService.setSession("session2", player2, 1);

        var actualPlayer1 = dictService.getSession("session1");

        Thread.sleep(1000); // wait for key to expire
        var actualPlayer2 = dictService.getSession("session2");

        // then
        Assertions.assertEquals(player1, actualPlayer1);
        Assertions.assertNull(actualPlayer2);
    }

    @Test
    public void testGetPlayers() {
        // given
        var player1 = new Duel.Player("id1", "name1");
        var player2 = new Duel.Player("id2", "name2");
        var player3 = new Duel.Player("id3", "name3");

        // when
        var actualPlayers1 = dictService.retrieveAndAppend(player1);
        var actualPlayers2 = dictService.retrieveAndAppend(player2);
        var actualPlayers3 = dictService.retrieveAndAppend(player3, Duration.ofSeconds(1).toMillis());

        // then
        Assertions.assertEquals(List.of(player1), actualPlayers1);
        Assertions.assertEquals(List.of(player1, player2), actualPlayers2);
        Assertions.assertEquals(List.of(player3), actualPlayers3);
    }
}
