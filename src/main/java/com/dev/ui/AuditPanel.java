package com.dev.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.dev.domain.AuditEvent;

/** Read-only viewer over the append-only audit trail. */
public final class AuditPanel extends JPanel implements Refreshable {

  private static final long serialVersionUID = 1L;

  private final transient Store store;

  private final DefaultTableModel model;
  private final JLabel status = new JLabel(" ");

  public AuditPanel(Store store) {
    this.store = store;

    setLayout(new BorderLayout(8, 8));
    setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

    model = new DefaultTableModel(
        new Object[] { "Fecha", "Actor", "Acción", "Entidad", "Referencia", "Detalle" }, 0) {
      private static final long serialVersionUID = 1L;

      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };

    JTable table = new JTable(model);
    table.setRowHeight(22);
    table.getTableHeader().setReorderingAllowed(false);

    JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
    JButton refresh = new JButton("Actualizar");
    refresh.addActionListener(event -> refresh());
    header.add(refresh);
    header.add(status);

    add(header, BorderLayout.NORTH);
    add(new JScrollPane(table), BorderLayout.CENTER);

    refresh();
  }

  @Override
  public void refresh() {
    model.setRowCount(0);

    for (AuditEvent event : store.auditEvents(300)) {
      model.addRow(new Object[] {
          Format.dateTime(event.at()),
          event.actor(),
          event.action(),
          event.entity(),
          event.entityId(),
          event.detail() });
    }

    status.setText("Eventos recientes: " + model.getRowCount());
  }
}
