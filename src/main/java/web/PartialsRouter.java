package web;

import io.jooby.Jooby;
import io.jooby.StatusCode;
import web.State;

import static utils.Log.LOGGER;

public class PartialsRouter extends Jooby {

    public PartialsRouter(State state) {
        var userDao = state.getUserDao();
        var historyDao = state.getHistoryDao();
        var templates = state.getTemplates();

        get("/partials/*", ctx -> {
            ctx.setResponseCode(StatusCode.NOT_FOUND_CODE);
            return "";
        });

        get("/partials/player-history", ctx -> {
                var userIdSlug = ctx.query("id");
                if (userIdSlug.isMissing()) {
                    ctx.setResponseCode(StatusCode.BAD_REQUEST_CODE);
                    return "";
                }
                var userId = userIdSlug.toString();
                Long afterId = ctx.query("afterId").toOptional().map(Long::parseUnsignedLong).orElse(null);

                var historyList = historyDao.getUserHistories(userId, afterId, 10);
                if (historyList.isEmpty()) {
                    ctx.setResponseCode(StatusCode.NOT_FOUND_CODE);
                    return "";
                }

                var template = templates.getHistoryListTemplate();
                return template.apply(historyList);
        });

        get("/partials/player-options", ctx -> {
                var nameIdSlug = ctx.query("name");
                if (nameIdSlug.isMissing()) {
                    ctx.setResponseCode(400);
                    return "";
                }
                var name = nameIdSlug.toString();

                var userList = userDao.quickSearchByName(name);

                var template = templates.getSearchOptionsTemplate();
                return template.apply(userList);
        });
    }
}
