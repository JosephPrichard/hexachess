package web;

import com.fasterxml.jackson.core.JsonProcessingException;
import domain.Hexagon;
import domain.Move;
import io.jooby.Jooby;
import io.jooby.StatusCode;
import io.jooby.WebSocket;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import models.GameState;
import models.Player;
import org.apache.commons.lang3.exception.ExceptionUtils;
import services.GameService;
import utils.ErrorResp;
import utils.Threading;
import web.State;

import static utils.Log.LOGGER;

public class WsRouter extends Jooby {

    public WsRouter(State state) {
        var jsonMapper = state.getJsonMapper();
        var broadcastService = state.getBroadcaster();
        var dictionary = state.getRemoteDict();

        ws("/games/join/{id}", (ctx, configurer) -> {
            var sessionId = ctx.query("sessionId").valueOrNull(); // query is safe for secrets over a websocket when using wss
            var gameIdSlug = ctx.path("id");
            if (gameIdSlug.isMissing()) {
                ErrorResp.throwJson(StatusCode.BAD_REQUEST, "Invalid request: must contain id within slug", jsonMapper);
            }

            var gameId = gameIdSlug.toString();
            var player = dictionary.getSessionOrDefault(sessionId);

            if (player == null) {
                throw new RuntimeException("Expected player to be non null");
            }

            configurer.onConnect(handleGameConnect(state, gameId, player));

            configurer.onMessage(handleGameMessage(state, gameId, player));

            configurer.onClose((ws, statusCode) -> broadcastService.unsubscribe(gameId, ws));
        });
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InputMsg {
        public static final int FORFEIT = 0;
        public static final int MOVE = 1;

        private int type;
        private Move move; // unused for forfeit...
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutputMsg {
        public static final int ERROR = 0;
        public static final int FORFEIT = 1;
        public static final int JOIN = 2;
        public static final int MOVE = 3;

        private int type;
        private String message; // only used for error
        private Player player; // only used for join, says who the joining player is
        private Move move; // only used for move
        private GameState gameState; // the current state of the game being played

        public static OutputMsg ofError(String message) {
            return new OutputMsg(ERROR, message, null, null, null);
        }

        public static OutputMsg ofForfeit(GameState gameState) {
            return new OutputMsg(FORFEIT, null, null, null, gameState);
        }

        public static OutputMsg ofJoin(Player player, GameState gameState) {
            return new OutputMsg(JOIN, null, player, null, gameState);
        }

        public static OutputMsg ofMove(GameState gameState, Move move) {
            return new OutputMsg(MOVE, null, null, move, gameState);
        }
    }

    public WebSocket.OnConnect handleGameConnect(State state, String gameId, Player player) {
        var gameService = state.getGameService();
        var socketExchange = state.getBroadcaster();
        var jsonMapper = state.getJsonMapper();

        return ws -> Threading.EXECUTOR.execute(() -> {
            try {
                var game = gameService.join(gameId, player);
                if (game == null) {
                    // we can't join... so just send an error and then disconnect
                    var jsonOutput = jsonMapper.writeValueAsString(OutputMsg.ofError("Invalid message type"));
                    ws.send(jsonOutput);
                    ws.close();
                    return;
                }
                socketExchange.subscribe(gameId, ws);
                // the joiner needs a snapshot of what the game actually looks like when joining!
                var jsonResult = jsonMapper.writeValueAsString(OutputMsg.ofJoin(player, game));
                ws.send(jsonResult);
                LOGGER.info(String.format("Player %s connected to game %s", player.getId(), gameId));
            } catch (Exception e) {
                // if we encounter some unknown error or maybe json failure, we can't really do anything so just log and close the connection
                LOGGER.error(String.format("Fatal exception occurred: %s", e.getMessage()));
                ws.close();
            }
        });
    }

    public WebSocket.OnMessage handleGameMessage(State state, String gameId, Player player) {
        var gameService = state.getGameService();
        var broadcastService = state.getBroadcaster();
        var jsonMapper = state.getJsonMapper();

        return (ws, message) -> Threading.EXECUTOR.execute(() -> {
            try {
                try {
                    LOGGER.info(String.format("Received message from player %s, %s on game %s", player.getId(), message.value(), gameId));
                    var input = jsonMapper.readValue(message.value(), InputMsg.class);
                    var type = input.getType();
                    switch (type) {
                        // handle the message cases by serializing the json and broadcasting to all listening clients
                        case InputMsg.FORFEIT -> {
                            var game = gameService.forfeit(gameId, player);
                            var jsonOutput = jsonMapper.writeValueAsString(OutputMsg.ofForfeit(game));
                            broadcastService.broadcast(gameId, jsonOutput);
                        }
                        case InputMsg.MOVE -> {
                            var move = input.getMove();
                            var game = gameService.makeMove(gameId, player, move);
                            var jsonOutput = jsonMapper.writeValueAsString(OutputMsg.ofMove(game, move));
                            broadcastService.broadcast(gameId, jsonOutput);
                        }
                        // unknown messages involve sending an error back to the og sender
                        default -> {
                            var resp = OutputMsg.ofError("Invalid message type: %d" + type);
                            var jsonOutput = jsonMapper.writeValueAsString(resp);
                            ws.send(jsonOutput);
                        }
                    }
                } catch (GameService.MoveException e) {
                    // handle an exceptional case that happens while attempting to make a move by sending an error back to og sender
                    var jsonOutput = jsonMapper.writeValueAsString(OutputMsg.ofError(e.getMessage()));
                    ws.send(jsonOutput);
                } catch (Exception e) {
                    // handle any unknown error that happens during message processing by sending an error back to og sender
                    var jsonOutput = jsonMapper.writeValueAsString(OutputMsg.ofError("An unexpected error has occurred"));
                    ws.send(jsonOutput);
                    LOGGER.error("Unexpected error occurred in websocket message handler " + ExceptionUtils.getStackTrace(e));
                }
            } catch (JsonProcessingException e) {
                // if we can't write the json back to the client, we can't really do anything so just log and close
                LOGGER.error("Failed to serialize json: " + ExceptionUtils.getStackTrace(e));
                ws.close();
            }
        });
    }
}
