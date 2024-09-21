package web;

import io.jooby.Jooby;
import io.jooby.StatusCode;
import models.Duel;
import services.SessionService;
import utils.ErrorResp;

public class RestRouter extends Jooby {

    public RestRouter(ServerState state) {

        var jsonMapper = state.getJsonMapper();
        var accountDao = state.getAccountDao();
        var dictService = state.getDictService();
        var sessionService = state.getSessionService();
        var historyDao = state.getHistoryDao();
        var duelService = state.getDuelService();

        post("/forms/signup", ctx -> {
            var form = ctx.form();
            var username = form.get("username");
            var password = form.get("password");
            if (username.isMissing() || password.isMissing()) {
                ErrorResp.throwJson(StatusCode.BAD_REQUEST, "Form data is invalid", jsonMapper);
            }

            var inst = accountDao.insert(username.toString(), password.toString());
            var player = new Duel.Player(inst.getNewId(), inst.getUsername());

            var sessionId = sessionService.createId();
            var cookie = sessionService.createCookie(sessionId);
            ctx.setResponseCookie(cookie);
            dictService.setSession(sessionId, player, cookie.getMaxAge());

            return sessionId;
        });

        post("/forms/login", ctx -> {
            var form = ctx.form();
            var username = form.get("username");
            var password = form.get("password");
            if (username.isMissing() || password.isMissing()) {
                ErrorResp.throwJson(StatusCode.BAD_REQUEST, "Form data is invalid", jsonMapper);
            }

            var usernameStr = username.toString();
            var id = accountDao.verify(usernameStr, password.toString());
            if (id == null) {
                ErrorResp.throwJson(StatusCode.UNAUTHORIZED, "Login credentials are invalid", jsonMapper);
            }
            var player = new Duel.Player(id, usernameStr);

            var sessionId = sessionService.createId();
            var cookie = sessionService.createCookie(sessionId);
            ctx.setResponseCookie(cookie);
            dictService.setSession(sessionId, player, cookie.getMaxAge());

            return sessionId;
        });

        get("/players/{id}/stats", ctx -> {
            var accountIdSlug = ctx.path("id");
            if (accountIdSlug.isMissing()) {
                ErrorResp.throwJson(StatusCode.BAD_REQUEST, "Invalid request: must contain id within slug", jsonMapper);
            }
            var accountId = accountIdSlug.toString();

            return accountDao.getStats(accountId);
        });

        get("/leaderboard", ctx -> {
            var cursorQuery = ctx.query("cursor");
            var cursor = cursorQuery.toOptional().map(Float::parseFloat).orElse(null);

            return accountDao.getLeaderboard(cursor, 20);
        });

        get("/player/{id}/history", ctx -> {
            var accountIdSlug = ctx.path("id");
            if (accountIdSlug.isMissing()) {
                ErrorResp.throwJson(StatusCode.BAD_REQUEST, "Invalid request: must contain id within slug", jsonMapper);
            }
            var accountId = accountIdSlug.toString();

            var cursorParam = ctx.query("cursor");
            var cursor = cursorParam.toOptional().orElse(null);

            return historyDao.getHistories(accountId, cursor);
        });

        get("/players/current", ctx -> {
            var sessionId = ctx.cookie(SessionService.COOKIE_NAME).valueOrNull();
            Duel.Player player = null;
            if (sessionId != null) {
                player = dictService.getSession(sessionId);
            }
            return dictService.retrieveAndAppend(player);
        });

        get("/games/history", ctx -> {
            var whiteIdParam = ctx.query("whiteId");
            var whiteId = whiteIdParam.toOptional().orElse(null);

            var blackIdParam = ctx.query("blackId");
            var blackId = blackIdParam.toOptional().orElse(null);

            var cursorParam = ctx.query("cursor");
            var cursor = cursorParam.toOptional().orElse(null);

            return historyDao.getHistories(whiteId, blackId, cursor);
        });

        post("/forms/games/create", ctx -> duelService.create());

        get("/games/current", ctx -> {
            var cursorParam = ctx.query("cursor");
            var cursor = cursorParam.isPresent() ? cursorParam.doubleValue() : null;
            return duelService.getManyKeys(cursor);
        });
    }
}
