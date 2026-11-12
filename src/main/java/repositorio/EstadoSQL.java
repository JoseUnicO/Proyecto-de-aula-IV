package repositorio;

import java.sql.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.*;
import modelo.*;
import utilidades.SistemaBiblioteca;

/**
 * Filas persistentes. Sólo emite DML para registros efectivamente modificados.
 */
public final class EstadoSQL {
    public final Map<String, List<Object>> libros = new LinkedHashMap<>(), usuarios = new LinkedHashMap<>(),
            ejemplares = new LinkedHashMap<>(), prestamos = new LinkedHashMap<>(), reservas = new LinkedHashMap<>();

    public static EstadoSQL leer(Connection c) throws SQLException {
        EstadoSQL e = new EstadoSQL();
        leer(c, "SELECT id,titulo,autor,isbn,anio,categoria FROM libros ORDER BY id", e.libros);
        leer(c, "SELECT id,nombre,email,limite,multa FROM usuarios ORDER BY id", e.usuarios);
        leer(c, "SELECT id,usuario_id,libro_id,fecha,esperada,fecha_real,estado,ejemplar_id FROM prestamos ORDER BY id",
                e.prestamos);
        leer(c, "SELECT id,usuario_id,libro_id,fecha,estado,orden FROM reservas ORDER BY orden", e.reservas);
        leer(c, "SELECT id,libro_id FROM ejemplares ORDER BY id", e.ejemplares);
        return e;
    }

    private static void leer(Connection c, String sql, Map<String, List<Object>> destino) throws SQLException {
        try (Statement q = c.createStatement(); ResultSet r = q.executeQuery(sql)) {
            int n = r.getMetaData().getColumnCount();
            while (r.next()) {
                List<Object> fila = new ArrayList<>();
                for (int i = 1; i <= n; i++)
                    fila.add(r.getObject(i));
                destino.put(r.getString(1), fila);
            }
        }
    }

    private static LocalDate fecha(Object x) {
        return x == null ? null : x instanceof java.sql.Date d ? d.toLocalDate() : LocalDate.parse(x.toString());
    }

