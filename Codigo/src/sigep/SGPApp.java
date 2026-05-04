package sigep;

import javax.swing.*;
import sigep.db.DB;

/**
 * SGP — Punto de entrada principal (renombrado desde SIGEPApp).
 */
// Uso: clase con `main` que inicia la aplicación, configura look&feel y
// muestra la ventana de login; prueba la conexión a la BD al arrancar.
public class SGPApp {

    public static void main(String[] args) {
        // Punto de entrada: configura look&feel, overrides UI y muestra login.
        // Intenta conectarse a la BD y ofrece MODO DEMO si falla.
        // Intentar apariencia del sistema para mejor rendering de fuentes
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | UnsupportedLookAndFeelException | IllegalAccessException | InstantiationException ignored) {}

        // Overrides globales
        UIManager.put("ScrollPane.border",          BorderFactory.createEmptyBorder());
        UIManager.put("Table.gridColor",             Theme.BORDER);
        UIManager.put("Table.selectionBackground",   Theme.PRIMARY_LIGHT);
        UIManager.put("TabbedPane.selected",         Theme.PRIMARY_LIGHT);
        UIManager.put("TabbedPane.background",       Theme.BG_PAGE);
        UIManager.put("TabbedPane.contentBorderInsets", new java.awt.Insets(0,0,0,0));
        UIManager.put("OptionPane.background",       Theme.BG_CARD);
        UIManager.put("Panel.background",            Theme.BG_CARD);

        SwingUtilities.invokeLater(() -> {
            // Probar conexión a Oracle al iniciar
            String testResult = DB.testConexion();
            boolean conectado = testResult.startsWith("✅");
            if (!conectado) {
                int opt = JOptionPane.showConfirmDialog(null,
                    "⚠️ No se pudo conectar a la base de datos Oracle:\n\n" +
                    testResult + "\n\n" +
                    "¿Desea iniciar en MODO DEMO (datos de prueba sin BD)?",
                    "Conexión fallida", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (opt != JOptionPane.YES_OPTION) System.exit(0);
            }

            // Cerrar BD al salir de la aplicación
            Runtime.getRuntime().addShutdownHook(new Thread(DB::cerrarConexion));

            showLogin();
        });
    }

    private static void showLogin() {
        // Crea la ventana de login y asigna acciones por rol para abrir portales.
        LoginWindow login = new LoginWindow();

        login.onDirector = () -> {
            login.dispose();
            try {
                PortalDirector portal = new PortalDirector();
                portal.setOnLogout(() -> { portal.dispose(); showLogin(); });
                portal.setVisible(true);
            } catch (Exception ex) {
                System.err.println("Error al abrir Portal del Director: " + ex.getMessage());
                JOptionPane.showMessageDialog(null,
                    "Ocurrió un error al abrir el Portal del Director:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
                showLogin();
            }
        };

        login.onStudent = () -> {
            login.dispose();
            PortalEstudiante portal = new PortalEstudiante();
            portal.setOnLogout(() -> { portal.dispose(); showLogin(); });
            portal.setVisible(true);
        };

        login.onDocente = () -> {
            login.dispose();
            PortalEvaluador portal = new PortalEvaluador("Docente Evaluador");
            portal.setOnLogout(() -> { portal.dispose(); showLogin(); });
            portal.setVisible(true);
        };

        login.onAsesor = () -> {
            login.dispose();
            PortalEvaluador portal = new PortalEvaluador("Asesor");
            portal.setOnLogout(() -> { portal.dispose(); showLogin(); });
            portal.setVisible(true);
        };

        login.setVisible(true);
    }
}
