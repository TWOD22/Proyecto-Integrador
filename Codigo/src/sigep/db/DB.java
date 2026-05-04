package sigep.db;

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
        try { UsuarioDAO.setHabilitado(cedula, hab); return true; }
        catch (SQLException e) { manejar(e); return false; }
    }

    public static boolean actualizarSemestre(String cedula, int semestre) {
        try { UsuarioDAO.actualizarSemestre(cedula, semestre); return true; }
        catch (SQLException e) { manejar(e); return false; }
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

    // Simple event bus para notificar cambios en entidades clave ("instituciones", "usuarios", "documentos")
    public interface ChangeListener { void onChange(String topic); }
    private static final java.util.List<ChangeListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    public static void addChangeListener(ChangeListener l) { listeners.add(l); }
    public static void removeChangeListener(ChangeListener l) { listeners.remove(l); }
    // Explicación: `notifyChange` permite que componentes de UI se suscriban a
    // cambios en entidades (por ejemplo actualizar una tabla cuando cambian instituciones).
    private static void notifyChange(String topic) { for (var l : listeners) { try { l.onChange(topic); } catch (Exception ignored){} } }

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
            // Después de cambiar el estado, comprobar si todos los documentos del estudiante están Aprobados
            String cedula = DocumentoDAO.cedulaPorDocumento(idDocumento);
            if (cedula != null) {
                boolean todos = DocumentoDAO.todosValidos(cedula);
                try { UsuarioDAO.setHabilitado(cedula, todos); } catch (SQLException ignore) {}
            }
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

    public static boolean cambiarEstadoAsignacion(String idAsig, String estado) {
        try { AsignacionDAO.cambiarEstado(idAsig, estado); return true; }
        catch (SQLException e) { manejar(e); return false; }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONEXIÓN
    // ═══════════════════════════════════════════════════════════════════════

    public static String testConexion()  { return Conexion.testConnection(); }
    public static void   cerrarConexion(){ Conexion.close(); }

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
