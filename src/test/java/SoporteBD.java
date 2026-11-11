import java.sql.*;
import java.util.*;
import modelo.*;
import seguridad.*;
import utilidades.SistemaBiblioteca;
public final class SoporteBD {
 public static Connection admin()throws SQLException{
  if(!infraestructura.ConfiguracionBD.esquema().equals("biblioteca_pruebas"))throw new IllegalStateException("Las pruebas sólo pueden modificar biblioteca_pruebas");
  return DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/biblioteca_pruebas?sslMode=DISABLED&allowPublicKeyRetrieval=true&characterEncoding=UTF-8",System.getenv("BIBLIOTECA_TEST_USER"),System.getenv("BIBLIOTECA_TEST_PASSWORD"));
 }
 public static void sql(String sql)throws Exception{try(Connection c=admin();Statement q=c.createStatement()){q.execute(sql);}}
 public static void reiniciar()throws Exception{
  try(Connection c=admin();Statement q=c.createStatement()){
   for(String t:List.of("operaciones","reservas","prestamos","usuarios","libros"))q.executeUpdate("DELETE FROM "+t);
   q.executeUpdate("UPDATE secuencias SET siguiente=1");q.executeUpdate("UPDATE administradores SET fallos=0,bloqueado_hasta=NULL,activo=1");
   try(ResultSet r=q.executeQuery("SELECT COUNT(*) FROM administradores")){
    r.next();if(r.getInt(1)==0){byte[] salt=new byte[16];new java.security.SecureRandom().nextBytes(salt);
     try(PreparedStatement a=c.prepareStatement("INSERT INTO administradores(usuario,nombre,hash,salt,iteraciones) VALUES('QA','Administrador QA',?,?,?)")){
      a.setBytes(1,Autenticacion.hash(System.getenv("BIBLIOTECA_ADMIN_PASSWORD").toCharArray(),salt,Autenticacion.ITERACIONES));a.setBytes(2,salt);a.setInt(3,Autenticacion.ITERACIONES);a.executeUpdate();
     }
    }
   }
  }
 }
 public static Sesion sesion()throws Exception{return new Autenticacion().ingresar("QA",System.getenv("BIBLIOTECA_ADMIN_PASSWORD").toCharArray());}
 public static SistemaBiblioteca sistema()throws Exception{return SistemaBiblioteca.cargar(sesion());}
 public static List<Libro> cargarLibros()throws Exception{return sistema().libros.obtenerTodosLibros();}
 public static List<Usuario> cargarUsuarios()throws Exception{return sistema().usuarios.obtenerTodosUsuarios();}
 public static List<Prestamo> cargarPrestamos()throws Exception{return sistema().prestamos.obtenerTodosPrestamos();}
 public static List<Reserva> cargarReservas()throws Exception{return sistema().reservas.obtenerTodasReservas();}
 public static int contar(String tabla)throws Exception{try(Connection c=admin();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT COUNT(*) FROM "+tabla)){r.next();return r.getInt(1);}}
}
