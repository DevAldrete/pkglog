package com.dev.db.repo;

import java.util.List;
import java.util.Optional;

import com.dev.domain.User;

/** Persistence port for {@link User}. */
public interface UserRepository {

  List<User> findAll();

  Optional<User> findById(int id);

  Optional<User> findByEmail(String email);

  int nextId();

  void save(User user);

  boolean deleteById(int id);

  long count();
}
