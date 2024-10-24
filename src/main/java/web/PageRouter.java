package web;

import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.Route;
import io.jooby.StatusCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import models.HistEntity;
import models.Pagination;
import models.UserEntity;
import utils.Threading;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
        List<HistEntity> historyList;
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
    public static class ErrorView {
        int code;
        String message;
    }

    private final String loginHtml;
    private final String registerHtml;

    public PageRouter(State state) {
        var userDao = state.getUserDao();
        var historyDao = state.getHistoryDao();
        var gameService = state.getGameService();
        var remoteDict = state.getRemoteDict();
        var sessionService = state.getSessionService();
        var templates = state.getTemplates();

        loginHtml = Templates.applyQuietly(templates.getLoginTemplate());
        registerHtml = Templates.applyQuietly(templates.getRegisterTemplate());

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
            return template.apply(new ErrorView(code, message));
        });

        Route.Handler indexHandler = (ctx) -> {
//            var gamesResult = gameService.getGames(null);
            var template = templates.getIndexTemplate();
            return template.apply(null);
        };

        get("/", indexHandler);

        get("/index", indexHandler);

        get("/play", indexHandler);

        get("/login", ctx -> loginHtml);

        get("/register", ctx -> registerHtml);

        // this route is un-cacheable due to using cookies
        get("/settings", ctx -> {
            var cookieStr =  ctx.header("Cookie").valueOrNull();

            var session = sessionService.getSession(cookieStr);
            if (session == null) {
                var code = StatusCode.UNAUTHORIZED_CODE;
                ctx.setResponseCode(code);
                var template = templates.getErrorTemplate();
                return template.apply(new ErrorView(code, "You must be logged in to access this page."));
            }

            var userEntity = userDao.getById(session.getPlayerId());

            var template = templates.getSettingsTemplate();
            return template.apply(userEntity);
        });

        get("/leaderboard", ctx -> {
            try {
                int page = ctx.query("page").toOptional().map(Integer::parseUnsignedInt).orElse(1);

                var entityListFut = CompletableFuture.supplyAsync(() -> userDao.getLeaderboard(page, 25), Threading.EXECUTOR);
                var totalPagesFut = CompletableFuture.supplyAsync(() -> userDao.countPages(25), Threading.EXECUTOR);
                var entityList = entityListFut.get();
                var totalPages = totalPagesFut.get();

                entityList.forEach(UserEntity::sanitize);

                var template = templates.getLeaderboardTemplate();
                return template.apply(new LeaderboardView(entityList, Pagination.withTotal("?", page, totalPages)));
            } catch (NumberFormatException ex) {
                var code = StatusCode.BAD_REQUEST_CODE;
                ctx.setResponseCode(code);
                var template = templates.getErrorTemplate();
                return template.apply(new ErrorView(code, "Invalid param 'page': must be a positive integer."));
            }
        });

//        get("/leaderboard", ctx -> {
//            try {
//                int page = ctx.query("page").toOptional().map(Integer::parseUnsignedInt).orElse(1);
//
//                var leaderboard = remoteDict.getLeaderboardPage(page, 20);
//                var entityList = userDao.getByRanks(leaderboard.getUsers());
//
//                RankedUser.joinRanks(leaderboard.getUsers(), entityList);
//                entityList.forEach(UserEntity::sanitize);
//
//                var template = templates.getLeaderboardTemplate();
//                return template.apply(new LeaderboardView(entityList, Pagination.withTotal("?", page, leaderboard.getPageCount())));
//            } catch (NumberFormatException ex) {
//                var code = StatusCode.BAD_REQUEST_CODE;
//                ctx.setResponseCode(code);
//                var template = templates.getErrorTemplate();
//                return template.apply(new Router.ErrorView(code, "Invalid param 'page': must be a positive integer."));
//            }
//        });

        get("/players/{id}", ctx -> {
            var userIdSlug = ctx.path("id");
            if (userIdSlug.isMissing()) {
                var code = StatusCode.BAD_REQUEST_CODE;
                ctx.setResponseCode(code);
                var template = templates.getErrorTemplate();
                return template.apply(new ErrorView(code, "Invalid param 'id': must contain id within slug."));
            }
            var userId = userIdSlug.toString();

            var userEntityFut = CompletableFuture.supplyAsync(() -> userDao.getByIdWithRank(userId), Threading.EXECUTOR);
            var historyListFut = CompletableFuture.supplyAsync(() -> historyDao.getUserHistories(userId, null, 25), Threading.EXECUTOR);
            var userEntity = userEntityFut.get();
            var historyList = historyListFut.get();

            userEntity.sanitize();
            historyList.forEach(HistEntity::sanitize);

            var template = templates.getProfileTemplate();
            return template.apply(new ProfileView(userEntity, historyList));
        });

//        get("/players/{id}", ctx -> {
//            var userIdSlug = ctx.path("id");
//            if (userIdSlug.isMissing()) {
//                var code = StatusCode.BAD_REQUEST_CODE;
//                ctx.setResponseCode(code);
//                var template = templates.getErrorTemplate();
//                return template.apply(new Router.ErrorView(code, "Invalid param 'id': must contain id within slug."));
//            }
//            var userId = userIdSlug.toString();
//
//            var userEntityFut = CompletableFuture.supplyAsync(() -> userDao.getById(userId), Threading.EXECUTOR);
//            var historyListFut = CompletableFuture.supplyAsync(() -> historyDao.getUserHistories(userId, null, 10), Threading.EXECUTOR);
//
//            var userEntity = userEntityFut.get();
//            var rank = remoteDict.getLeaderboardRank(userEntity.getId());
//            userEntity.setRank(rank);
//            var historyList = historyListFut.get();
//
//            userEntity.sanitize();
//            historyList.forEach(HistoryEntity::sanitize);
//
//            var template = templates.getProfileTemplate();
//            return template.apply(new ProfileView(userEntity, historyList));
//        });

        get("/players/search", ctx -> {
            try {
                int page = ctx.query("page").toOptional().map(Integer::parseUnsignedInt).orElse(1);
                var name = ctx.query("username").toOptional().orElse("");

                var template = templates.getSearchTemplate();
                if (name.isEmpty()) {
                    return template.apply(new SearchView(name, List.of(), Pagination.ofUnlimited("?", page)));
                }

                var entityList = userDao.searchByName(name, page, 20);
                entityList.forEach(UserEntity::sanitize);

                var pagination = Pagination.ofUnlimited(String.format("?username=%s&", name), page);
                return template.apply(new SearchView(name, entityList, pagination));
            } catch (NumberFormatException ex) {
                var code = StatusCode.BAD_REQUEST_CODE;
                ctx.setResponseCode(code);
                var template = templates.getErrorTemplate();
                return template.apply(new ErrorView(code, "Invalid param 'page': must be a positive integer."));
            }
        });
    }
}
