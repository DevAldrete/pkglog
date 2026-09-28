package com.dev.ui;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.dev.db.Repositories;
import com.dev.db.Seeder;
import com.dev.domain.AuditEvent;
import com.dev.domain.DeliveryStatus;
import com.dev.domain.DistributionCenter;
import com.dev.domain.Package;
import com.dev.domain.Route;
import com.dev.domain.User;
import com.dev.domain.Vehicle;
import com.dev.domain.Zone;
import com.dev.modules.Deliveries;

/**
 * Application facade over the persisted data. The UI reads immutable snapshots
 * from here and every mutation is written through to SQLite and recorded in the
 * audit log, so the in-memory view and the database never drift apart.
 */
public final class Store {

  private final Repositories repositories;

  private List<Package> packages;
  private List<Route> routes;
  private List<Zone> zones;
  private List<DistributionCenter> centers;
  private List<Vehicle> vehicles;

  private User currentUser;

  public Store(Repositories repositories) {
    this.repositories = repositories;
    Seeder.seedIfEmpty(repositories);
    reload();
  }

  /** Rebuilds every in-memory snapshot from the database. */
  public void reload() {
    this.packages = repositories.packages().findAll();
    this.routes = repositories.routes().findAll();
    this.zones = repositories.zones().findAll();
    this.centers = repositories.centers().findAll();
    this.vehicles = repositories.vehicles().findAll();
  }

  // ---------------------------------------------------------------------------
  // Reads
  // ---------------------------------------------------------------------------

  public List<Package> packages() {
    return packages;
  }

  public List<Route> routes() {
    return routes;
  }

  public List<Zone> zones() {
    return zones;
  }

  public List<DistributionCenter> centers() {
    return centers;
  }

  public List<Vehicle> vehicles() {
    return vehicles;
  }

  public Optional<Zone> findZone(int zoneId) {
    for (Zone zone : zones) {
      if (zone.id() == zoneId) {
        return Optional.of(zone);
      }
    }

    return Optional.empty();
  }

  public Optional<Route> findRoute(int routeId) {
    for (Route route : routes) {
      if (route.id() == routeId) {
        return Optional.of(route);
      }
    }

    return Optional.empty();
  }

  public String cityName(int zoneId) {
    return findZone(zoneId).map(Zone::city).orElse("Zona " + zoneId);
  }

  public String routeDescription(int routeId) {
    return findRoute(routeId)
        .map(route -> cityName(route.originId()) + " -> " + cityName(route.destinyId()))
        .orElse("Ruta " + routeId);
  }

  public String describeCenter(int centerId) {
    for (DistributionCenter center : centers) {
      if (center.id() == centerId) {
        return center.name();
      }
    }

    return "Centro " + centerId;
  }

  // ---------------------------------------------------------------------------
  // Packages
  // ---------------------------------------------------------------------------

  public int nextPackageId() {
    return repositories.packages().nextId();
  }

  public void savePackage(Package pkg) {
    repositories.packages().save(pkg);
    audit("SAVE", "Package", pkg.idGuia(), "guardó paquete " + pkg.idGuia());
    reload();
  }

  /** Bulk import; skips waybills that already exist. Returns how many were added. */
  public int importPackages(List<Package> incoming) {
    int imported = 0;
    int nextId = repositories.packages().nextId();

    for (Package pkg : incoming) {
      if (repositories.packages().findByWaybill(pkg.idGuia()).isPresent()) {
        continue;
      }

      int id = pkg.id() > 0 && repositories.packages().findById(pkg.id()).isEmpty()
          ? pkg.id()
          : nextId;

      repositories.packages().save(new Package(id, pkg.idGuia(), pkg.routeId(), pkg.weight(),
          pkg.priceInCents(), pkg.deadline(), pkg.priority(), pkg.status()));
      imported++;
      nextId = Math.max(nextId, id + 1);
    }

    audit("IMPORT", "Package", "bulk", "importó " + imported + " paquetes");
    reload();

    return imported;
  }

  public boolean deletePackage(int id) {
    boolean removed = repositories.packages().deleteById(id);
    if (removed) {
      audit("DELETE", "Package", String.valueOf(id), "eliminó paquete #" + id);
      reload();
    }
    return removed;
  }

