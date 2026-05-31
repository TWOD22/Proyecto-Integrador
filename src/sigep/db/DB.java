package sigep.db;

import sigep.StatusModel;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Fachada pública del subsistema de base de datos.
 *
 * Todos los paneles de la UI importan solo esta clase:
 *   import sigep.db.DB;
 *
 * Luego usan:
 *   DB.Usuario u = DB.login(cedula, pass);
 *   List<DB.Institucion> lista = DB.instituciones.listarTodas();
 */
public final class DB {

    private DB() {}

    // ── Re-exports de tipos ──────────────────────────────────────────────────
    public static class Usuario       extends UsuarioDAO.Usuario {}
    public static class Programa      extends ProgramaDAO.Programa {}
    public static class Institucion   extends InstitucionDAO.Institucion {}
    public static class Documento     extends DocumentoDAO.Documento {}
    public static class Seleccion     extends SeleccionDAO.Seleccion {}
    public static class Asignacion    extends AsignacionDAO.Asignacion {}

    // ═══════════════════════════════════════════════════════════════════════
    // AUTENTICACIÓN
    // ═══════════════════════════════════════════════════════════════════════

    /** Login: retorna el usuario si las credenciales son válidas, null si no. */
    public static UsuarioDAO.Usuario login(String cedula, String contrasena) {
        try { return UsuarioDAO.login(cedula, contrasena); }
        catch (SQLException e) { manejar(e); return null; }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // USUARIOS
    // ═══════════════════════════════════════════════════════════════════════

    public static List<Usuario> listarUsuarios() {
        try {
            var raw = UsuarioDAO.listarTodos();
            List<Usuario> out = new ArrayList<>();
            for (UsuarioDAO.Usuario u : raw) {
                Usuario nu = new Usuario();
                nu.cedula = u.cedula; nu.nombre = u.nombre; nu.apellido = u.apellido;
                nu.correo = u.correo; nu.rol = u.rol; nu.idPrograma = u.idPrograma;
                nu.semestre = u.semestre; nu.estadoUsuario = u.estadoUsuario; nu.habilitadoAsig = u.habilitadoAsig;
                out.add(nu);
            }
            return out;
        } catch (SQLException e) { manejar(e); return List.of(); }
    }

    public static List<UsuarioDAO.Usuario> listarPorRol(String rol) {
        try { return UsuarioDAO.listarPorRol(rol); }
        catch (SQLException e) { manejar(e); return List.of(); }
    }

    public static List<UsuarioDAO.Usuario> estudiantesHabilitados() {
        try { return UsuarioDAO.listarEstudiantesHabilitados(); }
        catch (SQLException e) { manejar(e); return List.of(); }
    }

    public static UsuarioDAO.Usuario buscarUsuario(String cedula) {
        try { return UsuarioDAO.buscarPorCedula(cedula); }
        catch (SQLException e) { manejar(e); return null; }
    }

    public static boolean insertarUsuario(UsuarioDAO.Usuario u) {
        try { UsuarioDAO.insertar(u); return true; }
        catch (SQLException e) { manejar(e); return false; }
    }

    public static boolean actualizarUsuario(UsuarioDAO.Usuario u) {
        try { UsuarioDAO.actualizar(u); return true; }
        catch (SQLException e) { manejar(e); return false; }
    }

    public static boolean cambiarEstadoUsuario(String cedula, String estado) {
        try { UsuarioDAO.cambiarEstado(cedula, estado); return true; }
        catch (SQLException e) { manejar(e); return false; }
    }

    public static String cambiarEstadoUsuarioSafe(String cedula, String estado) {
        try { UsuarioDAO.cambiarEstado(cedula, estado); notifyChange("usuarios"); return null; }
        catch (SQLException e) { manejar(e); return e.getMessage(); }
    }

    public static boolean setHabilitadoAsignacion(String cedula, boolean hab) {
        try { UsuarioDAO.setHabilitado(cedula, hab); notifyChange("usuarios"); return true; }
        catch (SQLException e) { manejar(e); return false; }
    }

    /** Establece el estado de asignación del estudiante (0=pendiente,1=esperando,2=listo) */
    public static boolean setEstadoAsignacion(String cedula, int estado) {
        try { UsuarioDAO.setEstadoAsignacion(cedula, estado); notifyChange("usuarios"); return true; }
        catch (SQLException e) { manejar(e); return false; }
    }

    public static boolean actualizarSemestre(String cedula, int semestre) {
        try { UsuarioDAO.actualizarSemestre(cedula, semestre); return true; }
        catch (SQLException e) { manejar(e); return false; }
    }

    /** Elimina un usuario por cédula. Devuelve true si se eliminó correctamente. */
    public static boolean eliminarUsuario(String cedula) {
        try { UsuarioDAO.eliminar(cedula); notifyChange("usuarios"); return true; }
        catch (SQLException e) { manejar(e); return false; }
    }

    /** Variante segura que devuelve null si se eliminó correctamente, o el mensaje de error en caso contrario. */
    public static String eliminarUsuarioSafe(String cedula) {
        try { UsuarioDAO.eliminar(cedula); notifyChange("usuarios"); return null; }
        catch (SQLException e) { manejar(e); return e.getMessage(); }
    }

    /** Devuelve un resumen de filas dependientes que referencian la cédula dada. */
    public static String referenciasDeUsuario(String cedula) {
        StringBuilder sb = new StringBuilder();
        try (Connection c = Conexion.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM Documentos WHERE Cedula_Estudiante=?")) {
                ps.setString(1, cedula); try (ResultSet rs = ps.executeQuery()) { if (rs.next()) sb.append("Documentos: ").append(rs.getInt(1)).append("\n"); }
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM Seleccion_Institucion WHERE Cedula_Estudiante=?")) {
                ps.setString(1, cedula); try (ResultSet rs = ps.executeQuery()) { if (rs.next()) sb.append("Selecciones: ").append(rs.getInt(1)).append("\n"); }
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM Asignacion_Practica WHERE Cedula_Estudiante=?")) {
                ps.setString(1, cedula); try (ResultSet rs = ps.executeQuery()) { if (rs.next()) sb.append("Asignaciones: ").append(rs.getInt(1)).append("\n"); }
            }
        } catch (SQLException ex) { manejar(ex); return "Error al consultar referencias: " + ex.getMessage(); }
        return sb.toString();
    }

