package estructuras;

/**
 * Clase de un nodo en una lista doblemente enlazada.
 * Cada nodo contiene un dato de tipo T,
 * una referencia al siguiente nodo de la lista,
 * y una referencia al nodo anterior.
 * Esta estructura permite la navegación eficiente en ambos sentidos.
 */
public class Nodo<T> {
    private T dato;
    private Nodo<T> siguiente;
    private Nodo<T> anterior;

    /**
     * Constructor que crea un nodo con el dato dado,
     * y referencias a siguiente y anterior nulas inicialmente.
     * 
     * @param dato El dato a almacenar en el nodo
     */
    public Nodo(T dato) {
        this.dato = dato;
        this.siguiente = null;
        this.anterior = null;
    }

    /**
     * Obtiene el dato almacenado en el nodo.
     * 
     * @return El dato del nodo
     */
    public T getDato() {
        return dato;
    }

    /**
     * Obtiene el nodo siguiente en la lista.
     * 
     * @return El nodo siguiente
     */
    public Nodo<T> getSiguiente() {
        return siguiente;
    }

    /**
     * Obtiene el nodo anterior en la lista.
     * 
     * @return El nodo anterior
     */
    public Nodo<T> getAnterior() {
        return anterior;
    }

    /**
     * Establece el nodo siguiente para este nodo.
     * 
     * @param siguiente Nodo que será el siguiente
     */
    public void setSiguiente(Nodo<T> siguiente) {
        this.siguiente = siguiente;
    }

    /**
     * Establece el nodo anterior para este nodo.
     * 
     * @param anterior Nodo que será el anterior
     */
    public void setAnterior(Nodo<T> anterior) {
        this.anterior = anterior;
    }
}
