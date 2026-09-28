package com.dev.pkglog;

import java.time.Duration;
import java.time.LocalDateTime;

import com.dev.domain.CenterLevel;
import com.dev.domain.DeliveryStatus;
import com.dev.domain.DistributionCenter;
import com.dev.domain.Package;
import com.dev.domain.Priority;
import com.dev.domain.Route;
import com.dev.domain.Vehicle;
import com.dev.domain.Zone;

/** Shared factories for building test data without repeating constructor noise. */
public final class Fixtures {

  public static final int ROUTE_ID = 1;
  public static final float WEIGHT = 1f;
  public static final long PRICE_IN_CENTS = 1000L;
  public static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 1, 1, 12, 0);

  private Fixtures() {
  }

  public static Package pkg(int id, String waybill, Priority priority) {
    return pkg(id, waybill, priority, DeliveryStatus.CREATED);
  }

  public static Package pkg(int id, String waybill, Priority priority, DeliveryStatus status) {
    return pkg(id, waybill, ROUTE_ID, WEIGHT, PRICE_IN_CENTS, priority, status);
  }

  public static Package pkg(int id, String waybill, int routeId, float weight, long priceInCents,
      Priority priority, DeliveryStatus status) {
    return pkg(id, waybill, routeId, weight, priceInCents, DEADLINE, priority, status);
  }

  public static Package pkg(int id, String waybill, int routeId, float weight, long priceInCents,
      LocalDateTime deadline, Priority priority, DeliveryStatus status) {
    return new Package(id, waybill, routeId, weight, priceInCents, deadline, priority, status);
  }

  public static Route route(int id, int originId, int destinyId, double distanceMeters,
      Duration estimatedTime, long expenseInCents) {
    return new Route(id, originId, destinyId, distanceMeters, estimatedTime, expenseInCents);
  }

  public static Zone zone(int id, String state, String city) {
    return new Zone(id, state, city);
  }

  public static Vehicle vehicle(int id, String plate, float capacityKg) {
    return new Vehicle(id, plate, capacityKg);
  }

  public static DistributionCenter center(int id, String name, CenterLevel level, int parentId,
      int zoneId) {
    return new DistributionCenter(id, name, level, parentId, zoneId);
  }
}
