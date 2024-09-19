package services;

import io.jooby.Cookie;
import io.jooby.SameSite;

import java.security.SecureRandom;

public class SessionService {

    private static final SecureRandom RANDOM = new SecureRandom();

    public String createId() {
        var length = 100;
        var sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            var characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            int index = RANDOM.nextInt(characters.length());
            sb.append(characters.charAt(index));
        }
        return sb.toString();
    }

    public Cookie createCookie(String sessionId) {
        var maxAgeSecs = 6 * 60 * 60; // 6 hours
        return new Cookie("sessionId", sessionId)
            .setSecure(true)
            .setHttpOnly(true)
            .setSameSite(SameSite.STRICT)
            .setMaxAge(maxAgeSecs);
    }
}
