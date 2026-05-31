package sigep;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import sigep.db.DB;

/**
 * Gestión de Instituciones Receptoras — replica InstitutionsManagement.tsx
 */
// Uso: administra CRUD de instituciones receptoras y su formulario.
public class PanelInstituciones extends JPanel {

    public static class Institucion {
        public String idInstitucion; // ID real en la BD
        public String nit,nombre,direccion,telefono,correo;
        public int cuposTotales,cuposOcupados;
        public boolean estadoConvenio;
        Institucion(String idInstitucion,String nit,String nombre,String dir,String tel,String correo,int tot,int ocu,boolean est){
            this.idInstitucion = idInstitucion; this.nit=nit; this.nombre=nombre; this.direccion=dir; this.telefono=tel; this.correo=correo;
            cuposTotales=tot; cuposOcupados=ocu; estadoConvenio=est;
        }
        public int cuposDisponibles(){ return cuposTotales-cuposOcupados; }
    }

    public final List<Institucion> instituciones=new ArrayList<>();
    private DefaultTableModel tableModel;
    private JTable table;
    private JPanel formPanel;
    private boolean showForm=false;
    private String editingId=null; // will hold DB ID_Institucion when editing
    private JButton toggleFormBtn;

    // Form fields
    private JTextField fNit,fNombre,fDir,fTel,fCorreo,fCupos;
    private JComboBox<String> fEstado;

