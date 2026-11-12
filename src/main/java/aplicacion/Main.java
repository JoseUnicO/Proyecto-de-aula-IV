package aplicacion;

import controlador.GestorLibros;
import controlador.GestorUsuarios;
import controlador.GestorPrestamos;
import controlador.GestorReservas;
import excepciones.BibliotecaException;
import excepciones.ElementoNoEncontradoException;
import excepciones.EstructuraVaciaException;
import excepciones.OperacionInvalidaException;
import modelo.Libro;
import modelo.Usuario;
import modelo.Reserva;
import modelo.Admin;
import utilidades.GeneradorReportes;
import seguridad.Autenticacion;
import seguridad.Sesion;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    private static utilidades.SistemaBiblioteca sistema;
    private static GestorLibros gestorLibros;
    private static GestorUsuarios gestorUsuarios;
    private static GestorPrestamos gestorPrestamos;
    private static GestorReservas gestorReservas;
    private static GeneradorReportes generadorReportes;
    private static Admin adminSistema;
    private static Sesion sesion;
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            if (iniciarSesionAdmin()) {
                inicializarSistema();
                try {
                    ejecutarMenuPrincipal();
                } catch (java.util.NoSuchElementException e) {
                    System.out.println("Fin de entrada");
                } finally {
                    cerrarSistema();
                }
            } else {
                System.out.println("Acceso denegado. Cerrando el sistema...");
                scanner.close();
            }
        } catch (java.util.NoSuchElementException e) {
            System.out.println("Fin de entrada");
            scanner.close();
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            scanner.close();
            System.exit(1);
        }
    }

    private static void inicializarSistema() {
        try {
            sistema = utilidades.SistemaBiblioteca.cargar(sesion);
            gestorLibros = sistema.libros;
            gestorUsuarios = sistema.usuarios;
            gestorPrestamos = sistema.prestamos;
            gestorReservas = sistema.reservas;
            generadorReportes = new GeneradorReportes(gestorLibros, gestorUsuarios, gestorPrestamos);
        } catch (IOException e) {
            throw new IllegalStateException("Inicio bloqueado: " + e.getMessage(), e);
        }
    }

    private static boolean iniciarSesionAdmin() {
        System.out.println("===== INICIO DE SESIÓN =====");
        int intentosMaximos = 5;
        for (int intento = 1; intento <= intentosMaximos; intento++) {
            String usuario = leerTexto("Usuario: ");
            System.out.print("Contraseña: ");
            String password = System.console() != null ? new String(System.console().readPassword())
                    : scanner.nextLine();

            try {
                sesion = new Autenticacion().ingresar(usuario, password.toCharArray());
                adminSistema = sesion.administrador();
                System.out.println("[OK] Acceso concedido. Bienvenido, " + adminSistema.getNombre());
                return true;
            } catch (java.sql.SQLException e) {
                throw new IllegalStateException("MySQL no está disponible. No se pudo iniciar sesión.", e);
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
        return false;
    }

    private static void ejecutarMenuPrincipal() {
        boolean salir = false;
        while (!salir) {
            mostrarMenuPrincipal();
            int opcion = leerEntero("Seleccione una opción: ");

            try {
                switch (opcion) {
                    case 1 -> menuLibros();
                    case 2 -> menuUsuarios();
                    case 3 -> menuPrestamos();
                    case 4 -> menuReservas();
                    case 5 -> menuReportes();
                    case 0 -> salir = true;
                    default -> System.out.println("Opción no válida. Intente de nuevo.");
                }
            } catch (BibliotecaException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Ocurrió un error inesperado: " + e.getMessage());
            }
            System.out.println();
        }
    }

    private static void mostrarMenuPrincipal() {
        System.out.println("======================================");
        System.out.println("          MENÚ PRINCIPAL");
        System.out.println("======================================");
        System.out.println("1. Gestión de Libros");
        System.out.println("2. Gestión de Usuarios");
        System.out.println("3. Gestión de Préstamos");
        System.out.println("4. Gestión de Reservas");
        System.out.println("5. Reportes");
        System.out.println("0. Salir");
        System.out.println("======================================");
    }

    private static void menuLibros() throws BibliotecaException {
        boolean volver = false;
        while (!volver) {
            System.out.println("\n---- MENÚ LIBROS ----");
            System.out.println("1. Agregar libro");
            System.out.println("2. Buscar libro por ID");
            System.out.println("3. Mostrar todos los libros");
            System.out.println("4. Buscar por categoría");
            System.out.println("5. Buscar por autor");
            System.out.println("6. Consultar ejemplares");
            System.out.println("7. Agregar ejemplares");
            System.out.println("8. Retirar ejemplar");
            System.out.println("0. Volver al menú principal");

            int opcion = leerEntero("Seleccione una opción: ");
            switch (opcion) {
                case 1 -> opcionAgregarLibro();
                case 2 -> opcionBuscarLibro();
                case 3 -> opcionMostrarLibros();
                case 4 -> opcionBuscarPorCategoria();
                case 5 -> opcionBuscarPorAutor();
                case 6 ->
                    gestorLibros.buscarLibro(leerTexto("ID del libro: ")).getEjemplares().forEach(System.out::println);
                case 7 -> gestorLibros.agregarEjemplares(leerTexto("ID del libro: "), leerEntero("Cantidad: "));
                case 8 -> gestorLibros.retirarEjemplar(leerTexto("ID del libro: "), leerTexto("ID del ejemplar: "));
                case 0 -> volver = true;
                default -> System.out.println("Opción no válida.");
            }
        }
    }

    private static void opcionAgregarLibro() throws BibliotecaException {
        System.out.println("\n--- Agregar libro ---");
        String id = leerTexto("ID: ");
        String titulo = leerTexto("Título: ");
        String autor = leerTexto("Autor: ");
        String isbn = leerTexto("ISBN (10 o 13 dígitos): ");
        int anio = leerEntero("Año de publicación: ");
        String categoria = leerTexto("Categoría: ");

        try {
            Libro libro = new Libro(id, titulo, autor, isbn, anio, categoria);
            gestorLibros.agregarLibro(libro, leerEntero("Cantidad de copias: "));

            System.out.println("[OK] Libro agregado y guardado correctamente.");
        } catch (OperacionInvalidaException e) {
            System.out.println("Error al agregar libro: " + e.getMessage());
        }
    }

    private static void opcionBuscarLibro() {
        System.out.println("\n--- Buscar libro por ID ---");
        String id = leerTexto("ID del libro: ");
        try {
            Libro libro = gestorLibros.buscarLibro(id);
            System.out.println("Libro encontrado:");
            System.out.println(libro);
        } catch (ElementoNoEncontradoException e) {
            System.out.println("No se encontró un libro con ese ID.");
        }
    }

    private static void opcionMostrarLibros() {
        System.out.println("\n--- Lista de libros ---");
        try {
            gestorLibros.mostrarLibrosOrdenados();
        } catch (EstructuraVaciaException e) {
            System.out.println("No hay libros registrados.");
        }
    }

    private static void opcionBuscarPorCategoria() {
        System.out.println("\n--- Buscar libros por categoría ---");
        String categoria = leerTexto("Categoría: ");
        try {
            gestorLibros.buscarPorCategoria(categoria);
        } catch (EstructuraVaciaException e) {
            System.out.println("No hay libros registrados.");
        }
    }

    private static void opcionBuscarPorAutor() {
        System.out.println("\n--- Buscar libros por autor ---");
        String autor = leerTexto("Autor (o parte del nombre): ");
        try {
            gestorLibros.buscarPorAutor(autor);
        } catch (EstructuraVaciaException e) {
            System.out.println("No hay libros registrados.");
        }
    }

    private static void menuUsuarios() throws BibliotecaException {
        boolean volver = false;
        while (!volver) {
            System.out.println("\n---- MENÚ USUARIOS ----");
            System.out.println("1. Agregar usuario");
            System.out.println("2. Buscar usuario por ID");
            System.out.println("3. Mostrar todos los usuarios");
            System.out.println("4. Mostrar usuarios con multas");
            System.out.println("0. Volver al menú principal");

            int opcion = leerEntero("Seleccione una opción: ");
            switch (opcion) {
                case 1 -> opcionAgregarUsuario();
                case 2 -> opcionBuscarUsuario();
                case 3 -> opcionMostrarUsuarios();
                case 4 -> opcionMostrarUsuariosConMultas();
                case 0 -> volver = true;
                default -> System.out.println("Opción no válida.");
            }
        }
    }

    private static void opcionAgregarUsuario() {
        System.out.println("\n--- Agregar usuario ---");
        String id = leerTexto("ID: ");
        String nombre = leerTexto("Nombre: ");
        String email = leerTexto("Email: ");
        int limite = leerEntero("Límite de préstamos: ");

        try {
            Usuario usuario = new Usuario(id, nombre, email, limite);
            gestorUsuarios.agregarUsuario(usuario);

            System.out.println("[OK] Usuario agregado y guardado correctamente.");
        } catch (OperacionInvalidaException e) {
            System.out.println("Error al agregar usuario: " + e.getMessage());
        }
    }

    private static void opcionBuscarUsuario() {
        System.out.println("\n--- Buscar usuario por ID ---");
        String id = leerTexto("ID del usuario: ");
        try {
            Usuario usuario = gestorUsuarios.buscarUsuario(id);
            System.out.println("Usuario encontrado:");
            System.out.println(usuario);
        } catch (ElementoNoEncontradoException e) {
            System.out.println("No se encontró un usuario con ese ID.");
        }
    }

    private static void opcionMostrarUsuarios() {
        System.out.println("\n--- Lista de usuarios ---");
        try {
            gestorUsuarios.mostrarUsuariosOrdenados();
        } catch (EstructuraVaciaException e) {
            System.out.println("No hay usuarios registrados.");
        }
    }

    private static void opcionMostrarUsuariosConMultas() {
        System.out.println("\n--- Usuarios con multas ---");
        try {
            gestorUsuarios.mostrarUsuariosConMultas();
        } catch (EstructuraVaciaException e) {
            System.out.println("No hay usuarios registrados.");
        }
    }

    private static void menuPrestamos() throws BibliotecaException {
        boolean volver = false;
        while (!volver) {
            System.out.println("\n---- MENÚ PRÉSTAMOS ----");
            System.out.println("1. Registrar préstamo");
            System.out.println("2. Registrar devolución");
            System.out.println("3. Renovar préstamo");
            System.out.println("4. Ver préstamos activos");
            System.out.println("5. Ver préstamos vencidos");
            System.out.println("6. Ver préstamos por usuario");
            System.out.println("0. Volver al menú principal");

            int opcion = leerEntero("Seleccione una opción: ");
            switch (opcion) {
                case 1 -> opcionRegistrarPrestamo();
                case 2 -> opcionRegistrarDevolucion();
                case 3 -> opcionRenovarPrestamo();
                case 4 -> opcionVerPrestamosActivos();
                case 5 -> opcionVerPrestamosVencidos();
                case 6 -> opcionVerPrestamosPorUsuario();
                case 0 -> volver = true;
                default -> System.out.println("Opción no válida.");
            }
        }
    }

    private static void opcionRegistrarPrestamo() {
        System.out.println("\n--- Registrar préstamo ---");
        String idUsuario = leerTexto("ID del usuario: ");
        String idLibro = leerTexto("ID del libro: ");

        try {
            var prestamo = gestorPrestamos.registrarPrestamo(idUsuario, idLibro);

            System.out.println("[OK] Préstamo registrado y guardado:");
            System.out.println(prestamo);
        } catch (BibliotecaException e) {
            System.out.println("Error al registrar préstamo: " + e.getMessage());
        }
    }

    private static void opcionRegistrarDevolucion() {
        System.out.println("\n--- Registrar devolución ---");
        String idPrestamo = leerTexto("ID del préstamo: ");

        try {
            gestorPrestamos.registrarDevolucion(idPrestamo);

            System.out.println("[OK] Devolución registrada y guardada correctamente.");
        } catch (BibliotecaException e) {
            System.out.println("Error al registrar devolución: " + e.getMessage());
        }
    }

    private static void opcionRenovarPrestamo() {
        System.out.println("\n--- Renovar préstamo ---");
        String idPrestamo = leerTexto("ID del préstamo: ");

        try {
            gestorPrestamos.renovarPrestamo(idPrestamo);

            System.out.println("[OK] Préstamo renovado y guardado correctamente.");
        } catch (BibliotecaException e) {
            System.out.println("Error al renovar préstamo: " + e.getMessage());
        }
    }

    private static void opcionVerPrestamosActivos() {
        try {
            gestorPrestamos.mostrarPrestamosActivos();
        } catch (EstructuraVaciaException e) {
            System.out.println("No hay préstamos registrados.");
        }
    }

    private static void opcionVerPrestamosVencidos() {
        try {
            gestorPrestamos.mostrarPrestamosVencidos();
        } catch (EstructuraVaciaException e) {
            System.out.println("No hay préstamos registrados.");
        }
    }

    private static void opcionVerPrestamosPorUsuario() {
        String idUsuario = leerTexto("ID del usuario: ");
        try {
            gestorPrestamos.mostrarPrestamosPorUsuario(idUsuario);
        } catch (BibliotecaException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void menuReservas() throws BibliotecaException {
        boolean volver = false;
        while (!volver) {
            System.out.println("\n---- MENÚ RESERVAS ----");
            System.out.println("1. Registrar reserva");
            System.out.println("2. Cancelar reserva");
            System.out.println("3. Ver reservas de un libro");
            System.out.println("4. Ver todas las reservas pendientes");
            System.out.println("5. Ver reservas de un usuario");
            System.out.println("0. Volver al menú principal");

            int opcion = leerEntero("Seleccione una opción: ");
            switch (opcion) {
                case 1 -> opcionRegistrarReserva();
                case 2 -> opcionCancelarReserva();
                case 3 -> opcionVerReservasDeLibro();
                case 4 -> opcionVerTodasReservas();
                case 5 -> opcionVerReservasDeUsuario();
                case 0 -> volver = true;
                default -> System.out.println("Opción no válida.");
            }
        }
    }

    private static void opcionRegistrarReserva() {
        System.out.println("\n--- Registrar reserva ---");
        String idUsuario = leerTexto("ID del usuario: ");
        String idLibro = leerTexto("ID del libro: ");

        try {
            Usuario usuario = gestorUsuarios.buscarUsuario(idUsuario);
            Libro libro = gestorLibros.buscarLibro(idLibro);
            Reserva reserva = gestorReservas.agregarReserva(usuario, libro);
            System.out.println("[OK] Reserva registrada:");
            System.out.println(reserva);
        } catch (BibliotecaException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void opcionCancelarReserva() {
        System.out.println("--- Cancelar reserva ---");
        String idReserva = leerTexto("ID de la reserva: ");
        try {
            gestorReservas.cancelarReserva(idReserva);
            System.out.println("[OK] Reserva cancelada correctamente.");
        } catch (excepciones.OperacionInvalidaException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (ElementoNoEncontradoException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void opcionVerReservasDeLibro() {
        System.out.println("\n--- Ver reservas de un libro ---");
        String idLibro = leerTexto("ID del libro: ");
        try {
            gestorReservas.mostrarReservasDeLibro(idLibro);
        } catch (EstructuraVaciaException e) {
            System.out.println("Error al mostrar reservas.");
        }
    }

    private static void opcionVerTodasReservas() {
        gestorReservas.mostrarTodasReservasPendientes();
    }

    private static void opcionVerReservasDeUsuario() {
        String idUsuario = leerTexto("ID del usuario: ");
        gestorReservas.mostrarReservasDeUsuario(idUsuario);
    }

    private static void menuReportes() {
        boolean volver = false;
        while (!volver) {
            System.out.println("\n---- MENÚ REPORTES ----");
            System.out.println("1. Reporte general");
            System.out.println("2. Reporte de libros");
            System.out.println("3. Reporte de usuarios");
            System.out.println("4. Reporte de préstamos");
            System.out.println("5. Reporte completo");
            System.out.println("6. Top 5 usuarios más activos");
            System.out.println("7. Estado financiero (multas)");
            System.out.println("0. Volver al menú principal");

            int opcion = leerEntero("Seleccione una opción: ");
            switch (opcion) {
                case 1 -> generadorReportes.generarReporteGeneral();
                case 2 -> generadorReportes.generarReporteLibros();
                case 3 -> generadorReportes.generarReporteUsuarios();
                case 4 -> generadorReportes.generarReportePrestamos();
                case 5 -> generadorReportes.generarReporteCompleto();
                case 6 -> generadorReportes.mostrarTop5UsuariosActivos();
                case 7 -> generadorReportes.mostrarEstadoFinanciero();
                case 0 -> volver = true;
                default -> System.out.println("Opción no válida.");
            }
        }
    }

    private static int leerEntero(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String entrada = scanner.nextLine();
            try {
                return Integer.parseInt(entrada.trim());
            } catch (NumberFormatException e) {
                System.out.println("Por favor, ingrese un número entero válido.");
            }
        }
    }

    private static String leerTexto(String mensaje) {
        System.out.print(mensaje);
        return scanner.nextLine().trim();
    }

    private static void cerrarSistema() {
        if (sistema != null)
            sistema.close();
        System.out.println("Sesión cerrada. Los cambios ya están confirmados en MySQL.");
        scanner.close();
    }
}
