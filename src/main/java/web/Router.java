package web;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.jooby.Jooby;
import models.ErrorView;
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
            var jedis = new JedisPooled(redisHost, redisPort);

            var loader = new ClassPathTemplateLoader();
            loader.setPrefix("/templates");
            loader.setSuffix(".hbs");
            var handlebars = new Handlebars(loader);

            var files = Config.createFilesMap();

            var state = new State(jedis, ds, handlebars, files);
//            var state = new State(null, null, null);
            return new Router(state);
        } catch (Exception ex) {
            LOGGER.error("Error occurred during router init " + ex);
            throw new RuntimeException(ex);
        }
    }

    public Router(State state) {
        error((ctx, cause, statusCode) -> {
            var template = state.getTemplates().getErrorTemplate();
            try {
                LOGGER.error(cause.toString());
                ctx.send(template.apply(new ErrorView(statusCode.value(), "Unexpected error has occurred. Contact website administrator.")));
            } catch (Exception e) {
                ctx.send("Unexpected error occurred while handling another error! Contact website administrator.");
            }
        });

        mount(new FileRouter(state));
        mount(new PageRouter(state));
        mount(new FormRouter(state));
        mount(new WsRouter(state));
    }
}
