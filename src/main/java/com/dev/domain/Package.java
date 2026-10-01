package com.dev.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public record Package(
    int id,
    String waybill,
    int routeId,
    float weight,
    long priceInCents,
    LocalDateTime deadline,
    Priority priority,
    DeliveryStatus status) {

  public Package {
    Objects.requireNonNull(waybill, "waybill must not be null");
    Objects.requireNonNull(deadline, "deadline must not be null");
    Objects.requireNonNull(priority, "priority must not be null");
    Objects.requireNonNull(status, "status must not be null");

    if (waybill.isBlank()) {
      throw new IllegalArgumentException("waybill must not be blank");
    }

    if (weight < 0) {
      throw new IllegalArgumentException("weight must be non negative");
    }

    if (priceInCents < 0) {
      throw new IllegalArgumentException("priceInCents must be non negative");
    }
  }

  /** Tracking code (guide number) used by the hash table. */
  public String idGuia() {
    return waybill;
  }

  public Package withPriority(Priority priority) {
    return new Package(id, waybill, routeId, weight, priceInCents, deadline, priority, status);
  }

  public Package withStatus(DeliveryStatus status) {
    return new Package(id, waybill, routeId, weight, priceInCents, deadline, priority, status);
  }

  public Package withDeadline(LocalDateTime deadline) {
    return new Package(id, waybill, routeId, weight, priceInCents, deadline, priority, status);
  }
}
