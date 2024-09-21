package services;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import models.Stats;
import org.apache.commons.dbutils.QueryRunner;
import org.junit.jupiter.api.*;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestAccountDao {

    private HikariDataSource ds;
    private AccountDao accountDao;

    @BeforeAll
    public void beforeAll() {
        var config = new HikariConfig();

        config.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(10);

        ds = new HikariDataSource(config);
        accountDao = new AccountDao(ds);
    }

    @BeforeEach
    public void beforeEach() throws SQLException {
        var runner = new QueryRunner(ds);
        runner.execute("DROP ALL OBJECTS DELETE FILES");
        accountDao.createTable();
        accountDao.createIndices();
    }

    public static void createTestData(AccountDao accountDao) {
        try {
            accountDao.insert(new AccountDao.AccountInst("id1", "user1", "password1", 1000f, 0, 0));
            accountDao.insert(new AccountDao.AccountInst("id2", "user2", "password2", 1005f, 1, 0));
            accountDao.insert(new AccountDao.AccountInst("id3", "user3", "password3", 900f, 1, 8));
            accountDao.insert(new AccountDao.AccountInst("id4", "user4", "password4", 2000f, 50, 20));
            accountDao.insert(new AccountDao.AccountInst("id5", "user5", "password5", 1500f, 40, 35));
        } catch (NoSuchAlgorithmException | SQLException ex) {
            throw new RuntimeException(ex);
        }
    }


    @Test
    public void testInsertThenVerify() throws SQLException, NoSuchAlgorithmException {
        // given
        var accountDao = new AccountDao(ds);

        // when
        var inst1 = accountDao.insert("user1", "password1");
        var inst2 = accountDao.insert("user2", "password2");
        var inst3 = accountDao.insert("user3", "password3");

        var id1 = accountDao.verify("user1", "password1");
        var id2 = accountDao.verify("user2", "password2");
        var id3 = accountDao.verify("user2", "wrong-password");
        var id4 = accountDao.verify("user3", "password3");
        var id5 = accountDao.verify("user1", "password3");

        // then
        Assertions.assertEquals(id1, inst1.getNewId());
        Assertions.assertEquals(id2, inst2.getNewId());
        Assertions.assertNull(id3);
        Assertions.assertEquals(id4, inst3.getNewId());
        Assertions.assertNull(id5);
    }

    @Test
    public void testUpdateThenGet() throws SQLException {
        // given
        var accountDao = new AccountDao(ds);
        createTestData(accountDao);

        // when
        accountDao.updateStats(new AccountDao.StatsUpdt("id1", 20f, 1, 0));
        accountDao.updateStats(new AccountDao.StatsUpdt("id2", -20f, 0, 1));
        accountDao.updateStats(new AccountDao.StatsUpdt("id3", 0f, 0, 0));

        var actualStats1 = accountDao.getStats("id1");
        var actualStats2 = accountDao.getStats("id2");
        var actualStats3 = accountDao.getStats("id3");

        // then
        var expectedStats1 = new Stats("id1", "user1", 1020f, 1, 0);
        var expectedStats2 = new Stats("id2", "user2", 985f, 1, 1);
        var expectedStats3 = new Stats("id3", "user3", 900f, 1, 8);

        Assertions.assertEquals(expectedStats1, actualStats1);
        Assertions.assertEquals(expectedStats2, actualStats2);
        Assertions.assertEquals(expectedStats3, actualStats3);
    }

    @Test
    public void testGetLeaderboard() throws SQLException {
        // given
        var accountDao = new AccountDao(ds);
        createTestData(accountDao);

        // when
        var actualStatsList1 = accountDao.getLeaderboard(null, 5);

        // then
        var expectedStatsList1 = List.of(
            new Stats("id4", "user4", 2000f, 50, 20),
            new Stats("id5", "user5", 1500f, 40, 35),
            new Stats("id2", "user2", 1005f, 1, 0),
            new Stats("id1", "user1", 1000f, 0, 0),
            new Stats("id3", "user3", 900f, 1, 8));
        Assertions.assertEquals(expectedStatsList1, actualStatsList1);

        // when
        var actualStatsList2 = accountDao.getLeaderboard(1005f, 5);

        // then
        var expectedStatsList2 = List.of(
            new Stats("id1", "user1", 1000f, 0, 0),
            new Stats("id3", "user3", 900f, 1, 8));
        Assertions.assertEquals(expectedStatsList2, actualStatsList2);
    }
}
