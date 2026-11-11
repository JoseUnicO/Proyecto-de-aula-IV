import java.lang.instrument.Instrumentation;
public final class AgenteArranque {
 public static void premain(String destino,Instrumentation i)throws Exception{
  if(System.getProperty("sun.java.command", "").startsWith("aplicacion.Lanzador"))Class.forName("SondaArranque").getMethod("iniciar",String.class).invoke(null,destino);
 }
}
