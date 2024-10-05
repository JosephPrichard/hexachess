package utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import web.Router;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Config {

    public static Map<String, String> readEnvConfig() throws IOException {
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
        }
        return envMap;
    }
    public static HikariDataSource createDataSource(Map<String, String> envMap) throws IOException {
        var dbUrl = envMap.get("DB_URL");
        var dbUser = envMap.get("DB_USER");
        var dbPassword = envMap.get("DB_PASSWORD");

        var config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(10);
        config.setAutoCommit(false);
        config.setDriverClassName("org.postgresql.Driver");

        return new HikariDataSource(config);
    }
}
