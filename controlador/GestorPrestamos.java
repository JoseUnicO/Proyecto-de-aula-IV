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
    }

    public GestorReservas getGestorReservas() {
        return gestorReservas;
    }

    /**
     * Registra un préstamo con duración por defecto.
     * Invoca el método general con cantidad de días predefinida (14 días).
     */
    public Prestamo registrarPrestamo(String idUsuario, String idLibro) throws BibliotecaException {
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
    public Prestamo registrarPrestamo(String idUsuario, String idLibro, int diasPrestamo) throws BibliotecaException {
        Usuario usuario = gestorUsuarios.buscarUsuario(idUsuario);
        Libro libro = gestorLibros.buscarLibro(idLibro);

        try {
            var reservaSiguiente = gestorReservas.obtenerSiguienteReserva(idLibro);
            if (reservaSiguiente != null && !reservaSiguiente.getUsuario().getId().equals(idUsuario)) {
                throw new OperacionInvalidaException(
                        "No se puede prestar: libro reservado para usuario [" + reservaSiguiente.getUsuario().getId()
                                + "] (Reserva: " + reservaSiguiente.getIdReserva() + ")");
            }
        } catch (Exception e) {
            // En caso de error con reservas, se sigue validando otras restricciones
        }

        if (!usuario.puedePedirPrestamo()) {
            throw new OperacionInvalidaException(
                    "Usuario [" + idUsuario + "] no puede solicitar préstamos: límite excedido o multas pendientes.");
        }

        if (!libro.isDisponible()) {
            throw new OperacionInvalidaException("No se puede prestar: libro [" + idLibro + "] no disponible.");
        }

        // Crear objeto préstamo y actualizar estados relacionados
        Prestamo prestamo = new Prestamo(usuario, libro, diasPrestamo);
        usuario.agregarPrestamo();
        libro.prestar();
        prestamosActivos.insertar(prestamo);

        // Si el préstamo coincide con una reserva existente del mismo usuario, se
        // elimina esa reserva
        try {
            var reservaSiguiente = gestorReservas.obtenerSiguienteReserva(idLibro);
            if (reservaSiguiente != null && reservaSiguiente.getUsuario().getId().equals(idUsuario)) {
                boolean removed = gestorReservas.eliminarReservaPorId(reservaSiguiente.getIdReserva());
                if (removed) {
                    try {
                        utilidades.PersistenciaArchivos.guardarReservas(gestorReservas.obtenerTodasReservas());
                    } catch (Exception e) {
                        System.err.println("[ERROR] No se pudo guardar reservas después de prestar: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            // Manejo de excepción en proceso de reservas
        }

        // Guardar estado de préstamos actualizados en persistencia
        try {
            utilidades.PersistenciaArchivos.guardarPrestamos(obtenerTodosPrestamos());
        } catch (Exception e) {
            System.err.println("Error al guardar préstamo: " + e.getMessage());
        }

        return prestamo;
    }

    /**
     * Registra la devolución de un préstamo.
     * Actualiza estados, calcula multas en caso de retraso y mueve el préstamo de
     * activos
     * al historial, además de procesar las reservas siguientes en cola.
     */
    public void registrarDevolucion(String idPrestamo) throws BibliotecaException {
        Prestamo prestamoBusqueda = buscarPrestamoActivo(idPrestamo);
        prestamoBusqueda.registrarDevolucion();

        long diasRetraso = prestamoBusqueda.getDiasRetraso();
        if (diasRetraso > 0) {
            double multa = prestamoBusqueda.calcularMulta(TARIFA_MULTA_DIA);
            prestamoBusqueda.getUsuario().agregarMulta(multa);
            System.out.println(String.format(
                    "[AVISO] Devolución con retraso de %d días. Multa: $%.2f",
                    diasRetraso, multa));
        }

        prestamoBusqueda.getLibro().devolver();
        prestamoBusqueda.getUsuario().devolverPrestamo();
        prestamosActivos.eliminar(prestamoBusqueda);
        historialPrestamos.encolar(prestamoBusqueda);

        try {
            gestorReservas.procesarSiguienteReserva(prestamoBusqueda.getLibro().getId());
        } catch (EstructuraVaciaException e) {
            // No hay reservas pendientes para el libro
        }

        try {
            utilidades.PersistenciaArchivos.guardarPrestamos(obtenerTodosPrestamos());
        } catch (Exception e) {
            System.err.println("[ERROR] No se pudo guardar los préstamos: " + e.getMessage());
        }

        System.out.println("[OK] Devolución registrada exitosamente");
    }

    /**
     * Cancela un préstamo activo.
     * Actualiza estados de libro y usuario, remueve del árbol de activos y agrega
     * al historial.
     * Persistencia también se actualiza para mantener integridad de datos.
     */
    public void cancelarPrestamo(String idPrestamo) throws BibliotecaException {
        Prestamo prestamo = buscarPrestamoActivo(idPrestamo);

        prestamo.cancelar();

        prestamo.getLibro().devolver();

        prestamo.getUsuario().devolverPrestamo();

        prestamosActivos.eliminar(prestamo);
        historialPrestamos.encolar(prestamo);

        try {
            utilidades.PersistenciaArchivos.guardarPrestamos(obtenerTodosPrestamos());
        } catch (Exception e) {
            System.err.println("[ERROR] No se pudo guardar los préstamos: " + e.getMessage());
        }

        System.out.println("[OK] Préstamo cancelado exitosamente: " + idPrestamo);
    }

    /**
     * Edita la fecha de devolución esperada de un préstamo activo.
     * Se valida que el préstamo esté activo y que la nueva fecha sea posterior al
     * préstamo.
     * Permite flexibilidad en extensión manual de plazos.
     */
    public void editarFechaDevolucion(String idPrestamo, LocalDate nuevaFechaDevolucion)
            throws BibliotecaException {
        Prestamo prestamo = buscarPrestamoActivo(idPrestamo);

        if (prestamo.getEstado() != Prestamo.EstadoPrestamo.ACTIVO) {
            throw new OperacionInvalidaException("Solo se pueden editar préstamos activos");
        }

        if (nuevaFechaDevolucion.isBefore(prestamo.getFechaPrestamo())) {
            throw new OperacionInvalidaException(
                    "La fecha de devolución no puede ser anterior a la fecha de préstamo");
        }

        prestamo.setFechaDevolucionEsperada(nuevaFechaDevolucion);

        try {
            utilidades.PersistenciaArchivos.guardarPrestamos(obtenerTodosPrestamos());
            System.out.println("[OK] Préstamo editado: " + idPrestamo +
                    " - Nueva fecha: " + nuevaFechaDevolucion);
        } catch (Exception e) {
            System.err.println("[ERROR] No se pudo guardar los cambios: " + e.getMessage());
            throw new BibliotecaException("Error al persistir los cambios");
        }
    }

    /**
     * Renueva un préstamo activo agregando días extra.
     * Verifica que se pueda renovar (no vencido ni renovado anteriormente) y que no
     * haya multas pendientes.
     */
    public void renovarPrestamo(String idPrestamo) throws BibliotecaException {
        Prestamo prestamo = buscarPrestamoActivo(idPrestamo);

        if (!prestamo.puedeRenovarse()) {
            throw new OperacionInvalidaException("El préstamo no puede ser renovado");
        }

        if (prestamo.getUsuario().getMultaAcumulada() > 0) {
            throw new OperacionInvalidaException("No se puede renovar con multas pendientes");
        }

        prestamo.renovar(DIAS_RENOVACION);
        System.out.println(String.format(
                "[OK] Préstamo renovado. Nueva fecha de devolución: %s",
                prestamo.getFechaDevolucionEsperada()));
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
                        prestamo.getEstado() == Prestamo.EstadoPrestamo.ACTIVO) {
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
            if (prestamo.getEstado() == Prestamo.EstadoPrestamo.ACTIVO) {
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
        prestamosActivos.insertar(prestamo);
    }

    /**
     * Restaura un préstamo al historial (cola).
     * Útil para mantener registro en cola sin perder orden cronológico.
     */
    public void restaurarHistorial(Prestamo prestamo) {
        historialPrestamos.encolar(prestamo);
    }

    /**
     * Obtiene una lista con todos los préstamos (activos + historial).
     * Incluye mensajes de debug para monitoreo de procesos.
     */
    public List<Prestamo> obtenerTodosPrestamos() {
        List<Prestamo> prestamos = new ArrayList<>();
        System.out.println("[DEBUG GestorPrestamos] Iniciando obtenerTodosPrestamos()");
        System.out.println("[DEBUG GestorPrestamos] Size del árbol: " + prestamosActivos.size());

        try {
            prestamosActivos.recorrerEnOrden(prestamo -> {
                prestamos.add(prestamo);
                System.out.println("[DEBUG GestorPrestamos] ✓ Agregado activo: " + prestamo.getIdPrestamo()
                        + " - " + prestamo.getLibro().getTitulo());
            });
        } catch (EstructuraVaciaException e) {
            System.out.println("[DEBUG GestorPrestamos] Árbol de préstamos activos vacío");
        }

        historialPrestamos.recorrer(prestamo -> {
            prestamos.add(prestamo);
            System.out.println("[DEBUG GestorPrestamos] ✓ Agregado historial: " + prestamo.getIdPrestamo()
                    + " - " + prestamo.getLibro().getTitulo());
        });

        System.out.println("[DEBUG GestorPrestamos] Total devuelto: " + prestamos.size() + " préstamos");
        return prestamos;
    }
}
