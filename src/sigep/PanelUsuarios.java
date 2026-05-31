package sigep;

import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import sigep.db.DB;
import sigep.db.UsuarioDAO;

/**
 * Gestión de Usuarios — conectado a Oracle (tabla USUARIO / T02)
 */
// Uso: panel para crear, editar, listar y filtrar usuarios del sistema.
public class PanelUsuarios extends JPanel {

    private DefaultTableModel tableModel;
    private JTable table;
    private JPanel formPanel;
    private boolean showForm = false;
    private JButton toggleFormBtn;

    private JTextField     fCedula, fNombre, fApellido, fCorreo;
    private JPasswordField fPass;
    private JComboBox<String> fRol, fSemestre;
    private JComboBox<Object> fPrograma;
    // Controles de filtro en la vista
    private JTextField searchField;
    private JComboBox<String> filterRol;
    private JComboBox<Object> filterPrograma;
    private JComboBox<String> filterSemestre;
    private JComboBox<String> filterEstado;

    Consumer<UsuarioDAO.Usuario> onVerPerfil;

    public PanelUsuarios() {
        // Constructor: inicializa el panel y construye la UI principal.
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));
        build();
    }

    private void build() {
        // Monta la estructura principal: header, action bar, formulario y tabla.
        add(buildSectionHeader(), BorderLayout.NORTH);
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(UIFactory.gap(16));
        center.add(buildActionBar());
        center.add(UIFactory.gap(10));
        formPanel = buildForm();
        formPanel.setVisible(false);
        center.add(formPanel);
        center.add(UIFactory.gap(6));
        center.add(buildTableCard());
        add(center, BorderLayout.CENTER);
    }

    private JPanel buildSectionHeader() {
        // Construye el header de la sección con icono y título.
        JPanel h = UIFactory.transparent(new BorderLayout());
        h.setBorder(new EmptyBorder(0, 0, 12, 0));
        JPanel left = UIFactory.transparent(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JLabel icon = new JLabel("👥"); icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        left.add(icon); left.add(UIFactory.h2("Gestión de Usuarios"));
        h.add(left, BorderLayout.WEST);
        JPanel w = UIFactory.transparent(new BorderLayout());
        w.add(h, BorderLayout.NORTH); w.add(UIFactory.hSep(), BorderLayout.SOUTH);
        return w;
    }

    private JPanel buildActionBar() {
        // Construye la barra de acciones (buscar + crear usuario).
        JPanel bar = UIFactory.actionBar();
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));

        // Search
        searchField = UIFactory.searchField("Buscar por cédula o nombre...");
        searchField.setPreferredSize(new Dimension(260, 34));
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { filtrarTabla(searchField.getText().trim()); }
        });

        // Filtros: Rol, Programa, Semestre, Estado
        filterRol = new JComboBox<>(new String[]{"Todos", "Estudiante", "Docente", "Asesor", "Director"});
        filterRol.setPreferredSize(new Dimension(140, 30));
        filterRol.addActionListener(e -> filtrarTabla(searchField.getText().trim()));

        filterPrograma = new JComboBox<>();
        filterPrograma.setPreferredSize(new Dimension(220, 30));
        filterPrograma.addItem("Todos");
        for (DB.Programa p : DB.listarProgramas()) filterPrograma.addItem(p);
        filterPrograma.addActionListener(e -> filtrarTabla(searchField.getText().trim()));

        filterSemestre = new JComboBox<>(new String[]{"Todos","1","2","3","4","5","6","7","8"});
        filterSemestre.setPreferredSize(new Dimension(90, 30));
        filterSemestre.addActionListener(e -> filtrarTabla(searchField.getText().trim()));

        java.util.List<String> allowed = DB.allowedValues("USUARIO", "ESTADO_USUARIO");
        java.util.Vector<String> estados = new java.util.Vector<>();
        estados.add("Todos");
        if (allowed.isEmpty()) { estados.add("Activo"); estados.add("Inactivo"); estados.add("Pendiente"); }
        else { for (String s : allowed) estados.add(s); }
        filterEstado = new JComboBox<>(estados);
        filterEstado.setPreferredSize(new Dimension(140, 30));
        filterEstado.addActionListener(e -> filtrarTabla(searchField.getText().trim()));

        JPanel left = UIFactory.transparent(new FlowLayout(FlowLayout.LEFT, 8, 8));
        left.add(searchField);
        left.add(filterRol);
        left.add(filterPrograma);
        left.add(filterSemestre);
        left.add(filterEstado);
        bar.add(left, BorderLayout.WEST);

        toggleFormBtn = UIFactory.primaryBtn("＋  Nuevo Usuario");
        toggleFormBtn.addActionListener(e -> toggleForm());
        bar.add(toggleFormBtn, BorderLayout.EAST);
        return bar;
    }

    private void toggleForm() {
        // Muestra/oculta el formulario para crear/editar usuarios.
        showForm = !showForm;
        formPanel.setVisible(showForm);
        toggleFormBtn.setText(showForm ? "✕  Cancelar" : "＋  Nuevo Usuario");
        if (showForm) cargarProgramasEnForm();
        revalidate(); repaint();
    }

    private JPanel buildForm() {
        // Construye el formulario para crear o editar un usuario.
        JPanel outer = new JPanel(new BorderLayout(0, 12));
        outer.setBackground(Color.WHITE);
        outer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xbfdbfe)),
            new EmptyBorder(18, 20, 18, 20)));
        outer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 360));

        JPanel top = UIFactory.transparent(new BorderLayout());
        top.add(UIFactory.h3("➕  Crear Nuevo Usuario"), BorderLayout.WEST);
        top.add(UIFactory.hSep(), BorderLayout.SOUTH);
        outer.add(top, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(4, 2, 12, 10));
        grid.setOpaque(false);

        fCedula   = UIFactory.textField("Ej: 1000222333");
        fNombre   = UIFactory.textField("Ej: Juan");
        fApellido = UIFactory.textField("Ej: Pérez Gómez");
        fCorreo   = UIFactory.textField("correo@dominio.com");
        fPass     = UIFactory.passwordField("Contraseña");
        fRol      = UIFactory.comboBox("Estudiante", "Docente", "Asesor", "Director");
        fPrograma = new JComboBox<>();
        fPrograma.setFont(Theme.FONT_BODY);
        fPrograma.setPreferredSize(new Dimension(0, 34));
        fSemestre = UIFactory.comboBox("—", "1", "2", "3", "4", "5", "6", "7", "8");

        fRol.addActionListener(e -> {
            boolean isStudent = "Estudiante".equals(fRol.getSelectedItem());
            fSemestre.setEnabled(isStudent);
            fPrograma.setEnabled(isStudent);
            if (!isStudent) {
                try { fSemestre.setSelectedIndex(0); } catch (Exception ignored) {}
                try { fPrograma.setSelectedIndex(-1); } catch (Exception ignored) {}
            }
        });

        grid.add(UIFactory.labeledField("Cédula", fCedula));
        grid.add(UIFactory.labeledField("Nombre(s)", fNombre));
        grid.add(UIFactory.labeledField("Apellido(s)", fApellido));
        grid.add(UIFactory.labeledField("Correo Electrónico", fCorreo));
        grid.add(UIFactory.labeledField("Contraseña", fPass));
        grid.add(UIFactory.labeledField("Rol en el Sistema", fRol));
        grid.add(UIFactory.labeledField("Programa Académico", fPrograma));
        grid.add(UIFactory.labeledField("Semestre", fSemestre));
        outer.add(grid, BorderLayout.CENTER);

        JPanel footer = UIFactory.transparent(new FlowLayout(FlowLayout.RIGHT));
        JButton save = UIFactory.primaryBtn("💾  Guardar Usuario");
        save.addActionListener(e -> guardarUsuario());
        footer.add(save);
        outer.add(footer, BorderLayout.SOUTH);
        return outer;
    }

    private void cargarProgramasEnForm() {
        // Carga la lista de programas desde la BD en el combo del formulario.
        fPrograma.removeAllItems();
        java.util.List<?> progs = DB.listarProgramas();
        for (Object p : progs) fPrograma.addItem(p);
        if (fPrograma.getItemCount() == 0) {
            fPrograma.addItem("Licenciatura en Educación Infantil");
        }
    }

    private void guardarUsuario() {
        // Valida los campos del formulario y persiste el usuario en la BD.
        String cedula = fCedula.getText().trim();
        if (cedula.isEmpty()) {
            JOptionPane.showMessageDialog(this, "La cédula es requerida.", "Error", JOptionPane.WARNING_MESSAGE); return;
        }
        UsuarioDAO.Usuario u = new UsuarioDAO.Usuario();
        u.cedula        = cedula;
        u.nombre        = fNombre.getText().trim();
        u.apellido      = fApellido.getText().trim();
        u.correo        = fCorreo.getText().trim();
        u.contrasena    = new String(fPass.getPassword()).trim();
        u.rol           = fRol.getSelectedItem().toString();
        // Estado por defecto según rol: estudiantes comienzan en 'Pendiente' y habilitado=0;
        // otros roles empiezan 'Activo' y habilitado=1 (no participan en asignación)
        if ("Estudiante".equals(u.rol)) {
            u.estadoUsuario = "Pendiente";
            u.habilitadoAsig = 0;
        } else {
            u.estadoUsuario = "Activo";
            u.habilitadoAsig = 1;
        }
        Object prog = fPrograma.getSelectedItem();
        if (prog instanceof DB.Programa) {
            u.idPrograma = ((DB.Programa) prog).idPrograma;
        }
        // If role is not Estudiante, clear programa and semestre (asesores/docentes externos)
        if (!"Estudiante".equals(u.rol)) {
            u.idPrograma = null;
            u.semestre = null;
        }
        if ("Estudiante".equals(u.rol)) {
            Object semObj = fSemestre.getSelectedItem();
            if (semObj instanceof Number num) {
                u.semestre = num.intValue();
            } else if (semObj != null) {
                try { u.semestre = Integer.parseInt(semObj.toString()); }
                catch (NumberFormatException ex) { u.semestre = null; }
            }
        }

        if (DB.insertarUsuario(u)) {
            JOptionPane.showMessageDialog(this, "✅ Usuario creado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            refreshTable(); toggleForm();
        } else {
            JOptionPane.showMessageDialog(this,
                "❌ Error al guardar.\nVerifique que la cédula no exista y que la BD esté activa.",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel buildTableCard() {
        JPanel card = UIFactory.card();
        card.setLayout(new BorderLayout());
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2000));

        String[] cols = {"Cédula", "Nombre Completo", "Rol / Programa", "Semestre", "Estado", "Acciones"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 5; }
        };

        this.table = new JTable(tableModel);
        UIFactory.styleTable(this.table);
        this.table.getColumnModel().getColumn(4).setMaxWidth(110);
        this.table.getColumnModel().getColumn(4).setCellRenderer(new EstadoRenderer());
        this.table.getColumnModel().getColumn(5).setPreferredWidth(210);
        this.table.getColumnModel().getColumn(5).setCellRenderer(new AccionesRenderer());

        // Handle clicks on the actions column without using a TableCellEditor.
        // This keeps the visual buttons rendered by the renderer while routing clicks
        // to the appropriate action handlers (perfil / editar estado / eliminar).
        this.table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = PanelUsuarios.this.table.rowAtPoint(e.getPoint());
                int col = PanelUsuarios.this.table.columnAtPoint(e.getPoint());
                if (row < 0 || col != 5) return;
                Object cell = PanelUsuarios.this.table.getModel().getValueAt(PanelUsuarios.this.table.convertRowIndexToModel(row), 5);
                if (!(cell instanceof UsuarioDAO.Usuario)) return;
                UsuarioDAO.Usuario u = (UsuarioDAO.Usuario) cell;
                Rectangle cellRect = PanelUsuarios.this.table.getCellRect(row, col, false);
                int relX = e.getX() - cellRect.x;
                int cellW = cellRect.width;
                int third = Math.max(1, cellW / 3);

                // First third: Perfil (only for estudiantes)
                if ("Estudiante".equals(u.rol) && relX < third) {
                    if (onVerPerfil != null) onVerPerfil.accept(u);
                    return;
                }

                // Second third: Editar Estado
                if (relX < 2 * third) {
                    SwingUtilities.invokeLater(() -> {
                        java.util.List<String> allowed = DB.allowedValues("USUARIO", "ESTADO_USUARIO");
                        String[] opts = allowed.isEmpty() ? new String[]{"Activo", "Inactivo"} : allowed.toArray(new String[0]);
                        if (opts == null || opts.length == 0) opts = new String[]{"(sin opciones)"};
                        JComboBox<String> cb = new JComboBox<>(opts);
                        cb.setSelectedItem(u.estadoUsuario == null ? opts[0] : u.estadoUsuario);
                        int r = JOptionPane.showConfirmDialog(PanelUsuarios.this, cb, "Editar estado de usuario", JOptionPane.OK_CANCEL_OPTION);
                        if (r == JOptionPane.OK_OPTION) {
                            String nuevo = cb.getSelectedItem().toString();
                            String err = DB.cambiarEstadoUsuarioSafe(u.cedula, nuevo);
                            if (err == null) {
                                u.estadoUsuario = nuevo;
                                refreshTable();
                            } else {
                                JOptionPane.showMessageDialog(PanelUsuarios.this, "No fue posible cambiar el estado:\n" + err, "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    });
                    return;
                }

                // Last third: Eliminar (if allowed)
                boolean hasActiveAsig = false;
                try {
                    for (DB.Asignacion a : DB.asignacionesPorEstudiante(u.cedula)) {
                        if (a != null && a.estadoAsignacion != null && "Activa".equalsIgnoreCase(a.estadoAsignacion)) { hasActiveAsig = true; break; }
                    }
                } catch (Exception ex) { hasActiveAsig = true; }

                if (!hasActiveAsig) {
                    int conf = JOptionPane.showConfirmDialog(PanelUsuarios.this,
                        "¿Eliminar usuario " + u.nombreCompleto() + "?",
                        "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
                    if (conf == JOptionPane.YES_OPTION) {
                        String err = DB.eliminarUsuarioSafe(u.cedula);
                        if (err == null) {
                            SwingUtilities.invokeLater(() -> refreshTable());
                        } else {
                            int resp = JOptionPane.showConfirmDialog(PanelUsuarios.this,
                                "No fue posible eliminar el usuario:\n" + err + "\n\n¿Eliminar también las dependencias relacionadas (documentos, selecciones, asignaciones) y volver a intentar?",
                                "Eliminar dependencias?", JOptionPane.YES_NO_OPTION);
                            if (resp == JOptionPane.YES_OPTION) {
                                String res2 = DB.eliminarUsuarioConDependenciasSafe(u.cedula);
                                if (res2 == null) {
                                    JOptionPane.showMessageDialog(PanelUsuarios.this, "Usuario y dependencias eliminados.", "OK", JOptionPane.INFORMATION_MESSAGE);
                                    SwingUtilities.invokeLater(() -> refreshTable());
                                } else {
                                    JOptionPane.showMessageDialog(PanelUsuarios.this, "No fue posible eliminar las dependencias:\n" + res2, "Error", JOptionPane.ERROR_MESSAGE);
                                }
                            } else {
                                JOptionPane.showMessageDialog(PanelUsuarios.this, "No fue posible eliminar el usuario:\n" + err, "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                }
            }
        });

        refreshTable();
        card.add(UIFactory.tableScroll(this.table), BorderLayout.CENTER);
        return card;
    }

    /** Stop any active cell editing and clear selection — useful when switching views. */
    public void stopEditingAndClearSelection() {
        try {
            if (this.table != null) {
                if (this.table.isEditing()) {
                    try { this.table.getCellEditor().stopCellEditing(); } catch (Exception ignored) { try { this.table.getCellEditor().cancelCellEditing(); } catch (Exception ignored2) {} }
                }
                this.table.clearSelection();
                this.table.repaint();
            }
        } catch (Exception ignored) {}
    }

    public void refreshTable() {
        if (tableModel == null) return;
        // Repoblamos aplicando filtros actuales (si los hay).
        String txt = searchField != null ? searchField.getText().trim() : "";
        filtrarTabla(txt);
    }

    private void filtrarTabla(String texto) {
        if (tableModel == null) return;
        tableModel.setRowCount(0);
        String q = texto.toLowerCase();
        String selRol = filterRol != null ? (String) filterRol.getSelectedItem() : "Todos";
        Object selProg = filterPrograma != null ? filterPrograma.getSelectedItem() : "Todos";
        String selSem = filterSemestre != null ? (String) filterSemestre.getSelectedItem() : "Todos";
        String selEstado = filterEstado != null ? (String) filterEstado.getSelectedItem() : "Todos";

        for (UsuarioDAO.Usuario u : DB.listarUsuarios()) {
            if (u == null) continue;
            // Texto (cédula / nombre)
            boolean textMatch = texto.isEmpty() || (u.cedula != null && u.cedula.toLowerCase().contains(q)) || (u.nombreCompleto() != null && u.nombreCompleto().toLowerCase().contains(q));
            if (!textMatch) continue;

            // Rol
            if (selRol != null && !"Todos".equals(selRol) && (u.rol == null || !selRol.equals(u.rol))) continue;

            // Programa
            if (selProg != null && !(selProg instanceof String && "Todos".equals(selProg))) {
                if (selProg instanceof DB.Programa p) {
                    if (u.idPrograma == null || !p.idPrograma.equals(u.idPrograma)) continue;
                }
            }

            // Semestre
            if (selSem != null && !"Todos".equals(selSem)) {
                try {
                    int sem = Integer.parseInt(selSem);
                    if (u.semestre == null || u.semestre.intValue() != sem) continue;
                } catch (NumberFormatException ex) { /* ignore */ }
            }

            // Estado
            if (selEstado != null && !"Todos".equals(selEstado)) {
                if (u.estadoUsuario == null || !selEstado.equals(u.estadoUsuario)) continue;
            }

            String displayEstado = u.estadoUsuario == null ? "" : u.estadoUsuario;
            tableModel.addRow(new Object[]{
                u.cedula, u.nombreCompleto(),
                u.rol + (u.idPrograma != null ? " · " + DB.nombreProgramaPorId(u.idPrograma) : ""),
                u.semestre != null ? "S" + u.semestre : "—",
                displayEstado, u
            });
        }
    }

    class EstadoRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int row, int col) {
            String val = v == null ? "" : v.toString();
            JLabel badge;
            if ("Activo".equals(val)) badge = UIFactory.badgeActivo();
            else if ("Inactivo".equals(val)) badge = UIFactory.badgeInactivo();
            else if (val.toLowerCase().contains("pendiente")) badge = UIFactory.badgePendiente();
            else badge = UIFactory.badgeRevision();
            JPanel p = UIFactory.transparent(new FlowLayout(FlowLayout.CENTER, 0, 7));
            p.setBackground(s ? Theme.PRIMARY_LIGHT : (row%2==0 ? Color.WHITE : new Color(0xfafafa)));
            p.add(badge); return p;
        }
    }
    class AccionesRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int row, int col) {
            if (v == null) return new JPanel();
            if (!(v instanceof UsuarioDAO.Usuario)) return new JPanel();
            return buildAcciones((UsuarioDAO.Usuario) v, row, false);
        }
    }
    class AccionesEditor extends AbstractCellEditor implements TableCellEditor {
        AccionesEditor(JTable t) {}
        @Override public Object getCellEditorValue() { return null; }
        @Override public Component getTableCellEditorComponent(JTable t, Object v, boolean s, int row, int col) {
            if (v == null) return new JPanel();
            if (!(v instanceof UsuarioDAO.Usuario)) return new JPanel();
            return buildAcciones((UsuarioDAO.Usuario) v, row, true);
        }
    }
    private JPanel buildAcciones(UsuarioDAO.Usuario u, int row, boolean live) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 7));
        p.setBackground(row%2==0 ? Color.WHITE : new Color(0xfafafa));
        if (u == null) return p;
        if ("Estudiante".equals(u.rol)) {
            JButton perfil = UIFactory.blueOutlineBtn("👁 Perfil");
            if (live) perfil.addActionListener(e -> { if (onVerPerfil != null) onVerPerfil.accept(u); });
            p.add(perfil);
        }
        JButton editarEstadoBtn = UIFactory.outlineBtn("⚙ Editar Estado");
        if (live) editarEstadoBtn.addActionListener(e -> {
            // Evitar que el editor de celda quede activo mientras mostramos el diálogo.
            JTable tableAncestor = (JTable) javax.swing.SwingUtilities.getAncestorOfClass(JTable.class, p);
            if (tableAncestor != null && tableAncestor.isEditing()) {
                try { tableAncestor.getCellEditor().stopCellEditing(); } catch (Exception ignored) {}
            }

            // Mostrar diálogo en la cola de eventos para mayor seguridad en el EDT.
            javax.swing.SwingUtilities.invokeLater(() -> {
                java.util.List<String> allowed = DB.allowedValues("USUARIO", "ESTADO_USUARIO");
                String[] opts = allowed.isEmpty() ? new String[]{"Activo", "Inactivo"} : allowed.toArray(new String[0]);
                if (opts == null || opts.length == 0) opts = new String[]{"(sin opciones)"};
                JComboBox<String> cb = new JComboBox<>(opts);
                cb.setSelectedItem(u.estadoUsuario == null ? opts[0] : u.estadoUsuario);
                int r = JOptionPane.showConfirmDialog(PanelUsuarios.this, cb, "Editar estado de usuario", JOptionPane.OK_CANCEL_OPTION);
                if (r == JOptionPane.OK_OPTION) {
                    String nuevo = cb.getSelectedItem().toString();
                    String err = DB.cambiarEstadoUsuarioSafe(u.cedula, nuevo);
                    if (err == null) {
                        u.estadoUsuario = nuevo;
                        // Refrescar tabla completa: evita inconsistencias de edición
                        refreshTable();
                        if (tableAncestor != null && tableAncestor.isEditing()) {
                            try { tableAncestor.getCellEditor().stopCellEditing(); } catch (Exception ignored) {}
                        }
                    } else {
                        JOptionPane.showMessageDialog(PanelUsuarios.this, "No fue posible cambiar el estado:\n" + err, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
        });
        p.add(editarEstadoBtn);

        // Mostrar botón de eliminar solo si el usuario NO tiene asignaciones activas.
        boolean hasActiveAsig = false;
        try {
            for (DB.Asignacion a : DB.asignacionesPorEstudiante(u.cedula)) {
                if (a != null && a.estadoAsignacion != null && "Activa".equalsIgnoreCase(a.estadoAsignacion)) { hasActiveAsig = true; break; }
            }
        } catch (Exception ex) {
            // Si falla la comprobación, ser conservador y deshabilitar eliminación.
            hasActiveAsig = true;
        }

        if (!hasActiveAsig) {
            JButton eliminarBtn = UIFactory.dangerBtn("🗑 Eliminar");
            if (live) eliminarBtn.addActionListener(e -> {
                int conf = JOptionPane.showConfirmDialog(this,
                    "¿Eliminar usuario " + u.nombreCompleto() + "?",
                    "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
                if (conf == JOptionPane.YES_OPTION) {
                    String err = DB.eliminarUsuarioSafe(u.cedula);
                    if (err == null) {
                        // Stop editor if still active (remove buttons view)
                        Component src = (Component) e.getSource();
                        JTable table = (JTable) SwingUtilities.getAncestorOfClass(JTable.class, src);
                        if (table != null && table.isEditing()) {
                            try { table.getCellEditor().stopCellEditing(); } catch (Exception ex) { try { table.getCellEditor().cancelCellEditing(); } catch (Exception ignored) {} }
                        }
                        SwingUtilities.invokeLater(() -> refreshTable());
                    } else {
                        // Mostrar motivo y ofrecer borrar dependencias
                        int resp = JOptionPane.showConfirmDialog(this,
                            "No fue posible eliminar el usuario:\n" + err + "\n\n¿Eliminar también las dependencias relacionadas (documentos, selecciones, asignaciones) y volver a intentar?",
                            "Eliminar dependencias?", JOptionPane.YES_NO_OPTION);
                        if (resp == JOptionPane.YES_OPTION) {
                            String res2 = DB.eliminarUsuarioConDependenciasSafe(u.cedula);
                            if (res2 == null) {
                                JOptionPane.showMessageDialog(this, "Usuario y dependencias eliminados.", "OK", JOptionPane.INFORMATION_MESSAGE);
                                SwingUtilities.invokeLater(() -> refreshTable());
                            } else {
                                JOptionPane.showMessageDialog(this, "No fue posible eliminar las dependencias:\n" + res2, "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        } else {
                            JOptionPane.showMessageDialog(this, "No fue posible eliminar el usuario:\n" + err, "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });
            p.add(eliminarBtn);
        }
        return p;
    }
}
