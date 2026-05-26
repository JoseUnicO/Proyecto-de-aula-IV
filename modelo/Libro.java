package modelo;

import excepciones.OperacionInvalidaException;

/**
 * Clase que representa un Libro en el sistema de biblioteca.
 * Implementa la interfaz Comparable para permitir ordenamiento basado en el ID.
 * Esta clase incluye validación de datos, control de disponibilidad y métodos
 * fundamentales para el manejo del ciclo de préstamo.
 */
public class Libro implements Comparable<Libro> {
    private String id;
    private String titulo;
    private String autor;
    private String isbn;
    private int anioPublicacion;
    private String categoria;
    private boolean disponible;

    /**
     * Constructor que crea una instancia de Libro con validación previa de los datos.
     * Inicializa el estado de disponibilidad como true (disponible).
     */ 
    public Libro(String id, String titulo, String autor, String isbn, int anioPublicacion, String categoria)
            throws OperacionInvalidaException {
        validarDatos(id, titulo, autor, isbn, anioPublicacion, categoria);
        this.id = id;
        this.titulo = titulo;
        this.autor = autor;
        this.isbn = isbn;
        this.anioPublicacion = anioPublicacion;
        this.categoria = categoria;
        this.disponible = true;
    }

    /**
     * Método privado que valida los datos de un libro antes de asignarlos.
     * Lanza OperacionInvalidaException si algún dato es inválido, ayudando a controlar
     * errores y mantener la integridad de los objetos Libro.
     */
    private void validarDatos(String id, String titulo, String autor, String isbn, int anio, String categoria)
            throws OperacionInvalidaException {
        if (id == null || id.trim().isEmpty()) {
            throw new OperacionInvalidaException("El ID del libro no puede estar vacío");
        }
        if (titulo == null || titulo.trim().isEmpty()) {
            throw new OperacionInvalidaException("El título del libro no puede estar vacío");
        }
        if (autor == null || autor.trim().isEmpty()) {
            throw new OperacionInvalidaException("El autor del libro no puede estar vacío");
        }
        if (isbn == null || !isbn.matches("^(97(8|9))?\\d{9}(\\d|X)$")) {
            throw new OperacionInvalidaException("El ISBN del libro no es válido");
        }
        if (anio < 1000 || anio > 2025) {
            throw new OperacionInvalidaException("El año de publicación no es válido");
        }
        if (categoria == null || categoria.trim().isEmpty()) {
            throw new OperacionInvalidaException("La categoría del libro no puede estar vacía");
        }
    }

    /**
     * Marca el libro como prestado cambiando su estado de disponibilidad a false.
     * Lanza OperacionInvalidaException si ya está prestado para evitar inconsistencias.
     */
    public void prestar() throws OperacionInvalidaException {
        if (!disponible) {
            throw new OperacionInvalidaException("El libro ya está prestado");
        }
        disponible = false;
    }

    /**
     * Marca el libro como disponible cambiando su estado de disponibilidad a true.
     * Lanza OperacionInvalidaException si el libro no está prestado para evitar errores lógicos.
     */   
    public void devolver() throws OperacionInvalidaException {
        if (disponible) {
            throw new OperacionInvalidaException("El libro no está prestado");
        }
        disponible = true;
    }

    /**
     * Establece la disponibilidad directamente sin validaciones de negocio.
     * EXCLUSIVO para uso de la capa de persistencia al restaurar el estado.
     */
    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public String getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getAutor() {
        return autor;
    }

    public String getIsbn() {
        return isbn;
    }

    public int getAnioPublicacion() {
        return anioPublicacion;
    }

    public String getCategoria() {
        return categoria;
    }

    public boolean isDisponible() {
        return disponible;
    }

    /**
     * Compara esta instancia de Libro con otro basado en su id para propósitos de ordenamiento.
     * Implementación obligatoria por la interfaz Comparable.
     */ 
    @Override
    public int compareTo(Libro otro) {
        return this.id.compareTo(otro.id);
    }
    
    /**
     * Método sobrescrito para comparar igualdad entre objetos Libro basándose en su id único.
    */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Libro libro = (Libro) obj;
        return id.equals(libro.id);
    }

    /**
     * Método sobrescrito para generar código hash basado en el id,
     * necesario para usar objetos Libro en colecciones que lo requieran.
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Libro[%s] %s - %s (%d) - %s - %s",
                id, titulo, autor, anioPublicacion, categoria,
                disponible ? "Disponible" : "Prestado");
    }
}