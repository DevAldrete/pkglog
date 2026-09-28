package com.dev.modules;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.dev.domain.DeliveryStatus;
import com.dev.domain.Package;
import com.dev.domain.Route;
import com.dev.domain.Vehicle;
import com.dev.domain.Zone;
import com.dev.ds.Graph;
import com.dev.ds.HashMap;
import com.dev.ds.Sorting;
import com.dev.ds.WeightedGraph;

/**
 * Operations over routes, zones and the delivery fleet: the transportation
 * network, route ordering, cumulative travel analysis, weighted shortest paths
 * and geographic partitioning.
 */
public final class Routing {

  private Routing() {
  }

  /** Metric used to weight the edges of the transportation network. */
  public enum RouteWeight {
    TIME,
    COST,
    DISTANCE
  }

  /** A load assigned to a vehicle by the partitioning algorithm. */
  public record Assignment(Vehicle vehicle, List<Package> packages, float totalWeight) {

    /** True when the assigned load exceeds the vehicle capacity. */
    public boolean overCapacity() {
      return totalWeight > vehicle.capacityKg();
    }
  }

  /**
   * Result of planning a batch against the fleet: the loads that fit and the
   * packages that no vehicle can carry.
   */
  public record Plan(List<Assignment> assignments, List<Package> unassigned) {

    public int assignedCount() {
      int total = 0;

      for (Assignment assignment : assignments) {
        total += assignment.packages().size();
      }

      return total;
    }

    public boolean isFullyAssigned() {
      return unassigned.isEmpty();
    }
  }

  /** Sorts routes from cheapest to most expensive, in cents. */
  public static final Comparator<Route> BY_EXPENSE = Comparator.comparingLong(Route::expenseInCents);

  /** Sorts routes from shortest to longest, in meters. */
  public static final Comparator<Route> BY_DISTANCE = Comparator
      .comparingDouble(Route::distanceMeters);

  /** Sorts routes from fastest to slowest, by estimated time. */
  public static final Comparator<Route> BY_TIME = Comparator.comparing(Route::estimatedTime);

  // ---------------------------------------------------------------------------
  // Graphs: cities and transportation routes
  // ---------------------------------------------------------------------------

  /** Builds an unweighted directed graph of zone ids connected by the routes. */
  public static Graph<Integer> network(List<Route> routes) {
    Objects.requireNonNull(routes, "routes must not be null");

    // Nodos = zonas, aristas = rutas dirigidas (sin peso).
    Graph<Integer> graph = new Graph<>(Math.max(routes.size() * 2, 2));

    for (Route route : routes) {
      graph.addEdge(route.originId(), route.destinyId());
    }

    return graph;
  }

  /** Finds the fewest-stops path between two zones, or empty when unreachable. */
  public static List<Integer> shortestPath(Graph<Integer> network, int originId, int destinyId) {
    Objects.requireNonNull(network, "network must not be null");

    return network.shortestPath(originId, destinyId);
  }

  /** Builds a directed, weighted graph using the chosen metric. */
  public static WeightedGraph<Integer> weightedNetwork(List<Route> routes, RouteWeight metric) {
    Objects.requireNonNull(routes, "routes must not be null");
    Objects.requireNonNull(metric, "metric must not be null");

    // Misma red, pero con peso (tiempo, costo o distancia) para Dijkstra.
    WeightedGraph<Integer> graph = new WeightedGraph<>(Math.max(routes.size() * 2, 2));

    for (Route route : routes) {
      graph.addEdge(route.originId(), route.destinyId(), weightOf(route, metric));
    }

    return graph;
  }

  /** Finds the cheapest-weight path between two zones, or empty when unreachable. */
  public static Optional<WeightedGraph.Path<Integer>> weightedShortestPath(
      WeightedGraph<Integer> network, int originId, int destinyId) {
    Objects.requireNonNull(network, "network must not be null");

    return network.shortestPath(originId, destinyId);
  }

  private static double weightOf(Route route, RouteWeight metric) {
    return switch (metric) {
      case TIME -> route.estimatedTime().toSeconds();
      case COST -> route.expenseInCents();
      case DISTANCE -> route.distanceMeters();
    };
  }

  // ---------------------------------------------------------------------------
  // Sorting: routes by distance, time and cost
  // ---------------------------------------------------------------------------

  /** Returns a new list of routes ordered by the given comparator. */
  public static List<Route> sortRoutes(List<Route> routes, Comparator<Route> comparator) {
    return Sorting.mergeSort(routes, comparator);
  }

