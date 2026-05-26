package controlador;

import estructuras.ListaEnlazada;
import modelo.Reserva;
import modelo.Usuario;
import modelo.Libro;
import excepciones.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import utilidades.PersistenciaArchivos;
import java.io.IOException;

/**
 * Clase que gestiona las reservas de libros.
 * Utiliza un mapa para almacenar listas enlazadas de reservas por cada libro.
 * Permite agregar, cancelar, eliminar, notificar y consultar reservas,
 * respetando reglas de negocio como impedir reservas si el libro está
 * disponible,
 * o restricciones para usuarios que ya tengan reservas activas del mismo libro.
 */
public class GestorReservas {

    // Mapa que asocia el ID del libro con su cola (lista enlazada) de reservas
    private Map<String, ListaEnlazada<Reserva>> reservasPorLibro;

    /**
     * Constructor que inicializa el mapa de reservas vacío.
     */
    public GestorReservas() {
        this.reservasPorLibro = new HashMap<>();
    }

    /**
     * Agrega una nueva reserva para un usuario y libro determinados.
     * Valida que el libro no esté disponible (sin préstamo) y que el usuario
     * no tenga una reserva activa para ese libro.
     *
     * @throws OperacionInvalidaException en caso de violación de las reglas
     *                                    anteriores.
     *
     * @return La reserva creada.
     */
    public Reserva agregarReserva(Usuario usuario, Libro libro) throws OperacionInvalidaException {
        if (libro.isDisponible()) {
            throw new OperacionInvalidaException(
                    "No se puede crear reserva: libro disponible [" + libro.getId() + "].");
        }

        if (tieneReservaPendiente(usuario.getId(), libro.getId())) {
            throw new OperacionInvalidaException("Usuario [" + usuario.getId()
                    + "] ya posee una reserva activa para el libro [" + libro.getId() + "].");
        }

        Reserva reserva = new Reserva(usuario, libro);

        if (!reservasPorLibro.containsKey(libro.getId())) {
            reservasPorLibro.put(libro.getId(), new ListaEnlazada<>());
        }

        reservasPorLibro.get(libro.getId()).agregarAlFinal(reserva);

        System.out.println("[OK] Reserva registrada [" + reserva.getIdReserva() + "] - Usuario: " + usuario.getId()
                + ", Libro: " + libro.getId() + ", Posición: " +
                obtenerPosicionEnCola(reserva));
        return reserva;
    }

    /**
     * Restaura una reserva agregándola al final de la cola correspondiente.
     * Útil para cargar reservas desde persistencia manteniendo orden.
     */
    public void restaurarReserva(Reserva reserva) {
        String idLibro = reserva.getLibro().getId();
        if (!reservasPorLibro.containsKey(idLibro)) {
            reservasPorLibro.put(idLibro, new ListaEnlazada<>());
        }
        reservasPorLibro.get(idLibro).agregarAlFinal(reserva);
    }

    /**
     * Devuelve la siguiente reserva activa (pendiente o notificada) para un libro
     * determinado.
     * Esta es la reserva prioritaria para procesar (notificar o prestar).
     */
    public Reserva obtenerSiguienteReserva(String idLibro) {
        if (!reservasPorLibro.containsKey(idLibro)) {
            return null;
        }

        ListaEnlazada<Reserva> cola = reservasPorLibro.get(idLibro);
        try {
            final Reserva[] encontrada = { null };

            cola.recorrer(reserva -> {
                if ((reserva.getEstado() == Reserva.EstadoReserva.PENDIENTE ||
                        reserva.getEstado() == Reserva.EstadoReserva.NOTIFICADA) && encontrada[0] == null) {
                    encontrada[0] = reserva;
                }
            });
            return encontrada[0];
        } catch (EstructuraVaciaException e) {
            return null;
        }
    }

