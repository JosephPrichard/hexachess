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
import java.util.Map;

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
    private Map<String, byte[]> files;

    private final ObjectMapper jsonMapper = new ObjectMapper()
        .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    public State(JedisPooled jedis, HikariDataSource ds, Handlebars handlebars, Map<String, byte[]> filesMap) throws IOException {
        userDao = new UserDao(ds);
        historyDao = new HistoryDao(ds);
        remoteDict = new RemoteDict(jedis, jsonMapper);
        gameService = new GameService(remoteDict, userDao, historyDao);
        sessionService = new SessionService();
        broadcaster = new GlobalBroadcaster(jedis);
        templates = new Templates(handlebars);
        files = filesMap;
    }
}