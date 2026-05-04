package sigep;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import sigep.db.DB;

/**
 * Gestión de Asignaciones — replica AssignmentsManagement.tsx
 * Tabs: Pendientes | Asignados
 * Modal para asignar plaza + docente + asesor
 */
// Uso: panel para que el director gestione selecciones y asignaciones.
// Contiene listas de pendientes, asignados y utilidades para asignar plazas.
public class PanelAsignaciones extends JPanel {

    // ── Modelos ───────────────────────────────────────────────────────────────
    static class Pendiente {
        int id; String nombre,apellido,cedula,programa,semestre,opcion1,opcion2;
        Pendiente(int i,String n,String a,String c,String p,String s,String o1,String o2){
            id=i;nombre=n;apellido=a;cedula=c;programa=p;semestre=s;opcion1=o1;opcion2=o2;
        }
        String nombreCompleto(){ return nombre+" "+apellido; }
    }

    static class Asignado {
        @SuppressWarnings("unused")
        int id;
        String nombre,cedula,programa,institucion,estado;
        Asignado(int i,String n,String c,String p,String inst,String e){
            id=i;nombre=n;cedula=c;programa=p;institucion=inst;estado=e;
        }
    }

    static class Inst { @SuppressWarnings("unused") int id; String nombre; int cupos;
        Inst(int i,String n,int c){ id=i;nombre=n;cupos=c; }
    }
    static class Persona { @SuppressWarnings("unused") int id; String nombre;
        Persona(int i,String n){ id=i;nombre=n; }
    }

    final List<Pendiente>  pendientes =new ArrayList<>();
    final List<Asignado>   asignados  =new ArrayList<>();
    final List<Inst>       insts      =new ArrayList<>();
    final List<Persona>    docentes   =new ArrayList<>();
    final List<Persona>    asesores   =new ArrayList<>();

    private DefaultTableModel pendModel, asigModel;
    private JTabbedPane tabs;

