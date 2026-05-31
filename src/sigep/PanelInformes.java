package sigep;

import java.awt.*;
import java.sql.*;
import javax.swing.*;
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.data.category.*;
import org.jfree.data.general.*;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.ui.RectangleInsets;
import sigep.db.Conexion;

/**
 * PanelInformes — panel con 3 reportes: (1) barras por institución,
 * (2) torta por estado filtrada por semestre, (3) barras horizontales carga docente/asesor.
 */
public class PanelInformes extends JPanel {

    private JComboBox<String> semestreBox;
    private ChartPanel chartInstituciones;
    private ChartPanel chartTorta;
    private ChartPanel chartCarga;
    private ChartPanel mainChartPanel;
    private JComboBox<String> chartSelector;

    public PanelInformes() {
        setLayout(new BorderLayout(8,8));
        setBorder(BorderFactory.createEmptyBorder(12,12,12,12));

        // Top: controles
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        top.add(new JLabel("Semestre:"));
        semestreBox = new JComboBox<>();
        top.add(semestreBox);
        add(top, BorderLayout.NORTH);

        // Center: selector arriba + un chart grande
        JPanel centerWrap = new JPanel(new BorderLayout(8,8));
        // Selector de gráficos
        JPanel selectorRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        chartSelector = new JComboBox<>(new String[]{"Instituciones", "Estado (Torta)", "Carga"});
        selectorRow.add(new JLabel("Ver:"));
        selectorRow.add(chartSelector);
        centerWrap.add(selectorRow, BorderLayout.NORTH);

        // Main chart area
        mainChartPanel = new ChartPanel(null);
        centerWrap.add(mainChartPanel, BorderLayout.CENTER);
        add(centerWrap, BorderLayout.CENTER);

        // Inicializar datos y listeners
        loadSemesters();
        buildAllCharts();

        // Listeners
        semestreBox.addActionListener(e -> {
            String sel = (String) semestreBox.getSelectedItem();
            if (sel != null && !sel.isEmpty()) updatePieChart(sel);
        });

        chartSelector.addActionListener(e -> {
            String sel = (String) chartSelector.getSelectedItem();
            showSelectedChart(sel);
        });
    }

