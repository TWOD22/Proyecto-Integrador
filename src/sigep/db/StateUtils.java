package sigep.db;

/**
 * Utilidades para normalizar y mapear estados relacionados con asignaciones
 * y la columna `Usuario.Habilitado_Asig`.
 * Centraliza la lógica para evitar discrepancias y violaciones de constraint.
 */
public final class StateUtils {
    private StateUtils() {}

    /** Clamp any requested habilitado int to 0 or 1 (DB constraint expects 0/1). */
    /**
     * Whether the database accepts 3-state values (0/1/2). Default false.
     * Set to true after running the migration scripts.
     */
    public static boolean DB_SUPPORTS_THREE = false;

    /**
     * Intenta detectar dinámicamente si la columna Usuario.Habilitado_Asig
     * admite el valor '2' mediante la cláusula CHECK en la definición de la tabla.
     * Si detecta el literal '2' en un CHECK IN(...), activa `DB_SUPPORTS_THREE`.
     */
    public static void detectThreeStateSupport() {
        try {
            java.sql.Connection c = Conexion.getConnection();
            String q = "SELECT c.SEARCH_CONDITION FROM USER_CONSTRAINTS c " +
                       "JOIN USER_CONS_COLUMNS cc ON c.CONSTRAINT_NAME=cc.CONSTRAINT_NAME " +
                       "WHERE cc.TABLE_NAME=? AND cc.COLUMN_NAME=? AND c.CONSTRAINT_TYPE='C'";
            try (java.sql.PreparedStatement ps = c.prepareStatement(q)) {
                ps.setString(1, "USUARIO"); ps.setString(2, "HABILITADO_ASIG");
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String cond = rs.getString(1);
                        if (cond != null && cond.contains("'2'")) { DB_SUPPORTS_THREE = true; return; }
                    }
                }
            }
        } catch (java.sql.SQLException ignored) {
            // No cambiar el flag si ocurre un error de detección
        }
    }

    /** Clamp any requested habilitado int to the allowed values.
     *  If `DB_SUPPORTS_THREE` is false, the result will be 0 or 1 only.
     */
    public static int clampHabilitado(int requested) {
        if (requested == 0) return 0;
        if (requested == 1) return 1;
        // requested >=2 -> choose 2 only if DB supports it
        return DB_SUPPORTS_THREE ? 2 : 1;
    }

    /**
     * Map an assignment-state string to the `Habilitado_Asig` integer (0/1).
     * - Pending/espera-like -> 0
     * - Any active/practica/final-like -> 1
     */
    public static int habilitadoFromEstado(String estado) {
        if (estado == null) return 1;
        String lower = estado.toLowerCase();
        if (lower.contains("pend") || lower.contains("esper")) return 0;
        // In-practice / active / final => 2 if DB supports 3-state, otherwise 1
        if (lower.contains("pract") || lower.contains("práct") || lower.contains("act") || lower.contains("activ") || lower.contains("curso") || lower.contains("final"))
            return DB_SUPPORTS_THREE ? 2 : 1;
        // default permissive -> 1
        return 1;
    }

    /** Normalize a estado string for consistency (optional simple mapping). */
    public static String normalizeEstadoAsignacion(String estado) {
        if (estado == null) return "Pendiente";
        String lower = estado.toLowerCase();
        if (lower.contains("pend")) return "Pendiente";
        if (lower.contains("esper")) return "Esperando";
        if (lower.contains("pract") || lower.contains("práct") || lower.contains("act") || lower.contains("activ") || lower.contains("curso")) return "En práctica";
        if (lower.contains("final")) return "Finalizada";
        return estado;
    }
}
