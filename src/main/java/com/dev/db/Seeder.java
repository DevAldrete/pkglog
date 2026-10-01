package com.dev.db;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.dev.data.Seed;
import com.dev.domain.DeliveryStatus;
import com.dev.domain.Package;
import com.dev.domain.Role;
import com.dev.domain.User;
import com.dev.security.Passwords;

/**
 * Populates an empty database with the deterministic baseline operation plus
 * the default accounts. Existing data is never touched, so it is safe to call
 * on every start; the file database keeps whatever the operator has entered.
 *
 * <p>{@link #refreshStaleSeedDeadlines} additionally self-heals a baseline
 * whose deadlines have aged out (every active seeded shipment past due), so a
 * long-lived database keeps showing a realistic mix instead of all red.
 */
public final class Seeder {

  /**
   * Waybills handed out by {@link Seed#packages()}; anything else is operator
   * data and must never be rewritten by the refresh.
   */
  private static final Pattern SEED_WAYBILL = Pattern.compile("^PKGLOG-\\d{4}$");

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

    Seed.packages(LocalDateTime.now()).forEach(repositories.packages()::save);
  }

  /**
   * Rewrites the deadlines of the baseline shipments when every active seeded
   * package is already overdue (for example a database seeded months ago).
   * Only waybills produced by {@link Seed} that are still active are touched:
   * ids, statuses, prices and any operator-entered package stay untouched.
   *
   * <p>Deadlines are recomputed relative to {@code now} from the same
   * deterministic seed, so the distribution of due dates is stable while a few
   * shipments remain intentionally past due.
   *
   * @return how many packages were rewritten (0 when nothing was stale)
   */
  public static int refreshStaleSeedDeadlines(Repositories repositories, LocalDateTime now) {
    List<Package> packages = repositories.packages().findAll();
    List<Package> activeSeed = new ArrayList<>();

    for (Package pkg : packages) {
      if (SEED_WAYBILL.matcher(pkg.idGuia()).matches() && isActive(pkg.status())) {
        activeSeed.add(pkg);
      }
    }

    if (activeSeed.isEmpty()) {
      return 0;
    }

    LocalDateTime latest = activeSeed.get(0).deadline();
    for (Package pkg : activeSeed) {
      if (pkg.deadline().isAfter(latest)) {
        latest = pkg.deadline();
      }
    }

    // As long as at least one baseline shipment still has a future deadline the
    // baseline is considered fresh, so the refresh never fights operator edits.
    if (!latest.isBefore(now)) {
      return 0;
    }

    Map<String, LocalDateTime> freshDeadlines = new HashMap<>();
    for (Package seeded : Seed.packages(now)) {
      freshDeadlines.put(seeded.idGuia(), seeded.deadline());
    }

    int refreshed = 0;
    for (Package pkg : activeSeed) {
      LocalDateTime deadline = freshDeadlines.get(pkg.idGuia());

      if (deadline != null && !deadline.equals(pkg.deadline())) {
        repositories.packages().save(pkg.withDeadline(deadline));
        refreshed++;
      }
    }

    return refreshed;
  }

  private static boolean isActive(DeliveryStatus status) {
    return status == DeliveryStatus.CREATED
        || status == DeliveryStatus.DISPATCHED
        || status == DeliveryStatus.IN_TRANSIT;
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
