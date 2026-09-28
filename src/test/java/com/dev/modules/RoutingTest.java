package com.dev.modules;

import static com.dev.pkglog.Fixtures.pkg;
import static com.dev.pkglog.Fixtures.route;
import static com.dev.pkglog.Fixtures.vehicle;
import static com.dev.pkglog.Fixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dev.domain.DeliveryStatus;
import com.dev.domain.Package;
import com.dev.domain.Priority;
import com.dev.domain.Route;
import com.dev.domain.Vehicle;
import com.dev.domain.Zone;
import com.dev.ds.Graph;
import com.dev.ds.HashMap;
import com.dev.ds.WeightedGraph;

class RoutingTest {

  private static List<Route> sampleRoutes() {
    return List.of(
        route(1, 10, 20, 300, Duration.ofMinutes(30), 900),
        route(2, 20, 30, 100, Duration.ofMinutes(10), 500),
        route(3, 10, 30, 200, Duration.ofMinutes(20), 300),
        route(4, 30, 40, 100, Duration.ofMinutes(50), 100));
  }

  private static List<Integer> ids(List<Route> routes) {
    return routes.stream().map(Route::id).toList();
  }

  @Test
  void networkConnectsOriginsAndDestinations() {
    Graph<Integer> network = Routing.network(sampleRoutes());

    assertEquals(4, network.size());
    assertTrue(network.containsVertex(10));
    assertTrue(network.containsVertex(40));
    assertTrue(network.neighbors(10).contains(20));
    assertTrue(network.neighbors(10).contains(30));
  }

  @Test
  void shortestPathFindsFewestStops() {
    Graph<Integer> network = Routing.network(sampleRoutes());

    assertEquals(List.of(10, 30, 40), Routing.shortestPath(network, 10, 40));
  }

  @Test
  void shortestPathReturnsEmptyWhenUnreachable() {
    Graph<Integer> network = Routing.network(sampleRoutes());

    assertTrue(Routing.shortestPath(network, 40, 10).isEmpty());
  }

  @Test
  void sortsRoutesByExpense() {
    assertEquals(List.of(4, 3, 2, 1), ids(Routing.sortByExpense(sampleRoutes())));
  }

  @Test
  void sortsRoutesByDistanceStably() {
    assertEquals(List.of(2, 4, 3, 1), ids(Routing.sortByDistance(sampleRoutes())));
  }

  @Test
  void sortsRoutesByTime() {
    assertEquals(List.of(2, 3, 1, 4), ids(Routing.sortByTime(sampleRoutes())));
  }

  @Test
  void sortingDoesNotMutateInput() {
    List<Route> routes = sampleRoutes();

    Routing.sortByExpense(routes);

    assertEquals(List.of(1, 2, 3, 4), ids(routes));
  }

  @Test
  void cumulativeTimeSumsEveryRoute() {
    assertEquals(Duration.ofMinutes(110), Routing.cumulativeTime(sampleRoutes()));
  }

  @Test
  void cumulativeTimeOfEmptyChainIsZero() {
    assertEquals(Duration.ZERO, Routing.cumulativeTime(List.of()));
  }

  @Test
  void partitionsZonesByState() {
    List<Zone> zones = List.of(
        zone(1, "SP", "Campinas"),
        zone(2, "SP", "Santos"),
        zone(3, "RJ", "Rio de Janeiro"),
        zone(4, "MG", "Belo Horizonte"),
        zone(5, "SP", "Osasco"));

    HashMap<String, List<Zone>> partitions = Routing.partitionByState(zones);

    assertEquals(3, partitions.keys().size());
    assertEquals(3, partitions.get("SP").size());
    assertEquals(1, partitions.get("RJ").size());
    assertEquals(1, partitions.get("MG").size());
  }

  @Test
  void partitionsHandleEmptyInput() {
    assertTrue(Routing.partitionByState(List.of()).keys().isEmpty());
  }

  @Test
  void cumulativeCostAndDistanceSumEveryRoute() {
    assertEquals(1800L, Routing.cumulativeCost(sampleRoutes()));
    assertEquals(700d, Routing.cumulativeDistance(sampleRoutes()), 0.0001d);
    assertEquals(0L, Routing.cumulativeCost(List.of()));
    assertEquals(0d, Routing.cumulativeDistance(List.of()), 0.0001d);
  }

