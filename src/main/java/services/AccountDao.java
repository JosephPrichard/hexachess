package services;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import static utils.Log.LOGGER;

public class AccountDao {
    private final HikariDataSource ds;

    public AccountDao(HikariDataSource ds) {
        this.ds = ds;
    }

    public void createTable(Connection conn) throws SQLException {
        var sql = """
            CREATE TABLE accounts
                (id VARCHAR, username VARCHAR, elo NUMERIC, wins INTEGER, losses INTEGER, password VARCHAR, salt VARCHAR,
                    PRIMARY KEY (id))
            """;
        try (var stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    public void createIndices(Connection conn) {

    }

    public static String generateSalt() throws NoSuchAlgorithmException {
        var salt = new byte[16];
        SecureRandom.getInstanceStrong().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public void insert(String username, String password) throws SQLException, NoSuchAlgorithmException {
        try (var conn = ds.getConnection()) {
            try (var stmt = conn.prepareStatement("INSERT INTO accounts VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                var newId = UUID.randomUUID().toString();
                var salt = generateSalt();
                var saltedPassword = password + salt;
                var hashedPassword = BCrypt.withDefaults().hashToString(12, saltedPassword.toCharArray());

                stmt.setString(1, newId);
                stmt.setString(2, username);
                stmt.setFloat(3, 0f);
                stmt.setInt(4, 0);
                stmt.setInt(5, 0);
                stmt.setString(6, hashedPassword);
                stmt.setString(7, salt);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            throw e;
        }
    }

    public boolean verify(String username, String inputPassword) throws SQLException {
        try (var conn = ds.getConnection()) {
            try (var stmt = conn.prepareStatement("SELECT password, salt FROM accounts WHERE username = ?")) {
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
            LOGGER.log(Level.SEVERE, e.getMessage());
            throw e;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class StatsUpdt {
        private String id;
        private float elo;
        private int wins;
        private int losses;
    }

    public void updateStats(StatsUpdt updt) throws SQLException {
        try (var conn = ds.getConnection()) {
            try (var stmt = conn.prepareStatement("UPDATE stats SET elo = ?, wins = ?, losses = ? WHERE id = ?")) {
                stmt.setFloat(1, updt.getElo());
                stmt.setInt(2, updt.getWins());
                stmt.setInt(3, updt.getLosses());
                stmt.setString(4, updt.getId());
                stmt.execute();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            throw e;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class Stats {
        private String id;
        private String username;
        private float elo;
        private int wins;
        private int losses;

        public static List<Stats> fromResultSet(ResultSet rs) throws SQLException {
            try (rs) {
                List<Stats> statList = new ArrayList<>();
                while (rs.next()) {
                    var id = rs.getString("id");
                    var username = rs.getString("username");
                    var elo = rs.getFloat("elo");
                    var wins = rs.getInt("wins");
                    var losses = rs.getInt("losses");
                    statList.add(new Stats(id, username, elo, wins, losses));
                }
                return statList;
            }
        }
    }

    public List<Stats> getLeaderboard(Float eloCursor) throws SQLException {
        try (var conn = ds.getConnection()) {
            var sql = "SELECT id, username, elo, wins, losses FROM accounts WHERE 1 = 1 ";
            if (eloCursor != null) {
                sql += "AND elo < ? ";
            }
            sql += "ORDER BY elo DESC LIMIT 20";
            try (var stmt = conn.prepareStatement(sql)) {
                if (eloCursor != null) {
                    stmt.setFloat(1, eloCursor);
                }
                var rs = stmt.executeQuery();
                return Stats.fromResultSet(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            throw e;
        }
    }
}