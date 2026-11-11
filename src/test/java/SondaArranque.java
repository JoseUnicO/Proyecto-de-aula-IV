import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.control.*;
import gui.*;
import gui.controllers.*;
/** Instrumentación exclusiva de QA, no incluida en el JAR de producción. */
public final class SondaArranque {
 static AtomicBoolean tomado=new AtomicBoolean();
 public static void iniciar(String destino){
  Thread t=new Thread(()->{for(int i=0;i<180&&!tomado.get();i++){
   try{Thread.sleep(250);Platform.runLater(()->probar(destino));}catch(IllegalStateException e){}catch(InterruptedException e){return;}
  }if(!tomado.get()){System.err.println("BAT_GUI_TIMEOUT");Runtime.getRuntime().halt(92);}},"qa-arranque");t.setDaemon(true);t.start();
 }
 static List<Node> nodos(Node n){List<Node> r=new ArrayList<>();r.add(n);if(n instanceof Parent p)for(Node c:p.getChildrenUnmodifiable())r.addAll(nodos(c));return r;}
 static void captura(Stage s,Path destino)throws Exception{
  s.getScene().getRoot().applyCss();s.getScene().getRoot().layout();var imagen=s.getScene().snapshot(null);
  int w=(int)imagen.getWidth(),h=(int)imagen.getHeight();var png=new java.awt.image.BufferedImage(w,h,java.awt.image.BufferedImage.TYPE_INT_ARGB);
  for(int y=0;y<h;y++)for(int x=0;x<w;x++)png.setRGB(x,y,imagen.getPixelReader().getArgb(x,y));javax.imageio.ImageIO.write(png,"png",destino.toFile());
 }
 static void probar(String ruta){
  if(tomado.get())return;
  Stage s=Window.getWindows().stream().filter(w->w instanceof Stage x&&x.isShowing()&&x.getTitle().contains("Inicio de Sesión")).map(w->(Stage)w).findFirst().orElse(null);
  if(s==null||!tomado.compareAndSet(false,true))return;
  try{
   try{BibliotecaApp.sistema();throw new AssertionError("Datos accesibles antes de login");}catch(IllegalStateException esperado){}
   Path destino=Path.of(ruta);Files.createDirectories(destino);captura(s,destino.resolve("BAT-login.png"));
   for(Node n:nodos(s.getScene().getRoot()))if(n instanceof PasswordField p)p.setText(System.getenv("BIBLIOTECA_QA_PASSWORD"));else if(n instanceof TextField u)u.setText(System.getenv().getOrDefault("BIBLIOTECA_QA_USER","QA"));
   Button entrar=nodos(s.getScene().getRoot()).stream().filter(n->n instanceof Button b&&b.getText().equals("Iniciar Sesión")).map(n->(Button)n).findFirst().orElseThrow();entrar.fire();
   if(BibliotecaApp.sistema().libros.getTotalLibros()<1)throw new AssertionError("Login no cargó los datos");
   var dashboard=new DashboardController(s,new modelo.Admin(1,"QA","Administrador QA"));dashboard.show();
   for(String modulo:List.of("mostrarInicio","mostrarLibros","mostrarUsuarios","mostrarPrestamos","mostrarReservas","mostrarReportes")){
    var m=DashboardController.class.getDeclaredMethod(modulo);m.setAccessible(true);m.invoke(dashboard);captura(s,destino.resolve("BAT-"+modulo+".png"));
   }
   BibliotecaApp.cerrarSesion();new LoginController(s).show();
   try{BibliotecaApp.sistema();throw new AssertionError("Datos accesibles después de logout");}catch(IllegalStateException esperado){}
   System.out.println("BAT_GUI_OK: login, seis módulos y cierre de sesión verificados");s.close();Platform.exit();
  }catch(Throwable e){e.printStackTrace();Runtime.getRuntime().halt(91);}
 }
}
