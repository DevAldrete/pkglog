package com.dev.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDateTime;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.dev.domain.DeliveryStatus;
import com.dev.domain.Package;
import com.dev.domain.Priority;
import com.dev.ds.Search;
import com.dev.modules.Analytics;
import com.dev.modules.Deliveries;

/** Sorted reports (merge/quick sort) plus binary search and summary metrics. */
public final class ReportsPanel extends JPanel implements Refreshable {

  private static final long serialVersionUID = 1L;

  private final transient Store store;

  private final DefaultTableModel model;
  private final JComboBox<String> criterion = new JComboBox<>(new String[] { "Costo", "Fecha límite", "Prioridad" });
  private final JComboBox<String> algorithm = new JComboBox<>(new String[] { "MergeSort", "QuickSort" });
  private final JTextField costField = new JTextField(8);
  private final JLabel searchResult = new JLabel(" ");
  private final JTextArea summary = new JTextArea(9, 40);

  public ReportsPanel(Store store) {
    this.store = store;

    setLayout(new BorderLayout(8, 8));
    setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

    model = new DefaultTableModel(
        new Object[] { "Guía", "Ruta", "Costo", "Fecha límite", "Prioridad", "Estado" }, 0) {
      private static final long serialVersionUID = 1L;

      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };

    JTable table = new JTable(model);
    table.setRowHeight(24);
    table.getTableHeader().setReorderingAllowed(false);

    add(buildControls(), BorderLayout.NORTH);
    add(new JScrollPane(table), BorderLayout.CENTER);
    add(buildFooter(), BorderLayout.SOUTH);

    refresh();
  }

  private JPanel buildControls() {
    JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));

    bar.add(new JLabel("Ordenar por:"));
    criterion.addActionListener(event -> generateReport());
    bar.add(criterion);

    bar.add(new JLabel("Algoritmo:"));
    algorithm.addActionListener(event -> generateReport());
    bar.add(algorithm);

    JButton generate = new JButton("Generar reporte");
    generate.addActionListener(event -> generateReport());
    bar.add(generate);

    bar.add(new JLabel("   Buscar por costo (R$):"));
    bar.add(costField);

    JButton search = new JButton("Búsqueda binaria");
    search.addActionListener(event -> binarySearch());
    bar.add(search);
    bar.add(searchResult);

    return bar;
  }

  private JPanel buildFooter() {
    JPanel panel = new JPanel(new BorderLayout(8, 8));
    panel.setBorder(BorderFactory.createTitledBorder("Resumen analítico"));

    summary.setEditable(false);
    summary.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    panel.add(new JScrollPane(summary), BorderLayout.CENTER);

    return panel;
  }

  @Override
  public void refresh() {
    generateReport();
    renderSummary();
  }

  private void generateReport() {
    String algorithmName = (String) algorithm.getSelectedItem();
    var sorted = "QuickSort".equals(algorithmName)
        ? Deliveries.sortPackagesQuick(store.packages(), comparator())
        : Deliveries.sortPackages(store.packages(), comparator());

    model.setRowCount(0);
    for (Package pkg : sorted) {
      model.addRow(new Object[] {
          pkg.idGuia(),
          pkg.routeId(),
          Format.money(pkg.priceInCents()),
          Format.dateTime(pkg.deadline()),
          pkg.priority().level() + " - " + pkg.priority().name(),
          pkg.status() });
    }
  }

  private Comparator<Package> comparator() {
    return switch ((String) criterion.getSelectedItem()) {
      case "Fecha límite" -> Deliveries.BY_DEADLINE;
      case "Prioridad" -> Deliveries.URGENCY;
      default -> Deliveries.BY_COST;
    };
  }

  private void binarySearch() {
    try {
      long cents = Math.round(Double.parseDouble(costField.getText().trim()) * 100);
      var sorted = Deliveries.sortPackages(store.packages(), Deliveries.BY_COST);
      Package probe = new Package(0, "consulta", 0, 0f, cents, LocalDateTime.MIN,
          Priority.NORMAL, DeliveryStatus.CREATED);

      int index = Search.binarySearch(sorted, probe, Deliveries.BY_COST);

      if (index >= 0) {
        searchResult.setText("Encontrado: " + sorted.get(index).idGuia());
        return;
      }

      int lower = Search.lowerBound(sorted, probe, Deliveries.BY_COST);

      if (lower < sorted.size()) {
        Package nearest = sorted.get(lower);
        searchResult.setText("No exacto; más cercano: " + nearest.idGuia()
            + " (" + Format.money(nearest.priceInCents()) + ")");
      } else {
        searchResult.setText("Sin paquetes con costo mayor o igual");
      }
    } catch (NumberFormatException exception) {
      searchResult.setText("Ingrese un costo numérico, por ejemplo 250.00");
    }
  }

  private void renderSummary() {
    StringBuilder text = new StringBuilder();

    line(text, "Paquetes", String.valueOf(store.packages().size()));
    line(text, "Ingresos (sin cancelados)", Format.money(Analytics.revenueInCents(store.packages())));
    line(text, "Ingresos entregados", Format.money(Analytics.deliveredRevenueInCents(store.packages())));
    line(text, "Peso total", Format.weight(Analytics.totalWeight(store.packages())));
    line(text, "Rutas", String.valueOf(store.routes().size()));
    line(text, "Costo de rutas", Format.money(Analytics.totalExpense(store.routes())));
    line(text, "Distancia de red", Format.distance(Analytics.totalDistance(store.routes())));
    line(text, "Entregados",
        String.valueOf(Deliveries.filterByStatus(store.packages(), DeliveryStatus.DELIVERED).size()));
    line(text, "Ruta más costosa", Analytics.mostExpensiveRoute(store.routes())
        .map(route -> "#" + route.id() + " " + Format.money(route.expenseInCents())).orElse("-"));
    line(text, "Paquete más pesado", Analytics.heaviestPackage(store.packages())
        .map(pkg -> pkg.idGuia() + " " + Format.weight(pkg.weight())).orElse("-"));

    summary.setText(text.toString());
    summary.setCaretPosition(0);
  }

  private static void line(StringBuilder text, String label, String value) {
    text.append(String.format("  %-24s %s%n", label, value));
  }
}
