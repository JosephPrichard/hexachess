import chess.Hexagon;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.jooby.*;
import io.jooby.exception.StatusCodeException;
import io.jooby.handler.RateLimitHandler;
import io.jooby.jackson.JacksonModule;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import model.Duel;
import redis.clients.jedis.JedisPooled;
import services.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static utils.Log.LOGGER;

public class Router extends Jooby {

    @Getter()
    public static class State {
        private final AccountDao accountDao;
        private final HistoryDao historyDao;
        private final DuelDao duelDao;
        private final DuelService duelService;
        private final BroadcastService broadcastService;
        private final ObjectMapper jsonMapper = new ObjectMapper();

        public State(JedisPooled jedis, HikariDataSource ds) {
            accountDao = new AccountDao(ds);
            historyDao = new HistoryDao(ds);
            duelDao = new DuelDao(jedis);
            duelService = new DuelService(duelDao);
            broadcastService = new BroadcastService(jedis);
        }
    }

    private final State state;
    private static final Random rand = new Random();
    private static final SecureRandom RANDOM = new SecureRandom();

    private static Map<String, String> readEnv() {
        var inputStream = Router.class.getClassLoader().getResourceAsStream(".env");
        if (inputStream == null) {
            throw new IllegalArgumentException(".env file is not found");
        }

        Map<String, String> envMap = new HashMap<>();

        try (var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                var tokens = line.split("=");
                if (tokens.length < 2) {
                    throw new IllegalArgumentException("Line must have at least two tokens: " + line);
                }
                envMap.put(tokens[0], tokens[1]);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        return envMap;
    }

    public static Router init() {
        var envMap = readEnv();

        var dbUrl = envMap.get("DB_URL");
        var dbUser = envMap.get("DB_USER");
        var dbPassword = envMap.get("DB_PASSWORD");
        var redisHost = envMap.get("REDIS_HOST");
        var redisPort = Integer.parseInt(envMap.get("REDIS_PORT"));

        var config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(10);
        config.setAutoCommit(false);

        var ds = new HikariDataSource(config);
        var jedis = new JedisPooled(redisHost, redisPort);
        var state = new State(jedis, ds);

        return new Router(state);
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

    public void sendErrorResp(StatusCode code, String message) {
        var jsonMapper = state.getJsonMapper();
        try {
            var errorJson = jsonMapper.writeValueAsString(new ErrorResp(code.value(), message));
            throw new StatusCodeException(code, errorJson);
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format("Failed to serialize json: %s", e.getMessage()));
            throw new StatusCodeException(StatusCode.SERVER_ERROR);
        }
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

    public WebSocket.OnMessage matchMessageHandler(String matchId, Duel.Player player) {
        var matchService = state.getDuelService();
        var socketExchange = state.getBroadcastService();
        var jsonMapper = state.getJsonMapper();
        return (ws, message) -> {
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
        };
    }

    @Getter
    @AllArgsConstructor
    static class CreateMatchResult {
        private String id;
    }

    @Getter
    @NoArgsConstructor
    static class SignupReqBody {
        private String username;
        private String password;
    }

    public Router(State state) {
        this.state = state;

        var accountDao = state.getAccountDao();
        var historyDao = state.getHistoryDao();
        var matchService = state.getDuelService();
        var socketExchange = state.getBroadcastService();
        var jsonMapper = state.getJsonMapper();

        install(new JacksonModule());

        post("/players/signup", ctx -> {
            var body = ctx.body().to(SignupReqBody.class);
            accountDao.insert(body.getUsername(), body.getPassword());
            return ""; // success with no message
        });

        post("/players/login", ctx -> {
            var body = ctx.body().to(SignupReqBody.class);
            var isValid = accountDao.verify(body.getUsername(), body.getPassword());
            if (!isValid) {
                sendErrorResp(StatusCode.UNAUTHORIZED, "Login credentials are invalid");
            }
            return ""; // success with no message
        });

        get("/players/stats/{id}", ctx -> {
            var accountIdSlug = ctx.path("id");
            if (accountIdSlug.isMissing()) {
                sendErrorResp(StatusCode.BAD_REQUEST, "Invalid request: must contain id within slug");
            }
            var accountId = accountIdSlug.toString();

            return accountDao.getStats(accountId);
        });

        get("/players/leaderboard", ctx -> {
            var cursorQuery = ctx.query("cursor");
            var cursor = cursorQuery.toOptional().map(Float::parseFloat).orElse(null);

            return accountDao.getLeaderboard(cursor, 20);
        });

        get("/history/account", ctx -> {
            var accountIdSlug = ctx.path("id");
            if (accountIdSlug.isMissing()) {
                sendErrorResp(StatusCode.BAD_REQUEST, "Invalid request: must contain id within slug");
            }
            var accountId = accountIdSlug.toString();

            var cursorParam = ctx.query("cursor");
            var cursor = cursorParam.toOptional().orElse(null);

            return historyDao.getHistories(accountId, cursor);
        });

        get("/history/matches", ctx -> {
            var whiteIdParam = ctx.query("whiteId");
            var whiteId = whiteIdParam.toOptional().orElse(null);

            var blackIdParam = ctx.query("blackId");
            var blackId = blackIdParam.toOptional().orElse(null);

            var cursorParam = ctx.query("cursor");
            var cursor = cursorParam.toOptional().orElse(null);

            return historyDao.getHistories(whiteId, blackId, cursor);
        });

        post("/matches/create", ctx -> {
            var id = matchService.create();
            return new CreateMatchResult(id);
        });

        get("/matches/many", ctx -> {
            var cursorParam = ctx.query("cursor");
            var cursor = cursorParam.toOptional().orElse(null);

            return matchService.getMany(cursor);
        });

        ws("/matches/join/{id}", (ctx, configurer) -> {
            var matchIdSlug = ctx.path("id");
            if (matchIdSlug.isMissing()) {
                sendErrorResp(StatusCode.BAD_REQUEST, "Invalid request: must contain id within slug");
            }
            var matchId = matchIdSlug.toString();
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

            configurer.onMessage(matchMessageHandler(matchId, player));

            configurer.onClose((ws, statusCode) -> socketExchange.unsubscribeLocal(matchId, ws));
        });
    }

    public static String getGuestName() {
        return "Guest " + rand.nextInt(1_000_000);
    }

    public static String getSessionToken() {
        var length = 100;
        var sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            var characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            int index = RANDOM.nextInt(characters.length());
            sb.append(characters.charAt(index));
        }
        return sb.toString();
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
            player = new Duel.Player(UUID.randomUUID().toString(), getGuestName());
            sessionId = getSessionToken();
            ctx.setResponseHeader("Authorization", sessionId);
        }
        if (player != null) {
            duelStore.setPlayer(sessionId, player);
        }

        return player;
    }
}
