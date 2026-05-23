package utilidades;

import controlador.*;
import modelo.*;
import excepciones.*;
import java.time.LocalDate;
import java.util.*;

public class GeneradorReportes {
    private GestorLibros gestorLibros;
    private GestorUsuarios gestorUsuarios;
    private GestorPrestamos gestorPrestamos;

    public GeneradorReportes(GestorLibros gestorLibros, GestorUsuarios gestorUsuarios,
            GestorPrestamos gestorPrestamos) {
        this.gestorLibros = gestorLibros;
        this.gestorUsuarios = gestorUsuarios;
        this.gestorPrestamos = gestorPrestamos;
    }

    public void generarReporteGeneral() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("         REPORTE GENERAL DEL SISTEMA");
        System.out.println("=".repeat(60));

        System.out.println("\n📊 ESTADÍSTICAS GENERALES:");
        System.out.println("   • Total de libros en el sistema: " + gestorLibros.getTotalLibros());
        System.out.println("   • Libros disponibles: " + gestorLibros.contarLibrosDisponibles());
        System.out.println("   • Libros prestados: " +
                (gestorLibros.getTotalLibros() - gestorLibros.contarLibrosDisponibles()));
        System.out.println("   • Total de usuarios registrados: " + gestorUsuarios.getTotalUsuarios());
        System.out.println("   • Préstamos activos: " + gestorPrestamos.contarPrestamosActivos());
        System.out.println("   • Historial total de préstamos: " + gestorPrestamos.contarHistorial());