  public void updatePackageStatus(String waybill, DeliveryStatus status) {
    Package pkg = repositories.packages().findByWaybill(waybill)
        .orElseThrow(() -> new IllegalArgumentException("Guía no registrada: " + waybill));

    repositories.packages().save(pkg.withStatus(status));
    audit("STATUS", "Package", waybill, waybill + " -> " + status);
    reload();
  }

  /** Persists the outcome of a priority dispatch (one package moved to IN_TRANSIT). */
  public void applyDispatch(Deliveries.Dispatch dispatch) {
    Package dispatched = dispatch.dispatched();
    repositories.packages().save(dispatched);
    audit("DISPATCH", "Package", dispatched.idGuia(),
        "despachó " + dispatched.idGuia() + " (prioridad " + dispatched.priority().level() + ")");
    reload();
  }

  // ---------------------------------------------------------------------------
  // Routes, zones, centers, vehicles
  // ---------------------------------------------------------------------------

  public int nextRouteId() {
    return repositories.routes().nextId();
  }

  public void saveRoute(Route route) {
    repositories.routes().save(route);
    audit("SAVE", "Route", String.valueOf(route.id()), "guardó ruta #" + route.id());
    reload();
  }

  public boolean deleteRoute(int id) {
    boolean removed = repositories.routes().deleteById(id);
    if (removed) {
      audit("DELETE", "Route", String.valueOf(id), "eliminó ruta #" + id);
      reload();
    }
    return removed;
  }

  public int nextZoneId() {
    return repositories.zones().nextId();
  }

  public void saveZone(Zone zone) {
    repositories.zones().save(zone);
    audit("SAVE", "Zone", String.valueOf(zone.id()), "guardó zona " + zone.city());
    reload();
  }

  public boolean deleteZone(int id) {
    boolean removed = repositories.zones().deleteById(id);
    if (removed) {
      audit("DELETE", "Zone", String.valueOf(id), "eliminó zona #" + id);
      reload();
    }
    return removed;
  }

  public int nextCenterId() {
    return repositories.centers().nextId();
  }

  public void saveCenter(DistributionCenter center) {
    repositories.centers().save(center);
    audit("SAVE", "Center", String.valueOf(center.id()), "guardó centro " + center.name());
    reload();
  }

  public boolean deleteCenter(int id) {
    boolean removed = repositories.centers().deleteById(id);
    if (removed) {
      audit("DELETE", "Center", String.valueOf(id), "eliminó centro #" + id);
      reload();
    }
    return removed;
  }

  public int nextVehicleId() {
    return repositories.vehicles().nextId();
  }

  public void saveVehicle(Vehicle vehicle) {
    repositories.vehicles().save(vehicle);
    audit("SAVE", "Vehicle", vehicle.plate(), "guardó vehículo " + vehicle.plate());
    reload();
  }

  public boolean deleteVehicle(int id) {
    boolean removed = repositories.vehicles().deleteById(id);
    if (removed) {
      audit("DELETE", "Vehicle", String.valueOf(id), "eliminó vehículo #" + id);
      reload();
    }
    return removed;
  }

  // ---------------------------------------------------------------------------
  // Users and auditing
  // ---------------------------------------------------------------------------

  public List<User> users() {
    return repositories.users().findAll();
  }

  public Optional<User> findUserByEmail(String email) {
    return repositories.users().findByEmail(email);
  }

  public int nextUserId() {
    return repositories.users().nextId();
  }

  public void saveUser(User user) {
    repositories.users().save(user);
    audit("SAVE", "User", user.email(), "guardó usuario " + user.email());
  }

  public boolean deleteUser(int id) {
    boolean removed = repositories.users().deleteById(id);
    if (removed) {
      audit("DELETE", "User", String.valueOf(id), "eliminó usuario #" + id);
    }
    return removed;
  }

  public User currentUser() {
    return currentUser;
  }

  public void setCurrentUser(User currentUser) {
    this.currentUser = currentUser;
  }

  public boolean isAdmin() {
    return currentUser != null && currentUser.role() == com.dev.domain.Role.ADMIN;
  }

  public List<AuditEvent> auditEvents(int limit) {
    return repositories.audit().recent(limit);
  }

  public void audit(String action, String entity, String entityId, String detail) {
    String actor = currentUser == null ? "system" : currentUser.name();
    repositories.audit()
        .record(new AuditEvent(0, LocalDateTime.now(), actor, action, entity, entityId, detail));
  }
}
