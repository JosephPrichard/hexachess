package web;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import lombok.Getter;

import java.io.IOException;

@Getter
public class Templates {

    private final Template indexTemplate;
    private final Template loginTemplate;
    private final Template registerTemplate;
    private final Template leaderboardTemplate;
    private final Template profileTemplate;
    private final Template currentGames;
    private final Template historyGames;
    private final Template searchPlayers;
    private final Template notFoundTemplate;

    public Templates(Handlebars handlebars) throws IOException {
        indexTemplate =  handlebars.compile("index");
        registerTemplate = handlebars.compile("register");
        loginTemplate = handlebars.compile("login");
        leaderboardTemplate = handlebars.compile("leaderboard");
        profileTemplate = handlebars.compile("profile");
        currentGames = handlebars.compile("current-games");
        historyGames = handlebars.compile("history-games");
        searchPlayers = handlebars.compile("search-players");
        notFoundTemplate = handlebars.compile("404");
    }
}
