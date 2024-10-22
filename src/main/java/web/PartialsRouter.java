package web;

import io.jooby.Jooby;
import io.jooby.StatusCode;
import models.HistoryEntity;
import models.UserEntity;
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

            historyList.forEach(HistoryEntity::sanitize);

            var template = templates.getHistoryListTemplate();
            return template.apply(historyList);
        });
    }
}
