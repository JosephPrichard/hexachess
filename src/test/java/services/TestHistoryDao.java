package services;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import models.History;
import org.apache.commons.dbutils.QueryRunner;
import org.junit.jupiter.api.*;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestHistoryDao {

    private HikariDataSource ds;
    private AccountDao accountDao;
    private HistoryDao historyDao;

    @BeforeAll
    public void beforeAll() {
        var config = new HikariConfig();

        config.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(10);

        ds = new HikariDataSource(config);
        accountDao = new AccountDao(ds);
        historyDao = new HistoryDao(ds);
    }

    @BeforeEach
    public void beforeEach() throws SQLException {
        var runner = new QueryRunner(ds);
        runner.execute("DROP ALL OBJECTS DELETE FILES");
        accountDao.createTable();
        accountDao.createIndices();
        historyDao.createTable();
        historyDao.createIndices();
    }

    public static void createTestData(AccountDao accountDao) {
        try {
            accountDao.insert(new AccountDao.AccountInst("id1", "user1", "password1", 0f, 0, 0));
            accountDao.insert(new AccountDao.AccountInst("id2", "user2", "password2", 0f, 0, 0));
            accountDao.insert(new AccountDao.AccountInst("id3", "user3", "password3", 0f, 0, 0));
            accountDao.insert(new AccountDao.AccountInst("id4", "user4", "password4", 0f, 0, 0));
            accountDao.insert(new AccountDao.AccountInst("id5", "user5", "password5", 0f, 0, 0));
        } catch (NoSuchAlgorithmException | SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testInsertThenGet() throws SQLException {
        // given
        var accountDao = new AccountDao(ds);
        var historyDao = new HistoryDao(ds);
        createTestData(accountDao);

        // when
        historyDao.insert(new HistoryDao.HistoryInst("id1", "id2", History.WIN, "data1"));
        historyDao.insert(new HistoryDao.HistoryInst("id2", "id3", History.LOSS, "data2"));
        historyDao.insert(new HistoryDao.HistoryInst("id3", "id1", History.DRAW, "data3"));

        var actualHistory1 = historyDao.getHistory(1);
        var actualHistory2 = historyDao.getHistory(2);
        var actualHistory3 = historyDao.getHistory(3);

        // then
        var expectedHistory1 = new History(1, "id1", "id2", "user1", "user2", History.WIN, "data1", null);
        var expectedHistory2 = new History(2, "id2", "id3", "user2", "user3", History.LOSS, "data2", null);
        var expectedHistory3 = new History(3, "id3", "id1", "user3", "user1", History.DRAW, "data3", null);

        Assertions.assertEquals(expectedHistory1, actualHistory1);
        Assertions.assertEquals(expectedHistory2, actualHistory2);
        Assertions.assertEquals(expectedHistory3, actualHistory3);
    }

    @Test
    public void testGetHistoriesOneAccount() throws SQLException {
        // given
        var accountDao = new AccountDao(ds);
        var historyDao = new HistoryDao(ds);
        createTestData(accountDao);

        historyDao.insert(new HistoryDao.HistoryInst("id1", "id2", History.WIN, "data1"));
        historyDao.insert(new HistoryDao.HistoryInst("id2", "id3", History.LOSS, "data2"));
        historyDao.insert(new HistoryDao.HistoryInst("id3", "id1", History.DRAW, "data3"));

        // when
        var actualHistoryList = historyDao.getHistories("id1", null);

        // then
        var expectedHistoryList = List.of(
            new History(3, "id3", "id1", "user3", "user1", History.DRAW, "data3", null),
            new History(1, "id1", "id2", "user1", "user2", History.WIN, "data1", null));

        Assertions.assertEquals(expectedHistoryList, actualHistoryList);
    }

    @Test
    public void testGetHistories() throws SQLException {
        // given
        var accountDao = new AccountDao(ds);
        var historyDao = new HistoryDao(ds);
        createTestData(accountDao);

        historyDao.insert(new HistoryDao.HistoryInst("id1", "id2", History.WIN, "data1"));
        historyDao.insert(new HistoryDao.HistoryInst("id2", "id3", History.LOSS, "data2"));
        historyDao.insert(new HistoryDao.HistoryInst("id3", "id1", History.DRAW, "data3"));

        // when
        var actualHistoryList = historyDao.getHistories("id1", null, null);

        // then
        var expectedHistoryList = List.of(
            new History(1, "id1", "id2", "user1", "user2", History.WIN, "data1", null));

        Assertions.assertEquals(expectedHistoryList, actualHistoryList);
    }
}
