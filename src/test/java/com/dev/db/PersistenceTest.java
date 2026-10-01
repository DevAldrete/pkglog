package com.dev.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dev.domain.AuditEvent;
import com.dev.domain.DeliveryStatus;
import com.dev.domain.Package;
import com.dev.domain.Priority;
import com.dev.domain.Route;
import com.dev.domain.Zone;
import com.dev.security.Passwords;

class PersistenceTest {

  private Database database;
  private Repositories repositories;

  @BeforeEach
  void setUp() {
    database = Database.inMemory();
    repositories = Repositories.jdbc(database);
  }

  @AfterEach
  void tearDown() {
    database.close();
  }

  @Test
  void seedingFillsAnEmptyDatabase() {
    Seeder.seedIfEmpty(repositories);

    assertEquals(12, repositories.zones().count());
    assertEquals(14, repositories.routes().count());
    assertEquals(12, repositories.centers().count());
    assertEquals(6, repositories.vehicles().count());
    assertEquals(60, repositories.packages().count());
    assertEquals(2, repositories.users().count());
  }

  @Test
  void seedingIsIdempotent() {
    Seeder.seedIfEmpty(repositories);
    Seeder.seedIfEmpty(repositories);

    assertEquals(60, repositories.packages().count());
  }

  @Test
  void staleSeedDeadlinesAreRefreshedWithoutTouchingOperatorData() {
    Seeder.seedIfEmpty(repositories);

    LocalDateTime now = LocalDateTime.of(2026, 10, 1, 12, 0);

    // Simulate a baseline seeded months ago: every deadline aged out.
    for (Package pkg : repositories.packages().findAll()) {
      repositories.packages().save(pkg.withDeadline(now.minusMonths(4)));
    }

    Package operatorPackage = new Package(repositories.packages().nextId(), "WB-MANUAL", 1, 2f,
        1_000L, now.minusDays(30), Priority.NORMAL, DeliveryStatus.CREATED);
    repositories.packages().save(operatorPackage);

    int refreshed = Seeder.refreshStaleSeedDeadlines(repositories, now);

    assertTrue(refreshed > 0, "stale baseline should be rewritten");
    assertEquals(now.minusDays(30),
        repositories.packages().findByWaybill("WB-MANUAL").orElseThrow().deadline(),
        "operator data must be untouched");

    long future = repositories.packages().findAll().stream()
        .filter(pkg -> pkg.idGuia().startsWith("PKGLOG-"))
        .filter(pkg -> pkg.deadline().isAfter(now))
        .count();
    assertTrue(future > 0, "some baseline deadlines should be back in the future");

    // Once refreshed the baseline is fresh and must not be rewritten again.
    assertEquals(0, Seeder.refreshStaleSeedDeadlines(repositories, now));
  }

  @Test
  void freshlySeededDeadlinesAreNotStale() {
    Seeder.seedIfEmpty(repositories);

    assertEquals(0, Seeder.refreshStaleSeedDeadlines(repositories, LocalDateTime.now()));
  }

  @Test
  void zoneRoundTripsThroughTheDatabase() {
    Zone saved = new Zone(99, "AM", "Manaus");
    repositories.zones().save(saved);

    assertEquals(saved, repositories.zones().findById(99).orElseThrow());

    repositories.zones().save(new Zone(99, "AM", "Manaus Industrial"));

    assertEquals("Manaus Industrial", repositories.zones().findById(99).orElseThrow().city());
    assertTrue(repositories.zones().deleteById(99));
    assertTrue(repositories.zones().findById(99).isEmpty());
  }

  @Test
  void routeRoundTripsDurationAndMoney() {
    repositories.zones().save(new Zone(1, "SP", "Campinas"));
    repositories.zones().save(new Zone(2, "SP", "Santos"));

    Route route = new Route(7, 1, 2, 1500d, Duration.ofMinutes(90), 12_345L);
    repositories.routes().save(route);

    Route loaded = repositories.routes().findById(7).orElseThrow();

    assertEquals(route, loaded);
    assertEquals(Duration.ofMinutes(90), loaded.estimatedTime());
  }

  @Test
  void packageRoundTripsDeadlinePriorityAndStatus() {
    repositories.zones().save(new Zone(1, "SP", "Campinas"));
    repositories.zones().save(new Zone(2, "SP", "Santos"));
    repositories.routes().save(new Route(1, 1, 2, 100d, Duration.ofMinutes(10), 500L));

    LocalDateTime deadline = LocalDateTime.of(2026, 5, 20, 14, 30);
    Package pkg = new Package(5, "WB-XYZ", 1, 3.5f, 9_900L, deadline, Priority.URGENT,
        DeliveryStatus.IN_TRANSIT);
    repositories.packages().save(pkg);

    assertEquals(pkg, repositories.packages().findById(5).orElseThrow());
    assertEquals(pkg, repositories.packages().findByWaybill("WB-XYZ").orElseThrow());
    assertFalse(repositories.packages().findByWaybill("WB-MISSING").isPresent());
  }

  @Test
  void auditEventsAreAppendOnlyAndNewestFirst() {
    repositories.audit().record(new AuditEvent(0, LocalDateTime.now(), "admin", "CREATE",
        "Zone", "1", "created zone"));
    repositories.audit().record(new AuditEvent(0, LocalDateTime.now(), "admin", "UPDATE",
        "Zone", "1", "renamed zone"));

    assertEquals(2, repositories.audit().count());
    assertEquals("UPDATE", repositories.audit().recent(1).get(0).action());
  }

  @Test
  void passwordsAreSaltedAndVerifiable() {
    String stored = Passwords.hash("s3cret");

    assertFalse(stored.contains("s3cret"));
    assertTrue(Passwords.verify("s3cret", stored));
    assertFalse(Passwords.verify("wrong", stored));
    assertFalse(Passwords.verify("s3cret", "garbage"));
  }
}
