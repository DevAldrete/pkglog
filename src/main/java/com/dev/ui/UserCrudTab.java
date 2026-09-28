package com.dev.ui;

import java.util.List;
import java.util.Optional;

import com.dev.domain.Role;
import com.dev.domain.User;
import com.dev.security.Passwords;

/** CRUD over application users (admin only). */
public final class UserCrudTab extends CrudTab<User> {

  private static final long serialVersionUID = 1L;

  public UserCrudTab(Store store) {
    super(store, "Usuario", new String[] { "Id", "Nombre", "Usuario", "Rol" });
  }

  @Override
  protected List<User> load() {
    return store.users();
  }

  @Override
  protected Object[] toRow(User user) {
    return new Object[] { user.id(), user.name(), user.email(), user.role() };
  }

  @Override
  protected Optional<User> build(User existing) {
    FormDialog form = new FormDialog()
        .text("name", "Nombre", existing == null ? "" : existing.name())
        .text("email", "Usuario", existing == null ? "" : existing.email())
        .combo("role", "Rol", names(Role.values()),
            existing == null ? Role.USER.name() : existing.role().name())
        .text("password", existing == null ? "Contraseña" : "Contraseña (vacío = sin cambio)", "");

    return form.show(this, existing == null ? "Nuevo usuario" : "Editar usuario")
        .map(values -> {
          String rawPassword = values.text("password");

          if (existing == null && rawPassword.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
          }

          String password = rawPassword.isBlank()
              ? existing.password()
              : Passwords.hash(rawPassword);

          return new User(
              existing == null ? store.nextUserId() : existing.id(),
              values.text("name"),
              values.text("email"),
              password,
              Role.valueOf(values.text("role")));
        });
  }

  @Override
  protected void persist(User item) {
    store.saveUser(item);
  }

  @Override
  protected boolean remove(User item) {
    if (store.currentUser() != null && store.currentUser().id() == item.id()) {
      throw new IllegalArgumentException("No puede eliminar su propia cuenta");
    }

    return store.deleteUser(item.id());
  }

  @Override
  protected String describe(User item) {
    return item.email();
  }
}
