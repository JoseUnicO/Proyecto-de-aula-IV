package seguridad;
import infraestructura.ConfiguracionBD;
import modelo.Admin;
import java.sql.*;
import java.util.Arrays;
import java.security.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
public final class Autenticacion {
 public static final int ITERACIONES=600000;
 public static byte[] hash(char[] clave,byte[] sal,int iteraciones){
  PBEKeySpec spec=new PBEKeySpec(clave,sal,iteraciones,256);
  try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}
  catch(GeneralSecurityException e){throw new IllegalStateException("No está disponible el cifrado de contraseñas",e);}
  finally{spec.clearPassword();}
 }
 public Sesion ingresar(String usuario,char[] clave) throws SQLException {
  try(Connection c=ConfiguracionBD.abrir()){
   ConfiguracionBD.verificar(c);c.setAutoCommit(false);
   try(PreparedStatement q=c.prepareStatement("SELECT *, bloqueado_hasta > UTC_TIMESTAMP(6) bloqueado FROM administradores WHERE usuario=? FOR UPDATE")){
    q.setString(1,usuario);
    try(ResultSet r=q.executeQuery()){
     if(!r.next()){hash(clave,new byte[16],ITERACIONES);c.rollback();throw new IllegalArgumentException("Credenciales incorrectas");}
     if(!r.getBoolean("activo")||r.getBoolean("bloqueado")){c.rollback();throw new IllegalArgumentException("Acceso bloqueado. Si falló cinco veces, espere 15 minutos.");}
     boolean valido=MessageDigest.isEqual(hash(clave,r.getBytes("salt"),r.getInt("iteraciones")),r.getBytes("hash"));
     long id=r.getLong("id");
     int fallos=r.getTimestamp("bloqueado_hasta")!=null?0:r.getInt("fallos");
     try(PreparedStatement u=c.prepareStatement(valido?"UPDATE administradores SET fallos=0,bloqueado_hasta=NULL WHERE id=?":"UPDATE administradores SET fallos=?,bloqueado_hasta=IF(? >= 5,UTC_TIMESTAMP(6)+INTERVAL 15 MINUTE,NULL) WHERE id=?")){
      if(valido)u.setLong(1,id);else{u.setInt(1,fallos+1);u.setInt(2,fallos+1);u.setLong(3,id);}u.executeUpdate();
     }
     c.commit();
     if(!valido)throw new IllegalArgumentException("Credenciales incorrectas"+(fallos+1>=5?". Espere 15 minutos.":""));
     return new Sesion(new Admin(id,r.getString("usuario"),r.getString("nombre")));
    }
   }catch(SQLException|RuntimeException e){c.rollback();throw e;}
  }finally{Arrays.fill(clave,'\0');}
 }
 public static void exigir(Connection c,Sesion sesion)throws SQLException{
  if(sesion==null)throw new IllegalStateException("Debe iniciar sesión");
  try(PreparedStatement q=c.prepareStatement("SELECT activo FROM administradores WHERE id=?")){
   q.setLong(1,sesion.administrador().id());try(ResultSet r=q.executeQuery()){if(!r.next()||!r.getBoolean(1))throw new IllegalStateException("La cuenta no está activa");}
  }
 }
}
