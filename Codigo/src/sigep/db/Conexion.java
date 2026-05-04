package sigep.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestor de conexión a Oracle DB.
 *
 * CONFIGURACIÓN:
 *   Host   : localhost (o IP del servidor)
 *   Puerto : 1521
 *   SID    : XE  (Express Edition) — cambiar si difiere
 *   Usuario: ADMI_PRACTICAS
 *   Clave  : admi
 *
 * DRIVER:
 *   Necesita ojdbc11.jar (o ojdbc8.jar) en la carpeta lib/
 *   IntelliJ: File → Project Structure → Libraries → + → lib/ojdbc11.jar
 */
public class Conexion {

    // ── Parámetros de conexión ────────────────────────────────────────────────
    // Estos parámetros configuran la conexión a la base de datos Oracle.
    // Modifícalos solo si sabes la configuración del servidor (host/puerto/SID).
    private static final String HOST    = "localhost";
    private static final String PORT    = "1521";
    private static final String SID     = "orclUDI";          // cambia a tu SID/ServiceName
    private static final String USER    = "ADMI_PRACTICAS";
    private static final String PASS    = "admi";

    private static final String URL =
        "jdbc:oracle:thin:@" + HOST + ":" + PORT + ":" + SID;

    private static Connection instance = null;
    /** Devuelve (o crea) la conexión singleton. */
    // Explicación: este método implementa un singleton para reutilizar la misma
    // conexión en toda la aplicación. Si no existe o está cerrada, la crea.
    // Se aplica `setAutoCommit(false)` para que las transacciones se controlen
    // manualmente (commit/rollback) desde los DAO.
    public static Connection getConnection() throws SQLException {
        if (instance == null || instance.isClosed()) {
            try {
                Class.forName("oracle.jdbc.driver.OracleDriver");
                instance = DriverManager.getConnection(URL, USER, PASS);
                instance.setAutoCommit(false);  // control manual de transacciones
                System.out.println("[DB] Conexión Oracle establecida → " + URL);
            } catch (ClassNotFoundException e) {
                throw new SQLException(
                    """
                    Driver Oracle no encontrado.
                    Agrega ojdbc11.jar a la carpeta lib/ y al classpath del proyecto.
                    Descarga: https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html
                    """,
                    e);
            }
        }
        return instance;
    }

    /** Cierra la conexión (llamar al cerrar la app). */
    // Explicación: libera recursos y cierra el socket hacia la DB. Llamar al
    // cerrar la aplicación para evitar leak de conexiones.
    public static void close() {
        if (instance != null) {
            try { instance.close(); System.out.println("[DB] Conexión cerrada."); }
            catch (SQLException e) { System.err.println("Error cerrando conexión: " + e.getMessage()); }
        }
    }

    /** Prueba la conexión y devuelve un mensaje de resultado. */
    // Explicación: método utilitario para mostrar en la UI si la BD responde.
    public static String testConnection() {
        try {
            Connection c = getConnection();
            if (c != null && !c.isClosed()) {
                return "✅ Conexión exitosa a Oracle DB\nUsuario: " + USER + "\nURL: " + URL;
            }
            return "❌ Conexión nula o cerrada.";
        } catch (SQLException e) {
            return "❌ Error de conexión:\n" + e.getMessage();
        }
    }

    private Conexion() {}
}
