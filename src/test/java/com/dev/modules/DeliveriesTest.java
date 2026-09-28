package com.dev.modules;

import static com.dev.pkglog.Fixtures.pkg;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dev.domain.DeliveryStatus;
import com.dev.domain.Package;
import com.dev.domain.Priority;
import com.dev.ds.HashMap;
import com.dev.ds.PriorityQueue;

class DeliveriesTest {

  private static List<Package> sample() {
    return List.of(
        pkg(3, "WB-URGENT-3", Priority.URGENT),
        pkg(1, "WB-IMPORTANT-1", Priority.IMPORTANT),
        pkg(2, "WB-URGENT-2", Priority.URGENT),
        pkg(0, "WB-NORMAL-0", Priority.NORMAL));
  }

  @Test
  void urgencyOrdersByPriorityThenId() {
    List<Package> sorted = new ArrayList<>(sample());
    sorted.sort(Deliveries.URGENCY);

    assertEquals(
        List.of("WB-URGENT-2", "WB-URGENT-3", "WB-IMPORTANT-1", "WB-NORMAL-0"),
        sorted.stream().map(Package::waybill).toList());
  }

  @Test
  void toPriorityQueueDequeuesInUrgencyOrder() {
    PriorityQueue<Package> queue = Deliveries.toPriorityQueue(sample());

    assertEquals(4, queue.size());
    assertEquals("WB-URGENT-2", queue.dequeue().waybill());
    assertEquals("WB-URGENT-3", queue.dequeue().waybill());
    assertEquals("WB-IMPORTANT-1", queue.dequeue().waybill());
    assertEquals("WB-NORMAL-0", queue.dequeue().waybill());
  }

  @Test
  void urgentReturnsMostUrgentFirst() {
    List<Package> urgent = Deliveries.urgent(sample(), 2);

    assertEquals(
        List.of("WB-URGENT-2", "WB-URGENT-3"),
        urgent.stream().map(Package::waybill).toList());
  }

  @Test
  void urgentClampsToAvailablePackages() {
    assertEquals(4, Deliveries.urgent(sample(), 10).size());
    assertTrue(Deliveries.urgent(sample(), 0).isEmpty());
    assertTrue(Deliveries.urgent(List.of(), 3).isEmpty());
  }

  @Test
  void urgentRejectsNegativeLimit() {
    assertThrows(IllegalArgumentException.class, () -> Deliveries.urgent(sample(), -1));
  }

  @Test
  void indexByWaybillStoresEveryPackage() {
    HashMap<String, Package> index = Deliveries.indexByWaybill(sample());

    assertEquals(4, index.size());
    assertTrue(index.containsKey("WB-IMPORTANT-1"));
  }

  @Test
  void findByWaybillReturnsTrackedPackage() {
    HashMap<String, Package> index = Deliveries.indexByWaybill(sample());

    Package found = Deliveries.findByWaybill(index, "WB-IMPORTANT-1").orElseThrow();

    assertEquals(1, found.id());
    assertEquals(Priority.IMPORTANT, found.priority());
  }

  @Test
  void findByWaybillReturnsEmptyWhenUnknown() {
    HashMap<String, Package> index = Deliveries.indexByWaybill(sample());

    assertTrue(Deliveries.findByWaybill(index, "WB-MISSING").isEmpty());
  }

  @Test
  void updateStatusReplacesOnlyMatchingWaybill() {
    List<Package> updated = Deliveries.updateStatus(sample(), "WB-IMPORTANT-1", DeliveryStatus.DELIVERED);

    assertNotSame(sample(), updated);
    assertEquals(DeliveryStatus.DELIVERED,
        Deliveries.findByWaybill(Deliveries.indexByWaybill(updated), "WB-IMPORTANT-1").orElseThrow()
            .status());
    assertEquals(1, Deliveries.filterByStatus(updated, DeliveryStatus.DELIVERED).size());
    assertEquals(3, Deliveries.filterByStatus(updated, DeliveryStatus.CREATED).size());
  }

  @Test
  void updateStatusDoesNotMutateInput() {
    List<Package> packages = sample();

    Deliveries.updateStatus(packages, "WB-IMPORTANT-1", DeliveryStatus.DELIVERED);

    assertEquals(DeliveryStatus.CREATED, packages.get(1).status());
  }

  @Test
  void totalsWeightAndPrice() {
    List<Package> packages = List.of(
        pkg(1, "WB-1", 1, 2.5f, 500, Priority.NORMAL, DeliveryStatus.CREATED),
        pkg(2, "WB-2", 1, 1.5f, 1500, Priority.IMPORTANT, DeliveryStatus.CREATED));

    assertEquals(4.0f, Deliveries.totalWeight(packages), 0.0001f);
    assertEquals(2000L, Deliveries.totalPriceInCents(packages));
  }

  @Test
  void filtersPackagesByPriority() {
    assertEquals(2, Deliveries.filterByPriority(sample(), Priority.URGENT).size());
    assertEquals(1, Deliveries.filterByPriority(sample(), Priority.IMPORTANT).size());
    assertTrue(Deliveries.filterByPriority(sample(), Priority.MODERATE).isEmpty());
  }

