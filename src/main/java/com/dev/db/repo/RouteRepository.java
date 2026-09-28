package com.dev.db.repo;

import java.util.List;
import java.util.Optional;

import com.dev.domain.Route;

/** Persistence port for {@link Route}. */
public interface RouteRepository {

  List<Route> findAll();

  Optional<Route> findById(int id);

  int nextId();

  void save(Route route);

  boolean deleteById(int id);

  long count();
}
