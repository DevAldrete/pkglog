package com.dev.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

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
import com.dev.ds.HashMap;
import com.dev.modules.Deliveries;

/** Package table with waybill tracking, filters and status transitions. */
public final class TrackingPanel extends JPanel implements Refreshable {

  private static final long serialVersionUID = 1L;

  private final transient Store store;

  private final DefaultTableModel model;
  private final JTable table;
  private final JTextField waybillField = new JTextField(14);
  private final JComboBox<String> statusFilter = new JComboBox<>();
  private final JComboBox<String> priorityFilter = new JComboBox<>();
  private final JTextArea detail = new JTextArea(5, 30);
  private final JLabel message = new JLabel(" ");

  private List<Package> displayed = List.of();

  public TrackingPanel(Store store) {
    this.store = store;

    setLayout(new BorderLayout(8, 8));
    setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

    model = new DefaultTableModel(
        new Object[] { "Id", "Guía", "Ruta", "Peso", "Precio", "Prioridad", "Estado", "Límite" },
        0) {
      private static final long serialVersionUID = 1L;

      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };

    table = new JTable(model);
    table.setRowHeight(24);
    table.getTableHeader().setReorderingAllowed(false);

    add(buildFilters(), BorderLayout.NORTH);
    add(new JScrollPane(table), BorderLayout.CENTER);
    add(buildFooter(), BorderLayout.SOUTH);

    refresh();
  }

  private JPanel buildFilters() {
    JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));

    bar.add(new JLabel("Guía:"));
    bar.add(waybillField);

    JButton track = new JButton("Buscar");
    track.addActionListener(event -> track());
    bar.add(track);

    bar.add(new JLabel("Estado:"));
    statusFilter.addItem("Todos");
    for (DeliveryStatus status : DeliveryStatus.values()) {
      statusFilter.addItem(status.name());
    }
    statusFilter.addActionListener(event -> refresh());
    bar.add(statusFilter);

    bar.add(new JLabel("Prioridad:"));
    priorityFilter.addItem("Todas");
    for (Priority priority : Priority.values()) {
      priorityFilter.addItem(priority.name());
    }
    priorityFilter.addActionListener(event -> refresh());
    bar.add(priorityFilter);

    JButton clear = new JButton("Limpiar");
    clear.addActionListener(event -> {
      waybillField.setText("");
      statusFilter.setSelectedItem("Todos");
      priorityFilter.setSelectedItem("Todas");
      detail.setText("");
      message.setText(" ");
      refresh();
    });
    bar.add(clear);

    return bar;
  }

  private JPanel buildFooter() {
    JPanel footer = new JPanel(new BorderLayout(8, 8));

    detail.setEditable(false);
    detail.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    detail.setBorder(BorderFactory.createTitledBorder("Detalle del paquete"));

    JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));

    JButton advance = new JButton("Avanzar estado");
    advance.addActionListener(event -> advanceSelected());
    actions.add(advance);

    JButton cancel = new JButton("Cancelar paquete");
    cancel.addActionListener(event -> setSelectedStatus(DeliveryStatus.CANCELED));
    actions.add(cancel);
    actions.add(message);

    footer.add(new JScrollPane(detail), BorderLayout.CENTER);
    footer.add(actions, BorderLayout.SOUTH);

    return footer;
  }

  @Override
  public void refresh() {
    displayed = visiblePackages();

    model.setRowCount(0);
    for (Package pkg : displayed) {
      model.addRow(new Object[] {
          pkg.id(),
          pkg.idGuia(),
          pkg.routeId(),
          Format.weight(pkg.weight()),
          Format.money(pkg.priceInCents()),
          pkg.priority().level() + " - " + pkg.priority().name(),
          pkg.status(),
          Format.dateTime(pkg.deadline()) });
    }
  }

  private List<Package> visiblePackages() {
    List<Package> filtered = new ArrayList<>(store.packages());

    String status = (String) statusFilter.getSelectedItem();
    if (status != null && !"Todos".equals(status)) {
      filtered = new ArrayList<>(Deliveries.filterByStatus(filtered, DeliveryStatus.valueOf(status)));
    }

    String priority = (String) priorityFilter.getSelectedItem();
    if (priority != null && !"Todas".equals(priority)) {
      filtered = new ArrayList<>(Deliveries.filterByPriority(filtered, Priority.valueOf(priority)));
    }

    return filtered;
  }

  private void track() {
    String waybill = waybillField.getText().trim();

    if (waybill.isEmpty()) {
      message.setText("Ingrese una guía para buscar.");
      return;
    }

    HashMap<String, Package> index = Deliveries.indexByWaybill(store.packages());
    var found = Deliveries.findByWaybill(index, waybill);

    if (found.isEmpty()) {
      detail.setText("Guía no registrada: " + waybill);
      message.setText("Sin resultados.");
      return;
    }

    Package pkg = found.get();
    detail.setText(describe(pkg));
    selectByWaybill(waybill);
    message.setText("Encontrado: " + pkg.idGuia());
  }

  private String describe(Package pkg) {
    return "Guía           : " + pkg.idGuia() + "\n"
        + "Ruta           : " + store.routeDescription(pkg.routeId()) + "\n"
        + "Costo          : " + Format.money(pkg.priceInCents()) + "\n"
        + "Peso           : " + Format.weight(pkg.weight()) + "\n"
        + "Prioridad      : " + pkg.priority().level() + " - " + pkg.priority().name() + "\n"
        + "Estado         : " + pkg.status() + "\n"
        + "Fecha límite   : " + Format.dateTime(pkg.deadline());
  }

  private void selectByWaybill(String waybill) {
    for (int row = 0; row < displayed.size(); row++) {
      if (displayed.get(row).idGuia().equals(waybill)) {
        table.setRowSelectionInterval(row, row);
        return;
      }
    }

    message.setText("La guía existe pero está oculta por los filtros.");
  }

  private void advanceSelected() {
    Package pkg = selectedPackage();

    if (pkg == null) {
      message.setText("Seleccione un paquete.");
      return;
    }

    DeliveryStatus next = nextStatus(pkg.status());

    if (next == pkg.status()) {
      message.setText(pkg.idGuia() + " ya está en " + next);
      return;
    }

    setSelectedStatus(next);
  }

  private void setSelectedStatus(DeliveryStatus status) {
    Package pkg = selectedPackage();

    if (pkg == null) {
      message.setText("Seleccione un paquete.");
      return;
    }

    store.updatePackageStatus(pkg.idGuia(), status);
    refresh();
    selectByWaybill(pkg.idGuia());
    Deliveries.findByWaybill(Deliveries.indexByWaybill(store.packages()), pkg.idGuia())
        .ifPresent(found -> detail.setText(describe(found)));
    message.setText(pkg.idGuia() + " -> " + status);
  }

  private Package selectedPackage() {
    int row = table.getSelectedRow();

    return row < 0 || row >= displayed.size() ? null : displayed.get(row);
  }

  private static DeliveryStatus nextStatus(DeliveryStatus status) {
    return switch (status) {
      case CREATED -> DeliveryStatus.DISPATCHED;
      case DISPATCHED -> DeliveryStatus.IN_TRANSIT;
      case IN_TRANSIT -> DeliveryStatus.DELIVERED;
      case DELIVERED, CANCELED -> status;
    };
  }
}
