package com.dev.ui;

import java.util.List;
import java.util.Optional;

import com.dev.domain.Vehicle;

/** CRUD over the delivery fleet (admin only). */
public final class VehicleCrudTab extends CrudTab<Vehicle> {

  private static final long serialVersionUID = 1L;

  public VehicleCrudTab(Store store) {
    super(store, "Vehículo", new String[] { "Id", "Placa", "Capacidad" });
  }

  @Override
  protected List<Vehicle> load() {
    return store.vehicles();
  }

  @Override
  protected Object[] toRow(Vehicle vehicle) {
    return new Object[] { vehicle.id(), vehicle.plate(), Format.weight(vehicle.capacityKg()) };
  }

  @Override
  protected Optional<Vehicle> build(Vehicle existing) {
    FormDialog form = new FormDialog()
        .text("plate", "Placa", existing == null ? "" : existing.plate())
        .text("capacity", "Capacidad (kg)", existing == null ? ""
            : String.valueOf(existing.capacityKg()));

    return form.show(this, existing == null ? "Nuevo vehículo" : "Editar vehículo")
        .map(values -> new Vehicle(
            existing == null ? store.nextVehicleId() : existing.id(),
            values.text("plate"),
            (float) values.doubleValue("capacity")));
  }

  @Override
  protected void persist(Vehicle item) {
    store.saveVehicle(item);
  }

  @Override
  protected boolean remove(Vehicle item) {
    return store.deleteVehicle(item.id());
  }

  @Override
  protected String describe(Vehicle item) {
    return item.plate();
  }
}
