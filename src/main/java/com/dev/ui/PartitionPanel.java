package com.dev.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

import com.dev.domain.Package;
import com.dev.modules.Routing;

/** Capacity-aware fleet planning: spread the dispatchable batch across vehicles. */
public final class PartitionPanel extends JPanel implements Refreshable {

  private static final long serialVersionUID = 1L;

  private final transient Store store;

  private final DefaultTableModel model;
  private final JLabel summary = new JLabel(" ");
  private final JTextArea unassignedArea = new JTextArea(4, 40);

  public PartitionPanel(Store store) {
    this.store = store;

    setLayout(new BorderLayout(8, 8));
    setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

    model = new DefaultTableModel(
        new Object[] { "Vehículo", "Placa", "Capacidad", "Paquetes", "Peso asignado", "Uso" }, 0) {
      private static final long serialVersionUID = 1L;

      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };

    JTable table = new JTable(model);
    table.setRowHeight(24);
    table.getTableHeader().setReorderingAllowed(false);

    JPanel header = new JPanel(new BorderLayout());
    header.setBorder(BorderFactory.createTitledBorder("Plan de reparto por vehículo"));
    header.add(new JLabel("Divide y vencerás + capacidad: ninguna carga supera la capacidad."),
        BorderLayout.NORTH);

    JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
    JButton partition = new JButton("Planificar reparto");
    partition.addActionListener(event -> refresh());
    actions.add(partition);
    actions.add(summary);
    header.add(actions, BorderLayout.SOUTH);

    add(header, BorderLayout.NORTH);
    add(new JScrollPane(table), BorderLayout.CENTER);
    add(buildUnassigned(), BorderLayout.SOUTH);

    refresh();
  }

  private JPanel buildUnassigned() {
    unassignedArea.setEditable(false);
    unassignedArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    unassignedArea.setForeground(new java.awt.Color(0x8A4B00));

    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createTitledBorder("Sin asignar (exceden la flota)"));
    panel.add(new JScrollPane(unassignedArea), BorderLayout.CENTER);

    return panel;
  }

  @Override
  public void refresh() {
    Routing.Plan plan = Routing.plan(store.packages(), store.vehicles());

    model.setRowCount(0);

    int packages = 0;
    float weight = 0f;

    for (Routing.Assignment assignment : plan.assignments()) {
      float capacity = assignment.vehicle().capacityKg();
      float assigned = assignment.totalWeight();
      float usage = capacity == 0 ? 0f : assigned / capacity * 100f;

      packages += assignment.packages().size();
      weight += assigned;

      model.addRow(new Object[] {
          "#" + assignment.vehicle().id(),
          assignment.vehicle().plate(),
          Format.weight(capacity),
          assignment.packages().size(),
          Format.weight(assigned),
          String.format("%.0f%%", usage) });
    }

    summary.setText("Vehículos usados: " + plan.assignments().size()
        + " | Paquetes: " + packages
        + " | Peso total: " + Format.weight(weight));

    renderUnassigned(plan);
  }

  private void renderUnassigned(Routing.Plan plan) {
    if (plan.isFullyAssigned()) {
      unassignedArea.setText("Todos los paquetes despachables fueron asignados.");
      return;
    }

    StringBuilder text = new StringBuilder();

    for (Package pkg : plan.unassigned()) {
      text.append(pkg.idGuia())
          .append("  ")
          .append(Format.weight(pkg.weight()))
          .append("  (")
          .append(pkg.priority().name())
          .append(")\n");
    }

    unassignedArea.setText(text.toString());
    unassignedArea.setCaretPosition(0);
  }
}
