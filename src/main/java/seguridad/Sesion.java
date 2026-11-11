package seguridad;
import modelo.Admin;
public final class Sesion implements AutoCloseable {
 private final Admin admin; private volatile boolean abierta=true;
 Sesion(Admin admin){this.admin=admin;}
 public Admin administrador(){exigir();return admin;}
 public void exigir(){if(!abierta)throw new IllegalStateException("La sesión está cerrada. Inicie sesión nuevamente.");}
 public void close(){abierta=false;}
}
