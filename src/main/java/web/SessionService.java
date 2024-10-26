package web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jooby.Context;
import io.jooby.Cookie;
import io.jooby.SameSite;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import models.Player;

import java.security.SecureRandom;

import static utils.Log.LOGGER;

@AllArgsConstructor
public class SessionService {

    private static final SecureRandom RANDOM = new SecureRandom();
    public static final String COOKIE_NAME = "session";
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public String createId() {
        var length = 100;
        var sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionValue {
        String sessionId;
        String playerId;
        String username;
    }

    public Cookie createCookie(String sessionId, Player player) {
        return createCookie(sessionId, player.getId(), player.getName());
    }

    public Cookie createCookie(String sessionId, String playerId, String username) {
        var sessionCsv = String.format("%s,%s,%s", sessionId, playerId, username);
        var maxAgeSecs = 6 * 60 * 60; // 6 hours

        // our cookie is not set to http only because javascript must read it starting a websocket
        return new Cookie(COOKIE_NAME, sessionCsv)
            .setDomain("localhost")
            .setSecure(true)
            .setSameSite(SameSite.STRICT)
            .setMaxAge(maxAgeSecs);
    }

    public Cookie createEmptyCookie() {
        return new Cookie(COOKIE_NAME, "")
            .setDomain("localhost")
            .setSecure(true)
            .setSameSite(SameSite.STRICT)
            .setMaxAge(1);
    }

    public SessionValue getSession(String cookieStr) {
        if (cookieStr == null) {
            return null;
        }
        var delimIndex = cookieStr.indexOf("=");
        if (delimIndex < 0) {
            return null;
        }
        var fields = cookieStr.substring(delimIndex + 1).split(",");
        if (fields.length < 3) {
            LOGGER.error("Session is in invalid format " + cookieStr);
            return null;
        }
        return new SessionValue(
            fields[0].substring(1),
            fields[1],
            fields[2].substring(0, fields[2].length() - 1));
    }
}
