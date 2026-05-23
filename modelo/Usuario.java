package modelo;

import excepciones.OperacionInvalidaException;

/**
 * Clase que representa un usuario en el sistema de biblioteca.
 * Controla la información básica del usuario, su límite de préstamos,
 * préstamos activos actuales y multas acumuladas.
 * Implementa Comparable para ordenar usuarios por su ID.
 */
public class Usuario implements Comparable<Usuario> {
    private String id;
    private String nombre;
    private String email;
    private int limitePrestamos;
    private int prestamosActuales;
    private double multaAcumulada;

    /**
     * Constructor que inicializa un nuevo usuario validando sus datos.
     * El usuario comienza sin préstamos activos ni multas.
     */
    public Usuario(String id, String nombre, String email, int limitePrestamos) throws OperacionInvalidaException {
        validarDatos(id, nombre, email, limitePrestamos);
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.limitePrestamos = limitePrestamos;
        this.prestamosActuales = 0;
        this.multaAcumulada = 0.0;
    }

    /**
     * Método privado para validar los datos de creación de usuario.
     * Lanza excepción si los datos no cumplen las reglas básicas de integridad.
     */
    private void validarDatos(String id, String nombre, String email, int limitePrestamos)
            throws OperacionInvalidaException {
        if (id == null || id.trim().isEmpty()) {
            throw new OperacionInvalidaException("El ID del usuario no puede estar vacío");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new OperacionInvalidaException("El nombre del usuario no puede estar vacío");
        }
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new OperacionInvalidaException("El email del usuario no es válido");
        }
        if (limitePrestamos <= 0) {
            throw new OperacionInvalidaException("El límite de préstamos debe ser mayor a 0");
        }
    }

    /**
     * Verifica si el usuario puede realizar un nuevo préstamo.
     * Debe estar por debajo del límite y no tener multas pendientes.
     */
    public boolean puedePedirPrestamo() {
        return prestamosActuales < limitePrestamos && multaAcumulada == 0;
    }

    /**
     * Incrementa el contador de préstamos actuales.
     * Lanza excepción si el usuario no puede pedir más préstamos.
     */
    public void agregarPrestamo() throws OperacionInvalidaException {
        if (!puedePedirPrestamo()) {
            throw new OperacionInvalidaException(
                    "El usuario ha alcanzado su límite de préstamos o tiene multas pendientes");
        }
        prestamosActuales++;
    }

    /**
     * Decrementa el contador de préstamos actuales.
     * Lanza excepción si no tiene préstamos activos para devolver.
     */
    public void devolverPrestamo() throws OperacionInvalidaException {
        if (prestamosActuales <= 0) {
            throw new OperacionInvalidaException("El usuario no tiene préstamos activos");
        }
        prestamosActuales--;
    }
    
    /**
     * Suma un monto a la multa acumulada del usuario.
     */
    public void agregarMulta(double monto) {
        this.multaAcumulada += monto;
    }

    /**
     * Permite pagar parcial o totalmente la multa acumulada.
     * Lanza excepción si el monto es inválido o excede la multa actual.
     */
    public void pagarMulta(double monto) throws OperacionInvalidaException {
        if (monto <= 0) {
            throw new OperacionInvalidaException("El monto a pagar debe ser mayor a 0");
        }
        if (monto > multaAcumulada) {
            throw new OperacionInvalidaException("El monto excede la multa acumulada");
        }
        this.multaAcumulada -= monto;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public int getLimitePrestamos() {
        return limitePrestamos;
    }

    public int getPrestamosActuales() {
        return prestamosActuales;
    }

    public double getMultaAcumulada() {
        return multaAcumulada;
    }

    /**
     * Compara dos usuarios para ordenarlos por su ID único.
     */
    @Override
    public int compareTo(Usuario otro) {
        return this.id.compareTo(otro.id);
    }

    /**
     * Compara igualdad entre usuarios basándose en su ID único.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Usuario usuario = (Usuario) obj;
        return id.equals(usuario.id);
    }

    /**
     * Código hash para uso en colecciones, generado a partir del ID.
     * 
     * @return Código hash basado en el ID
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Usuario[%s] %s - Email: %s - Préstamos: %d/%d - Multa: $%.2f",
                id, nombre, email, prestamosActuales, limitePrestamos, multaAcumulada);
    }
}