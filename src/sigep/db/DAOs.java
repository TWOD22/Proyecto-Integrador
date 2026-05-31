package sigep.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Comentarios generales: este archivo agrupa varios DAO (objetos de acceso a datos)
// para tablas específicas del sistema (Programa, Institucion, Documento,
// Seleccion, Asignacion). Cada clase está encargada de las consultas SQL
// y de mapear ResultSet a objetos Java simples usados por la capa `DB`.

// ═══════════════════════════════════════════════════════════════════════════
// ProgramaDAO — Tabla T01
// ═══════════════════════════════════════════════════════════════════════════

// DAO: Programa — operaciones CRUD simples sobre la tabla Programa_Academico.
class ProgramaDAO {

    // Modelo simple que representa una fila de la tabla Programa_Academico.
    public static class Programa {
        public String idPrograma, nombrePrograma;
        @Override public String toString() { return nombrePrograma; }
    }

    // Lista todos los programas ordenados por nombre.
    public static List<Programa> listarTodos() throws SQLException {
        List<Programa> lista = new ArrayList<>();
        String sql = "SELECT * FROM Programa_Academico ORDER BY Nombre_Programa";
        try (Statement st = Conexion.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Programa p = new Programa();
                p.idPrograma     = rs.getString("ID_Programa");
                p.nombrePrograma = rs.getString("Nombre_Programa");
                lista.add(p);
            }
        }
        return lista;
    }

    // Inserta un nuevo programa. Se usa desde la capa `DB` o desde utilitarios.
    public static void insertar(String id, String nombre) throws SQLException {
        String sql = "INSERT INTO Programa_Academico VALUES (?, ?)";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, id); ps.setString(2, nombre);
            ps.executeUpdate(); Conexion.getConnection().commit();
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// InstitucionDAO — Tabla T03
// ═══════════════════════════════════════════════════════════════════════════

// DAO: Institucion — manejo de Institucion_Receptora (listar, insertar,
// actualizar, cambiar estado, gestionar cupos).
class InstitucionDAO {

    // Modelo que representa una institución receptora.
    public static class Institucion {
        public String idInstitucion, nit, nombre, direccion,
                      estadoConvenio, telefono, correo;
        public int cuposTotales, cuposDisp;
        public int cuposDisponibles() { return cuposDisp; }
        @Override public String toString() { return nombre; }
    }

    // Devuelve todas las instituciones.
    public static List<Institucion> listarTodas() throws SQLException {
        String sql = "SELECT NIT, INSTITUCION, CONTACTO, CUPOS_DISPONIBLES, ESTADO, ID_INTERNO FROM VW_DIR_LISTADO_INSTITUCIONES";
        try (Statement st = Conexion.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            List<Institucion> lista = new ArrayList<>();
            while (rs.next()) {
                Institucion i = new Institucion();
                i.idInstitucion = rs.getString("ID_INTERNO");
                i.nit = rs.getString("NIT");
                i.nombre = rs.getString("INSTITUCION");
                String contacto = rs.getString("CONTACTO");
                if (contacto != null && contacto.contains(" / ")) {
                    String[] parts = contacto.split(" / ");
                    i.correo = parts[0];
                    i.telefono = parts[1];
                }
                String cupos = rs.getString("CUPOS_DISPONIBLES");
                if (cupos != null && cupos.contains("/")) {
                    String[] parts = cupos.split("/");
                    try {
                        int asignados = Integer.parseInt(parts[0]);
                        i.cuposTotales = Integer.parseInt(parts[1]);
                        i.cuposDisp = i.cuposTotales - asignados;
                    } catch (NumberFormatException e) {
                        // Fallback si el parseo falla
                        i.cuposDisp = 0;
                        i.cuposTotales = 0;
                    }
                }
                i.estadoConvenio = rs.getString("ESTADO");
                lista.add(i);
            }
            return lista;
        }
    }

    // Devuelve solo instituciones con convenio vigente y cupos disponibles.
    public static List<Institucion> listarVigentes() throws SQLException {
        String sql = "SELECT NIT, INSTITUCION, CONTACTO, CUPOS_DISPONIBLES, ESTADO, ID_INTERNO " +
                     "FROM VW_DIR_LISTADO_INSTITUCIONES WHERE ESTADO = 'Vigente'";
        try (Statement st = Conexion.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            List<Institucion> lista = new ArrayList<>();
            while (rs.next()) {
                Institucion i = new Institucion();
                i.idInstitucion = rs.getString("ID_INTERNO");
                i.nit = rs.getString("NIT");
                i.nombre = rs.getString("INSTITUCION");
                String contacto = rs.getString("CONTACTO");
                if (contacto != null && contacto.contains(" / ")) {
                    String[] parts = contacto.split(" / ");
                    i.correo = parts[0];
                    i.telefono = parts[1];
                }
                String cupos = rs.getString("CUPOS_DISPONIBLES");
                if (cupos != null && cupos.contains("/")) {
                    String[] parts = cupos.split("/");
                    try {
                        int asignados = Integer.parseInt(parts[0]);
                        i.cuposTotales = Integer.parseInt(parts[1]);
                        i.cuposDisp = i.cuposTotales - asignados;
                    } catch (NumberFormatException e) {
                        i.cuposDisp = 0;
                        i.cuposTotales = 0;
                    }
                }
                i.estadoConvenio = rs.getString("ESTADO");
                lista.add(i);
            }
            return lista;
        }
    }