    /**
     * Procesa la siguiente reserva pendiente de un libro, marcándola como
     * notificada.
     * Se utiliza para avisar al usuario que su reserva está lista para ser tomada.
     * Luego guarda el estado actualizado en persistencia.
     */
    public void procesarSiguienteReserva(String idLibro) throws EstructuraVaciaException {
        if (!reservasPorLibro.containsKey(idLibro)) {
            return;
        }

        ListaEnlazada<Reserva> cola = reservasPorLibro.get(idLibro);
        if (!cola.estaVacia()) {

            final Reserva[] encontrada = { null };
            cola.recorrer(reserva -> {
                if (reserva.getEstado() == Reserva.EstadoReserva.PENDIENTE && encontrada[0] == null) {
                    encontrada[0] = reserva;
                }
            });

            if (encontrada[0] != null) {
                encontrada[0].notificar();
                System.out.println(
                        "[OK] Reserva procesada [" + encontrada[0].getIdReserva() + "] - Estado: NOTIFICADA - Usuario: "
                                + encontrada[0].getUsuario().getId() + ", Libro: " + encontrada[0].getLibro().getId());
            }
        }
    }

    /**
     * Remueve la reserva que está al frente de la cola de reservas del libro.
     * Útil para eliminar reservas una vez que el libro ha sido prestado o reserva
     * cancelada.
     */
    public void removerReserva(String idLibro) throws EstructuraVaciaException {
        if (!reservasPorLibro.containsKey(idLibro)) {
            return;
        }

        ListaEnlazada<Reserva> cola = reservasPorLibro.get(idLibro);
        if (!cola.estaVacia()) {
            cola.eliminarPrimero();
        }
    }

    /**
     * Cancela una reserva específica identificada por su ID.
     * Valida que la reserva exista y no haya sido cancelada previamente.
     * Cambia su estado a CANCELADA y guarda el cambio en persistencia.
     */
    public void cancelarReserva(String idReserva) throws ElementoNoEncontradoException, OperacionInvalidaException {
        for (ListaEnlazada<Reserva> cola : reservasPorLibro.values()) {
            try {
                Reserva[] encontrada = { null };
                cola.recorrer(reserva -> {
                    if (reserva.getIdReserva().equals(idReserva)) {
                        encontrada[0] = reserva;
                    }
                });

                if (encontrada[0] != null) {
                    if (encontrada[0].getEstado() == Reserva.EstadoReserva.CANCELADA) {
                        throw new OperacionInvalidaException(
                                "La reserva [" + idReserva + "] ya fue cancelada previamente. " +
                                        "No se puede cancelar una reserva que ya está en estado CANCELADA.");
                    }

                    encontrada[0].cancelar();
                    cola.eliminar(encontrada[0]);
                    System.out.println("[OK] Reserva cancelada [" + encontrada[0].getIdReserva() +
                            "] - Usuario: " + encontrada[0].getUsuario().getId() +
                            ", Libro: " + encontrada[0].getLibro().getId());
                    return;
                }
            } catch (EstructuraVaciaException e) {
                continue;
            }
        }

        throw new ElementoNoEncontradoException("Reserva no encontrada con ID: " + idReserva);
    }

    /**
     * Elimina una reserva por su ID, removiéndola completamente de la cola.
     * Devuelve true si la reserva fue eliminada, false si no se encontró.
     */
    public boolean eliminarReservaPorId(String idReserva) {
        for (ListaEnlazada<Reserva> cola : reservasPorLibro.values()) {
            try {
                final Reserva[] encontrada = { null };
                cola.recorrer(reserva -> {
                    if (reserva.getIdReserva().equals(idReserva)) {
                        encontrada[0] = reserva;
                    }
                });

                if (encontrada[0] != null) {
                    boolean removed = cola.eliminar(encontrada[0]);
                    if (removed) {
                        System.out.println("[OK] Reserva eliminada de la cola: " + idReserva);
                    }
                    return removed;
                }
            } catch (EstructuraVaciaException e) {
                continue;
            }
        }

        return false;
    }

    /**
     * Muestra todas las reservas para un libro específico.
     * Útil para monitorear la cola de espera actual.
     */
    public void mostrarReservasDeLibro(String idLibro) throws EstructuraVaciaException {
        if (!reservasPorLibro.containsKey(idLibro)) {
            System.out.println("[INFO] No hay reservas para este libro.");
            return;
        }

        ListaEnlazada<Reserva> cola = reservasPorLibro.get(idLibro);
        if (cola.estaVacia()) {
            System.out.println("[INFO] No hay reservas para este libro.");
            return;
        }

        System.out.println("\n=== COLA DE RESERVAS ===");
        final int[] pos = { 1 };
        cola.recorrer(reserva -> {
            System.out.println(pos[0] + ". " + reserva);
            pos[0]++;
        });
    }

