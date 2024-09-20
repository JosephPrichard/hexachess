package web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.zaxxer.hikari.HikariDataSource;
import lombok.*;
import redis.clients.jedis.JedisPooled;
import services.*;

import java.io.IOException;
import java.util.HashMap;

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

    private final ObjectMapper jsonMapper = new ObjectMapper();

    private Template homeTemplate;

    public ServerState(JedisPooled jedis, HikariDataSource ds, Handlebars handlebars) throws IOException {
        accountDao = new AccountDao(ds);
        historyDao = new HistoryDao(ds);
        dictService = new DictService(jedis);
        duelService = new DuelService(dictService);
        sessionService = new SessionService();
        broadcastService = new BroadcastService(jedis);

        homeTemplate = handlebars.compile("home");
    }
}