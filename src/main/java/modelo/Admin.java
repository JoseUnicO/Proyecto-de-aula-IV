package modelo;

/** Identidad autenticada; nunca contiene contraseñas. */
public record Admin(long id, String usuario, String nombre) {
    public String getUsuario() {
        return usuario;
    }

    public String getNombre() {
        return nombre;
    }
}
