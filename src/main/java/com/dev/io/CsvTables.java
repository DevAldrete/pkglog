package com.dev.io;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.dev.domain.DeliveryStatus;
import com.dev.domain.DistributionCenter;
import com.dev.domain.Package;
import com.dev.domain.Priority;
import com.dev.domain.Route;
import com.dev.domain.Vehicle;
import com.dev.domain.Zone;

/**
 * Maps domain records to and from CSV. Package import is supported (the most
 * common bulk operation); the other tables are export-only for reporting and
 * migration hand-off.
 */
public final class CsvTables {

  private static final String PACKAGE_HEADER =
      "id,waybill,routeId,weight,priceInCents,deadline,priority,status";

  private CsvTables() {
  }

  // ---------------------------------------------------------------------------
  // Packages
  // ---------------------------------------------------------------------------

  public static String exportPackages(List<Package> packages) {
    StringBuilder csv = new StringBuilder(PACKAGE_HEADER).append('\n');

    for (Package pkg : packages) {
      csv.append(Csv.line(
          pkg.id(),
          pkg.idGuia(),
          pkg.routeId(),
          pkg.weight(),
          pkg.priceInCents(),
          pkg.deadline(),
          pkg.priority().name(),
          pkg.status().name())).append('\n');
    }

    return csv.toString();
  }

  public static List<Package> importPackages(String csv) {
    List<Package> packages = new ArrayList<>();
    List<String[]> rows = Csv.parse(csv);

    for (int index = 0; index < rows.size(); index++) {
      String[] row = rows.get(index);

      if (row.length == 0 || row[0].isBlank()) {
        continue;
      }

      if (index == 0 && "id".equalsIgnoreCase(row[0].trim())) {
        continue;
      }

      packages.add(new Package(
          Integer.parseInt(row[0].trim()),
          row[1].trim(),
          Integer.parseInt(row[2].trim()),
          Float.parseFloat(row[3].trim()),
          Long.parseLong(row[4].trim()),
          LocalDateTime.parse(row[5].trim()),
          Priority.valueOf(row[6].trim()),
          DeliveryStatus.valueOf(row[7].trim())));
    }

    return packages;
  }

  // ---------------------------------------------------------------------------
  // Export-only tables
  // ---------------------------------------------------------------------------

  public static String exportRoutes(List<Route> routes) {
    StringBuilder csv = new StringBuilder(
        "id,originId,destinyId,distanceMeters,estimatedTimeSeconds,expenseInCents\n");

    for (Route route : routes) {
      csv.append(Csv.line(route.id(), route.originId(), route.destinyId(), route.distanceMeters(),
          route.estimatedTime().getSeconds(), route.expenseInCents())).append('\n');
    }

    return csv.toString();
  }

  public static String exportZones(List<Zone> zones) {
    StringBuilder csv = new StringBuilder("id,state,city\n");

    for (Zone zone : zones) {
      csv.append(Csv.line(zone.id(), zone.state(), zone.city())).append('\n');
    }

    return csv.toString();
  }

  public static String exportVehicles(List<Vehicle> vehicles) {
    StringBuilder csv = new StringBuilder("id,plate,capacityKg\n");

    for (Vehicle vehicle : vehicles) {
      csv.append(Csv.line(vehicle.id(), vehicle.plate(), vehicle.capacityKg())).append('\n');
    }

    return csv.toString();
  }

  public static String exportCenters(List<DistributionCenter> centers) {
    StringBuilder csv = new StringBuilder("id,name,level,parentId,zoneId\n");

    for (DistributionCenter center : centers) {
      csv.append(Csv.line(center.id(), center.name(), center.level().name(), center.parentId(),
          center.zoneId())).append('\n');
    }

    return csv.toString();
  }
}
