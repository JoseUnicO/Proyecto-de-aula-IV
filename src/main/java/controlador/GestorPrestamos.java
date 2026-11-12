package controlador;

import estructuras.Cola;
import estructuras.ArbolBinario;
import modelo.Libro;
import modelo.Usuario;
import modelo.Prestamo;
import excepciones.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase que gestiona todas las operaciones relacionadas con los préstamos de
 * libros.
 * Mantiene préstamos activos en un árbol binario para búsquedas eficientes,
 * y un historial en una cola que conserva el orden de las
 * devoluciones/cancelaciones.
 * Integra control con gestores de usuarios, libros y reservas para validar
 * reglas de negocio.
 */
public class GestorPrestamos {
    // Referencias a gestores de libros y usuarios para validar y actualizar estados
    private GestorLibros gestorLibros;
    private GestorUsuarios gestorUsuarios;
    private GestorReservas gestorReservas;
    // Árbol binario para almacenar los préstamos que están activos (pendientes de
    // devolución)
    private ArbolBinario<Prestamo> prestamosActivos;
    // Cola para mantener el historial de préstamos devueltos o cancelados
    private Cola<Prestamo> historialPrestamos;
    // Constantes para reglas de negocio: duración por defecto, renovación y multa
    // diaria
    private static final int DIAS_PRESTAMO_DEFECTO = 14;
    private static final int DIAS_RENOVACION = 7;
    private static final double TARIFA_MULTA_DIA = 2.0;

    /**
     * Constructor que inicializa el gestor con los gestores de libros y usuarios.
     * También crea un gestor de reservas para la gestión paralela de reservas de
     * libros.
     */
    public GestorPrestamos(GestorLibros gestorLibros, GestorUsuarios gestorUsuarios) {
        this.gestorLibros = gestorLibros;
        this.gestorUsuarios = gestorUsuarios;
        this.gestorReservas = new GestorReservas();
        this.prestamosActivos = new ArbolBinario<>();
        this.historialPrestamos = new Cola<>();
        gestorLibros.protegerReferencias(
                id -> obtenerTodosPrestamos().stream().anyMatch(p -> p.getLibro().getId().equals(id)) || gestorReservas
                        .obtenerTodasReservas().stream().anyMatch(r -> r.getLibro().getId().equals(id)));
        gestorUsuarios.protegerReferencias(id -> obtenerTodosPrestamos().stream()
                .anyMatch(p -> p.getUsuario().getId().equals(id))
                || gestorReservas.obtenerTodasReservas().stream().anyMatch(r -> r.getUsuario().getId().equals(id)));
    }

    public GestorReservas getGestorReservas() {
        return gestorReservas;
    }

    /**
     * Registra un préstamo con duración por defecto.
     * Invoca el método general con cantidad de días predefinida (14 días).
     */
    private Prestamo registrarPrestamoInterno(String idUsuario, String idLibro) throws BibliotecaException {
        return registrarPrestamo(idUsuario, idLibro, DIAS_PRESTAMO_DEFECTO);
    }

    /**
     * Método principal para registrar un nuevo préstamo.
     * Valida que el usuario y libro existan, que el libro esté disponible, el
     * usuario pueda pedir préstamo,
     * y que no haya reservas conflictivas para ese libro por otros usuarios.
     * Actualiza estado de libro y usuario, inserta en préstamos activos, y elimina
     * reservas vinculadas.
     */
    private Prestamo registrarPrestamoInterno(String idUsuario, String idLibro, int diasPrestamo)
            throws BibliotecaException {
        if (diasPrestamo <= 0) {
            throw new OperacionInvalidaException("Los días de préstamo deben ser mayores a 0");
        }
        Usuario usuario = gestorUsuarios.buscarUsuario(idUsuario);
        Libro libro = gestorLibros.buscarLibro(idLibro);

        if (!libro.isDisponible()) {
            throw new OperacionInvalidaException("No se puede prestar: libro [" + idLibro + "] no disponible.");
        }

        if (!gestorReservas.puedePrestar(idLibro, idUsuario, libro.getEjemplaresDisponibles()))
            throw new OperacionInvalidaException(
                    "Los ejemplares disponibles están reservados para los primeros turnos");

        if (!usuario.puedePedirPrestamo()) {
            throw new OperacionInvalidaException(
                    "Usuario [" + idUsuario + "] no puede solicitar préstamos: límite excedido o multas pendientes.");
        }

        // Crear objeto préstamo y actualizar estados relacionados
        Prestamo prestamo = new Prestamo(usuario, libro, diasPrestamo);
        usuario.agregarPrestamo();
        var ejemplar = libro.getEjemplares().stream().filter(modelo.Ejemplar::isDisponible).findFirst().orElseThrow();
        prestamo.restaurarEjemplar(ejemplar.getId());
        ejemplar.restaurarPrestado(true);
        prestamosActivos.insertar(prestamo);

        // Si el préstamo coincide con una reserva existente del mismo usuario, se
        // elimina esa reserva
        var reservaSiguiente = gestorReservas.obtenerReservasActivas(idLibro).stream()
                .filter(r -> r.getUsuario().getId().equals(idUsuario)).findFirst().orElse(null);
        if (reservaSiguiente != null && reservaSiguiente.getUsuario().getId().equals(idUsuario)) {
            if (!gestorReservas.eliminarReservaPorId(reservaSiguiente.getIdReserva()))
                throw new OperacionInvalidaException("No se pudo consumir la reserva");
            reservaSiguiente.restaurarEstado(modelo.Reserva.EstadoReserva.ATENDIDA);
        }

        gestorReservas.procesarDisponibilidad(libro);
        return prestamo;
    }

