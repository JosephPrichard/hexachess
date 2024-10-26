package web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jooby.Jooby;
import io.jooby.StatusCode;
import io.jooby.exception.StatusCodeException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import models.Player;
import org.jsoup.Jsoup;
import services.UserDao;
import utils.Html;

import static utils.Globals.EXECUTOR;
import static utils.Globals.JSON_MAPPER;
import static utils.Log.LOGGER;

public class FormRouter extends Jooby {

    @Getter
    @AllArgsConstructor
    static class FormResp {
        String message;

        public static String ofJson(String message) throws JsonProcessingException {
            return JSON_MAPPER.writeValueAsString(new FormResp(message));
        }
    }

    public FormRouter(State state) {
        var userDao = state.getUserDao();
        var remoteDict = state.getRemoteDict();
        var sessionService = state.getSessionService();
        var gameService = state.getGameService();

        setWorker(EXECUTOR);

        post("/forms/signup", ctx -> {
            ctx.setResponseHeader("Content-Type", "application/json");

            var form = ctx.form();
            var username = form.get("username");
            var password = form.get("password");
            var dupPassword = form.get("duplicate-password");

            if (username.isMissing() || password.isMissing()) {
                throw new StatusCodeException(StatusCode.BAD_REQUEST, FormResp.ofJson("Form data is invalid"));
            }

            var usernameStr = username.toString();
            var passwordStr = password.toString();
            var dupPasswordStr = dupPassword.toString();

            if (usernameStr.length() < 5 || usernameStr.length() > 20) {
                throw new StatusCodeException(StatusCode.BAD_REQUEST, FormResp.ofJson("Username should be between 5 and 20 characters"));
            }
            if (!Jsoup.isValid(usernameStr, Html.SAFELIST)) {
                throw new StatusCodeException(StatusCode.BAD_REQUEST, FormResp.ofJson("Username cannot contain invalid or unsafe characters"));
            }
            if (passwordStr.length() < 10 || passwordStr.length() > 100) {
                throw new StatusCodeException(StatusCode.BAD_REQUEST, FormResp.ofJson("Password should be between 10 and 100 characters"));
            }
            if (!passwordStr.equals(dupPasswordStr)) {
                throw new StatusCodeException(StatusCode.BAD_REQUEST, FormResp.ofJson("Password and retyped password must be equal"));
            }

            try {
                var inst = userDao.insert(usernameStr, passwordStr);

                var player = new Player(inst.getNewId(), inst.getUsername());

                var sessionId = sessionService.createId();
                var cookie = sessionService.createCookie(sessionId, player);
                ctx.setResponseCookie(cookie);
                remoteDict.setSession(sessionId, player, cookie.getMaxAge());

                LOGGER.info("Registered a new player: " + player);

                return new FormResp("Signed up successfully!");
            } catch (UserDao.TakenUsernameException ex) {
                throw new StatusCodeException(StatusCode.BAD_REQUEST, FormResp.ofJson("Username is already taken, choose another"));
            }
        });

        post("/forms/login", ctx -> {
            ctx.setResponseHeader("Content-Type", "application/json");

            var form = ctx.form();
            var username = form.get("username");
            var password = form.get("password");
            if (username.isMissing() || password.isMissing()) {
                throw new StatusCodeException(StatusCode.BAD_REQUEST, FormResp.ofJson("Form data is invalid"));
            }

            var usernameStr = username.toString();
            var passwordStr = password.toString();

            var player = userDao.verify(usernameStr, passwordStr);
            if (player == null) {
                throw new StatusCodeException(StatusCode.UNAUTHORIZED, FormResp.ofJson("Login credentials are invalid"));
            }

            var sessionId = sessionService.createId();
            var cookie = sessionService.createCookie(sessionId, player);
            ctx.setResponseCookie(cookie);
            remoteDict.setSession(sessionId, player, cookie.getMaxAge());

            LOGGER.info("Player has logged in: " + player);

            return new FormResp("Logged in successfully!");
        });

        post("/forms/update-user", ctx -> {
            ctx.setResponseHeader("Content-Type", "application/json");

            var form = ctx.form();
            var username = form.get("username");
            var password = form.get("password");

            if (username.isMissing() || password.isMissing()) {
                throw new StatusCodeException(StatusCode.BAD_REQUEST, FormResp.ofJson("Form data is invalid"));
            }

            var usernameStr = username.toString();
            var passwordStr = password.toString();
            var newUsernameStr = form.get("new-username").valueOrNull();
            var newCountryStr = form.get("new-country").valueOrNull();
            var newPasswordStr = form.get("new-password").valueOrNull();

            var player = userDao.verify(usernameStr, passwordStr);
            if (player == null) {
                throw new StatusCodeException(StatusCode.UNAUTHORIZED, FormResp.ofJson("Login credentials are invalid"));
            }
            userDao.update(player.getId(), newUsernameStr, newCountryStr, newPasswordStr);

            LOGGER.info(String.format("Updated data of user: %s to newUsername: %s newCountry: %s", player, newUsernameStr, newCountryStr));

            return new FormResp("Updated password successfully!");
        });

        post("/forms/update-session", ctx -> {
            ctx.setResponseHeader("Content-Type", "application/json");

            var cookieStr =  ctx.header("Cookie").valueOrNull();

            var session = sessionService.getSession(cookieStr);
            if (session == null) {
                var cookie = sessionService.createEmptyCookie();
                ctx.setResponseCookie(cookie);
            } else {
                var cookie = sessionService.createCookie(session.getSessionId(), new Player(session.getPlayerId(), session.getUsername()));
                ctx.setResponseCookie(cookie);
                remoteDict.updateSessionEx(session.getSessionId(), cookie.getMaxAge());
            }

            LOGGER.info("Updated the session for player: " + session);

            return new FormResp("Updated session successfully!");
        });

        post("/forms/logout", ctx -> {
            ctx.setResponseHeader("Content-Type", "application/json");

            var cookieStr =  ctx.header("Cookie").valueOrNull();

            var session = sessionService.getSession(cookieStr);
            remoteDict.deleteSession(session.getSessionId());

            var cookie = sessionService.createEmptyCookie();
            ctx.setResponseCookie(cookie);

            LOGGER.info("Player has logged out: " + session);

            return new FormResp("Logged out successfully!");
        });

        post("/forms/create-game", ctx -> {
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
