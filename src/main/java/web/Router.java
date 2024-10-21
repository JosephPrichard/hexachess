package web;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import io.jooby.Jooby;
import io.jooby.jackson.JacksonModule;
import lombok.AllArgsConstructor;
import lombok.Data;
import redis.clients.jedis.JedisPooled;
import utils.Config;

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

    @Data
    @AllArgsConstructor
    public static class ErrorView {
        int code;
        String message;
    }

    public Router(State state) {
        error((ctx, cause, statusCode) -> {
            LOGGER.error(cause.toString());
            ctx.setResponseCode(statusCode);
            if (statusCode.value() == 500) {
                ctx.send("");
            } else {
                ctx.send(cause.getMessage());
            }
        });

        install(new JacksonModule());

        mount(new FileRouter(state));
        mount(new PageRouter(state));
        mount(new PartialsRouter(state));
        mount(new FormRouter(state));
        mount(new WsRouter(state));
    }
}
