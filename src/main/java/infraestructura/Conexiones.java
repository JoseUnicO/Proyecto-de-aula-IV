package infraestructura;
import java.sql.*;
/** Punto de apertura JDBC inyectable para verificar fallos de red sin cambiar los datos. */
@FunctionalInterface public interface Conexiones {Connection abrir()throws SQLException;}
