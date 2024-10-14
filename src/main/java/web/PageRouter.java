package web;

import domain.ChessBoard;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.StatusCode;
import models.*;
import utils.Threading;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PageRouter extends Jooby {

    public PageRouter(State state) {
        var userDao = state.getUserDao();
        var historyDao = state.getHistoryDao();
        var gameService = state.getGameService();
        var remoteDict = state.getRemoteDict();
        var templates = state.getTemplates();

        // write the index page at build time since it never changes...
        String indexHtml;
        try {
            var board = ChessBoard.initial();
            var boardJson = board.writePiecesAsJsonString();
            indexHtml = templates.getIndexTemplate().apply(boardJson);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        setWorker(Threading.EXECUTOR);

        use(next -> ctx -> {
            ctx.setResponseType(MediaType.HTML);
            return next.apply(ctx);
        });

        get("*", ctx -> {
            ctx.setResponseCode(StatusCode.NOT_FOUND_CODE);
            var template = templates.getErrorTemplate();
            var message = """
                Sorry, the page you are looking for does not exist.
                You might have followed a broken link or entered a URL that doesn't exist on this site.
                """;
            return template.apply(new ErrorView(404, message));
        });

        get("/", ctx -> indexHtml);

        get("/index", ctx -> indexHtml);

        get("/leaderboard", ctx -> {
            try {
                int page = ctx.query("page").toOptional().map(Integer::parseUnsignedInt).orElse(1);

                var entityList = CompletableFuture.supplyAsync(() -> userDao.getLeaderboard(page, 25), Threading.EXECUTOR);
                var totalPages = CompletableFuture.supplyAsync(() -> userDao.countPages(25), Threading.EXECUTOR);

//                var leaderboard = remoteDict.getLeaderboardPage(page, 20);
//                var entityList = userDao.getByRanks(leaderboard.getUsers());
//                RankedUser.joinRanks(leaderboard.getUsers(), entityList);

                var template = templates.getLeaderboardTemplate();

                return template.apply(new LeaderboardView(entityList.get(),
                    PaginationView.withTotal("?", page, totalPages.get())));
//                return template.apply(new LeaderboardView(entityList,
//                    PaginationView.withTotal("?", page, leaderboard.getPageCount())));
            } catch (NumberFormatException ex) {
                var template = templates.getErrorTemplate();
                return template.apply(new ErrorView(404, "Invalid param 'page': must be a positive integer."));
            }
        });

        get("/players/search", ctx -> {
            try {
                int page = ctx.query("page").toOptional().map(Integer::parseUnsignedInt).orElse(1);
                var name = ctx.query("username").toOptional().orElse("");

                var template = templates.getSearchTemplate();
                if (name.isEmpty()) {
                    return template.apply(new SearchView(name, List.of(), PaginationView.ofUnlimited("?", page)));
                }

                var entityList = userDao.searchByName(name, page, 20);

                return template.apply(new SearchView(name, entityList,
                    PaginationView.ofUnlimited(String.format("?username=%s&", name), page)));
            } catch (NumberFormatException ex) {
                var template = templates.getErrorTemplate();
                return template.apply(new ErrorView(404, "Invalid param 'page': must be a positive integer."));
            }
        });

        get("/players/{id}", ctx -> {
            var userIdSlug = ctx.path("id");
            if (userIdSlug.isMissing()) {
                var template = templates.getErrorTemplate();
                return template.apply(new ErrorView(404, "Invalid param 'id': must contain id within slug."));
            }
            var userId = userIdSlug.toString();

            var userEntity = CompletableFuture.supplyAsync(() -> userDao.getByIdWithRank(userId), Threading.EXECUTOR);
            var historyList = CompletableFuture.supplyAsync(() -> historyDao.getUserHistories(userId, null, 5), Threading.EXECUTOR);

//            var userEntity = userDao.getById(userId);
//            var rank = remoteDict.getLeaderboardRank(entity.getId());
//            userEntity.setRank(rank);

            var template = templates.getProfileTemplate();
            return template.apply(new ProfileView(userEntity.get(), historyList.get()));
        });

//        get("/games/history", ctx -> {
//            var whiteIdParam = ctx.query("whiteId");
//            var whiteId = whiteIdParam.toOptional().orElse(null);
//
//            var blackIdParam = ctx.query("blackId");
//            var blackId = blackIdParam.toOptional().orElse(null);
//
//            Long afterId = ctx.query("afterId").toOptional().map(Long::parseUnsignedLong).orElse(null);
//
//            return historyDao.getHistories(whiteId, blackId, afterId, 20);
//        });
//
//        get("/games/current", ctx -> {
//            var cursorParam = ctx.query("cursor");
//            var cursor = cursorParam.isPresent() ? cursorParam.doubleValue() : null;
//            return gameService.getManyKeys(cursor);
//        });
    }
}
