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
    private JPanel formPanel;
    private boolean showForm = false;
    private JButton toggleFormBtn;

    private JTextField     fCedula, fNombre, fApellido, fCorreo;
    private JPasswordField fPass;
    private JComboBox<String> fRol, fSemestre;
    private JComboBox<Object> fPrograma;

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
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        JTextField search = UIFactory.searchField("Buscar por cédula o nombre...");
        search.setPreferredSize(new Dimension(260, 34));
        search.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { filtrarTabla(search.getText().trim()); }
        });
        bar.add(search, BorderLayout.WEST);
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
        fSemestre = UIFactory.comboBox("1", "2", "3", "4", "5", "6", "7", "8");

        fRol.addActionListener(e -> fSemestre.setEnabled("Estudiante".equals(fRol.getSelectedItem())));

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

        JTable table = new JTable(tableModel);
        UIFactory.styleTable(table);
        table.getColumnModel().getColumn(4).setMaxWidth(110);
        table.getColumnModel().getColumn(4).setCellRenderer(new EstadoRenderer());
        table.getColumnModel().getColumn(5).setPreferredWidth(210);
        table.getColumnModel().getColumn(5).setCellRenderer(new AccionesRenderer());
        table.getColumnModel().getColumn(5).setCellEditor(new AccionesEditor(table));

        refreshTable();
        card.add(UIFactory.tableScroll(table), BorderLayout.CENTER);
        return card;
    }

    public void refreshTable() {
        if (tableModel == null) return;
        tableModel.setRowCount(0);
        for (UsuarioDAO.Usuario u : DB.listarUsuarios()) {
            String displayEstado = u.estadoUsuario == null ? "" : u.estadoUsuario;
            tableModel.addRow(new Object[]{
                u.cedula, u.nombreCompleto(),
                u.rol + (u.idPrograma != null ? " · " + DB.nombreProgramaPorId(u.idPrograma) : ""),
                u.semestre != null ? "S" + u.semestre : "—",
                displayEstado, u
            });
        }
    }

    private void filtrarTabla(String texto) {
        if (tableModel == null) return;
        tableModel.setRowCount(0);
        String q = texto.toLowerCase();
        for (UsuarioDAO.Usuario u : DB.listarUsuarios()) {
            if (texto.isEmpty() || u.cedula.toLowerCase().contains(q) || u.nombreCompleto().toLowerCase().contains(q)) {
                String displayEstado = u.estadoUsuario == null ? "" : u.estadoUsuario;
                tableModel.addRow(new Object[]{
                    u.cedula, u.nombreCompleto(),
                    u.rol + (u.idPrograma != null ? " · " + DB.nombreProgramaPorId(u.idPrograma) : ""),
                    u.semestre != null ? "S" + u.semestre : "—",
                    displayEstado, u
                });
            }
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
            return buildAcciones((UsuarioDAO.Usuario) v, row, false);
        }
    }
    class AccionesEditor extends AbstractCellEditor implements TableCellEditor {
        AccionesEditor(JTable t) {}
        @Override public Object getCellEditorValue() { return null; }
        @Override public Component getTableCellEditorComponent(JTable t, Object v, boolean s, int row, int col) {
            return buildAcciones((UsuarioDAO.Usuario) v, row, true);
        }
    }
    private JPanel buildAcciones(UsuarioDAO.Usuario u, int row, boolean live) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 7));
        p.setBackground(row%2==0 ? Color.WHITE : new Color(0xfafafa));
        if ("Estudiante".equals(u.rol)) {
            JButton perfil = UIFactory.blueOutlineBtn("👁 Perfil");
            if (live) perfil.addActionListener(e -> { if (onVerPerfil != null) onVerPerfil.accept(u); });
            p.add(perfil);
        }
        JButton editarEstadoBtn = UIFactory.outlineBtn("⚙ Editar Estado");
        if (live) editarEstadoBtn.addActionListener(e -> {
            java.util.List<String> allowed = DB.allowedValues("USUARIO", "ESTADO_USUARIO");
            String[] opts;
            if (allowed.isEmpty()) opts = new String[]{"Activo", "Inactivo"};
            else opts = allowed.toArray(new String[0]);
            JComboBox<String> cb = new JComboBox<>(opts);
            cb.setSelectedItem(u.estadoUsuario == null ? opts[0] : u.estadoUsuario);
            int r = JOptionPane.showConfirmDialog(this, cb, "Editar estado de usuario", JOptionPane.OK_CANCEL_OPTION);
            if (r == JOptionPane.OK_OPTION) {
                String nuevo = cb.getSelectedItem().toString();
                String err = DB.cambiarEstadoUsuarioSafe(u.cedula, nuevo);
                if (err == null) {
                    u.estadoUsuario = nuevo;
                    // actualizar fila por cédula
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        JTable table = (JTable) javax.swing.SwingUtilities.getAncestorOfClass(JTable.class, p);
                        if (table != null) {
                            int modelRow = -1;
                            for (int r2 = 0; r2 < tableModel.getRowCount(); r2++) {
                                Object ced = tableModel.getValueAt(r2, 0);
                                if (ced != null && ced.toString().equals(u.cedula)) { modelRow = r2; break; }
                            }
                            if (modelRow >= 0) {
                                try { tableModel.setValueAt(nuevo, modelRow, 4); } catch (Exception ignored) {}
                                try { tableModel.setValueAt(u, modelRow, 5); } catch (Exception ignored) {}
                            } else {
                                refreshTable();
                            }
                            if (table.isEditing()) { try { table.getCellEditor().stopCellEditing(); } catch (Exception ignored) {} }
                            table.repaint();
                        } else {
                            refreshTable();
                        }
                    });
                } else {
                    JOptionPane.showMessageDialog(this, "No fue posible cambiar el estado:\n" + err, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        p.add(editarEstadoBtn);
        boolean activo = "Activo".equals(u.estadoUsuario);
        JButton toggle = activo ? UIFactory.dangerBtn("✗ Inactivar") : UIFactory.greenBtn("✓ Activar");
        if (live) toggle.addActionListener(e -> {
            String nuevo = activo ? "Inactivo" : "Activo";
            if (DB.cambiarEstadoUsuario(u.cedula, nuevo)) {
                u.estadoUsuario = nuevo;
                javax.swing.SwingUtilities.invokeLater(() -> {
                    JTable table = (JTable) javax.swing.SwingUtilities.getAncestorOfClass(JTable.class, p);
                    if (table != null) {
                        int modelRow = -1;
                        for (int r = 0; r < tableModel.getRowCount(); r++) {
                            Object ced = tableModel.getValueAt(r, 0);
                            if (ced != null && ced.toString().equals(u.cedula)) { modelRow = r; break; }
                        }
                        if (modelRow >= 0) {
                            try { tableModel.setValueAt(nuevo, modelRow, 4); } catch (Exception ignored) {}
                            try { tableModel.setValueAt(u, modelRow, 5); } catch (Exception ignored) {}
                        } else {
                            refreshTable();
                        }
                        if (table.isEditing()) {
                            try { table.getCellEditor().stopCellEditing(); } catch (Exception ignored) {}
                        }
                        table.repaint();
                    } else {
                        refreshTable();
                    }
                });
            }
        });
        p.add(toggle);
        return p;
    }
}
