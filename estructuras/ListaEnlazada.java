package estructuras;

/**
 * Implementación de una Lista Doblemente Enlazada.
 * Permite almacenar elementos con enlaces a nodos anteriores y siguientes,
 * facilitando inserciones y eliminaciones eficientes de ambos extremos
 * así como búsqueda y recorrido lineal.
 */
import excepciones.EstructuraVaciaException;
import java.util.function.Consumer;

public class ListaEnlazada<T> {

    private Nodo<T> cabeza; // Primer nodo de la lista
    private Nodo<T> cola; // Último nodo de la lista
    private int tamanio; // Cantidad de elementos en la lista

    /**
     * Constructor que inicializa una lista vacía.
     */
    public ListaEnlazada() {
        this.cabeza = null;
        this.cola = null;
        this.tamanio = 0;
    }

    /**
     * Agrega un nuevo elemento al final de la lista.
     * Si la lista está vacía, el nuevo nodo será cabeza y cola.
     * En caso contrario, se enlaza al final de la lista actual.
     */
    public void agregarAlFinal(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);

        if (estaVacia()) {
            cabeza = cola = nuevo;
        } else {
            cola.setSiguiente(nuevo);
            nuevo.setAnterior(cola);
            cola = nuevo;
        }
        tamanio++;
    }

    /**
     * Agrega un nuevo elemento al inicio de la lista.
     * Si está vacía, el nuevo nodo será cabeza y cola.
     * En caso contrario, se enlaza antes del nodo actual cabeza.
     */
    public void agregarAlInicio(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);

        if (estaVacia()) {
            cabeza = cola = nuevo;
        } else {
            nuevo.setSiguiente(cabeza);
            cabeza.setAnterior(nuevo);
            cabeza = nuevo;
        }
        tamanio++;
    }

    /**
     * Elimina el primer nodo de la lista y devuelve su dato.
     * Lanza excepción si la lista está vacía.
     * Actualiza referencias para mantener la integridad de la lista.
     */
    public T eliminarPrimero() throws EstructuraVaciaException {
        if (estaVacia()) {
            throw new EstructuraVaciaException("La lista está vacía");
        }

        T dato = cabeza.getDato();

        if (cabeza == cola) {

            cabeza = cola = null;
        } else {
            cabeza = cabeza.getSiguiente();
            cabeza.setAnterior(null);
        }

        tamanio--;
        return dato;
    }

    /**
     * Elimina el último nodo de la lista y devuelve su dato.
     * Lanza excepción si la lista está vacía.
     * Actualiza referencias adecuadamente.
     */
    public T eliminarUltimo() throws EstructuraVaciaException {
        if (estaVacia()) {
            throw new EstructuraVaciaException("La lista está vacía");
        }

        T dato = cola.getDato();

        if (cabeza == cola) {

            cabeza = cola = null;
        } else {
            cola = cola.getAnterior();
            cola.setSiguiente(null);
        }

        tamanio--;
        return dato;
    }

    /**
     * Elimina el primer nodo que contenga el dato especificado.
     * Actualiza referencias para mantener la estructura.
     *
     * @param dato El dato a eliminar.
     * @return true si la eliminación fue exitosa, false si no se encontró el dato.
     */
    public boolean eliminar(T dato) {
        if (estaVacia()) {
            return false;
        }

        Nodo<T> actual = cabeza;

        while (actual != null) {
            if (actual.getDato().equals(dato)) {
                if (actual == cabeza) {
                    cabeza = actual.getSiguiente();
                    if (cabeza != null) {
                        cabeza.setAnterior(null);
                    }
                }

                if (actual == cola) {
                    cola = actual.getAnterior();
                    if (cola != null) {
                        cola.setSiguiente(null);
                    }
                }

                if (actual.getAnterior() != null) {
                    actual.getAnterior().setSiguiente(actual.getSiguiente());
                }

                if (actual.getSiguiente() != null) {
                    actual.getSiguiente().setAnterior(actual.getAnterior());
                }

                tamanio--;
                return true;
            }
            actual = actual.getSiguiente();
        }

        return false;
    }

    /**
     * Retorna el primer elemento de la lista sin eliminarlo.
     * Lanza excepción si la lista está vacía.
     * Útil para acceder rápidamente al elemento que se encuentra en la cabeza
     * (inicio).
     */
    public T obtenerPrimero() throws EstructuraVaciaException {
        if (estaVacia()) {
            throw new EstructuraVaciaException("La lista está vacía");
        }
        return cabeza.getDato();
    }

    /**
     * Retorna el último elemento de la lista sin eliminarlo.
     * Lanza excepción si la lista está vacía.
     * Permite conocer rápidamente el dato almacenado en el nodo final.
     */
    public T obtenerUltimo() throws EstructuraVaciaException {
        if (estaVacia()) {
            throw new EstructuraVaciaException("La lista está vacía");
        }
        return cola.getDato();
    }

    /**
     * Busca el primer nodo que contenga el dato especificado y lo devuelve.
     * Retorna null si no se encuentra el dato.
     */
    public T buscar(T dato) {
        Nodo<T> actual = cabeza;

        while (actual != null) {
            if (actual.getDato().equals(dato)) {
                return actual.getDato();
            }
            actual = actual.getSiguiente();
        }

        return null;
    }

    /**
     * Ejecuta una acción para cada elemento de la lista en orden desde la cabeza.
     * Lanza excepción si la lista está vacía.
     *
     * @param accion La acción a aplicar a cada elemento.
     */
    public void recorrer(Consumer<T> accion) throws EstructuraVaciaException {
        if (estaVacia()) {
            throw new EstructuraVaciaException("La lista está vacía");
        }

        Nodo<T> actual = cabeza;
        while (actual != null) {
            accion.accept(actual.getDato());
            actual = actual.getSiguiente();
        }
    }

    /**
     * Muestra por consola los elementos de la lista con su posición.
     */
    public void mostrar() {
        if (estaVacia()) {
            System.out.println("La lista está vacía.");
            return;
        }

        Nodo<T> actual = cabeza;
        int posicion = 1;

        while (actual != null) {
            System.out.println(posicion + ". " + actual.getDato());
            actual = actual.getSiguiente();
            posicion++;
        }
    }

    /**
     * Indica si la lista está vacía, lo que ocurre si la cabeza es null.
     *
     * @return true si vacía, false si contiene elementos.
     */
    public boolean estaVacia() {
        return cabeza == null;
    }

    /**
     * Retorna la cantidad de elementos almacenados en la lista.
     */
    public int size() {
        return tamanio;
    }

    /**
     * Vacía la lista eliminando todas las referencias y reseteando el tamaño.
     */
    public void limpiar() {
        cabeza = null;
        cola = null;
        tamanio = 0;
    }

    // Métodos para acceder directamente a la cabeza y cola, si es necesario
    public Nodo<T> getCabeza() {
        return cabeza;
    }

    public Nodo<T> getCola() {
        return cola;
    }
}
