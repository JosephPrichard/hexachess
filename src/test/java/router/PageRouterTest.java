package router;

import com.github.jknack.handlebars.Template;
import io.jooby.test.MockRouter;
import models.UserEntity;
import org.junit.jupiter.api.Test;
import services.*;
import web.PageRouter;
import web.State;
import web.Templates;

import java.util.List;

import static org.mockito.Mockito.*;

public class PageRouterTest {

    @Test
    public void testGetPlayer() {
        // given
        var state = new State();
        var userId = "1";
        var user = new UserEntity("1", "testUser", "us", 1050, 5, 3, 0);

        var mockUserDao = mock(UserDao.class);
        when(mockUserDao.getByIdWithRank(userId)).thenReturn(user);
        state.setUserDao(mockUserDao);

        var mockHistoryDao = mock(HistoryDao.class);
        when(mockHistoryDao.getUserHistories(eq(userId), eq(null), anyInt())).thenReturn(List.of());
        state.setHistoryDao(mockHistoryDao);

        var mockTemplate = mock(Template.class);
        var templates = new Templates();
        templates.setProfileTemplate(mockTemplate);
        state.setTemplates(templates);

        var mockRouter = new MockRouter(new PageRouter(state));

        // when
        mockRouter.get("/players/" + userId);

        // then
        verify(mockUserDao, times(1)).getByIdWithRank(eq(userId));
        verify(mockHistoryDao, times(1)).getUserHistories(eq(userId), eq(null), anyInt());
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
}