        System.out.println("\n" + "=".repeat(60));
    }

    public void generarReporteLibros() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("           REPORTE DE LIBROS");
        System.out.println("=".repeat(60));

        try {
            Map<String, Integer> librosPorCategoria = contarLibrosPorCategoria();
            Map<String, Integer> librosPorAutor = contarLibrosPorAutor();

            System.out.println("\n📚 DISTRIBUCIÓN POR CATEGORÍA:");
            librosPorCategoria.forEach((categoria, cantidad) -> System.out
                    .println(String.format("   • %s: %d libro(s)", categoria, cantidad)));

            System.out.println("\n✍️ DISTRIBUCIÓN POR AUTOR:");
            librosPorAutor.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .forEach(entry -> System.out
                            .println(String.format("   • %s: %d libro(s)", entry.getKey(), entry.getValue())));

            System.out.println("\n📖 DISPONIBILIDAD:");
            System.out.println("   • Libros disponibles: " + gestorLibros.contarLibrosDisponibles());
            System.out.println("   • Libros prestados: " +
                    (gestorLibros.getTotalLibros() - gestorLibros.contarLibrosDisponibles()));

            double tasaDisponibilidad = gestorLibros.getTotalLibros() > 0
                    ? (gestorLibros.contarLibrosDisponibles() * 100.0 / gestorLibros.getTotalLibros())
                    : 0;
            System.out.println(String.format("   • Tasa de disponibilidad: %.1f%%", tasaDisponibilidad));

        } catch (Exception e) {
            System.out.println("[AVISO] Error al generar reporte de libros: " + e.getMessage());
        }

        System.out.println("\n" + "=".repeat(60));
    }

    public void generarReporteUsuarios() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("           REPORTE DE USUARIOS");
        System.out.println("=".repeat(60));

        try {
            List<Usuario> usuarios = gestorUsuarios.obtenerTodosUsuarios();

            int usuariosConPrestamos = 0;
            int usuariosConMultas = 0;
            double totalMultas = 0.0;
            int totalPrestamosActivos = 0;

            for (Usuario usuario : usuarios) {
                if (usuario.getPrestamosActuales() > 0) {
                    usuariosConPrestamos++;
                    totalPrestamosActivos += usuario.getPrestamosActuales();
                }
                if (usuario.getMultaAcumulada() > 0) {
                    usuariosConMultas++;
                    totalMultas += usuario.getMultaAcumulada();
                }
            }

            System.out.println("\n👥 ESTADÍSTICAS DE USUARIOS:");
            System.out.println("   • Total de usuarios: " + gestorUsuarios.getTotalUsuarios());
            System.out.println("   • Usuarios con préstamos activos: " + usuariosConPrestamos);
            System.out.println("   • Usuarios con multas pendientes: " + usuariosConMultas);
            System.out.println("   • Total préstamos activos: " + totalPrestamosActivos);

            System.out.println("\n💰 MULTAS:");
            System.out.println(String.format("   • Total multas acumuladas: $%.2f", totalMultas));
            System.out.println("   • Usuarios con multas: " + usuariosConMultas);

            if (usuariosConMultas > 0) {
                System.out.println(String.format("   • Promedio de multa por usuario: $%.2f",
                        totalMultas / usuariosConMultas));
            }

            if (gestorUsuarios.getTotalUsuarios() > 0) {
                double promedioPrestamosPorUsuario = totalPrestamosActivos * 1.0 /
                        gestorUsuarios.getTotalUsuarios();
                System.out.println(String.format("\n📊 Promedio de préstamos por usuario: %.2f",
                        promedioPrestamosPorUsuario));
            }

        } catch (Exception e) {
            System.out.println("[AVISO] Error al generar reporte de usuarios: " + e.getMessage());
        }

        System.out.println("\n" + "=".repeat(60));
    }

    public void generarReportePrestamos() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("           REPORTE DE PRÉSTAMOS");
        System.out.println("=".repeat(60));

        try {
            int prestamosActivos = gestorPrestamos.contarPrestamosActivos();
            int historialTotal = gestorPrestamos.contarHistorial();
            int prestamosVencidos = contarPrestamosVencidos();

            System.out.println("\n📋 ESTADÍSTICAS DE PRÉSTAMOS:");
            System.out.println("   • Préstamos activos: " + prestamosActivos);
            System.out.println("   • Préstamos vencidos: " + prestamosVencidos);
            System.out.println("   • Historial total: " + historialTotal);
            System.out.println("   • Total préstamos realizados: " + (prestamosActivos + historialTotal));

            if (prestamosActivos > 0) {
                double tasaVencimiento = (prestamosVencidos * 100.0) / prestamosActivos;
                System.out.println(String.format("   • Tasa de vencimiento: %.1f%%", tasaVencimiento));
            }

            if (historialTotal > 0) {
                System.out.println("\n[OK] DEVOLUCIONES:");
                System.out.println("   • Total de devoluciones: " + historialTotal);
            }

        } catch (Exception e) {
            System.out.println("[AVISO] Error al generar reporte de préstamos: " + e.getMessage());
        }

        System.out.println("\n" + "=".repeat(60));
    }

    public void generarReporteCompleto() {
        System.out.println("\n\n");
        System.out.println("█".repeat(70));
        System.out.println("█" + " ".repeat(68) + "█");
        System.out.println("█" + " ".repeat(15) + "REPORTE COMPLETO DEL SISTEMA" + " ".repeat(26) + "█");
        System.out.println("█" + " ".repeat(20) + "Fecha: " + LocalDate.now() + " ".repeat(23) + "█");
        System.out.println("█" + " ".repeat(68) + "█");
        System.out.println("█".repeat(70));

        generarReporteGeneral();
        generarReporteLibros();
        generarReporteUsuarios();
        generarReportePrestamos();

        System.out.println("\n" + "█".repeat(70));
        System.out.println("█" + " ".repeat(68) + "█");
        System.out.println("█" + " ".repeat(23) + "FIN DEL REPORTE" + " ".repeat(30) + "█");
        System.out.println("█" + " ".repeat(68) + "█");
        System.out.println("█".repeat(70));
    }

    private Map<String, Integer> contarLibrosPorCategoria() {
        Map<String, Integer> categorias = new HashMap<>();
        List<Libro> libros = gestorLibros.obtenerTodosLibros();
        for (Libro libro : libros) {
            categorias.merge(libro.getCategoria(), 1, Integer::sum);
        }
        return categorias;
    }

    private Map<String, Integer> contarLibrosPorAutor() {
        Map<String, Integer> autores = new HashMap<>();
        List<Libro> libros = gestorLibros.obtenerTodosLibros();
        for (Libro libro : libros) {
            autores.merge(libro.getAutor(), 1, Integer::sum);
        }
        return autores;
    }

    private int contarPrestamosVencidos() {
        final int[] contador = { 0 };
        try {
            gestorPrestamos.getPrestamosActivos().recorrerEnOrden(prestamo -> {
                if (prestamo.estaVencido()) {
                    contador[0]++;
                }
            });
        } catch (EstructuraVaciaException e) {
            return 0;
        }
        return contador[0];
    }

    public void mostrarTop5UsuariosActivos() {
        System.out.println("\n🏆 TOP 5 USUARIOS MÁS ACTIVOS:");
        try {
            List<Usuario> usuarios = gestorUsuarios.obtenerTodosUsuarios();
            usuarios.stream()
                    .sorted((u1, u2) -> Integer.compare(u2.getPrestamosActuales(), u1.getPrestamosActuales()))
                    .limit(5)
                    .forEach(u -> System.out.println(String.format("   • %s - %d préstamo(s) activo(s)",
                            u.getNombre(), u.getPrestamosActuales())));
        } catch (Exception e) {
            System.out.println("   No hay datos suficientes");
        }
    }

    public void mostrarEstadoFinanciero() {
        System.out.println("\n💵 ESTADO FINANCIERO:");
        try {
            List<Usuario> usuarios = gestorUsuarios.obtenerTodosUsuarios();
            double totalMultas = usuarios.stream()
                    .mapToDouble(Usuario::getMultaAcumulada)
                    .sum();

            System.out.println(String.format("   • Total multas pendientes: $%.2f", totalMultas));
            System.out.println(String.format("   • Usuarios morosos: %d",
                    usuarios.stream().filter(u -> u.getMultaAcumulada() > 0).count()));
        } catch (Exception e) {
            System.out.println("   No hay datos disponibles");
        }
    }
}
