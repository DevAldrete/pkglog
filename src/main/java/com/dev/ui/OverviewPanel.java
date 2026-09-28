package com.dev.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDateTime;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.dev.domain.DeliveryStatus;
import com.dev.domain.Package;
import com.dev.domain.Route;
import com.dev.modules.Analytics;
import com.dev.modules.Deliveries;

/** Landing tab with the headline numbers for the whole operation. */
public final class OverviewPanel extends JPanel implements Refreshable {

  private static final long serialVersionUID = 1L;

  private final transient Store store;

  private final JLabel packages = new JLabel();
  private final JLabel delivered = new JLabel();
  private final JLabel inTransit = new JLabel();
  private final JLabel canceled = new JLabel();
  private final JLabel overdue = new JLabel();
  private final JLabel revenue = new JLabel();
  private final JLabel routes = new JLabel();
  private final JLabel distance = new JLabel();
  private final JLabel expense = new JLabel();
  private final JLabel zones = new JLabel();
  private final JLabel centers = new JLabel();
  private final JLabel vehicles = new JLabel();
  private final JLabel topRoute = new JLabel();
  private final JLabel heaviest = new JLabel();

  public OverviewPanel(Store store) {
    this.store = store;

    setLayout(new GridLayout(0, 2, 12, 12));
    setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

    add(card("Paquetes", packages));
    add(card("Entregados", delivered));
    add(card("En tránsito", inTransit));
    add(card("Cancelados", canceled));
    add(card("Vencidos", overdue));
    add(card("Ingresos (sin cancelados)", revenue));
    add(card("Rutas", routes));
    add(card("Distancia de red", distance));
    add(card("Costo de rutas", expense));
    add(card("Zonas", zones));
    add(card("Centros", centers));
    add(card("Vehículos", vehicles));
    add(card("Ruta más costosa", topRoute));
    add(card("Paquete más pesado", heaviest));

    refresh();
  }

  private static JPanel card(String title, JLabel value) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createTitledBorder(title));

    value.setFont(value.getFont().deriveFont(Font.BOLD, 18f));
    value.setBorder(BorderFactory.createEmptyBorder(4, 6, 6, 6));
    panel.add(value, BorderLayout.CENTER);

    return panel;
  }

  @Override
  public void refresh() {
    packages.setText(String.valueOf(store.packages().size()));
    delivered.setText(count(DeliveryStatus.DELIVERED));
    inTransit.setText(count(DeliveryStatus.IN_TRANSIT));
    canceled.setText(count(DeliveryStatus.CANCELED));
    overdue.setText(String.valueOf(Deliveries.overdueCount(store.packages(), LocalDateTime.now())));
    revenue.setText(Format.money(Analytics.revenueInCents(store.packages())));
    routes.setText(String.valueOf(store.routes().size()));
    distance.setText(Format.distance(Analytics.totalDistance(store.routes())));
    expense.setText(Format.money(Analytics.totalExpense(store.routes())));
    zones.setText(String.valueOf(store.zones().size()));
    centers.setText(String.valueOf(store.centers().size()));
    vehicles.setText(String.valueOf(store.vehicles().size()));
    topRoute.setText(Analytics.mostExpensiveRoute(store.routes())
        .map(OverviewPanel::describeRoute).orElse("-"));
    heaviest.setText(Analytics.heaviestPackage(store.packages())
        .map(OverviewPanel::describePackage).orElse("-"));
  }

  private String count(DeliveryStatus status) {
    return String.valueOf(Deliveries.filterByStatus(store.packages(), status).size());
  }

  private static String describeRoute(Route route) {
    return "#" + route.id() + "  " + Format.money(route.expenseInCents());
  }

  private static String describePackage(Package pkg) {
    return pkg.idGuia() + "  " + Format.weight(pkg.weight());
  }
}