    public PanelAsignaciones(){
        pendientes.clear(); asignados.clear(); insts.clear(); docentes.clear(); asesores.clear();
        try {
            var sels = DB.seleccionesPendientes();
            int idc = 1;
            for (var s : sels) {
                pendientes.add(new Pendiente(idc++, s.nombreEstudiante == null ? "Estudiante" : s.nombreEstudiante, "", s.cedulaEstudiante, "", "", s.nombreOp1, s.nombreOp2));
            }
            var asigs = DB.asignacionesActivas();
            idc = 1;
            for (var a : asigs) {
                asignados.add(new Asignado(idc++, a.nombreEstudiante == null ? "Estudiante" : a.nombreEstudiante, a.cedulaEstudiante, a.programaEstudiante, a.nombreInstitucion == null ? a.idInstitucion : a.nombreInstitucion, a.estadoAsignacion));
            }
            var instsDb = DB.listarInstituciones();
            int idx = 1; for (var ii : instsDb) insts.add(new Inst(idx++, ii.nombre, ii.cuposDisponibles()));
        } catch (Exception ignored) {}
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(Theme.PADDING,Theme.PADDING,Theme.PADDING,Theme.PADDING));
        build();
    }

    /** Recarga la data desde la base de datos y actualiza las vistas. */
    public void refreshFromDB(){
        pendientes.clear(); asignados.clear(); insts.clear(); docentes.clear(); asesores.clear();
        try {
            var sels = DB.seleccionesPendientes();
            int idc = 1;
            for (var s : sels) {
                pendientes.add(new Pendiente(idc++, s.nombreEstudiante == null ? "Estudiante" : s.nombreEstudiante, "", s.cedulaEstudiante, "", "", s.nombreOp1, s.nombreOp2));
            }
            var asigs = DB.asignacionesActivas();
            idc = 1;
            for (var a : asigs) {
                asignados.add(new Asignado(idc++, a.nombreEstudiante == null ? "Estudiante" : a.nombreEstudiante, a.cedulaEstudiante, a.programaEstudiante, a.nombreInstitucion == null ? a.idInstitucion : a.nombreInstitucion, a.estadoAsignacion));
            }
            var instsDb = DB.listarInstituciones();
            int idx = 1; for (var ii : instsDb) insts.add(new Inst(idx++, ii.nombre, ii.cuposDisponibles()));
        } catch (Exception ignored) {}
        refreshPendientes(); refreshAsignados();
    }

    

    // Construye la interfaz principal del panel (header + pestañas).
    private void build(){
        JPanel h=UIFactory.transparent(new BorderLayout());
        h.setBorder(new EmptyBorder(0,0,12,0));
        JPanel left=UIFactory.transparent(new FlowLayout(FlowLayout.LEFT,8,0));
        JLabel icon=new JLabel("📋"); icon.setFont(new Font("Segoe UI Emoji",Font.PLAIN,20));
        JPanel titleCol=UIFactory.transparent(new GridLayout(2,1,0,2));
        titleCol.add(UIFactory.h2("Gestión de Asignaciones"));
        titleCol.add(UIFactory.muted("Asigne plazas de práctica a los estudiantes habilitados."));
        left.add(icon); left.add(titleCol);
        h.add(left,BorderLayout.WEST);
        JPanel topBlock=UIFactory.transparent(new BorderLayout());
        topBlock.add(h,BorderLayout.NORTH); topBlock.add(UIFactory.hSep(),BorderLayout.SOUTH);
        add(topBlock,BorderLayout.NORTH);

        // Tabs
        tabs=buildTabs();
        add(tabs,BorderLayout.CENTER);
    }

    // Crea las pestañas (Pendientes, Asignados) y las llena con sus paneles.
    private JTabbedPane buildTabs(){
        JTabbedPane tp=new JTabbedPane();
        tp.setFont(Theme.FONT_LABEL);
        tp.addTab("⏳  Pendientes ("+pendientes.size()+")", buildPendientesPanel());
        tp.addTab("✅  Asignados ("+asignados.size()+")",  buildAsignadosPanel());
        return tp;
    }

    // ── Tab Pendientes ────────────────────────────────────────────────────────

    // Construye el panel que muestra las selecciones pendientes en una tabla.
    private JPanel buildPendientesPanel(){
        JPanel p=new JPanel(new BorderLayout());
        p.setOpaque(false); p.setBorder(new EmptyBorder(12,0,0,0));

        // Action bar con búsqueda
        JPanel bar=new JPanel(new BorderLayout());
        bar.setBackground(new Color(0xf9fafb));
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER),
            new EmptyBorder(8,14,8,14)));

        JPanel barLeft=UIFactory.transparent(new FlowLayout(FlowLayout.LEFT,8,0));
        JLabel barTitle=UIFactory.h3("Estudiantes Habilitados para Práctica");
        barLeft.add(barTitle);
        bar.add(barLeft,BorderLayout.WEST);

        JTextField search=UIFactory.searchField("Buscar estudiante...");
        search.setPreferredSize(new Dimension(220,32));
        bar.add(search,BorderLayout.EAST);

        p.add(bar,BorderLayout.NORTH);

        // Table
        String[] cols={"Estudiante","Programa","Preferencias de Plaza","Estado","Acciones"};
        pendModel=new DefaultTableModel(cols,0){
            @Override public boolean isCellEditable(int r,int c){ return c==4; }
        };

        JTable t=new JTable(pendModel);
        UIFactory.styleTable(t);
        t.getColumnModel().getColumn(3).setMaxWidth(100);
        t.getColumnModel().getColumn(3).setCellRenderer(new ListoBadgeRenderer());
        t.getColumnModel().getColumn(4).setPreferredWidth(180);
        t.getColumnModel().getColumn(4).setCellRenderer(new PendAccionesRenderer());
        t.getColumnModel().getColumn(4).setCellEditor(new PendAccionesEditor(t));

        refreshPendientes();
        p.add(UIFactory.tableScroll(t),BorderLayout.CENTER);
        return p;
    }

    // Rellena el modelo de la tabla de pendientes desde la lista `pendientes`.
    void refreshPendientes(){
        if(pendModel==null) return;
        pendModel.setRowCount(0);
        for(Pendiente s:pendientes){
            pendModel.addRow(new Object[]{
                s.nombreCompleto()+"\nC.C: "+s.cedula,
                s.programa+" • "+s.semestre,
                "1. "+s.opcion1+"\n2. "+s.opcion2,
                "Listo", s
            });
        }
        if(tabs!=null) tabs.setTitleAt(0,"⏳  Pendientes ("+pendientes.size()+")");
    }

    class ListoBadgeRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int row,int col){
            JLabel badge=UIFactory.badgeListo();
            JPanel p=UIFactory.transparent(new FlowLayout(FlowLayout.CENTER,0,9));
            p.setBackground(s?Theme.PRIMARY_LIGHT:(row%2==0?Color.WHITE:new Color(0xfafafa)));
            p.add(badge); return p;
        }
    }

    class PendAccionesRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int row,int col){
            return buildPendAcciones((Pendiente)v,row,false);
        }
    }
    class PendAccionesEditor extends AbstractCellEditor implements TableCellEditor {
        PendAccionesEditor(JTable t){}
        @Override public Object getCellEditorValue(){ return null; }
        @Override public Component getTableCellEditorComponent(JTable t,Object v,boolean s,int row,int col){
            return buildPendAcciones((Pendiente)v,row,true);
        }
    }
    // Construye el panel con los botones de acción para una fila de pendientes.
    private JPanel buildPendAcciones(Pendiente s, int row, boolean live){
        JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,6,7));
        p.setBackground(row%2==0?Color.WHITE:new Color(0xfafafa));
        JButton verBtn=UIFactory.outlineBtn("👤 Ver Perfil");
        JButton asigBtn=UIFactory.primaryBtn("Asignar Plaza →");
        if(live){
            verBtn.addActionListener(e->{ /* Ver perfil */ });
            asigBtn.addActionListener(e->showAsignarModal(s));
        }
        p.add(verBtn); p.add(asigBtn);
        return p;
    }

    // ── Tab Asignados ─────────────────────────────────────────────────────────

    // Construye el panel que muestra las asignaciones realizadas.
    private JPanel buildAsignadosPanel(){
        JPanel p=new JPanel(new BorderLayout());
        p.setOpaque(false); p.setBorder(new EmptyBorder(12,0,0,0));

        String[] cols={"Estudiante","Programa","Institución Receptora","Estado Práctica","Acciones"};
        asigModel=new DefaultTableModel(cols,0){
            @Override public boolean isCellEditable(int r,int c){ return c==4; }
        };

        JTable t=new JTable(asigModel);
        UIFactory.styleTable(t);
        t.getColumnModel().getColumn(3).setMaxWidth(130);
        t.getColumnModel().getColumn(3).setCellRenderer(new EstadoPracticaRenderer());
        t.getColumnModel().getColumn(4).setPreferredWidth(120);
        t.getColumnModel().getColumn(4).setCellRenderer(new AsigAccionesRenderer());
        t.getColumnModel().getColumn(4).setCellEditor(new AsigAccionesEditor(t));

        refreshAsignados();
        p.add(UIFactory.tableScroll(t),BorderLayout.CENTER);
        return p;
    }

    // Rellena el modelo de la tabla de asignados desde la lista `asignados`.
    void refreshAsignados(){
        if(asigModel==null) return;
        asigModel.setRowCount(0);
        for(Asignado s:asignados)
            asigModel.addRow(new Object[]{ s.nombre+"\nC.C: "+s.cedula, s.programa, "🏢 "+s.institucion, s.estado, s });
        if(tabs!=null) tabs.setTitleAt(1,"✅  Asignados ("+asignados.size()+")");
    }

    class EstadoPracticaRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int row,int col){
            String val=v==null?"":v.toString();
            JLabel badge=val.equals("En Curso")?UIFactory.badgeEnCurso():UIFactory.badgeFinalizada();
            JPanel p=UIFactory.transparent(new FlowLayout(FlowLayout.CENTER,0,9));
            p.setBackground(s?Theme.PRIMARY_LIGHT:(row%2==0?Color.WHITE:new Color(0xfafafa)));
            p.add(badge); return p;
        }
    }
    class AsigAccionesRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int row,int col){
            JPanel p=new JPanel(new FlowLayout(FlowLayout.CENTER,0,7));
            p.setBackground(row%2==0?Color.WHITE:new Color(0xfafafa));
            p.add(UIFactory.outlineBtn("👤 Ver Perfil")); return p;
        }
    }
    class AsigAccionesEditor extends AbstractCellEditor implements TableCellEditor {
        AsigAccionesEditor(JTable t){}
        @Override public Object getCellEditorValue(){ return null; }
        @Override public Component getTableCellEditorComponent(JTable t,Object v,boolean s,int row,int col){
            JPanel p=new JPanel(new FlowLayout(FlowLayout.CENTER,0,7));
            p.setBackground(row%2==0?Color.WHITE:new Color(0xfafafa));
            JButton b=UIFactory.outlineBtn("👤 Ver Perfil");
            b.addActionListener(e->{ /* perfil */ });
            p.add(b); return p;
        }
    }

    // ── Modal de Asignación ───────────────────────────────────────────────────

    // Muestra un diálogo modal para asignar plaza/docente/asesor al estudiante.
    private void showAsignarModal(Pendiente est){
        JDialog dlg=new JDialog((Frame)SwingUtilities.getWindowAncestor(this),"Asignar Plaza Institucional",true);
        dlg.setSize(520,470);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel root=new JPanel(new BorderLayout());
        dlg.setContentPane(root);

        // Header azul
        JPanel header=new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY);
        header.setBorder(new EmptyBorder(14,20,14,20));
        JPanel hLeft=UIFactory.transparent(new GridLayout(2,1,0,2));
        JLabel hName=new JLabel("🏢  Asignar Plaza Institucional");
        hName.setFont(Theme.FONT_H3); hName.setForeground(Color.WHITE);
        hLeft.add(hName);
        header.add(hLeft,BorderLayout.WEST);
        JButton closeBtn=new JButton("✕");
        closeBtn.setFont(Theme.FONT_BODY); closeBtn.setForeground(new Color(0xbfdbfe));
        closeBtn.setOpaque(false); closeBtn.setContentAreaFilled(false); closeBtn.setBorderPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e->dlg.dispose());
        header.add(closeBtn,BorderLayout.EAST);
        root.add(header,BorderLayout.NORTH);

        // Body
        JPanel body=new JPanel();
        body.setBackground(Color.WHITE);
        body.setLayout(new BoxLayout(body,BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(20,24,20,24));

        // Info estudiante (bg-blue-50)
        JPanel infoBox=new JPanel(new GridLayout(3,1,0,4));
        infoBox.setBackground(new Color(0xeff6ff));
        infoBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xbfdbfe)),
            new EmptyBorder(12,14,12,14)));
        infoBox.setMaximumSize(new Dimension(Integer.MAX_VALUE,90));

        JLabel estLabel=UIFactory.sectionLabel("Estudiante Seleccionado");
        JLabel estName=new JLabel(est.nombreCompleto());
        estName.setFont(Theme.FONT_H3); estName.setForeground(Theme.TEXT_PRIMARY);
        JLabel estSub=UIFactory.muted("C.C: "+est.cedula+" • "+est.programa);
        infoBox.add(estLabel); infoBox.add(estName); infoBox.add(estSub);
        body.add(infoBox);

        // Preferencias
        if(est.opcion1!=null && !est.opcion1.isEmpty()){
            body.add(UIFactory.gap(12));
            JPanel prefBox=new JPanel(new GridLayout(3,1,0,2));
            prefBox.setOpaque(false);
            prefBox.setMaximumSize(new Dimension(Integer.MAX_VALUE,70));
            prefBox.add(UIFactory.sectionLabel("Preferencias del Estudiante"));
            prefBox.add(UIFactory.body("1.  "+est.opcion1));
            prefBox.add(UIFactory.body("2.  "+est.opcion2));
            body.add(prefBox);
        }

        body.add(UIFactory.gap(14));

        // Selects de asignación
        String[] instNames=insts.stream().map(i->i.nombre+" ("+i.cupos+" cupos)").toArray(String[]::new);
        String[] docenteNames=docentes.stream().map(d->d.nombre).toArray(String[]::new);
        String[] asesorNames=asesores.stream().map(a->a.nombre).toArray(String[]::new);

        String[] instOpts=new String[instNames.length+1]; instOpts[0]="Seleccione una institución...";
        System.arraycopy(instNames,0,instOpts,1,instNames.length);
        String[] docOpts=new String[docenteNames.length+1]; docOpts[0]="Seleccione un docente...";
        System.arraycopy(docenteNames,0,docOpts,1,docenteNames.length);
        String[] aseOpts=new String[asesorNames.length+1]; aseOpts[0]="Seleccione un asesor...";
        System.arraycopy(asesorNames,0,aseOpts,1,asesorNames.length);

        JComboBox<String> instBox=UIFactory.comboBox(instOpts);
        JComboBox<String> docBox=UIFactory.comboBox(docOpts);
        JComboBox<String> aseBox=UIFactory.comboBox(aseOpts);

        JPanel selGrid=new JPanel(new GridLayout(3,1,0,10));
        selGrid.setOpaque(false);
        selGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE,120));
        selGrid.add(UIFactory.labeledField("Institución Receptora Disponible",instBox));
        selGrid.add(UIFactory.labeledField("Docente Encargado",docBox));
        selGrid.add(UIFactory.labeledField("Asesor Asignado",aseBox));
        body.add(selGrid);

        JScrollPane bodyScroll=new JScrollPane(body);
        bodyScroll.setBorder(BorderFactory.createEmptyBorder());
        root.add(bodyScroll,BorderLayout.CENTER);

        // Footer
        JPanel footer=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,10));
        footer.setBackground(new Color(0xf9fafb));
        footer.setBorder(BorderFactory.createMatteBorder(1,0,0,0,Theme.BORDER));

        JButton cancelBtn=UIFactory.outlineBtn("Cancelar");
        cancelBtn.addActionListener(e->dlg.dispose());

        JButton confirmBtn=UIFactory.primaryBtn("💾  Confirmar Asignación");
        confirmBtn.addActionListener(e->{
            if(instBox.getSelectedIndex()==0||docBox.getSelectedIndex()==0||aseBox.getSelectedIndex()==0){
                JOptionPane.showMessageDialog(dlg,"Complete todos los campos.","Error",JOptionPane.WARNING_MESSAGE);
                return;
            }
            int instIdx=instBox.getSelectedIndex()-1;
            Inst inst=insts.get(instIdx);
            if(inst.cupos>0) inst.cupos--;

            asignados.add(new Asignado(est.id,est.nombreCompleto(),est.cedula,est.programa,inst.nombre,"En Curso"));
            pendientes.removeIf(s->s.id==est.id);
            refreshPendientes(); refreshAsignados();
            dlg.dispose();
        });

        footer.add(cancelBtn); footer.add(confirmBtn);
        root.add(footer,BorderLayout.SOUTH);
        dlg.setVisible(true);
    }
}
