package web;

import com.fasterxml.jackson.core.JsonProcessingException;
import domain.Hexagon;
import io.jooby.Jooby;
import io.jooby.StatusCode;
import io.jooby.WebSocket;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import models.Duel;
import services.DuelService;
import utils.ErrorResp;

import static utils.Log.LOGGER;

public class WsRouter extends Jooby {

    public WsRouter(ServerState state) {
        var jsonMapper = state.getJsonMapper();
        var broadcastService = state.getBroadcastService();
        var dictService = state.getDictService();

        ws("/games/join/{id}", (ctx, configurer) -> {
            var sessionId = ctx.query("sessionId").valueOrNull(); // query is safe for secrets over a websocket when using wss
            var duelIdSlug = ctx.path("id");
            if (duelIdSlug.isMissing()) {
                ErrorResp.throwJson(StatusCode.BAD_REQUEST, "Invalid request: must contain id within slug", jsonMapper);
            }

            var duelId = duelIdSlug.toString();
            var player = dictService.getSessionOrDefault(sessionId);
            assert player != null;

            configurer.onConnect(handleDuelConnect(state, duelId, player));

            configurer.onMessage(handleDuelMessage(state, duelId, player));

            configurer.onClose((ws, statusCode) -> broadcastService.unsubscribeLocal(duelId, ws));
        });
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InputMsg {
        public static final int FORFEIT = 0;
        public static final int MOVE = 1;

        private int type;
        private Hexagon.Move move; // unused for forfeit...
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutputMsg {
        public static final int ERROR = 0;
        public static final int FORFEIT = 1;
        public static final int JOIN = 2;
        public static final int STATE = 3;

        private int type;
        private String message;
        private Duel.Player player; // only used for join, says who the receiver is playing as
        private Duel duel; // only used for state and join

        public static OutputMsg ofError(String message) {
            return new OutputMsg(ERROR, message, null,null);
        }

        public static OutputMsg ofForfeit() {
            return new OutputMsg(FORFEIT, null, null,null);
        }

        public static OutputMsg ofJoin(Duel.Player player, Duel duel) {
            return new OutputMsg(JOIN, null, player, duel);
        }

        public static OutputMsg ofState(Duel duel) {
            return new OutputMsg(STATE, null,null, duel);
        }
    }

    public static WebSocket.OnConnect handleDuelConnect(ServerState state, String duelId, Duel.Player player) {
        var duelService = state.getDuelService();
        var socketExchange = state.getBroadcastService();
        var jsonMapper = state.getJsonMapper();

        return ws -> {
            try {
                var duel = duelService.join(duelId, player);
                if (duel == null) {
                    // we can't join... so just send an error and then disconnect
                    var resp = OutputMsg.ofError("Invalid message type");
                    var jsonOutput = jsonMapper.writeValueAsString(resp);
                    ws.send(jsonOutput);
                    ws.close();
                    return;
                }
                socketExchange.subscribeLocal(duelId, ws);
                // the joiner needs a snapshot of what the game actually looks like when joining!
                var jsonResult = jsonMapper.writeValueAsString(OutputMsg.ofJoin(player, duel));
                ws.send(jsonResult);
                LOGGER.info(String.format("Player %s connected to duel %s", player.getId(), duelId));
            } catch (JsonProcessingException e) {
                // if we can't write the json back to the client, we can't really do anything so just log and close
                LOGGER.error(String.format("Failed to serialize json: %s", e.getMessage()));
                ws.close();
            }
        };
    }

    public static WebSocket.OnMessage handleDuelMessage(ServerState state, String duelId, Duel.Player player) {
        var duelService = state.getDuelService();
        var broadcastService = state.getBroadcastService();
        var jsonMapper = state.getJsonMapper();

        return (ws, message) -> {
            try {
                try {
                    LOGGER.info(String.format("Received message from player %s, %s on duel %s", player.getId(), message.value(), duelId));
                    var input = jsonMapper.readValue(message.value(), InputMsg.class);
                    var type = input.getType();
                    switch (type) {
                        // handle the message cases by serializing the json and broadcasting to all listening clients
                        case InputMsg.FORFEIT -> {
                            var match = duelService.forfeit(duelId);
                            var jsonOutput = jsonMapper.writeValueAsString(match);
                            broadcastService.broadcastGlobal(duelId, jsonOutput);
                        }
                        case InputMsg.MOVE -> {
                            var duel = duelService.makeMove(duelId, player, input.getMove());
                            var jsonOutput = jsonMapper.writeValueAsString(OutputMsg.ofState(duel));
                            broadcastService.broadcastGlobal(duelId, jsonOutput);
                        }
                        // unknown messages involve sending an error back to the og sender
                        default -> {
                            var resp = OutputMsg.ofError(String.format("Invalid message type: %d", type));
                            var jsonOutput = jsonMapper.writeValueAsString(resp);
                            ws.send(jsonOutput);
                        }
                    }
                } catch (DuelService.MoveException e) {
                    // handle an exceptional case that happens while attempting to make a move by sending an error back to og sender
                    var resp = OutputMsg.ofError(e.getMessage());
                    var jsonOutput = jsonMapper.writeValueAsString(resp);
                    ws.send(jsonOutput);
                }  catch (Exception e) {
                    // handle any unknown error that happens during message processing by sending an error back to og sender
                    var resp = OutputMsg.ofError("An unexpected error has occurred");
                    var jsonOutput = jsonMapper.writeValueAsString(resp);
                    ws.send(jsonOutput);
                    LOGGER.error("Unexpected error occurred in websocket message handler " + e.getMessage());
                }
            } catch (JsonProcessingException e) {
                // if we can't write the json back to the client, we can't really do anything so just log and close
                LOGGER.error(String.format("Failed to serialize json: %s", e.getMessage()));
                ws.close();
            }
        };
    }
}
