package modelo;

import java.time.LocalDate;

/**
 * Clase que representa una reserva de un libro por parte de un usuario.
 * Controla información como el usuario, libro, fecha y estado de la reserva.
 * Gestiona identificación única y estados de reserva.
 */
public class Reserva {

    private String idReserva;
    private Usuario usuario;
    private Libro libro;
    private LocalDate fechaReserva;
    private EstadoReserva estado;

    private static int contadorId = 1;

    public enum EstadoReserva {
        PENDIENTE, NOTIFICADA, CANCELADA
    }

    /**
     * Constructor para crear una nueva reserva con estado inicial PENDIENTE.
     * Genera un ID único automáticamente y registra la fecha actual como fecha de reserva.
     */
    public Reserva(Usuario usuario, Libro libro) {
        this.idReserva = generarId();
        this.usuario = usuario;
        this.libro = libro;
        this.fechaReserva = LocalDate.now();
        this.estado = EstadoReserva.PENDIENTE;
    }

    /**
     * Constructor completo para cargar reservas existentes en el sistema.
     * Permite sincronizar el contador para evitar duplicación de IDs al cargar datos.
     */
    public Reserva(String idReserva, Usuario usuario, Libro libro,
            LocalDate fechaReserva, EstadoReserva estado) {
        this.idReserva = idReserva;
        this.usuario = usuario;
        this.libro = libro;
        this.fechaReserva = fechaReserva;
        this.estado = estado;

        // Sincroniza el contador para evitar IDs duplicados al cargar reservas existentes
        try {
            if (idReserva != null && idReserva.startsWith("RES-")) {
                int num = Integer.parseInt(idReserva.substring(4));
                if (num >= contadorId) {
                    contadorId = num + 1;
                }
            }
        } catch (NumberFormatException e) {
            // En caso de error en el formato del ID, no se actualiza el contador
        }
    }

    /**
     * Genera un ID único para la reserva usando un contador estático.
     * Formato: "RES-" seguido de un número de cinco dígitos con ceros a la izquierda.
     */
    private String generarId() {
        return "RES-" + String.format("%05d", contadorId++);
    }

    public void notificar() {
        this.estado = EstadoReserva.NOTIFICADA;
    }

    public void cancelar() {
        this.estado = EstadoReserva.CANCELADA;
    }

    public String getIdReserva() {
        return idReserva;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Libro getLibro() {
        return libro;
    }

    public LocalDate getFechaReserva() {
        return fechaReserva;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    @Override
    public String toString() {
        return String.format(
                "Reserva[%s] Usuario: %s, Libro: %s, Fecha: %s, Estado: %s",
                idReserva,
                usuario.getNombre(),
                libro.getTitulo(),
                fechaReserva,
                estado);
    }
}
