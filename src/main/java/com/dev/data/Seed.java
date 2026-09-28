package com.dev.data;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.dev.domain.CenterLevel;
import com.dev.domain.DeliveryStatus;
import com.dev.domain.DistributionCenter;
import com.dev.domain.Package;
import com.dev.domain.Priority;
import com.dev.domain.Route;
import com.dev.domain.Vehicle;
import com.dev.domain.Zone;

/**
 * Deterministic generator of the operation's baseline data. Using a fixed seed
 * keeps every run (and every test) reproducible while providing the minimum
 * volumes required: at least 12 zones/centers, 12 routes and 50 packages.
 */
public final class Seed {

  private Seed() {
  }

  private static final long RANDOM_SEED = 42L;
  private static final int PACKAGE_COUNT = 60;
  private static final LocalDateTime BASE_DEADLINE = LocalDateTime.of(2026, 6, 1, 18, 0);

  private static final DeliveryStatus[] STATUSES = {
      DeliveryStatus.CREATED,
      DeliveryStatus.DISPATCHED,
      DeliveryStatus.IN_TRANSIT,
      DeliveryStatus.DELIVERED,
      DeliveryStatus.CANCELED
  };

  public static List<Zone> zones() {
    return List.of(
        new Zone(1, "SP", "Campinas"),
        new Zone(2, "SP", "Santos"),
        new Zone(3, "SP", "Sao Paulo"),
        new Zone(4, "RJ", "Rio de Janeiro"),
        new Zone(5, "MG", "Belo Horizonte"),
        new Zone(6, "PR", "Curitiba"),
        new Zone(7, "RS", "Porto Alegre"),
        new Zone(8, "BA", "Salvador"),
        new Zone(9, "PE", "Recife"),
        new Zone(10, "CE", "Fortaleza"),
        new Zone(11, "DF", "Brasilia"),
        new Zone(12, "GO", "Goiania"));
  }

  public static List<DistributionCenter> centers() {
    return List.of(
        new DistributionCenter(1, "Centro Nacional", CenterLevel.NACIONAL, 0, 11),
        new DistributionCenter(2, "Regional Sudeste", CenterLevel.REGIONAL, 1, 1),
        new DistributionCenter(3, "Regional Nordeste", CenterLevel.REGIONAL, 1, 8),
        new DistributionCenter(4, "Regional Sul", CenterLevel.REGIONAL, 1, 6),
        new DistributionCenter(5, "Regional Centro-Oeste", CenterLevel.REGIONAL, 1, 12),
        new DistributionCenter(6, "Local Campinas", CenterLevel.LOCAL, 2, 1),
        new DistributionCenter(7, "Local Sao Paulo", CenterLevel.LOCAL, 2, 3),
        new DistributionCenter(8, "Local Rio de Janeiro", CenterLevel.LOCAL, 2, 4),
        new DistributionCenter(9, "Local Salvador", CenterLevel.LOCAL, 3, 8),
        new DistributionCenter(10, "Local Recife", CenterLevel.LOCAL, 3, 9),
        new DistributionCenter(11, "Local Curitiba", CenterLevel.LOCAL, 4, 6),
        new DistributionCenter(12, "Local Porto Alegre", CenterLevel.LOCAL, 4, 7));
  }

  public static List<Route> routes() {
    List<Edge> edges = List.of(
        new Edge(1, 3, 100, 1.5, 90),
        new Edge(3, 2, 80, 1.2, 70),
        new Edge(3, 4, 430, 6.0, 380),
        new Edge(4, 5, 440, 6.5, 400),
        new Edge(5, 11, 720, 10.0, 650),
        new Edge(6, 7, 400, 5.5, 360),
        new Edge(7, 12, 320, 5.0, 300),
        new Edge(8, 9, 840, 12.0, 780),
        new Edge(9, 10, 800, 11.0, 740),
        new Edge(11, 12, 210, 3.0, 190),
        new Edge(1, 5, 590, 8.0, 540),
        new Edge(3, 6, 340, 5.0, 320),
        new Edge(4, 7, 1_550, 22.0, 1_400),
        new Edge(8, 11, 1_050, 15.0, 950));

    List<Route> routes = new ArrayList<>(edges.size());
    int id = 1;

    for (Edge edge : edges) {
      routes.add(new Route(
          id++,
          edge.origin(),
          edge.destiny(),
          edge.kilometers() * 1_000d,
          Duration.ofMinutes(Math.round(edge.hours() * 60)),
          edge.reais() * 100L));
    }

    return List.copyOf(routes);
  }

  public static List<Vehicle> vehicles() {
    return List.of(
        new Vehicle(1, "ABC-1A23", 1_200f),
        new Vehicle(2, "DEF-4B56", 3_500f),
        new Vehicle(3, "GHI-7C89", 600f),
        new Vehicle(4, "JKL-0D12", 8_000f),
        new Vehicle(5, "MNO-3E45", 2_200f),
        new Vehicle(6, "PQR-6F78", 5_000f));
  }

  public static List<Package> packages() {
    Random random = new Random(RANDOM_SEED);
    List<Route> routes = routes();
    List<Package> packages = new ArrayList<>(PACKAGE_COUNT);

    for (int i = 1; i <= PACKAGE_COUNT; i++) {
      packages.add(new Package(
          i,
          String.format("PKGLOG-%04d", i),
          routes.get(random.nextInt(routes.size())).id(),
          0.5f + random.nextInt(195) / 10f,
          1_500L + random.nextInt(58_501),
          BASE_DEADLINE.plusDays(1 + random.nextInt(10)),
          Priority.values()[random.nextInt(Priority.values().length)],
          STATUSES[random.nextInt(STATUSES.length)]));
    }

    return List.copyOf(packages);
  }

  private record Edge(int origin, int destiny, double kilometers, double hours, long reais) {
  }
}
