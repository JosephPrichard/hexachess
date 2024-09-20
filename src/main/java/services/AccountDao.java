package services;

import at.favre.lib.crypto.bcrypt.BCrypt;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import models.Stats;
import org.intellij.lang.annotations.Language;
import utils.Database;

import javax.sql.DataSource;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class AccountDao {

    private final DataSource ds;

    public AccountDao(DataSource ds) {
        this.ds = ds;
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
        Database.executeUpdate(ds, sql);
    }

    public void createIndices() throws SQLException {
        var sql = """
            CREATE INDEX idxUsername ON accounts(username);
            CREATE INDEX idxElo ON accounts(elo);
            """;
        Database.executeUpdate(ds, sql);
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
        Database.executeUpdate(ds, sql, (stmt) -> {
            stmt.setString(1, inst.getNewId());
            stmt.setString(2, inst.getUsername());
            stmt.setFloat(3, inst.getElo());
            stmt.setInt(4, inst.getWins());
            stmt.setInt(5, inst.getLosses());
            stmt.setString(6, hashedPassword);
            stmt.setString(7, salt);
        });
    }

    public String verify(String username, String inputPassword) throws SQLException {
        var sql = "SELECT id, password, salt FROM accounts WHERE username = ?";
        return Database.executeQuery(ds, sql, (stmt) -> {
            stmt.setString(1, username);

            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    var id = rs.getString("id");
                    var hashedPassword = rs.getString("password");
                    var salt = rs.getString("salt");

                    var saltedPassword = inputPassword + salt;
                    var result = BCrypt.verifyer().verify(saltedPassword.toCharArray(), hashedPassword);
                    return result.verified ? id : null;
                } else {
                    return null;
                }
            }
        });
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
        Database.executeUpdate(ds, sql, (stmt) -> {
            stmt.setFloat(1, updt.getEloInc());
            stmt.setInt(2, updt.getWinsInc());
            stmt.setInt(3, updt.getLossesInc());
            stmt.setString(4, updt.getId());
        });
    }

    public Stats getStats(String id) throws SQLException {
        var sql = "SELECT id, username, elo, wins, losses FROM accounts WHERE id = ? ";
        return Database.executeQuery(ds, sql, (stmt) -> {
            stmt.setString(1, id);

            var rs = stmt.executeQuery();
            return Database.oneOfResults(rs, Stats::ofResult);
        });
    }

    public List<Stats> getLeaderboard(Float eloCursor, int limit) throws SQLException {
        var sql = String.format(
            "SELECT id, username, elo, wins, losses FROM accounts WHERE 1 = 1 %s ORDER BY elo DESC LIMIT ?",
            eloCursor != null ? " AND elo < ? " : "");

        return Database.executeQuery(ds, sql, (stmt) -> {
            var index = 1;
            if (eloCursor != null) {
                stmt.setFloat(index++, eloCursor);
            }
            stmt.setInt(index, limit);

            var rs = stmt.executeQuery();
            return Database.manyOfResults(rs, Stats::ofResult);
        });
    }
}