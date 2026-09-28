package com.dev.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dev.domain.DeliveryStatus;
import com.dev.domain.Package;
import com.dev.domain.Priority;

class CsvTest {

  @Test
  void parsesQuotedFieldsWithCommasAndQuotes() {
    String csv = "a,\"b,c\",\"say \"\"hi\"\"\"\n1,2,3\n";

    List<String[]> rows = Csv.parse(csv);

    assertEquals(2, rows.size());
    assertEquals("b,c", rows.get(0)[1]);
    assertEquals("say \"hi\"", rows.get(0)[2]);
    assertEquals("3", rows.get(1)[2]);
  }

  @Test
  void packageTableRoundTrips() {
    List<Package> packages = List.of(
        new Package(1, "WB-1", 2, 3.5f, 9_900L, LocalDateTime.of(2026, 5, 1, 8, 0),
            Priority.URGENT, DeliveryStatus.CREATED),
        new Package(2, "WB,2", 3, 1.0f, 500L, LocalDateTime.of(2026, 5, 2, 9, 30),
            Priority.NORMAL, DeliveryStatus.IN_TRANSIT));

    String csv = CsvTables.exportPackages(packages);

    assertEquals(packages, CsvTables.importPackages(csv));
  }

  @Test
  void importIgnoresTheHeaderRow() {
    String csv = "id,waybill,routeId,weight,priceInCents,deadline,priority,status\n"
        + "7,WB-7,1,2.0,1234,2026-06-01T10:00,IMPORTANT,DISPATCHED\n";

    List<Package> packages = CsvTables.importPackages(csv);

    assertEquals(1, packages.size());
    assertEquals("WB-7", packages.get(0).idGuia());
    assertEquals(Priority.IMPORTANT, packages.get(0).priority());
  }
}
