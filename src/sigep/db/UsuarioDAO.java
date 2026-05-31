package sigep.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la tabla USUARIO (T02).
 * Implementa todas las operaciones CRUD más consultas especializadas.
 */
public class UsuarioDAO {

    // ═══════════════════════════════════════════════════════════════════════
    // MODELO
    // ═══════════════════════════════════════════════════════════════════════
    // Clase modelo que representa una fila de la tabla Usuario.
    // Contiene campos simples y métodos utilitarios como `isActivo()`.
    public static class Usuario {
        public String cedula, idPrograma, nombre, apellido,
                      correo, contrasena, rol, estadoUsuario;
        public Integer semestre;
        public int habilitadoAsig;   // 0 = no, 1 = sí

        public String nombreCompleto() { return nombre + " " + apellido; }
        public boolean isActivo()      { return "Activo".equals(estadoUsuario); }
        public boolean isHabilitado()  { return habilitadoAsig == 1; }

        @Override public String toString() { return nombreCompleto() + " [" + cedula + "]"; }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LOGIN
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Autentica un usuario por cédula + contraseña.
     * Retorna el Usuario si las credenciales son correctas y está Activo,
     * null si no existe o la contraseña no coincide.
     */
    public static Usuario login(String cedula, String contrasena) throws SQLException {
        // Primero intentar usando la función FN_VERIFICAR_LOGIN (retorna Rol o 'INVALIDO')
        // Intentar usar FN_VERIFICAR_LOGIN; la función suele recibir correo y contraseña.
        try (CallableStatement cs = Conexion.getConnection().prepareCall("{ ? = call FN_VERIFICAR_LOGIN(?, ?) }")) {
            cs.registerOutParameter(1, Types.VARCHAR);
            cs.setString(2, cedula);
            cs.setString(3, contrasena);
            cs.execute();
            String rol = cs.getString(1);
            if (rol != null && !rol.equalsIgnoreCase("INVALIDO") && !rol.equalsIgnoreCase("ERROR")) {
                Usuario u = buscarPorCedula(cedula);
                if (u != null) { u.rol = rol; return u; }
            }
        } catch (SQLException ignored) {}
        // Si la llamada anterior falló o devolvió INVALIDO, intentar pasar el correo real
        try {
            Usuario byCed = buscarPorCedula(cedula);
            if (byCed != null && byCed.correo != null && !byCed.correo.isEmpty()) {
                try (CallableStatement cs2 = Conexion.getConnection().prepareCall("{ ? = call FN_VERIFICAR_LOGIN(?, ?) }")) {
                    cs2.registerOutParameter(1, Types.VARCHAR);
                    cs2.setString(2, byCed.correo);
                    cs2.setString(3, contrasena);
                    cs2.execute();
                    String rol2 = cs2.getString(1);
                    if (rol2 != null && !rol2.equalsIgnoreCase("INVALIDO") && !rol2.equalsIgnoreCase("ERROR")) {
                        byCed.rol = rol2; return byCed;
                    }
                } catch (SQLException ignored2) {}
            }
        } catch (SQLException ignored) {}

        // Fallback: verifica credenciales y que el usuario esté en estado 'Activo'.
        String sql = "SELECT * FROM Usuario WHERE Cedula_Usuario = ? AND Contrasena = ? AND Estado_Usuario = 'Activo'";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, cedula);
            ps.setString(2, contrasena);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // READ
    // ═══════════════════════════════════════════════════════════════════════

    /** Lista completa de usuarios desde la vista VW_DIR_LISTADO_USUARIOS */
    public static List<Usuario> listarTodos() throws SQLException {
        String sql = "SELECT CEDULA AS Cedula_Usuario, NOMBRE_COMPLETO AS Nombre, ROL AS Rol, " +
                     "PROGRAMA AS Nombre_Programa, SEMESTRE, ESTADO AS Estado_Usuario, " +
                     "FILTRO_ID_PROG AS ID_Programa " +
                     "FROM VW_DIR_LISTADO_USUARIOS";
        try (Statement st = Conexion.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            List<Usuario> lista = new ArrayList<>();
            while (rs.next()) {
                Usuario u = new Usuario();
                u.cedula = rs.getString("Cedula_Usuario");
                // El nombre completo ya viene en una sola columna
                String nombreCompleto = rs.getString("Nombre");
                // Hacemos un split simple para mantener el modelo separado
                int lastSpace = nombreCompleto.lastIndexOf(' ');
                if (lastSpace > 0) {
                    u.nombre = nombreCompleto.substring(0, lastSpace);
                    u.apellido = nombreCompleto.substring(lastSpace + 1);
                } else {
                    u.nombre = nombreCompleto;
                    u.apellido = "";
                }
                u.rol = rs.getString("Rol");
                u.estadoUsuario = rs.getString("Estado_Usuario");
                u.idPrograma = rs.getString("ID_Programa");
                try {
                    u.semestre = Integer.parseInt(rs.getString("SEMESTRE"));
                } catch (NumberFormatException e) {
                    u.semestre = null; // O manejar como prefieras si no es un número
                }
                lista.add(u);
            }
            return lista;
        }
    }

