package com.dev.db;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the single SQLite connection used by the application. SQLite is an
 * embedded, zero-administration database, which suits a desktop logistics
 * installation while still giving us durable, queryable storage.
 *
 * <p>A single connection is shared because the Swing UI is single user; every
 * access is synchronized to avoid concurrent statement use.
 */
public final class Database implements AutoCloseable {

  /** Default file created next to the working directory. */
  public static final String DEFAULT_FILE = "pkglog.db";

  private final Connection connection;

  private Database(Connection connection) {
    this.connection = connection;
  }

  /** Opens (creating if absent) a file backed database and installs the schema. */
  public static Database open(Path file) {
    String url = "jdbc:sqlite:" + file.toAbsolutePath();

    try {
      Connection connection = DriverManager.getConnection(url);
      connection.setAutoCommit(true);

      try (Statement statement = connection.createStatement()) {
        // Referential integrity is off by default in SQLite.
        statement.execute("PRAGMA foreign_keys = ON");
      }

      Database database = new Database(connection);
      Schema.install(connection);

      return database;
    } catch (SQLException exception) {
      throw new IllegalStateException("Cannot open database at " + url, exception);
    }
  }

  /** Opens the default file database. */
  public static Database openDefault() {
    return open(Path.of(DEFAULT_FILE));
  }

  /** Creates a throw-away in-memory database, mainly for tests. */
  public static Database inMemory() {
    try {
      Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");

      try (Statement statement = connection.createStatement()) {
        statement.execute("PRAGMA foreign_keys = ON");
      }

      Schema.install(connection);

      return new Database(connection);
    } catch (SQLException exception) {
      throw new IllegalStateException("Cannot open in-memory database", exception);
    }
  }

  /** Shared connection. Callers must not close it. */
  public Connection connection() {
    return connection;
  }

  @Override
  public void close() {
    try {
      connection.close();
    } catch (SQLException exception) {
      throw new IllegalStateException("Cannot close database", exception);
    }
  }
}
