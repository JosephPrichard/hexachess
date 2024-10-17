package router;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import domain.Hexagon;
import domain.Move;
import io.jooby.test.MockRouter;
import io.jooby.test.MockWebSocketClient;
import lombok.Getter;
import models.Player;
import org.junit.jupiter.api.*;
import redis.clients.jedis.JedisPooled;
import redis.embedded.RedisServer;
import services.*;
import web.State;
import web.WsRouter;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.mock;
import static utils.Log.LOGGER;

// integration style test that mocks out external data stores but checks if a game can be played through websockets
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WsRouterTest {

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

    @Getter
    public static class MockWsClientTest {
        private Throwable exception;
        private MockWebSocketClient mockClient;

        // simulate a client (much like one that would run on the browser) to test our websocket server
        public static MockWsClientTest start(MockRouter mockRouter, String gameId, Queue<Move> moves) {
            var jsonMapper = new ObjectMapper()
                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

            AtomicReference<Player> player = new AtomicReference<>();

            var mockClientTest = new MockWsClientTest();
            mockClientTest.mockClient = mockRouter.ws("/games/join/" + gameId, (client) -> client.onMessage((message) -> {
                try {
                    LOGGER.info(String.format("Received message %s", message));

                    var msgObject = jsonMapper.readValue((String) message, WsRouter.OutputMsg.class);
                    if (msgObject.getType() == WsRouter.OutputMsg.ERROR) {
                        Assertions.fail(new RuntimeException(msgObject.getMessage()));
                    }
                    Assertions.assertNotNull(msgObject.getGameState());

                    // initialization logic - let's keep track of what player we are
                    if (msgObject.getType() == WsRouter.OutputMsg.JOIN) {
                        Assertions.assertNotNull(msgObject.getPlayer());
                        player.set(msgObject.getPlayer());
                    }

                    // ok, we can stop when there's no more moves to make
                    if (moves.isEmpty()) {
                        client.close();
                        return;
                    }

                    // if it's our turn, we make to take the next move from the queue and send it as a message
                    var currentPlayer = msgObject.getGameState().getCurrPlayer();
                    var isClientTurn = player.get().equals(currentPlayer);
                    if (isClientTurn) {
                        var msg = new WsRouter.InputMsg(WsRouter.InputMsg.MOVE, moves.poll());
                        client.send(jsonMapper.writeValueAsString(msg));
                    }
                } catch (Throwable ex) {
                    mockClientTest.exception = ex;
                    client.close();
                }
            }));
            return mockClientTest;
        }

        public void maybeThrow() throws Throwable {
            if (exception != null) {
                throw exception;
            }
        }
    }

    @Test
    public void testPlayGameOverWs() throws Throwable {
        var state = new State();

        var broadcaster = new LocalBroadcaster();

        var gameService = new GameService(remoteDict, null, null);
        var gameId = gameService.create(true); // first player joining will be white

        state.setBroadcaster(broadcaster);
        state.setRemoteDict(remoteDict);
        state.setGameService(gameService);

        var mockRouter = new MockRouter(new WsRouter(state));

        var moves = new LinkedBlockingQueue<>(List.of(
            new Move(Hexagon.fromNotation("f5"), Hexagon.fromNotation("f6")),
            new Move(Hexagon.fromNotation("b7"), Hexagon.fromNotation("b6")),
            new Move(Hexagon.fromNotation("j1"), Hexagon.fromNotation("j2")),
            new Move(Hexagon.fromNotation("j7"), Hexagon.fromNotation("j6"))));

        var client1 = MockWsClientTest.start(mockRouter, gameId, moves);
        var client2 = MockWsClientTest.start(mockRouter, gameId, moves);

        client1.maybeThrow();
        client2.maybeThrow();
    }
}
