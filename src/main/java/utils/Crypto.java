package utils;

import java.security.SecureRandom;

public class Crypto {
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String createToken() {
        var length = 100;
        var sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            var characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            int index = RANDOM.nextInt(characters.length());
            sb.append(characters.charAt(index));
        }
        return sb.toString();
    }
}
