package com.dev.modules;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.dev.domain.DeliveryStatus;
import com.dev.domain.Package;
import com.dev.domain.Priority;
import com.dev.ds.BucketQueue;
import com.dev.ds.HashMap;
import com.dev.ds.PriorityQueue;
import com.dev.ds.Sorting;

/**
 * Operations over packages: urgency ordering, dispatch, waybill lookup,
 * tracking, sorting and simple aggregates. All functions are stateless and
 * return new data, keeping packages immutable.
 */
public final class Deliveries {

  private Deliveries() {
  }

  /** Outcome of dispatching the next package: the chosen one plus the new list. */
  public record Dispatch(Package dispatched, List<Package> packages) {
  }

  // Urgencia: mayor nivel primero; en empate, menor id.
  /** Orders packages by urgency (highest level first), breaking ties by id. */
  public static final Comparator<Package> URGENCY = Comparator
      .comparingInt((Package p) -> -p.priority().level())
      .thenComparingInt(Package::id);

  // Reportes: de más barato a más caro.
  /** Orders packages from cheapest to most expensive. */
  public static final Comparator<Package> BY_COST = Comparator.comparingLong(Package::priceInCents);

  // Reportes: de fecha límite más próxima a más lejana.
  /** Orders packages from earliest to latest deadline. */
  public static final Comparator<Package> BY_DEADLINE = Comparator.comparing(Package::deadline);

  // ---------------------------------------------------------------------------
  // Priority dispatch
  // ---------------------------------------------------------------------------

  /** Loads the given packages into a min-priority queue ordered by urgency. */
  public static PriorityQueue<Package> toPriorityQueue(List<Package> packages) {
    Objects.requireNonNull(packages, "packages must not be null");

    PriorityQueue<Package> queue = new PriorityQueue<>(URGENCY);

    for (Package pkg : packages) {
      queue.enqueue(pkg);
    }

    return queue;
  }

  /** Returns the most urgent packages, at most {@code limit} of them. */
  public static List<Package> urgent(List<Package> packages, int limit) {
    if (limit < 0) {
      throw new IllegalArgumentException("limit must be non negative");
    }

    PriorityQueue<Package> queue = toPriorityQueue(packages);

    List<Package> result = new ArrayList<>(Math.min(limit, queue.size()));

    while (!queue.isEmpty() && result.size() < limit) {
      result.add(queue.dequeue());
    }

    return result;
  }

  /**
   * Extracts the most urgent dispatchable package in O(1) and moves it to
   * {@code IN_TRANSIT}. Equal priorities are served first-in first-out.
   * Returns empty when nothing is left to dispatch.
   */
  public static Optional<Dispatch> dispatchNext(List<Package> packages) {
    Objects.requireNonNull(packages, "packages must not be null");

    List<Package> candidates = new ArrayList<>();

    for (Package pkg : packages) {
      if (isDispatchable(pkg.status())) {
        candidates.add(pkg);
      }
    }

    if (candidates.isEmpty()) {
      return Optional.empty();
    }

    // Deadline-aware: enqueue in earliest-deadline order so the per-priority
    // FIFO bucket serves the most urgent due date first within each level.
    List<Package> byDeadline = Sorting.mergeSort(candidates, BY_DEADLINE);

    BucketQueue<Package> queue = new BucketQueue<>(Priority.CRITICAL.level());

    for (Package pkg : byDeadline) {
      queue.enqueue(pkg, pkg.priority().level());
    }

    Package next = queue.dequeueMax();
    List<Package> updated = updateStatus(packages, next.idGuia(), DeliveryStatus.IN_TRANSIT);

    return Optional.of(new Dispatch(next.withStatus(DeliveryStatus.IN_TRANSIT), updated));
  }

  private static boolean isDispatchable(DeliveryStatus status) {
    return status == DeliveryStatus.CREATED || status == DeliveryStatus.DISPATCHED;
  }

  private static boolean isActive(DeliveryStatus status) {
    return status == DeliveryStatus.CREATED
        || status == DeliveryStatus.DISPATCHED
        || status == DeliveryStatus.IN_TRANSIT;
  }

  /** Active packages whose deadline has already passed. */
  public static List<Package> overdue(List<Package> packages, LocalDateTime now) {
    Objects.requireNonNull(packages, "packages must not be null");
    Objects.requireNonNull(now, "now must not be null");

    List<Package> result = new ArrayList<>();

    for (Package pkg : packages) {
      if (isActive(pkg.status()) && pkg.deadline().isBefore(now)) {
        result.add(pkg);
      }
    }

    return result;
  }