    /**
     * Muestra todas las reservas pendientes en el sistema, divididas por libro.
     */
    public void mostrarTodasReservasPendientes() {
        System.out.println("\n=== TODAS LAS RESERVAS PENDIENTES ===");
        boolean hayReservas = false;

        for (Map.Entry<String, ListaEnlazada<Reserva>> entry : reservasPorLibro.entrySet()) {
            try {
                if (!entry.getValue().estaVacia()) {
                    System.out.println("\nLibro ID: " + entry.getKey());
                    entry.getValue().recorrer(reserva -> {
                        if (reserva.getEstado() == Reserva.EstadoReserva.PENDIENTE) {
                            System.out.println("  " + reserva);
                        }
                    });
                    hayReservas = true;
                }
            } catch (EstructuraVaciaException e) {
                continue;
            }
        }

        if (!hayReservas) {
            System.out.println("[INFO] No hay reservas pendientes.");
        }
    }

    /**
     * Muestra todas las reservas que ha hecho un usuario específico.
     * Permite ver el estado actual de cada reserva del usuario.
     */
    public void mostrarReservasDeUsuario(String idUsuario) {
        System.out.println("\n=== RESERVAS DEL USUARIO ===");
        for (ListaEnlazada<Reserva> cola : reservasPorLibro.values()) {
            try {
                cola.recorrer(reserva -> {
                    if (reserva.getUsuario().getId().equals(idUsuario)) {
                        System.out.println(reserva);
                    }
                });
            } catch (EstructuraVaciaException e) {
                continue;
            }
        }
    }

    /**
     * Verifica si un usuario ya tiene una reserva pendiente o notificada para un
     * libro determinado.
     */
    private boolean tieneReservaPendiente(String idUsuario, String idLibro) {
        if (!reservasPorLibro.containsKey(idLibro)) {
            return false;
        }

        try {
            final boolean[] tiene = { false };
            reservasPorLibro.get(idLibro).recorrer(reserva -> {
                if (reserva.getUsuario().getId().equals(idUsuario) &&
                        (reserva.getEstado() == Reserva.EstadoReserva.PENDIENTE ||
                                reserva.getEstado() == Reserva.EstadoReserva.NOTIFICADA)) {
                    tiene[0] = true;
                }
            });
            return tiene[0];
        } catch (EstructuraVaciaException e) {
            return false;
        }
    }

    /**
     * Obtiene la posición en la cola (lista enlazada) de una reserva determinada.
     * La posición es contada solo entre reservas pendientes, para reflejar el orden
     * real de espera.
     */
    private int obtenerPosicionEnCola(Reserva reserva) {
        ListaEnlazada<Reserva> cola = reservasPorLibro.get(reserva.getLibro().getId());
        try {
            final int[] posicion = { 0 };
            cola.recorrer(r -> {
                if (r.getEstado() == Reserva.EstadoReserva.PENDIENTE) {
                    posicion[0]++;
                }
            });
            return posicion[0];
        } catch (EstructuraVaciaException e) {
            return 0;
        }
    }

    /**
     * Retorna el total de reservas que existen en el sistema (todas las colas
     * combinadas).
     */
    public int contarReservas() {
        int total = 0;
        for (ListaEnlazada<Reserva> cola : reservasPorLibro.values()) {
            try {
                final int[] count = { 0 };
                cola.recorrer(reserva -> {
                    if (reserva != null) {
                        count[0]++;
                    }
                });
                total += count[0];
            } catch (EstructuraVaciaException e) {
                continue;
            }
        }
        return total;
    }

    /**
     * Devuelve una lista con todas las reservas en el sistema.
     * Útil para reportes y guardado en persistencia.
     */
    public List<Reserva> obtenerTodasReservas() {
        List<Reserva> todasReservas = new ArrayList<>();
        for (ListaEnlazada<Reserva> cola : reservasPorLibro.values()) {
            try {
                cola.recorrer(todasReservas::add);
            } catch (EstructuraVaciaException e) {
                continue;
            }
        }
        return todasReservas;
    }
}
