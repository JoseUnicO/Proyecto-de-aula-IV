package estructuras;

import excepciones.EstructuraVaciaException;

/**
 * Implementación de una Cola.
 * Permite encolar y desencolar elementos de forma eficiente,
 * además de consultar el frente, tamaño y limpiar la cola.
 */
public class Cola<T> {
    private Nodo<T> frente; // Nodo al frente de la cola, para desencolar
    private Nodo<T> fin; // Nodo al final de la cola, para encolar
    private int tamano; // Número de elementos almacenados en la cola

    /**
     * Constructor que inicializa la cola vacía (sin nodos y tamaño 0).
     */
    public Cola() {
        this.frente = null;
        this.fin = null;
        this.tamano = 0;
    }

    /**
     * Agrega un elemento al final de la cola.
     * Si no hay elementos, inicializa frente y fin con el nuevo nodo.
     *
     * @param dato Elemento a agregar.
     * @throws IllegalArgumentException si el dato es nulo.
     */
    public void encolar(T dato) {
        if (dato == null) {
            throw new IllegalArgumentException("No se puede encolar un elemento nulo");
        }
        Nodo<T> nuevo = new Nodo<>(dato);
        // Si ya hay un final, enlaza el nuevo nodo después de él
        if (fin != null) {
            fin.setSiguiente(nuevo);
        }
        fin = nuevo;
        // Si la cola estaba vacía, ahora frente apunta al nuevo nodo
        if (frente == null) {
            frente = nuevo;
        }
        tamano++;
    }

    /**
     * Elimina y retorna el elemento del frente de la cola.
     * Actualiza referencias; si queda vacía, fin se setea a null.
     *
     * @return Elemento desencolado.
     * @throws EstructuraVaciaException si la cola está vacía.
     */
    public T desencolar() throws EstructuraVaciaException {
        if (estaVacia()) {
            throw new EstructuraVaciaException("No se puede desencolar de una cola vacía");
        }
        T dato = frente.getDato();
        frente = frente.getSiguiente();
        // Si luego de desencolar no hay nodos, fin también es null
        if (frente == null) {
            fin = null;
        }
        tamano--;
        return dato;
    }

    /**
     * Retorna el elemento que está al frente sin eliminarlo.
     *
     * @return Elemento frente.
     * @throws EstructuraVaciaException si la cola está vacía.
     */
    public T verFrente() throws EstructuraVaciaException {
        if (estaVacia()) {
            throw new EstructuraVaciaException("La cola está vacía");
        }
        return frente.getDato();
    }

    /**
     * Indica si la cola está vacía, basado en si el frente apunta a null.
     *
     * @return true si vacía, false si contiene al menos un nodo.
     */
    public boolean estaVacia() {
        return frente == null;
    }

    /**
     * Retorna el número de elementos actualmente almacenados en la cola.
     *
     * @return Tamaño de la cola.
     */
    public int size() {
        return tamano;
    }

    /**
     * Limpia la cola, eliminando todas las referencias y reseteando el tamaño.
     * Esto libera la memoria asociada y deja la cola como vacía.
     */
    public void limpiar() {
        frente = null;
        fin = null;
        tamano = 0;
    }

    /**
     * Permite recorrer todos los elementos de la cola desde el frente hasta el
     * final,
     * aplicando una acción determinada para cada elemento. Útil para imprimir o
     * procesar datos.
     *
     * @param accion Acción a aplicar a cada elemento (por ejemplo, imprimir).
     */
    public void recorrer(java.util.function.Consumer<T> accion) {
        Nodo<T> aux = frente;
        while (aux != null) {
            accion.accept(aux.getDato());
            aux = aux.getSiguiente();
        }
    }
}