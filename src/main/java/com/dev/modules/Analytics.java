package com.dev.modules;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

import com.dev.domain.DeliveryStatus;
import com.dev.domain.Package;
import com.dev.domain.Route;
import com.dev.ds.HashMap;

/**
 * Aggregate metrics over routes and packages: costs, distances and revenue.
 * All functions are stateless and never mutate their inputs.
 */
public final class Analytics {

  private Analytics() {
  }

  // ---------------------------------------------------------------------------
  // Rutas: costo, distancia y extremos.
  // ---------------------------------------------------------------------------

  /** Sums the expense of every route, in cents. */
  public static long totalExpense(List<Route> routes) {
    Objects.requireNonNull(routes, "routes must not be null");

    long total = 0L;

    for (Route route : routes) {
      total += route.expenseInCents();
    }

    return total;
  }

  /** Averages the route expense, in cents, or empty when there are no routes. */
  public static OptionalDouble averageExpense(List<Route> routes) {
    Objects.requireNonNull(routes, "routes must not be null");

    if (routes.isEmpty()) {
      return OptionalDouble.empty();
    }

    return OptionalDouble.of((double) totalExpense(routes) / routes.size());
  }

  /** Returns the route with the highest expense, or empty when there are none. */
  public static Optional<Route> mostExpensiveRoute(List<Route> routes) {
    Objects.requireNonNull(routes, "routes must not be null");

    Route mostExpensive = null;

    for (Route route : routes) {
      if (mostExpensive == null || route.expenseInCents() > mostExpensive.expenseInCents()) {
        mostExpensive = route;
      }
    }

    return Optional.ofNullable(mostExpensive);
  }

  /** Returns the route with the lowest expense, or empty when there are none. */
  public static Optional<Route> cheapestRoute(List<Route> routes) {
    Objects.requireNonNull(routes, "routes must not be null");

    Route cheapest = null;

    for (Route route : routes) {
      if (cheapest == null || route.expenseInCents() < cheapest.expenseInCents()) {
        cheapest = route;
      }
    }

    return Optional.ofNullable(cheapest);
  }

  /** Sums the distance of every route, in meters. */
  public static double totalDistance(List<Route> routes) {
    Objects.requireNonNull(routes, "routes must not be null");

    double total = 0d;

    for (Route route : routes) {
      total += route.distanceMeters();
    }

    return total;
  }

  // ---------------------------------------------------------------------------
  // Paquetes: ingresos, promedios y conteos.
  // ---------------------------------------------------------------------------

  /**
   * Sums the price of every billable package, in cents. Canceled shipments are
   * excluded so the figure matches actual expected revenue.
   */
  public static long revenueInCents(List<Package> packages) {
    Objects.requireNonNull(packages, "packages must not be null");

    long total = 0L;

    for (Package pkg : packages) {
      if (isBillable(pkg)) {
        total += pkg.priceInCents();
      }
    }

    return total;
  }

  /** Sums the price of delivered packages only, in cents. */
  public static long deliveredRevenueInCents(List<Package> packages) {
    Objects.requireNonNull(packages, "packages must not be null");

    long total = 0L;

    for (Package pkg : packages) {
      if (pkg.status() == DeliveryStatus.DELIVERED) {
        total += pkg.priceInCents();
      }
    }

    return total;
  }

  /** Sums the weight of billable (non-canceled) packages, in kilograms. */
  public static float totalWeight(List<Package> packages) {
    Objects.requireNonNull(packages, "packages must not be null");

    float total = 0f;

    for (Package pkg : packages) {
      if (isBillable(pkg)) {
        total += pkg.weight();
      }
    }

    return total;
  }

  private static boolean isBillable(Package pkg) {
    return pkg.status() != DeliveryStatus.CANCELED;
  }

  /** Averages the package price, in cents, or empty when there are no packages. */
  public static OptionalDouble averagePriceInCents(List<Package> packages) {
    Objects.requireNonNull(packages, "packages must not be null");

    if (packages.isEmpty()) {
      return OptionalDouble.empty();
    }

    return OptionalDouble.of((double) revenueInCents(packages) / packages.size());
  }

  /** Returns the heaviest package, or empty when there are none. */
  public static Optional<Package> heaviestPackage(List<Package> packages) {
    Objects.requireNonNull(packages, "packages must not be null");

    Package heaviest = null;

    for (Package pkg : packages) {
      if (heaviest == null || pkg.weight() > heaviest.weight()) {
        heaviest = pkg;
      }
    }

    return Optional.ofNullable(heaviest);
  }

  /** Counts packages per delivery status. */
  public static HashMap<DeliveryStatus, Integer> countByStatus(List<Package> packages) {
    Objects.requireNonNull(packages, "packages must not be null");

    HashMap<DeliveryStatus, Integer> counts = new HashMap<>(Math.max(packages.size() * 2, 2));

    for (Package pkg : packages) {
      Integer current = counts.get(pkg.status());
      counts.put(pkg.status(), current == null ? 1 : current + 1);
    }

    return counts;
  }

  /** Sums the revenue per route id, in cents. */
  public static HashMap<Integer, Long> revenueByRoute(List<Package> packages) {
    Objects.requireNonNull(packages, "packages must not be null");

    HashMap<Integer, Long> revenue = new HashMap<>(Math.max(packages.size() * 2, 2));

    for (Package pkg : packages) {
      if (!isBillable(pkg)) {
        continue;
      }

      Long current = revenue.get(pkg.routeId());
      revenue.put(pkg.routeId(), current == null ? pkg.priceInCents() : current + pkg.priceInCents());
    }

    return revenue;
  }
}
