package com.dev.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.dev.domain.User;
import com.dev.security.Passwords;

/** Modal sign-in gate shown before the main window. */
public final class LoginDialog extends JDialog {

  private static final long serialVersionUID = 1L;

  private final transient Store store;

  private final JTextField emailField = new JTextField(18);
  private final JPasswordField passwordField = new JPasswordField(18);
  private final JLabel error = new JLabel(" ");

  private User authenticated;

  private LoginDialog(Frame owner, Store store) {
    super(owner, "PkgLog - Iniciar sesión", true);
    this.store = store;

    setLayout(new BorderLayout(8, 8));
    add(buildForm(), BorderLayout.CENTER);
    add(buildButtons(), BorderLayout.SOUTH);

    getRootPane().setDefaultButton(null);
    pack();
    setResizable(false);
    setLocationRelativeTo(owner);
  }

  /** Blocks until the user signs in or cancels; empty means "no session". */
  public static Optional<User> show(Frame owner, Store store) {
    LoginDialog dialog = new LoginDialog(owner, store);
    dialog.setVisible(true);
    return Optional.ofNullable(dialog.authenticated);
  }

  private JPanel buildForm() {
    JPanel form = new JPanel(new GridBagLayout());
    form.setBorder(BorderFactory.createEmptyBorder(16, 16, 4, 16));

    GridBagConstraints constraints = new GridBagConstraints();
    constraints.insets = new Insets(4, 4, 4, 4);
    constraints.anchor = GridBagConstraints.WEST;

    constraints.gridx = 0;
    constraints.gridy = 0;
    form.add(new JLabel("Usuario:"), constraints);

    constraints.gridx = 1;
    form.add(emailField, constraints);

    constraints.gridx = 0;
    constraints.gridy = 1;
    form.add(new JLabel("Contraseña:"), constraints);

    constraints.gridx = 1;
    form.add(passwordField, constraints);

    constraints.gridx = 1;
    constraints.gridy = 2;
    error.setForeground(new java.awt.Color(0xB00020));
    form.add(error, constraints);

    constraints.gridx = 1;
    constraints.gridy = 3;
    JLabel hint = new JLabel("Demo: admin / admin123");
    hint.setForeground(java.awt.Color.GRAY);
    form.add(hint, constraints);

    return form;
  }

  private JPanel buildButtons() {
    JButton signIn = new JButton("Entrar");
    signIn.addActionListener(event -> attempt());

    JButton cancel = new JButton("Salir");
    cancel.addActionListener(event -> {
      authenticated = null;
      dispose();
    });

    JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
    bar.add(cancel);
    bar.add(signIn);

    return bar;
  }

  private void attempt() {
    String email = emailField.getText().trim();
    String rawPassword = new String(passwordField.getPassword());

    Optional<User> found = store.findUserByEmail(email);

    if (found.isEmpty() || !Passwords.verify(rawPassword, found.get().password())) {
      error.setText("Credenciales inválidas.");
      passwordField.setText("");
      return;
    }

    authenticated = found.get();
    dispose();
  }
}
