package infraestructura;

import java.sql.*;

/** La secuencia se consume dentro de la misma transacción de negocio. */
public final class Identificadores {
    public static final ThreadLocal<Connection> TRANSACCION = new ThreadLocal<>();

    public static String nuevo(String nombre, String prefijo) {
        Connection c = TRANSACCION.get();
        if (c == null)
            return prefijo + java.util.UUID.randomUUID();
        try (PreparedStatement q = c.prepareStatement("SELECT siguiente FROM secuencias WHERE nombre=? FOR UPDATE")) {
            q.setString(1, nombre);
            long n;
            try (ResultSet r = q.executeQuery()) {
                if (!r.next())
                    throw new SQLException("Secuencia ausente");
                n = r.getLong(1);
            }
            try (PreparedStatement u = c
                    .prepareStatement("UPDATE secuencias SET siguiente=siguiente+1 WHERE nombre=?")) {
                u.setString(1, nombre);
                u.executeUpdate();
            }
            return prefijo + String.format("%05d", n);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo obtener el identificador", e);
        }
    }
}
