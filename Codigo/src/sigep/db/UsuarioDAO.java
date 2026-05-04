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
        // Explicación: verifica credenciales y que el usuario esté en estado 'Activo'.
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

    /** Lista completa de usuarios */
    public static List<Usuario> listarTodos() throws SQLException {
        return listarConFiltro(null, null);
    }

    /** Lista usuarios por rol */
    public static List<Usuario> listarPorRol(String rol) throws SQLException {
        return listarConFiltro("Rol", rol);
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
        if (campo != null) sql += " WHERE u." + campo + " = ?";
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
        // Explicación: inserta un usuario asegurando que `Estado_Usuario` cumpla
        // con las restricciones CHECK de la tabla si existen.
        String sql = "INSERT INTO Usuario (Cedula_Usuario, ID_Programa, Nombre, Apellido, " +
                     "Correo, Contrasena, Rol, Semestre, Estado_Usuario, Habilitado_Asig) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        // Asegurar que el valor para Estado_Usuario cumple el CHECK de la tabla
        try {
            String q = "SELECT c.SEARCH_CONDITION FROM USER_CONSTRAINTS c " +
                       "JOIN USER_CONS_COLUMNS cc ON c.CONSTRAINT_NAME=cc.CONSTRAINT_NAME " +
                       "WHERE cc.TABLE_NAME=? AND cc.COLUMN_NAME=? AND c.CONSTRAINT_TYPE='C'";
            try (PreparedStatement ps = Conexion.getConnection().prepareStatement(q)) {
                ps.setString(1, "USUARIO"); ps.setString(2, "ESTADO_USUARIO");
                ResultSet rs = ps.executeQuery();
                java.util.List<String> allowed = new java.util.ArrayList<>();
                while (rs.next()) {
                    String cond = rs.getString(1);
                    if (cond == null) continue;
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("'([^']*)'").matcher(cond);
                    while (m.find()) allowed.add(m.group(1));
                }
                if (!allowed.isEmpty()) {
                    String prefer = "Pendiente";
                    if (allowed.contains(prefer)) u.estadoUsuario = prefer;
                    else if (allowed.contains("Pendiente por asignacion")) u.estadoUsuario = "Pendiente por asignacion";
                    else if (allowed.contains("Activo")) u.estadoUsuario = "Activo";
                    else u.estadoUsuario = allowed.get(0);
                } else {
                    if (u.estadoUsuario == null) u.estadoUsuario = "Activo";
                }
            }
        } catch (SQLException ex) {
            // no bloquear inserción por fallo al leer metadata; usar valor proporcionado o 'Activo'
            if (u.estadoUsuario == null) u.estadoUsuario = "Activo";
        }
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, u.cedula);
            ps.setString(2, u.idPrograma);
            ps.setString(3, u.nombre);
            ps.setString(4, u.apellido);
            ps.setString(5, u.correo);
            ps.setString(6, u.contrasena);
            ps.setString(7, u.rol);
            if (u.semestre != null) ps.setInt(8, u.semestre);
            else ps.setNull(8, Types.INTEGER);
            ps.setString(9, u.estadoUsuario != null ? u.estadoUsuario : "Activo");
            ps.setInt(10, u.habilitadoAsig);
            ps.executeUpdate();
            Conexion.getConnection().commit();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════════════

    public static void actualizar(Usuario u) throws SQLException {
        String sql = "UPDATE Usuario SET Nombre=?, Apellido=?, Correo=?, ID_Programa=?, " +
                     "Semestre=?, Estado_Usuario=? WHERE Cedula_Usuario=?";
        try (PreparedStatement ps = Conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, u.nombre);
            ps.setString(2, u.apellido);
            ps.setString(3, u.correo);
            ps.setString(4, u.idPrograma);
            if (u.semestre != null) ps.setInt(5, u.semestre);
            else ps.setNull(5, Types.INTEGER);
            ps.setString(6, u.estadoUsuario);
            ps.setString(7, u.cedula);
            ps.executeUpdate();
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
            ps.setInt(1, habilitado ? 1 : 0);
            ps.setString(2, cedula);
            ps.executeUpdate();
            Conexion.getConnection().commit();
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