  public static List<Route> sortByExpense(List<Route> routes) {
    return Sorting.mergeSort(routes, BY_EXPENSE);
  }

  public static List<Route> sortByDistance(List<Route> routes) {
    return Sorting.mergeSort(routes, BY_DISTANCE);
  }

  public static List<Route> sortByTime(List<Route> routes) {
    return Sorting.mergeSort(routes, BY_TIME);
  }

  // ---------------------------------------------------------------------------
  // Recursion: cumulative time, cost and distance on a route chain
  // ---------------------------------------------------------------------------

  /** Sums the estimated time of every route in the chain through recursion. */
  public static Duration cumulativeTime(List<Route> routes) {
    Objects.requireNonNull(routes, "routes must not be null");

    return cumulativeTime(routes, routes.size());
  }

  // Recursión: último tramo + acumulado de los anteriores.
  private static Duration cumulativeTime(List<Route> routes, int count) {
    if (count == 0) {
      return Duration.ZERO;
    }

    return routes.get(count - 1).estimatedTime().plus(cumulativeTime(routes, count - 1));
  }

  /** Sums the expense of every route in the chain, in cents, through recursion. */
  public static long cumulativeCost(List<Route> routes) {
    Objects.requireNonNull(routes, "routes must not be null");

    return cumulativeCost(routes, routes.size());
  }

  // Recursión: costo del último tramo + acumulado previo.
  private static long cumulativeCost(List<Route> routes, int count) {
    if (count == 0) {
      return 0L;
    }

    return routes.get(count - 1).expenseInCents() + cumulativeCost(routes, count - 1);
  }

  /** Sums the distance of every route in the chain, in meters, through recursion. */
  public static double cumulativeDistance(List<Route> routes) {
    Objects.requireNonNull(routes, "routes must not be null");

    return cumulativeDistance(routes, routes.size());
  }

  // Recursión: distancia del último tramo + acumulado previo.
  private static double cumulativeDistance(List<Route> routes, int count) {
    if (count == 0) {
      return 0d;
    }

    return routes.get(count - 1).distanceMeters() + cumulativeDistance(routes, count - 1);
  }

  // ---------------------------------------------------------------------------
  // Divide and conquer: geographic partitioning
  // ---------------------------------------------------------------------------

  /** Groups zones by state by recursively splitting and merging the list. */
  public static HashMap<String, List<Zone>> partitionByState(List<Zone> zones) {
    Objects.requireNonNull(zones, "zones must not be null");

    return partitionByState(zones, 0, zones.size());
  }

  // Divide y vencerás: parte la lista a la mitad y combina los grupos por estado.
  private static HashMap<String, List<Zone>> partitionByState(List<Zone> zones, int from, int to) {
    if (to - from <= 1) {
      HashMap<String, List<Zone>> single = new HashMap<>(2);

      if (to - from == 1) {
        single.put(zones.get(from).state(), new ArrayList<>(List.of(zones.get(from))));
      }

      return single;
    }

    int middle = from + (to - from) / 2;

    HashMap<String, List<Zone>> left = partitionByState(zones, from, middle);
    HashMap<String, List<Zone>> right = partitionByState(zones, middle, to);

    return merge(left, right);
  }

  private static HashMap<String, List<Zone>> merge(HashMap<String, List<Zone>> left,
      HashMap<String, List<Zone>> right) {
    HashMap<String, List<Zone>> merged = new HashMap<>(Math.max((left.size() + right.size()) * 2, 2));

    for (String state : left.keys()) {
      merged.put(state, new ArrayList<>(left.get(state)));
    }

    for (String state : right.keys()) {
      List<Zone> existing = merged.get(state);

      if (existing == null) {
        merged.put(state, new ArrayList<>(right.get(state)));
      } else {
        existing.addAll(right.get(state));
      }
    }

    return merged;
  }

  /**
   * Splits the batch into at most one segment per vehicle and pairs the
   * heaviest segment with the largest vehicle. Returns just the loads; see
   * {@link #plan(List, List)} to learn which packages could not be carried.
   */
  public static List<Assignment> partitionDeliveries(List<Package> packages, List<Vehicle> vehicles) {
    return plan(packages, vehicles).assignments();
  }

