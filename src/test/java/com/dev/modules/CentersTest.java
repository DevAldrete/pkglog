package com.dev.modules;

import static com.dev.pkglog.Fixtures.center;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.dev.domain.CenterLevel;
import com.dev.domain.DistributionCenter;
import com.dev.ds.Tree;

class CentersTest {

  private static List<DistributionCenter> sample() {
    return List.of(
        center(1, "Nacional", CenterLevel.NACIONAL, 0, 0),
        center(2, "Regional SP", CenterLevel.REGIONAL, 1, 1),
        center(3, "Regional RJ", CenterLevel.REGIONAL, 1, 2),
        center(4, "Local Campinas", CenterLevel.LOCAL, 2, 1),
        center(5, "Local Santos", CenterLevel.LOCAL, 2, 1));
  }

  @Test
  void buildsHierarchyFromUnorderedInput() {
    List<DistributionCenter> shuffled = List.of(
        sample().get(3),
        sample().get(0),
        sample().get(4),
        sample().get(1),
        sample().get(2));

    Tree<DistributionCenter> tree = Centers.hierarchy(shuffled);

    assertEquals(5, tree.size());
    assertEquals("Nacional", tree.root().value().name());
  }

  @Test
  void nationalToLocalReturnsPreOrder() {
    assertEquals(
        List.of("Nacional", "Regional SP", "Local Campinas", "Local Santos", "Regional RJ"),
        Centers.nationalToLocal(sample()).stream().map(DistributionCenter::name).toList());
  }

  @Test
  void groupsCentersByLevel() {
    var byLevel = Centers.byLevel(sample());

    assertEquals(1, byLevel.get(CenterLevel.NACIONAL).size());
    assertEquals(2, byLevel.get(CenterLevel.REGIONAL).size());
    assertEquals(2, byLevel.get(CenterLevel.LOCAL).size());
  }

  @Test
  void rejectsMissingRoot() {
    assertThrows(IllegalArgumentException.class,
        () -> Centers.hierarchy(List.of(center(2, "Orphan", CenterLevel.LOCAL, 1, 1))));
  }

  @Test
  void rejectsUnknownParent() {
    assertThrows(IllegalArgumentException.class, () -> Centers.hierarchy(List.of(
        center(1, "Nacional", CenterLevel.NACIONAL, 0, 0),
        center(2, "Orphan", CenterLevel.LOCAL, 9, 1))));
  }
}
