package estructuras;

import excepciones.ElementoNoEncontradoException;
import excepciones.EstructuraVaciaException;

/**
 * Implementación de un Árbol Binario de Búsqueda (ABB).
 * Permite gestionar elementos ordenados que implementen Comparable,
 * y proporciona operaciones eficientes (inserción, búsqueda, eliminación)
 * con complejidad promedio O(log n) si está balanceado.
 */
public class ArbolBinario<T extends Comparable<T>> {

    /**
     * Clase interna que representa un nodo individual del árbol.
     * Cada nodo almacena el dato, y referencias a los subárboles izquierdo y
     * derecho.
     */
    private class NodoArbol {
        T dato;
        NodoArbol izquierdo;
        NodoArbol derecho;

        NodoArbol(T dato) {
            this.dato = dato;
        }
    }

    // Raíz del árbol
    private NodoArbol raiz;
    // Número total de elementos almacenados
    private int tamano;

    /**
     * Constructor: Inicializa el árbol vacío.
     */
    public ArbolBinario() {
        this.raiz = null;
        this.tamano = 0;
    }

    /**
     * Inserta un nuevo elemento en el árbol, manteniendo el orden.
     * Si el elemento es nulo, lanza una excepción, y si ya existe, no se inserta
     * duplicado.
     */
    public void insertar(T dato) {
        if (dato == null) {
            throw new IllegalArgumentException("No se puede insertar un elemento nulo");
        }
        raiz = insertarRecursivo(raiz, dato);
    }

    /**
     * Método recursivo privado para insertar un elemento.
     * Inserta en el subárbol correcto según el resultado de compareTo.
     * Incrementa el tamaño al agregar un nuevo nodo.
     */
    private NodoArbol insertarRecursivo(NodoArbol actual, T dato) {
        if (actual == null) {
            tamano++;
            return new NodoArbol(dato);
        }

        int comparacion = dato.compareTo(actual.dato);
        if (comparacion < 0) {
            actual.izquierdo = insertarRecursivo(actual.izquierdo, dato);
        } else if (comparacion > 0) {
            actual.derecho = insertarRecursivo(actual.derecho, dato);
        }

        // Si comparacion == 0, el dato ya existe y no se inserta (no permite
        // duplicados)
        return actual;
    }

    /**
     * Busca y retorna un elemento igual al argumento.
     * Lanza excepción si no lo encuentra o si el dato es nulo.
     */
    public T buscar(T dato) throws ElementoNoEncontradoException {
        if (dato == null) {
            throw new IllegalArgumentException("No se puede buscar un elemento nulo");
        }
        NodoArbol encontrado = buscarRecursivo(raiz, dato);
        if (encontrado == null) {
            throw new ElementoNoEncontradoException("Elemento no encontrado en el árbol");
        }
        return encontrado.dato;
    }

    /**
     * Búsqueda recursiva estándar en árbol binario.
     */
    private NodoArbol buscarRecursivo(NodoArbol actual, T dato) {
        if (actual == null) {
            return null;
        }

        int comparacion = dato.compareTo(actual.dato);
        if (comparacion == 0) {
            return actual;
        } else if (comparacion < 0) {
            return buscarRecursivo(actual.izquierdo, dato);
        } else {
            return buscarRecursivo(actual.derecho, dato);
        }
    }

    /**
     * Verifica si el árbol contiene el elemento solicitado.
     */
    public boolean contiene(T dato) {
        if (dato == null) {
            return false;
        }
        return buscarRecursivo(raiz, dato) != null;
    }

    /**
     * Elimina un elemento del árbol. Ajusta punteros para mantener
     * el orden binario. Lanza excepción si el dato es nulo o no existe.
     *
     * @return El dato eliminado.
     */
    public T eliminar(T dato) throws ElementoNoEncontradoException {
        if (dato == null) {
            throw new IllegalArgumentException("No se puede eliminar un elemento nulo");
        }
        if (!contiene(dato)) {
            throw new ElementoNoEncontradoException("Elemento no encontrado para eliminar");
        }
        T datoEliminado = buscar(dato);
        raiz = eliminarRecursivo(raiz, dato);
        tamano--;
        return datoEliminado;
    }

