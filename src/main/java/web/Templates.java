package web;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

@Data
@NoArgsConstructor
public class Templates {

    Template indexTemplate;
    Template loginTemplate;
    Template registerTemplate;
    Template leaderboardTemplate;
    Template profileTemplate;
    Template preferencesTemplates;
    Template currentGamesTemplate;
    Template GameStateoryTemplate;
    Template searchTemplate;
    Template errorTemplate;

    Template historyListTemplate;
    Template searchOptionsTemplate;

    public Templates(Handlebars handlebars) throws IOException {
        // pages
        indexTemplate = handlebars.compile("/pages/index");
        registerTemplate = handlebars.compile("/pages/register");
        loginTemplate = handlebars.compile("/pages/login");
        leaderboardTemplate = handlebars.compile("/pages/leaderboard");
        profileTemplate = handlebars.compile("/pages/profile");
        preferencesTemplates = handlebars.compile("/pages/preferences");
        currentGamesTemplate = handlebars.compile("/pages/currentGames");
        GameStateoryTemplate = handlebars.compile("/pages/gameHistory");
        searchTemplate = handlebars.compile("/pages/searchPlayers");
        errorTemplate = handlebars.compile("/pages/error");

        // partials
        historyListTemplate = handlebars.compile("partials/historyList");
    }
}