    /**
     * Registra la devolución de un préstamo.
     * Actualiza estados, calcula multas en caso de retraso y mueve el préstamo de
     * activos
     * al historial, además de procesar las reservas siguientes en cola.
     */
    private void registrarDevolucionInterno(String idPrestamo) throws BibliotecaException {
        Prestamo prestamoBusqueda = buscarPrestamoActivo(idPrestamo);

        if (prestamoBusqueda.getLibro().buscarEjemplar(prestamoBusqueda.getIdEjemplar()).isDisponible()
                || prestamoBusqueda.getUsuario().getPrestamosActuales() <= 0)
            throw new OperacionInvalidaException("Estado inconsistente del préstamo");
        prestamoBusqueda.registrarDevolucion();

        long diasRetraso = prestamoBusqueda.getDiasRetraso();
        if (diasRetraso > 0) {
            double multa = prestamoBusqueda.calcularMulta(TARIFA_MULTA_DIA);
            prestamoBusqueda.getUsuario().agregarMulta(multa);
            System.out.println(String.format(
                    "[AVISO] Devolución con retraso de %d días. Multa: $%.2f",
                    diasRetraso, multa));
        }

        prestamoBusqueda.getLibro().buscarEjemplar(prestamoBusqueda.getIdEjemplar()).restaurarPrestado(false);
        prestamoBusqueda.getUsuario().devolverPrestamo();
        prestamosActivos.eliminar(prestamoBusqueda);
        historialPrestamos.encolar(prestamoBusqueda);

        try {
            gestorReservas.procesarSiguienteReserva(prestamoBusqueda.getLibro().getId());
        } catch (EstructuraVaciaException e) {
            // No hay reservas pendientes para el libro
        }

        // Delegado al cierre de la aplicación

    }

    /**
     * Cancela un préstamo activo.
     * Actualiza estados de libro y usuario, remueve del árbol de activos y agrega
     * al historial.
     * Persistencia también se actualiza para mantener integridad de datos.
     */
    private void cancelarPrestamoInterno(String idPrestamo) throws BibliotecaException {
        Prestamo prestamo = buscarPrestamoActivo(idPrestamo);

        if (prestamo.getLibro().buscarEjemplar(prestamo.getIdEjemplar()).isDisponible()
                || prestamo.getUsuario().getPrestamosActuales() <= 0)
            throw new OperacionInvalidaException("Estado inconsistente del préstamo");
        prestamo.cancelar();

        prestamo.getLibro().buscarEjemplar(prestamo.getIdEjemplar()).restaurarPrestado(false);

        prestamo.getUsuario().devolverPrestamo();

        prestamosActivos.eliminar(prestamo);
        historialPrestamos.encolar(prestamo);
        gestorReservas.procesarSiguienteReserva(prestamo.getLibro().getId());

        // Delegado al cierre de la aplicación

    }

    /**
     * Edita la fecha de devolución esperada de un préstamo activo.
     * Se valida que el préstamo esté activo y que la nueva fecha sea posterior al
     * préstamo.
     * Permite flexibilidad en extensión manual de plazos.
     */
    private void editarFechaDevolucionInterno(String idPrestamo, LocalDate nuevaFechaDevolucion)
            throws BibliotecaException {
        Prestamo prestamo = buscarPrestamoActivo(idPrestamo);

        if (prestamo.getEstado() != Prestamo.EstadoPrestamo.ACTIVO) {
            throw new OperacionInvalidaException("Solo se pueden editar préstamos activos");
        }

        if (nuevaFechaDevolucion == null || nuevaFechaDevolucion.isBefore(prestamo.getFechaPrestamo())) {
            throw new OperacionInvalidaException(
                    "La fecha de devolución no puede ser anterior a la fecha de préstamo");
        }

        prestamo.setFechaDevolucionEsperada(nuevaFechaDevolucion);

    }

