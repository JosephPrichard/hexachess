package services;

import at.favre.lib.crypto.bcrypt.BCrypt;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import models.Player;
import models.StatsEntity;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import utils.Config;

import javax.sql.DataSource;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
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

    public void createTable() throws SQLException {
        var sql = """
            CREATE EXTENSION pg_trgm;
            CREATE TABLE accounts (
                id VARCHAR NOT NULL,
                username VARCHAR NOT NULL,
                country VARCHAR,
                elo NUMERIC NOT NULL,
                wins INTEGER NOT NULL,
                losses INTEGER NOT NULL,
                password VARCHAR NOT NULL,
                salt VARCHAR NOT NULL,
                PRIMARY KEY (id));
            CREATE INDEX idxTrgmUsername ON accounts USING GIST (username gist_trgm_ops);
            CREATE INDEX idxUsername ON accounts(username);
            CREATE INDEX idxElo ON accounts(elo);
            """;
        runner.execute(sql);
    }

    public void defProcUpdateStats() throws SQLException {
        var sql = """
            CREATE FUNCTION probability(IN elo1 NUMERIC, IN elo2 NUMERIC)
            RETURNS NUMERIC
            LANGUAGE plpgsql
            AS $$
            BEGIN
                RETURN 1.0 / (1.0 + POWER(10, (elo1 - elo2) / 400.0));
            END $$;
            
            CREATE PROCEDURE update_stats(IN winnerId VARCHAR, IN loserId VARCHAR)
            LANGUAGE plpgsql
            AS $$
            DECLARE
             winnerElo NUMERIC;
             loserElo NUMERIC;
             probWinner NUMERIC;
             probLoser NUMERIC;
             eloK NUMERIC;
            BEGIN
                eloK = 30;
            
                SELECT elo INTO winnerElo
                FROM accounts WHERE id = winnerId;
            
                SELECT elo INTO loserElo
                FROM accounts WHERE id = loserId;
            
                probWinner = probability(loserElo, winnerElo);
                probLoser = probability(winnerElo, loserElo);
            
                UPDATE accounts
                SET elo = winnerElo + (eloK * (1 - probWinner)), wins = wins + 1
                WHERE id = winnerId;
            
                UPDATE accounts
                SET elo = loserElo - (eloK * probLoser), losses = losses + 1
                WHERE id = loserId;
            END $$;
            """;
        runner.execute(sql);
    }

    @Data
    @AllArgsConstructor
    public static class AccountInst {
        private String newId;
        private String username;
        private String password;
        private String country;
        private float elo;
        private int wins;
        private int losses;
    }

    public AccountInst insert(String username, String password) throws SQLException, NoSuchAlgorithmException {
        var inst = new AccountInst(UUID.randomUUID().toString(), username, password, "USA", 1000f, 0, 0);
        insert(inst);
        return inst;
    }

    public static String generateSalt() throws NoSuchAlgorithmException {
        var salt = new byte[16];
        SecureRandom.getInstanceStrong().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public void insert(AccountInst inst) throws SQLException, NoSuchAlgorithmException {
        var salt = generateSalt();
        var saltedPassword = inst.getPassword() + salt;
        var hashedPassword = BCrypt.withDefaults().hashToString(12, saltedPassword.toCharArray());

        var sql = "BEGIN; INSERT INTO accounts (id, username, country, elo, wins, losses, password, salt) VALUES (?, ?, ?, ?, ?, ?, ?, ?); END";
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
        var sql = "SELECT id, username, password, salt FROM accounts WHERE username = ?";
        var accountCreds = runner.query(sql, credsMapper, username);

        if (accountCreds == null) {
            return null;
        }
        var saltedPassword = inputPassword + accountCreds.getSalt();
        var result = BCrypt.verifyer().verify(saltedPassword.toCharArray(), accountCreds.getPassword());
        return result.verified ? new Player(accountCreds.getId(), accountCreds.getUsername()) : null;
    }

    public void updateAccount(String id, String username, String country) throws SQLException {
        var sql = "BEGIN; UPDATE accounts SET username = ?, country = ? WHERE id = ?; END";
        runner.execute(sql, username, country, id);
    }


    public void updateStats(String winnerId, String loserId) throws SQLException {
        var sql = "CALL update_stats(?, ?)";
        runner.execute(sql, winnerId, loserId);
    }

    public StatsEntity getStats(String id) throws SQLException {
        var sql = "SELECT id, username, country, elo, wins, losses FROM accounts WHERE id = ? ";
        return runner.query(sql, statsMapper, id);
    }

    public List<StatsEntity> getLeaderboard(Float eloCursor, int limit) throws SQLException {
        var sql = new StringBuilder("SELECT id, username, country, elo, wins, losses FROM accounts WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();

        if (eloCursor != null) {
            sql.append(" AND elo < ? ");
            params.add(eloCursor);
        }
        sql.append(" ORDER BY elo DESC LIMIT ?");
        params.add(limit);

        return runner.query(sql.toString(), statsListMapper, params.toArray());
    }

    public List<StatsEntity> searchPlayerStats(String name) throws SQLException {
        var sql = "SELECT id, username, country, elo, wins, losses, (username <-> ?) as rank FROM accounts WHERE username % ? ORDER BY rank";
        return runner.query(sql, statsListMapper, name, name);
    }

    public static void main(String[] args) throws Exception {
        var envMap = Config.readEnvConfig();
        var ds = Config.createDataSource(envMap);

        var userDao = new UserDao(ds);
        userDao.insert(new UserDao.AccountInst("id1", "user1", "password1", "us", 1000f, 0, 0));
        userDao.insert(new UserDao.AccountInst("id2", "user2", "password2", "us", 1005f, 1, 0));
        userDao.insert(new UserDao.AccountInst("id3", "user3", "password3", "us", 900f, 1, 8));
        userDao.insert(new UserDao.AccountInst("id4", "user4", "password4", "us", 2000f, 50, 20));
        userDao.insert(new UserDao.AccountInst("id5", "user5", "password5", "us", 1500f, 40, 35));

        ds.close();
    }
}