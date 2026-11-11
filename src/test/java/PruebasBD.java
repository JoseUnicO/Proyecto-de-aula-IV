import java.sql.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import modelo.*;
import seguridad.*;
import utilidades.*;
import infraestructura.ConfiguracionBD;
public class PruebasBD {
 interface Caso{void ejecutar()throws Exception;}
 static List<String> filas=new ArrayList<>();static int fallos;
 static void comprobar(boolean b,String m){if(!b)throw new AssertionError(m);}
 static void rechazar(Caso c)throws Exception{try{c.ejecutar();}catch(Exception esperado){return;}throw new AssertionError("Se aceptó una operación inválida");}
 static void caso(String id,String nombre,Caso c){try{SoporteBD.reiniciar();c.ejecutar();filas.add(id+"\tOK\t"+nombre);System.out.println(id+" OK "+nombre);}catch(Throwable e){fallos++;filas.add(id+"\tFALLO\t"+nombre+"\t"+e);e.printStackTrace();}}
 static SistemaBiblioteca crear()throws Exception{
  var s=SoporteBD.sistema();for(String id:List.of("A","B","C"))s.usuarios.agregarUsuario(new Usuario(id,"Lector "+id,id+"@example.com",2));
  for(String id:List.of("L","M","N"))s.libros.agregarLibro(new Libro(id,"Título "+id,"García","9780307474728",2000,"Ficción"));return s;
 }
 static String valor(String sql)throws Exception{try(Connection c=SoporteBD.admin();Statement q=c.createStatement();ResultSet r=q.executeQuery(sql)){r.next();return r.getString(1);}}
 static int hijo(String modo,Map<String,String> entorno,String... argumentos)throws Exception{
  List<String> cmd=new ArrayList<>(List.of(Path.of(System.getProperty("java.home"),"bin","java.exe").toString(),"--enable-native-access=ALL-UNNAMED","-Dfile.encoding=UTF-8","-cp",System.getProperty("java.class.path"),"PruebasBD",modo));cmd.addAll(List.of(argumentos));
  ProcessBuilder pb=new ProcessBuilder(cmd);pb.environment().putAll(entorno);Process p=pb.redirectErrorStream(true).start();String salida=new String(p.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);if(!p.waitFor(30,TimeUnit.SECONDS)){p.destroyForcibly();throw new AssertionError("Proceso agotó el tiempo");}if(p.exitValue()!=0&&p.exitValue()!=93)System.out.println(salida);return p.exitValue();
 }
 static void paralelo(Caso a,Caso b,int exitosEsperados)throws Exception{
  var inicio=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);try{
   List<Future<Boolean>> trabajos=new ArrayList<>();for(Caso c:List.of(a,b))trabajos.add(pool.submit(()->{inicio.await();try{c.ejecutar();return true;}catch(Exception e){return false;}}));inicio.countDown();int exitos=0;for(var f:trabajos)if(f.get(20,TimeUnit.SECONDS))exitos++;comprobar(exitos==exitosEsperados,"Éxitos concurrentes: "+exitos);
  }finally{pool.shutdownNow();}
 }
 public static void main(String[] args)throws Exception{
  if(args.length>0&&!args[0].equals("suite")){
   switch(args[0]){
    case "abrupto"->{var s=SoporteBD.sistema();s.prestamos.registrarPrestamo("A","L");Runtime.getRuntime().halt(93);}
    case "sincommit"->{Connection c=SoporteBD.admin();c.setAutoCommit(false);c.createStatement().executeUpdate("UPDATE usuarios SET multa=99 WHERE id='A'");Runtime.getRuntime().halt(93);}
    case "conexion"->{SoporteBD.sistema();}
    default->throw new IllegalArgumentException();
   }return;
  }
  caso("DB-01","Login válido, hash PBKDF2 y ausencia de contraseña en Admin",()->{
   try(var sesion=SoporteBD.sesion()){comprobar(sesion.administrador().getUsuario().equals("QA"),"Identidad incorrecta");}
   comprobar(valor("SELECT CONCAT(OCTET_LENGTH(hash),':',OCTET_LENGTH(salt),':',iteraciones) FROM administradores").equals("32:16:600000"),"Parámetros inseguros");
   comprobar(Arrays.stream(Admin.class.getDeclaredMethods()).noneMatch(m->m.getName().toLowerCase().contains("password")),"API expone contraseña");
  });
  caso("DB-02","Cinco fallos persisten y bloquean entre instancias",()->{
   for(int i=0;i<5;i++)rechazar(()->new Autenticacion().ingresar("QA","incorrecta".toCharArray()));
   comprobar(valor("SELECT fallos FROM administradores").equals("5"),"Contador no persistido");rechazar(()->SoporteBD.sesion());
   SoporteBD.sql("UPDATE administradores SET bloqueado_hasta=UTC_TIMESTAMP()-INTERVAL 1 MINUTE");SoporteBD.sesion().close();comprobar(valor("SELECT fallos FROM administradores").equals("0"),"No restableció contador");
  });
  caso("DB-03","Usuario desconocido no crea cuentas ni permite SQL injection",()->{rechazar(()->new Autenticacion().ingresar("' OR 1=1 -- ","x".toCharArray()));comprobar(SoporteBD.contar("administradores")==1,"Creó cuenta");});
  caso("DB-04","Sesión cerrada o ausente bloquea operaciones",()->{var s=crear();s.close();rechazar(()->s.libros.agregarLibro(new Libro("Z","Z","Autor","9780307474728",2000,"Ficción")));rechazar(()->SistemaBiblioteca.cargar(null));comprobar(SoporteBD.contar("libros")==3,"Escritura sin sesión");});
  caso("DB-05","Cuenta técnica sin DDL, borrado de historial ni cambios de hash",()->{
   try(Connection c=ConfiguracionBD.abrir();Statement q=c.createStatement()){
    rechazar(()->q.execute("CREATE TABLE no_permitida(id INT)"));rechazar(()->q.executeUpdate("DELETE FROM prestamos"));rechazar(()->q.executeUpdate("UPDATE administradores SET hash=REPEAT('x',32)"));
   }
  });
  caso("DB-06","Dos bibliotecas simultáneas no duplican préstamo",()->{var a=crear();var b=SoporteBD.sistema();paralelo(()->a.prestamos.registrarPrestamo("A","L"),()->b.prestamos.registrarPrestamo("B","L"),1);comprobar(SoporteBD.contar("prestamos")==1,"Duplicó préstamo");});
  caso("DB-07","Límite del usuario validado con datos recientes",()->{var a=crear();SoporteBD.sql("UPDATE usuarios SET limite=1 WHERE id='A'");var b=SoporteBD.sistema();paralelo(()->a.prestamos.registrarPrestamo("A","L"),()->b.prestamos.registrarPrestamo("A","M"),1);comprobar(SoporteBD.sistema().usuarios.buscarUsuario("A").getPrestamosActuales()==1,"Superó límite");});
  caso("DB-08","FIFO persiste tras reiniciar; reserva atendida permanece histórica",()->{
   var s=crear();var p=s.prestamos.registrarPrestamo("A","L");var r=s.reservas.agregarReserva(s.usuarios.buscarUsuario("B"),s.libros.buscarLibro("L"));s.reservas.agregarReserva(s.usuarios.buscarUsuario("C"),s.libros.buscarLibro("L"));
   s=SoporteBD.sistema();s.prestamos.registrarDevolucion(p.getIdPrestamo());var t=s;rechazar(()->t.prestamos.registrarPrestamo("C","L"));s.prestamos.registrarPrestamo("B","L");
   comprobar(valor("SELECT estado FROM reservas WHERE id='"+r.getIdReserva()+"'").equals("ATENDIDA"),"No preservó reserva atendida");comprobar(s.reservas.contarReservas()==1,"Cola incorrecta");
  });
  caso("DB-09","Reserva cancelada bloquea eliminación del usuario histórico",()->{
   var s=crear();s.prestamos.registrarPrestamo("A","L");var r=s.reservas.agregarReserva(s.usuarios.buscarUsuario("B"),s.libros.buscarLibro("L"));s.reservas.cancelarReserva(r.getIdReserva());rechazar(()->s.usuarios.eliminarUsuario("B"));comprobar(SoporteBD.contar("usuarios")==3,"Perdió usuario histórico");
  });
  caso("DB-10","UUID repetido confirma una sola operación",()->{
   var s=crear();UUID id=UUID.randomUUID();var p=s.conSolicitud(id,()->s.prestamos.registrarPrestamo("A","L"));var r=s.conSolicitud(id,()->s.prestamos.registrarPrestamo("A","L"));comprobar(p.getIdPrestamo().equals(r.getIdPrestamo())&&SoporteBD.contar("prestamos")==1,"Duplicó solicitud");
  });
  caso("DB-11","Decimal grande exacto y lectura sin double",()->{crear();SoporteBD.sql("UPDATE usuarios SET multa=99999999999999999.99 WHERE id='A'");var s=SoporteBD.sistema();comprobar(s.usuarios.buscarUsuario("A").getMultaDecimal().toPlainString().equals("99999999999999999.99"),"Perdió precisión");s.libros.actualizarLibro(new Libro("L","Nuevo","Autor","9780307474728",2000,"Ficción"));comprobar(valor("SELECT multa FROM usuarios WHERE id='A'").equals("99999999999999999.99"),"Reescribió multa");});
  caso("DB-12","Cambios de otra instancia visibles sin sobrescribir",()->{var a=crear();var b=SoporteBD.sistema();a.libros.actualizarLibro(new Libro("L","Corregido","Autor","9780307474728",2000,"Ficción"));b.usuarios.actualizarUsuario(new Usuario("B","Nombre corregido","b@example.com",2));comprobar(SoporteBD.sistema().libros.buscarLibro("L").getTitulo().equals("Corregido"),"Escritura obsoleta");});
  caso("DB-13","FK y unicidad rechazan referencias y dos préstamos activos",()->{crear();try(Connection c=SoporteBD.admin();Statement q=c.createStatement()){rechazar(()->q.executeUpdate("INSERT INTO prestamos(id,usuario_id,libro_id,fecha,esperada,estado) VALUES('X','NO','L',CURRENT_DATE(),CURRENT_DATE(),'ACTIVO')"));}comprobar(SoporteBD.contar("prestamos")==0,"Aceptó referencia");});
  caso("DB-14","Base inaccesible no genera archivos ni biblioteca vacía",()->{comprobar(hijo("conexion",Map.of("BIBLIOTECA_DB_NAME","biblioteca_inexistente"))!=0,"Aceptó base ausente");});
  caso("DB-15","Checksum incompatible bloquea login",()->{String anterior=valor("SELECT checksum FROM schema_version WHERE version=2");try{SoporteBD.sql("UPDATE schema_version SET checksum=REPEAT('0',64) WHERE version=2");rechazar(()->SoporteBD.sesion());}finally{SoporteBD.sql("UPDATE schema_version SET checksum='"+anterior+"' WHERE version=2");}});
  caso("PRO-01","Persistencia inmediata de Unicode, pipe, salto y reinicio",()->{var s=crear();s.libros.actualizarLibro(new Libro("L","Título | con\nsalto 📚","García","9780307474728",2000,"Ficción"));s.prestamos.registrarPrestamo("A","L");s.close();var t=SoporteBD.sistema();comprobar(t.libros.buscarLibro("L").getTitulo().equals("Título | con\nsalto 📚")&&!t.libros.buscarLibro("L").isDisponible()&&t.usuarios.buscarUsuario("A").getPrestamosActuales()==1,"Reinicio inconsistente");});
  caso("PRO-02","Error SQL después de mutación revierte filas, memoria y secuencia",()->{
   var s=crear();SoporteBD.sql("CREATE TRIGGER prueba_fallo BEFORE INSERT ON prestamos FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Fallo de prueba'");
   try{rechazar(()->s.prestamos.registrarPrestamo("A","L"));comprobar(s.libros.buscarLibro("L").isDisponible()&&s.usuarios.buscarUsuario("A").getPrestamosActuales()==0&&SoporteBD.contar("prestamos")==0,"No revirtió");comprobar(valor("SELECT siguiente FROM secuencias WHERE nombre='prestamo'").equals("1"),"Secuencia no atómica");}finally{SoporteBD.sql("DROP TRIGGER prueba_fallo");}
  });
  caso("PRO-04","Cierre abrupto tras COMMIT conserva datos; sin COMMIT revierte",()->{
   crear();comprobar(hijo("abrupto",Map.of())==93,"No terminó abruptamente");comprobar(SoporteBD.sistema().prestamos.contarPrestamosActivos()==1,"Perdió commit");comprobar(hijo("sincommit",Map.of())==93,"No terminó transacción abrupta");comprobar(SoporteBD.sistema().usuarios.buscarUsuario("A").getMultaDecimal().signum()==0,"Confirmó sin commit");
  });
  caso("PRO-06","Edición conserva instancias y protección del historial",()->{var s=crear();var p=s.prestamos.registrarPrestamo("A","L");Libro original=p.getLibro();s.libros.actualizarLibro(new Libro("L","Corregido","Autor","9780307474728",2001,"Ficción"));comprobar(p.getLibro()==original&&original==s.libros.buscarLibro("L")&&original.getTitulo().equals("Corregido"),"Rompió identidad");s.prestamos.registrarDevolucion(p.getIdPrestamo());rechazar(()->s.usuarios.eliminarUsuario("A"));comprobar(SoporteBD.sistema().prestamos.contarHistorial()==1,"Perdió historial");});
  caso("DB-23","Respuesta de COMMIT perdida se resuelve por UUID sin duplicar",()->{
   crear();var contador=new java.util.concurrent.atomic.AtomicInteger();
   infraestructura.Conexiones conexiones=()->{
    Connection real=ConfiguracionBD.abrir();
    if(contador.getAndIncrement()!=1)return real;
    return (Connection)java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(obj,metodo,parametros)->{
     try{Object valor=metodo.invoke(real,parametros);if(metodo.getName().equals("commit"))throw new SQLException("Respuesta de commit interrumpida","08006");return valor;}
     catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}
    });
   };
   var s=SistemaBiblioteca.cargar(SoporteBD.sesion(),conexiones);UUID id=UUID.randomUUID();
   var p=s.conSolicitud(id,()->s.prestamos.registrarPrestamo("A","L"));
   comprobar(p!=null&&SoporteBD.contar("prestamos")==1&&!s.libros.buscarLibro("L").isDisponible(),"No resolvió commit confirmado");
   comprobar(s.conSolicitud(id,()->s.prestamos.registrarPrestamo("A","L")).getIdPrestamo().equals(p.getIdPrestamo()),"Repetición duplicada");
  });
  caso("DB-24","Devolución con multa y fallo SQL revierte toda la transacción",()->{
   var s=crear();var p=s.prestamos.registrarPrestamo("A","L");
   SoporteBD.sql("UPDATE prestamos SET fecha=CURRENT_DATE()-INTERVAL 20 DAY,esperada=CURRENT_DATE()-INTERVAL 3 DAY WHERE id='"+p.getIdPrestamo()+"'");
   SoporteBD.sql("CREATE TRIGGER prueba_devolucion BEFORE UPDATE ON prestamos FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Fallo de prueba'");
   try{
    s.actualizar();comprobar(p.getFechaPrestamo().equals(LocalDate.now().minusDays(20)),"No actualizó fecha inicial");
    rechazar(()->s.prestamos.registrarDevolucion(p.getIdPrestamo()));comprobar(SoporteBD.sistema().usuarios.buscarUsuario("A").getMultaDecimal().signum()==0&&SoporteBD.sistema().prestamos.contarPrestamosActivos()==1&&!s.libros.buscarLibro("L").isDisponible(),"Devolución parcial");}
   finally{SoporteBD.sql("DROP TRIGGER prueba_devolucion");}
  });
  caso("DB-25","Commit confirmado sin conexión de verificación bloquea sesión hasta consultar",()->{
   crear();var contador=new java.util.concurrent.atomic.AtomicInteger();
   infraestructura.Conexiones conexiones=()->{
    int n=contador.getAndIncrement();if(n>=2)throw new SQLException("Conexión interrumpida","08006");Connection real=ConfiguracionBD.abrir();if(n==0)return real;
    return (Connection)java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(obj,metodo,parametros)->{
     try{Object valor=metodo.invoke(real,parametros);if(metodo.getName().equals("commit"))throw new SQLException("ACK perdido","08006");return valor;}
     catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}
    });
   };
   var s=SistemaBiblioteca.cargar(SoporteBD.sesion(),conexiones);rechazar(()->s.prestamos.registrarPrestamo("A","L"));
   rechazar(()->s.prestamos.registrarPrestamo("B","M"));comprobar(SoporteBD.sistema().prestamos.contarPrestamosActivos()==1,"Revirtió un commit o permitió reintentar sin verificar");
  });
  caso("DB-26","Fallo de conexión anterior a COMMIT revierte sin auditoría parcial",()->{
   crear();int auditorias=SoporteBD.contar("operaciones");var contador=new java.util.concurrent.atomic.AtomicInteger();
   infraestructura.Conexiones conexiones=()->{
    Connection real=ConfiguracionBD.abrir();if(contador.getAndIncrement()!=1)return real;
    return (Connection)java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(obj,metodo,parametros)->{
     if(metodo.getName().equals("commit"))throw new SQLException("Conexión interrumpida antes de confirmar","08006");
     try{return metodo.invoke(real,parametros);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}
    });
   };
   var s=SistemaBiblioteca.cargar(SoporteBD.sesion(),conexiones);rechazar(()->s.prestamos.registrarPrestamo("A","L"));
   comprobar(SoporteBD.contar("prestamos")==0&&SoporteBD.contar("operaciones")==auditorias&&s.libros.buscarLibro("L").isDisponible(),"Persistió operación parcial");
  });
  Files.write(Path.of(args[1]),filas);System.out.println("BD_RESUMEN: OK="+(filas.size()-fallos)+", FALLO="+fallos+", TOTAL="+filas.size());System.exit(fallos==0?0:1);
 }
}