  /** Number of active packages past their deadline. */
  public static int overdueCount(List<Package> packages, LocalDateTime now) {
    return overdue(packages, now).size();
  }

  // ---------------------------------------------------------------------------
  // Waybill index and tracking
  // ---------------------------------------------------------------------------

  /** Builds a waybill-to-package index for constant time lookups. */
  public static HashMap<String, Package> indexByWaybill(List<Package> packages) {
    Objects.requireNonNull(packages, "packages must not be null");

    // Índice guía -> paquete: rastreo en tiempo promedio O(1).
    HashMap<String, Package> index = new HashMap<>(Math.max(packages.size() * 2, 2));

    for (Package pkg : packages) {
      index.put(pkg.idGuia(), pkg);
    }

    return index;
  }

  /** Looks up a package by waybill, returning empty when it is not tracked. */
  public static Optional<Package> findByWaybill(HashMap<String, Package> index, String waybill) {
    Objects.requireNonNull(index, "index must not be null");
    Objects.requireNonNull(waybill, "waybill must not be null");

    return Optional.ofNullable(index.get(waybill));
  }

  /** Returns a new list with the matching waybill moved to the given status. */
  public static List<Package> updateStatus(List<Package> packages, String waybill,
      DeliveryStatus status) {
    Objects.requireNonNull(packages, "packages must not be null");
    Objects.requireNonNull(waybill, "waybill must not be null");
    Objects.requireNonNull(status, "status must not be null");

    List<Package> updated = new ArrayList<>(packages.size());

    for (Package pkg : packages) {
      updated.add(pkg.idGuia().equals(waybill) ? pkg.withStatus(status) : pkg);
    }

    return updated;
  }

  // ---------------------------------------------------------------------------
  // Filters
  // ---------------------------------------------------------------------------

  /** Returns the packages carrying the given status. */
  public static List<Package> filterByStatus(List<Package> packages, DeliveryStatus status) {
    Objects.requireNonNull(packages, "packages must not be null");
    Objects.requireNonNull(status, "status must not be null");

    List<Package> result = new ArrayList<>();

    for (Package pkg : packages) {
      if (pkg.status() == status) {
        result.add(pkg);
      }
    }

    return result;
  }

  /** Returns the packages carrying the given priority. */
  public static List<Package> filterByPriority(List<Package> packages, Priority priority) {
    Objects.requireNonNull(packages, "packages must not be null");
    Objects.requireNonNull(priority, "priority must not be null");

    List<Package> result = new ArrayList<>();

    for (Package pkg : packages) {
      if (pkg.priority() == priority) {
        result.add(pkg);
      }
    }

    return result;
  }

  // ---------------------------------------------------------------------------
  // Sorting reports
  // ---------------------------------------------------------------------------

  /** Sorts packages with the given comparator using stable merge sort. */
  public static List<Package> sortPackages(List<Package> packages, Comparator<Package> comparator) {
    return Sorting.mergeSort(packages, comparator);
  }

  /** Sorts packages with the given comparator using quick sort. */
  public static List<Package> sortPackagesQuick(List<Package> packages,
      Comparator<Package> comparator) {
    return Sorting.quickSort(packages, comparator);
  }

  public static List<Package> sortByCost(List<Package> packages) {
    return Sorting.mergeSort(packages, BY_COST);
  }

  public static List<Package> sortByDeadline(List<Package> packages) {
    return Sorting.mergeSort(packages, BY_DEADLINE);
  }

  public static List<Package> sortByPriority(List<Package> packages) {
    return Sorting.mergeSort(packages, URGENCY);
  }

  // ---------------------------------------------------------------------------
  // Aggregates
  // ---------------------------------------------------------------------------

  /** Sums the weight of every package. */
  public static float totalWeight(List<Package> packages) {
    Objects.requireNonNull(packages, "packages must not be null");

    float total = 0f;

    for (Package pkg : packages) {
      total += pkg.weight();
    }

    return total;
  }

  /** Sums the price of every package, in cents. */
  public static long totalPriceInCents(List<Package> packages) {
    Objects.requireNonNull(packages, "packages must not be null");

    long total = 0L;

    for (Package pkg : packages) {
      total += pkg.priceInCents();
    }

    return total;
  }

  /** Counts the packages with the given priority. */
  public static int countByPriority(List<Package> packages, Priority priority) {
    Objects.requireNonNull(packages, "packages must not be null");
    Objects.requireNonNull(priority, "priority must not be null");

    int count = 0;

    for (Package pkg : packages) {
      if (pkg.priority() == priority) {
        count++;
      }
    }

    return count;
  }
}
