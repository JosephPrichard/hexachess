package web;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import redis.clients.jedis.JedisPooled;
import services.*;

import java.io.IOException;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerState {
    private AccountDao accountDao;
    private HistoryDao historyDao;
    private DictService dictService;
    private DuelService duelService;
    private SessionService sessionService;
    private BroadcastService broadcastService;

    private Template homeTemplate;

    private final ObjectMapper jsonMapper = new ObjectMapper()
        .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    public ServerState(JedisPooled jedis, HikariDataSource ds, Handlebars handlebars) throws IOException {
        accountDao = new AccountDao(ds);
        historyDao = new HistoryDao(ds);
        dictService = new RemoteDictService(jedis);
        duelService = new DuelService(dictService);
        sessionService = new SessionService();
        broadcastService = new BroadcastService(jedis);

        homeTemplate = handlebars.compile("home");
    }
}