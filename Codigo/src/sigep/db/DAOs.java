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
        return query("SELECT * FROM Institucion_Receptora ORDER BY Nombre_Institucion");
    }

    // Devuelve solo instituciones con convenio vigente y cupos disponibles.
    public static List<Institucion> listarVigentes() throws SQLException {
        return query("SELECT * FROM Institucion_Receptora WHERE Estado_Convenio='Vigente' AND Cupos_Disp>0 ORDER BY Nombre_Institucion");
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
        String sql = "INSERT INTO Institucion_Receptora VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            // Generar ID progresivo si no existe
            if (i.idInstitucion == null || i.idInstitucion.isEmpty()) {
                i.idInstitucion = IDGenerator.generarID("INST");
            }
            ps.setString(1, i.idInstitucion); ps.setString(2, i.nit);
            ps.setString(3, i.nombre);        ps.setString(4, i.direccion);
            ps.setString(5, i.estadoConvenio);ps.setInt   (6, i.cuposTotales);
            ps.setInt   (7, i.cuposDisp);     ps.setString(8, i.telefono);
            ps.setString(9, i.correo);
            ps.executeUpdate(); Conexion.getConnection().commit();
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

    /**
     * Devuelve la lista de literales permitidos en la cláusula CHECK para
     * la columna dada en la tabla (p. ej. DOCUMENTOS, ESTADO_DOC) si existe
     * un CHECK con un IN(...). Retorna lista vacía si no se encuentra.
     */
    // Lee los literales permitidos por un CHECK constraint tipo IN(...) para
    // ayudar a elegir valores por defecto válidos (p. ej. Estado_Doc).
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
                // Extraer literales entre comillas simples
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("'([^']*)'").matcher(cond);
                while (m.find()) out.add(m.group(1));
            }
        }
        return out;
    }

    // Lista documentos asociados a una cédula, ordenados por fecha (más reciente primero).
    public static List<Documento> listarPorEstudiante(String cedula) throws SQLException {
        String sql = "SELECT ID_Documento, Cedula_Estudiante, Tipo_Documento, " +
                     "Estado_Doc, Nombre_Archivo, Fecha_Carga " +
                     "FROM Documentos WHERE Cedula_Estudiante=? ORDER BY Fecha_Carga DESC";
        List<Documento> lista = new ArrayList<>();
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, cedula);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(map(rs));
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
            String sql = "INSERT INTO Documentos (ID_Documento, Cedula_Estudiante, Tipo_Documento, " +
                         "Estado_Doc, Nombre_Archivo, Contenido_Archivo, Fecha_Carga) " +
                         "VALUES (?,?,?,?,?,?,SYSDATE)";
            // Asegurar que Estado_Doc cumple el CHECK: si existe un CHECK con literales, usar el primero como default
            try {
                List<String> allowed = obtenerValoresCheck("DOCUMENTOS", "ESTADO_DOC");
                if (!allowed.isEmpty()) {
                    if (d.estadoDoc == null || !allowed.contains(d.estadoDoc)) d.estadoDoc = allowed.get(0);
                } else {
                    if (d.estadoDoc == null) d.estadoDoc = "En Revisión";
                }
            } catch (SQLException ex) { if (d.estadoDoc == null) d.estadoDoc = "En Revisión"; }

                try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
                String id = d.idDocumento == null ? IDGenerator.generarID("DOC") : d.idDocumento;
                d.idDocumento = id;
                ps.setString(1, id);
                ps.setString(2, d.cedulaEstudiante);
                ps.setString(3, d.tipoDocumento);
                ps.setString(4, d.estadoDoc != null ? d.estadoDoc : "En Revisión");
                ps.setString(5, d.nombreArchivo);
                if (d.contenidoBlob != null) ps.setBytes(6, d.contenidoBlob);
                else ps.setNull(6, java.sql.Types.BLOB);
                ps.executeUpdate(); Conexion.getConnection().commit();
                System.err.println("[DocumentoDAO] inserted BLOB doc id="+id+" cedula="+d.cedulaEstudiante+" nombre="+d.nombreArchivo);
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
        // Verificar si existe la columna Nombre_Archivo
        boolean hasNombre = false;
        try (PreparedStatement psChk = Conexion.getConnection().prepareStatement(
                "SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE TABLE_NAME='DOCUMENTOS' AND COLUMN_NAME=?")) {
            psChk.setString(1, "NOMBRE_ARCHIVO");
            ResultSet rs = psChk.executeQuery(); if (rs.next()) hasNombre = rs.getInt(1) > 0;
        }
        if (hasNombre) {
            String sql = "INSERT INTO Documentos (ID_Documento, Cedula_Estudiante, Tipo_Documento, " +
                         "Estado_Doc, Nombre_Archivo, Fecha_Carga) " +
                         "VALUES (?,?,?,?,?,SYSDATE)";
            // Asegurar estado válido según CHECK
            try {
                List<String> allowed = obtenerValoresCheck("DOCUMENTOS", "ESTADO_DOC");
                if (!allowed.isEmpty()) {
                    if (d.estadoDoc == null || !allowed.contains(d.estadoDoc)) d.estadoDoc = allowed.get(0);
                } else {
                    if (d.estadoDoc == null) d.estadoDoc = "En Revisión";
                }
            } catch (SQLException ex) { if (d.estadoDoc == null) d.estadoDoc = "En Revisión"; }

            try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
                ps.setString(1, IDGenerator.generarID("DOC"));
                ps.setString(2, d.cedulaEstudiante);
                ps.setString(3, d.tipoDocumento);
                ps.setString(4, d.estadoDoc != null ? d.estadoDoc : "En Revisión");
                ps.setString(5, d.nombreArchivo != null ? d.nombreArchivo : "");
                ps.executeUpdate(); Conexion.getConnection().commit();
            }
        } else {
            String sql = "INSERT INTO Documentos (ID_Documento, Cedula_Estudiante, Tipo_Documento, Estado_Doc, Fecha_Carga) " +
                         "VALUES (?,?,?,?,SYSDATE)";
            try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
                ps.setString(1, IDGenerator.generarID("DOC"));
                ps.setString(2, d.cedulaEstudiante);
                ps.setString(3, d.tipoDocumento);
                ps.setString(4, d.estadoDoc != null ? d.estadoDoc : "En Revisión");
                ps.executeUpdate(); Conexion.getConnection().commit();
            }
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
        String sql = "UPDATE Documentos SET Estado_Doc=? WHERE ID_Documento=?";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, nuevoEstado); ps.setString(2, idDocumento);
            ps.executeUpdate(); Conexion.getConnection().commit();
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
    public static List<Seleccion> listarPendientes() throws SQLException {
        String sql = "SELECT s.*, u.Nombre||' '||u.Apellido AS nombreEst, " +
                     "i1.Nombre_Institucion AS n1, i2.Nombre_Institucion AS n2 " +
                     "FROM Seleccion_Institucion s " +
                     "JOIN Usuario u ON s.Cedula_Estudiante = u.Cedula_Usuario " +
                     "JOIN Institucion_Receptora i1 ON s.ID_Inst_Op1 = i1.ID_Institucion " +
                     "JOIN Institucion_Receptora i2 ON s.ID_Inst_Op2 = i2.ID_Institucion " +
                     "WHERE s.Estado_Seleccion='Pendiente'";
        List<Seleccion> lista = new ArrayList<>();
        try (Statement st = Conexion.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Seleccion s = map(rs);
                s.nombreEstudiante = rs.getString("nombreEst");
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

    // Actualiza el estado de una selección (p. ej. 'Pendiente' -> 'Procesada').
    public static void actualizarEstado(String idSeleccion, String estado) throws SQLException {
        String sql = "UPDATE Seleccion_Institucion SET Estado_Seleccion=? WHERE ID_Seleccion=?";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, estado); ps.setString(2, idSeleccion);
            ps.executeUpdate(); Conexion.getConnection().commit();
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
        String sql = buildSelectJoin("a.Estado_Asignacion='Activa'");
        return ejecutarLista(sql, null);
    }

    // Lista asignaciones para un estudiante específico.
    public static List<Asignacion> listarPorEstudiante(String cedula) throws SQLException {
        String sql = buildSelectJoin("a.Cedula_Estudiante=?");
        return ejecutarLista(sql, cedula);
    }

    // Inserta una nueva asignación en la tabla Asignacion_Practica.
    public static void insertar(Asignacion a) throws SQLException {
        String sql = "INSERT INTO Asignacion_Practica VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, "ASIG-" + System.currentTimeMillis() % 100000);
            ps.setString(2, a.cedulaEstudiante);
            ps.setString(3, a.idInstitucion);
            ps.setString(4, a.cedulaDocente);
            ps.setString(5, a.cedulaAsesor);
            ps.setString(6, a.periodoAcademico);
            ps.setString(7, a.estadoAsignacion != null ? a.estadoAsignacion : "Activa");
            ps.executeUpdate(); Conexion.getConnection().commit();
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
            "JOIN Usuario doc  ON a.Cedula_Docente    = doc.Cedula_Usuario " +
            "JOIN Usuario ase  ON a.Cedula_Asesor     = ase.Cedula_Usuario " +
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
}
