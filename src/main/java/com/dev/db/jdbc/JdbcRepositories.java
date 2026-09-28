package com.dev.db.jdbc;

import java.sql.Connection;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.dev.db.Database;
import com.dev.db.Repositories;
import com.dev.db.repo.AuditRepository;
import com.dev.db.repo.CenterRepository;
import com.dev.db.repo.PackageRepository;
import com.dev.db.repo.RouteRepository;
import com.dev.db.repo.UserRepository;
import com.dev.db.repo.VehicleRepository;
import com.dev.db.repo.ZoneRepository;
import com.dev.domain.AuditEvent;
import com.dev.domain.CenterLevel;
import com.dev.domain.DeliveryStatus;
import com.dev.domain.DistributionCenter;
import com.dev.domain.Package;
import com.dev.domain.Priority;
import com.dev.domain.Role;
import com.dev.domain.Route;
import com.dev.domain.User;
import com.dev.domain.Vehicle;
import com.dev.domain.Zone;

/**
 * SQLite implementations of every repository port, grouped in a single factory
 * so the connection is shared and the wiring lives in one place.
 */
public final class JdbcRepositories {

  private JdbcRepositories() {
  }

  public static Repositories create(Database database) {
    Connection connection = database.connection();

    return new Repositories(
        new ZoneJdbc(connection),
        new RouteJdbc(connection),
        new CenterJdbc(connection),
        new VehicleJdbc(connection),
        new PackageJdbc(connection),
        new UserJdbc(connection),
        new AuditJdbc(connection));
  }

  private static final class ZoneJdbc extends JdbcSupport implements ZoneRepository {

    ZoneJdbc(Connection connection) {
      super(connection);
    }

    @Override
    public List<Zone> findAll() {
      return query("SELECT id, state, city FROM zones ORDER BY id",
          rs -> new Zone(rs.getInt("id"), rs.getString("state"), rs.getString("city")));
    }

    @Override
    public Optional<Zone> findById(int id) {
      return queryOne("SELECT id, state, city FROM zones WHERE id = ?",
          rs -> new Zone(rs.getInt("id"), rs.getString("state"), rs.getString("city")), id);
    }

    @Override
    public int nextId() {
      return nextId("zones");
    }

    @Override
    public void save(Zone zone) {
      update("""
          INSERT INTO zones (id, state, city) VALUES (?, ?, ?)
          ON CONFLICT(id) DO UPDATE SET state = excluded.state, city = excluded.city
          """, zone.id(), zone.state(), zone.city());
    }

    @Override
    public boolean deleteById(int id) {
      return update("DELETE FROM zones WHERE id = ?", id) > 0;
    }

    @Override
    public long count() {
      return count("zones");
    }
  }

  private static final class RouteJdbc extends JdbcSupport implements RouteRepository {

    RouteJdbc(Connection connection) {
      super(connection);
    }

    @Override
    public List<Route> findAll() {
      return query("""
          SELECT id, origin_id, destiny_id, distance_meters, estimated_time_seconds, expense_cents
          FROM routes ORDER BY id
          """, JdbcRepositories::mapRoute);
    }

    @Override
    public Optional<Route> findById(int id) {
      return queryOne("""
          SELECT id, origin_id, destiny_id, distance_meters, estimated_time_seconds, expense_cents
          FROM routes WHERE id = ?
          """, JdbcRepositories::mapRoute, id);
    }

    @Override
    public int nextId() {
      return nextId("routes");
    }

    @Override
    public void save(Route route) {
      update("""
          INSERT INTO routes
            (id, origin_id, destiny_id, distance_meters, estimated_time_seconds, expense_cents)
          VALUES (?, ?, ?, ?, ?, ?)
          ON CONFLICT(id) DO UPDATE SET
            origin_id = excluded.origin_id,
            destiny_id = excluded.destiny_id,
            distance_meters = excluded.distance_meters,
            estimated_time_seconds = excluded.estimated_time_seconds,
            expense_cents = excluded.expense_cents
          """, route.id(), route.originId(), route.destinyId(), route.distanceMeters(),
          route.estimatedTime().getSeconds(), route.expenseInCents());
    }

