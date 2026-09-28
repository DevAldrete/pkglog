package com.dev.db;

import com.dev.data.Seed;
import com.dev.domain.Role;
import com.dev.domain.User;
import com.dev.security.Passwords;

/**
 * Populates an empty database with the deterministic baseline operation plus
 * the default accounts. Existing data is never touched, so it is safe to call
 * on every start; the file database keeps whatever the operator has entered.
 */
public final class Seeder {

  private Seeder() {
  }

  public static void seedIfEmpty(Repositories repositories) {
    seedZones(repositories);
    seedRoutes(repositories);
    seedCenters(repositories);
    seedVehicles(repositories);
    seedPackages(repositories);
    seedUsers(repositories);
  }

  private static void seedZones(Repositories repositories) {
    if (repositories.zones().count() > 0) {
      return;
    }

    Seed.zones().forEach(repositories.zones()::save);
  }

  private static void seedRoutes(Repositories repositories) {
    if (repositories.routes().count() > 0) {
      return;
    }

    Seed.routes().forEach(repositories.routes()::save);
  }

  private static void seedCenters(Repositories repositories) {
    if (repositories.centers().count() > 0) {
      return;
    }

    Seed.centers().forEach(repositories.centers()::save);
  }

  private static void seedVehicles(Repositories repositories) {
    if (repositories.vehicles().count() > 0) {
      return;
    }

    Seed.vehicles().forEach(repositories.vehicles()::save);
  }

  private static void seedPackages(Repositories repositories) {
    if (repositories.packages().count() > 0) {
      return;
    }

    Seed.packages().forEach(repositories.packages()::save);
  }

  private static void seedUsers(Repositories repositories) {
    if (repositories.users().count() > 0) {
      return;
    }

    repositories.users().save(new User(
        repositories.users().nextId(), "Administrator", "admin", Passwords.hash("admin123"),
        Role.ADMIN));
    repositories.users().save(new User(
        repositories.users().nextId(), "Operator", "operator", Passwords.hash("operator123"),
        Role.USER));
  }
}
