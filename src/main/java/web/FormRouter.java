package web;

import io.jooby.Jooby;
import io.jooby.StatusCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import models.Player;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import utils.ErrorResp;
import utils.Threading;

import static web.SessionService.COOKIE_NAME;

public class FormRouter extends Jooby {

    @Getter
    @AllArgsConstructor
    static class FormResp {
        String message;
    }

    public FormRouter(State state) {
        var jsonMapper = state.getJsonMapper();
        var accountDao = state.getUserDao();
        var remoteDict = state.getRemoteDict();
        var sessionService = state.getSessionService();
        var gameService = state.getGameService();

        setWorker(Threading.EXECUTOR);

        post("/forms/signup", ctx -> {
            ctx.setResponseHeader("Content-Type", "application/json");

            var form = ctx.form();
            var username = form.get("username");
            var password = form.get("password");
            var dupPassword = form.get("duplicate-password");

            if (username.isMissing() || password.isMissing()) {
                ErrorResp.throwJson(StatusCode.BAD_REQUEST, "Form data is invalid", jsonMapper);
            }

            var usernameStr = username.toString();
            var passwordStr = password.toString();
            var dupPasswordStr = dupPassword.toString();

            if (usernameStr.length() < 5 || usernameStr.length() > 20) {
                ErrorResp.throwJson(StatusCode.BAD_REQUEST, "Username should be between 5 and 20 characters", jsonMapper);
            }
            if (!Jsoup.isValid(usernameStr, Safelist.basic())) {
                ErrorResp.throwJson(StatusCode.BAD_REQUEST, "Username cannot contain invalid or unsafe characters", jsonMapper);
            }
            if (passwordStr.length() < 10 || passwordStr.length() > 100) {
                ErrorResp.throwJson(StatusCode.BAD_REQUEST, "Password should be between 10 and 100 characters", jsonMapper);
            }
            if (!passwordStr.equals(dupPasswordStr)) {
                ErrorResp.throwJson(StatusCode.BAD_REQUEST, "Password and retyped password must be equal", jsonMapper);
            }

            var inst = accountDao.insert(usernameStr, passwordStr);
            if (inst == null) {
                ErrorResp.throwJson(StatusCode.BAD_REQUEST, "Username is already taken, choose another", jsonMapper);
            }
            assert inst != null;
            var player = new Player(inst.getNewId(), inst.getUsername());

            var sessionId = sessionService.createId();
            var cookie = sessionService.createCookie(sessionId, player);
            ctx.setResponseCookie(cookie);
//            remoteDict.setSession(sessionId, player, cookie.getMaxAge());

            return new FormResp("Signed up successfully!");
        });

        post("/forms/login", ctx -> {
            ctx.setResponseHeader("Content-Type", "application/json");

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
            assert player != null;

            var sessionId = sessionService.createId();
            var cookie = sessionService.createCookie(sessionId, player);
            ctx.setResponseCookie(cookie);
//            remoteDict.setSession(sessionId, player, cookie.getMaxAge());

            return new FormResp("Logged in successfully!");
        });

        post("/forms/logout", ctx -> {
            ctx.setResponseHeader("Content-Type", "application/json");

            var session = sessionService.getSession(ctx);
            remoteDict.deleteSession(session.getSessionId());

            var cookie = sessionService.createEmptyCookie();
            ctx.setResponseCookie(cookie);
            remoteDict.deleteSession(session.getSessionId());

            return new FormResp("Logged out successfully!");
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