    public void cargarEn(SistemaBiblioteca s) throws Exception {
        Map<String, Libro> ls = new LinkedHashMap<>();
        Map<String, Usuario> us = new LinkedHashMap<>();
        Map<String, Libro> originales = new HashMap<>();
        for (Libro l : s.libros.obtenerTodosLibros())
            originales.put(l.getId(), l);
        for (var d : libros.values()) {
            Libro candidato = new Libro((String) d.get(0), (String) d.get(1), (String) d.get(2), (String) d.get(3),
                    ((Number) d.get(4)).intValue(), (String) d.get(5));
            Libro l = originales.get(candidato.getId());
            if (l == null)
                l = candidato;
            else
                l.actualizar(candidato);
            l.reponerEjemplares(java.util.List.of());
            ls.put(l.getId(), l);
        }
        for (var d : ejemplares.values()) {
            Libro l = Objects.requireNonNull(ls.get(d.get(1)), "Libro del ejemplar inexistente");
            var lista = new ArrayList<>(l.getEjemplares());
            lista.add(new Ejemplar((String) d.get(0)));
            l.reponerEjemplares(lista);
        }
        Map<String, Usuario> originalesU = new HashMap<>();
        for (Usuario u : s.usuarios.obtenerTodosUsuarios())
            originalesU.put(u.getId(), u);
        for (var d : usuarios.values()) {
            Usuario candidato = new Usuario((String) d.get(0), (String) d.get(1), (String) d.get(2),
                    ((Number) d.get(3)).intValue());
            Usuario u = originalesU.get(candidato.getId());
            if (u == null)
                u = candidato;
            else {
                u.setPrestamosActuales(0);
                u.actualizar(candidato);
            }
            u.restaurarMulta((BigDecimal) d.get(4));
            us.put(u.getId(), u);
        }
        Map<String, Prestamo> originalesP = new HashMap<>();
        for (Prestamo p : s.prestamos.obtenerTodosPrestamos())
            originalesP.put(p.getIdPrestamo(), p);
        Map<String, Reserva> originalesR = new HashMap<>();
        for (Reserva r : s.reservas.obtenerTodasReservas())
            originalesR.put(r.getIdReserva(), r);
        List<Prestamo> ps = new ArrayList<>();
        Map<String, Integer> conteos = new HashMap<>();
        for (var d : prestamos.values()) {
            Usuario u = Objects.requireNonNull(us.get(d.get(1)), "Usuario inexistente");
            Libro l = Objects.requireNonNull(ls.get(d.get(2)), "Libro inexistente");
            Prestamo x = new Prestamo((String) d.get(0), u, l, fecha(d.get(3)), fecha(d.get(4)), fecha(d.get(5)),
                    Prestamo.EstadoPrestamo.valueOf((String) d.get(6)));
            x.restaurarEjemplar((String) d.get(7));
            Prestamo previo = originalesP.get(x.getIdPrestamo());
            if (previo != null && previo.getLibro() == l && previo.getUsuario() == u) {
                previo.restaurarDatos(x);
                previo.restaurarEjemplar(x.getIdEjemplar());
                x = previo;
            }
            ps.add(x);
            if (x.getEstado() == Prestamo.EstadoPrestamo.ACTIVO) {
                l.buscarEjemplar(x.getIdEjemplar()).restaurarPrestado(true);
                conteos.merge(u.getId(), 1, Integer::sum);
            }
        }
        for (Usuario u : us.values())
            u.setPrestamosActuales(conteos.getOrDefault(u.getId(), 0));
        List<Reserva> rs = new ArrayList<>();
        for (var d : reservas.values())
            if (d.get(4).equals("PENDIENTE") || d.get(4).equals("NOTIFICADA")) {
                Reserva r = originalesR.get(d.get(0));
                if (r == null || r.getLibro() != ls.get(d.get(2)) || r.getUsuario() != us.get(d.get(1)))
                    r = new Reserva((String) d.get(0), Objects.requireNonNull(us.get(d.get(1))),
                            Objects.requireNonNull(ls.get(d.get(2))), fecha(d.get(3)),
                            Reserva.EstadoReserva.valueOf((String) d.get(4)));
                else
                    r.restaurarEstado(Reserva.EstadoReserva.valueOf((String) d.get(4)));
                rs.add(r);
            }
        s.libros.reponer(new ArrayList<>(ls.values()));
        s.usuarios.reponer(new ArrayList<>(us.values()));
        s.prestamos.reponer(ps);
        s.reservas.reponer(rs);
    }

    private static List<Object> fila(Object... x) {
        return new ArrayList<>(Arrays.asList(x));
    }

    public static EstadoSQL desde(SistemaBiblioteca s) {
        EstadoSQL e = new EstadoSQL();
        for (Libro l : s.libros.obtenerTodosLibros())
            e.libros.put(l.getId(), fila(l.getId(), l.getTitulo(), l.getAutor(), l.getIsbn(), l.getAnioPublicacion(),
                    l.getCategoria()));
        for (Libro l : s.libros.obtenerTodosLibros())
            for (Ejemplar x : l.getEjemplares())
                e.ejemplares.put(x.getId(), fila(x.getId(), l.getId()));
        for (Usuario u : s.usuarios.obtenerTodosUsuarios())
            e.usuarios.put(u.getId(), fila(u.getId(), u.getNombre(), u.getEmail(), u.getLimitePrestamos(),
                    u.getMultaDecimal().setScale(2, java.math.RoundingMode.UNNECESSARY)));
        for (Prestamo p : s.prestamos.obtenerTodosPrestamos())
            e.prestamos.put(p.getIdPrestamo(), fila(p.getIdPrestamo(), p.getUsuario().getId(), p.getLibro().getId(),
                    java.sql.Date.valueOf(p.getFechaPrestamo()), java.sql.Date.valueOf(p.getFechaDevolucionEsperada()),
                    p.getFechaDevolucionReal() == null ? null : java.sql.Date.valueOf(p.getFechaDevolucionReal()),
                    p.getEstado().name(), p.getIdEjemplar()));
        for (Reserva r : s.reservas.obtenerTodasReservas())
            e.reservas.put(r.getIdReserva(), fila(r.getIdReserva(), r.getUsuario().getId(), r.getLibro().getId(),
                    java.sql.Date.valueOf(r.getFechaReserva()), r.getEstado().name()));
        return e;
    }

