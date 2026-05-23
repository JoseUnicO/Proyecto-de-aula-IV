package utilidades;

import modelo.Libro;
import modelo.Usuario;
import modelo.Prestamo;
import modelo.Admin;
import modelo.Reserva;
import excepciones.OperacionInvalidaException;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PersistenciaArchivos {

    private static final String RUTA_LIBROS = "datos/libros.txt";
    private static final String RUTA_USUARIOS = "datos/usuarios.txt";
    private static final String RUTA_PRESTAMOS = "datos/prestamos.txt";
    private static final String RUTA_RESERVAS = "datos/reservas.txt";
    private static final String RUTA_ADMIN = "datos/admin.txt";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public static void guardarLibros(List<Libro> libros) throws IOException {
        crearDirectorioSiNoExiste();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(RUTA_LIBROS))) {
            for (Libro libro : libros) {
                String linea = String.format(
                        "%s|%s|%s|%s|%d|%s|%b",
                        libro.getId(),
                        libro.getTitulo(),
                        libro.getAutor(),
                        libro.getIsbn(),
                        libro.getAnioPublicacion(),
                        libro.getCategoria(),
                        libro.isDisponible());
                writer.write(linea);
                writer.newLine();
            }
        }
    }

    public static List<Libro> cargarLibros() throws IOException, OperacionInvalidaException {
        List<Libro> libros = new ArrayList<>();
        File archivo = new File(RUTA_LIBROS);
        if (!archivo.exists()) {
            return libros;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] datos = linea.split("\\|");
                if (datos.length == 7) {
                    Libro libro = new Libro(
                            datos[0],
                            datos[1],
                            datos[2],
                            datos[3],
                            Integer.parseInt(datos[4]),
                            datos[5]);

                    boolean disponible = Boolean.parseBoolean(datos[6]);
                    if (!disponible) {
                        try {
                            libro.prestar();
                        } catch (OperacionInvalidaException e) {

                        }
                    }

                    libros.add(libro);
                }
            }
        }

        return libros;
    }

    public static void guardarUsuarios(List<Usuario> usuarios) throws IOException {
        crearDirectorioSiNoExiste();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(RUTA_USUARIOS))) {
            for (Usuario usuario : usuarios) {
                String linea = String.format(
                        Locale.US,
                        "%s|%s|%s|%d|%d|%.2f",
                        usuario.getId(),
                        usuario.getNombre(),
                        usuario.getEmail(),
                        usuario.getLimitePrestamos(),
                        usuario.getPrestamosActuales(),
                        usuario.getMultaAcumulada());
                writer.write(linea);
                writer.newLine();
            }
        }
    }

    public static List<Usuario> cargarUsuarios() throws IOException, OperacionInvalidaException {
        List<Usuario> usuarios = new ArrayList<>();
        File archivo = new File(RUTA_USUARIOS);
        if (!archivo.exists()) {
            return usuarios;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] datos = linea.split("\\|");
                if (datos.length == 6) {
                    Usuario usuario = new Usuario(
                            datos[0],
                            datos[1],
                            datos[2],
                            Integer.parseInt(datos[3]));

                    int prestamosActuales = Integer.parseInt(datos[4]);
                    for (int i = 0; i < prestamosActuales; i++) {
                        usuario.agregarPrestamo();
                    }

                    String textoMulta = datos[5].replace(",", ".");
                    double multa = Double.parseDouble(textoMulta);
                    if (multa > 0) {
                        usuario.agregarMulta(multa);
                    }

                    usuarios.add(usuario);
                }
            }
        }

        return usuarios;
    }

    public static void guardarPrestamos(List<Prestamo> prestamos) throws IOException {
        crearDirectorioSiNoExiste();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(RUTA_PRESTAMOS))) {
            for (Prestamo prestamo : prestamos) {
                String fechaDevReal = (prestamo.getFechaDevolucionReal() != null)
                        ? prestamo.getFechaDevolucionReal().format(DATE_FORMATTER)
                        : "null";

                String linea = String.format(
                        "%s|%s|%s|%s|%s|%s|%s",
                        prestamo.getIdPrestamo(),
                        prestamo.getUsuario().getId(),
                        prestamo.getLibro().getId(),
                        prestamo.getFechaPrestamo().format(DATE_FORMATTER),
                        prestamo.getFechaDevolucionEsperada().format(DATE_FORMATTER),
                        fechaDevReal,
                        prestamo.getEstado().name());
                writer.write(linea);
                writer.newLine();
            }
        }
    }

    public static class DatosPrestamo {
        public String idPrestamo;
        public String idUsuario;
        public String idLibro;
        public LocalDate fechaPrestamo;
        public LocalDate fechaDevolucionEsperada;
        public LocalDate fechaDevolucionReal;
        public Prestamo.EstadoPrestamo estado;

        public DatosPrestamo(String idPrestamo, String idUsuario, String idLibro,
                LocalDate fechaPrestamo, LocalDate fechaDevolucionEsperada,
                LocalDate fechaDevolucionReal, Prestamo.EstadoPrestamo estado) {
            this.idPrestamo = idPrestamo;
            this.idUsuario = idUsuario;
            this.idLibro = idLibro;
            this.fechaPrestamo = fechaPrestamo;
            this.fechaDevolucionEsperada = fechaDevolucionEsperada;
            this.fechaDevolucionReal = fechaDevolucionReal;
            this.estado = estado;
        }
    }

    public static List<DatosPrestamo> cargarPrestamos() throws IOException {
        List<DatosPrestamo> prestamos = new ArrayList<>();
        File archivo = new File(RUTA_PRESTAMOS);
        if (!archivo.exists()) {
            return prestamos;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] datos = linea.split("\\|");
                if (datos.length == 7) {
                    LocalDate fechaDevReal = datos[5].equals("null")
                            ? null
                            : LocalDate.parse(datos[5], DATE_FORMATTER);

                    DatosPrestamo p = new DatosPrestamo(
                            datos[0],
                            datos[1],
                            datos[2],
                            LocalDate.parse(datos[3], DATE_FORMATTER),
                            LocalDate.parse(datos[4], DATE_FORMATTER),
                            fechaDevReal,
                            Prestamo.EstadoPrestamo.valueOf(datos[6]));
                    prestamos.add(p);
                }
            }
        }

        return prestamos;
    }

    public static class DatosReserva {
        public String idReserva;
        public String idUsuario;
        public String idLibro;
        public LocalDate fechaReserva;
        public Reserva.EstadoReserva estado;

        public DatosReserva(String idReserva, String idUsuario, String idLibro,
                LocalDate fechaReserva, Reserva.EstadoReserva estado) {
            this.idReserva = idReserva;
            this.idUsuario = idUsuario;
            this.idLibro = idLibro;
            this.fechaReserva = fechaReserva;
            this.estado = estado;
        }
    }

    public static void guardarReservas(List<Reserva> reservas) throws IOException {
        crearDirectorioSiNoExiste();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(RUTA_RESERVAS))) {
            for (Reserva reserva : reservas) {
                String linea = String.format(
                        "%s|%s|%s|%s|%s",
                        reserva.getIdReserva(),
                        reserva.getUsuario().getId(),
                        reserva.getLibro().getId(),
                        reserva.getFechaReserva().format(DATE_FORMATTER),
                        reserva.getEstado().name());
                writer.write(linea);
                writer.newLine();
            }
        }
    }

    public static List<DatosReserva> cargarReservas() throws IOException {
        List<DatosReserva> reservas = new ArrayList<>();
        File archivo = new File(RUTA_RESERVAS);
        if (!archivo.exists()) {
            return reservas;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] datos = linea.split("\\|");
                if (datos.length == 5) {
                    DatosReserva r = new DatosReserva(
                            datos[0],
                            datos[1],
                            datos[2],
                            LocalDate.parse(datos[3], DATE_FORMATTER),
                            Reserva.EstadoReserva.valueOf(datos[4]));
                    reservas.add(r);
                }
            }
        }

        return reservas;
    }

    public static void guardarAdmin(Admin admin) throws IOException {
        crearDirectorioSiNoExiste();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(RUTA_ADMIN))) {
            String linea = String.format(
                    "%s|%s|%s",
                    admin.getUsuario(),
                    admin.getPassword(),
                    admin.getNombre());
            writer.write(linea);
            writer.newLine();
        }
    }

    public static Admin cargarAdmin() throws IOException {
        File archivo = new File(RUTA_ADMIN);
        if (!archivo.exists()) {
            Admin adminPorDefecto = new Admin("Bibliotecario", "Bibliotecario2025", "Administrador");
            guardarAdmin(adminPorDefecto);
            return adminPorDefecto;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            String linea = reader.readLine();
            if (linea != null && !linea.trim().isEmpty()) {
                String[] datos = linea.split("\\|");
                if (datos.length == 3) {
                    return new Admin(datos[0], datos[1], datos[2]);
                }
            }
        }

        Admin adminPorDefecto = new Admin("Bibliotecario", "Bibliotecario2025", "Administrador");
        guardarAdmin(adminPorDefecto);
        return adminPorDefecto;
    }

    private static void crearDirectorioSiNoExiste() {
        File directorio = new File("datos");
        if (!directorio.exists()) {
            directorio.mkdir();
        }
    }
}
