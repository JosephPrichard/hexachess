package router;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.jooby.Cookie;
import io.jooby.Formdata;
import io.jooby.test.MockContext;
import io.jooby.test.MockResponse;
import io.jooby.test.MockRouter;
import models.Player;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import services.GameService;
import services.UserDao;
import services.RemoteDict;
import web.SessionService;
import web.FormRouter;
import web.State;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class FormRouterTest {
    @Test
    public void testPostSignup() throws JsonProcessingException, UserDao.TakenUsernameException {
        // given
        var accountInst = new UserDao.UserInst("1", "testUser", "testPassword", "USA", 1000, 3, 3);
        var player = new Player("1", "testUser");
        var cookie = new Cookie("sessionToken");

        var mockUserDao = mock(UserDao.class);
        var mockSessionService = mock(SessionService.class);

        when(mockUserDao.insert("testUser", "testPassword")).thenReturn(accountInst);
        when(mockSessionService.createId()).thenReturn("sessionToken");
        when(mockSessionService.createCookie("sessionToken", player)).thenReturn(cookie);

        var state = new State();
        state.setUserDao(mockUserDao);
        state.setSessionService(mockSessionService);

        var mockDict = mock(RemoteDict.class);
        state.setRemoteDict(mockDict);

        var mockRouter = new MockRouter(new FormRouter(state));

        var mockContext = new MockContext();
        var mockForm = Formdata.create(mockContext);
        mockForm.put("username", "testUser");
        mockForm.put("password", "testPassword");
        mockForm.put("duplicate-password", "testPassword");
        mockContext.setForm(mockForm);

        // when
        AtomicReference<MockResponse> response = new AtomicReference<>();
        mockRouter.post("/forms/signup", mockContext, response::set);
        var actualCookie = response.get().getHeaders().get("Set-Cookie");

        // then
        verify(mockUserDao, times(1)).insert("testUser", "testPassword");
        verify(mockDict, times(1)).setSession(anyString(), eq(player), anyLong());
        verify(mockSessionService, times(1)).createCookie("sessionToken", player);

        Assertions.assertEquals(actualCookie, cookie.toString());
    }

    @Test
    public void testPostLogin() throws JsonProcessingException {
        // given
        var player = new Player("1", "testUser");
        var cookie = new Cookie("sessionToken");

        var mockUserDao = mock(UserDao.class);
        var mockSessionService = mock(SessionService.class);

        when(mockUserDao.verify("testUser", "testPass")).thenReturn(new Player("1", "username"));
        when(mockSessionService.createId()).thenReturn("sessionToken");
        when(mockSessionService.createCookie("sessionToken", player)).thenReturn(cookie);

        var state = new State();
        state.setUserDao(mockUserDao);
        state.setSessionService(mockSessionService);

        var mockDict = mock(RemoteDict.class);
        state.setRemoteDict(mockDict);

        var mockRouter = new MockRouter(new FormRouter(state));

        var mockContext = new MockContext();
        var mockForm = Formdata.create(mockContext);
        mockForm.put("username", "testUser");
        mockForm.put("password", "testPass");
        mockContext.setForm(mockForm);

        // when
        AtomicReference<MockResponse> response = new AtomicReference<>();
        mockRouter.post("/forms/login", mockContext, response::set);
        var actualCookie = response.get().getHeaders().get("Set-Cookie");

        // then
        verify(mockUserDao, times(1)).verify("testUser", "testPass");
        verify(mockDict, times(1)).setSession(anyString(), eq(player), anyLong());
        verify(mockSessionService, times(1)).createCookie("sessionToken", player);

        Assertions.assertEquals(actualCookie, cookie.toString());
    }

    @Test
    public void testPostCreateGame() {
        // given
        var state = new State();

        var mockgameService = mock(GameService.class);
        when(mockgameService.create(null)).thenReturn("test-id");
        state.setGameService(mockgameService);

        var mockRouter = new MockRouter(new FormRouter(state));

        // when
        var result = mockRouter.post("/forms/games/create");

        // then
        verify(mockgameService, times(1)).create(null);
        Assertions.assertEquals("test-id", result.value());
    }
}
