import controlador.*;
import estructuras.*;
import excepciones.*;
import modelo.*;
import utilidades.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.io.*;

public class PruebasNucleo {
    interface Checked { String run() throws Exception; }
    static int ok,fail,error;
    static List<String> rows=new ArrayList<>();
    static void expect(boolean condition,String detail) { if(!condition) throw new AssertionError(detail); }
    static void reject(Checked a) throws Exception { try {a.run();} catch(BibliotecaException e){return;} throw new AssertionError("Operacion aceptada; se esperaba BibliotecaException"); }
    static void test(String id,String title,Checked action) {
        String status,detail;
        try {detail=action.run();status="OK";ok++;}
        catch(AssertionError e){status="FALLO";detail=e.getMessage();fail++;}
        catch(Throwable e){status="ERROR";detail=e.getClass().getSimpleName()+": "+e.getMessage();error++;}
        rows.add(String.join("\t",id,status,title,detail.replace('\n',' ').replace('\t',' ')));
        System.out.println(id+" | "+status+" | "+title+" | "+detail);
    }
    static Libro book(String id) throws Exception {return new Libro(id,"Titulo "+id,"Autor","9780307474728",2000,"Ficcion");}
    static Usuario user(String id) throws Exception{return new Usuario(id,"Lector "+id,id+"@example.com",3);}
    static class F {
        GestorLibros gl=new GestorLibros();GestorUsuarios gu=new GestorUsuarios();GestorPrestamos gp=new GestorPrestamos(gl,gu);GestorReservas gr=gp.getGestorReservas();
        Usuario a=user("A"),b=user("B"),c=user("C");Libro l=book("L");
        F() throws Exception {gu.agregarUsuario(a);gu.agregarUsuario(b);gu.agregarUsuario(c);gl.agregarLibro(l);}
        Prestamo loan() throws Exception{return gp.registrarPrestamo("A","L");}
    }
    public static void main(String[] args) throws Exception {
        test("NUC-01","Prestamo normal y plazo de 14 dias",()->{F f=new F();Prestamo p=f.loan();expect(!f.l.isDisponible()&&f.a.getPrestamosActuales()==1&&f.gp.contarPrestamosActivos()==1&&p.getFechaDevolucionEsperada().equals(LocalDate.now().plusDays(14)),"Estado o plazo incorrecto");return "Estados y plazo correctos";});
        test("NUC-02","No prestar un libro ocupado",()->{F f=new F();f.loan();reject(()->{f.gp.registrarPrestamo("B","L");return "";});return "Rechazado";});
        test("NUC-03","Limite de prestamos",()->{F f=new F();f.a.setPrestamosActuales(3);reject(()->{f.loan();return "";});return "Rechazado";});
        test("NUC-04","Multa bloquea prestamo",()->{F f=new F();f.a.agregarMulta(2);reject(()->{f.loan();return "";});return "Rechazado";});
        test("NUC-05","Dias no positivos",()->{F f=new F();reject(()->{f.gp.registrarPrestamo("A","L",0);return "";});return "Rechazado";});
        test("NUC-06","IDs inexistentes",()->{F f=new F();reject(()->{f.gp.registrarPrestamo("NO","L");return "";});return "Rechazado";});
        test("NUC-07","No duplicar libro",()->{F f=new F();reject(()->{f.gl.agregarLibro(book("L"));return "";});return "Rechazado";});
        test("NUC-08","No duplicar usuario",()->{F f=new F();reject(()->{f.gu.agregarUsuario(user("A"));return "";});return "Rechazado";});
        test("NUC-09","Devolucion y doble devolucion",()->{F f=new F();Prestamo p=f.loan();f.gp.registrarDevolucion(p.getIdPrestamo());reject(()->{f.gp.registrarDevolucion(p.getIdPrestamo());return "";});expect(f.l.isDisponible()&&f.a.getPrestamosActuales()==0&&f.gp.contarHistorial()==1,"Estados incorrectos");return "Correcto; segundo intento rechazado";});
        test("NUC-10","Multa de tres dias de retraso",()->{F f=new F();Prestamo p=new Prestamo("PREST-10000",f.a,f.l,LocalDate.now().minusDays(20),LocalDate.now().minusDays(3),null,Prestamo.EstadoPrestamo.ACTIVO);f.l.prestar();f.a.agregarPrestamo();f.gp.restaurarPrestamo(p);f.gp.registrarDevolucion(p.getIdPrestamo());expect(f.a.getMultaAcumulada()==6&&p.getDiasRetraso()==3,"Multa incorrecta");return "Multa=6";});
        test("NUC-11","Renovacion vencida",()->{F f=new F();Prestamo p=new Prestamo("PREST-10001",f.a,f.l,LocalDate.now().minusDays(20),LocalDate.now().minusDays(3),null,Prestamo.EstadoPrestamo.ACTIVO);f.gp.restaurarPrestamo(p);reject(()->{f.gp.renovarPrestamo(p.getIdPrestamo());return "";});return "Rechazado";});
        test("NUC-12","No borrar libro prestado",()->{F f=new F();f.loan();reject(()->{f.gl.eliminarLibro("L");return "";});return "Rechazado";});
        test("NUC-13","No borrar lector con prestamo",()->{F f=new F();f.loan();reject(()->{f.gu.eliminarUsuario("A");return "";});return "Rechazado";});
        test("NUC-14","No reservar disponible",()->{F f=new F();reject(()->{f.gr.agregarReserva(f.b,f.l);return "";});return "Rechazado";});
        test("NUC-15","No duplicar reserva",()->{F f=new F();f.loan();f.gr.agregarReserva(f.b,f.l);reject(()->{f.gr.agregarReserva(f.b,f.l);return "";});return "Rechazado";});
        test("NUC-16","Devolver notifica y prestar consume reserva",()->{F f=new F();Prestamo p=f.loan();Reserva r=f.gr.agregarReserva(f.b,f.l);f.gp.registrarDevolucion(p.getIdPrestamo());expect(r.getEstado()==Reserva.EstadoReserva.NOTIFICADA,"Sin notificacion");f.gp.registrarPrestamo("B","L");expect(f.gr.contarReservas()==0,"No consumida");return "Correcto";});
        test("NUC-17","Prioridad de reserva frente a otro lector",()->{F f=new F();Prestamo p=f.loan();f.gr.agregarReserva(f.b,f.l);f.gp.registrarDevolucion(p.getIdPrestamo());reject(()->{f.gp.registrarPrestamo("C","L");return "";});return "Protegida";});
        test("NUC-18","Cancelar prestamo notifica la reserva",()->{F f=new F();Prestamo p=f.loan();Reserva r=f.gr.agregarReserva(f.b,f.l);f.gp.cancelarPrestamo(p.getIdPrestamo());expect(r.getEstado()==Reserva.EstadoReserva.NOTIFICADA,"Reserva queda "+r.getEstado()+" con libro disponible="+f.l.isDisponible());return "Notificada";});
        test("NUC-19","Cancelar primera reserva disponible avanza turno",()->{F f=new F();Prestamo p=f.loan();Reserva r=f.gr.agregarReserva(f.b,f.l);Reserva s=f.gr.agregarReserva(f.c,f.l);f.gp.registrarDevolucion(p.getIdPrestamo());f.gr.cancelarReserva(r.getIdReserva());expect(s.getEstado()==Reserva.EstadoReserva.NOTIFICADA,"Segunda reserva queda "+s.getEstado());return "Turno actualizado";});
        test("NUC-20","Libro publicado en 2026",()->{try{new Libro("NUEVO","Titulo","Autor","9780307474728",2026,"Categoria");}catch(OperacionInvalidaException e){throw new AssertionError(e.getMessage());}return "Aceptado";});
        test("NUC-21","Persistir pipe en MySQL",()->{SoporteBD.reiniciar();var s=SoporteBD.sistema();s.libros.agregarLibro(new Libro("PIPE","Uno|Dos","Autor","9780307474728",2000,"Categoria"));expect(SoporteBD.cargarLibros().getFirst().getTitulo().equals("Uno|Dos"),"Texto alterado");return "Preservado";});
        test("NUC-22","Persistir salto de línea en MySQL",()->{SoporteBD.reiniciar();var s=SoporteBD.sistema();s.usuarios.agregarUsuario(new Usuario("U","Uno\nDos","u@example.com",3));expect(SoporteBD.cargarUsuarios().getFirst().getNombre().equals("Uno\nDos"),"Texto alterado");return "Preservado";});
        test("NUC-23","Pago completo con decimales",()->{Usuario u=user("U");u.agregarMulta(.3);u.pagarMulta(.1);try{u.pagarMulta(.2);}catch(OperacionInvalidaException e){throw new AssertionError("Pago 0.2 rechazado tras 0.1 sobre 0.3: saldo="+u.getMultaAcumulada());}expect(u.puedePedirPrestamo(),"Saldo residual="+u.getMultaAcumulada());return "Sin deuda";});
        test("NUC-24","Devolucion atomica con contador inconsistente",()->{F f=new F();Prestamo p=f.loan();f.a.setPrestamosActuales(0);try{f.gp.registrarDevolucion(p.getIdPrestamo());}catch(BibliotecaException e){}expect(p.getEstado()==Prestamo.EstadoPrestamo.ACTIVO&&!f.l.isDisponible(),"Estado="+p.getEstado()+", disponible="+f.l.isDisponible()+", arbol="+f.gp.contarPrestamosActivos()+", historial="+f.gp.contarHistorial());return "Sin cambios parciales";});
        test("NUC-25","Referencias al editar libro reservado",()->{F f=new F();Prestamo p=f.loan();Reserva r=f.gr.agregarReserva(f.b,f.l);f.gp.registrarDevolucion(p.getIdPrestamo());Libro nuevo=new Libro("L","EDITADO","Autor","9780307474728",2000,"Categoria");f.gl.actualizarLibro(nuevo);f.gp.registrarPrestamo("B","L");expect(!r.getLibro().isDisponible()&&r.getLibro()==f.gl.buscarLibro("L"),"Reserva retiene instancia antigua disponible="+r.getLibro().isDisponible()+"; instancia actual disponible="+f.gl.buscarLibro("L").isDisponible());return "Referencia consistente";});
        test("NUC-26","Historial protegido por FK",()->{SoporteBD.reiniciar();var s=SoporteBD.sistema();s.libros.agregarLibro(book("L"));s.usuarios.agregarUsuario(user("A"));var p=s.prestamos.registrarPrestamo("A","L");s.prestamos.registrarDevolucion(p.getIdPrestamo());reject(()->{s.libros.eliminarLibro("L");return "";});expect(SoporteBD.cargarPrestamos().size()==1,"Perdió historial");return "Conservado";});
        test("NUC-27","Reporte distingue devolucion y cancelacion",()->{F f=new F();Prestamo p=f.loan();f.gp.cancelarPrestamo(p.getIdPrestamo());ByteArrayOutputStream b=new ByteArrayOutputStream();PrintStream old=System.out;try{System.setOut(new PrintStream(b));new GeneradorReportes(f.gl,f.gu,f.gp).generarReportePrestamos();}finally{System.setOut(old);}String txt=b.toString();expect(!txt.contains("Total de devoluciones: 1"),"Una cancelacion se reporta como una devolucion");return "Correcto";});
        test("NUC-28","Restauracion de VENCIDO debe permitir devolucion",()->{F f=new F();Prestamo p=new Prestamo("PREST-11000",f.a,f.l,LocalDate.now().minusDays(20),LocalDate.now().minusDays(3),null,Prestamo.EstadoPrestamo.VENCIDO);f.l.setDisponible(false);f.a.setPrestamosActuales(1);if(p.getEstado()==Prestamo.EstadoPrestamo.ACTIVO)f.gp.restaurarPrestamo(p);else f.gp.restaurarHistorial(p);try{f.gp.registrarDevolucion(p.getIdPrestamo());}catch(BibliotecaException e){throw new AssertionError(e.getMessage());}return "Devuelto";});
        test("NUC-29","Unicode en MySQL",()->{SoporteBD.reiniciar();var s=SoporteBD.sistema();s.libros.agregarLibro(new Libro("UNI","Cien años: acción y ñ","García","9780307474728",2000,"Ficción"));expect(SoporteBD.cargarLibros().getFirst().getTitulo().equals("Cien años: acción y ñ"),"Texto alterado");return "UTF-8 conservado";});
        test("NUC-30","ABB insercion y eliminacion con dos hijos",()->{ArbolBinario<Integer> t=new ArbolBinario<>();for(int n:new int[]{4,2,6,1,3,5,7})t.insertar(n);t.eliminar(4);List<Integer> n=new ArrayList<>();t.recorrerEnOrden(n::add);expect(t.size()==6&&n.equals(List.of(1,2,3,5,6,7)),"ABB incorrecto");return "Orden y size correctos";});
        test("NUC-31","Lista doble elimina centro y extremos",()->{ListaEnlazada<Integer> l=new ListaEnlazada<>();l.agregarAlFinal(2);l.agregarAlInicio(1);l.agregarAlFinal(3);expect(l.eliminar(2)&&l.eliminarPrimero()==1&&l.eliminarUltimo()==3&&l.size()==0,"Lista incorrecta");return "Enlaces y size correctos";});
        test("NUC-32","Cola FIFO y pila LIFO",()->{Cola<Integer> c=new Cola<>();Pila<Integer> p=new Pila<>();c.encolar(1);c.encolar(2);p.apilar(1);p.apilar(2);expect(c.desencolar()==1&&p.desapilar()==2,"Orden incorrecto");return "Orden correcto";});
        test("NUC-33","Correo sin dominio valido",()->{reject(()->{new Usuario("U","Nombre","a@ ",3);return "";});return "Rechazado";});
        test("NUC-34","API de multa rechaza NaN",()->{Usuario u=user("U");u.agregarMulta(5);try{u.pagarMulta(Double.NaN);}catch(OperacionInvalidaException e){return "Rechazado";}expect(Double.isFinite(u.getMultaAcumulada()),"Multa almacenada="+u.getMultaAcumulada());return "Valido";});
        test("NUC-35","Fecha nula informa error de dominio",()->{F f=new F();Prestamo p=f.loan();try{f.gp.editarFechaDevolucion(p.getIdPrestamo(),null);}catch(BibliotecaException e){return "Rechazado con BibliotecaException";}catch(NullPointerException e){throw new AssertionError("NullPointerException en vez de validacion");}return "Aceptado";});
        test("NUC-38","Pago parcial y total normal",()->{Usuario u=user("U");u.agregarMulta(6);u.pagarMulta(2);expect(u.getMultaAcumulada()==4,"Pago parcial incorrecto");u.pagarMulta(4);expect(u.puedePedirPrestamo(),"No habilitado");return "Correcto";});
        test("NUC-39","Cancelar reserva la retira",()->{F f=new F();f.loan();Reserva r=f.gr.agregarReserva(f.b,f.l);f.gr.cancelarReserva(r.getIdReserva());expect(f.gr.contarReservas()==0&&r.getEstado()==Reserva.EstadoReserva.CANCELADA,"No retirada");return "Correcto";});
        Files.write(Path.of(args[0]),rows,java.nio.charset.StandardCharsets.UTF_8);
        System.out.println("RESUMEN: OK="+ok+", FALLO="+fail+", ERROR="+error+", TOTAL="+rows.size());
        System.exit(fail+error>0?1:0);
    }
}