    /**
     * Intenta eliminar al usuario y todas las filas dependientes (Documentos, Selecciones, Asignaciones).
     * Devuelve null si todo se borró correctamente, o el mensaje de error si falló.
     * Esta operación es destructiva: pedir confirmación al usuario antes de llamar.
     */
    public static String eliminarUsuarioConDependenciasSafe(String cedula) {
        try {
            // 1) borrar documentos
            var docs = DocumentoDAO.listarPorEstudiante(cedula);
            for (var d : docs) {
                try { DocumentoDAO.eliminar(d.idDocumento); } catch (SQLException ignored) {}
            }
            // 2) borrar selecciones
            try { SeleccionDAO.eliminarPorEstudiante(cedula); } catch (SQLException ignored) {}
            // 3) borrar asignaciones: usar eliminarAsignacionPorId para devolver cupos
            var asigs = AsignacionDAO.listarPorEstudiante(cedula);
            for (var a : asigs) {
                try { eliminarAsignacionPorId(a.idAsignacion); } catch (Exception ignored) {}
            }
            // 4) finalmente borrar el usuario
            try { UsuarioDAO.eliminar(cedula); } catch (SQLException e) { manejar(e); return e.getMessage(); }
            notifyChange("documentos"); notifyChange("selecciones"); notifyChange("asignaciones"); notifyChange("usuarios");
            return null;
        } catch (SQLException e) { manejar(e); return e.getMessage(); }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PROGRAMAS
    // ═══════════════════════════════════════════════════════════════════════

    public static List<Programa> listarProgramas() {
        try {
            var raw = ProgramaDAO.listarTodos();
            List<Programa> out = new ArrayList<>();
            for (ProgramaDAO.Programa p : raw) {
                Programa np = new Programa();
                np.idPrograma = p.idPrograma; np.nombrePrograma = p.nombrePrograma;
                out.add(np);
            }
            return out;
        } catch (SQLException e) { manejar(e); return List.of(); }
    }

    /** Devuelve el nombre del programa dado su id, o cadena vacía si no existe. */
    public static String nombreProgramaPorId(String idPrograma) {
        if (idPrograma == null) return "";
        try {
            var list = ProgramaDAO.listarTodos();
            for (ProgramaDAO.Programa p : list) {
                if (idPrograma.equals(p.idPrograma)) return p.nombrePrograma;
            }
        } catch (SQLException e) { manejar(e); }
        return "";
    }

    public static boolean insertarPrograma(String id, String nombre) {
        try { ProgramaDAO.insertar(id, nombre); return true; }
        catch (SQLException e) { manejar(e); return false; }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // INSTITUCIONES
    // ═══════════════════════════════════════════════════════════════════════

    public static List<Institucion> listarInstituciones() {
        try {
            var raw = InstitucionDAO.listarTodas();
            List<Institucion> out = new ArrayList<>();
            for (InstitucionDAO.Institucion i : raw) {
                Institucion ni = new Institucion();
                ni.idInstitucion = i.idInstitucion; ni.nit = i.nit; ni.nombre = i.nombre;
                ni.direccion = i.direccion; ni.estadoConvenio = i.estadoConvenio; ni.cuposTotales = i.cuposTotales;
                ni.cuposDisp = i.cuposDisp; ni.telefono = i.telefono; ni.correo = i.correo;
                out.add(ni);
            }
            return out;
        } catch (SQLException e) { manejar(e); return List.of(); }
    }

    public static List<Institucion> listarInstitucionesVigentes() {
        try {
            var raw = InstitucionDAO.listarVigentes();
            List<Institucion> out = new ArrayList<>();
            for (InstitucionDAO.Institucion i : raw) {
                Institucion ni = new Institucion();
                ni.idInstitucion = i.idInstitucion; ni.nit = i.nit; ni.nombre = i.nombre;
                ni.direccion = i.direccion; ni.estadoConvenio = i.estadoConvenio; ni.cuposTotales = i.cuposTotales;
                ni.cuposDisp = i.cuposDisp; ni.telefono = i.telefono; ni.correo = i.correo;
                out.add(ni);
            }
            return out;
        } catch (SQLException e) { manejar(e); return List.of(); }
    }

    public static InstitucionDAO.Institucion buscarInstitucion(String id) {
        try { return InstitucionDAO.buscarPorId(id); }
        catch (SQLException e) { manejar(e); return null; }
    }

    public static boolean insertarInstitucion(InstitucionDAO.Institucion i) {
        try { InstitucionDAO.insertar(i); return true; }
        catch (SQLException e) { manejar(e); return false; }
    }

    public static boolean actualizarInstitucion(InstitucionDAO.Institucion i) {
        try { InstitucionDAO.actualizar(i); return true; }
        catch (SQLException e) { manejar(e); return false; }
    }

    public static boolean cambiarEstadoConvenio(String id, String estado) {
        try { InstitucionDAO.cambiarEstado(id, estado); return true; }
        catch (SQLException e) { manejar(e); return false; }
    }

    /** Variante segura que devuelve null si cambió correctamente, o el mensaje de error en caso contrario. */
    public static String cambiarEstadoConvenioSafe(String id, String estado) {
        try { InstitucionDAO.cambiarEstado(id, estado); notifyChange("instituciones"); return null; }
        catch (SQLException e) { manejar(e); return e.getMessage(); }
    }

    /** Elimina una institución por ID. Devuelve true si se eliminó correctamente. */
    public static boolean eliminarInstitucion(String id) {
        try { InstitucionDAO.eliminar(id); notifyChange("instituciones"); return true; }
        catch (SQLException e) { manejar(e); return false; }
    }

    // Simple event bus para notificar cambios en entidades clave ("instituciones", "usuarios", "documentos")
    public interface ChangeListener { void onChange(String topic); }
    private static final java.util.List<ChangeListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    public static void addChangeListener(ChangeListener l) { listeners.add(l); }
    public static void removeChangeListener(ChangeListener l) { listeners.remove(l); }
    // Explicación: `notifyChange` permite que componentes de UI se suscriban a
    // cambios en entidades (por ejemplo actualizar una tabla cuando cambian instituciones).
    private static void notifyChange(String topic) { for (var l : listeners) { try { l.onChange(topic); } catch (Exception ignored){} } }
    // Hook StatusModel so models update when DB notifies topics
    static {
        addChangeListener(topic -> { StatusModel.refreshRegistryFor(topic); });
        // Detect DB capability (0/1 vs 0/1/2 habilitado_asig)
        try { StateUtils.detectThreeStateSupport(); } catch (Throwable ignored) {}
    }

    /** Devuelve los literales contenidos en un CHECK IN(...) para una tabla/columna, o lista vacía. */
    public static java.util.List<String> allowedValues(String tabla, String columna) {
        java.util.List<String> out = new java.util.ArrayList<>();
        String q = "SELECT c.SEARCH_CONDITION FROM USER_CONSTRAINTS c " +
                   "JOIN USER_CONS_COLUMNS cc ON c.CONSTRAINT_NAME=cc.CONSTRAINT_NAME " +
                   "WHERE cc.TABLE_NAME=? AND cc.COLUMN_NAME=? AND c.CONSTRAINT_TYPE='C'";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(q)) {
            ps.setString(1, tabla.toUpperCase()); ps.setString(2, columna.toUpperCase());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String cond = rs.getString(1); if (cond == null) continue;
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("'([^']*)'").matcher(cond);
                while (m.find()) out.add(m.group(1));
            }
        } catch (SQLException ex) { /* ignore and return empty */ }
        return out;
    }

