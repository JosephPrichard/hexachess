package services;

import at.favre.lib.crypto.bcrypt.BCrypt;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import models.Player;
import models.RankedUser;
import models.UserEntity;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import javax.sql.DataSource;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

public class UserDao {

    private static final ResultSetHandler<AccountCreds> CREDS_MAPPER = new BeanHandler<>(AccountCreds.class);
    private static final ResultSetHandler<UserEntity> USER_MAPPER = new BeanHandler<>(UserEntity.class);
    private static final ResultSetHandler<List<UserEntity>> USER_LIST_MAPPER = new BeanListHandler<>(UserEntity.class);
    private static final ResultSetHandler<List<UserTuple>> USER_TUPLE_LIST_MAPPER = new BeanListHandler<>(UserTuple.class);
    private static final ResultSetHandler<Integer> INT_MAPPER = new ScalarHandler<>();

    private final QueryRunner runner;

    public UserDao(DataSource ds) {
        runner = new QueryRunner(ds);
    }

    @Data
    @AllArgsConstructor
    public static class UserInst {
        String newId;
        String username;
        String password;
        String country;
        float elo;
        int wins;
        int losses;
    }

    public UserInst insert(String username, String password) {
        var inst = new UserInst(UUID.randomUUID().toString(), username, password, "USA", 1000f, 0, 0);
        if (!insert(inst)) {
            return null;
        }
        return inst;
    }

    public static String generateSalt() {
        try {
            var salt = new byte[16];
            SecureRandom.getInstanceStrong().nextBytes(salt);
            return Base64.getEncoder().encodeToString(salt);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean insert(UserInst inst) {
        var salt = generateSalt();
        var saltedPassword = inst.getPassword() + salt;
        var hashedPassword = BCrypt.withDefaults().hashToString(12, saltedPassword.toCharArray());

        var sql = """
            BEGIN;
            INSERT INTO
                users (id, username, country, elo, highestElo, wins, losses, password, salt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
            UPDATE users_metadata SET count = count + 1 WHERE id = 1;
            END;""";
        try {
            runner.execute(sql, inst.getNewId(), inst.getUsername(), inst.getCountry(),
                inst.getElo(), inst.getElo(), inst.getWins(), inst.getLosses(),
                hashedPassword, salt);
            return true;
        } catch (SQLException ex) {
            if (Objects.equals(ex.getSQLState(), "23505")) {
                return false;
            }
            throw new RuntimeException(ex);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountCreds {
        String id;
        String username;
        String password;
        String salt;
    }

    public Player verify(String username, String inputPassword) {
        var sql = "SELECT id, username, password, salt FROM users WHERE UPPER(username) = UPPER(?)";
        try {
            var accountCreds = runner.query(sql, CREDS_MAPPER, username);
            if (accountCreds == null) {
                return null;
            }
            var saltedPassword = inputPassword + accountCreds.getSalt();
            var result = BCrypt.verifyer().verify(saltedPassword.toCharArray(), accountCreds.getPassword());
            return result.verified ? new Player(accountCreds.getId(), accountCreds.getUsername()) : null;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void updateAccount(String id, String username, String country) {
        try {
            var sql = "BEGIN; UPDATE users SET username = ?, country = ? WHERE id = ?; END";
            runner.execute(sql, username, country, id);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
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

    public EloChangeSet updateStatsUsingResult(String winId, String loseId) {
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
        } catch (SQLException ex) {
            DbUtils.commitAndCloseQuietly(conn);
            throw new RuntimeException(ex);
        } finally {
            DbUtils.closeQuietly(conn);
            DbUtils.closeQuietly(stmt);
        }
    }

    public UserEntity getById(String id) {
        var sql = """
            SELECT id, username, country, elo, highestElo, wins, losses, joinedOn
            FROM users
            WHERE id = ?""";
        try {
            return runner.query(sql, USER_MAPPER, id);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public UserEntity getByIdWithRank(String id) {
        var sql = """
            SELECT u1.id, u1.username, u1.country, u1.elo, u1.highestElo, u1.wins, u1.losses, u1.joinedOn,
                (SELECT COUNT(*) FROM users u2 WHERE u2.elo >= u1.elo) as rank
            FROM users u1
            WHERE u1.id = ?""";
        try {
            return runner.query(sql, USER_MAPPER, id);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<UserEntity> getByRanks(List<RankedUser> users) {
        return getByIds(users.stream().map(RankedUser::getId).toList());
    }

    public List<UserEntity> getByIds(List<String> ids) {
        var sql = """
            SELECT id, username, country, elo, wins, losses
            FROM users
            WHERE 1 = 1 AND"""
              + ids.stream().map(x -> "?").collect(Collectors.joining(",", " id IN (", ") "));
        try {
            return runner.query(sql, USER_LIST_MAPPER, ids.toArray());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<UserEntity> getAll() {
        var sql = "SELECT id, username, country, elo, wins, losses FROM users";
        try {
            return runner.query(sql, USER_LIST_MAPPER);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<UserEntity> getLeaderboard(int page, int perPage) {
        var sql = """
            SELECT id, username, country, elo, wins, losses
            FROM users
            ORDER BY elo DESC LIMIT ? OFFSET ?""";

        page = Math.max(page, 1);
        var offset = (page - 1) * perPage;

        try {
            var results = runner.query(sql, USER_LIST_MAPPER, perPage, offset);
            for (int i = 0; i < results.size(); i++) {
                results.get(i).setRank((page - 1) * perPage + i + 1);
            }
            return results;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<UserEntity> searchByName(String name, int page, int perPage) {
        var sql = """
            SELECT id, username, country, elo, wins, losses, (username <-> ?) as rank
            FROM users
            WHERE 1 = 1 AND username % ?
            ORDER BY rank DESC LIMIT ? OFFSET ?""";

        page = Math.max(page, 1);
        var offset = (page - 1) * perPage;

        try {
            var results = runner.query(sql, USER_LIST_MAPPER, name, name, perPage, offset);
            for (int i = 0; i < results.size(); i++) {
                results.get(i).setRank(i + 1);
            }
            return results;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserTuple {
        String id;
        String username;
    }

    public List<UserTuple> quickSearchByName(String name) {
        var sql = """
            SELECT id, username, (username <-> ?) as rank
            FROM users
            WHERE 1 = 1 AND username % ?
            ORDER BY rank DESC LIMIT 10""";

        try {
            return runner.query(sql, USER_TUPLE_LIST_MAPPER, name, name);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public int countUsers() {
        var sql = "SELECT count FROM users_metadata";
        try {
            return runner.query(sql, INT_MAPPER);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public int countPages(int perPage) {
        return countUsers() / perPage + 1;
    }
}