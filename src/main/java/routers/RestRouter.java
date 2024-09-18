package routers;

import io.jooby.Jooby;
import io.jooby.StatusCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import models.Duel;
import utils.Crypto;
import utils.ErrorResp;

public class RestRouter extends Jooby {

    @Getter
    @AllArgsConstructor
    static class CreateMatchResult {
        private String id;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    static class CredentialsForm {
        private String username;
        private String password;
    }

    public RestRouter(RouterState state) {

        var jsonMapper = state.getJsonMapper();
        var accountDao = state.getAccountDao();
        var duelDao = state.getDuelDao();
        var historyDao = state.getHistoryDao();
        var matchService = state.getDuelService();

        post("/players/signup", ctx -> {
            var body = ctx.form().to(CredentialsForm.class);

            var inst = accountDao.insert(body.getUsername(), body.getPassword());
            var player = new Duel.Player(inst.getNewId(), inst.getUsername());

            var sessionId = Crypto.createToken();
            ctx.setResponseHeader("Authorization", sessionId);
            duelDao.setPlayer(sessionId, player);

            return ctx.sendRedirect("/");
        });

        post("/players/login", ctx -> {
            var body = ctx.form().to(CredentialsForm.class);
            var username = body.getUsername();

            var id = accountDao.verify(username, body.getPassword());
            if (id == null) {
                ErrorResp.throwJson(StatusCode.UNAUTHORIZED, "Login credentials are invalid", jsonMapper);
            }
            var player = new Duel.Player(id, username);

            var sessionId = Crypto.createToken();
            ctx.setResponseHeader("Authorization", sessionId);
            duelDao.setPlayer(sessionId, player);

            return ctx.sendRedirect("/");
        });

        get("/players/stats/{id}", ctx -> {
            var accountIdSlug = ctx.path("id");
            if (accountIdSlug.isMissing()) {
                ErrorResp.throwJson(StatusCode.BAD_REQUEST, "Invalid request: must contain id within slug", jsonMapper);
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
                ErrorResp.throwJson(StatusCode.BAD_REQUEST, "Invalid request: must contain id within slug", jsonMapper);
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

        post("/duels/create", ctx -> {
            var id = matchService.create();
            return new CreateMatchResult(id);
        });

        get("/duels//many", ctx -> {
            var cursorParam = ctx.query("cursor");
            var cursor = cursorParam.toOptional().orElse(null);

            return matchService.getMany(cursor);
        });
    }
}