    public static boolean descontarCupo(String idInstitucion) {
        try { InstitucionDAO.descontarCupo(idInstitucion); return true; }
        catch (SQLException e) { manejar(e); return false; }
    }

    public static boolean devolverCupo(String idInstitucion) {
        try { InstitucionDAO.devolverCupo(idInstitucion); return true; }
        catch (SQLException e) { manejar(e); return false; }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DOCUMENTOS
    // ═══════════════════════════════════════════════════════════════════════

    public static List<Documento> listarDocumentos(String cedulaEstudiante) {
        try {
            var raw = DocumentoDAO.listarPorEstudiante(cedulaEstudiante);
            List<Documento> out = new ArrayList<>();
            for (DocumentoDAO.Documento d : raw) {
                Documento nd = new Documento();
                nd.idDocumento = d.idDocumento; nd.cedulaEstudiante = d.cedulaEstudiante; nd.tipoDocumento = d.tipoDocumento;
                nd.estadoDoc = d.estadoDoc; nd.nombreArchivo = d.nombreArchivo; nd.fechaCarga = d.fechaCarga;
                out.add(nd);
            }
            return out;
        } catch (SQLException e) { manejar(e); return List.of(); }
    }

    public static boolean subirDocumento(DocumentoDAO.Documento d) {
        try { DocumentoDAO.insertar(d); return true; }
        catch (SQLException e) { manejar(e); return false; }
    }

    public static boolean subirDocumentoConBlob(DocumentoDAO.Documento d) {
        try { DocumentoDAO.insertarConBlob(d); return true; }
        catch (SQLException e) { manejar(e); return false; }
    }

    /**
     * Variante que devuelve null si se subió correctamente, o el mensaje de error en caso contrario.
     * Útil para mostrar detalles de fallo en la UI sin lanzar excepciones.
     */
    public static String subirDocumentoConBlobSafe(DocumentoDAO.Documento d) {
        try { DocumentoDAO.insertarConBlob(d); return null; }
        catch (SQLException e) {
            manejar(e);
            String msg = e.getMessage();
            try {
                    // Si es un ORA-00904, añadimos los nombres de columna de la tabla Documentos
                if (e.getErrorCode() == 904) {
                    StringBuilder cols = new StringBuilder();
                    String sql = "SELECT COLUMN_NAME FROM USER_TAB_COLUMNS WHERE TABLE_NAME='DOCUMENTOS' ORDER BY COLUMN_ID";
                    try (Statement st = Conexion.getConnection().createStatement(); ResultSet rs = st.executeQuery(sql)) {
                        while (rs.next()) {
                            if (cols.length() > 0) cols.append(", ");
                            cols.append(rs.getString(1));
                        }
                    }
                    if (cols.length() > 0) msg += "\nColumnas en DOCUMENTOS: " + cols.toString();
                }

                // Si es un ORA-02290 (check constraint), mostramos la cláusula del CHECK
                if (e.getErrorCode() == 2290) {
                    String text = e.getMessage();
                    // Buscar nombre de constraint entre paréntesis
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\(([^)]+)\\)").matcher(text);
                    if (m.find()) {
                        String full = m.group(1); // puede ser OWNER.CONSTRAINT
                        String owner = null, cons = full;
                        if (full.contains(".")) { String[] parts = full.split("\\."); owner = parts[0]; cons = parts[1]; }
                        String clause = null;
                        String q;
                        if (owner != null) {
                            q = "SELECT SEARCH_CONDITION FROM ALL_CONSTRAINTS WHERE CONSTRAINT_NAME=? AND OWNER=?";
                            try (PreparedStatement ps = Conexion.getConnection().prepareStatement(q)) {
                                ps.setString(1, cons); ps.setString(2, owner);
                                ResultSet rs = ps.executeQuery(); if (rs.next()) clause = rs.getString(1);
                            }
                        } else {
                            q = "SELECT SEARCH_CONDITION FROM USER_CONSTRAINTS WHERE CONSTRAINT_NAME=?";
                            try (PreparedStatement ps = Conexion.getConnection().prepareStatement(q)) {
                                ps.setString(1, cons);
                                ResultSet rs = ps.executeQuery(); if (rs.next()) clause = rs.getString(1);
                            }
                        }
                        if (clause != null) {
                            msg += "\nCheck clause: " + clause;
                            // Extraer literales dentro de IN(...)
                            java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("IN\\s*\\(([^)]+)\\)").matcher(clause);
                            if (m2.find()) {
                                msg += "\nValores permitidos: " + m2.group(1);
                            }
                        }
                    }
                }
                    String lower = msg == null ? "" : msg.toLowerCase();
                    // Si detectamos que el esquema no soporta BLOBs, devolvemos un mensaje amigable
                    if (lower.contains("contenido_archivo") || lower.contains("nombre_archivo") || lower.contains("no soporta blobs") || lower.contains("no soporta blob")) {
                        String ddl = "-- Ejecutar en Oracle como DBA / usuario con permisos:\n"
                            + "ALTER TABLE Documentos ADD (Nombre_Archivo VARCHAR2(255));\n"
                            + "ALTER TABLE Documentos ADD (Contenido_Archivo BLOB);\n";
                        String friendly = "No fue posible almacenar el archivo en la base de datos porque el esquema actual no admite columnas de tipo BLOB.\n"
                            + "Para garantizar que los documentos subidos permanezcan en la BD, pídale al administrador de la base de datos ejecutar las siguientes sentencias:\n\n"
                            + ddl
                            + "\nDespués de aplicar los cambios, reinicie la aplicación.\nSi necesita soporte, proporcione al administrador este mensaje y los detalles técnicos.\n\nDetalles técnicos:\n" + msg;
                        return friendly;
                    }
            } catch (SQLException ex) { /* ignore metadata errors */ }
            return msg;
        }
    }