  /**
   * Capacity-aware fleet plan. Dispatchable packages are split between the
   * vehicles with the same divide-and-conquer balancing as
   * {@link #partitionDeliveries(List, List)}, then any load that overflows its
   * vehicle is trimmed and the removed packages are re-tried against the
   * remaining capacity. Packages that fit nowhere are returned as unassigned
   * instead of silently overloading a truck.
   */
  public static Plan plan(List<Package> packages, List<Vehicle> vehicles) {
    Objects.requireNonNull(packages, "packages must not be null");
    Objects.requireNonNull(vehicles, "vehicles must not be null");

    List<Package> batch = new ArrayList<>();

    for (Package pkg : packages) {
      if (isDispatchable(pkg.status())) {
        batch.add(pkg);
      }
    }

    if (batch.isEmpty() || vehicles.isEmpty()) {
      return new Plan(List.of(), List.of());
    }

    List<List<Package>> segments = new ArrayList<>();

    for (List<Package> segment : divide(batch, vehicles.size())) {
      if (!segment.isEmpty()) {
        segments.add(segment);
      }
    }

    List<Vehicle> orderedVehicles = Sorting.mergeSort(vehicles,
        Comparator.comparingDouble(Vehicle::capacityKg).reversed());
    List<List<Package>> orderedSegments = Sorting.mergeSort(segments,
        Comparator.comparingDouble(Routing::segmentWeight).reversed());

    // One load slot per vehicle, aligned with orderedVehicles.
    List<List<Package>> loads = new ArrayList<>(orderedVehicles.size());
    for (int i = 0; i < orderedVehicles.size(); i++) {
      loads.add(new ArrayList<>());
    }

    int paired = Math.min(orderedVehicles.size(), orderedSegments.size());
    for (int i = 0; i < paired; i++) {
      loads.set(i, new ArrayList<>(orderedSegments.get(i)));
    }

    // Trim each load to its vehicle capacity, pooling whatever does not fit.
    List<Package> overflow = new ArrayList<>();

    for (int i = 0; i < loads.size(); i++) {
      List<Package> kept = new ArrayList<>();
      float capacity = orderedVehicles.get(i).capacityKg();
      float sum = 0f;

      for (Package pkg : Sorting.mergeSort(loads.get(i),
          Comparator.comparingDouble(Package::weight).reversed())) {
        if (sum + pkg.weight() <= capacity) {
          kept.add(pkg);
          sum += pkg.weight();
        } else {
          overflow.add(pkg);
        }
      }

      loads.set(i, kept);
    }

    // Best-fit-decreasing of the pool against the remaining capacity.
    float[] remaining = new float[orderedVehicles.size()];
    for (int i = 0; i < orderedVehicles.size(); i++) {
      remaining[i] = orderedVehicles.get(i).capacityKg() - (float) segmentWeight(loads.get(i));
    }

    List<Package> unassigned = new ArrayList<>();

    for (Package pkg : Sorting.mergeSort(overflow,
        Comparator.comparingDouble(Package::weight).reversed())) {
      int best = -1;
      float bestRemaining = Float.MAX_VALUE;

      for (int i = 0; i < orderedVehicles.size(); i++) {
        if (pkg.weight() <= remaining[i] && remaining[i] < bestRemaining) {
          best = i;
          bestRemaining = remaining[i];
        }
      }

      if (best < 0) {
        unassigned.add(pkg);
      } else {
        loads.get(best).add(pkg);
        remaining[best] -= pkg.weight();
      }
    }

    List<Assignment> assignments = new ArrayList<>();

    for (int i = 0; i < orderedVehicles.size(); i++) {
      if (!loads.get(i).isEmpty()) {
        assignments.add(new Assignment(orderedVehicles.get(i), loads.get(i),
            (float) segmentWeight(loads.get(i))));
      }
    }

    return new Plan(assignments, unassigned);
  }

  private static boolean isDispatchable(DeliveryStatus status) {
    return status == DeliveryStatus.CREATED || status == DeliveryStatus.DISPATCHED;
  }

  // Divide el lote en tantos segmentos como partes solicitadas.
  private static List<List<Package>> divide(List<Package> packages, int parts) {
    if (parts <= 1 || packages.size() <= 1) {
      List<List<Package>> single = new ArrayList<>();
      single.add(new ArrayList<>(packages));
      return single;
    }

    int middle = packages.size() / 2;
    int leftParts = parts / 2;

    List<List<Package>> segments = new ArrayList<>();
    segments.addAll(divide(packages.subList(0, middle), leftParts));
    segments.addAll(divide(packages.subList(middle, packages.size()), parts - leftParts));

    return segments;
  }

  private static double segmentWeight(List<Package> packages) {
    double total = 0d;

    for (Package pkg : packages) {
      total += pkg.weight();
    }

    return total;
  }
}
