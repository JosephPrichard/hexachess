package utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbutils.QueryRunner;
import web.Router;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static utils.Log.LOGGER;

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
    public static HikariDataSource createDataSource(Map<String, String> envMap) {
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

    public static Map<String, byte[]> createFilesMap() {
        Map<String, byte[]> files = new HashMap<>();

        var classLoader = Thread.currentThread().getContextClassLoader();
        try {
            var resourcePath = Paths.get(Objects.requireNonNull(classLoader.getResource("flags")).toURI());
            try (Stream<Path> paths = Files.walk(resourcePath)) {
                paths.filter(Files::isRegularFile).forEach(filePath -> {
                    try (var inputStream = classLoader.getResourceAsStream("flags/" + filePath.getFileName().toString())) {
                        if (inputStream != null) {
                            files.put(filePath.getFileName().toString(), inputStream.readAllBytes());
                        }
                    } catch (IOException ex) {
                        LOGGER.error("Error occurred while stepping through files " + ex);
                        throw new RuntimeException(ex);
                    }
                });
            }
        } catch (URISyntaxException | IOException ex) {
            LOGGER.error("Error occurred while creating files map " + ex);
            throw new RuntimeException(ex);
        }

        return files;
    }

    public static void executeQuery(QueryRunner runner, String path) {
        var classLoader = Thread.currentThread().getContextClassLoader();
        try (var inputStream = classLoader.getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new RuntimeException("Expected input stream to be non null");
            }
            var sql = new String(inputStream.readAllBytes());
            runner.update(sql);
        } catch (SQLException | IOException ex) {
            LOGGER.error("Error occurred while creating schema " + ex);
            throw new RuntimeException(ex);
        }
    }

    public static void createSchema(DataSource ds) {
        try {
            var runner = new QueryRunner(ds);
            runner.execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public;");
            executeQuery(runner, "database/schema.sql");
        } catch (SQLException ex) {
            LOGGER.error("Error occurred while creating schema " + ex);
            throw new RuntimeException(ex);
        }
    }
}
