package web;

import com.fasterxml.jackson.core.JsonProcessingException;
import domain.Hexagon;
import io.jooby.Jooby;
import io.jooby.StatusCode;
import io.jooby.WebSocket;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import models.Duel;
import services.DuelService;
import utils.ErrorResp;

import static utils.Log.LOGGER;

public class WsRouter extends Jooby {

    private final ServerState state;

    public WsRouter(ServerState state) {
        this.state = state;

        var jsonMapper = state.getJsonMapper();
        var broadcastService = state.getBroadcastService();
        var dictService = state.getDictService();

        ws("/duels/join/{id}", (ctx, configurer) -> {
            var sessionId = ctx.query("sessionId").valueOrNull(); // query is safe for secrets over a websocket when using wss
            var duelIdSlug = ctx.path("id");
            if (duelIdSlug.isMissing()) {
                ErrorResp.throwJson(StatusCode.BAD_REQUEST, "Invalid request: must contain id within slug", jsonMapper);
            }
            var duelId = duelIdSlug.toString();
            var player = dictService.getSessionOrDefault(sessionId);

            configurer.onConnect(handleDuelConnect(duelId, player));

            configurer.onMessage(handleDuelMessage(duelId, player));

            configurer.onClose((ws, statusCode) -> broadcastService.unsubscribeLocal(duelId, ws));
        });
    }

    @Data
    @AllArgsConstructor
    static class WsMessage {
        static final int FORFEIT = 0;
        static final int MOVE = 1;

        private int type;
        private Hexagon.Move move;
    }

    public WebSocket.OnConnect handleDuelConnect(String duelId, Duel.Player player) {
        var duelService = state.getDuelService();
        var socketExchange = state.getBroadcastService();
        var jsonMapper = state.getJsonMapper();

        return ws -> {
            try {
                var match = duelService.join(duelId, player);
                if (match == null) {
                    ws.close();
                    return;
                }
                socketExchange.subscribeLocal(duelId, ws);
                var jsonResult = jsonMapper.writeValueAsString(match);
                ws.send(jsonResult);
            } catch (JsonProcessingException e) {
                LOGGER.error(String.format("Failed to serialize json: %s", e.getMessage()));
                ws.close();
            }
        };
    }

    public WebSocket.OnMessage handleDuelMessage(String duelId, Duel.Player player) {
        var duelService = state.getDuelService();
        var broadcastService = state.getBroadcastService();
        var jsonMapper = state.getJsonMapper();

        return (ws, message) -> {
            try {
                var input = message.to(WsMessage.class);
                var type = input.getType();
                switch (type) {
                    case WsMessage.FORFEIT -> {
                        var match = duelService.forfeit(duelId);
                        var jsonOutput = jsonMapper.writeValueAsString(match);
                        broadcastService.broadcastLocal(duelId, jsonOutput);
                    }
                    case WsMessage.MOVE -> {
                        try {
                            var match = duelService.makeMove(duelId, player, input.getMove());
                            var jsonOutput = jsonMapper.writeValueAsString(match);
                            broadcastService.broadcastLocal(duelId, jsonOutput);
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
        };
    }
}
