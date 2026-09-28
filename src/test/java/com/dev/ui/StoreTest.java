package com.dev.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dev.db.Database;
import com.dev.db.Repositories;
import com.dev.domain.DeliveryStatus;
import com.dev.domain.DistributionCenter;
import com.dev.domain.Priority;
import com.dev.domain.Role;
import com.dev.domain.User;
import com.dev.domain.Zone;
import com.dev.domain.Package;
import com.dev.domain.Route;
import com.dev.domain.Vehicle;
import com.dev.modules.Centers;
import com.dev.security.Passwords;

class StoreTest {

  private Database database;
  private Store store;

  @BeforeEach
  void setUp() {
    database = Database.inMemory();
    store = new Store(Repositories.jdbc(database));
  }

  @AfterEach
  void tearDown() {
    database.close();
  }

  @Test
  void seedMeetsMinimumVolumes() {
    assertTrue(store.packages().size() >= 50, "expected at least 50 packages");
    assertTrue(store.routes().size() >= 12, "expected at least 12 routes");
    assertTrue(store.zones().size() >= 12, "expected at least 12 zones");
    assertTrue(store.centers().size() >= 12, "expected at least 12 centers");
    assertFalse(store.vehicles().isEmpty());
  }

  @Test
  void seedRoutesReferenceKnownZones() {
    for (Route route : store.routes()) {
      assertTrue(store.findZone(route.originId()).isPresent());
      assertTrue(store.findZone(route.destinyId()).isPresent());
    }
  }

  @Test
  void seedPackagesReferenceKnownRoutes() {
    for (Package pkg : store.packages()) {
      boolean knownRoute = store.routes().stream().anyMatch(route -> route.id() == pkg.routeId());
      assertTrue(knownRoute, "package " + pkg.idGuia() + " points to a missing route");
    }
  }

  @Test
  void seedCentersFormASingleHierarchy() {
    long roots = store.centers().stream().filter(DistributionCenter::isRoot).count();
    assertEquals(1, roots);

    for (DistributionCenter center : store.centers()) {
      boolean parentExists = center.isRoot()
          || store.centers().stream().anyMatch(other -> other.id() == center.parentId());
      assertTrue(parentExists, "center " + center.name() + " has an unknown parent");
    }

    assertEquals(store.centers().size(), Centers.hierarchy(store.centers()).size());
  }

  @Test
  void cityNameFallsBackForUnknownZone() {
    assertEquals("Campinas", store.cityName(1));
    assertEquals("Zona 999", store.cityName(999));
  }

  @Test
  void mutationsArePersistedAndAudited() {
    Vehicle vehicle = new Vehicle(store.nextVehicleId(), "ZZZ-9Z99", 750f);
    store.saveVehicle(vehicle);

    assertTrue(store.vehicles().contains(vehicle));
    assertTrue(store.auditEvents(10).stream().anyMatch(event -> "Vehicle".equals(event.entity())));

    assertTrue(store.deleteVehicle(vehicle.id()));
    assertFalse(store.vehicles().contains(vehicle));
  }

  @Test
  void packageStatusChangeIsPersisted() {
    Package first = store.packages().get(0);

    store.updatePackageStatus(first.idGuia(), com.dev.domain.DeliveryStatus.DELIVERED);

    Package reloaded = store.packages().stream()
        .filter(pkg -> pkg.idGuia().equals(first.idGuia())).findFirst().orElseThrow();

    assertEquals(com.dev.domain.DeliveryStatus.DELIVERED, reloaded.status());
  }

  @Test
  void masterDataCrudRoundTripsThroughTheDatabase() {
    Zone zone = new Zone(store.nextZoneId(), "AM", "Manaus");
    store.saveZone(zone);
    assertTrue(store.zones().contains(zone));

    Route route = new Route(store.nextRouteId(), zone.id(), 1, 1_000d, Duration.ofMinutes(15),
        2_500L);
    store.saveRoute(route);
    assertTrue(store.routes().contains(route));

    User user = new User(store.nextUserId(), "Tester", "tester", "hash", Role.USER);
    store.saveUser(user);
    assertTrue(store.findUserByEmail("tester").isPresent());

    assertTrue(store.deleteRoute(route.id()));
    assertFalse(store.routes().contains(route));
    assertTrue(store.deleteZone(zone.id()));
    assertFalse(store.zones().contains(zone));
    assertTrue(store.deleteUser(user.id()));
    assertTrue(store.findUserByEmail("tester").isEmpty());
  }

  @Test
  void seededAccountsAuthenticateWithHashedPasswords() {
    var admin = store.findUserByEmail("admin").orElseThrow();

    assertTrue(Passwords.verify("admin123", admin.password()));
    assertFalse(Passwords.verify("wrong", admin.password()));
    assertFalse(admin.password().contains("admin123"));
  }

  @Test
  void importSkipsExistingWaybillsAndAssignsIds() {
    Package existing = store.packages().get(0);
    Package fresh = new Package(0, "WB-IMPORT-1", existing.routeId(), 2f, 1_000L,
        LocalDateTime.now().plusDays(1), Priority.NORMAL, DeliveryStatus.CREATED);

    int imported = store.importPackages(List.of(existing, fresh));

    assertEquals(1, imported);
    assertTrue(store.packages().stream().anyMatch(pkg -> pkg.idGuia().equals("WB-IMPORT-1")));
    assertTrue(store.auditEvents(5).stream().anyMatch(event -> "IMPORT".equals(event.action())));
  }

  @Test
  void reloadKeepsDataConsistent() {
    store.reload();

    assertFalse(store.packages().isEmpty());
    assertFalse(store.zones().isEmpty());
  }
}