    @Override
    public boolean deleteById(int id) {
      return update("DELETE FROM routes WHERE id = ?", id) > 0;
    }

    @Override
    public long count() {
      return count("routes");
    }
  }

  private static final class CenterJdbc extends JdbcSupport implements CenterRepository {

    CenterJdbc(Connection connection) {
      super(connection);
    }

    @Override
    public List<DistributionCenter> findAll() {
      return query("SELECT id, name, level, parent_id, zone_id FROM centers ORDER BY id",
          JdbcRepositories::mapCenter);
    }

    @Override
    public Optional<DistributionCenter> findById(int id) {
      return queryOne("SELECT id, name, level, parent_id, zone_id FROM centers WHERE id = ?",
          JdbcRepositories::mapCenter, id);
    }

    @Override
    public int nextId() {
      return nextId("centers");
    }

    @Override
    public void save(DistributionCenter center) {
      update("""
          INSERT INTO centers (id, name, level, parent_id, zone_id) VALUES (?, ?, ?, ?, ?)
          ON CONFLICT(id) DO UPDATE SET
            name = excluded.name,
            level = excluded.level,
            parent_id = excluded.parent_id,
            zone_id = excluded.zone_id
          """, center.id(), center.name(), center.level().name(), center.parentId(),
          center.zoneId());
    }

    @Override
    public boolean deleteById(int id) {
      return update("DELETE FROM centers WHERE id = ?", id) > 0;
    }

    @Override
    public long count() {
      return count("centers");
    }
  }

  private static final class VehicleJdbc extends JdbcSupport implements VehicleRepository {

    VehicleJdbc(Connection connection) {
      super(connection);
    }

    @Override
    public List<Vehicle> findAll() {
      return query("SELECT id, plate, capacity_kg FROM vehicles ORDER BY id",
          rs -> new Vehicle(rs.getInt("id"), rs.getString("plate"), rs.getFloat("capacity_kg")));
    }

    @Override
    public Optional<Vehicle> findById(int id) {
      return queryOne("SELECT id, plate, capacity_kg FROM vehicles WHERE id = ?",
          rs -> new Vehicle(rs.getInt("id"), rs.getString("plate"), rs.getFloat("capacity_kg")),
          id);
    }

    @Override
    public int nextId() {
      return nextId("vehicles");
    }

    @Override
    public void save(Vehicle vehicle) {
      update("""
          INSERT INTO vehicles (id, plate, capacity_kg) VALUES (?, ?, ?)
          ON CONFLICT(id) DO UPDATE SET plate = excluded.plate,
            capacity_kg = excluded.capacity_kg
          """, vehicle.id(), vehicle.plate(), vehicle.capacityKg());
    }

    @Override
    public boolean deleteById(int id) {
      return update("DELETE FROM vehicles WHERE id = ?", id) > 0;
    }

    @Override
    public long count() {
      return count("vehicles");
    }
  }

  private static final class PackageJdbc extends JdbcSupport implements PackageRepository {

    PackageJdbc(Connection connection) {
      super(connection);
    }

    @Override
    public List<Package> findAll() {
      return query("""
          SELECT id, waybill, route_id, weight, price_in_cents, deadline, priority, status
          FROM packages ORDER BY id
          """, JdbcRepositories::mapPackage);
    }

    @Override
    public Optional<Package> findById(int id) {
      return queryOne("""
          SELECT id, waybill, route_id, weight, price_in_cents, deadline, priority, status
          FROM packages WHERE id = ?
          """, JdbcRepositories::mapPackage, id);
    }

    @Override
    public Optional<Package> findByWaybill(String waybill) {
      return queryOne("""
          SELECT id, waybill, route_id, weight, price_in_cents, deadline, priority, status
          FROM packages WHERE waybill = ?
          """, JdbcRepositories::mapPackage, waybill);
    }

    @Override
    public int nextId() {
      return nextId("packages");
    }

