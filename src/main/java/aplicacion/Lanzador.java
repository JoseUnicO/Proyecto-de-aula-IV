package aplicacion;
public final class Lanzador {
 public static void main(String[] args){
  if(args.length>0&&args[0].equals("--consola")){Main.main(new String[0]);return;}
  javafx.application.Application.launch(gui.BibliotecaApp.class,args);
 }
}
