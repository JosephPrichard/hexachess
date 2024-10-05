package web;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Handlebars;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import redis.clients.jedis.JedisPooled;
import services.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static utils.Log.LOGGER;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class State {
    private UserDao userDao;
    private HistoryDao historyDao;
    private RemoteDict remoteDict;
    private GameService gameService;
    private SessionService sessionService;
    private Broadcaster broadcaster;
    private Templates templates;
    private Map<String, byte[]> filesMap;

    private final ObjectMapper jsonMapper = new ObjectMapper()
        .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    public static Map<String, byte[]> createFilesMap() {
        Map<String, byte[]> filesMap = new HashMap<>();

        var classLoader = Thread.currentThread().getContextClassLoader();
        try {
            var resourcePath = Paths.get(Objects.requireNonNull(classLoader.getResource("flags")).toURI());
            try (Stream<Path> paths = Files.walk(resourcePath)) {
                paths.filter(Files::isRegularFile).forEach(filePath -> {
                    try (var inputStream = classLoader.getResourceAsStream("flags/" + filePath.getFileName().toString())) {
                        if (inputStream != null) {
                            filesMap.put(filePath.getFileName().toString(), inputStream.readAllBytes());
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

        return filesMap;
    }

    public State(JedisPooled jedis, HikariDataSource ds, Handlebars handlebars) throws IOException {
        userDao = new UserDao(ds);
        historyDao = new HistoryDao(ds);
        remoteDict = new RemoteDict(jedis, jsonMapper);
        gameService = new GameService(remoteDict, userDao, historyDao);
        sessionService = new SessionService();
        broadcaster = new GlobalBroadcaster(jedis);
        templates = new Templates(handlebars);
        filesMap = createFilesMap();
    }
}