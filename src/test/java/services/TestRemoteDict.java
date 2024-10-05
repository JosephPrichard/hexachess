package services;

import com.fasterxml.jackson.databind.json.JsonMapper;
import domain.ChessBoard;
import models.GameState;
import models.Player;
import org.junit.jupiter.api.*;
import redis.clients.jedis.JedisPooled;
import redis.embedded.RedisServer;

// this is an integration test that runs against a real redis instance
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestRemoteDict {

    private RedisServer redisServer;
    private JedisPooled jedis;
    private RemoteDict remoteDict;

    @BeforeAll
    public void beforeAll() {
        redisServer = new RedisServer(6379);
        redisServer.start();
        jedis = new JedisPooled("localhost", 6379);
        remoteDict = new RemoteDict(jedis, new JsonMapper());
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
    public void testGameUpdate() {
        var id = "test-id";
        remoteDict.setGame(id, GameState.startWithGame(id));

        var firstGame = remoteDict.getGame(id);
        Assertions.assertEquals(GameState.startWithGame(id), firstGame);

        firstGame.getGame().getBoard().setPiece("a1", ChessBoard.BLACK_QUEEN);
        remoteDict.setGame(id, firstGame);

        var secondGame = remoteDict.getGame(id);

        Assertions.assertEquals(firstGame, secondGame);
    }

    @Test
    public void testGameScan() {
        // given
        var id1 = "test-id1";
        var id2 = "test-id2";
        var id3 = "test-id3";
        var id4 = "test-id4";

        var player1 = new Player("id1", "name1");
        var player2 = new Player("id2", "name2");
        var player3 = new Player("id3", "name3");

        var game1 = GameState.ofPlayers(player1, player2);
        var game2 = GameState.ofPlayers(player2, player3);
        var game3 = GameState.ofPlayers(player3, player1);
        var game4 = GameState.ofPlayers(player2, player1);

        // when
        remoteDict.setGame(id1, game1);
        remoteDict.setGame(id2, game2);
        remoteDict.setGame(id3, game3);
        remoteDict.setGame(id4, game4);

        var scanResult1 = remoteDict.getGameKeys(null, 2);
        var scanResult2 = remoteDict.getGameKeys(scanResult1.getNextCursor(), 2);

        // then
        Assertions.assertEquals(2, scanResult1.getKeys().size());
        Assertions.assertEquals(2, scanResult2.getKeys().size());
        Assertions.assertNull(scanResult2.getNextCursor());
    }

    @Test
    public void testSessions() throws InterruptedException {
        // given
        var player1 = new Player("test-id1", "test-name1");
        var player2 = new Player("test-id2", "test-name2");

        // when
        remoteDict.setSession("session1", player1, 100);
        remoteDict.setSession("session2", player2, 1);

        var actualPlayer1 = remoteDict.getSession("session1");

        Thread.sleep(1000); // wait for key to expire
        var actualPlayer2 = remoteDict.getSession("session2");

        // then
        Assertions.assertEquals(player1, actualPlayer1);
        Assertions.assertNull(actualPlayer2);
    }
}
