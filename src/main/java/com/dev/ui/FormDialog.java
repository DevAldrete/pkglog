package com.dev.ui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Small builder for modal create/edit forms. Fields are addressed by key and
 * parsed on demand, keeping each entity dialog to a handful of lines.
 */
public final class FormDialog {

  private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private final JPanel panel = new JPanel(new GridBagLayout());
  private final Map<String, JTextField> texts = new LinkedHashMap<>();
  private final Map<String, JComboBox<String>> combos = new LinkedHashMap<>();
  private int row;

  public FormDialog() {
    panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
  }

  public FormDialog text(String key, String label, String value) {
    JTextField field = new JTextField(value == null ? "" : value, 18);
    texts.put(key, field);
    addRow(label, field);
    return this;
  }

  public FormDialog combo(String key, String label, String[] options, String value) {
    JComboBox<String> box = new JComboBox<>(options);
    if (value != null) {
      box.setSelectedItem(value);
    }
    combos.put(key, box);
    addRow(label, box);
    return this;
  }

  private void addRow(String label, java.awt.Component field) {
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.insets = new Insets(4, 4, 4, 4);
    constraints.anchor = GridBagConstraints.WEST;

    constraints.gridx = 0;
    constraints.gridy = row;
    panel.add(new JLabel(label), constraints);

    constraints.gridx = 1;
    panel.add(field, constraints);

    row++;
  }

  /** Shows the form; empty means the user cancelled. */
  public Optional<Values> show(Component parent, String title) {
    int choice = JOptionPane.showConfirmDialog(parent, panel, title,
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

    if (choice != JOptionPane.OK_OPTION) {
      return Optional.empty();
    }

    Map<String, String> values = new LinkedHashMap<>();
    texts.forEach((key, field) -> values.put(key, field.getText().trim()));
    combos.forEach((key, box) -> values.put(key, String.valueOf(box.getSelectedItem())));

    return Optional.of(new Values(values));
  }

  /** Typed accessors over the captured key/value map. */
  public record Values(Map<String, String> map) {

    public String text(String key) {
      return map.getOrDefault(key, "");
    }

    public int intValue(String key) {
      return Integer.parseInt(require(key));
    }

    public double doubleValue(String key) {
      return Double.parseDouble(require(key).replace(",", "."));
    }

    public LocalDateTime dateTime(String key) {
      return LocalDateTime.parse(require(key), DATE_TIME);
    }

    private String require(String key) {
      String value = map.get(key);
      if (value == null || value.isBlank()) {
        throw new IllegalArgumentException("El campo " + key + " es obligatorio");
      }
      return value;
    }
  }
}
