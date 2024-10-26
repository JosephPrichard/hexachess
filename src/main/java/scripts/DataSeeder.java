package scripts;

import lombok.AllArgsConstructor;
import models.GameHist;
import org.apache.commons.dbutils.QueryRunner;
import services.HistoryDao;
import services.RemoteDict;
import services.UserDao;
import utils.Config;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static utils.Globals.EXECUTOR;
import static utils.Log.LOGGER;

@AllArgsConstructor
public class DataSeeder {

    private final DataSource ds;
    private final RemoteDict remoteDict;

    private static final List<UserDao.UserInst> USER_INSTS = List.of(
        new UserDao.UserInst("id1", "user1", "password1", "us", 1000f, 0, 0),
        new UserDao.UserInst("id2", "user2", "password2", "us", 1005f, 1, 0),
        new UserDao.UserInst("id3", "user3", "password3", "us", 900f, 1, 8),
        new UserDao.UserInst("id4", "user4", "password4", "us", 2000f, 50, 20),
        new UserDao.UserInst("id5", "user5", "password5", "us", 1500f, 40, 35),
        new UserDao.UserInst("id6", "john_doe", "password6", "eu", 1700f, 60, 10),
        new UserDao.UserInst("id7", "jane_smith", "password7", "dk", 1100f, 5, 2),
        new UserDao.UserInst("id8", "alice_wonder", "password8", "au", 1200f, 3, 5),
        new UserDao.UserInst("id9", "bob_builder", "password9", "us", 1800f, 30, 25),
        new UserDao.UserInst("id10", "charlie_brown", "password10", "eu", 1900f, 15, 12),
        new UserDao.UserInst("id11", "david_king", "password11", "dk", 1050f, 2, 1),
        new UserDao.UserInst("id12", "emma_green", "password12", "au", 950f, 20, 18),
        new UserDao.UserInst("id13", "frank_white", "password13", "us", 2100f, 12, 5),
        new UserDao.UserInst("id14", "george_clark", "password14", "eu", 1650f, 9, 4),
        new UserDao.UserInst("id15", "harry_potter", "password15", "dk", 2500f, 55, 30),
        new UserDao.UserInst("id16", "isla_fisher", "password16", "au", 1450f, 7, 6),
        new UserDao.UserInst("id17", "jack_sparrow", "password17", "us", 1300f, 1, 0),
        new UserDao.UserInst("id18", "kate_bishop", "password18", "eu", 1150f, 10, 8),
        new UserDao.UserInst("id19", "luke_skywalker", "password19", "dk", 980f, 4, 2),
        new UserDao.UserInst("id20", "maria_shar", "password20", "au", 1325f, 22, 15),
        new UserDao.UserInst("id21", "nancy_drew", "password21", "us", 1880f, 35, 25),
        new UserDao.UserInst("id22", "oliver_twine", "password22", "eu", 1720f, 18, 14),
        new UserDao.UserInst("id23", "peter_pan", "password23", "dk", 2025f, 40, 28),
        new UserDao.UserInst("id24", "quinn_bryant", "password24", "au", 1455f, 5, 4),
        new UserDao.UserInst("id25", "rachel_green", "password25", "us", 2100f, 65, 55),
        new UserDao.UserInst("id26", "samuel_jackson", "password26", "eu", 1375f, 12, 9),
        new UserDao.UserInst("id27", "tony_stark", "password27", "dk", 1650f, 25, 18),
        new UserDao.UserInst("id28", "ursula_stein", "password28", "au", 980f, 8, 7),
        new UserDao.UserInst("id29", "victor_hugo", "password29", "us", 1220f, 20, 15),
        new UserDao.UserInst("id30", "walter_white", "password30", "eu", 1685f, 45, 40),
        new UserDao.UserInst("id31", "xander_harris", "password31", "dk", 1125f, 3, 1),
        new UserDao.UserInst("id32", "yara_grey", "password32", "au", 1590f, 17, 12),
        new UserDao.UserInst("id33", "zeus_king", "password33", "us", 1845f, 30, 22),
        new UserDao.UserInst("id34", "amelia_clark", "password34", "eu", 1410f, 9, 7),
        new UserDao.UserInst("id35", "brad_pitt", "password35", "dk", 2200f, 70, 50),
        new UserDao.UserInst("id36", "claire_redfield", "password36", "au", 1800f, 32, 28),
        new UserDao.UserInst("id37", "don_drake", "password37", "us", 1950f, 18, 12),
        new UserDao.UserInst("id38", "eva_green", "password38", "eu", 2000f, 27, 21),
        new UserDao.UserInst("id39", "fiona_apple", "password39", "dk", 1700f, 14, 9),
        new UserDao.UserInst("id40", "gregory_house", "password40", "au", 1540f, 20, 15),
        new UserDao.UserInst("id41", "hank_sch", "password41", "us", 1880f, 33, 24),
        new UserDao.UserInst("id42", "irene_adler", "password42", "eu", 1300f, 11, 6),
        new UserDao.UserInst("id43", "john_snow", "password43", "dk", 1990f, 36, 28),
        new UserDao.UserInst("id44", "karen_smith", "password44", "au", 1500f, 12, 8),
        new UserDao.UserInst("id45", "leo_messi", "password45", "us", 1750f, 25, 18),
        new UserDao.UserInst("id46", "mike_tyson", "password46", "eu", 1420f, 8, 5));

