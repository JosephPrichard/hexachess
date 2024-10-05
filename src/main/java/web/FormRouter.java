package web;

import io.jooby.Jooby;
import io.jooby.StatusCode;
import models.Player;
import utils.ErrorResp;
import utils.Threading;

public class FormRouter extends Jooby {
    public FormRouter(State state) {
        var jsonMapper = state.getJsonMapper();
        var accountDao = state.getUserDao();
        var remoteDict = state.getRemoteDict();
        var sessionService = state.getSessionService();
        var gameService = state.getGameService();

        setWorker(Threading.VIRTUAL_EXECUTOR);

        post("/forms/signup", ctx -> {
            var form = ctx.form();
            var username = form.get("username");
            var password = form.get("password");
            if (username.isMissing() || password.isMissing()) {
                ErrorResp.throwJson(StatusCode.BAD_REQUEST, "Form data is invalid", jsonMapper);
            }

            var usernameStr = username.toString();
            var passwordStr = password.toString();
            if (usernameStr.length() < 5 || usernameStr.length() > 20) {
                ErrorResp.throwJson(StatusCode.BAD_REQUEST, "Username should be between 5 and 20 characters", jsonMapper);
            }
            if (passwordStr.length() < 10 || passwordStr.length() > 100) {
                ErrorResp.throwJson(StatusCode.BAD_REQUEST, "Password should be between 10 and 100 characters", jsonMapper);
            }

            var inst = accountDao.insert(usernameStr, passwordStr);
            var player = new Player(inst.getNewId(), inst.getUsername());

            var sessionId = sessionService.createId();
            var cookie = sessionService.createCookie(sessionId);
            ctx.setResponseCookie(cookie);
            remoteDict.setSession(sessionId, player, cookie.getMaxAge());

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
            var passwordStr = password.toString();
            var player = accountDao.verify(usernameStr, passwordStr);
            if (player == null) {
                ErrorResp.throwJson(StatusCode.UNAUTHORIZED, "Login credentials are invalid", jsonMapper);
            }

            var sessionId = sessionService.createId();
            var cookie = sessionService.createCookie(sessionId);
            ctx.setResponseCookie(cookie);
            remoteDict.setSession(sessionId, player, cookie.getMaxAge());

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
            var passwordStr = password.toString();
            var player = accountDao.verify(usernameStr, passwordStr);
            if (player == null) {
                ErrorResp.throwJson(StatusCode.UNAUTHORIZED, "Login credentials are invalid", jsonMapper);
            }

            var sessionId = sessionService.createId();
            var cookie = sessionService.createCookie(sessionId);
            ctx.setResponseCookie(cookie);
            remoteDict.setSession(sessionId, player, cookie.getMaxAge());

            return sessionId;
        });

        post("/forms/games/create", ctx -> {
            var colorParam = ctx.query("color").toOptional().orElse(null);

            Boolean isFirstPlayerWhite = null;
            if (colorParam != null && colorParam.equals("white")) {
                isFirstPlayerWhite = true;
            } else if (colorParam != null && colorParam.equals("black")) {
                isFirstPlayerWhite = false;
            }

            return gameService.create(isFirstPlayerWhite);
        });
    }
}
