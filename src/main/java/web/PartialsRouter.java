package web;

import io.jooby.Jooby;
import io.jooby.StatusCode;
import models.ErrorView;

public class PartialsRouter extends Jooby {

    public PartialsRouter(State state) {
        var historyDao = state.getHistoryDao();
        var templates = state.getTemplates();

        get("/partials/*", ctx -> {
            ctx.setResponseCode(StatusCode.NOT_FOUND_CODE);
            return "";
        });

        get("/partials/players/{id}/history", ctx -> {
            var userIdSlug = ctx.path("id");
            if (userIdSlug.isMissing()) {
                var template = templates.getErrorTemplate();
                return template.apply(new ErrorView(404, "Invalid param 'id': must contain id within slug."));
            }
            var userId = userIdSlug.toString();
            Long afterId = ctx.query("afterId").toOptional().map(Long::parseUnsignedLong).orElse(null);

            var historyList = historyDao.getUserHistories(userId, afterId, 20);

            var template = templates.getHistoryListTemplate();
            return template.apply(historyList);
        });
    }
}
