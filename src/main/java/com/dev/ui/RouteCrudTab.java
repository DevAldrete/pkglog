package com.dev.ui;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.dev.domain.Route;

/** CRUD over the transportation routes (admin only). */
public final class RouteCrudTab extends CrudTab<Route> {

  private static final long serialVersionUID = 1L;

  public RouteCrudTab(Store store) {
    super(store, "Ruta",
        new String[] { "Id", "Origen", "Destino", "Distancia", "Tiempo", "Costo" });
  }

  @Override
  protected List<Route> load() {
    return store.routes();
  }

  @Override
  protected Object[] toRow(Route route) {
    return new Object[] {
        route.id(),
        store.cityName(route.originId()),
        store.cityName(route.destinyId()),
        Format.distance(route.distanceMeters()),
        Format.duration(route.estimatedTime()),
        Format.money(route.expenseInCents()) };
  }

  @Override
  protected Optional<Route> build(Route existing) {
    FormDialog form = new FormDialog()
        .text("origin", "Origen (zona id)",
            existing == null ? "" : String.valueOf(existing.originId()))
        .text("destiny", "Destino (zona id)",
            existing == null ? "" : String.valueOf(existing.destinyId()))
        .text("distance", "Distancia (km)", existing == null ? ""
            : String.format("%.1f", existing.distanceMeters() / 1_000d))
        .text("time", "Tiempo (min)", existing == null ? ""
            : String.valueOf(existing.estimatedTime().toMinutes()))
        .text("cost", "Costo (R$)", existing == null ? ""
            : String.format("%.2f", existing.expenseInCents() / 100.0));

    return form.show(this, existing == null ? "Nueva ruta" : "Editar ruta")
        .map(values -> new Route(
            existing == null ? store.nextRouteId() : existing.id(),
            values.intValue("origin"),
            values.intValue("destiny"),
            values.doubleValue("distance") * 1_000d,
            Duration.ofMinutes(Math.round(values.doubleValue("time"))),
            Math.round(values.doubleValue("cost") * 100)));
  }

  @Override
  protected void persist(Route item) {
    store.saveRoute(item);
  }

  @Override
  protected boolean remove(Route item) {
    return store.deleteRoute(item.id());
  }

  @Override
  protected String describe(Route item) {
    return "ruta #" + item.id();
  }
}