    public static void diferencias(Connection c, EstadoSQL antes, EstadoSQL despues, String tipo) throws SQLException {
        // Bajas de ejemplares antes de libros; altas después de crear sus libros.
        Map<String, List<Object>> conservados = new LinkedHashMap<>(antes.ejemplares);
        conservados.keySet().retainAll(despues.ejemplares.keySet());
        aplicar(c, "ejemplares", "id,libro_id", antes.ejemplares, conservados);
        aplicar(c, "libros", "id,titulo,autor,isbn,anio,categoria", antes.libros, despues.libros);
        aplicar(c, "ejemplares", "id,libro_id", conservados, despues.ejemplares);
        aplicar(c, "usuarios", "id,nombre,email,limite,multa", antes.usuarios, despues.usuarios);
        aplicar(c, "prestamos", "id,usuario_id,libro_id,fecha,esperada,fecha_real,estado,ejemplar_id", antes.prestamos,
                despues.prestamos);
        Map<String, List<Object>> rs = new LinkedHashMap<>();
        for (var ent : despues.reservas.entrySet()) {
            List<Object> d = new ArrayList<>(ent.getValue());
            List<Object> previo = antes.reservas.get(ent.getKey());
            if (d.size() == 6) {
            } else if (previo != null)
                d.add(previo.get(5));
            else {
                long orden;
                try (Statement q = c.createStatement();
                        ResultSet r = q
                                .executeQuery("SELECT siguiente FROM secuencias WHERE nombre='orden' FOR UPDATE")) {
                    r.next();
                    orden = r.getLong(1);
                }
                try (Statement q = c.createStatement()) {
                    q.executeUpdate("UPDATE secuencias SET siguiente=siguiente+1 WHERE nombre='orden'");
                }
                d.add(orden);
            }
            rs.put(ent.getKey(), d);
        }
        for (var ent : antes.reservas.entrySet())
            if (!rs.containsKey(ent.getKey())) {
                List<Object> d = new ArrayList<>(ent.getValue());
                if (d.get(4).equals("PENDIENTE") || d.get(4).equals("NOTIFICADA"))
                    d.set(4, tipo.equals("cancelarReserva") ? "CANCELADA" : "ATENDIDA");
                rs.put(ent.getKey(), d);
            }
        aplicar(c, "reservas", "id,usuario_id,libro_id,fecha,estado,orden", antes.reservas, rs);
    }

    private static void aplicar(Connection c, String tabla, String campos, Map<String, List<Object>> antes,
            Map<String, List<Object>> despues) throws SQLException {
        String[] columnas = campos.split(",");
        for (String id : antes.keySet())
            if (!despues.containsKey(id)) {
                if (!tabla.equals("libros") && !tabla.equals("usuarios") && !tabla.equals("ejemplares"))
                    throw new SQLException("No se permite borrar historial");
                ejecutar(c, "DELETE FROM " + tabla + " WHERE id=?", List.of(id));
            }
        for (var ent : despues.entrySet()) {
            var d = ent.getValue();
            var a = antes.get(ent.getKey());
            if (a == null)
                ejecutar(c, "INSERT INTO " + tabla + "(" + campos + ") VALUES("
                        + String.join(",", Collections.nCopies(d.size(), "?")) + ")", d);
            else if (!a.equals(d)) {
                List<String> asignaciones = new ArrayList<>();
                for (int i = 1; i < columnas.length; i++)
                    asignaciones.add(columnas[i] + "=?");
                List<Object> valores = new ArrayList<>(d.subList(1, d.size()));
                valores.add(ent.getKey());
                ejecutar(c, "UPDATE " + tabla + " SET " + String.join(",", asignaciones) + " WHERE id=?", valores);
            }
        }
    }

    public static void ejecutar(Connection c, String sql, List<?> valores) throws SQLException {
        try (PreparedStatement q = c.prepareStatement(sql)) {
            for (int i = 0; i < valores.size(); i++)
                q.setObject(i + 1, valores.get(i));
            q.executeUpdate();
        }
    }
}
