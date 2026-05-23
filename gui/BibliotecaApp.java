package gui;

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import gui.controllers.LoginController;
import controlador.GestorLibros;
import controlador.GestorUsuarios;
import controlador.GestorPrestamos;
import controlador.GestorReservas;
import modelo.Libro;
import modelo.Usuario;
import modelo.Prestamo;
import modelo.Reserva;
import utilidades.PersistenciaArchivos;
import excepciones.ElementoNoEncontradoException;
import java.io.IOException;
import java.util.List;

public class BibliotecaApp extends Application {

    private static GestorLibros gestorLibros;
    private static GestorUsuarios gestorUsuarios;
    private static GestorPrestamos gestorPrestamos;
    private static GestorReservas gestorReservas;

    @Override
    public void start(Stage primaryStage) {
        inicializarSistema();

        primaryStage.setTitle("Sistema de Biblioteca");
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(700);

        try {
            Image icon = new Image(getClass().getResourceAsStream("/resources/icon1.png"));
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            System.out.println("No se pudo cargar el icono");
        }

        LoginController loginController = new LoginController(primaryStage);
        loginController.show();
    }

    private void inicializarSistema() {
        gestorLibros = new GestorLibros();
        gestorUsuarios = new GestorUsuarios();
        gestorPrestamos = new GestorPrestamos(gestorLibros, gestorUsuarios);
        gestorReservas = gestorPrestamos.getGestorReservas();

        System.out.println();
        System.out.println("=== SISTEMA DE BIBLIOTECA - INICIALIZANDO ===");
        System.out.println();

        try {

            List<Usuario> usuariosCargados = PersistenciaArchivos.cargarUsuarios();
            for (Usuario usuario : usuariosCargados) {
                gestorUsuarios.agregarUsuario(usuario);
            }

            List<Libro> librosCargados = PersistenciaArchivos.cargarLibros();
            for (Libro libro : librosCargados) {
                gestorLibros.agregarLibro(libro);
            }
            List<PersistenciaArchivos.DatosPrestamo> datosPrestamos = PersistenciaArchivos.cargarPrestamos();
            for (PersistenciaArchivos.DatosPrestamo datos : datosPrestamos) {
                try {
                    Usuario usuario = gestorUsuarios.buscarUsuario(datos.idUsuario);
                    Libro libro = gestorLibros.buscarLibro(datos.idLibro);

                    Prestamo prestamo = new Prestamo(
                            datos.idPrestamo,
                            usuario,
                            libro,
                            datos.fechaPrestamo,
                            datos.fechaDevolucionEsperada,
                            datos.fechaDevolucionReal,
                            datos.estado);

                    if (datos.estado == Prestamo.EstadoPrestamo.ACTIVO) {

                        if (libro.isDisponible()) {
                            try {
                                libro.prestar();
                            } catch (excepciones.OperacionInvalidaException e) {
                                System.err.println("Error al marcar libro prestado: " + e.getMessage());
                            }
                        }

                        gestorPrestamos.restaurarPrestamo(prestamo);
                    } else {

                        gestorPrestamos.restaurarHistorial(prestamo);
                    }

                } catch (ElementoNoEncontradoException e) {
                    System.out.println("Advertencia: No se pudo restaurar préstamo: " + datos.idPrestamo);
                }
            }

            List<PersistenciaArchivos.DatosReserva> datosReservas = PersistenciaArchivos.cargarReservas();
            for (PersistenciaArchivos.DatosReserva datos : datosReservas) {
                try {
                    Usuario usuario = gestorUsuarios.buscarUsuario(datos.idUsuario);
                    Libro libro = gestorLibros.buscarLibro(datos.idLibro);

                    Reserva reserva = new Reserva(
                            datos.idReserva,
                            usuario,
                            libro,
                            datos.fechaReserva,
                            datos.estado);

                    gestorReservas.restaurarReserva(reserva);
                } catch (ElementoNoEncontradoException e) {
                    System.out.println("Advertencia: No se pudo restaurar reserva: " + datos.idReserva);
                }
            }

            int totalDatos = usuariosCargados.size() + librosCargados.size() +
                    datosPrestamos.size() + datosReservas.size();

            if (totalDatos > 0) {
                System.out.println(String.format(
                        "✓ OK: Datos cargados → %d usuarios, %d libros, %d préstamos, %d reservas",
                        usuariosCargados.size(),
                        librosCargados.size(),
                        datosPrestamos.size(),
                        datosReservas.size()));
            } else {
                System.out.println("INFO: No se encontraron datos previos. Sistema iniciado vacío.");
            }

        } catch (IOException e) {
            System.out.println("⚠ AVISO: Error al acceder a los archivos de datos.");
        } catch (Exception e) {
            System.out.println("⚠ AVISO: Error al cargar datos: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        System.out.println();
        System.out.println("=== CERRANDO SISTEMA - GUARDANDO DATOS ===");
        System.out.println();

        try {
            PersistenciaArchivos.guardarLibros(gestorLibros.obtenerTodosLibros());
            System.out.println("✓ OK: Libros guardados correctamente");

            PersistenciaArchivos.guardarUsuarios(gestorUsuarios.obtenerTodosUsuarios());
            System.out.println("✓ OK: Usuarios guardados correctamente");

            PersistenciaArchivos.guardarPrestamos(gestorPrestamos.obtenerTodosPrestamos());
            System.out.println("✓ OK: Préstamos guardados correctamente");

            PersistenciaArchivos.guardarReservas(gestorReservas.obtenerTodasReservas());
            System.out.println("✓ OK: Reservas guardadas correctamente");

            System.out.println();
            System.out.println("✓ Todos los datos se guardaron exitosamente");

        } catch (IOException e) {
            System.err.println("✗ ERROR: No se pudieron guardar los datos: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("✗ ERROR: Error inesperado al guardar: " + e.getMessage());
        }

        System.out.println();
        System.out.println("=== Sistema cerrado. ¡Hasta luego! ===");
        System.out.println();
    }

    public static GestorLibros getGestorLibros() {
        return gestorLibros;
    }

    public static GestorUsuarios getGestorUsuarios() {
        return gestorUsuarios;
    }

    public static GestorPrestamos getGestorPrestamos() {
        return gestorPrestamos;
    }

    public static GestorReservas getGestorReservas() {
        return gestorReservas;
    }

    public static void cancelarPrestamoApp(String idPrestamo) {
        try {
            gestorPrestamos.cancelarPrestamo(idPrestamo);
            PersistenciaArchivos.guardarLibros(gestorLibros.obtenerTodosLibros());
            PersistenciaArchivos.guardarUsuarios(gestorUsuarios.obtenerTodosUsuarios());
            PersistenciaArchivos.guardarPrestamos(gestorPrestamos.obtenerTodosPrestamos());
        } catch (Exception e) {
            System.err.println("Error al cancelar préstamo: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static javafx.scene.image.Image obtenerIcono() {
        try {
            return new javafx.scene.image.Image(BibliotecaApp.class.getResourceAsStream("/resources/icon1.png"));
        } catch (Exception e) {
            System.err.println("No se pudo cargar el icono: " + e.getMessage());
            return null;
        }
    }

    public static void aplicarIconoAStage(javafx.stage.Stage stage) {
        javafx.scene.image.Image icono = obtenerIcono();
        if (icono != null) {
            stage.getIcons().add(icono);
        }
    }

    public static <T> javafx.scene.control.Dialog<T> aplicarIconoADialog(javafx.scene.control.Dialog<T> dialog) {

        dialog.setOnShown(_ -> {
            javafx.stage.Stage stage = (javafx.stage.Stage) dialog.getDialogPane().getScene().getWindow();
            aplicarIconoAStage(stage);
        });
        return dialog;
    }

    public static javafx.scene.control.Alert aplicarIconoAAlert(javafx.scene.control.Alert alert) {

        alert.setOnShown(_ -> {
            javafx.stage.Stage stage = (javafx.stage.Stage) alert.getDialogPane().getScene().getWindow();
            aplicarIconoAStage(stage);
        });
        return alert;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
