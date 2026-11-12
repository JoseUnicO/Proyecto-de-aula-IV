package modelo;

/**
 * Copia física identificable de un libro. Ocupación reconstruida desde
 * préstamos.
 */
public final class Ejemplar {
    private final String id;
    private boolean prestado;

    public Ejemplar(String id) {
        this.id = java.util.Objects.requireNonNull(id);
    }

    public String getId() {
        return id;
    }

    public boolean isDisponible() {
        return !prestado;
    }

    public void restaurarPrestado(boolean valor) {
        prestado = valor;
    }

    public String toString() {
        return id + (prestado ? " — Prestado" : " — Disponible");
    }
}