    // Busca una institución por su ID.
    public static Institucion buscarPorId(String id) throws SQLException {
        String sql = "SELECT * FROM Institucion_Receptora WHERE ID_Institucion=?";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        }
        return null;
    }

    // Inserta una nueva institución (genera ID si es necesario).
    public static void insertar(Institucion i) throws SQLException {
        // Usar procedimiento almacenado SP_DIR_CREAR_INSTITUCION
        if (i.idInstitucion == null || i.idInstitucion.isEmpty()) {
            i.idInstitucion = IDGenerator.generarID("INST");
        }
        try (CallableStatement cs = Conexion.getConnection().prepareCall("{ call SP_DIR_CREAR_INSTITUCION(?, ?, ?, ?, ?, ?, ?) }")) {
            cs.setString(1, i.idInstitucion);
            cs.setString(2, i.nit);
            cs.setString(3, i.nombre);
            cs.setString(4, i.direccion);
            cs.setString(5, i.telefono);
            cs.setString(6, i.correo);
            cs.setInt(7, i.cuposTotales);
            cs.execute();
            Conexion.getConnection().commit();
        } catch (SQLException ex) {
            // Si el procedimiento falla por restricción de convenio u otro motivo,
            // intentar un INSERT directo como fallback, normalizando Estado_Convenio.
            String estado = i.estadoConvenio;
            if (estado == null || estado.trim().isEmpty()) estado = "Vigente";
            String insert = "INSERT INTO Institucion_Receptora (ID_Institucion, NIT_Institucion, Nombre_Institucion, Direccion, Estado_Convenio, Cupos_Totales, Cupos_Disp, Tel_Institucion, Correo_Institucion) VALUES (?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = Conexion.getConnection().prepareStatement(insert)) {
                ps.setString(1, i.idInstitucion);
                ps.setString(2, i.nit);
                ps.setString(3, i.nombre);
                ps.setString(4, i.direccion);
                ps.setString(5, estado);
                ps.setInt(6, i.cuposTotales);
                ps.setInt(7, i.cuposTotales); // inicialmente disponibles == totales
                ps.setString(8, i.telefono);
                ps.setString(9, i.correo);
                ps.executeUpdate();
                Conexion.getConnection().commit();
            } catch (SQLException ex2) {
                // Re-lanzar la excepción original para mantener trazabilidad
                throw ex;
            }
        }
    }

    // Actualiza los datos de la institución.
    public static void actualizar(Institucion i) throws SQLException {
        String sql = "UPDATE Institucion_Receptora SET NIT_Institucion=?,Nombre_Institucion=?," +
                     "Direccion=?,Estado_Convenio=?,Cupos_Totales=?,Cupos_Disp=?," +
                     "Tel_Institucion=?,Correo_Institucion=? WHERE ID_Institucion=?";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1,i.nit); ps.setString(2,i.nombre); ps.setString(3,i.direccion);
            ps.setString(4,i.estadoConvenio); ps.setInt(5,i.cuposTotales); ps.setInt(6,i.cuposDisp);
            ps.setString(7,i.telefono); ps.setString(8,i.correo); ps.setString(9,i.idInstitucion);
            ps.executeUpdate(); Conexion.getConnection().commit();
        }
    }

    // Cambia el estado del convenio (p. ej. 'Vigente', 'No Vigente').
    public static void cambiarEstado(String id, String nuevoEstado) throws SQLException {
        String sql = "UPDATE Institucion_Receptora SET Estado_Convenio=? WHERE ID_Institucion=?";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, nuevoEstado); ps.setString(2, id);
            ps.executeUpdate(); Conexion.getConnection().commit();
        }
    }

    /** Elimina una institución por ID. Lanzará SQLException si existen FK que lo impidan. */
    public static void eliminar(String id) throws SQLException {
        String sql = "DELETE FROM Institucion_Receptora WHERE ID_Institucion=?";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate(); Conexion.getConnection().commit();
        }
    }

    /** Descuenta un cupo (se llama al confirmar asignación) */
    // Descuenta un cupo disponible (usado al confirmar una asignación).
    public static void descontarCupo(String id) throws SQLException {
        String sql = "UPDATE Institucion_Receptora SET Cupos_Disp = Cupos_Disp - 1 " +
                     "WHERE ID_Institucion=? AND Cupos_Disp>0";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, id); ps.executeUpdate(); Conexion.getConnection().commit();
        }
    }

    /** Devuelve un cupo (cancelación de asignación) */
    // Devuelve un cupo (usado al cancelar una asignación).
    public static void devolverCupo(String id) throws SQLException {
        String sql = "UPDATE Institucion_Receptora SET Cupos_Disp = Cupos_Disp + 1 " +
                     "WHERE ID_Institucion=? AND Cupos_Disp < Cupos_Totales";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, id); ps.executeUpdate(); Conexion.getConnection().commit();
        }
    }

    // Método helper para ejecutar queries que retornan listas de Institucion.
    private static List<Institucion> query(String sql) throws SQLException {
        List<Institucion> lista = new ArrayList<>();
        try (Statement st = Conexion.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(map(rs));
        }
        return lista;
    }

    // Convierte un ResultSet en una instancia de Institucion.
    private static Institucion map(ResultSet rs) throws SQLException {
        Institucion i = new Institucion();
        i.idInstitucion  = rs.getString("ID_Institucion");
        i.nit            = rs.getString("NIT_Institucion");
        i.nombre         = rs.getString("Nombre_Institucion");
        i.direccion      = rs.getString("Direccion");
        i.estadoConvenio = rs.getString("Estado_Convenio");
        i.cuposTotales   = rs.getInt("Cupos_Totales");
        i.cuposDisp      = rs.getInt("Cupos_Disp");
        i.telefono       = rs.getString("Tel_Institucion");
        i.correo         = rs.getString("Correo_Institucion");
        return i;
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// DocumentoDAO — Tabla T04
// ═══════════════════════════════════════════════════════════════════════════

// DAO: Documento — maneja almacenamiento y recuperación de documentos
// (soporta BLOBs y metadatos como estado, fecha, nombre de archivo).
class DocumentoDAO {

    // Modelo que representa la fila de la tabla Documentos.
    public static class Documento {
        public String idDocumento, cedulaEstudiante, tipoDocumento,
                      estadoDoc, nombreArchivo;
        public byte[] contenidoBlob;  // Para almacenar contenido del archivo
        public Timestamp fechaCarga;
    }

    // Extrae literales de un CHECK constraint tipo IN(...) para una tabla/columna.
    private static List<String> obtenerValoresCheck(String tabla, String columna) throws SQLException {
        List<String> out = new ArrayList<>();
        String q = "SELECT c.SEARCH_CONDITION FROM USER_CONSTRAINTS c " +
                   "JOIN USER_CONS_COLUMNS cc ON c.CONSTRAINT_NAME=cc.CONSTRAINT_NAME " +
                   "WHERE cc.TABLE_NAME=? AND cc.COLUMN_NAME=? AND c.CONSTRAINT_TYPE='C'";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(q)) {
            ps.setString(1, tabla.toUpperCase()); ps.setString(2, columna.toUpperCase());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String cond = rs.getString(1);
                if (cond == null) continue;
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("'([^']*)'").matcher(cond);
                while (m.find()) out.add(m.group(1));
            }
        }
        return out;
    }

    // Lista documentos asociados a una cédula, ordenados por fecha (más reciente primero).
    public static List<Documento> listarPorEstudiante(String cedula) throws SQLException {
        String sql = "SELECT ID_DOC, CEDULA_ESTUDIANTE, TIPO_DOCUMENTO, ESTADO, FECHA_SUBIDA " +
                     "FROM VW_EST_MIS_DOCUMENTOS WHERE CEDULA_ESTUDIANTE=? ORDER BY FECHA_SUBIDA DESC";
        List<Documento> lista = new ArrayList<>();
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, cedula);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Documento d = new Documento();
                d.idDocumento = rs.getString("ID_DOC");
                d.cedulaEstudiante = rs.getString("CEDULA_ESTUDIANTE");
                d.tipoDocumento = rs.getString("TIPO_DOCUMENTO");
                d.estadoDoc = rs.getString("ESTADO");
                d.fechaCarga = rs.getTimestamp("FECHA_SUBIDA");
                d.nombreArchivo = d.tipoDocumento; // La vista ahora unifica el nombre
                lista.add(d);
            }
        }
        return lista;
    }

    /**
     * Inserta un documento con contenido BLOB
     */
    // Inserta un documento guardando el contenido en BLOB dentro de la BD.
    // Si el esquema no soporta BLOBs, lanza SQLException explicativa.
    public static void insertarConBlob(Documento d) throws SQLException {
        // Verificar que la tabla contenga las columnas esperadas para BLOB/NOMBRE
        boolean hasContenido = false;
        boolean hasNombre = false;
        try (PreparedStatement psChk = Conexion.getConnection().prepareStatement(
                "SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE TABLE_NAME='DOCUMENTOS' AND COLUMN_NAME=?")) {
            psChk.setString(1, "CONTENIDO_ARCHIVO");
            ResultSet rs1 = psChk.executeQuery(); if (rs1.next()) hasContenido = rs1.getInt(1) > 0;
            psChk.setString(1, "NOMBRE_ARCHIVO");
            ResultSet rs2 = psChk.executeQuery(); if (rs2.next()) hasNombre = rs2.getInt(1) > 0;
        }

        if (hasContenido && hasNombre) {
            // Usar procedimiento almacenado SP_EST_GUARDAR_DOCUMENTO(p_cedula, p_nombre_doc, p_archivo)
            // Asegurar que Estado_Doc cumple el CHECK: si existe un CHECK con literales, usar el primero como default
            try {
                List<String> allowed = obtenerValoresCheck("DOCUMENTOS", "ESTADO_DOC");
                if (!allowed.isEmpty()) {
                    if (d.estadoDoc == null || !allowed.contains(d.estadoDoc)) d.estadoDoc = allowed.get(0);
                } else {
                    if (d.estadoDoc == null) d.estadoDoc = "En Revisión";
                }
            } catch (SQLException ex) { if (d.estadoDoc == null) d.estadoDoc = "En Revisión"; }

                // Generar ID localmente para referencia
                if (d.idDocumento == null) d.idDocumento = IDGenerator.generarID("DOC");
                try (CallableStatement cs = Conexion.getConnection().prepareCall("{ call SP_EST_GUARDAR_DOCUMENTO(?, ?, ?) }")) {
                    cs.setString(1, d.cedulaEstudiante);
                    cs.setString(2, d.nombreArchivo != null ? d.nombreArchivo : d.tipoDocumento);
                    if (d.contenidoBlob != null) cs.setBytes(3, d.contenidoBlob); else cs.setNull(3, java.sql.Types.BLOB);
                    cs.execute();
                    Conexion.getConnection().commit();
                    System.err.println("[DocumentoDAO] SP inserted BLOB doc cedula="+d.cedulaEstudiante+" nombre="+d.nombreArchivo);
                } catch (SQLException ex) {
                    // SP failed (e.g., ORA-01400 inserting NULL ID_DOCUMENTO). Fallback to manual INSERT
                    System.err.println("[DocumentoDAO] SP_EST_GUARDAR_DOCUMENTO failed, falling back to INSERT: " + ex.getMessage());
                    String sql = "INSERT INTO Documentos (ID_Documento, Cedula_Estudiante, Tipo_Documento, Estado_Doc, Nombre_Archivo, Contenido_Archivo, Fecha_Carga) VALUES (?,?,?,?,?,?,SYSDATE)";
                    try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
                        ps.setString(1, d.idDocumento);
                        ps.setString(2, d.cedulaEstudiante);
                        ps.setString(3, d.tipoDocumento);
                        ps.setString(4, d.estadoDoc != null ? d.estadoDoc : "En Revisión");
                        ps.setString(5, d.nombreArchivo != null ? d.nombreArchivo : d.tipoDocumento);
                        if (d.contenidoBlob != null) ps.setBytes(6, d.contenidoBlob); else ps.setNull(6, java.sql.Types.BLOB);
                        ps.executeUpdate();
                        Conexion.getConnection().commit();
                        System.err.println("[DocumentoDAO] Fallback INSERT completed for ID="+d.idDocumento);
                    } catch (SQLException ex2) {
                        // Re-throw original exception to preserve root cause
                        throw ex;
                    }
                }
        } else {
            // No permitimos fallback a disco: exigimos que la tabla DOCUMENTOS tenga columna
            // CONTENIDO_ARCHIVO y NOMBRE_ARCHIVO para garantizar persistencia en la BD.
            throw new SQLException("El esquema de la base de datos no soporta BLOBs. Falta la columna CONTENIDO_ARCHIVO/NOMBRE_ARCHIVO en la tabla DOCUMENTOS.\n" +
                    "La aplicación requiere almacenar los archivos en la base de datos para garantizar persistencia. Contacte al administrador para actualizar el esquema.");
        }
    }

    /**
     * Inserta un documento (ruta tradicional, sin BLOB)
     */
    // Inserta un documento sin contenido BLOB (solo metadatos / nombre de archivo).
    public static void insertar(Documento d) throws SQLException {
        // Preferir el procedimiento almacenado SP_EST_GUARDAR_DOCUMENTO para centralizar la lógica
        boolean hasNombre = false;
        try (PreparedStatement psChk = Conexion.getConnection().prepareStatement(
                "SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE TABLE_NAME='DOCUMENTOS' AND COLUMN_NAME=?")) {
            psChk.setString(1, "NOMBRE_ARCHIVO");
            ResultSet rs = psChk.executeQuery(); if (rs.next()) hasNombre = rs.getInt(1) > 0;
        }
        String nombre = d.nombreArchivo != null ? d.nombreArchivo : d.tipoDocumento;
        try (CallableStatement cs = Conexion.getConnection().prepareCall("{ call SP_EST_GUARDAR_DOCUMENTO(?, ?, ?) }")) {
            cs.setString(1, d.cedulaEstudiante);
            cs.setString(2, nombre);
            if (d.contenidoBlob != null) cs.setBytes(3, d.contenidoBlob); else cs.setNull(3, java.sql.Types.BLOB);
            cs.execute(); Conexion.getConnection().commit();
            return;
        } catch (SQLException ex) {
            // fallback: intentar INSERT tradicional
        }
        // Fallback: insertar metadatos si el SP no existe o falla
        String sql = hasNombre ?
            "INSERT INTO Documentos (ID_Documento, Cedula_Estudiante, Tipo_Documento, Estado_Doc, Nombre_Archivo, Fecha_Carga) VALUES (?,?,?,?,?,SYSDATE)" :
            "INSERT INTO Documentos (ID_Documento, Cedula_Estudiante, Tipo_Documento, Estado_Doc, Fecha_Carga) VALUES (?,?,?,?,SYSDATE)";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, IDGenerator.generarID("DOC"));
            ps.setString(2, d.cedulaEstudiante);
            ps.setString(3, d.tipoDocumento);
            ps.setString(4, d.estadoDoc != null ? d.estadoDoc : "En Revisión");
            if (hasNombre) ps.setString(5, nombre);
            ps.executeUpdate(); Conexion.getConnection().commit();
        }
    }

    /**
     * Recupera el contenido BLOB de un documento
     */
    // Recupera el contenido BLOB de un documento dado su ID.
    public static byte[] obtenerBlob(String idDocumento) throws SQLException {
        // Leer siempre desde la columna CONTENIDO_ARCHIVO
        String sql = "SELECT Contenido_Archivo FROM Documentos WHERE ID_Documento=?";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, idDocumento);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBytes("Contenido_Archivo");
        }
        return null;
    }

    /** Elimina un documento por ID */
    // Elimina un documento por su ID.
    public static void eliminar(String idDocumento) throws SQLException {
        String sql = "DELETE FROM Documentos WHERE ID_Documento=?";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, idDocumento);
            ps.executeUpdate();
            Conexion.getConnection().commit();
        }
    }

    /** Director: cambia estado a 'Aprobado' o 'Rechazado' */
    // Cambia el estado de un documento (usado por el director para aprobar/rechazar).
    public static void cambiarEstado(String idDocumento, String nuevoEstado) throws SQLException {
        // Agregamos el prefijo de esquema PRUEBA y enviamos ambos parámetros como String
        String sql = "{ call PRUEBA.SP_DIR_EVALUAR_DOCUMENTO(?, ?) }";
        try (Connection conn = Conexion.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.setString(1, idDocumento);
            cs.setString(2, nuevoEstado);
            cs.execute();
            // No hacemos commit aquí; el SP se encarga del COMMIT según la nueva implementación.
        }
    }

    /** Devuelve la cédula del estudiante asociada a un documento */
    // Devuelve la cédula del estudiante asociada a un documento.
    public static String cedulaPorDocumento(String idDocumento) throws SQLException {
        String sql = "SELECT Cedula_Estudiante FROM Documentos WHERE ID_Documento=?";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, idDocumento);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString(1);
        }
        return null;
    }

    /** Verifica si TODOS los documentos del estudiante están en 'Aprobado' */
    // Verifica si todos los documentos de un estudiante están en 'Aprobado'.
    public static boolean todosValidos(String cedula) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Documentos WHERE Cedula_Estudiante=? AND Estado_Doc <> 'Aprobado'";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, cedula);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) == 0;
        }
    }

    // Mapea una fila ResultSet a un objeto Documento.
    private static Documento map(ResultSet rs) throws SQLException {
        Documento d = new Documento();
        d.idDocumento      = rs.getString("ID_Documento");
        d.cedulaEstudiante = rs.getString("Cedula_Estudiante");
        d.tipoDocumento    = rs.getString("Tipo_Documento");
        d.estadoDoc        = rs.getString("Estado_Doc");
        // Ruta_Archivo removed from schema
        try { d.nombreArchivo = rs.getString("Nombre_Archivo"); } catch (SQLException ignored) { d.nombreArchivo = null; }
        d.fechaCarga       = rs.getTimestamp("Fecha_Carga");
        return d;
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SeleccionDAO — Tabla T05
// ═══════════════════════════════════════════════════════════════════════════

// DAO: Seleccion — gestiona las elecciones de instituciones por estudiante.
class SeleccionDAO {

    // Modelo para la selección de instituciones por estudiante.
    public static class Seleccion {
        public String idSeleccion, cedulaEstudiante,
                      idInstOp1, idInstOp2, estadoSeleccion;
        // Campos auxiliares (JOIN)
        public String nombreOp1, nombreOp2, nombreEstudiante;
    }

    // Busca la selección registrada de un estudiante (opciones y estado).
    public static Seleccion buscarPorEstudiante(String cedula) throws SQLException {
        String sql = "SELECT s.*, i1.Nombre_Institucion AS n1, i2.Nombre_Institucion AS n2 " +
                     "FROM Seleccion_Institucion s " +
                     "JOIN Institucion_Receptora i1 ON s.ID_Inst_Op1 = i1.ID_Institucion " +
                     "JOIN Institucion_Receptora i2 ON s.ID_Inst_Op2 = i2.ID_Institucion " +
                     "WHERE s.Cedula_Estudiante=?";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, cedula);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        }
        return null;
    }

    // Lista selecciones que están pendientes de revisión/procesamiento.
    // Leer directamente desde la tabla `Seleccion_Institucion` para evitar discrepancias con vistas
    public static List<Seleccion> listarPendientes() throws SQLException {
        String sql = "SELECT s.Cedula_Estudiante, est.Nombre || ' ' || est.Apellido AS NOMBRE, " +
                     "i1.Nombre_Institucion AS OPCION_1, i2.Nombre_Institucion AS OPCION_2, " +
                     "p.Nombre_Programa AS PROGRAMA, est.Semestre AS SEMESTRE " +
                     "FROM Seleccion_Institucion s " +
                     "JOIN Usuario est ON s.Cedula_Estudiante = est.Cedula_Usuario " +
                     "LEFT JOIN Institucion_Receptora i1 ON s.ID_Inst_Op1 = i1.ID_Institucion " +
                     "LEFT JOIN Institucion_Receptora i2 ON s.ID_Inst_Op2 = i2.ID_Institucion " +
                     "LEFT JOIN Programa_Academico p ON est.ID_Programa = p.ID_Programa " +
                     "ORDER BY est.Nombre, est.Apellido";
        List<Seleccion> lista = new ArrayList<>();
        try (Statement st = Conexion.getConnection().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Seleccion s = new Seleccion();
                s.cedulaEstudiante = rs.getString("CEDULA_ESTUDIANTE");
                s.nombreEstudiante = rs.getString("NOMBRE");
                s.nombreOp1 = rs.getString("OPCION_1");
                s.nombreOp2 = rs.getString("OPCION_2");
                s.estadoSeleccion = "Pendiente";
                lista.add(s);
            }
        }
        return lista;
    }

    // Inserta una nueva selección para un estudiante.
    public static void insertar(Seleccion s) throws SQLException {
        String sql = "INSERT INTO Seleccion_Institucion VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, "SEL-" + System.currentTimeMillis() % 100000);
            ps.setString(2, s.cedulaEstudiante);
            ps.setString(3, s.idInstOp1);
            ps.setString(4, s.idInstOp2);
            ps.setString(5, "Pendiente");
            ps.executeUpdate(); Conexion.getConnection().commit();
        }
    }

    // Elimina selecciones duplicadas dejando solo la más reciente (por ID) para el estudiante dado.
    public static void eliminarDuplicadosPorEstudiante(String cedula) throws SQLException {
        String q = "SELECT ID_Seleccion FROM Seleccion_Institucion WHERE Cedula_Estudiante=? ORDER BY ID_Seleccion DESC";
        java.util.List<String> ids = new java.util.ArrayList<>();
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(q)) {
            ps.setString(1, cedula);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getString(1));
        }
        // conservar la primera (más reciente) y eliminar el resto
        if (ids.size() > 1) {
            String del = "DELETE FROM Seleccion_Institucion WHERE ID_Seleccion=?";
            try (PreparedStatement psd = Conexion.getConnection().prepareStatement(del)) {
                for (int i = 1; i < ids.size(); i++) {
                    psd.setString(1, ids.get(i));
                    psd.executeUpdate();
                }
            }
            Conexion.getConnection().commit();
        }
    }

    // Actualiza el estado de una selección (p. ej. 'Pendiente' -> 'Procesada').
    public static void actualizarEstado(String idSeleccion, String estado) throws SQLException {
        String sql = "UPDATE Seleccion_Institucion SET Estado_Seleccion=? WHERE ID_Seleccion=?";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, estado); ps.setString(2, idSeleccion);
            ps.executeUpdate(); Conexion.getConnection().commit();
        }
    }

    // Elimina la(s) selección(es) asociadas a un estudiante (por cédula).
    public static void eliminarPorEstudiante(String cedula) throws SQLException {
        String sql = "DELETE FROM Seleccion_Institucion WHERE Cedula_Estudiante=?";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, cedula);
            ps.executeUpdate();
            Conexion.getConnection().commit();
        }
    }

    // Convierte una fila ResultSet a Seleccion.
    private static Seleccion map(ResultSet rs) throws SQLException {
        Seleccion s = new Seleccion();
        s.idSeleccion       = rs.getString("ID_Seleccion");
        s.cedulaEstudiante  = rs.getString("Cedula_Estudiante");
        s.idInstOp1         = rs.getString("ID_Inst_Op1");
        s.idInstOp2         = rs.getString("ID_Inst_Op2");
        s.estadoSeleccion   = rs.getString("Estado_Seleccion");
        try { s.nombreOp1   = rs.getString("n1"); } catch (SQLException | NullPointerException ignored) {}
        try { s.nombreOp2   = rs.getString("n2"); } catch (SQLException | NullPointerException ignored) {}
        return s;
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// AsignacionDAO — Tabla T06
// ═══════════════════════════════════════════════════════════════════════════

// DAO: Asignacion — gestiona asignaciones de prácticas (listar, insertar, cambiar estado).
class AsignacionDAO {

    // Modelo que representa una asignación de práctica.
    public static class Asignacion {
        public String idAsignacion, cedulaEstudiante, idInstitucion,
                      cedulaDocente, cedulaAsesor,
                      periodoAcademico, estadoAsignacion;
        // Campos auxiliares (JOIN)
        public String nombreEstudiante, nombreInstitucion,
                      nombreDocente, nombreAsesor, programaEstudiante;
    }

    // Devuelve todas las asignaciones (incluye joins para nombres legibles).
    public static List<Asignacion> listarTodas() throws SQLException {
        String sql = buildSelectJoin(null);
        return ejecutarLista(sql, null);
    }

    // Devuelve solo las asignaciones activas.
    public static List<Asignacion> listarActivas() throws SQLException {
        // Leer directamente desde la tabla Asignacion_Practica con JOINs para asegurar que
        // se incluyan registros que no tengan docente/asesor o que la vista pudiera filtrar.
        String sql = buildSelectJoin(null);
        return ejecutarLista(sql, null);
    }

    // Lista asignaciones para un estudiante específico.
    public static List<Asignacion> listarPorEstudiante(String cedula) throws SQLException {
        String sql = buildSelectJoin("a.Cedula_Estudiante=?");
        return ejecutarLista(sql, cedula);
    }

    // Inserta una nueva asignación en la tabla Asignacion_Practica.
    public static void insertar(Asignacion a) throws SQLException {
        // Usar procedimiento almacenado SP_DIR_PROCESAR_ASIGNACION
        try (CallableStatement cs = Conexion.getConnection().prepareCall("{ call SP_DIR_PROCESAR_ASIGNACION(?, ?) }")) {
            cs.setString(1, a.idInstitucion);
            cs.setString(2, a.cedulaEstudiante);
            cs.execute();
            Conexion.getConnection().commit();
        }
    }

    // Cambia el estado de una asignación (p. ej. 'Activa' -> 'Finalizada').
    public static void cambiarEstado(String idAsignacion, String estado) throws SQLException {
        String sql = "UPDATE Asignacion_Practica SET Estado_Asignacion=? WHERE ID_Asignacion=?";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, estado); ps.setString(2, idAsignacion);
            ps.executeUpdate(); Conexion.getConnection().commit();
        }
    }

    // Actualiza docente, asesor y estado de una asignación existente.
    public static void actualizarAsignacion(String idAsignacion, String cedulaDocente, String cedulaAsesor, String estado) throws SQLException {
        // Intentar usar SP_DIR_COMPLETAR_TUTORES si el ID es numérico y se proveen tutores
        String digits = idAsignacion == null ? "" : idAsignacion.replaceAll("\\D", "");
        if (!digits.isEmpty() && (cedulaDocente != null || cedulaAsesor != null)) {
            try {
                int numericId = Integer.parseInt(digits);
                try (CallableStatement cs = Conexion.getConnection().prepareCall("{ call SP_DIR_COMPLETAR_TUTORES(?, ?, ?) }")) {
                    cs.setInt(1, numericId);
                    cs.setString(2, cedulaDocente);
                    cs.setString(3, cedulaAsesor);
                    cs.execute(); Conexion.getConnection().commit();
                    return;
                }
            } catch (NumberFormatException | SQLException ignored) {
                // fallback to manual update below
            }
        }
        // Fallback: ejecutar UPDATE clásico
        String sql = "UPDATE Asignacion_Practica SET Cedula_Docente=?, Cedula_Asesor=?, Estado_Asignacion=? WHERE ID_Asignacion=?";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, cedulaDocente);
            ps.setString(2, cedulaAsesor);
            ps.setString(3, estado);
            ps.setString(4, idAsignacion);
            ps.executeUpdate(); Conexion.getConnection().commit();
        }
    }

    // Construye la consulta SELECT con JOINs para obtener campos relacionados.
    private static String buildSelectJoin(String where) {
        String sql = "SELECT a.*, " +
            "est.Nombre||' '||est.Apellido AS nomEst, " +
            "doc.Nombre||' '||doc.Apellido AS nomDoc, " +
            "ase.Nombre||' '||ase.Apellido AS nomAse, " +
            "inst.Nombre_Institucion AS nomInst, " +
            "p.Nombre_Programa AS progEst " +
            "FROM Asignacion_Practica a " +
            "JOIN Usuario est  ON a.Cedula_Estudiante = est.Cedula_Usuario " +
            "LEFT JOIN Usuario doc  ON a.Cedula_Docente    = doc.Cedula_Usuario " +
            "LEFT JOIN Usuario ase  ON a.Cedula_Asesor     = ase.Cedula_Usuario " +
            "JOIN Institucion_Receptora inst ON a.ID_Institucion = inst.ID_Institucion " +
            "LEFT JOIN Programa_Academico p ON est.ID_Programa = p.ID_Programa";
        if (where != null) sql += " WHERE " + where;
        return sql + " ORDER BY a.Periodo_Academico DESC";
    }

    // Ejecuta la consulta y mapea cada fila a Asignacion.
    private static List<Asignacion> ejecutarLista(String sql, String param) throws SQLException {
        List<Asignacion> lista = new ArrayList<>();
        PreparedStatement ps = Conexion.getConnection().prepareStatement(sql);
        if (param != null) ps.setString(1, param);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Asignacion a = new Asignacion();
            a.idAsignacion      = rs.getString("ID_Asignacion");
            a.cedulaEstudiante  = rs.getString("Cedula_Estudiante");
            a.idInstitucion     = rs.getString("ID_Institucion");
            a.cedulaDocente     = rs.getString("Cedula_Docente");
            a.cedulaAsesor      = rs.getString("Cedula_Asesor");
            a.periodoAcademico  = rs.getString("Periodo_Academico");
            a.estadoAsignacion  = rs.getString("Estado_Asignacion");
            a.nombreEstudiante  = rs.getString("nomEst");
            a.nombreDocente     = rs.getString("nomDoc");
            a.nombreAsesor      = rs.getString("nomAse");
            a.nombreInstitucion = rs.getString("nomInst");
            a.programaEstudiante= rs.getString("progEst");
            lista.add(a);
        }
        ps.close();
        return lista;
    }

    /** Lista estudiantes asignados a una institución usando la vista VW_DIR_POPOP_ESTUDIANTES_INST */
    public static List<Asignacion> listarPorInstitucion(String idInstitucion) throws SQLException {
        // Leer directamente desde la tabla y JOINs para obtener todos los campos útiles
        String sql = buildSelectJoin("a.ID_Institucion = ?");
        List<Asignacion> lista = new ArrayList<>();
        PreparedStatement ps = Conexion.getConnection().prepareStatement(sql);
        ps.setString(1, idInstitucion);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Asignacion a = new Asignacion();
            a.idAsignacion = rs.getString("ID_Asignacion");
            a.cedulaEstudiante = rs.getString("Cedula_Estudiante");
            a.idInstitucion = rs.getString("ID_Institucion");
            a.cedulaDocente = rs.getString("Cedula_Docente");
            a.cedulaAsesor = rs.getString("Cedula_Asesor");
            a.periodoAcademico = rs.getString("Periodo_Academico");
            a.estadoAsignacion = rs.getString("Estado_Asignacion");
            a.nombreEstudiante = rs.getString("nomEst");
            a.nombreDocente = rs.getString("nomDoc");
            a.nombreAsesor = rs.getString("nomAse");
            a.nombreInstitucion = rs.getString("nomInst");
            a.programaEstudiante = rs.getString("progEst");
            lista.add(a);
        }
        ps.close();
        return lista;
    }

    /** Lista asignaciones que necesitan tutor (docente o asesor) usando VW_DIR_ASIG_BUSCAR_TUTOR */
    public static List<Asignacion> listarSinTutor() throws SQLException {
        String sql = "SELECT NOMBRE, PROGRAMA, INSTITUCION_RECEPTORA, CEDULA_FILTRO, ID_ASIG_INTERNO FROM VW_DIR_ASIG_BUSCAR_TUTOR";
        List<Asignacion> lista = new ArrayList<>();
        try (Statement st = Conexion.getConnection().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Asignacion a = new Asignacion();
                a.idAsignacion = rs.getString("ID_ASIG_INTERNO");
                a.cedulaEstudiante = rs.getString("CEDULA_FILTRO");
                a.nombreEstudiante = rs.getString("NOMBRE");
                a.programaEstudiante = rs.getString("PROGRAMA");
                a.nombreInstitucion = rs.getString("INSTITUCION_RECEPTORA");
                lista.add(a);
            }
        }
        return lista;
    }

    /** Lista las asignaciones de un docente o asesor específico usando VW_DOC_MIS_ESTUDIANTES. */
    public static List<Asignacion> listarPorDocenteOAsesor(String cedula) throws SQLException {
        String sql = "SELECT NOMBRE_ESTUDIANTE, CEDULA_ESTUDIANTE, PROGRAMA, INSTITUCION_RECEPTORA, ESTADO_PRACTICA FROM VW_DOC_MIS_ESTUDIANTES WHERE CEDULA_DOCENTE = ? OR CEDULA_ASESOR = ?";
        List<Asignacion> lista = new ArrayList<>();
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, cedula); ps.setString(2, cedula);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Asignacion a = new Asignacion();
                a.nombreEstudiante = rs.getString("NOMBRE_ESTUDIANTE");
                a.cedulaEstudiante = rs.getString("CEDULA_ESTUDIANTE");
                a.programaEstudiante = rs.getString("PROGRAMA");
                a.nombreInstitucion = rs.getString("INSTITUCION_RECEPTORA");
                a.estadoAsignacion = rs.getString("ESTADO_PRACTICA");
                a.cedulaDocente = cedula;
                lista.add(a);
            }
        }
        return lista;
    }

    /** Busca una asignación por su ID (sin joins) — devuelve null si no existe. */
    public static Asignacion buscarPorId(String idAsignacion) throws SQLException {
        String sql = "SELECT ID_Asignacion, Cedula_Estudiante, ID_Institucion, Cedula_Docente, Cedula_Asesor, Periodo_Academico, Estado_Asignacion " +
                     "FROM Asignacion_Practica WHERE ID_Asignacion=?";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, idAsignacion);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Asignacion a = new Asignacion();
                a.idAsignacion     = rs.getString("ID_Asignacion");
                a.cedulaEstudiante = rs.getString("Cedula_Estudiante");
                a.idInstitucion    = rs.getString("ID_Institucion");
                a.cedulaDocente    = rs.getString("Cedula_Docente");
                a.cedulaAsesor     = rs.getString("Cedula_Asesor");
                a.periodoAcademico = rs.getString("Periodo_Academico");
                a.estadoAsignacion = rs.getString("Estado_Asignacion");
                return a;
            }
        }
        return null;
    }

    /** Elimina una asignación por su ID. */
    public static void eliminar(String idAsignacion) throws SQLException {
        String sql = "DELETE FROM Asignacion_Practica WHERE ID_Asignacion=?";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, idAsignacion);
            ps.executeUpdate();
            Conexion.getConnection().commit();
        }
    }
}
