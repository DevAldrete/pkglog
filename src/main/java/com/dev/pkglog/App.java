package com.dev.pkglog;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.dev.db.Database;
import com.dev.db.Repositories;
import com.dev.domain.User;
import com.dev.ui.LoginDialog;
import com.dev.ui.MainFrame;
import com.dev.ui.Store;

public class App {

  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      useSystemLookAndFeel();

      Database database = Database.openDefault();
      Repositories repositories = Repositories.jdbc(database);
      Store store = new Store(repositories);

      Optional<User> session = LoginDialog.show(null, store);

      if (session.isEmpty()) {
        database.close();
        return;
      }

      store.setCurrentUser(session.get());

      MainFrame frame = new MainFrame(store);
      frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
      frame.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent event) {
          database.close();
          frame.dispose();
          System.exit(0);
        }
      });
      frame.setVisible(true);
    });
  }

  private static void useSystemLookAndFeel() {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception exception) {
      // Keep the default look and feel when the system one is unavailable.
    }
  }
}
