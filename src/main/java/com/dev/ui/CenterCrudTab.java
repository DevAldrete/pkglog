package com.dev.ui;

import java.util.List;
import java.util.Optional;

import com.dev.domain.CenterLevel;
import com.dev.domain.DistributionCenter;

/** CRUD over the distribution-center hierarchy (admin only). */
public final class CenterCrudTab extends CrudTab<DistributionCenter> {

  private static final long serialVersionUID = 1L;

  public CenterCrudTab(Store store) {
    super(store, "Centro", new String[] { "Id", "Nombre", "Nivel", "Padre", "Zona" });
  }

  @Override
  protected List<DistributionCenter> load() {
    return store.centers();
  }

  @Override
  protected Object[] toRow(DistributionCenter center) {
    return new Object[] {
        center.id(),
        center.name(),
        center.level(),
        center.isRoot() ? "-" : store.describeCenter(center.parentId()),
        center.zoneId() };
  }

  @Override
  protected Optional<DistributionCenter> build(DistributionCenter existing) {
    FormDialog form = new FormDialog()
        .text("name", "Nombre", existing == null ? "" : existing.name())
        .combo("level", "Nivel", names(CenterLevel.values()),
            existing == null ? CenterLevel.LOCAL.name() : existing.level().name())
        .text("parent", "Padre (id, 0 = raíz)",
            existing == null ? "1" : String.valueOf(existing.parentId()))
        .text("zone", "Zona (id)", existing == null ? "1" : String.valueOf(existing.zoneId()));

    return form.show(this, existing == null ? "Nuevo centro" : "Editar centro")
        .map(values -> new DistributionCenter(
            existing == null ? store.nextCenterId() : existing.id(),
            values.text("name"),
            CenterLevel.valueOf(values.text("level")),
            values.intValue("parent"),
            values.intValue("zone")));
  }

  @Override
  protected void persist(DistributionCenter item) {
    store.saveCenter(item);
  }

  @Override
  protected boolean remove(DistributionCenter item) {
    return store.deleteCenter(item.id());
  }

  @Override
  protected String describe(DistributionCenter item) {
    return item.name();
  }
}
