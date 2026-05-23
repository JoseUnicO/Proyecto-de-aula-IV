package estructuras;

import excepciones.EstructuraVaciaException;

/**
 * Implementación de una Pila.
 * Utiliza nodos enlazados para almacenar los elementos en orden de inserción,
 * con una referencia a la cima para operaciones eficientes de apilar y
 * desapilar.
 */
public class Pila<T> {
    private Nodo<T> cima; // Referencia al nodo que está en la cima de la pila
    private int tamano; // Número de elementos en la pila

    /**
     * Constructor que inicializa una pila vacía.
     */
    public Pila() {
        this.cima = null;
        this.tamano = 0;
    }

    /**
     * Agrega un elemento a la cima de la pila.
     * Lanza excepción si el dato es nulo.
     * 
     * @param dato Elemento a apilar.
     */
    public void apilar(T dato) {
        if (dato == null) {
            throw new IllegalArgumentException("No se puede apilar un elemento nulo");
        }
        Nodo<T> nuevo = new Nodo<>(dato);
        nuevo.setSiguiente(cima); // El nuevo nodo apunta al actual cima
        cima = nuevo; // La cima ahora es el nuevo nodo
        tamano++;
    }

    /**
     * Elimina y retorna el elemento en la cima de la pila.
     * Lanza excepción si la pila está vacía.
     * 
     * @return Elemento desapilado.
     */
    public T desapilar() throws EstructuraVaciaException {
        if (estaVacia()) {
            throw new EstructuraVaciaException("No se puede desapilar de una pila vacía");
        }
        T dato = cima.getDato();
        cima = cima.getSiguiente(); // Actualiza la cima al siguiente nodo
        tamano--;
        return dato;
    }

    /**
     * Retorna el elemento en la cima sin eliminarlo.
     * Lanza excepción si la pila está vacía.
     * 
     * @return Elemento en la cima.
     */
    public T verCima() throws EstructuraVaciaException {
        if (estaVacia()) {
            throw new EstructuraVaciaException("La pila está vacía");
        }
        return cima.getDato();
    }

    /**
     * Indica si la pila está vacía.
     * 
     * @return true si vacía, false en caso contrario.
     */
    public boolean estaVacia() {
        return cima == null;
    }

    /**
     * Retorna el número de elementos almacenados en la pila.
     * 
     * @return Tamaño actual de la pila.
     */
    public int size() {
        return tamano;
    }

    /**
     * Limpia la pila eliminando todos los elementos.
     */
    public void limpiar() {
        cima = null;
        tamano = 0;
    }

    /**
     * Recorrido desde la cima hacia abajo, aplicando una acción a cada elemento.
     * Útil para impresión o procesamiento secuencial.
     * 
     * @param accion Acción a aplicar a cada dato.
     */
    public void recorrer(java.util.function.Consumer<T> accion) {
        Nodo<T> aux = cima;
        while (aux != null) {
            accion.accept(aux.getDato());
            aux = aux.getSiguiente();
        }
    }
}
