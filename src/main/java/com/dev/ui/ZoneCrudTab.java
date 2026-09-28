package com.dev.ui;

import java.util.List;
import java.util.Optional;

import com.dev.domain.Zone;

/** CRUD over geographic zones (admin only). */
public final class ZoneCrudTab extends CrudTab<Zone> {

  private static final long serialVersionUID = 1L;

  public ZoneCrudTab(Store store) {
    super(store, "Zona", new String[] { "Id", "Estado", "Ciudad" });
  }

  @Override
  protected List<Zone> load() {
    return store.zones();
  }

  @Override
  protected Object[] toRow(Zone zone) {
    return new Object[] { zone.id(), zone.state(), zone.city() };
  }

  @Override
  protected Optional<Zone> build(Zone existing) {
    FormDialog form = new FormDialog()
        .text("state", "Estado", existing == null ? "" : existing.state())
        .text("city", "Ciudad", existing == null ? "" : existing.city());

    return form.show(this, existing == null ? "Nueva zona" : "Editar zona")
        .map(values -> new Zone(
            existing == null ? store.nextZoneId() : existing.id(),
            values.text("state"),
            values.text("city")));
  }

  @Override
  protected void persist(Zone item) {
    store.saveZone(item);
  }

  @Override
  protected boolean remove(Zone item) {
    return store.deleteZone(item.id());
  }

  @Override
  protected String describe(Zone item) {
    return item.city();
  }
}
