package router;

import io.jooby.Cookie;
import io.jooby.Formdata;
import io.jooby.test.MockContext;
import io.jooby.test.MockResponse;
import io.jooby.test.MockRouter;
import models.Duel;
import models.Stats;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import services.*;
import web.RestRouter;
import web.ServerState;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;

import static org.mockito.Mockito.*;

public class TestRestRouter {

    @Test
    public void testPostSignup() throws SQLException, NoSuchAlgorithmException {
        // given
        var accountInst = new AccountDao.AccountInst("1", "testUser", "testPass", 1000, 3, 3);
        var player = new Duel.Player("1", "testUser");
        var cookie = new Cookie("sessionToken");

        var mockAccountDao = mock(AccountDao.class);
        var mockSessionService = mock(SessionService.class);

        when(mockAccountDao.insert("testUser", "testPass")).thenReturn(accountInst);
        when(mockSessionService.createId()).thenReturn("sessionToken");
        when(mockSessionService.createCookie("sessionToken")).thenReturn(cookie);

        var state = new ServerState();
        state.setAccountDao(mockAccountDao);
        state.setSessionService(mockSessionService);

        var mockDictService = mock(DictService.class);
        state.setDictService(mockDictService);

        var mockRouter = new MockRouter(new RestRouter(state));

        var mockContext = new MockContext();
        var mockForm = Formdata.create(mockContext);
        mockForm.put("username", "testUser");
        mockForm.put("password", "testPass");
        mockContext.setForm(mockForm);

        // when
        var response = new Object() { MockResponse value; };
        var result = mockRouter.post("/forms/signup", mockContext, resp -> response.value = resp);
        var actualCookie = response.value.getHeaders().get("Set-Cookie");

        // then
        verify(mockAccountDao, times(1)).insert("testUser", "testPass");
        verify(mockDictService, times(1)).setSession(anyString(), eq(player), anyLong());
        verify(mockSessionService, times(1)).createCookie("sessionToken");

        Assertions.assertEquals("sessionToken", result.value());
        Assertions.assertEquals(actualCookie, cookie.toString());
    }

    @Test
    public void testPostLogin() throws SQLException {
        // given
        var player = new Duel.Player("1", "testUser");
        var cookie = new Cookie("sessionToken");

        var mockAccountDao = mock(AccountDao.class);
        var mockSessionService = mock(SessionService.class);

        when(mockAccountDao.verify("testUser", "testPass")).thenReturn("1");
        when(mockSessionService.createId()).thenReturn("sessionToken");
        when(mockSessionService.createCookie("sessionToken")).thenReturn(cookie);

        var state = new ServerState();
        state.setAccountDao(mockAccountDao);
        state.setSessionService(mockSessionService);

        var mockDictService = mock(DictService.class);
        state.setDictService(mockDictService);

        var mockRouter = new MockRouter(new RestRouter(state));

        var mockContext = new MockContext();
        var mockForm = Formdata.create(mockContext);
        mockForm.put("username", "testUser");
        mockForm.put("password", "testPass");
        mockContext.setForm(mockForm);

        // when
        var response = new Object() { MockResponse value; };
        var result = mockRouter.post("/forms/login", mockContext, resp -> response.value = resp);
        var actualCookie = response.value.getHeaders().get("Set-Cookie");

        // then
        verify(mockAccountDao, times(1)).verify("testUser", "testPass");
        verify(mockDictService, times(1)).setSession(anyString(), eq(player), anyLong());
        verify(mockSessionService, times(1)).createCookie("sessionToken");

        Assertions.assertEquals("sessionToken", result.value());
        Assertions.assertEquals(actualCookie, cookie.toString());
    }

    @Test
    public void testGetStats() throws SQLException {
        // given
        var state = new ServerState();
        var accountId = "1";
        var stats = new Stats("1", "testUser", 1050, 5, 3);

        var mockAccountDao = mock(AccountDao.class);
        when(mockAccountDao.getStats(accountId)).thenReturn(stats);
        state.setAccountDao(mockAccountDao);

        var mockRouter = new MockRouter(new RestRouter(state));

        // when
        var result = mockRouter.get("/players/" + accountId + "/stats");

        // then
        verify(mockAccountDao, times(1)).getStats(accountId);
        Assertions.assertEquals(stats, result.value());
    }

    @Test
    public void testGetLeaderboard() throws SQLException {
        // given
        var state = new ServerState();
        var leaderboard = List.of(
            new Stats("1", "testUser1", 1000, 3, 3),
            new Stats("2", "testUser2", 1500, 15, 3));

        var mockAccountDao = mock(AccountDao.class);
        when(mockAccountDao.getLeaderboard(null, 20)).thenReturn(leaderboard);  // No cursor
        state.setAccountDao(mockAccountDao);

        var mockRouter = new MockRouter(new RestRouter(state));

        // when
        var result = mockRouter.get("/leaderboard");

        // then
        verify(mockAccountDao, times(1)).getLeaderboard(null, 20);
        Assertions.assertEquals(leaderboard, result.value());
    }

    @Test
    public void testGetPlayerHistory() throws SQLException {
        // given
        var state = new ServerState();

        var mockHistoryDao = mock(HistoryDao.class);
        when(mockHistoryDao.getHistories("1", "2")).thenReturn(List.of());
        state.setHistoryDao(mockHistoryDao);

        var mockRouter = new MockRouter(new RestRouter(state));

        // when
        var result = mockRouter.get("/player/1/history", new MockContext().setQueryString("cursor=2"));

        // then
        verify(mockHistoryDao, times(1)).getHistories("1", "2");
        Assertions.assertEquals(List.of(), result.value());
    }

    @Test
    public void testGetHistory() throws SQLException {
        // given
        var state = new ServerState();

        var mockHistoryDao = mock(HistoryDao.class);
        when(mockHistoryDao.getHistories("1", "2", "3")).thenReturn(List.of());
        state.setHistoryDao(mockHistoryDao);

        var mockRouter = new MockRouter(new RestRouter(state));

        // when
        var result = mockRouter.get("/games/history", new MockContext().setQueryString("whiteId=1&blackId=2&cursor=3"));

        // then
        verify(mockHistoryDao, times(1)).getHistories("1", "2", "3");
        Assertions.assertEquals(List.of(), result.value());
    }

    @Test
    public void testPostCreateDuel() {
        // given
        var state = new ServerState();

        var mockDuelService = mock(DuelService.class);
        when(mockDuelService.create(null)).thenReturn("test-id");
        state.setDuelService(mockDuelService);

        var mockRouter = new MockRouter(new RestRouter(state));

        // when
        var result = mockRouter.post("/forms/games/create");

        // then
        verify(mockDuelService, times(1)).create(null);
        Assertions.assertEquals("test-id", result.value());
    }

    @Test
    public void testGetManyDuels() {
        // given
        var state = new ServerState();
        var scanResult = new DictService.GetDuelKeysResult(22d, List.of());

        var mockDuelService = mock(DuelService.class);
        when(mockDuelService.getManyKeys(12d)).thenReturn(scanResult);
        state.setDuelService(mockDuelService);

        var mockRouter = new MockRouter(new RestRouter(state));

        // when
        var result = mockRouter.get("/games/current", new MockContext().setQueryString("cursor=12"));

        // then
        verify(mockDuelService, times(1)).getManyKeys(12d);
        Assertions.assertEquals(scanResult, result.value());
    }
}
