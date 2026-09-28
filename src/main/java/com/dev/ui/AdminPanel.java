package com.dev.ui;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/** Central place to manage every master-data table. */
public final class AdminPanel extends JPanel implements Refreshable {

  private static final long serialVersionUID = 1L;

  private final List<Refreshable> tabs = new ArrayList<>();

  public AdminPanel(Store store) {
    setLayout(new BorderLayout());

    JTabbedPane inner = new JTabbedPane();
    addTab(inner, "Paquetes", new PackageCrudTab(store));
    addTab(inner, "Rutas", new RouteCrudTab(store));
    addTab(inner, "Zonas", new ZoneCrudTab(store));
    addTab(inner, "Centros", new CenterCrudTab(store));
    addTab(inner, "Vehículos", new VehicleCrudTab(store));
    addTab(inner, "Usuarios", new UserCrudTab(store));

    add(inner, BorderLayout.CENTER);
  }

  private void addTab(JTabbedPane pane, String title, CrudTab<?> tab) {
    pane.addTab(title, tab);
    tabs.add(tab);
  }

  @Override
  public void refresh() {
    tabs.forEach(Refreshable::refresh);
  }
}
