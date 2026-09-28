package com.dev.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Declarative schema installation for the SQLite store. The file is the source
 * of truth for the domain tables; keep it in sync with the repository mappers.
 */
public final class Schema {

  /** Bump when {@link #install} changes in a way that needs migration work. */
  public static final int VERSION = 1;

  private Schema() {
  }

  public static void install(Connection connection) {
    try (Statement statement = connection.createStatement()) {
      statement.executeUpdate("""
          CREATE TABLE IF NOT EXISTS zones (
            id     INTEGER PRIMARY KEY,
            state  TEXT NOT NULL,
            city   TEXT NOT NULL
          )
          """);

      statement.executeUpdate("""
          CREATE TABLE IF NOT EXISTS routes (
            id                     INTEGER PRIMARY KEY,
            origin_id              INTEGER NOT NULL REFERENCES zones(id),
            destiny_id             INTEGER NOT NULL REFERENCES zones(id),
            distance_meters        REAL    NOT NULL,
            estimated_time_seconds INTEGER NOT NULL,
            expense_cents          INTEGER NOT NULL
          )
          """);

      statement.executeUpdate("""
          CREATE TABLE IF NOT EXISTS centers (
            id        INTEGER PRIMARY KEY,
            name      TEXT    NOT NULL,
            level     TEXT    NOT NULL,
            parent_id INTEGER NOT NULL,
            zone_id   INTEGER NOT NULL REFERENCES zones(id)
          )
          """);

      statement.executeUpdate("""
          CREATE TABLE IF NOT EXISTS vehicles (
            id          INTEGER PRIMARY KEY,
            plate       TEXT NOT NULL UNIQUE,
            capacity_kg REAL NOT NULL
          )
          """);

      statement.executeUpdate("""
          CREATE TABLE IF NOT EXISTS packages (
            id               INTEGER PRIMARY KEY,
            waybill          TEXT    NOT NULL UNIQUE,
            route_id         INTEGER NOT NULL REFERENCES routes(id),
            weight           REAL    NOT NULL,
            price_in_cents   INTEGER NOT NULL,
            deadline         TEXT    NOT NULL,
            priority         TEXT    NOT NULL,
            status           TEXT    NOT NULL
          )
          """);

      statement.executeUpdate("""
          CREATE TABLE IF NOT EXISTS users (
            id       INTEGER PRIMARY KEY,
            name     TEXT NOT NULL,
            email    TEXT NOT NULL UNIQUE,
            password TEXT NOT NULL,
            role     TEXT NOT NULL
          )
          """);

      statement.executeUpdate("""
          CREATE TABLE IF NOT EXISTS audit_events (
            id        INTEGER PRIMARY KEY AUTOINCREMENT,
            at        TEXT NOT NULL,
            actor     TEXT NOT NULL,
            action    TEXT NOT NULL,
            entity    TEXT NOT NULL,
            entity_id TEXT NOT NULL,
            detail    TEXT NOT NULL
          )
          """);

      statement.executeUpdate("PRAGMA user_version = " + VERSION);
    } catch (SQLException exception) {
      throw new IllegalStateException("Cannot install database schema", exception);
    }
  }
}
