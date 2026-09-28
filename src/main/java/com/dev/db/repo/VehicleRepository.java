package com.dev.db.repo;

import java.util.List;
import java.util.Optional;

import com.dev.domain.Vehicle;

/** Persistence port for {@link Vehicle}. */
public interface VehicleRepository {

  List<Vehicle> findAll();

  Optional<Vehicle> findById(int id);

  int nextId();

  void save(Vehicle vehicle);

  boolean deleteById(int id);

  long count();
}
