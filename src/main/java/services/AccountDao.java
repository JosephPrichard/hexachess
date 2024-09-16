package services;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import model.Stats;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static utils.Log.LOGGER;

public class AccountDao {

    private final HikariDataSource ds;

    public AccountDao(HikariDataSource ds) {
        this.ds = ds;
    }

    public static void createTable(Connection conn) throws SQLException {
        var sql = """
            CREATE TABLE accounts
                (id VARCHAR, username VARCHAR, elo NUMERIC, wins INTEGER, losses INTEGER, password VARCHAR, salt VARCHAR,
                    PRIMARY KEY (id))
            """;
        try (var stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    public static void createIndices(Connection conn) throws SQLException {
        var sql = """
            CREATE INDEX idxUsername ON accounts(username);
            CREATE INDEX idxElo ON accounts(elo);
            """;
        try (var stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    public static String generateSalt() throws NoSuchAlgorithmException {
        var salt = new byte[16];
        SecureRandom.getInstanceStrong().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    @Getter
    @AllArgsConstructor
    public static class AccountInst {
        private String newId;
        private String username;
        private String password;
        private float elo;
        private int wins;
        private int losses;
    }

    public void insert(String username, String password) throws SQLException, NoSuchAlgorithmException {
        insert(new AccountInst(UUID.randomUUID().toString(), username, password, 1000f, 0, 0));
    }

    public void insert(AccountInst inst) throws SQLException, NoSuchAlgorithmException {
        var salt = generateSalt();
        var saltedPassword = inst.getPassword() + salt;
        var hashedPassword = BCrypt.withDefaults().hashToString(12, saltedPassword.toCharArray());

        try (var conn = ds.getConnection()) {
            var sql = "INSERT INTO accounts VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (var stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, inst.getNewId());
                stmt.setString(2, inst.getUsername());
                stmt.setFloat(3, inst.getElo());
                stmt.setInt(4, inst.getWins());
                stmt.setInt(5, inst.getLosses());
                stmt.setString(6, hashedPassword);
                stmt.setString(7, salt);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            throw e;
        }
    }

    public boolean verify(String username, String inputPassword) throws SQLException {
        try (var conn = ds.getConnection()) {
            var sql = "SELECT password, salt FROM accounts WHERE username = ?";
            try (var stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);

                try (var rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        var hashedPassword = rs.getString("password");
                        var salt = rs.getString("salt");

                        var saltedPassword = inputPassword + salt;
                        var result = BCrypt.verifyer().verify(saltedPassword.toCharArray(), hashedPassword);
                        return result.verified;
                    } else {
                        return false;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            throw e;
        }
    }

    @Getter
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

        try (var conn = ds.getConnection()) {
            var sql = "UPDATE accounts SET elo = elo + ?, wins = wins + ?, losses = losses + ? WHERE id = ?";
            try (var stmt = conn.prepareStatement(sql)) {
                stmt.setFloat(1, updt.getEloInc());
                stmt.setInt(2, updt.getWinsInc());
                stmt.setInt(3, updt.getLossesInc());
                stmt.setString(4, updt.getId());
                stmt.execute();
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            throw e;
        }
    }

    public Stats getStats(String id) throws SQLException {
        try (var conn = ds.getConnection()) {
            var sql = "SELECT id, username, elo, wins, losses FROM accounts WHERE id = ? ";
            try (var stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, id);

                var rs = stmt.executeQuery();
                return Stats.oneOfResults(rs);
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            throw e;
        }
    }

    public List<Stats> getLeaderboard(Float eloCursor, int limit) throws SQLException {
        try (var conn = ds.getConnection()) {
            var sql = "SELECT id, username, elo, wins, losses FROM accounts WHERE 1 = 1 ";
            if (eloCursor != null) {
                sql += " AND elo < ? ";
            }
            sql += " ORDER BY elo DESC LIMIT ? ";
            try (var stmt = conn.prepareStatement(sql)) {
                var index = 1;
                if (eloCursor != null) {
                    stmt.setFloat(index++, eloCursor);
                }
                stmt.setInt(index, limit);

                var rs = stmt.executeQuery();
                return Stats.manyOfResults(rs);
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            throw e;
        }
    }
}