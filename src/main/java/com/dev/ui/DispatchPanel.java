package com.dev.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.dev.domain.DeliveryStatus;
import com.dev.domain.Package;
import com.dev.domain.Priority;
import com.dev.ds.HashMap;
import com.dev.modules.Deliveries;

/** Deadline-aware urgent dispatch: priority breakdown and next shipment. */
public final class DispatchPanel extends JPanel implements Refreshable {

  private static final long serialVersionUID = 1L;

  private final transient Store store;

  private final DefaultTableModel model;
  private final JTable table;
  private final JLabel message = new JLabel(" ");
  private final JLabel[] priorityCounts = new JLabel[Priority.values().length];
  private final JLabel pendingValue = new JLabel();
  private final JLabel overdueValue = new JLabel();

  private HashMap<String, Boolean> overdueWaybills = new HashMap<>(16);

  public DispatchPanel(Store store) {
    this.store = store;

    setLayout(new BorderLayout(8, 8));
    setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

    model = new DefaultTableModel(
        new Object[] { "Guía", "Ruta", "Prioridad", "Peso", "Estado", "Límite" }, 0) {
      private static final long serialVersionUID = 1L;

      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };

    table = new JTable(model);
    table.setRowHeight(24);
    table.getTableHeader().setReorderingAllowed(false);
    table.setDefaultRenderer(Object.class, overdueRenderer());

    add(buildPriorityBar(), BorderLayout.NORTH);
    add(new JScrollPane(table), BorderLayout.CENTER);
    add(buildActionBar(), BorderLayout.SOUTH);

    refresh();
  }

  private DefaultTableCellRenderer overdueRenderer() {
    return new DefaultTableCellRenderer() {
      private static final long serialVersionUID = 1L;

      @Override
      public Component getTableCellRendererComponent(JTable source, Object value, boolean selected,
          boolean focused, int row, int column) {
        Component component = super.getTableCellRendererComponent(source, value, selected, focused,
            row, column);
        String waybill = String.valueOf(model.getValueAt(source.convertRowIndexToModel(row), 0));

        if (!selected) {
          component.setForeground(overdueWaybills.containsKey(waybill) ? Color.RED : Color.BLACK);
        }

        return component;
      }
    };
  }

  private JPanel buildPriorityBar() {
    JPanel panel = new JPanel(new BorderLayout(8, 8));
    panel.setBorder(BorderFactory.createTitledBorder("Cola por prioridad"));

    JPanel counts = new JPanel(new GridLayout(1, Priority.values().length, 8, 8));

    for (Priority priority : Priority.values()) {
      JLabel value = new JLabel("0", JLabel.CENTER);
      value.setFont(value.getFont().deriveFont(Font.BOLD, 20f));

      JPanel card = new JPanel(new BorderLayout());
      card.setBorder(BorderFactory.createTitledBorder(priority.level() + " - " + priority.name()));
      card.add(value, BorderLayout.CENTER);

      priorityCounts[priority.ordinal()] = value;
      counts.add(card);
    }

    JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    header.add(new JLabel("Pendientes de despacho:"));
    header.add(pendingValue);
    header.add(new JLabel("   |   Vencidos:"));
    overdueValue.setForeground(Color.RED);
    header.add(overdueValue);

    panel.add(header, BorderLayout.NORTH);
    panel.add(counts, BorderLayout.CENTER);

    return panel;
  }

  private JPanel buildActionBar() {
    JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));

    JButton dispatch = new JButton("Despachar Siguiente Envío");
    dispatch.addActionListener(event -> dispatchNext());
    bar.add(dispatch);
    bar.add(message);

    return bar;
  }

  @Override
  public void refresh() {
    for (Priority priority : Priority.values()) {
      priorityCounts[priority.ordinal()]
          .setText(String.valueOf(Deliveries.countByPriority(store.packages(), priority)));
    }

    LocalDateTime now = LocalDateTime.now();
    List<Package> pending = pendingPackages();
    pendingValue.setText(String.valueOf(pending.size()));

    overdueWaybills = new HashMap<>(16);
    for (Package pkg : Deliveries.overdue(store.packages(), now)) {
      overdueWaybills.put(pkg.idGuia(), true);
    }
    overdueValue.setText(String.valueOf(overdueWaybills.size()));

    model.setRowCount(0);
    for (Package pkg : Deliveries.sortByPriority(pending)) {
      model.addRow(new Object[] {
          pkg.idGuia(),
          store.routeDescription(pkg.routeId()),
          pkg.priority().level() + " - " + pkg.priority().name(),
          Format.weight(pkg.weight()),
          pkg.status(),
          Format.dateTime(pkg.deadline()) });
    }
  }

  private void dispatchNext() {
    Deliveries.Dispatch dispatch = Deliveries.dispatchNext(store.packages()).orElse(null);

    if (dispatch == null) {
      message.setText("No hay envíos pendientes de despacho.");
      return;
    }

    store.applyDispatch(dispatch);
    refresh();

    Package pkg = dispatch.dispatched();
    message.setText("Despachado " + pkg.idGuia() + " (prioridad " + pkg.priority().level()
        + ", límite " + Format.dateTime(pkg.deadline()) + ") -> " + pkg.status());
  }

  private List<Package> pendingPackages() {
    List<Package> pending = new ArrayList<>();

    for (Package pkg : store.packages()) {
      if (pkg.status() == DeliveryStatus.CREATED || pkg.status() == DeliveryStatus.DISPATCHED) {
        pending.add(pkg);
      }
    }

    return pending;
  }
}