    @Override
    public void save(Package pkg) {
      update("""
          INSERT INTO packages
            (id, waybill, route_id, weight, price_in_cents, deadline, priority, status)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?)
          ON CONFLICT(id) DO UPDATE SET
            waybill = excluded.waybill,
            route_id = excluded.route_id,
            weight = excluded.weight,
            price_in_cents = excluded.price_in_cents,
            deadline = excluded.deadline,
            priority = excluded.priority,
            status = excluded.status
          """, pkg.id(), pkg.idGuia(), pkg.routeId(), pkg.weight(), pkg.priceInCents(),
          pkg.deadline().toString(), pkg.priority().name(), pkg.status().name());
    }

    @Override
    public boolean deleteById(int id) {
      return update("DELETE FROM packages WHERE id = ?", id) > 0;
    }

    @Override
    public long count() {
      return count("packages");
    }
  }

  private static final class UserJdbc extends JdbcSupport implements UserRepository {

    UserJdbc(Connection connection) {
      super(connection);
    }

    @Override
    public List<User> findAll() {
      return query("SELECT id, name, email, password, role FROM users ORDER BY id",
          JdbcRepositories::mapUser);
    }

    @Override
    public Optional<User> findById(int id) {
      return queryOne("SELECT id, name, email, password, role FROM users WHERE id = ?",
          JdbcRepositories::mapUser, id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
      return queryOne("SELECT id, name, email, password, role FROM users WHERE email = ?",
          JdbcRepositories::mapUser, email);
    }

    @Override
    public int nextId() {
      return nextId("users");
    }

    @Override
    public void save(User user) {
      update("""
          INSERT INTO users (id, name, email, password, role) VALUES (?, ?, ?, ?, ?)
          ON CONFLICT(id) DO UPDATE SET
            name = excluded.name,
            email = excluded.email,
            password = excluded.password,
            role = excluded.role
          """, user.id(), user.name(), user.email(), user.password(), user.role().name());
    }

    @Override
    public boolean deleteById(int id) {
      return update("DELETE FROM users WHERE id = ?", id) > 0;
    }

    @Override
    public long count() {
      return count("users");
    }
  }

  private static final class AuditJdbc extends JdbcSupport implements AuditRepository {

    AuditJdbc(Connection connection) {
      super(connection);
    }

    @Override
    public void record(AuditEvent event) {
      update("""
          INSERT INTO audit_events (at, actor, action, entity, entity_id, detail)
          VALUES (?, ?, ?, ?, ?, ?)
          """, event.at().toString(), event.actor(), event.action(), event.entity(),
          event.entityId(), event.detail());
    }

    @Override
    public List<AuditEvent> recent(int limit) {
      return query("""
          SELECT id, at, actor, action, entity, entity_id, detail
          FROM audit_events ORDER BY id DESC LIMIT ?
          """, rs -> new AuditEvent(
          rs.getLong("id"),
          LocalDateTime.parse(rs.getString("at")),
          rs.getString("actor"),
          rs.getString("action"),
          rs.getString("entity"),
          rs.getString("entity_id"),
          rs.getString("detail")), limit);
    }

    @Override
    public long count() {
      return count("audit_events");
    }
  }

  private static Route mapRoute(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new Route(
        rs.getInt("id"),
        rs.getInt("origin_id"),
        rs.getInt("destiny_id"),
        rs.getDouble("distance_meters"),
        Duration.ofSeconds(rs.getLong("estimated_time_seconds")),
        rs.getLong("expense_cents"));
  }

  private static DistributionCenter mapCenter(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new DistributionCenter(
        rs.getInt("id"),
        rs.getString("name"),
        CenterLevel.valueOf(rs.getString("level")),
        rs.getInt("parent_id"),
        rs.getInt("zone_id"));
  }

  private static Package mapPackage(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new Package(
        rs.getInt("id"),
        rs.getString("waybill"),
        rs.getInt("route_id"),
        rs.getFloat("weight"),
        rs.getLong("price_in_cents"),
        LocalDateTime.parse(rs.getString("deadline")),
        Priority.valueOf(rs.getString("priority")),
        DeliveryStatus.valueOf(rs.getString("status")));
  }

  private static User mapUser(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new User(
        rs.getInt("id"),
        rs.getString("name"),
        rs.getString("email"),
        rs.getString("password"),
        Role.valueOf(rs.getString("role")));
  }
}
