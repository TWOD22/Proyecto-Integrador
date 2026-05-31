package sigep.db;

import java.sql.*;

/**
 * Generador de IDs progresivos para el sistema SGP.
 * Genera IDs con formato: PREFIX-NN (ej: INST-01, DOC-01, etc)
 */
public class IDGenerator {

    /**
     * Genera el siguiente ID progresivo para una entidad
     * @param prefix Prefijo del ID (ej: "INST", "DOC")
     * @return ID generado con formato "PREFIX-NN"
     */
    public static String generarID(String prefix) throws SQLException {
        // Mapear prefijo a columna y tabla reales
        // Explicación: el método consulta el MAX(id) en la tabla correspondiente
        // y genera el siguiente número secuencial con formato de 2 dígitos.
        String column, table;
        switch (prefix) {
            case "INST": column = "ID_Institucion"; table = "Institucion_Receptora"; break;
            case "DOC":  column = "ID_Documento";  table = "Documentos"; break;
            case "SEL":  column = "ID_Seleccion";  table = "Seleccion_Institucion"; break;
            case "ASIG": column = "ID_Asignacion"; table = "Asignacion_Practica"; break;
            default: throw new IllegalArgumentException("Prefijo desconocido: " + prefix);
        }

        String sql = "SELECT MAX(" + column + ") FROM " + table;
        try (Statement st = Conexion.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                String ultimoId = rs.getString(1);
                if (ultimoId == null || ultimoId.isEmpty()) return prefix + "-01";
                String[] partes = ultimoId.split("-");
                if (partes.length >= 2) {
                    String numPart = partes[partes.length - 1];
                    try {
                        int numero = Integer.parseInt(numPart);
                        return String.format("%s-%02d", prefix, numero + 1);
                    } catch (NumberFormatException e) {
                        return prefix + "-01";
                    }
                }
            }
        }
        return prefix + "-01";
    }
}
