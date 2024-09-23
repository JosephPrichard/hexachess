package router;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.Hexagon;
import io.jooby.test.MockRouter;
import io.jooby.test.MockWebSocketClient;
import lombok.Getter;
import models.Duel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import services.BroadcastService;
import services.DuelService;
import services.LocalDictService;
import web.ServerState;
import web.WsRouter;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.mock;
import static utils.Log.LOGGER;

// integration style test that mocks out external data stores but checks if an entire game can be played through websockets
public class TestWsRouter {

    @Getter
    public static class MockWsClientTest {
        private Throwable exception;
        private MockWebSocketClient mockClient;

        // simulate a client (much like one that would run on the browser) to test our websocket server
        public static MockWsClientTest start(MockRouter mockRouter, String duelId, Queue<Hexagon.Move> moves) {
            var jsonMapper = new ObjectMapper()
                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

            AtomicReference<Duel.Player> player = new AtomicReference<>();

            var mockClientTest = new MockWsClientTest();
            mockClientTest.mockClient = mockRouter.ws("/games/join/" + duelId, (client) -> client.onMessage((message) -> {
                try {
                    LOGGER.info(String.format("Received message %s", message));

                    var msgObject = jsonMapper.readValue((String) message, WsRouter.OutputMsg.class);
                    Assertions.assertTrue(msgObject.getType() == WsRouter.OutputMsg.STATE || msgObject.getType() == WsRouter.OutputMsg.JOIN);
                    Assertions.assertNotNull(msgObject.getDuel());

                    // initialization logic - let's keep track of what player we are
                    if (msgObject.getType() == WsRouter.OutputMsg.JOIN) {
                        Assertions.assertNotNull(msgObject.getPlayer());
                        player.set(msgObject.getPlayer());
                    }

                    // if it's our turn, we make to take the next move from the queue and send it as a message
                    var currentPlayer = msgObject.getDuel().getCurrPlayer();
                    var isClientTurn = player.get().equals(currentPlayer);
                    if (isClientTurn) {
                        var msg = new WsRouter.InputMsg(WsRouter.InputMsg.MOVE, moves.poll());
                        client.send(jsonMapper.writeValueAsString(msg));
                    }
                } catch (Exception ex) {
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
        var state = new ServerState();

        var mockDictService = new LocalDictService();
        var mockBroadcastService = mock(BroadcastService.class);

        var duelService = new DuelService(mockDictService);
        var duelId = duelService.create(true); // first player joining will be white

        state.setBroadcastService(mockBroadcastService);
        state.setDictService(mockDictService);
        state.setDuelService(duelService);

        var mockRouter = new MockRouter(new WsRouter(state));

        var moves = new LinkedBlockingQueue<>(List.of(
            new Hexagon.Move(Hexagon.fromNotation("f5"), Hexagon.fromNotation("f6")),
            new Hexagon.Move(Hexagon.fromNotation("b7"), Hexagon.fromNotation("b6")),
            new Hexagon.Move(Hexagon.fromNotation("k1"), Hexagon.fromNotation("k2")),
            new Hexagon.Move(Hexagon.fromNotation("k7"), Hexagon.fromNotation("k6"))));

        var client1 = MockWsClientTest.start(mockRouter, duelId, moves);
        var client2 = MockWsClientTest.start(mockRouter, duelId, moves);

        client1.maybeThrow();
        client2.maybeThrow();
    }
}