    public static byte[] descargarDocumento(String idDocumento) {
        try { return DocumentoDAO.obtenerBlob(idDocumento); }
        catch (SQLException e) { manejar(e); return null; }
    }

    public static boolean cambiarEstadoDocumento(String idDocumento, String estado) {
        try {
            DocumentoDAO.cambiarEstado(idDocumento, estado);
            // If the document belongs to a student who is currently in 'esperando asignacion' (1),
            // revert them to 'pendiente' (0) because a document changed and needs revalidation.
            try {
                String ced = DocumentoDAO.cedulaPorDocumento(idDocumento);
                if (ced != null) {
                    var u = UsuarioDAO.buscarPorCedula(ced);
                    if (u != null && u.habilitadoAsig == 1) {
                        UsuarioDAO.setEstadoAsignacion(ced, 0);
                        // notify that users changed so models/UI update
                        notifyChange("usuarios");
                    }
                }
            } catch (SQLException ex) { manejar(ex); }
            notifyChange("documentos");
            return true;
        } catch (SQLException e) { manejar(e); return false; }
    }

    public static boolean eliminarDocumento(String idDocumento) {
        try { DocumentoDAO.eliminar(idDocumento); return true; }
        catch (SQLException e) { manejar(e); return false; }
    }

    public static boolean todosDocumentosValidos(String cedula) {
        try { return DocumentoDAO.todosValidos(cedula); }
        catch (SQLException e) { manejar(e); return false; }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SELECCIÓN DE INSTITUCIÓN
    // ═══════════════════════════════════════════════════════════════════════

    public static SeleccionDAO.Seleccion seleccionDeEstudiante(String cedula) {
        try { return SeleccionDAO.buscarPorEstudiante(cedula); }
        catch (SQLException e) { manejar(e); return null; }
    }

    public static List<Seleccion> seleccionesPendientes() {
        try {
            var raw = SeleccionDAO.listarPendientes();
            List<Seleccion> out = new ArrayList<>();
            for (SeleccionDAO.Seleccion s : raw) {
                // Skip selections for students who already have an assignment (avoid ghost records)
                boolean hasAsig = false;
                try {
                    var as = AsignacionDAO.listarPorEstudiante(s.cedulaEstudiante);
                    if (as != null && !as.isEmpty()) hasAsig = true;
                } catch (SQLException ignored) { /* if DAO fails, fall back to including the selection */ }
                if (hasAsig) continue;
                Seleccion ns = new Seleccion();
                ns.idSeleccion = s.idSeleccion; ns.cedulaEstudiante = s.cedulaEstudiante; ns.idInstOp1 = s.idInstOp1; ns.idInstOp2 = s.idInstOp2;
                ns.estadoSeleccion = s.estadoSeleccion; ns.nombreOp1 = s.nombreOp1; ns.nombreOp2 = s.nombreOp2; ns.nombreEstudiante = s.nombreEstudiante;
                out.add(ns);
            }
            return out;
        } catch (SQLException e) { manejar(e); return List.of(); }
    }

    public static boolean registrarSeleccion(SeleccionDAO.Seleccion s) {
        try { SeleccionDAO.insertar(s); return true; }
        catch (SQLException e) { manejar(e); return false; }
    }

    public static boolean registrarSeleccionNotify(SeleccionDAO.Seleccion s) {
        try { SeleccionDAO.insertar(s); notifyChange("selecciones"); return true; }
        catch (SQLException e) { manejar(e); return false; }
    }

    /** Variante que devuelve null si se registró correctamente, o el mensaje de error en caso contrario. */
    public static String registrarSeleccionNotifySafe(SeleccionDAO.Seleccion s) {
        try { SeleccionDAO.insertar(s); notifyChange("selecciones"); return null; }
        catch (SQLException e) { manejar(e); return e.getMessage(); }
    }

    /** Overload that accepts the public DB.Seleccion type. */
    public static String registrarSeleccionNotifySafe(Seleccion s) {
        try { SeleccionDAO.insertar(s); notifyChange("selecciones"); return null; }
        catch (SQLException e) { manejar(e); return e.getMessage(); }
    }

    /** Elimina duplicados de selección dejando una sola por estudiante (si existieran). */
    public static void eliminarSeleccionDuplicadosPorEstudiante(String cedula) {
        try { SeleccionDAO.eliminarDuplicadosPorEstudiante(cedula); notifyChange("selecciones"); }
        catch (SQLException e) { manejar(e); }
    }

    /** Elimina la(s) selección(es) de un estudiante; devuelve true si OK. */
    public static boolean eliminarSeleccionPorEstudiante(String cedula) {
        try { SeleccionDAO.eliminarPorEstudiante(cedula); notifyChange("selecciones"); return true; }
        catch (SQLException e) { manejar(e); return false; }
    }

    /** Recorre la tabla Seleccion_Institucion y elimina duplicados para todos los estudiantes. */
    public static void limpiarSeleccionDuplicadosGlobal() {
        String q = "SELECT Cedula_Estudiante FROM Seleccion_Institucion GROUP BY Cedula_Estudiante HAVING COUNT(*)>1";
        try (Statement st = Conexion.getConnection().createStatement(); ResultSet rs = st.executeQuery(q)) {
            java.util.List<String> list = new java.util.ArrayList<>();
            while (rs.next()) list.add(rs.getString(1));
            for (String ced : list) {
                try { SeleccionDAO.eliminarDuplicadosPorEstudiante(ced); } catch (SQLException ignored) {}
            }
            if (!list.isEmpty()) notifyChange("selecciones");
        } catch (SQLException e) { manejar(e); }
    }

    /** Recorre la tabla Asignacion_Practica y elimina duplicados dejando una sola por estudiante. */
    public static void limpiarAsignacionDuplicadosGlobal() {
        String q = "SELECT Cedula_Estudiante FROM Asignacion_Practica GROUP BY Cedula_Estudiante HAVING COUNT(*)>1";
        try (Statement st = Conexion.getConnection().createStatement(); ResultSet rs = st.executeQuery(q)) {
            java.util.List<String> list = new java.util.ArrayList<>();
            while (rs.next()) list.add(rs.getString(1));
            for (String ced : list) {
                try {
                    // Obtener ids ordenados (más reciente primero) y eliminar los extras
                    String q2 = "SELECT ID_Asignacion FROM Asignacion_Practica WHERE Cedula_Estudiante=? ORDER BY ID_Asignacion DESC";
                    java.util.List<String> ids = new java.util.ArrayList<>();
                    try (PreparedStatement ps = Conexion.getConnection().prepareStatement(q2)) {
                        ps.setString(1, ced);
                        ResultSet rs2 = ps.executeQuery(); while (rs2.next()) ids.add(rs2.getString(1));
                    }
                    if (ids.size() > 1) {
                        for (int i = 1; i < ids.size(); i++) {
                            try { AsignacionDAO.eliminar(ids.get(i)); } catch (SQLException ignored) {}
                        }
                    }
                } catch (Exception ignored) {}
            }
            if (!list.isEmpty()) notifyChange("asignaciones");
        } catch (SQLException e) { manejar(e); }
    }

    public static boolean actualizarEstadoSeleccion(String idSeleccion, String estado) {
        try { SeleccionDAO.actualizarEstado(idSeleccion, estado); return true; }
        catch (SQLException e) { manejar(e); return false; }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ASIGNACIONES
    // ═══════════════════════════════════════════════════════════════════════

    public static List<Asignacion> listarAsignaciones() {
        try {
            var raw = AsignacionDAO.listarTodas();
            List<Asignacion> out = new ArrayList<>();
            for (AsignacionDAO.Asignacion a : raw) {
                Asignacion na = new Asignacion();
                na.idAsignacion = a.idAsignacion; na.cedulaEstudiante = a.cedulaEstudiante; na.idInstitucion = a.idInstitucion;
                na.cedulaDocente = a.cedulaDocente; na.cedulaAsesor = a.cedulaAsesor; na.periodoAcademico = a.periodoAcademico;
                na.estadoAsignacion = a.estadoAsignacion; na.nombreEstudiante = a.nombreEstudiante; na.nombreInstitucion = a.nombreInstitucion;
                na.nombreDocente = a.nombreDocente; na.nombreAsesor = a.nombreAsesor; na.programaEstudiante = a.programaEstudiante;
                out.add(na);
            }
            return out;
        } catch (SQLException e) { manejar(e); return List.of(); }
    }

    public static List<Asignacion> asignacionesActivas() {
        try {
            var raw = AsignacionDAO.listarActivas();
            List<Asignacion> out = new ArrayList<>();
            for (AsignacionDAO.Asignacion a : raw) {
                Asignacion na = new Asignacion();
                na.idAsignacion = a.idAsignacion; na.cedulaEstudiante = a.cedulaEstudiante; na.idInstitucion = a.idInstitucion;
                na.cedulaDocente = a.cedulaDocente; na.cedulaAsesor = a.cedulaAsesor; na.periodoAcademico = a.periodoAcademico;
                na.estadoAsignacion = a.estadoAsignacion; na.nombreEstudiante = a.nombreEstudiante; na.nombreInstitucion = a.nombreInstitucion;
                na.nombreDocente = a.nombreDocente; na.nombreAsesor = a.nombreAsesor; na.programaEstudiante = a.programaEstudiante;
                out.add(na);
            }
            return out;
        } catch (SQLException e) { manejar(e); return List.of(); }
    }

    public static List<Asignacion> asignacionesPorEstudiante(String cedula) {
        try {
            var raw = AsignacionDAO.listarPorEstudiante(cedula);
            List<Asignacion> out = new ArrayList<>();
            for (AsignacionDAO.Asignacion a : raw) {
                Asignacion na = new Asignacion();
                na.idAsignacion = a.idAsignacion; na.cedulaEstudiante = a.cedulaEstudiante; na.idInstitucion = a.idInstitucion;
                na.cedulaDocente = a.cedulaDocente; na.cedulaAsesor = a.cedulaAsesor; na.periodoAcademico = a.periodoAcademico;
                na.estadoAsignacion = a.estadoAsignacion; na.nombreEstudiante = a.nombreEstudiante; na.nombreInstitucion = a.nombreInstitucion;
                na.nombreDocente = a.nombreDocente; na.nombreAsesor = a.nombreAsesor; na.programaEstudiante = a.programaEstudiante;
                out.add(na);
            }
            return out;
        } catch (SQLException e) { manejar(e); return List.of(); }
    }

    public static boolean crearAsignacion(AsignacionDAO.Asignacion a) {
        try { AsignacionDAO.insertar(a); return true; }
        catch (SQLException e) { manejar(e); return false; }
    }

    /** Variante que devuelve null si se creó correctamente, o el mensaje de error en caso contrario. */
    public static String crearAsignacionSafe(AsignacionDAO.Asignacion a) {
        // Realizar creación o actualización de asignación en una transacción única
        Connection c = null;
        boolean prevAuto = true;
        try {
            c = Conexion.getConnection();
            prevAuto = c.getAutoCommit();
            c.setAutoCommit(false);

            // Sanitizar estado: si faltan docente/asesor preferimos un literal 'pendiente'
            boolean faltaDoc = (a.cedulaDocente == null || a.cedulaDocente.isEmpty());
            boolean faltaAse = (a.cedulaAsesor == null || a.cedulaAsesor.isEmpty());
            try {
                java.util.List<String> allowed = allowedValues("Asignacion_Practica", "Estado_Asignacion");
                if (allowed != null && !allowed.isEmpty()) {
                    int currentMap = StateUtils.habilitadoFromEstado(a.estadoAsignacion);
                    if ((faltaDoc || faltaAse) && currentMap != 0) {
                        // pick a literal that maps to 0
                        String pick = null;
                        for (String v : allowed) { if (v==null) continue; if (StateUtils.habilitadoFromEstado(v)==0) { pick = v; break; } }
                        if (pick == null) pick = "Pendiente";
                        a.estadoAsignacion = pick;
                    } else if (!(faltaDoc || faltaAse) && currentMap == 0) {
                        // pick an active literal
                        String pick = null;
                        for (String v : allowed) { if (v==null) continue; if (StateUtils.habilitadoFromEstado(v) >= 1) { pick = v; break; } }
                        if (pick == null) pick = "Activa";
                        a.estadoAsignacion = pick;
                    }
                }
            } catch (Exception ignored) {}

            // Comprobar si ya existe asignación para el estudiante
            var existing = AsignacionDAO.listarPorEstudiante(a.cedulaEstudiante);
            if (existing != null && !existing.isEmpty()) {
                // Actualizar la asignación existente delegando en AsignacionDAO (intenta SP primero)
                AsignacionDAO.Asignacion ex = existing.get(0);
                try {
                    AsignacionDAO.actualizarAsignacion(ex.idAsignacion, a.cedulaDocente, a.cedulaAsesor, a.estadoAsignacion);
                } catch (SQLException e) {
                    // si falla, rethrow para rollback y mensaje
                    throw e;
                }

                // Si la institución cambió, ajustar cupos usando DAO helpers
                if (a.idInstitucion != null && !a.idInstitucion.equals(ex.idInstitucion)) {
                    try {
                        var instNow = InstitucionDAO.buscarPorId(a.idInstitucion);
                        if (instNow == null || instNow.cuposDisp <= 0) { c.rollback(); return "No hay cupos disponibles en la institución seleccionada."; }
                        InstitucionDAO.descontarCupo(a.idInstitucion);
                        try { InstitucionDAO.devolverCupo(ex.idInstitucion); } catch (SQLException ignored) {}
                        // Actualizar campo ID_Institucion en la asignación (fallback)
                        try (PreparedStatement psi2 = c.prepareStatement("UPDATE Asignacion_Practica SET ID_Institucion=? WHERE ID_Asignacion=?")) { psi2.setString(1, a.idInstitucion); psi2.setString(2, ex.idAsignacion); psi2.executeUpdate(); }
                    } catch (SQLException exx) { c.rollback(); return exx.getMessage(); }
                }

                // Actualizar estado del usuario a partir del estado proporcionado
                try (PreparedStatement psu = c.prepareStatement("UPDATE Usuario SET Habilitado_Asig=? WHERE Cedula_Usuario=?")) {
                    int mapped = mapEstadoInt(a.estadoAsignacion);
                    var oldU = UsuarioDAO.buscarPorCedula(a.cedulaEstudiante);
                    Integer oldVal = oldU == null ? null : oldU.habilitadoAsig;
                    psu.setInt(1, mapped); psu.setString(2, a.cedulaEstudiante); psu.executeUpdate();
                    sigep.db.HabilitadoLogger.logChange(a.cedulaEstudiante, oldVal, mapped, "DB.crearAsignacionSafe:post-update");
                }

                c.commit(); notifyChange("asignaciones"); notifyChange("usuarios"); notifyChange("instituciones");
                return null;
            }

            // Delegar creación al procedimiento almacenado SP_DIR_PROCESAR_ASIGNACION
            try (CallableStatement cs = c.prepareCall("{ call SP_DIR_PROCESAR_ASIGNACION(?, ?) }")) {
                cs.setString(1, a.idInstitucion);
                cs.setString(2, a.cedulaEstudiante);
                cs.execute();
            }

            // Actualizar estado del usuario según el estado resultante (Asignado -> habilitado 1)
            try (PreparedStatement psu = c.prepareStatement("UPDATE Usuario SET Habilitado_Asig=? WHERE Cedula_Usuario=?")) {
                var oldU = UsuarioDAO.buscarPorCedula(a.cedulaEstudiante);
                Integer oldVal = oldU == null ? null : oldU.habilitadoAsig;
                int newVal = StateUtils.habilitadoFromEstado("Asignado");
                psu.setInt(1, newVal); psu.setString(2, a.cedulaEstudiante); psu.executeUpdate();
                sigep.db.HabilitadoLogger.logChange(a.cedulaEstudiante, oldVal, newVal, "DB.crearAsignacionSafe:post-insert-sp");
            }

            c.commit(); notifyChange("asignaciones"); notifyChange("usuarios"); notifyChange("instituciones");
            return null;
        } catch (SQLException e) {
            try { if (c != null) c.rollback(); } catch (SQLException ignored) {}
            manejar(e); return e.getMessage();
        } finally {
            try { if (c != null) c.setAutoCommit(prevAuto); } catch (SQLException ignored) {}
        }
    }

    /** Variante que actualiza docente/asesor/estado de una asignación existente y devuelve null si OK. */
    public static String actualizarAsignacionSafe(AsignacionDAO.Asignacion a) {
        Connection c = null; boolean prevAuto = true;
        try {
            c = Conexion.getConnection(); prevAuto = c.getAutoCommit(); c.setAutoCommit(false);
            // Delegar la actualización a AsignacionDAO (intenta SP si aplica)
            AsignacionDAO.actualizarAsignacion(a.idAsignacion, a.cedulaDocente, a.cedulaAsesor, a.estadoAsignacion);
            // Actualizar estado del usuario
            String cedulaEst = null;
            try (PreparedStatement q = c.prepareStatement("SELECT Cedula_Estudiante FROM Asignacion_Practica WHERE ID_Asignacion=?")) {
                q.setString(1, a.idAsignacion);
                try (var rs = q.executeQuery()) { if (rs.next()) cedulaEst = rs.getString(1); }
            }
            if (cedulaEst != null) {
                Integer oldVal = null; var oldU = UsuarioDAO.buscarPorCedula(cedulaEst); oldVal = oldU == null ? null : oldU.habilitadoAsig;
                int newVal = mapEstadoInt(a.estadoAsignacion);
                try (PreparedStatement psu = c.prepareStatement("UPDATE Usuario SET Habilitado_Asig=? WHERE Cedula_Usuario=?")) {
                    psu.setInt(1, newVal); psu.setString(2, cedulaEst); psu.executeUpdate();
                }
                sigep.db.HabilitadoLogger.logChange(cedulaEst, oldVal, newVal, "DB.actualizarAsignacionSafe:post-update");
            }
            c.commit(); notifyChange("asignaciones"); notifyChange("usuarios"); notifyChange("instituciones"); return null;
        } catch (SQLException e) { try { if (c!=null) c.rollback(); } catch (SQLException ignored) {} manejar(e); return e.getMessage(); }
        finally { try { if (c!=null) c.setAutoCommit(prevAuto); } catch (SQLException ignored) {} }
    }

    // Helper: map Estado_Asignacion literal to Habilitado_Asig integer (approximation)
    // Note: DB constraint may only allow 0/1 — map to 1 for any assigned/in-progress/final states,
    // and 0 for pending states to avoid CHECK constraint violations.
    private static int mapEstadoInt(String estado) {
        return StateUtils.habilitadoFromEstado(estado);
    }

    /** Elimina una asignación de práctica por ID; devuelve true si se eliminó correctamente.
     *  Al eliminar: devuelve el cupo a la institución y restablece el estado de asignación
     *  del estudiante a '1' (esperando asignación) para que pueda volver a elegir.
     */
    public static boolean eliminarAsignacionPorId(String idAsignacion) {
        try {
            // Obtener registro para saber institución y estudiante
            var a = AsignacionDAO.buscarPorId(idAsignacion);
            if (a == null) return false;
            String ced = a.cedulaEstudiante;
            String idInst = a.idInstitucion;
            // Eliminar la asignación
            AsignacionDAO.eliminar(idAsignacion);
            // Devolver cupo
            try { InstitucionDAO.devolverCupo(idInst); } catch (SQLException ignored) {}
            // Restablecer estado de asignación del estudiante a '1' (esperando asignación)
            try { UsuarioDAO.setEstadoAsignacion(ced, 1); } catch (SQLException ignored) {}
            // Notificar cambios para refrescar UI
            notifyChange("asignaciones"); notifyChange("instituciones"); notifyChange("usuarios");
            return true;
        } catch (SQLException e) { manejar(e); return false; }
    }

    public static boolean cambiarEstadoAsignacion(String idAsig, String estado) {
        try { AsignacionDAO.cambiarEstado(idAsig, estado); return true; }
        catch (SQLException e) { manejar(e); return false; }
    }

    /**
     * Sincroniza el campo Usuario.Habilitado_Asig según el Estado_Asignacion
     * actual de las filas en Asignacion_Practica. Devuelve el número de
     * usuarios actualizados.
     */
    public static int syncAsignacionesToUsuarios() {
        int updated = 0;
        String q = "SELECT Cedula_Estudiante, MAX(Estado_Asignacion) AS EST FROM Asignacion_Practica GROUP BY Cedula_Estudiante";
        try (Connection c = Conexion.getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(q)) {
            java.util.Map<String,String> map = new java.util.HashMap<>();
            while (rs.next()) map.put(rs.getString(1), rs.getString(2));
            for (var e : map.entrySet()) {
                String ced = e.getKey(); String estado = e.getValue(); int val = mapEstadoInt(estado);
                try (PreparedStatement ps = c.prepareStatement("UPDATE Usuario SET Habilitado_Asig=? WHERE Cedula_Usuario=?")) {
                    var oldU = UsuarioDAO.buscarPorCedula(ced);
                    Integer oldVal = oldU == null ? null : oldU.habilitadoAsig;
                    ps.setInt(1, val); ps.setString(2, ced); int n = ps.executeUpdate(); if (n>0) {
                        updated += n;
                        sigep.db.HabilitadoLogger.logChange(ced, oldVal, val, "DB.syncAsignacionesToUsuarios");
                    }
                }
            }
            c.commit(); if (updated>0) notifyChange("usuarios");
        } catch (SQLException ex) { manejar(ex); }
        return updated;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONEXIÓN
    // ═══════════════════════════════════════════════════════════════════════

    public static String testConexion()  { return Conexion.testConnection(); }
    public static void   cerrarConexion(){ Conexion.close(); }

    /** Devuelve información diagnóstica breve sobre la conexión y tablas clave. */
    public static String debugStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append(Conexion.testConnection()).append("\n\n");
        try {
            var c = Conexion.getConnection();
            try (java.sql.Statement st = c.createStatement()) {
                try (java.sql.ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM Asignacion_Practica")) { if (rs.next()) sb.append("Asignacion_Practica rows: ").append(rs.getInt(1)).append("\n"); }
                try (java.sql.ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM Seleccion_Institucion")) { if (rs.next()) sb.append("Seleccion_Institucion rows: ").append(rs.getInt(1)).append("\n"); }
                // sample a few rows
                try (java.sql.ResultSet rs = st.executeQuery("SELECT ID_Asignacion, Cedula_Estudiante, ID_Institucion FROM Asignacion_Practica WHERE ROWNUM<=5")) {
                    sb.append("\nMuestra Asignacion_Practica (<=5):\n");
                    while (rs.next()) {
                        sb.append(rs.getString(1)).append(" | ").append(rs.getString(2)).append(" | ").append(rs.getString(3)).append("\n");
                    }
                }
            }
        } catch (Exception e) {
            sb.append("Error al inspeccionar BD: ").append(e.getMessage()).append("\n");
        }
        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MANEJO DE ERRORES
    // ═══════════════════════════════════════════════════════════════════════

    private static void manejar(SQLException e) {
        // Explicación: centraliza el manejo mínimo de errores SQL para la app.
        // Aquí se imprimen en consola; la UI puede mostrar mensajes amigables.
        System.err.println("[DB ERROR] " + e.getMessage());
        e.printStackTrace();
    }
}
