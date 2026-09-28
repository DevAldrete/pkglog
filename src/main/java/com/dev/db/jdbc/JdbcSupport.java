package com.dev.db.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Minimal JDBC helper shared by the repositories: binds positional parameters
 * and maps result sets. Keeping it small avoids pulling in an ORM while the
 * schema stays declarative in {@link com.dev.db.Schema}.
 */
abstract class JdbcSupport {

  @FunctionalInterface
  interface RowMapper<T> {
    T map(ResultSet resultSet) throws SQLException;
  }

  private final Connection connection;

  JdbcSupport(Connection connection) {
    this.connection = connection;
  }

  synchronized int update(String sql, Object... parameters) {
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      bind(statement, parameters);
      return statement.executeUpdate();
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed update: " + sql, exception);
    }
  }

  synchronized <T> List<T> query(String sql, RowMapper<T> mapper, Object... parameters) {
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      bind(statement, parameters);

      try (ResultSet resultSet = statement.executeQuery()) {
        List<T> rows = new ArrayList<>();

        while (resultSet.next()) {
          rows.add(mapper.map(resultSet));
        }

        return rows;
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed query: " + sql, exception);
    }
  }

  synchronized <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... parameters) {
    List<T> rows = query(sql, mapper, parameters);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  synchronized long count(String table) {
    return queryOne("SELECT COUNT(*) AS total FROM " + table,
        resultSet -> resultSet.getLong("total")).orElse(0L);
  }

  synchronized int nextId(String table) {
    return queryOne("SELECT COALESCE(MAX(id), 0) + 1 AS next FROM " + table,
        resultSet -> resultSet.getInt("next")).orElse(1);
  }

  private static void bind(PreparedStatement statement, Object... parameters) throws SQLException {
    for (int index = 0; index < parameters.length; index++) {
      statement.setObject(index + 1, parameters[index]);
    }
  }
}
