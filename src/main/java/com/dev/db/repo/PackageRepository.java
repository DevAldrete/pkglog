package com.dev.db.repo;

import java.util.List;
import java.util.Optional;

import com.dev.domain.Package;

/** Persistence port for {@link Package}. */
public interface PackageRepository {

  List<Package> findAll();

  Optional<Package> findById(int id);

  Optional<Package> findByWaybill(String waybill);

  int nextId();

  void save(Package pkg);

  boolean deleteById(int id);

  long count();
}
