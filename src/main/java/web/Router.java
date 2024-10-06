package web;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.jooby.Jooby;
import redis.clients.jedis.JedisPooled;
import utils.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static utils.Log.LOGGER;

public class Router extends Jooby {

    public static Router init() {
        try {
            var envMap = Config.readEnvConfig();
            var ds = Config.createDataSource(envMap);

            var redisHost = envMap.get("REDIS_HOST");
            var redisPort = Integer.parseInt(envMap.get("REDIS_PORT"));

            var loader = new ClassPathTemplateLoader();
            loader.setPrefix("/templates");
            loader.setSuffix(".hbs");

            var files = Config.createFilesMap();

            var handlebars = new Handlebars(loader);
            var jedis = new JedisPooled(redisHost, redisPort);

            var state = new State(jedis, ds, handlebars, files);
//            var state = new State(null, null, null);
            return new Router(state);
        } catch (Exception ex) {
            LOGGER.error("Error occurred during router init " + ex);
            throw new RuntimeException(ex);
        }
    }

    public Router(State state) {
        mount(new FileRouter(state));
        mount(new PageRouter(state));
        mount(new FormRouter(state));
        mount(new WsRouter(state));
    }
}
