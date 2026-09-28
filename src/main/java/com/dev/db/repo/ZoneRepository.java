package com.dev.db.repo;

import java.util.List;
import java.util.Optional;

import com.dev.domain.Zone;

/** Persistence port for {@link Zone}. */
public interface ZoneRepository {

  List<Zone> findAll();

  Optional<Zone> findById(int id);

  int nextId();

  void save(Zone zone);

  boolean deleteById(int id);

  long count();
}
