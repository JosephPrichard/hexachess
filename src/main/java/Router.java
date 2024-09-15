import chess.Hexagon;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.jackson.JacksonModule;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import model.Duel;
import redis.clients.jedis.JedisPooled;
import services.*;

import java.util.Random;
import java.util.UUID;

import static utils.Log.LOGGER;

public class Router extends Jooby {

    @Getter()
    public static class State {
        private final DuelDao duelDao;
        private final DuelService duelService;
        private final BroadcastService broadcastService;
        private final ObjectMapper jsonMapper = new ObjectMapper();

        public State(JedisPooled jedis) {
            duelDao = new DuelDao(jedis);
            duelService = new DuelService(duelDao);
            broadcastService = new BroadcastService(jedis);
        }
    }

    @Getter
    @AllArgsConstructor
    static class CreateMatchResult {
        private String id;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    static class WsMessage {
        static final int FORFEIT = 0;
        static final int MOVE = 1;

        private int type;
        private Hexagon.Move move;
    }

    @Getter
    @AllArgsConstructor
    static class ErrorResp {
        private int status;
        private String message;

        public ErrorResp(String message) {
            this.message = message;
        }
    }

    private final State state;
    private final Random rand = new Random();

    public static Router init() {
        var jedis = new JedisPooled("localhost", 6379);
        var state = new State(jedis);
        return new Router(state);
    }

    public Router(State state) {
        this.state = state;

        var matchService = state.getDuelService();
        var socketExchange = state.getBroadcastService();
        var jsonMapper = state.getJsonMapper();

        install(new JacksonModule());

        post("/players/signup", ctx -> "");

        post("/players/login", ctx -> "");

        get("/players/stats", ctx -> "");

        post("/matches/create", ctx -> {
            var id = matchService.create();
            return new CreateMatchResult(id);
        });

        get("/matches/many", ctx -> {
            var cursorParam = ctx.query("cursor");
            var cursor = cursorParam.isPresent() ? cursorParam.toString() : "0";
            return matchService.getMany(cursor);
        });

        ws("/matches/join/{id}", (ctx, configurer) -> {
            var matchIdSlug = ctx.path("id");
            var matchId = matchIdSlug.isPresent() ? matchIdSlug.toString() : "";
            var player = authorize(ctx);

            configurer.onConnect(ws -> {
                try {
                    var match = matchService.join(matchId, player);
                    if (match == null) {
                        ws.close();
                        return;
                    }
                    socketExchange.subscribeLocal(matchId, ws);
                    var jsonResult = jsonMapper.writeValueAsString(match);
                    ws.send(jsonResult);
                } catch (JsonProcessingException e) {
                    LOGGER.error(String.format("Failed to serialize json: %s", e.getMessage()));
                    ws.close();
                }
            });

            configurer.onMessage((ws, message) -> {
                try {
                    var input = message.to(WsMessage.class);
                    var type = input.getType();
                    switch (type) {
                        case WsMessage.FORFEIT -> {
                            var match = matchService.forfeit(matchId);
                            var jsonOutput = jsonMapper.writeValueAsString(match);
                            socketExchange.broadcastLocal(matchId, jsonOutput);
                        }
                        case WsMessage.MOVE -> {
                            try {
                                var match = matchService.makeMove(matchId, player, input.getMove());
                                var jsonOutput = jsonMapper.writeValueAsString(match);
                                socketExchange.broadcastLocal(matchId, jsonOutput);
                            } catch (DuelService.MoveException e) {
                                var resp = new ErrorResp(e.getMessage());
                                var jsonOutput = jsonMapper.writeValueAsString(resp);
                                ws.send(jsonOutput);
                            }
                        }
                        default -> {
                            var resp = new ErrorResp(String.format("Invalid message type: %d", type));
                            var jsonOutput = jsonMapper.writeValueAsString(resp);
                            ws.send(jsonOutput);
                        }
                    }
                } catch (JsonProcessingException e) {
                    LOGGER.error(String.format("Failed to serialize json: %s", e.getMessage()));
                    ws.close();
                }
            });

            configurer.onClose((ws, statusCode) -> socketExchange.unsubscribeLocal(matchId, ws));
        });
    }

    public String guestName() {
        return "Guest " + rand.nextInt(1_000_000);
    }

    public Duel.Player authorize(Context ctx) {
        var duelStore = state.getDuelDao();
        var sessionId = ctx.header("Authorization").valueOrNull();
        Duel.Player player;

        if (sessionId != null) {
            // we have an authorization header... so get which player we're actually logged in as
            player = duelStore.getPlayer(sessionId);
        } else {
            // we don't have an authorization header... so create a "GUEST" player and set that as the logged in player
            player = new Duel.Player(UUID.randomUUID().toString(), guestName());
            sessionId = "";
            ctx.setResponseHeader("Authorization", sessionId);
        }
        if (player != null) {
            duelStore.setPlayer(sessionId, player);
        }

        return player;
    }
}