    private static final List<HistoryDao.HistoryInst> HISTORY_INSTS = List.of(
        new HistoryDao.HistoryInst("id37", "id5", 1, 15d, -15d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id2", "id27", 0, 10d, -10d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id4", "id19", 2, 0d, 0d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id12", "id32", 1, 20d, -20d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id29", "id8", 0, 30d, -30d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id14", "id23", 2, 0d, 0d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id30", "id10", 1, 18d, -18d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id7", "id33", 0, 23d, -23d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id21", "id25", 2, 0d, 0d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id9", "id11", 1, 16d, -16d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id1", "id18", 0, 28d, -28d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id24", "id6", 2, 0d, 0d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id15", "id20", 1, 30d, -30d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id35", "id31", 0, 25d, -25d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id26", "id3", 2, 0d, 0d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id28", "id17", 1, 11d, -11d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id13", "id16", 0, 22d, -22d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id3", "id34", 2, 0d, 0d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id11", "id22", 1, 13d, -13d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id34", "id38", 0, 27d, -27d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id36", "id1", 2, 0d, 0d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id31", "id7", 1, 19d, -19d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id39", "id10", 0, 22d, -22d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id22", "id4", 2, 0d, 0d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id20", "id15", 1, 27d, -27d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id8", "id14", 0, 18d, -18d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id18", "id9", 2, 0d, 0d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id5", "id13", 1, 24d, -24d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id16", "id30", 0, 25d, -25d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id33", "id19", 2, 0d, 0d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id24", "id7", 1, 28d, -28d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id6", "id21", 0, 22d, -22d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id8", "id2", 1, 19d, -19d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id1", "id2", 1, 15d, -15d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id2", "id1", 0, 10d, -10d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id1", "id2", 2, 0d, 0d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id2", "id1", 1, 20d, -20d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id1", "id2", 0, 30d, -30d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id2", "id1", 2, 0d, 0d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id1", "id2", 1, 18d, -18d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id2", "id1", 0, 23d, -23d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id1", "id2", 2, 0d, 0d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id2", "id1", 1, 16d, -16d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id1", "id2", 0, 28d, -28d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id2", "id1", 2, 0d, 0d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id1", "id2", 1, 30d, -30d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id2", "id1", 0, 25d, -25d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id1", "id2", 2, 0d, 0d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id2", "id1", 1, 11d, -11d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id1", "id2", 0, 22d, -22d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id2", "id1", 2, 0d, 0d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id1", "id2", 1, 13d, -13d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id2", "id1", 0, 27d, -27d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id1", "id2", 1, 24d, -24d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id2", "id1", 0, 19d, -19d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id1", "id2", 2, 0d, 0d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id2", "id1", 1, 26d, -26d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id1", "id2", 0, 21d, -21d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id2", "id1", 2, 0d, 0d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id1", "id2", 1, 28d, -28d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id2", "id1", 0, 15d, -15d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id1", "id2", 2, 0d, 0d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id2", "id1", 1, 12d, -12d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id1", "id2", 0, 18d, -18d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id2", "id1", 2, 0d, 0d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id1", "id2", 1, 23d, -23d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id2", "id1", 0, 17d, -17d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id1", "id2", 2, 0d, 0d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id2", "id1", 1, 27d, -27d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id1", "id2", 0, 20d, -20d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id2", "id1", 2, 0d, 0d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id1", "id2", 1, 14d, -14d, GameHist.randomAsJson()),
        new HistoryDao.HistoryInst("id2", "id1", 0, 30d, -30d, GameHist.randomAsJson()));

    private void seedUsersTable(List<UserDao.UserInst> insts) {
        var userDao = new UserDao(ds);
        var futures = insts.stream()
            .map((inst) -> EXECUTOR.submit(() -> userDao.insert(inst)))
            .toList();
        futures.forEach((f) -> {
            try {
                f.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void seedHistTable(List<HistoryDao.HistoryInst> insts) {
        var userDao = new HistoryDao(ds);
        var futures = insts.stream()
            .map((inst) -> EXECUTOR.submit(() -> userDao.insert(inst)))
            .toList();
        futures.forEach((f) -> {
            try {
                f.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void seedUsersDict() {
        var userDao = new UserDao(ds);
        var allUsers = userDao.getAll();
        var changeSets = allUsers.stream()
            .map(entity -> new RemoteDict.EloChangeSet(entity.getId(), entity.getElo()))
            .toArray(RemoteDict.EloChangeSet[]::new);
        remoteDict.incrLeaderboardUser(changeSets);
    }

    public static void main(String[] args) throws Exception {
        var envMap = Config.readEnvConfig();

        var startTime = System.currentTimeMillis();

        var ds = Config.createDataSource(envMap);
        new QueryRunner(ds).execute("BEGIN; DROP SCHEMA public CASCADE; CREATE SCHEMA public; END;");

        Config.createSchema(ds);

//        var redisHost = envMap.get("REDIS_HOST");
//        var redisPort = Integer.parseInt(envMap.get("REDIS_PORT"));
//        var jedis = new JedisPooled(redisHost, redisPort);
//        var remoteDict = new RemoteDict(jedis, new ObjectMapper());

        var seeder = new DataSeeder(ds, null);
//        var seeder = new DataSeeder(ds, remoteDict);
        seeder.seedUsersTable(USER_INSTS);
//        seeder.seedUsersDict();
        seeder.seedHistTable(HISTORY_INSTS);

        var endTime = System.currentTimeMillis() - startTime;
        LOGGER.info(String.format("Took %s ms to execute seeding script", endTime));

//        jedis.close();
        ds.close();
    }
}
