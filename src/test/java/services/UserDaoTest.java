package services;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import models.StatsEntity;
import org.apache.commons.dbutils.QueryRunner;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserDaoTest {

    public EmbeddedPostgres pg;
    private DataSource ds;
    private UserDao userDao;

    @BeforeAll
    public void beforeAll() throws IOException {
        pg = EmbeddedPostgres.builder().start();
        ds = pg.getPostgresDatabase();

        userDao = new UserDao(ds);
    }

    @BeforeEach
    public void beforeEach() throws SQLException {
        var runner = new QueryRunner(ds);
        runner.execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public;");
        userDao.createExtensions();
        userDao.createTable();
    }

    @AfterAll
    public void afterAll() throws IOException {
        pg.close();
    }

    public static void createTestData(UserDao userDao) {
        try {
            userDao.insert(new UserDao.UserInst("id1", "user1", "password1", "us", 1000f, 0, 0));
            userDao.insert(new UserDao.UserInst("id2", "user2", "password2", "us", 1005f, 1, 0));
            userDao.insert(new UserDao.UserInst("id3", "user3", "password3", "us", 900f, 1, 8));
            userDao.insert(new UserDao.UserInst("id4", "user4", "password4", "us", 2000f, 50, 20));
            userDao.insert(new UserDao.UserInst("id5", "user5", "password5", "us", 1500f, 40, 35));
        } catch (NoSuchAlgorithmException | SQLException ex) {
            throw new RuntimeException(ex);
        }
    }


    @Test
    public void testInsertThenVerify() throws SQLException, NoSuchAlgorithmException {
         // when
        var inst1 = userDao.insert("user1", "password1");
        var inst2 = userDao.insert("user2", "password2");
        var inst3 = userDao.insert("user3", "password3");

        var player1 = userDao.verify("user1", "password1");
        var player2 = userDao.verify("user2", "password2");
        var player3 = userDao.verify("user2", "wrong-password");
        var player4 = userDao.verify("user3", "password3");
        var player5 = userDao.verify("user1", "password3");

        // then
        Assertions.assertEquals(player1.getId(), inst1.getNewId());
        Assertions.assertEquals(player2.getId(), inst2.getNewId());
        Assertions.assertNull(player3);
        Assertions.assertEquals(player4.getId(), inst3.getNewId());
        Assertions.assertNull(player5);
    }

    @Test
    public void testUpdateAccounts() throws SQLException, NoSuchAlgorithmException {
        // when
        var inst1 = userDao.insert("user1", "password1");
        var inst2 = userDao.insert("user2", "password2");
        var inst3 = userDao.insert("user3", "password3");

        var player1 = userDao.verify("user1", "password1");
        var player2 = userDao.verify("user2", "password2");
        var player3 = userDao.verify("user2", "wrong-password");
        var player4 = userDao.verify("user3", "password3");
        var player5 = userDao.verify("user1", "password3");

        // then
        Assertions.assertEquals(player1.getId(), inst1.getNewId());
        Assertions.assertEquals(player2.getId(), inst2.getNewId());
        Assertions.assertNull(player3);
        Assertions.assertEquals(player4.getId(), inst3.getNewId());
        Assertions.assertNull(player5);
    }

    @Test
    public void testUpdateStatsUsingResult() throws SQLException {
        // given
        createTestData(userDao);
        userDao.defineProcedures();

        // when
        var changeSet = userDao.updateStatsUsingResult("id1", "id2");
        var actualStats1 = userDao.getStats("id1");
        var actualStats2 = userDao.getStats("id2");

        changeSet.roundElo();
        actualStats1.roundElo();
        actualStats2.roundElo();

        // then
        var expectedStats1 = new StatsEntity("id1", "user1", "us", 1015f, 1, 0);
        var expectedStats2 = new StatsEntity("id2", "user2", "us", 990f, 1, 1);

        Assertions.assertEquals(changeSet, new UserDao.EloChangeSet(15f, -15f));
        Assertions.assertEquals(expectedStats1, actualStats1);
        Assertions.assertEquals(expectedStats2, actualStats2);
    }

    @Test
    public void testUpdateAccount() throws SQLException {
        // given
        createTestData(userDao);

        // when
        userDao.updateAccount("id1", "user1-changed", "us");
        userDao.updateAccount("id2", "user2-changed", "eu");

        var actualStats1 = userDao.getStats("id1");
        var actualStats2 = userDao.getStats("id2");

        // then
        var expectedStats1 = new StatsEntity("id1", "user1-changed", "us", 1000f, 0, 0);
        var expectedStats2 = new StatsEntity("id2", "user2-changed", "eu", 1005f, 1, 0);

        Assertions.assertEquals(expectedStats1, actualStats1);
        Assertions.assertEquals(expectedStats2, actualStats2);
    }

    @Test
    public void testGetLeaderboard() throws SQLException {
        // given
        createTestData(userDao);

        // when
        var actualStatsList1 = userDao.getLeaderboard(1, 5);

        // then
        var expectedStatsList1 = List.of(
            new StatsEntity("id4", "user4", "us", 2000f, 50, 20),
            new StatsEntity("id5", "user5", "us", 1500f, 40, 35),
            new StatsEntity("id2", "user2", "us", 1005f, 1, 0),
            new StatsEntity("id1", "user1", "us", 1000f, 0, 0),
            new StatsEntity("id3", "user3", "us", 900f, 1, 8));
        Assertions.assertEquals(expectedStatsList1, actualStatsList1);
    }

    @Test
    public void testSearchUsers() throws SQLException, NoSuchAlgorithmException {
        // given
        createTestData(userDao);
        userDao.insert(new UserDao.UserInst("id6", "johnny", "password6", "us", 0f, 0, 0));
        userDao.insert(new UserDao.UserInst("id7", "john", "password7", "us", 0f, 0, 0));

        // when
        var actualStatsList1 = userDao.searchUsers("john", 1, 20);

        // then
        var expectedStatsList1 = List.of(
            new StatsEntity("id6", "johnny", "us", 0f, 0, 0, 1),
            new StatsEntity("id7", "john", "us", 0f, 0, 0, 2));
        Assertions.assertEquals(expectedStatsList1, actualStatsList1);
    }
}
