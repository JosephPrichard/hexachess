package web;

import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.StatusCode;
import models.LeaderboardView;
import utils.ErrorResp;
import utils.Threading;

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

        get("*", ctx -> templates.getNotFoundTemplate().apply(null));

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
            var cursorQuery = ctx.query("cursor");
            var cursor = cursorQuery.toOptional().map(Float::parseFloat).orElse(null);
            var entityList = accountDao.getLeaderboard(cursor, 20);

            var leaderboardTemplate = templates.getLeaderboardTemplate();
            var viewList = LeaderboardView.StatsView.fromEntityList(entityList);
            return leaderboardTemplate.apply(new LeaderboardView(viewList));
        });

        get("/player/{id}/history", ctx -> {
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
