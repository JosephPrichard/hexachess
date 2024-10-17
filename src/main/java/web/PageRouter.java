package web;

import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.Route;
import io.jooby.StatusCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import models.HistoryEntity;
import models.Pagination;
import models.UserEntity;
import utils.Threading;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static utils.Log.LOGGER;

public class PageRouter extends Jooby {

    @Data
    @AllArgsConstructor
    public static class LeaderboardView {
        List<UserEntity> userList;
        Pagination pages;
    }

    @Data
    @AllArgsConstructor
    public static class ProfileView {
        UserEntity user;
        List<HistoryEntity> historyList;
    }

    @Data
    @AllArgsConstructor
    public static class SearchView {
        String searchText;
        List<UserEntity> userList;
        Pagination pages;
    }

    @Data
    @AllArgsConstructor
    public static class GameView {
        String whiteName;
        String blackName;
        List<HistoryEntity> historyList;
    }

    public PageRouter(State state) {
        var userDao = state.getUserDao();
        var historyDao = state.getHistoryDao();
        var gameService = state.getGameService();
        var remoteDict = state.getRemoteDict();
        var templates = state.getTemplates();

        setWorker(Threading.EXECUTOR);

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
            var code = StatusCode.NOT_FOUND_CODE;
            ctx.setResponseCode(code);
            return template.apply(new Router.ErrorView(code, message));
        });

        Route.Handler indexHandler = (ctx) -> {
            var gamesResult = gameService.getGames(null);

            var template = templates.getIndexTemplate();
            return template.apply(gamesResult);
        };

        get("/", indexHandler);

        get("/index", indexHandler);

        get("/play", indexHandler);

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
                    Pagination.withTotal("?", page, totalPages.get())));
//                return template.apply(new LeaderboardView(entityList,
//                    PaginationView.withTotal("?", page, leaderboard.getPageCount())));
            } catch (NumberFormatException ex) {
                var code = StatusCode.BAD_REQUEST_CODE;
                ctx.setResponseCode(code);
                var template = templates.getErrorTemplate();
                return template.apply(new Router.ErrorView(code, "Invalid param 'page': must be a positive integer."));
            }
        });

        get("/players/search", ctx -> {
            try {
                int page = ctx.query("page").toOptional().map(Integer::parseUnsignedInt).orElse(1);
                var name = ctx.query("username").toOptional().orElse("");

                var template = templates.getSearchTemplate();
                if (name.isEmpty()) {
                    return template.apply(new SearchView(name, List.of(), Pagination.ofUnlimited("?", page)));
                }

                var entityList = userDao.searchByName(name, page, 20);

                return template.apply(new SearchView(name, entityList,
                    Pagination.ofUnlimited(String.format("?username=%s&", name), page)));
            } catch (NumberFormatException ex) {
                var code = StatusCode.BAD_REQUEST_CODE;
                ctx.setResponseCode(code);
                var template = templates.getErrorTemplate();
                return template.apply(new Router.ErrorView(code, "Invalid param 'page': must be a positive integer."));
            }
        });

        get("/players/{id}", ctx -> {
            var userIdSlug = ctx.path("id");
            if (userIdSlug.isMissing()) {
                var code = StatusCode.BAD_REQUEST_CODE;
                ctx.setResponseCode(code);
                var template = templates.getErrorTemplate();
                return template.apply(new Router.ErrorView(code, "Invalid param 'id': must contain id within slug."));
            }
            var userId = userIdSlug.toString();

            var userEntity = CompletableFuture.supplyAsync(() -> userDao.getByIdWithRank(userId), Threading.EXECUTOR);
            var historyList = CompletableFuture.supplyAsync(() -> historyDao.getUserHistories(userId, null, 10), Threading.EXECUTOR);

//            var userEntity = userDao.getById(userId);
//            var rank = remoteDict.getLeaderboardRank(entity.getId());
//            userEntity.setRank(rank);

            var template = templates.getProfileTemplate();
            return template.apply(new ProfileView(userEntity.get(), historyList.get()));
        });

        get("/games", ctx -> {
            var whiteId = ctx.query("whiteId").toOptional().orElse(null);
            var blackId = ctx.query("blackId").toOptional().orElse(null);
            var historyList = historyDao.getHistories(whiteId, blackId, null, 20);

            var template = templates.getGameHistoryTemplate();
            return template.apply(new GameView(whiteId, blackId, historyList));
        });

        get("/profile", ctx -> {
            var cookie = ctx.header("Cookie");
            if (cookie.isMissing()) {
                return ctx.sendRedirect("/login");
            } else {
                var sessionId = cookie.toString();
                var player = remoteDict.getSession(sessionId);
                return ctx.sendRedirect("/players/" + player.getId());
            }
        });
    }
}
