package com.dev.ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import com.dev.domain.DeliveryStatus;
import com.dev.domain.Package;
import com.dev.domain.Priority;

/** CRUD over shipments. Create/edit are open to operators; delete is admin-only. */
public final class PackageCrudTab extends CrudTab<Package> {

  private static final long serialVersionUID = 1L;

  private static final DateTimeFormatter DEADLINE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  public PackageCrudTab(Store store) {
    super(store, "Paquete",
        new String[] { "Id", "Guía", "Ruta", "Peso", "Precio", "Prioridad", "Estado", "Límite" });
  }

  @Override
  protected List<Package> load() {
    return store.packages();
  }

  @Override
  protected Object[] toRow(Package pkg) {
    return new Object[] {
        pkg.id(),
        pkg.idGuia(),
        store.routeDescription(pkg.routeId()),
        Format.weight(pkg.weight()),
        Format.money(pkg.priceInCents()),
        pkg.priority().level() + " - " + pkg.priority().name(),
        pkg.status(),
        Format.dateTime(pkg.deadline()) };
  }

  @Override
  protected Optional<Package> build(Package existing) {
    FormDialog form = new FormDialog()
        .text("waybill", "Guía", existing == null ? "" : existing.idGuia())
        .text("route", "Ruta (id)", existing == null ? "1" : String.valueOf(existing.routeId()))
        .text("weight", "Peso (kg)", existing == null ? "" : String.valueOf(existing.weight()))
        .text("price", "Precio (R$)", existing == null ? ""
            : String.format("%.2f", existing.priceInCents() / 100.0))
        .combo("priority", "Prioridad", names(Priority.values()),
            existing == null ? Priority.NORMAL.name() : existing.priority().name())
        .combo("status", "Estado", names(DeliveryStatus.values()),
            existing == null ? DeliveryStatus.CREATED.name() : existing.status().name())
        .text("deadline", "Límite (yyyy-MM-dd HH:mm)", existing == null
            ? LocalDateTime.now().plusDays(3).withSecond(0).withNano(0).format(DEADLINE)
            : existing.deadline().format(DEADLINE));

    return form.show(this, existing == null ? "Nuevo paquete" : "Editar paquete")
        .map(values -> new Package(
            existing == null ? store.nextPackageId() : existing.id(),
            values.text("waybill"),
            values.intValue("route"),
            (float) values.doubleValue("weight"),
            Math.round(values.doubleValue("price") * 100),
            values.dateTime("deadline"),
            Priority.valueOf(values.text("priority")),
            DeliveryStatus.valueOf(values.text("status"))));
  }

  @Override
  protected void persist(Package item) {
    store.savePackage(item);
  }

  @Override
  protected boolean remove(Package item) {
    return store.deletePackage(item.id());
  }

  @Override
  protected String describe(Package item) {
    return item.idGuia();
  }

  @Override
  protected boolean canCreate() {
    return true;
  }

  @Override
  protected boolean canEdit() {
    return true;
  }
}
