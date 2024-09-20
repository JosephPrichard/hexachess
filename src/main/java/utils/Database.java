package utils;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Database {

    public interface DbConsumer<T> {
        void accept(T stmt) throws SQLException;
    }

    public interface DbFunction<In, Out> {
        Out apply(In stmt) throws SQLException;
    }

    public static void executeUpdate(DataSource ds, String sql) throws SQLException {
        try (var conn = ds.getConnection()) {
            try (var stmt = conn.prepareStatement(sql)) {
                stmt.executeUpdate();
            }
        }
    }

    public static void executeUpdate(DataSource ds, String sql, DbConsumer<PreparedStatement> executor) throws SQLException {
        try (var conn = ds.getConnection()) {
            try (var stmt = conn.prepareStatement(sql)) {
                executor.accept(stmt);
                stmt.executeUpdate();
            }
        }
    }

    public static <T> T executeQuery(DataSource ds, String sql, DbFunction<PreparedStatement, T> executor) throws SQLException {
        try (var conn = ds.getConnection()) {
            try (var stmt = conn.prepareStatement(sql)) {
                return executor.apply(stmt);
            }
        }
    }

    public static <T> T oneOfResults(ResultSet rs, DbFunction<ResultSet, T> ofResult) throws SQLException {
        try (rs) {
            if (rs.next()) {
                return ofResult.apply(rs);
            }
            return null;
        }
    }

    public static <T> List<T> manyOfResults(ResultSet rs, DbFunction<ResultSet, T> ofResult) throws SQLException {
        try (rs) {
            List<T> objectList = new ArrayList<>();
            while (rs.next()) {
                var object = ofResult.apply(rs);
                objectList.add(object);
            }
            return objectList;
        }
    }
}
