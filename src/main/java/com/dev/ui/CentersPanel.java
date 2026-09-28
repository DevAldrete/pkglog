package com.dev.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.dev.domain.DistributionCenter;
import com.dev.domain.Vehicle;
import com.dev.ds.BinaryTree;
import com.dev.ds.Tree;
import com.dev.modules.Centers;

/** Distribution hierarchy (n-ary tree) and fleet ordered by capacity (BST). */
public final class CentersPanel extends JPanel implements Refreshable {

  private static final long serialVersionUID = 1L;

  private final transient Store store;

  private final JTextArea hierarchyArea = new JTextArea();
  private final DefaultTableModel model;
  private final JTextField idField = new JTextField(4);
  private final JTextField plateField = new JTextField(8);
  private final JTextField capacityField = new JTextField(6);
  private final JLabel message = new JLabel(" ");

  public CentersPanel(Store store) {
    this.store = store;

    setLayout(new BorderLayout(8, 8));
    setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

    model = new DefaultTableModel(new Object[] { "Id", "Placa", "Capacidad" }, 0) {
      private static final long serialVersionUID = 1L;

      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };

    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildHierarchyPanel(),
        buildFleetPanel());
    split.setResizeWeight(0.5);

    add(split, BorderLayout.CENTER);

    refresh();
  }

  private JPanel buildHierarchyPanel() {
    hierarchyArea.setEditable(false);
    hierarchyArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createTitledBorder("Jerarquía de centros (árbol)"));

    panel.add(new JLabel("Nacional -> Regional -> Local"), BorderLayout.NORTH);
    panel.add(new JScrollPane(hierarchyArea), BorderLayout.CENTER);

    return panel;
  }

  private JPanel buildFleetPanel() {
    JPanel panel = new JPanel(new BorderLayout(8, 8));
    panel.setBorder(BorderFactory.createTitledBorder("Flota por capacidad (árbol binario)"));

    JTable table = new JTable(model);
    table.setRowHeight(24);
    table.getTableHeader().setReorderingAllowed(false);
    panel.add(new JScrollPane(table), BorderLayout.CENTER);

    JPanel addBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
    addBar.add(new JLabel("Id:"));
    addBar.add(idField);
    addBar.add(new JLabel("Placa:"));
    addBar.add(plateField);
    addBar.add(new JLabel("Capacidad (kg):"));
    addBar.add(capacityField);

    JButton add = new JButton("Agregar");
    add.addActionListener(event -> addVehicle());
    addBar.add(add);
    addBar.add(message);

    panel.add(addBar, BorderLayout.SOUTH);

    return panel;
  }

  @Override
  public void refresh() {
    renderHierarchy();
    renderFleet();
  }

  private void renderHierarchy() {
    Tree<DistributionCenter> tree = Centers.hierarchy(store.centers());
    StringBuilder text = new StringBuilder();

    appendNode(tree.root(), text, 0);

    hierarchyArea.setText(text.toString());
    hierarchyArea.setCaretPosition(0);
  }

  private void appendNode(Tree.Node<DistributionCenter> node, StringBuilder text, int depth) {
    text.append("  ".repeat(depth));

    if (depth > 0) {
      text.append("- ");
    }

    DistributionCenter center = node.value();
    text.append(center.name()).append(" [").append(center.level()).append("]").append('\n');

    for (Tree.Node<DistributionCenter> child : node.children()) {
      appendNode(child, text, depth + 1);
    }
  }

  private void renderFleet() {
    BinaryTree<Vehicle> tree = new BinaryTree<>();

    for (Vehicle vehicle : store.vehicles()) {
      tree.push(vehicle);
    }

    model.setRowCount(0);
    for (Vehicle vehicle : tree.asList(BinaryTree.Order.INORDER)) {
      model.addRow(new Object[] { vehicle.id(), vehicle.plate(), Format.weight(vehicle.capacityKg()) });
    }
  }

  private void addVehicle() {
    try {
      int id = Integer.parseInt(idField.getText().trim());
      String plate = plateField.getText().trim();
      float capacity = Float.parseFloat(capacityField.getText().trim());

      store.saveVehicle(new Vehicle(id, plate, capacity));

      idField.setText("");
      plateField.setText("");
      capacityField.setText("");
      message.setText("Agregado: " + plate);

      refresh();
    } catch (NumberFormatException exception) {
      message.setText("Id y capacidad deben ser numéricos");
    } catch (IllegalArgumentException exception) {
      message.setText(exception.getMessage());
    }
  }
}
