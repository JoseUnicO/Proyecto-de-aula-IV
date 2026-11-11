package infraestructura;
import java.sql.*;
import java.util.Properties;
/** Configuración externa. Nunca crea una base ni permite almacenamiento alternativo. */
public final class ConfiguracionBD {
    private ConfiguracionBD() {}
    public static String esquema() {
        String n = System.getenv("BIBLIOTECA_DB_NAME");
        if (n == null || !n.matches("biblioteca(?:_[a-z0-9_]+)?")) throw new IllegalStateException("Falta una base biblioteca válida en la configuración protegida");
        return n;
    }
    public static Connection abrir() throws SQLException {
        String usuario = System.getenv("BIBLIOTECA_DB_USER"), clave = System.getenv("BIBLIOTECA_DB_PASSWORD");
        if (usuario == null || clave == null || clave.isBlank()) throw new SQLException("Configuración protegida de MySQL ausente. Inicie mediante run.bat.");
        Properties p = new Properties();p.setProperty("user",usuario);p.setProperty("password",clave);
        p.setProperty("connectTimeout","5000");p.setProperty("socketTimeout","15000");p.setProperty("characterEncoding","UTF-8");
        p.setProperty("connectionTimeZone","UTC");p.setProperty("sslMode","DISABLED");p.setProperty("allowPublicKeyRetrieval","true");
        // Este despliegue está restringido a loopback; nunca acepta host remoto por configuración.
        return DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/"+esquema(),p);
    }
    public static void verificar() throws SQLException {try(Connection c=abrir()){verificar(c);}}
    public static void verificar(Connection c)throws SQLException{
        String[] definiciones={"V001__biblioteca.sql","V002__retirar_importaciones.sql"};
        try(Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT version,checksum FROM schema_version ORDER BY version")){
            for(int i=0;i<definiciones.length;i++){
                try(var entrada=ConfiguracionBD.class.getResourceAsStream("/db/"+definiciones[i])){
                    if(entrada==null)throw new SQLException("No está disponible la definición del esquema");
                    String esperado=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(entrada.readAllBytes()));
                    if(!r.next()||r.getInt(1)!=i+1||!esperado.equals(r.getString(2)))throw new SQLException("Versión o checksum del esquema incompatible");
                }
            }
            if(r.next())throw new SQLException("Versión del esquema más reciente que la aplicación");
        }catch(java.io.IOException|java.security.GeneralSecurityException e){throw new SQLException("No se pudo verificar el esquema",e);}
    }
}
