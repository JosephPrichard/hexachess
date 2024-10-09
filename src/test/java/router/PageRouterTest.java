package router;

import io.jooby.test.MockContext;
import io.jooby.test.MockRouter;
import models.StatsEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import services.*;
import web.PageRouter;
import web.State;

import java.sql.SQLException;
import java.util.List;

import static org.mockito.Mockito.*;

public class PageRouterTest {

    @Test
    public void testGetStats() throws SQLException {
        // given
        var state = new State();
        var accountId = "1";
        var stats = new StatsEntity("1", "testUser", "us", 1050, 5, 3, 0);

        var mockAccountDao = mock(UserDao.class);
        when(mockAccountDao.getStats(accountId)).thenReturn(stats);
        state.setUserDao(mockAccountDao);

        var mockRouter = new MockRouter(new PageRouter(state));

        // when
        var result = mockRouter.get("/players/" + accountId + "/stats");

        // then
        verify(mockAccountDao, times(1)).getStats(accountId);
        Assertions.assertEquals(stats, result.value());
    }

    @Test
    public void testGetLeaderboard() throws SQLException {
        // given
        var state = new State();
        var leaderboard = List.of(
            new StatsEntity("1", "testUser1", "us", 1000, 3, 3, 0),
            new StatsEntity("2", "testUser2", "us", 1500, 15, 3, 0));

        var mockAccountDao = mock(UserDao.class);
        when(mockAccountDao.getLeaderboard(1, 20)).thenReturn(leaderboard);  // No cursor
        state.setUserDao(mockAccountDao);

        var mockRouter = new MockRouter(new PageRouter(state));

        // when
        var result = mockRouter.get("/leaderboard");

        // then
        verify(mockAccountDao, times(1)).getLeaderboard(1, 20);
        Assertions.assertEquals(leaderboard, result.value());
    }

    @Test
    public void testGetPlayerHistory() throws SQLException {
        // given
        var state = new State();

        var mockHistoryDao = mock(HistoryDao.class);
        when(mockHistoryDao.getHistories("1", "2")).thenReturn(List.of());
        state.setHistoryDao(mockHistoryDao);

        var mockRouter = new MockRouter(new PageRouter(state));

        // when
        var result = mockRouter.get("/players/1/history", new MockContext().setQueryString("cursor=2"));

        // then
        verify(mockHistoryDao, times(1)).getHistories("1", "2");
        Assertions.assertEquals(List.of(), result.value());
    }

    @Test
    public void testGetHistory() throws SQLException {
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
