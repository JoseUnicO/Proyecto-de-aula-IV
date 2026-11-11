package utilidades;
import controlador.*;
import modelo.*;
import excepciones.*;
import java.util.*;
import java.io.*;
import java.sql.*;
import infraestructura.*;
import repositorio.EstadoSQL;
import seguridad.*;
/** Unidad de trabajo JDBC. Los árboles y colas son proyecciones de datos confirmados. */
public final class SistemaBiblioteca implements AutoCloseable {
 public final GestorLibros libros; public final GestorUsuarios usuarios;
 public final GestorPrestamos prestamos; public final GestorReservas reservas;
 private final infraestructura.Conexiones conexiones; private final Sesion sesion; private final ThreadLocal<Boolean> ejecutando=ThreadLocal.withInitial(()->false);
 private final ThreadLocal<UUID> solicitud=new ThreadLocal<>();
 private SistemaBiblioteca(Sesion sesion,infraestructura.Conexiones conexiones){
  this.conexiones=conexiones;
  this.sesion=sesion;libros=new GestorLibros();usuarios=new GestorUsuarios();prestamos=new GestorPrestamos(libros,usuarios);reservas=prestamos.getGestorReservas();
 }
 private void conectar(){libros.conectar(this);usuarios.conectar(this);prestamos.conectar(this);reservas.conectar(this);}
 public static SistemaBiblioteca cargar(Sesion sesion)throws IOException{return cargar(sesion,ConfiguracionBD::abrir);}
 public static SistemaBiblioteca cargar(Sesion sesion,infraestructura.Conexiones conexiones)throws IOException{
  SistemaBiblioteca s=new SistemaBiblioteca(sesion,conexiones);
  try(Connection c=conexiones.abrir()){ConfiguracionBD.verificar(c);Autenticacion.exigir(c,sesion);EstadoSQL.leer(c).cargarEn(s);s.validar();s.conectar();return s;}
  catch(Exception e){throw new IOException("No se pudo cargar MySQL. No se iniciará una biblioteca vacía: "+e.getMessage(),e);}
 }
 public static void validarDatos(EstadoSQL datos)throws Exception{SistemaBiblioteca s=new SistemaBiblioteca(null,ConfiguracionBD::abrir);datos.cargarEn(s);s.validar();}
 public synchronized void actualizar()throws IOException{
  sesion.exigir();try(Connection c=conexiones.abrir()){ConfiguracionBD.verificar(c);Autenticacion.exigir(c,sesion);EstadoSQL datos=EstadoSQL.leer(c);validarDatos(datos);datos.cargarEn(this);validar();}
  catch(Exception e){throw new IOException("No se pudo actualizar desde MySQL: "+e.getMessage(),e);}
 }
 public <T> T conSolicitud(UUID id,java.util.concurrent.Callable<T> accion)throws Exception{
  if(solicitud.get()!=null)throw new IllegalStateException("Solicitud anidada");solicitud.set(Objects.requireNonNull(id));try{return accion.call();}finally{solicitud.remove();}
 }
 @FunctionalInterface public interface Accion<T,E extends Exception>{T aplicar()throws E;}
 public static <T,E extends Exception>T ejecutar(Object gestor,String tipo,Accion<T,E> accion)throws E{
  SistemaBiblioteca s=switch(gestor){case GestorLibros x->x.sistema();case GestorUsuarios x->x.sistema();case GestorPrestamos x->x.sistema();case GestorReservas x->x.sistema();default->null;};
  if(s==null||s.ejecutando.get())return accion.aplicar();
  UUID identificador=s.solicitud.get();
  try{return gui.TareasBD.invocar(()->s.transaccion(tipo,identificador,accion));}
  catch(RuntimeException e){throw e;}catch(Exception e){@SuppressWarnings("unchecked") E x=(E)e;throw x;}
 }
 private synchronized <T,E extends Exception>T transaccion(String tipo,UUID identificador,Accion<T,E> accion)throws E{
  sesion.exigir();UUID id=Optional.ofNullable(identificador).orElseGet(UUID::randomUUID);estructuras.Pila<Estado> deshacer=new estructuras.Pila<>();boolean confirmado=false;
  try(Connection c=conexiones.abrir()){
   c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);c.setAutoCommit(false);
   try{
    ConfiguracionBD.verificar(c);Autenticacion.exigir(c,sesion);
    // Orden único de bloqueos, compartido por todos los procesos. Evita escrituras sobre proyecciones obsoletas.
    try(Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT nombre FROM secuencias ORDER BY nombre FOR UPDATE")){while(r.next()){} }
    try(PreparedStatement q=c.prepareStatement("SELECT tipo,entidad,administrador_id FROM operaciones WHERE id=?")){
     q.setString(1,id.toString());try(ResultSet r=q.executeQuery()){if(r.next()){
      if(!r.getString(1).equals(tipo)||r.getLong(3)!=sesion.administrador().id())throw new IllegalArgumentException("UUID utilizado por otra operación");
      EstadoSQL.leer(c).cargarEn(this);c.rollback();return resultado(tipo,r.getString(2));
     }}
    }
    EstadoSQL antes=EstadoSQL.leer(c);validarDatos(antes);antes.cargarEn(this);validar();deshacer.apilar(new Estado());
    ejecutando.set(true);Identificadores.TRANSACCION.set(c);
    T valor=accion.aplicar();validar();
    EstadoSQL.diferencias(c,antes,EstadoSQL.desde(this),tipo);
    String entidad=valor instanceof Prestamo x?x.getIdPrestamo():valor instanceof Reserva x?x.getIdReserva():null;
    try(PreparedStatement q=c.prepareStatement("INSERT INTO operaciones(id,administrador_id,tipo,entidad) VALUES(?,?,?,?)")){
     q.setString(1,id.toString());q.setLong(2,sesion.administrador().id());q.setString(3,tipo);q.setString(4,entidad);q.executeUpdate();
    }
    c.commit();confirmado=true;return valor;
   }catch(Exception fallo){
    try{c.rollback();}catch(SQLException ignored){}
    // Si se perdió la conexión durante COMMIT, comprobar el UUID con otra conexión antes de afirmar que falló.
    if(fallo instanceof SQLException sql && sql.getSQLState()!=null && sql.getSQLState().startsWith("08")){
     try(Connection v=conexiones.abrir();PreparedStatement q=v.prepareStatement("SELECT entidad FROM operaciones WHERE id=?")){
      q.setString(1,id.toString());try(ResultSet r=q.executeQuery()){if(r.next()){confirmado=true;EstadoSQL.leer(v).cargarEn(this);return resultado(tipo,r.getString(1));}}
     }catch(Exception desconocido){close();throw new IllegalStateException("Conexión perdida. Resultado pendiente de comprobar. Solicitud "+id+". Vuelva a iniciar sesión antes de continuar.",desconocido);}
    }
    if(!confirmado&&!deshacer.estaVacia())try{deshacer.desapilar().restaurar();}catch(EstructuraVaciaException imposible){throw new IllegalStateException(imposible);}
    if(fallo instanceof SQLException)throw new IllegalStateException("MySQL rechazó la operación; no se aplicaron cambios. "+mensajeSQL((SQLException)fallo),fallo);
    if(fallo instanceof IOException)throw new IllegalStateException(fallo.getMessage(),fallo);
    @SuppressWarnings("unchecked") E e=(E)fallo;throw e;
   }finally{ejecutando.remove();Identificadores.TRANSACCION.remove();}
  }catch(SQLException e){throw new IllegalStateException("MySQL no está disponible. La operación no se confirmó.",e);}
 }
 private String mensajeSQL(SQLException e){return e.getErrorCode()==1451?"Hay préstamos o reservas históricos relacionados.":e.getErrorCode()==1062?"El registro ya existe o el libro ya está prestado.":"Revise el tamaño y formato de los datos (código "+e.getErrorCode()+").";}
 @SuppressWarnings("unchecked") private <T>T resultado(String tipo,String entidad){
  if(entidad==null)return null;
  if(tipo.equals("registrarPrestamo"))return (T)prestamos.obtenerTodosPrestamos().stream().filter(x->x.getIdPrestamo().equals(entidad)).findFirst().orElseThrow();
  return (T)reservas.obtenerTodasReservas().stream().filter(x->x.getIdReserva().equals(entidad)).findFirst().orElse(null);
 }
 public synchronized void close(){sesion.close();libros.reponer(List.of());usuarios.reponer(List.of());prestamos.reponer(List.of());reservas.reponer(List.of());}
    public void validar()throws IOException{
        Map<String,Integer> cont=new HashMap<>();Set<String> ocupados=new HashSet<>(),ids=new HashSet<>();
        for(Prestamo p:prestamos.obtenerTodosPrestamos()){
            if(!ids.add(p.getIdPrestamo()))throw new IOException("prestamos: ID duplicado "+p.getIdPrestamo());
            try{if(libros.buscarLibro(p.getLibro().getId())!=p.getLibro()||usuarios.buscarUsuario(p.getUsuario().getId())!=p.getUsuario())throw new IOException("Referencias distintas: "+p.getIdPrestamo());}catch(BibliotecaException e){throw new IOException("prestamos: "+p.getIdPrestamo()+": "+e.getMessage(),e);}
            if(p.getFechaPrestamo()==null||p.getFechaDevolucionEsperada()==null||p.getFechaDevolucionEsperada().isBefore(p.getFechaPrestamo()))throw new IOException("Fecha inválida: "+p.getIdPrestamo());
            if(p.getEstado()==Prestamo.EstadoPrestamo.ACTIVO||p.getEstado()==Prestamo.EstadoPrestamo.VENCIDO){if(p.getFechaDevolucionReal()!=null||!ocupados.add(p.getLibro().getId()))throw new IOException("Préstamo activo inconsistente: "+p.getIdPrestamo());cont.merge(p.getUsuario().getId(),1,Integer::sum);}
            else if(p.getFechaDevolucionReal()==null||p.getFechaDevolucionReal().isBefore(p.getFechaPrestamo()))throw new IOException("Fecha real inválida: "+p.getIdPrestamo());
        }
        for(Libro l:libros.obtenerTodosLibros())if(l.isDisponible()==ocupados.contains(l.getId()))throw new IOException("libros: disponibilidad inconsistente "+l.getId());
        for(Usuario u:usuarios.obtenerTodosUsuarios())if(u.getPrestamosActuales()!=cont.getOrDefault(u.getId(),0))throw new IOException("usuarios: contador inconsistente "+u.getId());
        ids.clear();Set<String> pares=new HashSet<>();
        for(Reserva r:reservas.obtenerTodasReservas())try{
            if(!ids.add(r.getIdReserva())||!pares.add(r.getUsuario().getId()+"\0"+r.getLibro().getId()))throw new IOException("reservas: reserva duplicada "+r.getIdReserva());
            if(libros.buscarLibro(r.getLibro().getId())!=r.getLibro()||usuarios.buscarUsuario(r.getUsuario().getId())!=r.getUsuario())throw new IOException("reservas: referencias inconsistentes "+r.getIdReserva());
        }catch(BibliotecaException e){throw new IOException("reservas: "+r.getIdReserva()+": "+e.getMessage(),e);}
    }
    private final class Estado {
        final List<Libro> ls=libros.obtenerTodosLibros();final List<Usuario> us=usuarios.obtenerTodosUsuarios();
        final List<Prestamo> ps=prestamos.obtenerTodosPrestamos();final List<Reserva> rs=reservas.obtenerTodasReservas();
        final Map<Libro,Libro> lc=new HashMap<>();final Map<Usuario,Usuario> uc=new HashMap<>();
        final Map<Prestamo,Prestamo> pc=new HashMap<>();final Map<Reserva,Reserva.EstadoReserva> rc=new HashMap<>();
        Estado(){try{for(Libro l:ls){Libro c=new Libro(l.getId(),l.getTitulo(),l.getAutor(),l.getIsbn(),l.getAnioPublicacion(),l.getCategoria());c.setDisponible(l.isDisponible());lc.put(l,c);}for(Usuario u:us){Usuario c=new Usuario(u.getId(),u.getNombre(),u.getEmail(),u.getLimitePrestamos());c.setPrestamosActuales(u.getPrestamosActuales());c.restaurarMulta(u.getMultaDecimal());uc.put(u,c);}for(Prestamo p:ps)pc.put(p,new Prestamo(p.getIdPrestamo(),p.getUsuario(),p.getLibro(),p.getFechaPrestamo(),p.getFechaDevolucionEsperada(),p.getFechaDevolucionReal(),p.getEstado()));for(Reserva r:rs)rc.put(r,r.getEstado());}catch(BibliotecaException e){throw new IllegalStateException(e);}}
        void restaurar(){try{for(Libro l:ls){l.actualizar(lc.get(l));l.setDisponible(lc.get(l).isDisponible());}for(Usuario u:us){u.setPrestamosActuales(0);u.actualizar(uc.get(u));u.setPrestamosActuales(uc.get(u).getPrestamosActuales());u.restaurarMulta(uc.get(u).getMultaDecimal());}for(Prestamo p:ps){Prestamo c=pc.get(p);p.setEstado(c.getEstado());p.setFechaDevolucionEsperada(c.getFechaDevolucionEsperada());p.restaurarFechaReal(c.getFechaDevolucionReal());}for(Reserva r:rs)r.restaurarEstado(rc.get(r));libros.reponer(ls);usuarios.reponer(us);prestamos.reponer(ps);reservas.reponer(rs);}catch(BibliotecaException e){throw new IllegalStateException("No se pudo revertir",e);}}
    }
}
