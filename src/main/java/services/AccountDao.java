package services;

import at.favre.lib.crypto.bcrypt.BCrypt;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import models.Stats;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import javax.sql.DataSource;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class AccountDao {

    private final QueryRunner runner;
    private final ResultSetHandler<AccountCreds> credsMapper = new BeanHandler<>(AccountCreds.class);
    private final ResultSetHandler<Stats> statsMapper = new BeanHandler<>(Stats.class);
    private final ResultSetHandler<List<Stats>> statsListMapper = new BeanListHandler<>(Stats.class);

    public AccountDao(DataSource ds) {
        runner = new QueryRunner(ds);
    }

    public void createTable() throws SQLException {
        var sql = """
            CREATE TABLE accounts (
                id VARCHAR,
                username VARCHAR,
                elo NUMERIC,
                wins INTEGER,
                losses INTEGER,
                password VARCHAR,
                salt VARCHAR,
                PRIMARY KEY (id))
            """;
        runner.execute(sql);
    }

    public void createIndices() throws SQLException {
        var sql = """
            CREATE INDEX idxUsername ON accounts(username);
            CREATE INDEX idxElo ON accounts(elo);
            """;
        runner.execute(sql);
    }

    @Data
    @AllArgsConstructor
    public static class AccountInst {
        private String newId;
        private String username;
        private String password;
        private float elo;
        private int wins;
        private int losses;
    }

    public AccountInst insert(String username, String password) throws SQLException, NoSuchAlgorithmException {
        var inst = new AccountInst(UUID.randomUUID().toString(), username, password, 1000f, 0, 0);
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

        var sql = "INSERT INTO accounts VALUES (?, ?, ?, ?, ?, ?, ?)";
        runner.execute(sql, inst.getNewId(), inst.getUsername(), inst.getElo(), inst.getWins(), inst.getLosses(), hashedPassword, salt);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountCreds {
        private String id;
        private String password;
        private String salt;
    }

    public String verify(String username, String inputPassword) throws SQLException {
        var sql = "SELECT id, password, salt FROM accounts WHERE username = ?";
        var accountCreds = runner.query(sql, credsMapper, username);

        if (accountCreds == null) {
            return null;
        }
        var saltedPassword = inputPassword + accountCreds.getSalt();
        var result = BCrypt.verifyer().verify(saltedPassword.toCharArray(), accountCreds.getPassword());
        return result.verified ? accountCreds.getId() : null;
    }

    @Data
    @AllArgsConstructor
    public static class StatsUpdt {
        private String id;
        private float eloInc;
        private int winsInc;
        private int lossesInc;
    }

    public void updateStats(StatsUpdt updt) throws SQLException {
        if (updt.getEloInc() == 0 && updt.getWinsInc() == 0 && updt.getLossesInc() == 0) {
            return; // noop
        }
        var sql = "UPDATE accounts SET elo = elo + ?, wins = wins + ?, losses = losses + ? WHERE id = ?";
        runner.execute(sql, updt.getEloInc(), updt.getWinsInc(), updt.getLossesInc(), updt.getId());
    }

    public Stats getStats(String id) throws SQLException {
        var sql = "SELECT id, username, elo, wins, losses FROM accounts WHERE id = ? ";
        return runner.query(sql, statsMapper, id);
    }

    public List<Stats> getLeaderboard(Float eloCursor, int limit) throws SQLException {
        var sql = new StringBuilder("SELECT id, username, elo, wins, losses FROM accounts WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();

        if (eloCursor != null) {
            sql.append(" AND elo < ? ");
            params.add(eloCursor);
        }
        sql.append(" ORDER BY elo DESC LIMIT ?");
        params.add(limit);

        return runner.query(sql.toString(), statsListMapper, params.toArray());
    }
}