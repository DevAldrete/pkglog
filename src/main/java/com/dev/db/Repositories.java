package com.dev.db;

import com.dev.db.repo.AuditRepository;
import com.dev.db.repo.CenterRepository;
import com.dev.db.repo.PackageRepository;
import com.dev.db.repo.RouteRepository;
import com.dev.db.repo.UserRepository;
import com.dev.db.repo.VehicleRepository;
import com.dev.db.repo.ZoneRepository;

/**
 * Bundle of every persistence port. Passing this around keeps the UI and the
 * {@link com.dev.ui.Store} independent from JDBC.
 */
public final class Repositories {

  private final ZoneRepository zones;
  private final RouteRepository routes;
  private final CenterRepository centers;
  private final VehicleRepository vehicles;
  private final PackageRepository packages;
  private final UserRepository users;
  private final AuditRepository audit;

  public Repositories(
      ZoneRepository zones,
      RouteRepository routes,
      CenterRepository centers,
      VehicleRepository vehicles,
      PackageRepository packages,
      UserRepository users,
      AuditRepository audit) {
    this.zones = zones;
    this.routes = routes;
    this.centers = centers;
    this.vehicles = vehicles;
    this.packages = packages;
    this.users = users;
    this.audit = audit;
  }

  /** Opens JDBC backed repositories over the given database. */
  public static Repositories jdbc(Database database) {
    return com.dev.db.jdbc.JdbcRepositories.create(database);
  }

  public ZoneRepository zones() {
    return zones;
  }

  public RouteRepository routes() {
    return routes;
  }

  public CenterRepository centers() {
    return centers;
  }

  public VehicleRepository vehicles() {
    return vehicles;
  }

  public PackageRepository packages() {
    return packages;
  }

  public UserRepository users() {
    return users;
  }

  public AuditRepository audit() {
    return audit;
  }
}
