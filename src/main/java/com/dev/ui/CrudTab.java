package com.dev.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Reusable list + create/edit/delete tab. Subclasses decide how a row maps to
 * and from its domain record; permissions default to admin-only.
 *
 * @param <T> the domain type managed by the tab
 */
public abstract class CrudTab<T> extends JPanel implements Refreshable {

  private static final long serialVersionUID = 1L;

  protected final transient Store store;

  private final String entity;
  private final DefaultTableModel model;
  private final JTable table;
  private final JLabel message = new JLabel(" ");

  private final JButton createButton = new JButton("Nuevo");
  private final JButton editButton = new JButton("Editar");
  private final JButton deleteButton = new JButton("Eliminar");

  private List<T> rows = List.of();

  protected CrudTab(Store store, String entity, String[] columns) {
    this.store = store;
    this.entity = entity;

    setLayout(new BorderLayout(8, 8));
    setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

    model = new DefaultTableModel(columns, 0) {
      private static final long serialVersionUID = 1L;

      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };

    table = new JTable(model);
    table.setRowHeight(24);
    table.getTableHeader().setReorderingAllowed(false);

    add(new JScrollPane(table), BorderLayout.CENTER);
    add(buildActions(), BorderLayout.SOUTH);

    refresh();
  }

  private JPanel buildActions() {
    createButton.addActionListener(event -> create());
    editButton.addActionListener(event -> edit());
    deleteButton.addActionListener(event -> delete());

    JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
    bar.add(createButton);
    bar.add(editButton);
    bar.add(deleteButton);
    bar.add(message);

    return bar;
  }

  @Override
  public void refresh() {
    rows = load();

    model.setRowCount(0);
    for (T item : rows) {
      model.addRow(toRow(item));
    }

    createButton.setEnabled(canCreate());
    editButton.setEnabled(canEdit());
    deleteButton.setEnabled(canDelete());
  }

  private void create() {
    run(() -> build(null).ifPresent(item -> {
      persist(item);
      refresh();
      info(entity + " creado correctamente.");
    }));
  }

  private void edit() {
    T selected = selected();

    if (selected == null) {
      info("Seleccione una fila para editar.");
      return;
    }

    run(() -> build(selected).ifPresent(item -> {
      persist(item);
      refresh();
      info(entity + " actualizado.");
    }));
  }

  private void delete() {
    T selected = selected();

    if (selected == null) {
      info("Seleccione una fila para eliminar.");
      return;
    }

    int choice = JOptionPane.showConfirmDialog(this,
        "¿Eliminar " + describe(selected) + "?", "Confirmar", JOptionPane.YES_NO_OPTION);

    if (choice != JOptionPane.YES_OPTION) {
      return;
    }

    run(() -> {
      if (remove(selected)) {
        refresh();
        info(entity + " eliminado.");
      }
    });
  }

  private T selected() {
    int row = table.getSelectedRow();

    return row < 0 || row >= rows.size() ? null : rows.get(row);
  }

  private void run(Runnable action) {
    try {
      action.run();
    } catch (RuntimeException exception) {
      String detail = exception.getMessage() == null
          ? exception.getClass().getSimpleName()
          : exception.getMessage();
      info(detail);
    }
  }

  protected void info(String text) {
    message.setText(text == null || text.isBlank() ? " " : text);
  }

  protected static <E extends Enum<E>> String[] names(E[] values) {
    String[] out = new String[values.length];

    for (int index = 0; index < values.length; index++) {
      out[index] = values[index].name();
    }

    return out;
  }

  protected abstract List<T> load();

  protected abstract Object[] toRow(T item);

  /** Opens the form, returning the entity to persist, or empty when cancelled. */
  protected abstract Optional<T> build(T existing);

  protected abstract void persist(T item);

  protected abstract boolean remove(T item);

  protected abstract String describe(T item);

  protected boolean canCreate() {
    return store.isAdmin();
  }

  protected boolean canEdit() {
    return store.isAdmin();
  }

  protected boolean canDelete() {
    return store.isAdmin();
  }
}