    /**
     * Renueva un préstamo activo agregando días extra.
     * Verifica que se pueda renovar (no vencido ni renovado anteriormente) y que no
     * haya multas pendientes.
     */
    private void renovarPrestamoInterno(String idPrestamo) throws BibliotecaException {
        Prestamo prestamo = buscarPrestamoActivo(idPrestamo);

        if (!prestamo.puedeRenovarse()) {
            throw new OperacionInvalidaException("El préstamo no puede ser renovado");
        }

        if (prestamo.getUsuario().getMultaAcumulada() > 0) {
            throw new OperacionInvalidaException("No se puede renovar con multas pendientes");
        }

        prestamo.renovar(DIAS_RENOVACION);

    }

    /**
     * Busca un préstamo activo por su ID.
     * Retorna el préstamo si existe y está activo; lanza excepción si no existe o
     * no está activo.
     */
    private Prestamo buscarPrestamoActivo(String idPrestamo) throws ElementoNoEncontradoException {
        final Prestamo[] resultado = { null };
        try {
            prestamosActivos.recorrerEnOrden(prestamo -> {
                if (prestamo.getIdPrestamo().equals(idPrestamo) &&
                        (prestamo.getEstado() == Prestamo.EstadoPrestamo.ACTIVO
                                || prestamo.getEstado() == Prestamo.EstadoPrestamo.VENCIDO)) {
                    resultado[0] = prestamo;
                }
            });
        } catch (EstructuraVaciaException e) {
            throw new ElementoNoEncontradoException("No hay préstamos activos");
        }

        if (resultado[0] == null) {
            throw new ElementoNoEncontradoException("Préstamo no encontrado con ID: " + idPrestamo);
        }

        return resultado[0];
    }

    /**
     * Muestra todos los préstamos activos en consola.
     * Útil para seguimiento en tiempo real de préstamos que aún no han sido
     * devueltos.
     */
    public void mostrarPrestamosActivos() throws EstructuraVaciaException {
        System.out.println("\n=== PRÉSTAMOS ACTIVOS ===");
        prestamosActivos.recorrerEnOrden(prestamo -> {
            if ((prestamo.getEstado() == Prestamo.EstadoPrestamo.ACTIVO
                    || prestamo.getEstado() == Prestamo.EstadoPrestamo.VENCIDO)) {
                System.out.println(prestamo);
            }
        });
    }

    /**
     * Muestra préstamos activos que están vencidos (fecha devolución esperada
     * pasada).
     * Permite localizar casos de mora y tomar acciones correspondientes.
     */
    public void mostrarPrestamosVencidos() throws EstructuraVaciaException {
        System.out.println("\n=== PRÉSTAMOS VENCIDOS ===");
        prestamosActivos.recorrerEnOrden(prestamo -> {
            if (prestamo.estaVencido()) {
                System.out.println(prestamo);
            }
        });
    }

    /**
     * Muestra el historial completo de préstamos ya finalizados (devueltos o
     * cancelados).
     * Mostrar historial ayuda a auditoría y análisis del uso general.
     */
    public void mostrarHistorialCompleto() {
        System.out.println("\n=== HISTORIAL DE PRÉSTAMOS ===");
        historialPrestamos.recorrer(prestamo -> System.out.println(prestamo));
    }

    /**
     * Muestra todos los préstamos (activos) de un usuario específico.
     * Util para revisar la actividad y estado actual de un usuario.
     */
    public void mostrarPrestamosPorUsuario(String idUsuario) throws BibliotecaException {
        Usuario usuario = gestorUsuarios.buscarUsuario(idUsuario);
        System.out.println("\n=== PRÉSTAMOS DE: " + usuario.getNombre() + " ===");
        prestamosActivos.recorrerEnOrden(prestamo -> {
            if (prestamo.getUsuario().getId().equals(idUsuario)) {
                System.out.println(prestamo);
            }
        });
    }

    /**
     * Retorna la cantidad de préstamos activos actualmente.
     */
    public int contarPrestamosActivos() {
        return prestamosActivos.size();
    }

    /**
     * Retorna la cantidad de préstamos en el historial (finalizados).
     */
    public int contarHistorial() {
        return historialPrestamos.size();
    }

