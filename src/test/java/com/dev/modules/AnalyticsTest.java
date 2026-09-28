package com.dev.modules;

import static com.dev.pkglog.Fixtures.pkg;
import static com.dev.pkglog.Fixtures.route;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dev.domain.DeliveryStatus;
import com.dev.domain.Package;
import com.dev.domain.Priority;
import com.dev.domain.Route;
import com.dev.ds.HashMap;

class AnalyticsTest {

  private static List<Route> sampleRoutes() {
    return List.of(
        route(1, 10, 20, 300, Duration.ofMinutes(30), 900),
        route(2, 20, 30, 100, Duration.ofMinutes(10), 500),
        route(3, 10, 30, 200, Duration.ofMinutes(20), 300));
  }

  private static List<Package> samplePackages() {
    return List.of(
        pkg(1, "WB-1", 10, 2.5f, 500, Priority.NORMAL, DeliveryStatus.CREATED),
        pkg(2, "WB-2", 20, 1.5f, 1500, Priority.IMPORTANT, DeliveryStatus.DELIVERED),
        pkg(3, "WB-3", 10, 4.0f, 1000, Priority.CRITICAL, DeliveryStatus.CREATED));
  }

  @Test
  void sumsRouteExpense() {
    assertEquals(1700L, Analytics.totalExpense(sampleRoutes()));
  }

  @Test
  void averagesRouteExpense() {
    assertEquals(1700d / 3d, Analytics.averageExpense(sampleRoutes()).orElseThrow(), 0.0001d);
  }

  @Test
  void averageExpenseIsEmptyWithoutRoutes() {
    assertTrue(Analytics.averageExpense(List.of()).isEmpty());
  }

  @Test
  void findsExpensiveAndCheapRoutes() {
    assertEquals(1, Analytics.mostExpensiveRoute(sampleRoutes()).orElseThrow().id());
    assertEquals(3, Analytics.cheapestRoute(sampleRoutes()).orElseThrow().id());
  }

  @Test
  void extremesAreEmptyWithoutRoutes() {
    assertTrue(Analytics.mostExpensiveRoute(List.of()).isEmpty());
    assertTrue(Analytics.cheapestRoute(List.of()).isEmpty());
  }

  @Test
  void sumsRouteDistance() {
    assertEquals(600d, Analytics.totalDistance(sampleRoutes()), 0.0001d);
  }

  @Test
  void sumsPackageRevenue() {
    assertEquals(3000L, Analytics.revenueInCents(samplePackages()));
  }

  @Test
  void averagesPackagePrice() {
    assertEquals(1000d, Analytics.averagePriceInCents(samplePackages()).orElseThrow(), 0.0001d);
  }

  @Test
  void revenueAndWeightExcludeCanceledPackages() {
    List<Package> packages = List.of(
        pkg(1, "WB-1", 10, 2f, 500, Priority.NORMAL, DeliveryStatus.CREATED),
        pkg(2, "WB-2", 10, 3f, 700, Priority.NORMAL, DeliveryStatus.CANCELED));

    assertEquals(500L, Analytics.revenueInCents(packages));
    assertEquals(2f, Analytics.totalWeight(packages), 0.0001f);
    assertTrue(Analytics.revenueByRoute(packages).get(10) == 500L);
  }

  @Test
  void averagePriceIsEmptyWithoutPackages() {
    assertTrue(Analytics.averagePriceInCents(List.of()).isEmpty());
  }

  @Test
  void findsHeaviestPackage() {
    assertEquals(3, Analytics.heaviestPackage(samplePackages()).orElseThrow().id());
    assertTrue(Analytics.heaviestPackage(List.of()).isEmpty());
  }

  @Test
  void countsPackagesByStatus() {
    HashMap<DeliveryStatus, Integer> counts = Analytics.countByStatus(samplePackages());

    assertEquals(2, counts.get(DeliveryStatus.CREATED));
    assertEquals(1, counts.get(DeliveryStatus.DELIVERED));
  }

  @Test
  void sumsRevenueByRoute() {
    HashMap<Integer, Long> revenue = Analytics.revenueByRoute(samplePackages());

    assertEquals(1500L, revenue.get(10));
    assertEquals(1500L, revenue.get(20));
  }
}