    /**
     * Elimina usando reemplazo por el menor del subárbol derecho, si es necesario.
     * Maneja los tres casos clásicos: hoja, un hijo, dos hijos.
     */
    private NodoArbol eliminarRecursivo(NodoArbol actual, T dato) {
        if (actual == null) {
            return null;
        }

        int comparacion = dato.compareTo(actual.dato);

        if (comparacion < 0) {
            actual.izquierdo = eliminarRecursivo(actual.izquierdo, dato);
        } else if (comparacion > 0) {
            actual.derecho = eliminarRecursivo(actual.derecho, dato);
        } else {

            // Caso: solo hijo derecho o nulo
            if (actual.izquierdo == null) {
                return actual.derecho;
                // Caso: solo hijo izquierdo o nulo
            } else if (actual.derecho == null) {
                return actual.izquierdo;
            }

            // Caso: dos hijos, se reemplaza con el valor mínimo del subárbol derecho
            actual.dato = encontrarMinimo(actual.derecho);
            actual.derecho = eliminarRecursivo(actual.derecho, actual.dato);
        }

        return actual;
    }

    /**
     * Encuentra y retorna el dato mínimo en un subárbol (el nodo más a la
     * izquierda).
     */
    private T encontrarMinimo(NodoArbol nodo) {
        while (nodo.izquierdo != null) {
            nodo = nodo.izquierdo;
        }
        return nodo.dato;
    }

    /**
     * Realiza un recorrido in-orden, aplicando la acción dada a cada elemento.
     * Lanza excepción si el árbol está vacío.
     * Permite obtener los elementos ordenados de menor a mayor.
     */
    public void recorrerEnOrden(java.util.function.Consumer<T> accion) throws EstructuraVaciaException {
        if (estaVacio()) {
            throw new EstructuraVaciaException("El árbol está vacío");
        }
        recorrerEnOrdenRecursivo(raiz, accion);
    }

    private void recorrerEnOrdenRecursivo(NodoArbol nodo, java.util.function.Consumer<T> accion) {
        if (nodo != null) {
            recorrerEnOrdenRecursivo(nodo.izquierdo, accion);
            accion.accept(nodo.dato);
            recorrerEnOrdenRecursivo(nodo.derecho, accion);
        }
    }

    /**
     * Recorrido pre-orden: primero el nodo, luego subárbol izquierdo, luego
     * derecho.
     * Útil para replicar la estructura del árbol.
     */
    public void recorrerPreOrden(java.util.function.Consumer<T> accion) throws EstructuraVaciaException {
        if (estaVacio()) {
            throw new EstructuraVaciaException("El árbol está vacío");
        }
        recorrerPreOrdenRecursivo(raiz, accion);
    }

    private void recorrerPreOrdenRecursivo(NodoArbol nodo, java.util.function.Consumer<T> accion) {
        if (nodo != null) {
            accion.accept(nodo.dato);
            recorrerPreOrdenRecursivo(nodo.izquierdo, accion);
            recorrerPreOrdenRecursivo(nodo.derecho, accion);
        }
    }

    /**
     * Recorrido post-orden: subárbol izquierdo, subárbol derecho, luego el nodo.
     * Útil para operaciones de borrado/recolección.
     */
    public void recorrerPostOrden(java.util.function.Consumer<T> accion) throws EstructuraVaciaException {
        if (estaVacio()) {
            throw new EstructuraVaciaException("El árbol está vacío");
        }
        recorrerPostOrdenRecursivo(raiz, accion);
    }

    private void recorrerPostOrdenRecursivo(NodoArbol nodo, java.util.function.Consumer<T> accion) {
        if (nodo != null) {
            recorrerPostOrdenRecursivo(nodo.izquierdo, accion);
            recorrerPostOrdenRecursivo(nodo.derecho, accion);
            accion.accept(nodo.dato);
        }
    }

    /**
     * Devuelve el número total de elementos en el árbol.
     */
    public int size() {
        return tamano;
    }

    /**
     * Retorna true si el árbol está vacío.
     */
    public boolean estaVacio() {
        return raiz == null;
    }

    /**
     * Elimina todos los elementos del árbol, dejándolo vacío.
     */
    public void limpiar() {
        raiz = null;
        tamano = 0;
    }

    /**
     * Calcula la altura máxima del árbol (el número de niveles).
     * Mide la eficiencia potencial de las operaciones (cuanto menor, mejor).
     */
    public int altura() {
        return calcularAltura(raiz);
    }

    private int calcularAltura(NodoArbol nodo) {
        if (nodo == null) {
            return 0;
        }
        return 1 + Math.max(calcularAltura(nodo.izquierdo), calcularAltura(nodo.derecho));
    }
}
