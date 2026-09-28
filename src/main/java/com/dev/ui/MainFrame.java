package com.dev.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.dev.io.CsvTables;

/** Main window: one tab per concern, refreshed from the store when selected. */
public final class MainFrame extends JFrame {

  private static final long serialVersionUID = 1L;

  private final transient Store store;

  public MainFrame(Store store) {
    super("PkgLog - Sistema de Gestión Logística");
    this.store = store;

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setJMenuBar(buildMenuBar());

    JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Resumen", new OverviewPanel(store));
    tabs.addTab("Despacho", new DispatchPanel(store));
    tabs.addTab("Rastreo", new TrackingPanel(store));
    tabs.addTab("Red", new NetworkPanel(store));
    tabs.addTab("Repartos", new PartitionPanel(store));
    tabs.addTab("Centros", new CentersPanel(store));
    tabs.addTab("Reportes", new ReportsPanel(store));
    tabs.addTab("Datos", new AdminPanel(store));
    tabs.addTab("Auditoría", new AuditPanel(store));

    tabs.addChangeListener(event -> {
      Component selected = tabs.getSelectedComponent();
      if (selected instanceof Refreshable refreshable) {
        refreshable.refresh();
      }
    });

    add(tabs, BorderLayout.CENTER);
    add(buildStatusBar(store), BorderLayout.SOUTH);

    setSize(1120, 740);
    setMinimumSize(new Dimension(960, 640));
    setLocationRelativeTo(null);
  }

  private static JPanel buildStatusBar(Store store) {
    JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 4));

    String name = store.currentUser() == null ? "invitado" : store.currentUser().name();
    String role = store.currentUser() == null ? "-" : store.currentUser().role().name();

    bar.add(new JLabel("Sesión: " + name + " (" + role + ")"));
    bar.add(new JLabel(" | "));
    bar.add(new JLabel("Datos: SQLite"));

    return bar;
  }

  private JMenuBar buildMenuBar() {
    JMenuItem exit = new JMenuItem("Salir");
    exit.addActionListener(event -> System.exit(0));

    JMenu file = new JMenu("Archivo");
    file.add(exportItem("Exportar paquetes (CSV)", "paquetes.csv",
        () -> CsvTables.exportPackages(store.packages())));
    file.add(exportItem("Exportar rutas (CSV)", "rutas.csv",
        () -> CsvTables.exportRoutes(store.routes())));
    file.add(exportItem("Exportar zonas (CSV)", "zonas.csv",
        () -> CsvTables.exportZones(store.zones())));
    file.add(exportItem("Exportar vehículos (CSV)", "vehiculos.csv",
        () -> CsvTables.exportVehicles(store.vehicles())));
    file.add(exportItem("Exportar centros (CSV)", "centros.csv",
        () -> CsvTables.exportCenters(store.centers())));
    file.addSeparator();

    JMenuItem importPackages = new JMenuItem("Importar paquetes (CSV)...");
    importPackages.addActionListener(event -> importPackages());
    file.add(importPackages);

    file.addSeparator();
    file.add(exit);

    JMenuBar menuBar = new JMenuBar();
    menuBar.add(file);

    return menuBar;
  }

  private JMenuItem exportItem(String title, String defaultName, java.util.function.Supplier<String> content) {
    JMenuItem item = new JMenuItem(title + "...");
    item.addActionListener(event -> export(defaultName, content.get()));
    return item;
  }

  private void export(String defaultName, String content) {
    JFileChooser chooser = new JFileChooser();
    chooser.setSelectedFile(new File(defaultName));

    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }

    try {
      Files.writeString(chooser.getSelectedFile().toPath(), content);
      JOptionPane.showMessageDialog(this, "Exportado a " + chooser.getSelectedFile().getName());
    } catch (IOException exception) {
      JOptionPane.showMessageDialog(this, "Error al exportar: " + exception.getMessage());
    }
  }

  private void importPackages() {
    JFileChooser chooser = new JFileChooser();

    if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }

    try {
      String content = Files.readString(chooser.getSelectedFile().toPath());
      int imported = store.importPackages(CsvTables.importPackages(content));
      JOptionPane.showMessageDialog(this, "Paquetes importados: " + imported);
    } catch (IOException | RuntimeException exception) {
      JOptionPane.showMessageDialog(this, "Error al importar: " + exception.getMessage());
    }
  }
}
