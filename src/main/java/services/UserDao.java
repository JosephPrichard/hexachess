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
    private static final ResultSetHandler<Integer> INT_MAPPER = new ScalarHandler<>();

    private final QueryRunner runner;

    public UserDao(DataSource ds) {
        runner = new QueryRunner(ds);
    }

    public void createExtensions() {
        try {
            var sql = "BEGIN; CREATE EXTENSION pg_trgm; END;";
            runner.execute(sql);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void createTable()  {
        try {
            var sql = """
                BEGIN;
                    CREATE TABLE users (
                        id VARCHAR NOT NULL,
                        username VARCHAR NOT NULL,
                        country VARCHAR,
                        elo NUMERIC NOT NULL,
                        highestElo NUMERIC NOT NULL,
                        wins INTEGER NOT NULL,
                        losses INTEGER NOT NULL,
                        joinedOn TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        password VARCHAR NOT NULL,
                        salt VARCHAR NOT NULL,
                        PRIMARY KEY (id));
                    
                    CREATE TABLE users_metadata (
                        id NUMERIC,
                        count INTEGER,
                        PRIMARY KEY (id));
                        
                    CREATE INDEX idxTrgmUsername ON users USING GIST (username gist_trgm_ops);
                    CREATE INDEX idxUsername ON users(username);
                    CREATE INDEX idxElo ON users(elo);
                END;
                """;
            runner.execute(sql);

            sql = "BEGIN; INSERT INTO users_metadata (id, count) VALUES (1, 0); END;";
            runner.execute(sql);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void defineProcedures() {
        try {
            var sql = """
                BEGIN;
                    CREATE FUNCTION probabilityWins(IN elo1 NUMERIC, IN elo2 NUMERIC)
                        RETURNS NUMERIC
                    LANGUAGE plpgsql
                    AS $$
                    BEGIN
                        RETURN 1.0 / (1.0 + POWER(10, (elo1 - elo2) / 400.0));
                    END $$;
        
                    CREATE PROCEDURE updateStatsUsingResult(
                        IN winId VARCHAR, IN loseId VARCHAR,
                        OUT winEloNext NUMERIC, OUT loseEloNext NUMERIC)
                    LANGUAGE plpgsql
                    AS $$
                    DECLARE
                        winElo NUMERIC;
                        loseElo NUMERIC;
                    BEGIN
                        SELECT elo INTO winElo FROM users WHERE id = winId;
                        SELECT elo INTO loseElo FROM users WHERE id = loseId;
                        
                        winEloNext = winElo + (30 * (1 - probabilityWins(loseElo, winElo)));
                        loseEloNext = loseElo + ((30 * probabilityWins(winElo, loseElo)) * -1);
                       
                        UPDATE users
                        SET elo = winEloNext, wins = wins + 1, highestElo = GREATEST(highestElo, winEloNext)
                        WHERE id = winId;
                        
                        UPDATE users
                        SET elo = loseEloNext, losses = losses + 1
                        WHERE id = loseId;
                    END $$;
                END;""";
            runner.execute(sql);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
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

    public UserInst insert(String username, String password) {
        var inst = new UserInst(UUID.randomUUID().toString(), username, password, "USA", 1000f, 0, 0);
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

    public void insert(UserInst inst) {
        try {
            var salt = generateSalt();
            var saltedPassword = inst.getPassword() + salt;
            var hashedPassword = BCrypt.withDefaults().hashToString(12, saltedPassword.toCharArray());

            var sql = """
                BEGIN;
                    INSERT INTO users (id, username, country, elo, highestElo, wins, losses, password, salt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
                    UPDATE users_metadata SET count = count + 1 WHERE id = 1;
                END;""";
            runner.execute(sql, inst.getNewId(), inst.getUsername(), inst.getCountry(),
                inst.getElo(), inst.getElo(), inst.getWins(), inst.getLosses(),
                hashedPassword, salt);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
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

    public Player verify(String username, String inputPassword) {
        try {
            var sql = "SELECT id, username, password, salt FROM users WHERE username = ?";
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
        try {
            var sql = """
            SELECT id, username, country, elo, highestElo, wins, losses, joinedOn
            FROM users
            WHERE id = ?""";
            return runner.query(sql, USER_MAPPER, id);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public UserEntity getByIdWithRank(String id) {
        try {
            var sql = """
            SELECT u1.id, u1.username, u1.country, u1.elo, u1.highestElo, u1.wins, u1.losses, u1.joinedOn,
                (SELECT COUNT(*) FROM users u2 WHERE u2.elo >= u1.elo) as rank
            FROM users u1
            WHERE u1.id = ?""";
            return runner.query(sql, USER_MAPPER, id);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<UserEntity> getByRanks(List<RankedUser> users) {
        return getByIds(users.stream().map(RankedUser::getId).toList());
    }

    public List<UserEntity> getByIds(List<String> ids) {
        try {
            var sql = """
            SELECT id, username, country, elo, wins, losses
            FROM users
            WHERE id IN""";
            sql += ids.stream().map(x -> "?").collect(Collectors.joining(",", " (", ") "));

            return runner.query(sql, USER_LIST_MAPPER, ids.toArray());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<UserEntity> getAll() {
        try {
            var sql = "SELECT id, username, country, elo, wins, losses FROM users";
            return runner.query(sql, USER_LIST_MAPPER);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<UserEntity> getLeaderboard(int page, int perPage) {
        try {
            var sql = """
            SELECT id, username, country, elo, wins, losses
            FROM users
            ORDER BY elo DESC LIMIT ? OFFSET ?
            """;

            page = Math.max(page, 1);
            var offset = (page - 1) * perPage;
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
        try {
            var sql = """
            SELECT id, username, country, elo, wins, losses, (username <-> ?) as rank
            FROM users
            WHERE username % ?
            ORDER BY rank DESC LIMIT ? OFFSET ?""";

            page = Math.max(page, 1);
            var offset = (page - 1) * perPage;
            var results = runner.query(sql, USER_LIST_MAPPER, name, name, perPage, offset);

            for (int i = 0; i < results.size(); i++) {
                results.get(i).setRank(i + 1);
            }
            return results;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public int countUsers() {
        try {
            var sql = "SELECT count FROM users_metadata";
            return runner.query(sql, INT_MAPPER);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public int countPages(int perPage) {
        return countUsers() / perPage + 1;
    }
}