    // Carga semestres disponibles en el combo
    private void loadSemesters() {
        SwingUtilities.invokeLater(() -> {
            semestreBox.removeAllItems();
            semestreBox.addItem("Todos");
            String sql = "SELECT DISTINCT Semestre FROM Usuario WHERE Semestre IS NOT NULL ORDER BY Semestre";
            try (Connection c = Conexion.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    semestreBox.addItem(String.valueOf(rs.getString(1)));
                }
            } catch (Exception ex) {
                System.err.println("Error loadSemesters: " + ex.getMessage());
            }
        });
    }

    // Construye/actualiza los tres charts
    private void buildAllCharts() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Create charts and set the main chart to instituciones by default
                JFreeChart chart1 = createInstitucionesChart(fetchInstitucionesDataset());
                chartInstituciones = new ChartPanel(chart1);

                String initialSem = semestreBox.getItemCount() > 1 ? (String) semestreBox.getItemAt(1) : "Todos";
                if (initialSem == null) initialSem = "Todos";
                JFreeChart pie = createPieChart(fetchPieDatasetForSemester(initialSem), initialSem);
                chartTorta = new ChartPanel(pie);

                JFreeChart chart3 = createCargaChart(fetchCargaDataset());
                chartCarga = new ChartPanel(chart3);

                // Set default visible chart
                mainChartPanel.setChart(chart1);
            } catch (Exception ex) {
                System.err.println("Error building charts: " + ex.getMessage());
            }
        });
    }

    // ---------- Chart creators ----------

    private JFreeChart createInstitucionesChart(CategoryDataset dataset) {
        JFreeChart chart = ChartFactory.createBarChart(
            "Estudiantes por Institución",
            "Institución",
            "Estudiantes",
            dataset,
            PlotOrientation.VERTICAL,
            false, true, false);
        chart.setPadding(new RectangleInsets(6,6,6,6));
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setMaximumBarWidth(0.08);
        return chart;
    }

    private JFreeChart createPieChart(PieDataset dataset, String semestreLabel) {
        JFreeChart chart = ChartFactory.createPieChart(
            "Estado de asignaciones — Semestre: " + semestreLabel,
            dataset,
            true, true, false);
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setLabelGenerator(new org.jfree.chart.labels.StandardPieSectionLabelGenerator("{0}: {1} ({2})"));
        return chart;
    }

    private JFreeChart createCargaChart(CategoryDataset dataset) {
        JFreeChart chart = ChartFactory.createBarChart(
            "Carga: Docentes vs Asesores",
            "Carga (nº estudiantes)",
            "Persona",
            dataset,
            PlotOrientation.HORIZONTAL,
            true, true, false);
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setRangeGridlinesVisible(true);
        return chart;
    }

    // ---------- Data fetchers (JDBC) ----------

    private CategoryDataset fetchInstitucionesDataset() {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        // Filtrar por semestre si está seleccionado
        String semestre = null;
        try { semestre = (String) semestreBox.getSelectedItem(); } catch (Exception ignored) {}
        boolean all = semestre == null || semestre.isEmpty() || "Todos".equalsIgnoreCase(semestre);
        String sql = "SELECT i.Nombre_Institucion, COUNT(DISTINCT ap.Cedula_Estudiante) AS num_estudiantes " +
                     "FROM Asignacion_Practica ap JOIN Institucion_Receptora i ON ap.ID_Institucion = i.ID_Institucion " +
                     "JOIN Usuario u ON ap.Cedula_Estudiante = u.Cedula_Usuario ";
        if (!all) sql += "WHERE u.Semestre = ? ";
        sql += "GROUP BY i.Nombre_Institucion ORDER BY num_estudiantes DESC";
        try (Connection c = Conexion.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (!all) ps.setString(1, semestre);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ds.addValue(rs.getInt("num_estudiantes"), "Estudiantes", rs.getString("Nombre_Institucion"));
                }
            }
        } catch (Exception ex) { System.err.println("fetchInstitucionesDataset: " + ex.getMessage()); }
        return ds;
    }

    private PieDataset fetchPieDatasetForSemester(String semestre) {
        DefaultPieDataset ds = new DefaultPieDataset();
        boolean all = semestre == null || semestre.isEmpty() || "Todos".equalsIgnoreCase(semestre);
        // We'll compute five mutually exclusive categories using SQL counts:
        // 1) Sin documentos
        // 2) Documentos subidos y habilitado para escoger, pero sin seleccionar plaza ni asignación
        // 3) Con plaza escogida (selección) pero sin asignación
        // 4) Con plaza escogida y asignación pero falta docente/asesor
        // 5) Con docente+asesor y en práctica (completos)
        String semFilter = all ? "" : " AND u.Semestre = ? ";
        String sql1 = "SELECT COUNT(DISTINCT u.Cedula_Usuario) AS cnt FROM Usuario u WHERE u.Rol='Estudiante' AND NOT EXISTS (SELECT 1 FROM Documentos d WHERE d.Cedula_Estudiante = u.Cedula_Usuario)" + (all ? "" : " AND u.Semestre = ?");
        String sql2 = "SELECT COUNT(DISTINCT u.Cedula_Usuario) AS cnt FROM Usuario u WHERE u.Rol='Estudiante' AND NVL(u.Habilitado_Asig,0) >= 1 AND NOT EXISTS (SELECT 1 FROM Seleccion_Institucion s WHERE s.Cedula_Estudiante = u.Cedula_Usuario) AND NOT EXISTS (SELECT 1 FROM Asignacion_Practica a WHERE a.Cedula_Estudiante = u.Cedula_Usuario)" + (all ? "" : " AND u.Semestre = ?");
        String sql3 = "SELECT COUNT(DISTINCT s.Cedula_Estudiante) AS cnt FROM Seleccion_Institucion s JOIN Usuario u ON s.Cedula_Estudiante = u.Cedula_Usuario WHERE NOT EXISTS (SELECT 1 FROM Asignacion_Practica a WHERE a.Cedula_Estudiante = s.Cedula_Estudiante)" + (all ? "" : " AND u.Semestre = ?");
        String sql4 = "SELECT COUNT(DISTINCT a.Cedula_Estudiante) AS cnt FROM Asignacion_Practica a JOIN Usuario u ON a.Cedula_Estudiante = u.Cedula_Usuario WHERE (a.Cedula_Docente IS NULL OR a.Cedula_Asesor IS NULL)" + (all ? "" : " AND u.Semestre = ?");
        String sql5 = "SELECT COUNT(DISTINCT a.Cedula_Estudiante) AS cnt FROM Asignacion_Practica a JOIN Usuario u ON a.Cedula_Estudiante = u.Cedula_Usuario WHERE a.Cedula_Docente IS NOT NULL AND a.Cedula_Asesor IS NOT NULL" + (all ? "" : " AND u.Semestre = ?");
        try (Connection c = Conexion.getConnection()) {
            try (PreparedStatement p1 = c.prepareStatement(sql1)) {
                if (!all) p1.setString(1, semestre);
                try (ResultSet r = p1.executeQuery()) { if (r.next()) ds.setValue("Sin documentos", r.getInt("cnt")); }
            }
            try (PreparedStatement p2 = c.prepareStatement(sql2)) {
                if (!all) p2.setString(1, semestre);
                try (ResultSet r = p2.executeQuery()) { if (r.next()) ds.setValue("Listos para escoger (sin plaza)", r.getInt("cnt")); }
            }
            try (PreparedStatement p3 = c.prepareStatement(sql3)) {
                if (!all) p3.setString(1, semestre);
                try (ResultSet r = p3.executeQuery()) { if (r.next()) ds.setValue("Plaza escogida (sin asignación)", r.getInt("cnt")); }
            }
            try (PreparedStatement p4 = c.prepareStatement(sql4)) {
                if (!all) p4.setString(1, semestre);
                try (ResultSet r = p4.executeQuery()) { if (r.next()) ds.setValue("Plaza con asignación (falta docente/asesor)", r.getInt("cnt")); }
            }
            try (PreparedStatement p5 = c.prepareStatement(sql5)) {
                if (!all) p5.setString(1, semestre);
                try (ResultSet r = p5.executeQuery()) { if (r.next()) ds.setValue("Asignados y en práctica", r.getInt("cnt")); }
            }
        } catch (Exception ex) { System.err.println("fetchPieDatasetForSemester(sem): " + ex.getMessage()); }
        return ds;
    }

    private CategoryDataset fetchCargaDataset() {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        String sqlDoc = "SELECT u.Nombre || ' ' || u.Apellido AS nombre, COUNT(DISTINCT ap.Cedula_Estudiante) AS carga " +
                        "FROM Usuario u LEFT JOIN Asignacion_Practica ap ON u.Cedula_Usuario = ap.Cedula_Docente " +
                        "WHERE u.Rol = 'Docente' GROUP BY u.Nombre, u.Apellido";
        String sqlAse = "SELECT u.Nombre || ' ' || u.Apellido AS nombre, COUNT(DISTINCT ap.Cedula_Estudiante) AS carga " +
                        "FROM Usuario u LEFT JOIN Asignacion_Practica ap ON u.Cedula_Usuario = ap.Cedula_Asesor " +
                        "WHERE u.Rol = 'Asesor' GROUP BY u.Nombre, u.Apellido";
        try (Connection c = Conexion.getConnection();
             PreparedStatement ps1 = c.prepareStatement(sqlDoc);
             ResultSet rs1 = ps1.executeQuery()) {
            while (rs1.next()) {
                ds.addValue(rs1.getInt("carga"), "Docente", rs1.getString("nombre"));
            }
        } catch (Exception ex) { System.err.println("fetchCargaDataset(doc): " + ex.getMessage()); }
        try (Connection c = Conexion.getConnection();
             PreparedStatement ps2 = c.prepareStatement(sqlAse);
             ResultSet rs2 = ps2.executeQuery()) {
            while (rs2.next()) {
                ds.addValue(rs2.getInt("carga"), "Asesor", rs2.getString("nombre"));
            }
        } catch (Exception ex) { System.err.println("fetchCargaDataset(ase): " + ex.getMessage()); }
        return ds;
    }

    // ---------- Updaters ----------

    private void updatePieChart(String semestre) {
        SwingUtilities.invokeLater(() -> {
            PieDataset ds = fetchPieDatasetForSemester(semestre);
            JFreeChart chart = createPieChart(ds, semestre == null ? "Todos" : semestre);
            if (chartTorta == null) chartTorta = new ChartPanel(chart);
            else chartTorta.setChart(chart);
            // If user currently views the pie, update main chart
            String sel = chartSelector == null ? "Instituciones" : (String) chartSelector.getSelectedItem();
            if (sel != null && sel.startsWith("Estado")) mainChartPanel.setChart(chart);
        });
    }

    // Exposición pública para forzar refresh completo (UI botón externo)
    public void refreshAll() {
        SwingUtilities.invokeLater(() -> {
            JFreeChart c1 = createInstitucionesChart(fetchInstitucionesDataset());
            JFreeChart cPie = createPieChart(fetchPieDatasetForSemester((String) semestreBox.getSelectedItem()), (String) semestreBox.getSelectedItem());
            JFreeChart cCarga = createCargaChart(fetchCargaDataset());
            if (chartInstituciones == null) chartInstituciones = new ChartPanel(c1); else chartInstituciones.setChart(c1);
            if (chartTorta == null) chartTorta = new ChartPanel(cPie); else chartTorta.setChart(cPie);
            if (chartCarga == null) chartCarga = new ChartPanel(cCarga); else chartCarga.setChart(cCarga);
            String sel = chartSelector == null ? "Instituciones" : (String) chartSelector.getSelectedItem();
            showSelectedChart(sel);
        });
    }

    private void showSelectedChart(String sel) {
        if (sel == null) sel = "Instituciones";
        switch (sel) {
            case "Carga":
                if (chartCarga != null) mainChartPanel.setChart(chartCarga.getChart());
                break;
            case "Estado (Torta)":
                if (chartTorta != null) mainChartPanel.setChart(chartTorta.getChart());
                break;
            default:
                if (chartInstituciones != null) mainChartPanel.setChart(chartInstituciones.getChart());
        }
    }
}
