package web;

import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.StatusCode;
import models.ErrorView;
import models.LeaderboardView;
import models.SearchView;
import models.StatsView;
import utils.ErrorResp;
import utils.Threading;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class PageRouter extends Jooby {

    public PageRouter(State state) {
        var jsonMapper = state.getJsonMapper();
        var accountDao = state.getUserDao();
        var historyDao = state.getHistoryDao();
        var gameService = state.getGameService();
        var templates = state.getTemplates();

        setWorker(Threading.VIRTUAL_EXECUTOR);

        assets("/css/index.css", "/css/index.css");

        use(next -> ctx -> {
            ctx.setResponseType(MediaType.HTML);
            return next.apply(ctx);
        });

        get("*", ctx -> {
            var template = templates.getErrorTemplate();
            var message = """
                Sorry, the page you are looking for does not exist.
                You might have followed a broken link or entered a URL that doesn't exist on this site.
                """;
            return template.apply(new ErrorView(404, message));
        });

        get("/", ctx -> templates.getIndexTemplate().apply(null));

        get("/index", ctx -> templates.getIndexTemplate().apply(null));

        get("/players/{id}/stats", ctx -> {
            var accountIdSlug = ctx.path("id");
            if (accountIdSlug.isMissing()) {
                ErrorResp.throwJson(StatusCode.BAD_REQUEST, "Invalid request: must contain id within slug", jsonMapper);
            }
            var accountId = accountIdSlug.toString();

            return accountDao.getStats(accountId);
        });

        get("/leaderboard", ctx -> {
            try {
                var page = ctx.query("page").toOptional().map(Integer::parseUnsignedInt).orElse(1);

                var entityList = accountDao.getLeaderboard(page, 20);
                var template = templates.getLeaderboardTemplate();
                var viewList = StatsView.fromEntityList(entityList);
                return template.apply(new LeaderboardView(viewList));
            } catch (NumberFormatException ex) {
                var template = templates.getErrorTemplate();
                return template.apply(new ErrorView(404, "Invalid param 'page' - must be a positive integer."));
            }
        });

        get("/players/search", ctx -> {
            try {
                int page = ctx.query("page").toOptional().map(Integer::parseUnsignedInt).orElse(1);
                var name = ctx.query("username").toOptional().orElse("");

                var template = templates.getSearchTemplate();
                if (!name.isEmpty()) {
                    var entityList = accountDao.searchUsers(name, page, 20);
                    var viewList = StatsView.fromEntityList(entityList);
                    return template.apply(new SearchView(name, viewList));
                } else {
                    return template.apply(new SearchView(name, List.of()));
                }
            } catch (NumberFormatException ex) {
                var template = templates.getErrorTemplate();
                return template.apply(new ErrorView(404, "Invalid param 'page' - must be a positive integer."));
            }
        });

        get("/players/{id}/history", ctx -> {
            var accountIdSlug = ctx.path("id");
            if (accountIdSlug.isMissing()) {
                ErrorResp.throwJson(StatusCode.BAD_REQUEST, "Invalid request: must contain id within slug", jsonMapper);
            }
            var accountId = accountIdSlug.toString();

            var cursorParam = ctx.query("cursor");
            var cursor = cursorParam.toOptional().orElse(null);

            return historyDao.getHistories(accountId, cursor);
        });

        get("/games/history", ctx -> {
            var whiteIdParam = ctx.query("whiteId");
            var whiteId = whiteIdParam.toOptional().orElse(null);

            var blackIdParam = ctx.query("blackId");
            var blackId = blackIdParam.toOptional().orElse(null);

            var cursorParam = ctx.query("cursor");
            var cursor = cursorParam.toOptional().orElse(null);

            return historyDao.getHistories(whiteId, blackId, cursor);
        });

        get("/games/current", ctx -> {
            var cursorParam = ctx.query("cursor");
            var cursor = cursorParam.isPresent() ? cursorParam.doubleValue() : null;
            return gameService.getManyKeys(cursor);
        });
    }
}
