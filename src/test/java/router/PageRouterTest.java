package router;

import com.github.jknack.handlebars.Template;
import io.jooby.test.MockContext;
import io.jooby.test.MockRouter;
import models.UserEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import services.*;
import web.PageRouter;
import web.State;
import web.Templates;

import java.sql.SQLException;
import java.util.List;

import static org.mockito.Mockito.*;

public class PageRouterTest {

    @Test
    public void testGetStats() {
        // given
        var state = new State();
        var accountId = "1";
        var user = new UserEntity("1", "testUser", "us", 1050, 5, 3, 0);

        var mockAccountDao = mock(UserDao.class);
        when(mockAccountDao.getById(accountId)).thenReturn(user);
        state.setUserDao(mockAccountDao);

        var mockTemplate = mock(Template.class);
        var templates = new Templates();
        templates.setProfileTemplate(mockTemplate);
        state.setTemplates(templates);

        var mockRouter = new MockRouter(new PageRouter(state));

        // when
        mockRouter.get("/players/" + accountId);

        // then
        verify(mockAccountDao, times(1)).getById(accountId);
    }

    @Test
    public void testGetLeaderboard() {
        // given
        var state = new State();
        var leaderboard = List.of(
            new UserEntity("1", "testUser1", "us", 1000, 3, 3, 0),
            new UserEntity("2", "testUser2", "us", 1500, 15, 3, 0));

        var mockAccountDao = mock(UserDao.class);
        when(mockAccountDao.getLeaderboard(1, 25)).thenReturn(leaderboard);
        state.setUserDao(mockAccountDao);

        var mockTemplate = mock(Template.class);
        var templates = new Templates();
        templates.setLeaderboardTemplate(mockTemplate);
        state.setTemplates(templates);

        var mockRouter = new MockRouter(new PageRouter(state));

        // when
        mockRouter.get("/leaderboard");

        // then
        verify(mockAccountDao, times(1)).getLeaderboard(1, 25);
    }

    @Test
    public void testGetHistory() {
        // given
        var state = new State();

        var mockHistoryDao = mock(HistoryDao.class);
        when(mockHistoryDao.getHistories("1", "2", "3")).thenReturn(List.of());
        state.setHistoryDao(mockHistoryDao);

        var mockRouter = new MockRouter(new PageRouter(state));

        // when
        var result = mockRouter.get("/games/history", new MockContext().setQueryString("whiteId=1&blackId=2&cursor=3"));

        // then
        verify(mockHistoryDao, times(1)).getHistories("1", "2", "3");
        Assertions.assertEquals(List.of(), result.value());
    }

    @Test
    public void testGetManyGames() {
        // given
        var state = new State();
        var scanResult = new RemoteDict.GetGameKeysResult(22d, List.of());

        var mockgameService = mock(GameService.class);
        when(mockgameService.getManyKeys(12d)).thenReturn(scanResult);
        state.setGameService(mockgameService);

        var mockRouter = new MockRouter(new PageRouter(state));

        // when
        var result = mockRouter.get("/games/current", new MockContext().setQueryString("cursor=12"));

        // then
        verify(mockgameService, times(1)).getManyKeys(12d);
        Assertions.assertEquals(scanResult, result.value());
    }
}
