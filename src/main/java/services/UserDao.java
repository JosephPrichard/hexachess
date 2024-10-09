package services;

import at.favre.lib.crypto.bcrypt.BCrypt;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import models.Player;
import models.StatsEntity;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import utils.Config;

import javax.sql.DataSource;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class UserDao {

    private final QueryRunner runner;
    private final ResultSetHandler<AccountCreds> credsMapper = new BeanHandler<>(AccountCreds.class);
    private final ResultSetHandler<StatsEntity> statsMapper = new BeanHandler<>(StatsEntity.class);
    private final ResultSetHandler<List<StatsEntity>> statsListMapper = new BeanListHandler<>(StatsEntity.class);

    public UserDao(DataSource ds) {
        runner = new QueryRunner(ds);
    }

    public void createExtensions() throws SQLException {
        var sql = "CREATE EXTENSION pg_trgm";
        runner.execute(sql);
    }

    public void createTable() throws SQLException {
        var sql = """
            CREATE TABLE users (
                id VARCHAR NOT NULL,
                username VARCHAR NOT NULL,
                country VARCHAR,
                elo NUMERIC NOT NULL,
                wins INTEGER NOT NULL,
                losses INTEGER NOT NULL,
                password VARCHAR NOT NULL,
                salt VARCHAR NOT NULL,
                PRIMARY KEY (id));
                
            CREATE INDEX idxTrgmUsername ON users USING GIST (username gist_trgm_ops);
            CREATE INDEX idxUsername ON users(username);
            CREATE INDEX idxElo ON users(elo);""";
        runner.execute(sql);
    }

    public void defineProcedures() throws SQLException {
        var sql = """
            CREATE FUNCTION probabilityWins(IN elo1 NUMERIC, IN elo2 NUMERIC)
                RETURNS NUMERIC
            LANGUAGE plpgsql
            AS $$
            BEGIN
                RETURN 1.0 / (1.0 + POWER(10, (elo1 - elo2) / 400.0));
            END $$;

            CREATE PROCEDURE updateStatsUsingResult(
                IN winId VARCHAR, IN loseId VARCHAR,
                OUT winEloDiff NUMERIC, OUT loseEloDiff NUMERIC)
            LANGUAGE plpgsql
            AS $$
            DECLARE
                winElo NUMERIC;
                loseElo NUMERIC;
            BEGIN
                SELECT elo INTO winElo FROM users WHERE id = winId;
                SELECT elo INTO loseElo FROM users WHERE id = loseId;
                
                winEloDiff = 30 * (1 - probabilityWins(loseElo, winElo));
                loseEloDiff = (30 * probabilityWins(winElo, loseElo)) * -1;
               
                UPDATE users
                SET elo = winElo + winEloDiff, wins = wins + 1
                WHERE id = winId;
                
                UPDATE users
                SET elo = loseElo + loseEloDiff, losses = losses + 1
                WHERE id = loseId;
            END $$;""";
        runner.execute(sql);
    }

    @Data
    @AllArgsConstructor
    public static class UserInst {
        private String newId;
        private String username;
        private String password;
        private String country;
        private float elo;
        private int wins;
        private int losses;
    }

    public UserInst insert(String username, String password) throws SQLException, NoSuchAlgorithmException {
        var inst = new UserInst(UUID.randomUUID().toString(), username, password, "USA", 1000f, 0, 0);
        insert(inst);
        return inst;
    }

    public static String generateSalt() throws NoSuchAlgorithmException {
        var salt = new byte[16];
        SecureRandom.getInstanceStrong().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public void insert(UserInst inst) throws SQLException, NoSuchAlgorithmException {
        var salt = generateSalt();
        var saltedPassword = inst.getPassword() + salt;
        var hashedPassword = BCrypt.withDefaults().hashToString(12, saltedPassword.toCharArray());

        var sql = "BEGIN; INSERT INTO users (id, username, country, elo, wins, losses, password, salt) VALUES (?, ?, ?, ?, ?, ?, ?, ?); END;";
        runner.execute(sql, inst.getNewId(), inst.getUsername(),
            inst.getCountry(), inst.getElo(), inst.getWins(), inst.getLosses(), hashedPassword, salt);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountCreds {
        private String id;
        private String username;
        private String password;
        private String salt;
    }

    public Player verify(String username, String inputPassword) throws SQLException {
        var sql = "SELECT id, username, password, salt FROM users WHERE username = ?";
        var accountCreds = runner.query(sql, credsMapper, username);

        if (accountCreds == null) {
            return null;
        }
        var saltedPassword = inputPassword + accountCreds.getSalt();
        var result = BCrypt.verifyer().verify(saltedPassword.toCharArray(), accountCreds.getPassword());
        return result.verified ? new Player(accountCreds.getId(), accountCreds.getUsername()) : null;
    }

    public void updateAccount(String id, String username, String country) throws SQLException {
        var sql = "BEGIN; UPDATE users SET username = ?, country = ? WHERE id = ?; END";
        runner.execute(sql, username, country, id);
    }

    @Data
    @AllArgsConstructor
    public static class EloChangeSet {
        double winElo;
        double loseElo;

        public void roundElo() {
            winElo = Math.round(winElo);
            loseElo = Math.round(loseElo);
        }
    }

    public EloChangeSet updateStatsUsingResult(String winId, String loseId) throws SQLException {
        var sql = "CALL updateStatsUsingResult(?, ?, ?, ?)";

        Connection conn = null;
        CallableStatement stmt = null;
        try {
            conn = runner.getDataSource().getConnection();

            stmt = conn.prepareCall(sql);
            stmt.setString(1, winId);
            stmt.setString(2, loseId);
            stmt.registerOutParameter(3, Types.NUMERIC);
            stmt.registerOutParameter(4, Types.NUMERIC);

            stmt.execute();

            double winElo = stmt.getBigDecimal(3).doubleValue();
            double loseElo = stmt.getBigDecimal(4).doubleValue();

            return new EloChangeSet(winElo, loseElo);
        } finally {
            DbUtils.close(conn);
            DbUtils.close(stmt);
        }
    }

    public StatsEntity getStats(String id) throws SQLException {
        var sql = """
            SELECT id, username, country, elo, wins, losses
            FROM users
            WHERE id = ?""";
        return runner.query(sql, statsMapper, id);
    }

    public List<StatsEntity> getLeaderboard(int page, int perPage) throws SQLException {
        var sql = """
            SELECT id, username, country, elo, wins, losses
            FROM users
            ORDER BY elo DESC LIMIT ? OFFSET ?
            """;

        page = Math.max(page, 1);
        var offset = (page - 1) * perPage;
        return runner.query(sql, statsListMapper, perPage, offset);
    }

    public List<StatsEntity> searchUsers(String name, int page, int perPage) throws SQLException {
        var sql = """
            SELECT id, username, country, elo, wins, losses, (username <-> ?) as rank
            FROM users
            WHERE username % ?
            ORDER BY rank DESC LIMIT ? OFFSET ?""";

        page = Math.max(page, 1);
        var offset = (page - 1) * perPage;
        var results = runner.query(sql, statsListMapper, name, name, perPage, offset);

        for (int i = 0; i < results.size(); i++) {
            results.get(i).setRank(i + 1);
        }
        return results;
    }

    public static void main(String[] args) throws Exception {
        var envMap = Config.readEnvConfig();
        var ds = Config.createDataSource(envMap);

        var userDao = new UserDao(ds);
//        userDao.createExtensions();
//        userDao.createTable();
//        userDao.defineProcedures();

        userDao.insert(new UserInst("id1", "user1", "password1", "us", 1000f, 0, 0));
        userDao.insert(new UserInst("id2", "user2", "password2", "us", 1005f, 1, 0));
        userDao.insert(new UserInst("id3", "user3", "password3", "us", 900f, 1, 8));
        userDao.insert(new UserInst("id4", "user4", "password4", "us", 2000f, 50, 20));
        userDao.insert(new UserInst("id5", "user5", "password5", "us", 1500f, 40, 35));

        ds.close();
    }
}