    /** Lista usuarios por rol usando la vista VW_DIR_LISTADO_USUARIOS */
    public static List<Usuario> listarPorRol(String rol) throws SQLException {
        String sql = "SELECT CEDULA AS Cedula_Usuario, NOMBRE_COMPLETO AS Nombre, ROL AS Rol, " +
                     "PROGRAMA AS Nombre_Programa, SEMESTRE, ESTADO AS Estado_Usuario, " +
                     "FILTRO_ID_PROG AS ID_Programa " +
                     "FROM VW_DIR_LISTADO_USUARIOS WHERE UPPER(TRIM(ROL)) = UPPER(TRIM(?))";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, rol);
            ResultSet rs = ps.executeQuery();
            List<Usuario> lista = new ArrayList<>();
            while (rs.next()) {
                Usuario u = new Usuario();
                u.cedula = rs.getString("Cedula_Usuario");
                String nombreCompleto = rs.getString("Nombre");
                int lastSpace = nombreCompleto.lastIndexOf(' ');
                if (lastSpace > 0) {
                    u.nombre = nombreCompleto.substring(0, lastSpace);
                    u.apellido = nombreCompleto.substring(lastSpace + 1);
                } else {
                    u.nombre = nombreCompleto;
                    u.apellido = "";
                }
                u.rol = rs.getString("Rol");
                u.estadoUsuario = rs.getString("Estado_Usuario");
                u.idPrograma = rs.getString("ID_Programa");
                 try {
                    u.semestre = Integer.parseInt(rs.getString("SEMESTRE"));
                } catch (NumberFormatException e) {
                    u.semestre = null;
                }
                lista.add(u);
            }
            return lista;
        }
    }

    /** Lista estudiantes habilitados para asignación */
    public static List<Usuario> listarEstudiantesHabilitados() throws SQLException {
        String sql = "SELECT * FROM Usuario WHERE Rol = 'Estudiante' AND Habilitado_Asig = 1 AND Estado_Usuario = 'Activo' ORDER BY Apellido";
        return ejecutarLista(sql);
    }

    /** Busca un usuario por cédula */
    public static Usuario buscarPorCedula(String cedula) throws SQLException {
        String sql = "SELECT * FROM Usuario WHERE Cedula_Usuario = ?";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, cedula);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    private static List<Usuario> listarConFiltro(String campo, String valor) throws SQLException {
        // Explicación: construcción dinámica de la consulta para listar usuarios
        // con un filtro opcional (p. ej. por rol) y obtener el nombre del programa.
        String sql = "SELECT u.*, p.Nombre_Programa FROM Usuario u " +
                 "LEFT JOIN Programa_Academico p ON u.ID_Programa = p.ID_Programa";
        if (campo != null) sql += " WHERE UPPER(TRIM(u." + campo + ")) = UPPER(TRIM(?))"; // case-insensitive, trim both sides
        sql += " ORDER BY u.Apellido, u.Nombre";

        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            if (campo != null) ps.setString(1, valor);
            ResultSet rs = ps.executeQuery();
            List<Usuario> lista = new ArrayList<>();
            while (rs.next()) lista.add(mapRow(rs));
            return lista;
        }
    }

    private static List<Usuario> ejecutarLista(String sql) throws SQLException {
           // Explicación: ejecuta una consulta estática y mapea cada fila a Usuario.
           try (Statement st = Conexion.getConnection().createStatement();
               ResultSet rs = st.executeQuery(sql)) {
            List<Usuario> lista = new ArrayList<>();
            while (rs.next()) lista.add(mapRow(rs));
            return lista;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CREATE
    // ═══════════════════════════════════════════════════════════════════════

    public static void insertar(Usuario u) throws SQLException {
        // Usar procedimiento almacenado SP_DIR_CREAR_USUARIO
        try (CallableStatement cs = Conexion.getConnection().prepareCall("{ call SP_DIR_CREAR_USUARIO(?, ?, ?, ?, ?, ?, ?, ?) }")) {
            cs.setString(1, u.cedula);
            cs.setString(2, u.idPrograma);
            cs.setString(3, u.nombre);
            cs.setString(4, u.apellido);
            cs.setString(5, u.correo);
            cs.setString(6, u.contrasena);
            cs.setString(7, u.rol);
            if (u.semestre != null) cs.setInt(8, u.semestre);
            else cs.setNull(8, Types.INTEGER);
            cs.execute();
            Conexion.getConnection().commit();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════════════

    public static void actualizar(Usuario u) throws SQLException {
        try (CallableStatement cs = Conexion.getConnection().prepareCall("{ call SP_DIR_MODIFICAR_USUARIO(?, ?, ?, ?, ?, ?, ?, ?) }")) {
            cs.setString(1, u.cedula);
            cs.setString(2, u.idPrograma);
            cs.setString(3, u.nombre);
            cs.setString(4, u.apellido);
            cs.setString(5, u.correo);
            cs.setString(6, u.rol);
            if (u.semestre != null) cs.setInt(7, u.semestre); else cs.setNull(7, Types.INTEGER);
            cs.setString(8, u.estadoUsuario);
            cs.execute();
            Conexion.getConnection().commit();
        }
    }

    /** Cambia el estado Activo/Inactivo */
    public static void cambiarEstado(String cedula, String nuevoEstado) throws SQLException {
        String sql = "UPDATE Usuario SET Estado_Usuario=? WHERE Cedula_Usuario=?";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setString(2, cedula);
            ps.executeUpdate();
            Conexion.getConnection().commit();
        }
    }

    /** Habilita o deshabilita la asignación del estudiante */
    public static void setHabilitado(String cedula, boolean habilitado) throws SQLException {
        String sql = "UPDATE Usuario SET Habilitado_Asig=? WHERE Cedula_Usuario=?";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            // read old value for audit
            Usuario old = buscarPorCedula(cedula);
            Integer oldVal = old == null ? null : old.habilitadoAsig;
            int newVal = habilitado ? 1 : 0;
            ps.setInt(1, newVal);
            ps.setString(2, cedula);
            ps.executeUpdate();
            Conexion.getConnection().commit();
            sigep.db.HabilitadoLogger.logChange(cedula, oldVal, newVal, "UsuarioDAO.setHabilitado");
        }
    }

    /**
     * Establece el estado de asignación en la columna `Habilitado_Asig`.
     * La tabla sólo acepta valores 0/1; por seguridad normalizamos cualquier
     * entero recibido: 0 -> 0, cualquier otro -> 1.
     */
    public static void setEstadoAsignacion(String cedula, int estado) throws SQLException {
        int val = sigep.db.StateUtils.clampHabilitado(estado);
        String sql = "UPDATE Usuario SET Habilitado_Asig=? WHERE Cedula_Usuario=?";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            Usuario old = buscarPorCedula(cedula);
            Integer oldVal = old == null ? null : old.habilitadoAsig;
            ps.setInt(1, val);
            ps.setString(2, cedula);
            ps.executeUpdate();
            Conexion.getConnection().commit();
            sigep.db.HabilitadoLogger.logChange(cedula, oldVal, val, "UsuarioDAO.setEstadoAsignacion");
        }
    }

    /** Cambia semestre del estudiante */
    public static void actualizarSemestre(String cedula, int semestre) throws SQLException {
        String sql = "UPDATE Usuario SET Semestre=? WHERE Cedula_Usuario=?";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setInt(1, semestre);
            ps.setString(2, cedula);
            ps.executeUpdate();
            Conexion.getConnection().commit();
        }
    }

    /** Elimina un usuario por cédula. Lanzará SQLException si existen restricciones de FK. */
    public static void eliminar(String cedula) throws SQLException {
        try (CallableStatement cs = Conexion.getConnection().prepareCall("{ call SP_DIR_ELIMINAR_USUARIO(?) }")) {
            cs.setString(1, cedula);
            cs.execute();
            Conexion.getConnection().commit();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MAPPER
    // ═══════════════════════════════════════════════════════════════════════

    private static Usuario mapRow(ResultSet rs) throws SQLException {
        // Explicación: mapea las columnas del ResultSet a los campos del modelo.
        Usuario u = new Usuario();
        u.cedula          = rs.getString("Cedula_Usuario");
        u.idPrograma      = rs.getString("ID_Programa");
        u.nombre          = rs.getString("Nombre");
        u.apellido        = rs.getString("Apellido");
        u.correo          = rs.getString("Correo");
        u.contrasena      = rs.getString("Contrasena");
        u.rol             = rs.getString("Rol");
        u.estadoUsuario   = rs.getString("Estado_Usuario");
        u.habilitadoAsig  = rs.getInt("Habilitado_Asig");
        int sem           = rs.getInt("Semestre");
        u.semestre        = rs.wasNull() ? null : sem;
        return u;
    }
}
