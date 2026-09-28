package com.dev.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * An immutable record of a single business mutation, used for traceability and
 * compliance. Every write in the application produces one of these.
 *
 * @param id       surrogate key assigned by the store
 * @param at       when the mutation happened
 * @param actor    who performed it (user name, or "system")
 * @param action   what happened (CREATE, UPDATE, DELETE, DISPATCH, ...)
 * @param entity   the affected entity type (Package, Route, ...)
 * @param entityId identifier of the affected row, as text
 * @param detail   free-form human readable description
 */
public record AuditEvent(
    long id,
    LocalDateTime at,
    String actor,
    String action,
    String entity,
    String entityId,
    String detail) {

  public AuditEvent {
    Objects.requireNonNull(at, "at must not be null");
    Objects.requireNonNull(actor, "actor must not be null");
    Objects.requireNonNull(action, "action must not be null");
    Objects.requireNonNull(entity, "entity must not be null");
    Objects.requireNonNull(entityId, "entityId must not be null");
    Objects.requireNonNull(detail, "detail must not be null");
  }
}