    public PanelInstituciones(){
        // Constructor: carga instituciones desde la BD y construye la UI.
        instituciones.clear();
        try {
            var list = DB.listarInstituciones();
            for (var ii : list) {
                instituciones.add(new Institucion(ii.idInstitucion, ii.nit, ii.nombre, ii.direccion, ii.telefono, ii.correo, ii.cuposTotales, ii.cuposDisp, "Vigente".equals(ii.estadoConvenio)));
            }
        } catch (Exception ignored) {}
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(Theme.PADDING,Theme.PADDING,Theme.PADDING,Theme.PADDING));
        build();
    }

    

    private void build(){
        // Monta la estructura del panel: header, action bar, formulario y tabla.
        add(buildSectionHeader(),BorderLayout.NORTH);

        JPanel center=new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center,BoxLayout.Y_AXIS));
        center.add(UIFactory.gap(16));
        center.add(buildActionBar());
        center.add(UIFactory.gap(10));
        formPanel=buildForm(); formPanel.setVisible(false);
        center.add(formPanel);
        center.add(UIFactory.gap(6));
        center.add(buildTableCard());
        add(center,BorderLayout.CENTER);
    }

    private JPanel buildSectionHeader(){
        // Construye el header de la sección con icono y título.
        JPanel h=UIFactory.transparent(new BorderLayout());
        h.setBorder(new EmptyBorder(0,0,12,0));
        JPanel left=UIFactory.transparent(new FlowLayout(FlowLayout.LEFT,8,0));
        JLabel icon=new JLabel("🏢"); icon.setFont(new Font("Segoe UI Emoji",Font.PLAIN,20));
        left.add(icon); left.add(UIFactory.h2("Instituciones y Convenios"));
        h.add(left,BorderLayout.WEST);
        JPanel w=UIFactory.transparent(new BorderLayout());
        w.add(h,BorderLayout.NORTH); w.add(UIFactory.hSep(),BorderLayout.SOUTH);
        return w;
    }

    private JPanel buildActionBar(){
        // Construye la barra de acciones (buscar + botón crear institución).
        JPanel bar=UIFactory.actionBar();
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE,56));
        JTextField search=UIFactory.searchField("Buscar NIT o Nombre de Institución...");
        search.setPreferredSize(new Dimension(280,34));
        bar.add(search,BorderLayout.WEST);
        toggleFormBtn=UIFactory.primaryBtn("＋  Crear Institución");
        toggleFormBtn.addActionListener(e->{ editingId=null; toggleForm(false); });
        bar.add(toggleFormBtn,BorderLayout.EAST);
        return bar;
    }

    @SuppressWarnings("unused")
    private void toggleForm(boolean _editing){
        // Muestra u oculta el formulario de creación/edición.
        showForm = !showForm;
        if(showForm && editingId!=null) showForm=true;
        formPanel.setVisible(showForm);
        toggleFormBtn.setText(showForm?"✕  Cancelar":"＋  Crear Institución");
        revalidate(); repaint();
    }

    private JPanel buildForm(){
        // Crea y devuelve el formulario para registrar/editar una institución.
        JPanel outer=new JPanel(new BorderLayout(0,12));
        outer.setBackground(Color.WHITE);
        outer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER),
            new EmptyBorder(18,20,18,20)));
        outer.setMaximumSize(new Dimension(Integer.MAX_VALUE,260));

        JLabel fTitle=UIFactory.h3("🏢  Registrar / Editar Institución");
        JPanel top=UIFactory.transparent(new BorderLayout());
        top.add(fTitle,BorderLayout.WEST);
        top.add(UIFactory.hSep(),BorderLayout.SOUTH);
        outer.add(top,BorderLayout.NORTH);

        // 3 columnas
        JPanel grid=new JPanel(new GridLayout(2,3,12,10));
        grid.setOpaque(false);

        fNit     = UIFactory.textField("Ej: 900.123.456-7");
        fNombre  = UIFactory.textField("Nombre completo");
        fDir     = UIFactory.textField("Ej: Calle 123 # 45-67");
        fTel     = UIFactory.textField("Ej: 300 123 4567");
        fCorreo  = UIFactory.textField("correo@institucion.com");
        fCupos   = UIFactory.textField("Ej: 5");
        fEstado  = UIFactory.comboBox("Vigente","Vencido");

        grid.add(UIFactory.labeledField("NIT de la Institución",fNit));
        grid.add(UIFactory.labeledField("Nombre",fNombre));
        grid.add(UIFactory.labeledField("Dirección",fDir));
        grid.add(UIFactory.labeledField("Teléfono",fTel));
        grid.add(UIFactory.labeledField("Correo Electrónico",fCorreo));
        grid.add(UIFactory.labeledField("Cupos Totales",fCupos));

        outer.add(grid,BorderLayout.CENTER);

        JPanel footer=UIFactory.transparent(new FlowLayout(FlowLayout.RIGHT));
        JButton save=UIFactory.primaryBtn("💾  Guardar Institución");
        save.addActionListener(e->saveInst());
        footer.add(save);
        outer.add(footer,BorderLayout.SOUTH);
        return outer;
    }

    private void fillForm(Institucion inst){
        fNit.setText(inst.nit); fNombre.setText(inst.nombre); fDir.setText(inst.direccion);
        fTel.setText(inst.telefono); fCorreo.setText(inst.correo);
        fCupos.setText(String.valueOf(inst.cuposTotales));
        fEstado.setSelectedIndex(inst.estadoConvenio?0:1);
    }

    private void saveInst(){
        try{
            int cupos=Integer.parseInt(fCupos.getText().trim());
                if(editingId!=null){
                    // Actualizar en BD
                    try {
                        DB.Institucion u = new DB.Institucion();
                        u.idInstitucion = editingId;
                        u.nit = fNit.getText().trim(); u.nombre = fNombre.getText().trim(); u.direccion = fDir.getText().trim();
                        u.telefono = fTel.getText().trim(); u.correo = fCorreo.getText().trim(); u.cuposTotales = cupos;
                        u.cuposDisp = 0; // dejar como está en BD; we'll refresh from DB
                        u.estadoConvenio = fEstado.getSelectedIndex()==0 ? "Vigente" : "Vencido";
                        if (DB.actualizarInstitucion(u)) {
                            // recargar lista desde BD
                            instituciones.clear();
                            var list = DB.listarInstituciones();
                            for (var ii : list) instituciones.add(new Institucion(ii.idInstitucion, ii.nit, ii.nombre, ii.direccion, ii.telefono, ii.correo, ii.cuposTotales, ii.cuposDisp, "Vigente".equals(ii.estadoConvenio)));
                        } else {
                            JOptionPane.showMessageDialog(this,"No se pudo actualizar la institución en la base de datos.","Error",JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) { JOptionPane.showMessageDialog(this,"Error al actualizar: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE); }
                } else {
                    try {
                        DB.Institucion u = new DB.Institucion();
                        u.nit = fNit.getText().trim(); u.nombre = fNombre.getText().trim(); u.direccion = fDir.getText().trim();
                        u.telefono = fTel.getText().trim(); u.correo = fCorreo.getText().trim(); u.cuposTotales = cupos;
                        u.cuposDisp = cupos; // al crear, cupos disponibles = cupos totales
                        u.estadoConvenio = fEstado.getSelectedIndex()==0 ? "Vigente" : "Vencido";
                        if (DB.insertarInstitucion(u)) {
                            instituciones.clear();
                            var list = DB.listarInstituciones();
                            for (var ii : list) instituciones.add(new Institucion(ii.idInstitucion, ii.nit, ii.nombre, ii.direccion, ii.telefono, ii.correo, ii.cuposTotales, ii.cuposDisp, "Vigente".equals(ii.estadoConvenio)));
                        } else {
                            JOptionPane.showMessageDialog(this,"No se pudo guardar la institución en la base de datos.","Error",JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) { JOptionPane.showMessageDialog(this,"Error al guardar: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE); }
                }
            refreshTable(); showForm=false; editingId=null;
            formPanel.setVisible(false);
            toggleFormBtn.setText("＋  Crear Institución");
            revalidate(); repaint();
        }catch(NumberFormatException ex){
            JOptionPane.showMessageDialog(this,"Ingrese un número válido de cupos.","Error",JOptionPane.WARNING_MESSAGE);
        }
    }

    private JPanel buildTableCard(){
        JPanel card=UIFactory.card();
        card.setLayout(new BorderLayout());
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE,2000));

        String[] cols={"Institución / NIT","Contacto","Cupos Disponibles","Estado Convenio","Acciones"};
        tableModel=new DefaultTableModel(cols,0){
            @Override public boolean isCellEditable(int r,int c){ return c==4; }
        };

        this.table = new JTable(tableModel);
        UIFactory.styleTable(this.table);
        this.table.getColumnModel().getColumn(2).setMaxWidth(130);
        this.table.getColumnModel().getColumn(3).setMaxWidth(130);
        this.table.getColumnModel().getColumn(3).setCellRenderer(new ConvenioRenderer());
        this.table.getColumnModel().getColumn(4).setPreferredWidth(210);
        this.table.getColumnModel().getColumn(4).setCellRenderer(new InstAccionesRenderer());

        // Handle clicks on actions column without a TableCellEditor to keep renderer visible
        this.table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = PanelInstituciones.this.table.rowAtPoint(e.getPoint());
                int col = PanelInstituciones.this.table.columnAtPoint(e.getPoint());
                if (row < 0 || col != 4) return;
                Object cell = PanelInstituciones.this.table.getModel().getValueAt(PanelInstituciones.this.table.convertRowIndexToModel(row), 4);
                if (!(cell instanceof Institucion)) return;
                Institucion inst = (Institucion) cell;
                Rectangle cellRect = PanelInstituciones.this.table.getCellRect(row, col, false);
                int relX = e.getX() - cellRect.x;

                // Build a non-live actions panel to compute preferred widths and detect which button was clicked.
                JPanel probe = buildInstAcciones(inst, row, false);
                // FlowLayout left with hgap=4 per buildInstAcciones
                int hgap = 4;
                int cursor = hgap;
                Component[] comps = probe.getComponents();
                boolean handled = false;
                for (Component c : comps) {
                    Dimension ps = c.getPreferredSize();
                    int w = ps.width;
                    if (relX >= cursor && relX <= cursor + w) {
                        // Map by button text to existing actions
                        String txt = c instanceof JButton ? ((JButton)c).getText() : "";
                        if (txt != null && txt.contains("Editar") && txt.contains("✏")) {
                            // Edit action
                            editingId = inst.idInstitucion; fillForm(inst); showForm = true; formPanel.setVisible(true); toggleFormBtn.setText("✕  Cancelar"); revalidate(); repaint();
                        } else if (txt != null && (txt.contains("Editar Estado") || txt.contains("⚙"))) {
                            JTable tableAncestor = (JTable) javax.swing.SwingUtilities.getAncestorOfClass(JTable.class, PanelInstituciones.this);
                            if (tableAncestor != null && tableAncestor.isEditing()) {
                                try { tableAncestor.getCellEditor().stopCellEditing(); } catch (Exception ignored) {}
                            }
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                java.util.List<String> allowed = DB.allowedValues("INSTITUCION_RECEPTORA", "ESTADO_CONVENIO");
                                String[] opts = allowed.isEmpty() ? new String[]{"Vigente","Vencido"} : allowed.toArray(new String[0]);
                                if (opts == null || opts.length == 0) opts = new String[]{"(sin opciones)"};
                                JComboBox<String> cb = new JComboBox<>(opts);
                                cb.setSelectedItem(inst.estadoConvenio?opts[0]:(opts.length>1?opts[1]:opts[0]));
                                int r = JOptionPane.showConfirmDialog(PanelInstituciones.this, cb, "Editar estado del convenio", JOptionPane.OK_CANCEL_OPTION);
                                if (r == JOptionPane.OK_OPTION) {
                                    String nuevoStr = cb.getSelectedItem().toString();
                                    boolean nuevo = "Vigente".equals(nuevoStr);
                                    String err = DB.cambiarEstadoConvenioSafe(inst.idInstitucion, nuevoStr);
                                    if (err == null) {
                                        inst.estadoConvenio = nuevo;
                                        try {
                                            instituciones.clear();
                                            var list = DB.listarInstituciones();
                                            for (var ii : list) instituciones.add(new Institucion(ii.idInstitucion, ii.nit, ii.nombre, ii.direccion, ii.telefono, ii.correo, ii.cuposTotales, ii.cuposDisp, "Vigente".equals(ii.estadoConvenio)));
                                        } catch (Exception ex) { /* ignore */ }
                                        refreshTable();
                                    } else {
                                        JOptionPane.showMessageDialog(PanelInstituciones.this, "No fue posible cambiar el estado en la base de datos.\nDetalle: " + err, "Error", JOptionPane.ERROR_MESSAGE);
                                    }
                                }
                            });
                        } else if (txt != null && (txt.contains("Estudiantes") || txt.contains("👥"))) {
                            showStudents(inst);
                        } else if (txt != null && (txt.contains("Eliminar") || txt.contains("🗑"))) {
                            JTable tableAncestor = (JTable) javax.swing.SwingUtilities.getAncestorOfClass(JTable.class, PanelInstituciones.this);
                            if (tableAncestor != null && tableAncestor.isEditing()) {
                                try { tableAncestor.getCellEditor().stopCellEditing(); } catch (Exception ignored) {}
                            }
                            int conf = JOptionPane.showConfirmDialog(PanelInstituciones.this,
                                "¿Eliminar institución " + inst.nombre + "?",
                                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
                            if (conf == JOptionPane.YES_OPTION) {
                                if (DB.eliminarInstitucion(inst.idInstitucion)) {
                                    try {
                                        instituciones.clear(); var list = DB.listarInstituciones(); for (var ii : list) instituciones.add(new Institucion(ii.idInstitucion, ii.nit, ii.nombre, ii.direccion, ii.telefono, ii.correo, ii.cuposTotales, ii.cuposDisp, "Vigente".equals(ii.estadoConvenio)));
                                    } catch (Exception ex) { /* ignore */ }
                                    SwingUtilities.invokeLater(() -> refreshTable());
                                } else {
                                    JOptionPane.showMessageDialog(PanelInstituciones.this, "No fue posible eliminar la institución.", "Error", JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        }
                        handled = true;
                        break;
                    }
                    cursor += w + hgap;
                }
                if (handled) return;
            }
        });

        refreshTable();
        card.add(UIFactory.tableScroll(this.table),BorderLayout.CENTER);
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

    public void refreshTable(){
        if(tableModel==null) return;
        tableModel.setRowCount(0);
        // Recompute available cupos based on active assignments to avoid stale DB values.
        java.util.List<DB.Asignacion> active = DB.asignacionesActivas();
        for(Institucion i:instituciones){
            String nameHtml = "<html>" + escapeHtml(i.nombre) + "<br/>NIT: " + escapeHtml(i.nit) + "</html>";
            String contactHtml = "<html>" + escapeHtml(i.correo) + "<br/>" + escapeHtml(i.telefono) + "</html>";
            int assigned = 0;
            try {
                for (DB.Asignacion a : active) if (a != null && a.idInstitucion != null && a.idInstitucion.equals(i.idInstitucion)) assigned++;
            } catch (Exception ignored) {}
            int assignedCount = assigned;
            if (assignedCount < 0) assignedCount = 0;
            if (assignedCount > i.cuposTotales) assignedCount = i.cuposTotales;
            tableModel.addRow(new Object[]{
                nameHtml, contactHtml,
                assignedCount + " / " + i.cuposTotales,
                i.estadoConvenio?"Vigente":"Vencido", i
            });
        }
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("\n", "<br/>");
    }

    class ConvenioRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int row,int col){
            JLabel badge=v!=null&&v.toString().equals("Vigente")?UIFactory.badgeActivo():UIFactory.badgeInactivo();
            badge.setHorizontalAlignment(SwingConstants.CENTER);
            JPanel p=UIFactory.transparent(new FlowLayout(FlowLayout.CENTER,0,7));
            p.setBackground(s?Theme.PRIMARY_LIGHT:(row%2==0?Color.WHITE:new Color(0xfafafa)));
            p.add(badge); return p;
        }
    }

    class InstAccionesRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int row,int col){
            return buildInstAcciones((Institucion)v,row,false);
        }
    }
    class InstAccionesEditor extends AbstractCellEditor implements TableCellEditor {
        InstAccionesEditor(JTable t){}
        @Override public Object getCellEditorValue(){ return null; }
        @Override public Component getTableCellEditorComponent(JTable t,Object v,boolean s,int row,int col){
            return buildInstAcciones((Institucion)v,row,true);
        }
    }

    private JPanel buildInstAcciones(Institucion inst, int row, boolean live){
        JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,4,7));
        p.setBackground(row%2==0?Color.WHITE:new Color(0xfafafa));

        JButton editBtn=UIFactory.outlineBtn("✏ Editar");
        JButton editarEstadoBtn = UIFactory.outlineBtn("⚙ Editar Estado");
        JButton estudBtn=UIFactory.blueOutlineBtn("👥 Estudiantes");

        if(live){
            editBtn.addActionListener(e->{ editingId=inst.idInstitucion; fillForm(inst); showForm=true; formPanel.setVisible(true); toggleFormBtn.setText("✕  Cancelar"); revalidate(); repaint(); });
            editarEstadoBtn.addActionListener(e -> {
                // Detener edición activa antes de mostrar diálogo
                JTable tableAncestor = (JTable) javax.swing.SwingUtilities.getAncestorOfClass(JTable.class, p);
                if (tableAncestor != null && tableAncestor.isEditing()) {
                    try { tableAncestor.getCellEditor().stopCellEditing(); } catch (Exception ignored) {}
                }
                javax.swing.SwingUtilities.invokeLater(() -> {
                    java.util.List<String> allowed = DB.allowedValues("INSTITUCION_RECEPTORA", "ESTADO_CONVENIO");
                    String[] opts = allowed.isEmpty() ? new String[]{"Vigente","Vencido"} : allowed.toArray(new String[0]);
                    if (opts == null || opts.length == 0) opts = new String[]{"(sin opciones)"};
                    JComboBox<String> cb = new JComboBox<>(opts);
                    cb.setSelectedItem(inst.estadoConvenio?opts[0]:(opts.length>1?opts[1]:opts[0]));
                    int r = JOptionPane.showConfirmDialog(PanelInstituciones.this, cb, "Editar estado del convenio", JOptionPane.OK_CANCEL_OPTION);
                    if (r == JOptionPane.OK_OPTION) {
                        String nuevoStr = cb.getSelectedItem().toString();
                        boolean nuevo = "Vigente".equals(nuevoStr);
                        String err = DB.cambiarEstadoConvenioSafe(inst.idInstitucion, nuevoStr);
                        if (err == null) {
                            inst.estadoConvenio = nuevo;
                            // Refrescar lista completa para evitar inconsistencias
                            try {
                                instituciones.clear();
                                var list = DB.listarInstituciones();
                                for (var ii : list) instituciones.add(new Institucion(ii.idInstitucion, ii.nit, ii.nombre, ii.direccion, ii.telefono, ii.correo, ii.cuposTotales, ii.cuposDisp, "Vigente".equals(ii.estadoConvenio)));
                            } catch (Exception ex) { /* ignore */ }
                            refreshTable();
                            if (tableAncestor != null && tableAncestor.isEditing()) {
                                try { tableAncestor.getCellEditor().stopCellEditing(); } catch (Exception ignored) {}
                            }
                        } else {
                            JOptionPane.showMessageDialog(PanelInstituciones.this, "No fue posible cambiar el estado en la base de datos.\nDetalle: " + err, "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
            });
            estudBtn.addActionListener(e->showStudents(inst));
        }

        // Mostrar botón Eliminar solo si NO existen asignaciones activas en la institución.
        boolean hasActiveAsig = false;
        try {
            for (DB.Asignacion a : DB.asignacionesActivas()) {
                if (a != null && a.idInstitucion != null && a.idInstitucion.equals(inst.idInstitucion)) { hasActiveAsig = true; break; }
            }
        } catch (Exception ex) { hasActiveAsig = true; }

        if (!hasActiveAsig) {
            JButton eliminarBtn = UIFactory.dangerBtn("🗑 Eliminar");
            if (live) eliminarBtn.addActionListener(e -> {
                JTable tableAncestor = (JTable) javax.swing.SwingUtilities.getAncestorOfClass(JTable.class, p);
                if (tableAncestor != null && tableAncestor.isEditing()) {
                    try { tableAncestor.getCellEditor().stopCellEditing(); } catch (Exception ignored) {}
                }
                int conf = JOptionPane.showConfirmDialog(PanelInstituciones.this,
                    "¿Eliminar institución " + inst.nombre + "?",
                    "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
                if (conf == JOptionPane.YES_OPTION) {
                    if (DB.eliminarInstitucion(inst.idInstitucion)) {
                        try {
                            instituciones.clear();
                            var list = DB.listarInstituciones();
                            for (var ii : list) instituciones.add(new Institucion(ii.idInstitucion, ii.nit, ii.nombre, ii.direccion, ii.telefono, ii.correo, ii.cuposTotales, ii.cuposDisp, "Vigente".equals(ii.estadoConvenio)));
                        } catch (Exception ex) { /* ignore */ }
                        // Ensure editor stopped and refresh on EDT so buttons disappear
                        JTable tbl = (JTable) javax.swing.SwingUtilities.getAncestorOfClass(JTable.class, p);
                        if (tbl != null && tbl.isEditing()) {
                            try { tbl.getCellEditor().stopCellEditing(); } catch (Exception ex) { try { tbl.getCellEditor().cancelCellEditing(); } catch (Exception ignored) {} }
                        }
                        SwingUtilities.invokeLater(() -> refreshTable());
                    } else {
                        JOptionPane.showMessageDialog(PanelInstituciones.this, "No fue posible eliminar la institución.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            p.add(eliminarBtn);
        }

        p.add(editBtn); p.add(editarEstadoBtn); p.add(estudBtn);
        return p;
    }

    private void showStudents(Institucion inst){
        // Construir tabla con los estudiantes vinculados a la institución.
        String[] cols = {"Nombre","Cédula","Programa","Semestre","Asesor","Docente","Estado"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        try {
            var all = DB.listarAsignaciones();
            for (DB.Asignacion a : all) {
                if (a == null) continue;
                if (a.idInstitucion == null || !a.idInstitucion.equals(inst.idInstitucion)) continue;
                String ced = a.cedulaEstudiante == null ? "" : a.cedulaEstudiante;
                String nombre = a.nombreEstudiante != null ? a.nombreEstudiante : "";
                String programa = a.programaEstudiante != null ? a.programaEstudiante : DB.nombreProgramaPorId(null);
                String semestre = "—";
                String estado = "";
                try {
                    var u = DB.buscarUsuario(ced);
                    if (u != null) {
                        if (u.nombre != null || u.apellido != null) nombre = (u.nombre==null?"":u.nombre) + " " + (u.apellido==null?"":u.apellido);
                        if (u.semestre != null) semestre = "S" + u.semestre;
                        if (u.idPrograma != null && (programa == null || programa.isEmpty())) programa = DB.nombreProgramaPorId(u.idPrograma);
                        estado = u.estadoUsuario == null ? "" : u.estadoUsuario;
                    }
                } catch (Exception ignored) {}

                String asesor = a.nombreAsesor == null ? "" : a.nombreAsesor;
                String docente = a.nombreDocente == null ? "" : a.nombreDocente;

                model.addRow(new Object[]{ nombre, ced, programa, semestre, asesor, docente, estado });
            }
        } catch (Exception ex) {
            // Si falla, mostrar mensaje y salir
            JOptionPane.showMessageDialog(this, "No fue posible cargar los estudiantes: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JTable t = new JTable(model);
        UIFactory.styleTable(t);
        t.getColumnModel().getColumn(0).setPreferredWidth(220);
        t.getColumnModel().getColumn(2).setPreferredWidth(160);
        JScrollPane sp = UIFactory.tableScroll(t);
        sp.setPreferredSize(new Dimension(820, 320));

        JPanel panel = new JPanel(new BorderLayout(0,10));
        panel.add(UIFactory.h3("Estudiantes en " + inst.nombre), BorderLayout.NORTH);
        panel.add(sp, BorderLayout.CENTER);

        // Mostrar diálogo modal con lista de estudiantes
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Estudiantes — " + inst.nombre, true);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dlg.setSize(880, 440);
        dlg.setLocationRelativeTo(this);
        dlg.setContentPane(panel);
        dlg.setVisible(true);
    }
}
