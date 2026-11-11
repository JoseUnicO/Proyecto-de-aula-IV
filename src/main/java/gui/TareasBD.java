package gui;
import javafx.application.Platform;
import javafx.stage.Window;
import javafx.scene.Parent;
import javafx.scene.Cursor;
import java.util.*;
import java.util.concurrent.*;
/** Mantiene el hilo JavaFX disponible durante JDBC y evita doble pulsación. */
public final class TareasBD {
 private static final ExecutorService ejecutor=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"biblioteca-mysql");t.setDaemon(true);return t;});
 public static <T> T invocar(Callable<T> trabajo)throws Exception{
  if(!Platform.isFxApplicationThread())return trabajo.call();
  if(!Platform.canStartNestedEventLoop())throw new IllegalStateException("La operación debe iniciarse desde una acción del usuario");
  Object bucle=new Object();Map<Parent,Boolean> estados=new IdentityHashMap<>();
  for(Window w:List.copyOf(Window.getWindows()))if(w.getScene()!=null){Parent root=w.getScene().getRoot();estados.put(root,root.isDisable());root.setDisable(true);root.setCursor(Cursor.WAIT);}
  FutureTask<T> futuro=new FutureTask<>(trabajo);
  ejecutor.execute(()->{futuro.run();Platform.runLater(()->Platform.exitNestedEventLoop(bucle,null));});
  try{Platform.enterNestedEventLoop(bucle);return futuro.get();}
  catch(ExecutionException e){if(e.getCause() instanceof Exception x)throw x;throw new IllegalStateException(e.getCause());}
  finally{estados.forEach((root,disabled)->{root.setDisable(disabled);root.setCursor(Cursor.DEFAULT);});}
 }
}
