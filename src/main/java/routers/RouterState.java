package routers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import redis.clients.jedis.JedisPooled;
import services.*;

@Getter()
@AllArgsConstructor
public class RouterState {
    private final AccountDao accountDao;
    private final HistoryDao historyDao;
    private final DuelDao duelDao;
    private final DuelService duelService;
    private final BroadcastService broadcastService;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    public RouterState(JedisPooled jedis, HikariDataSource ds) {
        accountDao = new AccountDao(ds);
        historyDao = new HistoryDao(ds);
        duelDao = new DuelDao(jedis);
        duelService = new DuelService(duelDao);
        broadcastService = new BroadcastService(jedis);
    }
}