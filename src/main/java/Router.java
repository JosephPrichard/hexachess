import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.jooby.Jooby;
import io.jooby.jackson.JacksonModule;
import redis.clients.jedis.JedisPooled;
import web.RestRouter;
import web.ServerState;
import web.WsRouter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Router extends Jooby {

    public static Router init() {
        try {
            var envMap = readEnv();

            var dbUrl = envMap.get("DB_URL");
            var dbUser = envMap.get("DB_USER");
            var dbPassword = envMap.get("DB_PASSWORD");
            var redisHost = envMap.get("REDIS_HOST");
            var redisPort = Integer.parseInt(envMap.get("REDIS_PORT"));

            var config = new HikariConfig();
            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUser);
            config.setPassword(dbPassword);
            config.setMaximumPoolSize(10);
            config.setAutoCommit(false);

            var loader = new ClassPathTemplateLoader();
            loader.setPrefix("/templates");
            loader.setSuffix(".hbs");

            var handlebars = new Handlebars(loader);
            var ds = new HikariDataSource(config);
            var jedis = new JedisPooled(redisHost, redisPort);

            var state = new ServerState(jedis, ds, handlebars);
            return new Router(state);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Map<String, String> readEnv() {
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
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        return envMap;
    }

    public Router(ServerState state) {
        install(new JacksonModule());
        mount(new RestRouter(state));
        mount(new WsRouter(state));
    }

    public static void main(String[] args) {
        runApp(args, Router::init);
    }
}