  @Test
  void weightedPathPrefersCheapestNotFewestStops() {
    List<Route> routes = List.of(
        route(1, 1, 2, 100, Duration.ofMinutes(10), 100),
        route(2, 1, 3, 100, Duration.ofMinutes(10), 500),
        route(3, 2, 3, 100, Duration.ofMinutes(10), 100),
        route(4, 3, 4, 100, Duration.ofMinutes(10), 100));

    Graph<Integer> unweighted = Routing.network(routes);
    WeightedGraph<Integer> byCost = Routing.weightedNetwork(routes, Routing.RouteWeight.COST);

    assertEquals(List.of(1, 3, 4), Routing.shortestPath(unweighted, 1, 4));

    WeightedGraph.Path<Integer> path = Routing.weightedShortestPath(byCost, 1, 4).orElseThrow();

    assertEquals(List.of(1, 2, 3, 4), path.vertices());
    assertEquals(300d, path.weight(), 0.0001d);
  }

  @Test
  void weightedPathCanWeightByTimeAndDistance() {
    List<Route> routes = List.of(
        route(1, 1, 2, 500, Duration.ofMinutes(5), 900),
        route(2, 2, 3, 100, Duration.ofMinutes(5), 900),
        route(3, 1, 3, 100, Duration.ofMinutes(30), 100));

    WeightedGraph<Integer> byTime = Routing.weightedNetwork(routes, Routing.RouteWeight.TIME);
    WeightedGraph<Integer> byDistance = Routing.weightedNetwork(routes,
        Routing.RouteWeight.DISTANCE);

    assertEquals(600d, Routing.weightedShortestPath(byTime, 1, 3).orElseThrow().weight(),
        0.0001d);
    assertEquals(100d, Routing.weightedShortestPath(byDistance, 1, 3).orElseThrow().weight(),
        0.0001d);
  }

  @Test
  void partitionDeliveriesSplitsAndBalancesAcrossVehicles() {
    List<Package> packages = List.of(
        pkg(1, "WB-1", 1, 5f, 100, Priority.NORMAL, DeliveryStatus.CREATED),
        pkg(2, "WB-2", 1, 1f, 100, Priority.NORMAL, DeliveryStatus.CREATED),
        pkg(3, "WB-3", 1, 1f, 100, Priority.NORMAL, DeliveryStatus.CREATED),
        pkg(4, "WB-4", 1, 1f, 100, Priority.NORMAL, DeliveryStatus.CREATED));

    List<Vehicle> vehicles = List.of(
        vehicle(1, "AAA-0001", 5_000f),
        vehicle(2, "BBB-0002", 1_000f));

    List<Routing.Assignment> assignments = Routing.partitionDeliveries(packages, vehicles);

    assertEquals(2, assignments.size());
    assertEquals(1, assignments.get(0).vehicle().id());
    assertEquals(6.0f, assignments.get(0).totalWeight(), 0.0001f);

    int assigned = assignments.stream().mapToInt(a -> a.packages().size()).sum();
    assertEquals(packages.size(), assigned);
  }

  @Test
  void planRespectsCapacityAndReportsUnassigned() {
    List<Package> packages = List.of(
        pkg(1, "WB-HEAVY", 1, 5_000f, 100, Priority.NORMAL, DeliveryStatus.CREATED),
        pkg(2, "WB-LIGHT", 1, 100f, 100, Priority.NORMAL, DeliveryStatus.CREATED));

    List<Vehicle> vehicles = List.of(vehicle(1, "AAA-0001", 1_000f));

    Routing.Plan plan = Routing.plan(packages, vehicles);

    assertEquals(1, plan.assignments().size());
    assertEquals(100f, plan.assignments().get(0).totalWeight(), 0.001f);
    assertFalse(plan.assignments().get(0).overCapacity());
    assertEquals(1, plan.unassigned().size());
    assertEquals("WB-HEAVY", plan.unassigned().get(0).idGuia());
  }

  @Test
  void planOnlyConsidersDispatchablePackages() {
    List<Package> packages = List.of(
        pkg(1, "WB-DONE", 1, 1f, 100, Priority.NORMAL, DeliveryStatus.DELIVERED),
        pkg(2, "WB-LIVE", 1, 1f, 100, Priority.NORMAL, DeliveryStatus.CREATED));

    Routing.Plan plan = Routing.plan(packages, List.of(vehicle(1, "AAA-0001", 100f)));

    assertEquals(1, plan.assignedCount());
    assertTrue(plan.isFullyAssigned());
  }

  @Test
  void partitionDeliveriesHandlesEmptyInputs() {
    assertTrue(Routing.partitionDeliveries(List.of(), List.of(vehicle(1, "AAA-0001", 100f)))
        .isEmpty());
    assertTrue(Routing.partitionDeliveries(
        List.of(pkg(1, "WB-1", Priority.NORMAL)), List.of()).isEmpty());
  }
}
