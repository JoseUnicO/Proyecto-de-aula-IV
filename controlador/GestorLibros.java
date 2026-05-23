package controlador;

import estructuras.ArbolBinario;
import modelo.Libro;
import excepciones.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase controladora que gestiona todas las operaciones relacionadas con los
 * libros.
 * Utiliza un Árbol Binario de Búsqueda para almacenar y organizar los libros de
 * forma eficiente.
 * Esta estructura permite búsquedas, inserciones y eliminaciones en tiempo
 * O(log n) en el caso promedio.
 */

public class GestorLibros {

    // Árbol binario que almacena todos los libros del sistema
    private ArbolBinario<Libro> arbolLibros;

    /**
     * Constructor que inicializa el árbol binario vacío.
     * Al utilizar un árbol binario, garantizamos que los libros se mantengan
     * ordenados
     * según su criterio de comparación (ID).
     */
    public GestorLibros() {
        arbolLibros = new ArbolBinario<>();
    }

    /**
     * Agrega un nuevo libro al sistema.
     * Valida que el libro no sea nulo y que no exista ya un libro con el mismo ID.
     * 
     * @param libro El objeto Libro a agregar
     * @throws OperacionInvalidaException si el libro es nulo o ya existe
     */
    public void agregarLibro(Libro libro) throws OperacionInvalidaException {
        // Validación: el libro no puede ser nulo
        if (libro == null) {
            throw new OperacionInvalidaException("El libro no puede ser nulo");
        }
        // Validación: no permitir libros duplicados (mismo ID)
        if (arbolLibros.contiene(libro)) {
            throw new OperacionInvalidaException("Ya existe un libro con ese ID: " + libro.getId());
        }
        // Insertar el libro en el árbol binario
        arbolLibros.insertar(libro);
    }

    /**
     * Busca un libro específico por su ID.
     * Crea un libro temporal con el ID buscado para realizar la comparación en el
     * árbol.
     * 
     * @param id El identificador único del libro
     * @return El objeto Libro encontrado
     * @throws ElementoNoEncontradoException si el libro no existe en el sistema
     */
    public Libro buscarLibro(String id) throws ElementoNoEncontradoException {
        try {
            // Crear un libro temporal con el ID buscado para la comparación
            Libro libroBusqueda = new Libro(id, "temp", "temp", "9781234567890", 2000, "temp");
            return arbolLibros.buscar(libroBusqueda);
        } catch (OperacionInvalidaException e) {
            throw new ElementoNoEncontradoException("Error al buscar libro: " + e.getMessage());
        }
    }

    /**
     * Verifica si existe un libro con el ID especificado.
     * Método auxiliar que simplifica la validación de existencia.
     * 
     * @param id El identificador del libro a verificar
     * @return true si el libro existe, false en caso contrario
     */
    public boolean existeLibro(String id) {
        try {
            buscarLibro(id);
            return true;
        } catch (ElementoNoEncontradoException e) {
            return false;
        }
    }

    /**
     * Elimina un libro del sistema.
     * Valida que el libro exista y que esté disponible (no prestado) antes de
     * eliminarlo.
     * Esto garantiza la integridad de los datos y evita eliminar libros en uso.
     * 
     * @param id El identificador del libro a eliminar
     * @throws ElementoNoEncontradoException si el libro no existe
     * @throws OperacionInvalidaException    si el libro está prestado
     */
    public void eliminarLibro(String id) throws ElementoNoEncontradoException, OperacionInvalidaException {
        // Primero buscar el libro para validar su existencia
        Libro libro = buscarLibro(id);
        // Validación de negocio: no se puede eliminar un libro prestado
        if (!libro.isDisponible()) {
            throw new OperacionInvalidaException("No se puede eliminar un libro prestado");
        }
        // Eliminar el libro del árbol binario
        arbolLibros.eliminar(libro);
    }

    /**
     * Muestra todos los libros del sistema ordenados por ID (recorrido in-orden del
     * árbol).
     * El recorrido in-orden de un árbol binario de búsqueda garantiza el orden
     * ascendente.
     * 
     * @throws EstructuraVaciaException si no hay libros en el sistema
     */
    public void mostrarLibrosOrdenados() throws EstructuraVaciaException {
        arbolLibros.recorrerEnOrden(libro -> System.out.println(libro));
    }

    /**
     * Busca y muestra todos los libros que pertenecen a una categoría específica.
     * Utiliza el recorrido in-orden para revisar todos los libros.
     * 
     * @param categoria La categoría a filtrar (ej: "Ficción", "Ciencia")
     * @throws EstructuraVaciaException si no hay libros en el sistema
     */
    public void buscarPorCategoria(String categoria) throws EstructuraVaciaException {
        arbolLibros.recorrerEnOrden(libro -> {
            // Comparación case-insensitive para mayor flexibilidad
            if (libro.getCategoria().equalsIgnoreCase(categoria)) {
                System.out.println(libro);
            }
        });
    }

    /**
     * Busca y muestra todos los libros de un autor específico.
     * Permite búsquedas parciales (el nombre del autor puede estar contenido).
     * 
     * @param autor El nombre del autor a buscar
     * @throws EstructuraVaciaException si no hay libros en el sistema
     */
    public void buscarPorAutor(String autor) throws EstructuraVaciaException {
        arbolLibros.recorrerEnOrden(libro -> {
            // Búsqueda flexible: permite coincidencias parciales en minúsculas
            if (libro.getAutor().toLowerCase().contains(autor.toLowerCase())) {
                System.out.println(libro);
            }
        });
    }

    /**
     * Cuenta cuántos libros están actualmente disponibles para préstamo.
     * Utiliza un array de un elemento para poder modificar el contador dentro de la
     * expresión lambda.
     * 
     * @return El número de libros disponibles
     */
    public int contarLibrosDisponibles() {
        // Array de un elemento para poder modificarlo
        final int[] contador = { 0 };
        try {
            arbolLibros.recorrerEnOrden(libro -> {
                if (libro.isDisponible()) {
                    contador[0]++;
                }
            });
        } catch (EstructuraVaciaException e) {
            return 0;
        }
        return contador[0];
    }

    /**
     * Obtiene una lista con todos los libros del sistema.
     * Útil para mostrar los libros en la interfaz gráfica o generar reportes.
     * 
     * @return Lista con todos los libros (puede estar vacía)
     */
    public List<Libro> obtenerTodosLibros() {
        List<Libro> libros = new ArrayList<>();
        try {
            // Recorrer el árbol y agregar cada libro a la lista
            arbolLibros.recorrerEnOrden(libro -> libros.add(libro));
        } catch (EstructuraVaciaException e) {
            // Si está vacío, simplemente devolver la lista vacía

        }
        return libros;
    }

    /**
     * Obtiene el número total de libros en el sistema.
     * 
     * @return Cantidad total de libros registrados
     */
    public int getTotalLibros() {
        return arbolLibros.size();
    }

    /**
     * Verifica si el sistema tiene libros registrados.
     * 
     * @return true si no hay libros, false si hay al menos uno
     */
    public boolean estaVacio() {
        return arbolLibros.estaVacio();
    }
}
