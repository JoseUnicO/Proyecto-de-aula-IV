package modelo;

/**
 * Clase que representa un Administrador del sistema de biblioteca.
 * Esta clase encapsula la información de autenticación y datos básicos
 * de un usuario con privilegios administrativos.
 */
public class Admin {
    private String usuario;
    private String password;
    private String nombre;

    public Admin(String usuario, String password, String nombre) {
        this.usuario = usuario;
        this.password = password;
        this.nombre = nombre;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getPassword() {
        return password;
    }

    public String getNombre() {
        return nombre;
    }

    /**
     * Valida las credenciales de acceso del administrador.
     * Compara el usuario y contraseña proporcionados con los almacenados en el objeto.
     * Utiliza el método equals() para comparación de cadenas de forma segura.
     */  
    public boolean validarCredenciales(String usuario, String password) {
        return this.usuario.equals(usuario) && this.password.equals(password);
    }

    @Override
    public String toString() {
        return String.format("Admin[%s] %s", usuario, nombre);
    }
}

