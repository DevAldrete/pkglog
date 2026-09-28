package com.dev.db.repo;

import java.util.List;
import java.util.Optional;

import com.dev.domain.DistributionCenter;

/** Persistence port for {@link DistributionCenter}. */
public interface CenterRepository {

  List<DistributionCenter> findAll();

  Optional<DistributionCenter> findById(int id);

  int nextId();

  void save(DistributionCenter center);

  boolean deleteById(int id);

  long count();
}
