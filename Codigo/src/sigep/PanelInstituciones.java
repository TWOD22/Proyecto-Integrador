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

        JTable table=new JTable(tableModel);
        UIFactory.styleTable(table);
        table.getColumnModel().getColumn(2).setMaxWidth(130);
        table.getColumnModel().getColumn(3).setMaxWidth(130);
        table.getColumnModel().getColumn(3).setCellRenderer(new ConvenioRenderer());
        table.getColumnModel().getColumn(4).setPreferredWidth(210);
        table.getColumnModel().getColumn(4).setCellRenderer(new InstAccionesRenderer());
        table.getColumnModel().getColumn(4).setCellEditor(new InstAccionesEditor(table));

        refreshTable();
        card.add(UIFactory.tableScroll(table),BorderLayout.CENTER);
        return card;
    }

    public void refreshTable(){
        if(tableModel==null) return;
        tableModel.setRowCount(0);
        for(Institucion i:instituciones){
            tableModel.addRow(new Object[]{
                i.nombre+"\nNIT: "+i.nit, i.telefono+"\n"+i.correo,
                i.cuposDisponibles()+" / "+i.cuposTotales,
                i.estadoConvenio?"Vigente":"Vencido", i
            });
        }
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
                java.util.List<String> allowed = DB.allowedValues("INSTITUCION_RECEPTORA", "ESTADO_CONVENIO");
                String[] opts = allowed.isEmpty() ? new String[]{"Vigente","Vencido"} : allowed.toArray(new String[0]);
                JComboBox<String> cb = new JComboBox<>(opts);
                cb.setSelectedItem(inst.estadoConvenio?opts[0]:(opts.length>1?opts[1]:opts[0]));
                int r = JOptionPane.showConfirmDialog(this, cb, "Editar estado del convenio", JOptionPane.OK_CANCEL_OPTION);
                if (r == JOptionPane.OK_OPTION) {
                    String nuevoStr = cb.getSelectedItem().toString();
                    boolean nuevo = "Vigente".equals(nuevoStr);
                    String err = DB.cambiarEstadoConvenioSafe(inst.idInstitucion, nuevoStr);
                    if (err == null) {
                        inst.estadoConvenio = nuevo;
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            JTable table = (JTable) javax.swing.SwingUtilities.getAncestorOfClass(JTable.class, p);
                            if (table != null) {
                                int modelRow = -1;
                                for (int r2 = 0; r2 < tableModel.getRowCount(); r2++) {
                                    Object cell = tableModel.getValueAt(r2, 4);
                                    if (cell instanceof Institucion ii && ii.idInstitucion != null && ii.idInstitucion.equals(inst.idInstitucion)) { modelRow = r2; break; }
                                }
                                if (modelRow >= 0) {
                                    try { tableModel.setValueAt(inst.estadoConvenio?"Vigente":"Vencido", modelRow, 3); } catch (Exception ignored) {}
                                    try { tableModel.setValueAt(inst, modelRow, 4); } catch (Exception ignored) {}
                                } else { refreshTable(); }
                                if (table.isEditing()) { try { table.getCellEditor().stopCellEditing(); } catch (Exception ignored) {} }
                                table.repaint();
                            } else { refreshTable(); }
                        });
                    } else {
                        JOptionPane.showMessageDialog(this, "No fue posible cambiar el estado en la base de datos.\nDetalle: " + err, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            estudBtn.addActionListener(e->showStudents(inst));
        }
        p.add(editBtn); p.add(editarEstadoBtn); p.add(estudBtn);
        return p;
    }

    private void showStudents(Institucion inst){
        String[][] rows={
            {"Ana María Pérez","10023456","Psicología","45 / 160 hrs","En Curso"},
            {"Carlos González","10034567","Ing. Sistemas","160 / 160 hrs","Finalizada"},
        };
        String[] cols={"Nombre","Cédula","Programa","Progreso","Estado"};
        JTable t=new JTable(new DefaultTableModel(rows,cols));
        UIFactory.styleTable(t);
        JScrollPane sp=UIFactory.tableScroll(t);
        sp.setPreferredSize(new Dimension(620,140));

        JPanel panel=new JPanel(new BorderLayout(0,10));
        panel.add(UIFactory.h3("Estudiantes en "+inst.nombre),BorderLayout.NORTH);
        panel.add(sp,BorderLayout.CENTER);
        JOptionPane.showMessageDialog(this,panel,"Estudiantes — "+inst.nombre,JOptionPane.PLAIN_MESSAGE);
    }
}
