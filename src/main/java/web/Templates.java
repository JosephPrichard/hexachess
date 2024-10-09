package web;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Options;
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
    private final Template currentTemplate;
    private final Template historyTemplate;
    private final Template searchTemplate;
    private final Template errorTemplate;

    public Templates(Handlebars handlebars) throws IOException {
        indexTemplate =  handlebars.compile("index");
        registerTemplate = handlebars.compile("register");
        loginTemplate = handlebars.compile("login");
        leaderboardTemplate = handlebars.compile("leaderboard");
        profileTemplate = handlebars.compile("profile");
        currentTemplate = handlebars.compile("current-games");
        historyTemplate = handlebars.compile("history-games");
        searchTemplate = handlebars.compile("search-players");
        errorTemplate = handlebars.compile("error");

        registerHelpers(handlebars);
    }

    public static void registerHelpers(Handlebars handlebars) {
        handlebars.registerHelper("percentColor", (Integer context, Options options) -> {
            if (context > 50) {
                return "green-color";
            } else if (context < 50) {
                return "red-color";
            }
            return "yellow-color";
        });
    }
}