    public ArbolBinario<Prestamo> getPrestamosActivos() {
        return prestamosActivos;
    }

    /**
     * Obtiene una lista con todos los préstamos activos.
     * Útil para interfaces gráficas y procesamiento.
     */
    public List<Prestamo> obtenerTodosPrestamosActivos() {
        List<Prestamo> prestamos = new ArrayList<>();
        try {
            prestamosActivos.recorrerEnOrden(prestamo -> prestamos.add(prestamo));
        } catch (EstructuraVaciaException e) {
            // Si no hay préstamos activos, se devuelve lista vacía
        }
        return prestamos;
    }

    /**
     * Restaura un préstamo al estado activo en el árbol.
     * Usado para reinsertar préstamos cuando es necesario.
     */
    public void restaurarPrestamo(Prestamo prestamo) {
        if (prestamo.getEstado() == Prestamo.EstadoPrestamo.VENCIDO)
            prestamo.setEstado(Prestamo.EstadoPrestamo.ACTIVO);
        prestamosActivos.insertar(prestamo);
    }

    /**
     * Restaura un préstamo al historial (cola).
     * Útil para mantener registro en cola sin perder orden cronológico.
     */
    public void restaurarHistorial(Prestamo prestamo) {
        if (prestamo.getEstado() == Prestamo.EstadoPrestamo.VENCIDO) {
            prestamo.setEstado(Prestamo.EstadoPrestamo.ACTIVO);
            prestamosActivos.insertar(prestamo);
        } else
            historialPrestamos.encolar(prestamo);
    }

    /**
     * Obtiene una lista con todos los préstamos (activos + historial).
     * Incluye mensajes de debug para monitoreo de procesos.
     */
    public List<Prestamo> obtenerTodosPrestamos() {
        List<Prestamo> prestamos = new ArrayList<>();

        try {
            prestamosActivos.recorrerEnOrden(prestamo -> {
                prestamos.add(prestamo);
            });
        } catch (EstructuraVaciaException e) {
        }

        historialPrestamos.recorrer(prestamo -> {
            prestamos.add(prestamo);
        });

        return prestamos;
    }

    public void reponer(java.util.List<Prestamo> lista) {
        prestamosActivos.limpiar();
        historialPrestamos.limpiar();
        for (Prestamo p : lista) {
            if (p.getEstado() == Prestamo.EstadoPrestamo.ACTIVO || p.getEstado() == Prestamo.EstadoPrestamo.VENCIDO)
                prestamosActivos.insertar(p);
            else
                historialPrestamos.encolar(p);
        }
    }

    public Prestamo registrarPrestamo(String idUsuario, String idLibro) throws BibliotecaException {
        return utilidades.SistemaBiblioteca.ejecutar(this, "registrarPrestamo", () -> {
            return registrarPrestamoInterno(idUsuario, idLibro);
        });
    }

    public Prestamo registrarPrestamo(String idUsuario, String idLibro, int diasPrestamo) throws BibliotecaException {
        return utilidades.SistemaBiblioteca.ejecutar(this, "registrarPrestamo", () -> {
            return registrarPrestamoInterno(idUsuario, idLibro, diasPrestamo);
        });
    }

    public void registrarDevolucion(String idPrestamo) throws BibliotecaException {
        utilidades.SistemaBiblioteca.ejecutar(this, "registrarDevolucion", () -> {
            registrarDevolucionInterno(idPrestamo);
            return null;
        });
    }

    public void cancelarPrestamo(String idPrestamo) throws BibliotecaException {
        utilidades.SistemaBiblioteca.ejecutar(this, "cancelarPrestamo", () -> {
            cancelarPrestamoInterno(idPrestamo);
            return null;
        });
    }

    public void editarFechaDevolucion(String idPrestamo, LocalDate nuevaFechaDevolucion)
            throws BibliotecaException {
        utilidades.SistemaBiblioteca.ejecutar(this, "editarFechaDevolucion", () -> {
            editarFechaDevolucionInterno(idPrestamo, nuevaFechaDevolucion);
            return null;
        });
    }

    public void renovarPrestamo(String idPrestamo) throws BibliotecaException {
        utilidades.SistemaBiblioteca.ejecutar(this, "renovarPrestamo", () -> {
            renovarPrestamoInterno(idPrestamo);
            return null;
        });
    }

    private utilidades.SistemaBiblioteca sistema;

    public void conectar(utilidades.SistemaBiblioteca s) {
        sistema = s;
    }

    public utilidades.SistemaBiblioteca sistema() {
        return sistema;
    }
}
