package com.dev.db.repo;

import java.util.List;

import com.dev.domain.AuditEvent;

/** Append-only persistence port for {@link AuditEvent}. */
public interface AuditRepository {

  void record(AuditEvent event);

  List<AuditEvent> recent(int limit);

  long count();
}
