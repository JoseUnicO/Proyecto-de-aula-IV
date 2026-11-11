package gui;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import controlador.*;
import seguridad.Sesion;
import utilidades.SistemaBiblioteca;
public final class BibliotecaApp extends Application {
 private static SistemaBiblioteca sistema;
 public static void abrir(Sesion sesion)throws java.io.IOException{
  try{sistema=SistemaBiblioteca.cargar(sesion);}catch(java.io.IOException e){sesion.close();throw e;}
 }
 public static void cerrarSesion(){if(sistema!=null){sistema.close();sistema=null;}}
 public static SistemaBiblioteca sistema(){if(sistema==null)throw new IllegalStateException("Debe iniciar sesión");return sistema;}
 public static GestorLibros getGestorLibros(){return sistema().libros;}
 public static GestorUsuarios getGestorUsuarios(){return sistema().usuarios;}
 public static GestorPrestamos getGestorPrestamos(){return sistema().prestamos;}
 public static GestorReservas getGestorReservas(){return sistema().reservas;}
 public static void actualizar()throws Exception{TareasBD.invocar(()->{sistema().actualizar();return null;});}
 public void start(Stage stage){new gui.controllers.LoginController(stage).show();}
 public void stop(){cerrarSesion();}
 public static Image obtenerIcono(){var url=BibliotecaApp.class.getResource("/resources/icon1.png");return url==null?null:new Image(url.toExternalForm());}
 public static void aplicarIconoAStage(Stage stage){Image i=obtenerIcono();if(i!=null)stage.getIcons().setAll(i);}
 public static void aplicarIconoADialog(Dialog<?> dialog){dialog.setOnShown(_->{if(dialog.getDialogPane().getScene().getWindow() instanceof Stage s)aplicarIconoAStage(s);});}
 public static void aplicarIconoAAlert(Alert alert){aplicarIconoADialog(alert);}
}
