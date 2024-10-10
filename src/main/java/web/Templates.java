package web;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.IOException;

@Data
@NoArgsConstructor
public class Templates {

    private Template indexTemplate;
    private Template loginTemplate;
    private Template registerTemplate;
    private Template leaderboardTemplate;
    private Template profileTemplate;
    private Template currentTemplate;
    private Template historyTemplate;
    private Template searchTemplate;
    private Template errorTemplate;

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
    }
}