  @Test
  void countsPackagesByPriority() {
    assertEquals(2, Deliveries.countByPriority(sample(), Priority.URGENT));
    assertEquals(1, Deliveries.countByPriority(sample(), Priority.NORMAL));
    assertEquals(0, Deliveries.countByPriority(sample(), Priority.MODERATE));
  }

  @Test
  void dispatchNextPicksMostUrgentAndMarksInTransit() {
    Deliveries.Dispatch dispatch = Deliveries.dispatchNext(sample()).orElseThrow();

    assertEquals("WB-URGENT-3", dispatch.dispatched().idGuia());
    assertEquals(DeliveryStatus.IN_TRANSIT, dispatch.dispatched().status());
    assertEquals(4, dispatch.packages().size());
    assertEquals(DeliveryStatus.IN_TRANSIT,
        Deliveries.indexByWaybill(dispatch.packages()).get("WB-URGENT-3").status());
  }

  @Test
  void dispatchNextSkipsNonDispatchablePackages() {
    List<Package> packages = List.of(
        pkg(1, "WB-DELIVERED", Priority.CRITICAL, DeliveryStatus.DELIVERED),
        pkg(2, "WB-CANCELED", Priority.CRITICAL, DeliveryStatus.CANCELED),
        pkg(3, "WB-PENDING", Priority.NORMAL, DeliveryStatus.CREATED));

    Deliveries.Dispatch dispatch = Deliveries.dispatchNext(packages).orElseThrow();

    assertEquals("WB-PENDING", dispatch.dispatched().idGuia());
  }

  @Test
  void dispatchNextReturnsEmptyWhenNothingIsDispatchable() {
    List<Package> packages = List.of(
        pkg(1, "WB-A", Priority.URGENT, DeliveryStatus.DELIVERED),
        pkg(2, "WB-B", Priority.NORMAL, DeliveryStatus.IN_TRANSIT));

    assertTrue(Deliveries.dispatchNext(packages).isEmpty());
  }

  @Test
  void dispatchNextPrefersEarlierDeadlineWithinSamePriority() {
    LocalDateTime base = LocalDateTime.of(2026, 1, 10, 12, 0);

    List<Package> packages = List.of(
        pkg(1, "WB-LATE", 1, 1f, 100, base.plusDays(5), Priority.URGENT, DeliveryStatus.CREATED),
        pkg(2, "WB-EARLY", 1, 1f, 100, base.plusDays(1), Priority.URGENT, DeliveryStatus.CREATED));

    Deliveries.Dispatch dispatch = Deliveries.dispatchNext(packages).orElseThrow();

    assertEquals("WB-EARLY", dispatch.dispatched().idGuia());
  }

  @Test
  void overdueReturnsOnlyActiveLatePackages() {
    LocalDateTime now = LocalDateTime.of(2026, 1, 10, 12, 0);

    List<Package> packages = List.of(
        pkg(1, "WB-OVER", 1, 1f, 100, now.minusDays(1), Priority.NORMAL, DeliveryStatus.CREATED),
        pkg(2, "WB-FUTURE", 1, 1f, 100, now.plusDays(1), Priority.NORMAL, DeliveryStatus.CREATED),
        pkg(3, "WB-DONE", 1, 1f, 100, now.minusDays(2), Priority.NORMAL, DeliveryStatus.DELIVERED));

    List<Package> overdue = Deliveries.overdue(packages, now);

    assertEquals(1, overdue.size());
    assertEquals("WB-OVER", overdue.get(0).idGuia());
    assertEquals(1, Deliveries.overdueCount(packages, now));
  }

  @Test
  void sortsPackagesByCostAndDeadline() {
    LocalDateTime base = LocalDateTime.of(2026, 3, 1, 8, 0);

    List<Package> packages = List.of(
        pkg(1, "WB-1", 1, 1f, 1500, base.plusDays(3), Priority.NORMAL, DeliveryStatus.CREATED),
        pkg(2, "WB-2", 1, 1f, 500, base.plusDays(1), Priority.NORMAL, DeliveryStatus.CREATED),
        pkg(3, "WB-3", 1, 1f, 1000, base.plusDays(2), Priority.NORMAL, DeliveryStatus.CREATED));

    assertEquals(List.of("WB-2", "WB-3", "WB-1"),
        Deliveries.sortByCost(packages).stream().map(Package::idGuia).toList());
    assertEquals(List.of("WB-2", "WB-3", "WB-1"),
        Deliveries.sortByDeadline(packages).stream().map(Package::idGuia).toList());
  }

  @Test
  void sortByPriorityMatchesUrgencyOrder() {
    assertEquals(List.of("WB-URGENT-2", "WB-URGENT-3", "WB-IMPORTANT-1", "WB-NORMAL-0"),
        Deliveries.sortByPriority(sample()).stream().map(Package::idGuia).toList());
  }

  @Test
  void quickSortProducesTheSameOrderAsMergeSort() {
    assertEquals(
        Deliveries.sortPackages(sample(), Deliveries.BY_COST).stream().map(Package::idGuia).toList(),
        Deliveries.sortPackagesQuick(sample(), Deliveries.BY_COST).stream().map(Package::idGuia)
            .toList());
  }
}
