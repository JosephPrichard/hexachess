package services;

import at.favre.lib.crypto.bcrypt.BCrypt;
import lombok.AllArgsConstructor;
import lombok.Data;
import models.Player;
import models.RankedUser;
import models.UserEntity;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import javax.sql.DataSource;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import static utils.Log.LOGGER;

public class UserDao {

    private static final ResultSetHandler<UserEntity> USER_MAPPER = new BeanHandler<>(UserEntity.class);
    private static final ResultSetHandler<List<UserEntity>> USER_LIST_MAPPER = new BeanListHandler<>(UserEntity.class);
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

    public static class TakenUsernameException extends RuntimeException {
    }

    public UserInst insert(String username, String password) throws TakenUsernameException {
        var inst = new UserInst(UUID.randomUUID().toString(), username, password, "USA", UserEntity.START_ELO, 0, 0);
        insert(inst);
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

    public void insert(UserInst inst) throws TakenUsernameException {
        var salt = generateSalt();
        var saltedPassword = inst.getPassword() + salt;
        var hashedPassword = BCrypt.withDefaults().hashToString(12, saltedPassword.toCharArray());

        var sql = """
            BEGIN;
            INSERT INTO users (id, username, country, elo, highestElo, wins, losses, password, salt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
            UPDATE users_metadata SET count = count + 1 WHERE id = 1;
            END;""";
        try {
            runner.execute(sql,
                inst.getNewId(),
                inst.getUsername(),
                inst.getCountry(),
                inst.getElo(),
                inst.getElo(),
                inst.getWins(),
                inst.getLosses(),
                hashedPassword,
                salt);
            LOGGER.info(String.format("Successfully inserted user=%s", inst));
        } catch (SQLException ex) {
            LOGGER.error(String.format("Failed to insert user=%s", inst), ex);
            var nextEx = ex.getNextException();
            if ("23505".equals(nextEx.getSQLState())) {
                throw new TakenUsernameException();
            }
            throw new RuntimeException(ex);
        }
    }

    public Player verify(String username, String inputPassword) {
        var sql = "SELECT id, username, password, salt FROM users WHERE UPPER(username) = UPPER(?)";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = runner.getDataSource().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);

            rs = stmt.executeQuery();

            if (!rs.next()) {
                LOGGER.warn(String.format("No user found with username=%s", username));
                return null;
            }
            var salt = rs.getString("salt");
            var password = rs.getString("password");
            var id = rs.getString("id");
            var usernameOut = rs.getString("username");

            var saltedPassword = inputPassword + salt;
            var result = BCrypt.verifyer().verify(saltedPassword.toCharArray(), password);
            LOGGER.info(String.format("Password verification %s for username=%s", result.verified ? "successful" : "failed", usernameOut));
            return result.verified ? new Player(id, usernameOut) : null;
        } catch (SQLException ex) {
            LOGGER.error(String.format("Failed to select user credentials for user=%s", username), ex);
            DbUtils.rollbackAndCloseQuietly(conn);
            throw new RuntimeException(ex);
        } finally {
            DbUtils.closeQuietly(conn);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(rs);
        }
    }

    public void update(String id, String newUsername, String newCountry, String newPassword) {
        if (newUsername == null && newCountry == null && newPassword == null) {
            LOGGER.info("No fields provided for update.");
            return;
        }

        var sql = new StringBuilder("BEGIN; UPDATE users SET ");
        List<Object> params = new ArrayList<>();

        if (newUsername != null) {
            sql.append("username = ?,");
            params.add(newUsername);
        }
        if (newCountry != null) {
            sql.append("country = ?,");
            params.add(newCountry);
        }
        if (newPassword != null) {
            sql.append("password = ?,");
            params.add(newPassword);
        }
        sql.deleteCharAt(sql.length() - 1);
        sql.append(" WHERE id = ?; END");
        params.add(id);

        try {
            runner.execute(sql.toString(), params.toArray());
            LOGGER.info(String.format("Successfully updated user with id=%s", id));
        } catch (SQLException ex) {
            LOGGER.error(String.format("Failed to update user with id=%s", id), ex);
            throw new RuntimeException(ex);
        }
    }

    @Data
    @AllArgsConstructor
    public static class EloChangeSet {
        double winEloDiff;
        double loseEloDiff;

        public void roundElo() {
            winEloDiff = Math.round(winEloDiff);
            loseEloDiff = Math.round(loseEloDiff);
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

            double winEloDiff = stmt.getBigDecimal(3).doubleValue();
            double loseEloDiff = stmt.getBigDecimal(4).doubleValue();

            LOGGER.info(String.format("Updated stats: winId=%s, loseId=%s, winEloDiff=%.2f, loseEloDiff=%.2f", winId, loseId, winEloDiff, loseEloDiff));
            return new EloChangeSet(winEloDiff, loseEloDiff);
        } catch (SQLException ex) {
            LOGGER.error(String.format("Failed to update stats for winId=%s, loseId=%s", winId, loseId), ex);
            DbUtils.rollbackAndCloseQuietly(conn);
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
            var user = runner.query(sql, USER_MAPPER, id);
            LOGGER.info(String.format("Successfully fetched user by id=%s", id));
            return user;
        } catch (SQLException ex) {
            LOGGER.error(String.format("Failed to fetch user by id=%s", id), ex);
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
            var user = runner.query(sql, USER_MAPPER, id);
            LOGGER.info(String.format("Successfully fetched user with rank by id=%s", id));
            return user;
        } catch (SQLException ex) {
            LOGGER.error(String.format("Failed to fetch user with rank by id=%s", id), ex);
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

        var idsStr = ids.stream().collect(Collectors.joining(",", "[", "]"));
        try {
            var results = runner.query(sql, USER_LIST_MAPPER, ids.toArray());
            LOGGER.info(String.format("Successfully selected users by ids=%s", idsStr));
            return results;
        } catch (SQLException e) {
            LOGGER.error(String.format("Failed to select users by ids=%s", idsStr));
            throw new RuntimeException(e);
        }
    }

    public List<UserEntity> getAll() {
        var sql = "SELECT id, username, country, elo, wins, losses FROM users";
        try {
            var results = runner.query(sql, USER_LIST_MAPPER);
            LOGGER.info("Successfully selected ALL records from the user table");
            return results;
        } catch (SQLException ex) {
            LOGGER.error("Failed to select ALL records from the user table");
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
            LOGGER.info(String.format("Successfully selected leaderboard for page=%s, perPage=%s", page, perPage));
            return results;
        } catch (SQLException ex) {
            LOGGER.error(String.format("Failed to select leaderboard for page=%s, perPage=%s", page, perPage));
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
            LOGGER.info(String.format("Successfully selected users by name for name=%s, page=%s, perPage=%s", name, page, perPage));
            return results;
        } catch (SQLException ex) {
            LOGGER.error(String.format("Failed to select users by name for name=%s, page=%s, perPage=%s", name, page, perPage));
            throw new RuntimeException(ex);
        }
    }

    public int countUsers() {
        var sql = "SELECT count FROM users_metadata";
        try {
            var count = runner.query(sql, INT_MAPPER);
            LOGGER.info(String.format("Successfully counted user table records with count=%s", count));
            return count;
        } catch (SQLException ex) {
            LOGGER.error("Failed to count user table records");
            throw new RuntimeException(ex);
        }
    }

    public int countPages(int perPage) {
        return countUsers() / perPage + 1;
    }
}