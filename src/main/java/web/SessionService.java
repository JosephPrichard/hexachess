package web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jooby.Context;
import io.jooby.Cookie;
import io.jooby.SameSite;
import lombok.AllArgsConstructor;
import lombok.Data;
import models.Player;

import java.security.SecureRandom;

@AllArgsConstructor
public class SessionService {

    private static final SecureRandom RANDOM = new SecureRandom();
    public static final String COOKIE_NAME = "sessionId";
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final ObjectMapper jsonMapper;

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
    @AllArgsConstructor
    public static class SessionValue {
        String sessionId;
        String playerId;
        String username;
    }

    public Cookie createCookie(String sessionId, Player player) throws JsonProcessingException {
        return createCookie(new SessionService.SessionValue(sessionId, player.getId(), player.getName()));
    }

    public Cookie createCookie(SessionValue session) throws JsonProcessingException {
        var sessionJson = jsonMapper.writeValueAsString(session);

        var maxAgeSecs = 6 * 60 * 60; // 6 hours
        // our cookie is not set to http only because javascript must read it starting a websocket
        return new Cookie(COOKIE_NAME, sessionJson)
            .setSecure(true)
            .setSameSite(SameSite.STRICT)
            .setMaxAge(maxAgeSecs);
    }

    public Cookie createEmptyCookie() {
        return new Cookie(COOKIE_NAME, "")
            .setSecure(true)
            .setSameSite(SameSite.STRICT)
            .setMaxAge(1);
    }

    public SessionValue getSession(Context ctx) throws JsonProcessingException {
        var cookie = ctx.cookie(COOKIE_NAME).valueOrNull();
        if (cookie == null) {
            return null;
        }
        return jsonMapper.readValue(cookie, SessionValue.class);
    }
}
