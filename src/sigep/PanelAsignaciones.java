package sigep;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import sigep.db.DB;
import sigep.db.UsuarioDAO;

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
        String idAsignacion;
        String nombre,cedula,programa,institucion,estado;
        String nota; // mensaje adicional: falta docente/asesor
        Asignado(String idAsig,String n,String c,String p,String inst,String e){
            idAsignacion = idAsig; nombre=n;cedula=c;programa=p;institucion=inst;estado=e;
        }
    }

    static class Inst { @SuppressWarnings("unused") int id; String nombre; int cupos;
        Inst(int i,String n,int c){ id=i;nombre=n;cupos=c; }
    }
    static class Persona { String cedula; String nombre;
        Persona(String ced,String n){ cedula=ced; nombre=n; }
        @Override public String toString(){ return nombre==null?"":nombre; }
    }

    final List<Pendiente>  pendientes =new ArrayList<>();
    final List<Asignado>   asignados  =new ArrayList<>();
    final List<Inst>       insts      =new ArrayList<>();
    final List<Persona>    docentes   =new ArrayList<>();
    final List<Persona>    asesores   =new ArrayList<>();

    private DefaultTableModel pendModel, asigDocModel, asigFinalModel;
    private JTabbedPane tabs;
    private JTable pendTable, asigDocTable, asigFinalTable;
    private javax.swing.Timer autoRefreshTimer;
    // Callback invoked when the director requests to view a student's profile.
    public java.util.function.Consumer<UsuarioDAO.Usuario> onVerPerfil = null;

    public PanelAsignaciones(){
        pendientes.clear(); asignados.clear(); insts.clear(); docentes.clear(); asesores.clear();
        try {
            // Limpiar duplicados en la tabla de selecciones por si hay errores previos
        DB.limpiarSeleccionDuplicadosGlobal(); 
            var sels = DB.seleccionesPendientes();
            // DEBUG: imprimir selecciones recuperadas
            try { System.err.println("[DEBUG PEND] seleccionesPendientes fetched: " + (sels==null?0:sels.size())); if (sels!=null) for (var ss: sels) System.err.println("[DEBUG PEND] SEL ced="+ss.cedulaEstudiante+" op1="+ss.nombreOp1+" op2="+ss.nombreOp2); } catch(Exception ignored) {}
            int idc = 1;
            java.util.Set<String> seenPend = new java.util.HashSet<>();
            for (var s : sels) {
                if (s.cedulaEstudiante != null && seenPend.contains(s.cedulaEstudiante)) continue;
                if (s.cedulaEstudiante != null) seenPend.add(s.cedulaEstudiante);
                String prog="", sem="";
                String ced = s.cedulaEstudiante;
                try { var u = DB.buscarUsuario(ced); if (u!=null) { prog = DB.nombreProgramaPorId(u.idPrograma); sem = String.valueOf(u.semestre); } } catch (Exception ignored) {}
                String nombre = s.nombreEstudiante == null ? "Estudiante" : s.nombreEstudiante;
                pendientes.add(new Pendiente(idc++, nombre, "", ced, prog, sem, s.nombreOp1, s.nombreOp2));
            }
            var asigs = DB.asignacionesActivas();
            // DEBUG: imprimir filas traídas desde la BD para diagnóstico visual
            try {
                System.err.println("[DEBUG] asignacionesActivas fetched: " + (asigs==null?0:asigs.size()));
                if (asigs != null) for (var da : asigs) System.err.println("[DEBUG] ASIG FETCH id=" + da.idAsignacion + " ced=" + da.cedulaEstudiante + " inst=" + da.idInstitucion + " estado=" + da.estadoAsignacion);
            } catch (Exception ignored) {}
            idc = 1;
            java.util.Set<String> seenCedulas = new java.util.HashSet<>();
            for (var a : asigs) {
                // Evitar mostrar más de una asignación por estudiante en la vista
                if (a.cedulaEstudiante != null && seenCedulas.contains(a.cedulaEstudiante)) continue;
                if (a.cedulaEstudiante != null) seenCedulas.add(a.cedulaEstudiante);
                String nota = "";
                boolean faltaDoc = a.cedulaDocente == null || a.cedulaDocente.isEmpty();
                boolean faltaAse = a.cedulaAsesor == null || a.cedulaAsesor.isEmpty();
                if (faltaDoc && faltaAse) nota = "Falta docente y asesor";
                else if (faltaDoc) nota = "Falta docente";
                else if (faltaAse) nota = "Falta asesor";
                String idAsig = a.idAsignacion;
                String nombreEst = a.nombreEstudiante == null ? "Estudiante" : a.nombreEstudiante;
                String nombreInst = a.nombreInstitucion == null ? a.idInstitucion : a.nombreInstitucion;
                Asignado as = new Asignado(idAsig, nombreEst, a.cedulaEstudiante, a.programaEstudiante, nombreInst, a.estadoAsignacion);
                as.nota = nota;
                asignados.add(as);
            }
            var instsDb = DB.listarInstituciones();
            int idx = 1; for (var ii : instsDb) insts.add(new Inst(idx++, ii.nombre, ii.cuposDisponibles()));
            // Cargar docentes y asesores disponibles (una sola vez)
            try { var docs = DB.listarPorRol("Docente"); for (var du : docs) docentes.add(new Persona(du.cedula, du.nombre + (du.apellido==null?"":" "+du.apellido))); } catch (Exception ignored) {}
            try { var ases = DB.listarPorRol("Asesor"); for (var au : ases) asesores.add(new Persona(au.cedula, au.nombre + (au.apellido==null?"":" "+au.apellido))); } catch (Exception ignored) {}
        } catch (Exception ignored) {}
        // Escuchar cambios en selecciones para refrescar la lista de pendientes en tiempo real
        DB.addChangeListener(topic -> { if ("selecciones".equals(topic)) { SwingUtilities.invokeLater(this::refreshFromDB); } });
        // Auto-refresh periódicamente para detectar cambios hechos fuera de esta instancia
        autoRefreshTimer = new javax.swing.Timer(10_000, ev -> SwingUtilities.invokeLater(this::refreshFromDB));
        autoRefreshTimer.setRepeats(true);
        autoRefreshTimer.start();
        // Detener timer cuando el panel deje de ser displayable
        this.addHierarchyListener(e -> {
            boolean displayable = (e.getChangeFlags() & java.awt.event.HierarchyEvent.DISPLAYABILITY_CHANGED) != 0 ? this.isDisplayable() : this.isDisplayable();
            if (!displayable && autoRefreshTimer != null && autoRefreshTimer.isRunning()) autoRefreshTimer.stop();
            if (displayable && autoRefreshTimer != null && !autoRefreshTimer.isRunning()) autoRefreshTimer.start();
        });
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(Theme.PADDING,Theme.PADDING,Theme.PADDING,Theme.PADDING));
        build();
    }

    /** Recarga la data desde la base de datos y actualiza las vistas. */
    public void refreshFromDB(){
        pendientes.clear(); asignados.clear(); insts.clear(); docentes.clear(); asesores.clear();
        try {
        // Limpiar duplicados en la tabla de selecciones por si hay errores previos
        DB.limpiarSeleccionDuplicadosGlobal(); 
            var sels = DB.seleccionesPendientes();
            // DEBUG: imprimir selecciones en refresh
            try { System.err.println("[DEBUG PEND REFRESH] seleccionesPendientes fetched: " + (sels==null?0:sels.size())); if (sels!=null) for (var ss: sels) System.err.println("[DEBUG PEND REFRESH] SEL ced="+ss.cedulaEstudiante+" op1="+ss.nombreOp1+" op2="+ss.nombreOp2); } catch(Exception ignored) {}
            int idc = 1;
            java.util.Set<String> seenPend = new java.util.HashSet<>();
            for (var s : sels) {
                if (s.cedulaEstudiante != null && seenPend.contains(s.cedulaEstudiante)) continue;
                if (s.cedulaEstudiante != null) seenPend.add(s.cedulaEstudiante);
                String prog="", sem="";
                String ced = s.cedulaEstudiante;
                try { var u = DB.buscarUsuario(ced); if (u!=null) { prog = DB.nombreProgramaPorId(u.idPrograma); sem = String.valueOf(u.semestre); } } catch (Exception ignored) {}
                String nombre = s.nombreEstudiante == null ? "Estudiante" : s.nombreEstudiante;
                pendientes.add(new Pendiente(idc++, nombre, "", ced, prog, sem, s.nombreOp1, s.nombreOp2));
            }
            var asigs = DB.asignacionesActivas();
            // DEBUG: imprimir filas traídas desde la BD para diagnóstico en refresh
            try {
                System.err.println("[DEBUG REFRESH] asignacionesActivas fetched: " + (asigs==null?0:asigs.size()));
                if (asigs != null) for (var da : asigs) System.err.println("[DEBUG REFRESH] ASIG id=" + da.idAsignacion + " ced=" + da.cedulaEstudiante + " inst=" + da.idInstitucion + " estado=" + da.estadoAsignacion);
            } catch (Exception ignored) {}
            idc = 1;
            java.util.Set<String> seenCedulas = new java.util.HashSet<>();
            for (var a : asigs) {
                // Evitar mostrar más de una asignación por estudiante en la vista
                if (a.cedulaEstudiante != null && seenCedulas.contains(a.cedulaEstudiante)) continue;
                if (a.cedulaEstudiante != null) seenCedulas.add(a.cedulaEstudiante);
                String nota = "";
                boolean faltaDoc = a.cedulaDocente == null || a.cedulaDocente.isEmpty();
                boolean faltaAse = a.cedulaAsesor == null || a.cedulaAsesor.isEmpty();
                if (faltaDoc && faltaAse) nota = "Falta docente y asesor";
                else if (faltaDoc) nota = "Falta docente";
                else if (faltaAse) nota = "Falta asesor";
                String idAsig = a.idAsignacion;
                String nombreEst = a.nombreEstudiante == null ? "Estudiante" : a.nombreEstudiante;
                String nombreInst = a.nombreInstitucion == null ? a.idInstitucion : a.nombreInstitucion;
                Asignado as = new Asignado(idAsig, nombreEst, a.cedulaEstudiante, a.programaEstudiante, nombreInst, a.estadoAsignacion);
                as.nota = nota;
                asignados.add(as);
            }
            var instsDb = DB.listarInstituciones();
            int idx = 1; for (var ii : instsDb) insts.add(new Inst(idx++, ii.nombre, ii.cuposDisponibles()));
        } catch (Exception ignored) {}
        refreshPendientes(); refreshAsignadosDocentes(); refreshAsignadosFinales();
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
        // Botón de diagnóstico visible en la cabecera principal
        JButton mainDbBtn = UIFactory.outlineBtn("DB");
        mainDbBtn.setToolTipText("Mostrar estado de la BD (usuario, conteos)");
        mainDbBtn.addActionListener(ev -> JOptionPane.showMessageDialog(this, DB.debugStatus(), "Estado BD", JOptionPane.INFORMATION_MESSAGE));
        // Botón para forzar refresco de la vista (útil cuando varias instancias o datos quedan desincronizados)
        JButton refreshBtn = UIFactory.outlineBtn("Refrescar");
        refreshBtn.setToolTipText("Refrescar listado de asignaciones desde la base de datos");
        refreshBtn.addActionListener(ev -> {
            System.err.println("[USER ACTION] Refrescando PanelAsignaciones desde UI");
            SwingUtilities.invokeLater(this::refreshFromDB);
        });
        JPanel right = UIFactory.transparent(new FlowLayout(FlowLayout.RIGHT,0,0));
        right.add(mainDbBtn); right.add(UIFactory.gap(8)); right.add(refreshBtn);
        h.add(right, BorderLayout.EAST);
        JPanel topBlock=UIFactory.transparent(new BorderLayout());
        topBlock.add(h,BorderLayout.NORTH); topBlock.add(UIFactory.hSep(),BorderLayout.SOUTH);
        add(topBlock,BorderLayout.NORTH);

        // Tabs
        tabs=buildTabs();
        add(tabs,BorderLayout.CENTER);
        // Ensure initial painting and layout so row buttons render immediately
        SwingUtilities.invokeLater(() -> { tabs.revalidate(); tabs.repaint(); });
    }

    // Crea las pestañas (Pendientes, Asignados) y las llena con sus paneles.
    private JTabbedPane buildTabs(){
        JTabbedPane tp=new JTabbedPane();
        tp.setFont(Theme.FONT_LABEL);
        tp.addTab("⏳  Asignar Plaza ("+pendientes.size()+")", buildPendientesPanel());
        tp.addTab("🧑‍🏫  Asignar Docente/Asesor ("+asignados.size()+")", buildAsignadosDocPanel());
        tp.addTab("🏁  En Práctica/Finalizada ("+asignados.size()+")", buildAsignadosFinalPanel());
        // Asegurar que al cambiar de pestaña los conteos y tablas se refresquen (evita desincronía visual)
        tp.addChangeListener(e -> SwingUtilities.invokeLater(this::refreshFromDB));
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

        // Table: Nombre | Programa | Semestre | Preferencia de Plaza | Acciones
        String[] cols={"Nombre","Programa Académico","Semestre","Preferencia de Plaza","Acciones"};
        pendModel=new DefaultTableModel(cols,0){
            @Override public boolean isCellEditable(int r,int c){ return c==4; }
        };

        pendTable = new JTable(pendModel);
        UIFactory.styleTable(pendTable);
        // Acciones (col 4)
        pendTable.getColumnModel().getColumn(4).setPreferredWidth(220);
        pendTable.getColumnModel().getColumn(4).setCellRenderer(new PendAccionesRenderer());

        pendTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = pendTable.rowAtPoint(e.getPoint());
                int col = pendTable.columnAtPoint(e.getPoint());
                if (row < 0 || col != 4) return;
                Object cell = pendTable.getModel().getValueAt(pendTable.convertRowIndexToModel(row), 4);
                if (!(cell instanceof Pendiente)) return;
                Pendiente s = (Pendiente) cell;
                Rectangle cellRect = pendTable.getCellRect(row, col, false);
                int relX = e.getX() - cellRect.x;
                int cellW = cellRect.width; int third = Math.max(1, cellW/3);
                if (relX < third) {
                    // Ver perfil: intentar abrir perfil vía callback si está disponible
                    try {
                        var u = DB.buscarUsuario(s.cedula);
                        if (onVerPerfil != null) { onVerPerfil.accept(u); }
                        else { if (u!=null) JOptionPane.showMessageDialog(PanelAsignaciones.this, "Perfil: " + u.nombreCompleto()); }
                    } catch (Exception ignored) {}
                    return;
                }
                if (relX < 2*third) { showAsignarModal(s, true); return; }
                // delete
                int resp = JOptionPane.showConfirmDialog(PanelAsignaciones.this, "¿Eliminar la selección de '"+s.nombreCompleto()+"'?\nEl estudiante podrá enviar otra solicitud.", "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
                if (resp==JOptionPane.YES_OPTION) {
                    boolean ok = DB.eliminarSeleccionPorEstudiante(s.cedula);
                    if (!ok) { JOptionPane.showMessageDialog(PanelAsignaciones.this, "No fue posible eliminar la selección. Revisa los logs.", "Error", JOptionPane.ERROR_MESSAGE); return; }
                    SwingUtilities.invokeLater(() -> { refreshFromDB(); JOptionPane.showMessageDialog(PanelAsignaciones.this, "Selección eliminada."); });
                }
            }
        });
        // Renderizar preferencias en dos líneas: opción 2 debajo de opción 1
        pendTable.getColumnModel().getColumn(3).setCellRenderer(new PreferencesRenderer());
        // Ajuste visual de anchos
        pendTable.getColumnModel().getColumn(0).setPreferredWidth(280);
        pendTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        pendTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        // Renderizar preferencias en múltiples líneas y ajustar alto de fila dinámicamente

        refreshPendientes();
        p.add(UIFactory.tableScroll(pendTable),BorderLayout.CENTER);
        return p;
    }

    // Renderer para mostrar las preferencias en múltiples líneas
    class PreferencesRenderer extends DefaultTableCellRenderer {
        private final JTextArea area = new JTextArea();
        PreferencesRenderer(){
            area.setLineWrap(true); area.setWrapStyleWord(true); area.setOpaque(false);
            area.setFont(Theme.FONT_BODY);
        }
        @Override public Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row,int column){
            String text = value==null?"":value.toString();
            area.setText(text.replace("\\n", System.lineSeparator()));
            // Ajustar ancho del area al ancho real de la columna para calcular altura preferida
            int colWidth = table.getColumnModel().getColumn(column).getWidth();
            area.setSize(new Dimension(colWidth, Short.MAX_VALUE));
            int prefH = area.getPreferredSize().height;
            int targetH = Math.max(42, prefH + 12); // mínimo 42, añadir padding
            if (table.getRowHeight(row) != targetH) table.setRowHeight(row, targetH);

            JPanel p = UIFactory.transparent(new FlowLayout(FlowLayout.LEFT,6,6));
            p.setBackground(isSelected?Theme.PRIMARY_LIGHT:(row%2==0?Color.WHITE:new Color(0xfafafa)));
            p.add(area);
            return p;
        }
    }

    // Rellena el modelo de la tabla de pendientes desde la lista `pendientes`.
    void refreshPendientes(){
        if(pendModel==null) return;
        pendModel.setRowCount(0);
        for(Pendiente s:pendientes){
            pendModel.addRow(new Object[]{
                s.nombreCompleto(),
                s.programa,
                s.semestre == null ? "" : s.semestre,
                "1. "+s.opcion1+"\n2. "+s.opcion2,
                s
            });
        }
        if(tabs!=null) tabs.setTitleAt(0,"⏳  Pendientes ("+pendientes.size()+")");
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
        JButton delBtn=UIFactory.dangerBtn("Eliminar");
        if(live){
            verBtn.addActionListener(e->{ /* Ver perfil */ });
            asigBtn.addActionListener(e->showAsignarModal(s, true));
            delBtn.addActionListener(e->{
                int resp = JOptionPane.showConfirmDialog(this, "¿Eliminar la selección de '"+s.nombreCompleto()+"'?\nEl estudiante podrá enviar otra solicitud.", "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
                if(resp==JOptionPane.YES_OPTION){
                    boolean ok = DB.eliminarSeleccionPorEstudiante(s.cedula);
                    if(!ok) {
                        JOptionPane.showMessageDialog(this, "No fue posible eliminar la selección. Revisa los logs.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    // Stop cell editing if the button is inside a table editor so the editor view is removed
                    Component src = (Component) e.getSource();
                    JTable table = (JTable) SwingUtilities.getAncestorOfClass(JTable.class, src);
                    if (table != null && table.isEditing()) {
                        try { table.getCellEditor().stopCellEditing(); } catch (Exception ex) { try { table.getCellEditor().cancelCellEditing(); } catch (Exception ignored) {} }
                    }
                    // Refresh data on EDT
                    SwingUtilities.invokeLater(() -> { refreshFromDB(); JOptionPane.showMessageDialog(this, "Selección eliminada."); });
                }
            });
        }
        p.add(verBtn); p.add(asigBtn); p.add(delBtn);
        return p;
    }

    // ── Tab 2: Asignar Docente/Asesor ─────────────────────────────────────────
    private JPanel buildAsignadosDocPanel(){
        JPanel p=new JPanel(new BorderLayout());
        p.setOpaque(false); p.setBorder(new EmptyBorder(12,0,0,0));

        String[] cols={"Estudiante","Programa","Institución Receptora","Acciones"};
        asigDocModel=new DefaultTableModel(cols,0){
            @Override public boolean isCellEditable(int r,int c){ return c==3; }
        };

        asigDocTable = new JTable(asigDocModel);
        UIFactory.styleTable(asigDocTable);
        // Removed the explicit "Estado" column. Actions are now at column index 3.
        asigDocTable.getColumnModel().getColumn(3).setPreferredWidth(220);
        asigDocTable.getColumnModel().getColumn(3).setCellRenderer(new AsigAccionesRenderer());

        asigDocTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = asigDocTable.rowAtPoint(e.getPoint());
                int col = asigDocTable.columnAtPoint(e.getPoint());
                if (row < 0 || col != 3) return;
                Object cell = asigDocTable.getModel().getValueAt(asigDocTable.convertRowIndexToModel(row), 3);
                if (!(cell instanceof Asignado)) return;
                Asignado a = (Asignado) cell;
                Rectangle cellRect = asigDocTable.getCellRect(row, col, false);
                int relX = e.getX() - cellRect.x; int cellW = cellRect.width; int third = Math.max(1, cellW/3);
                if (relX < third) {
                    try {
                        var u = DB.buscarUsuario(a.cedula);
                        if (onVerPerfil != null) { onVerPerfil.accept(u); }
                        else { if (u!=null) JOptionPane.showMessageDialog(PanelAsignaciones.this, "Perfil: " + u.nombreCompleto()); }
                    } catch (Exception ignored) {}
                    return;
                }
                if (relX < 2*third) { /* Assign Doc/Asesor */ showDocAsesorModal(a); return; }
                // delete
                int resp = JOptionPane.showConfirmDialog(PanelAsignaciones.this, "¿Eliminar la asignación de '"+a.nombre+"'?\nEsto devolverá el cupo.", "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
                if (resp != JOptionPane.YES_OPTION) return;
                new Thread(() -> {
                    boolean ok = false;
                    try { ok = DB.eliminarAsignacionPorId(a.idAsignacion); } catch (Exception ex) { ok = false; }
                    final boolean res = ok;
                    SwingUtilities.invokeLater(() -> {
                        if (!res) {
                            JOptionPane.showMessageDialog(PanelAsignaciones.this, "La asignación no se encontró en la base de datos. Se refrescará la vista.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(PanelAsignaciones.this, "Asignación eliminada y cupo restaurado.");
                        }
                        refreshFromDB();
                    });
                }).start();
            }
        });

        refreshAsignadosDocentes();
        p.add(UIFactory.tableScroll(asigDocTable),BorderLayout.CENTER);
        return p;
    }

    void refreshAsignadosDocentes(){
        if(asigDocModel==null) return;
        asigDocModel.setRowCount(0);
        int count = 0;
        for(Asignado s:asignados){
            boolean faltaDoc = s.nota!=null && s.nota.toLowerCase().contains("docente") || (s.nota!=null && s.nota.toLowerCase().contains("doc"));
            boolean faltaAse = s.nota!=null && s.nota.toLowerCase().contains("asesor") || (s.nota!=null && s.nota.toLowerCase().contains("ase"));
            // Show assignments that have an institution but missing doc or asesor
            if ((s.institucion!=null && !s.institucion.isEmpty()) && (faltaDoc || faltaAse)){
                asigDocModel.addRow(new Object[]{ s.nombre+"\nC.C: "+s.cedula, s.programa, "🏢 "+s.institucion, s });
                count++;
            }
        }
        if(tabs!=null) tabs.setTitleAt(1,"🧑‍🏫  Asignar Docente/Asesor ("+count+")");
    }

    // ── Tab 3: En Práctica / Finalizada ──────────────────────────────────────
    private JPanel buildAsignadosFinalPanel(){
        JPanel p=new JPanel(new BorderLayout());
        p.setOpaque(false); p.setBorder(new EmptyBorder(12,0,0,0));

        String[] cols={"Estudiante","Programa","Institución","Estado Práctica","Acciones"};
        asigFinalModel=new DefaultTableModel(cols,0){
            @Override public boolean isCellEditable(int r,int c){ return c==4; }
        };

        asigFinalTable = new JTable(asigFinalModel);
        UIFactory.styleTable(asigFinalTable);
        asigFinalTable.getColumnModel().getColumn(3).setMaxWidth(130);
        asigFinalTable.getColumnModel().getColumn(3).setCellRenderer(new EstadoPracticaRenderer());
        asigFinalTable.getColumnModel().getColumn(4).setPreferredWidth(160);
        asigFinalTable.getColumnModel().getColumn(4).setCellRenderer(new AsigAccionesRenderer());

        asigFinalTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = asigFinalTable.rowAtPoint(e.getPoint());
                int col = asigFinalTable.columnAtPoint(e.getPoint());
                if (row < 0 || col != 4) return;
                Object cell = asigFinalTable.getModel().getValueAt(asigFinalTable.convertRowIndexToModel(row), 4);
                if (!(cell instanceof Asignado)) return;
                Asignado a = (Asignado) cell;
                Rectangle cellRect = asigFinalTable.getCellRect(row, col, false);
                int relX = e.getX() - cellRect.x; int cellW = cellRect.width; int third = Math.max(1, cellW/3);
                if (relX < third) {
                    try {
                        var u = DB.buscarUsuario(a.cedula);
                        if (onVerPerfil != null) { onVerPerfil.accept(u); }
                        else { if (u!=null) JOptionPane.showMessageDialog(PanelAsignaciones.this, "Perfil: " + u.nombreCompleto()); }
                    } catch (Exception ignored) {}
                    return;
                }
                if (relX < 2*third) { /* potentially assign doc if missing */ return; }
                // delete similar to above
                int resp = JOptionPane.showConfirmDialog(PanelAsignaciones.this, "¿Eliminar la asignación de '"+a.nombre+"'?\nEsto devolverá el cupo.", "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
                if (resp != JOptionPane.YES_OPTION) return;
                new Thread(() -> {
                    boolean ok = false;
                    try { ok = DB.eliminarAsignacionPorId(a.idAsignacion); } catch (Exception ex) { ok = false; }
                    final boolean res = ok;
                    SwingUtilities.invokeLater(() -> {
                        if (!res) {
                            JOptionPane.showMessageDialog(PanelAsignaciones.this, "La asignación no se encontró en la base de datos. Se refrescará la vista.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(PanelAsignaciones.this, "Asignación eliminada y cupo restaurado.");
                        }
                        refreshFromDB();
                    });
                }).start();
            }
        });

        refreshAsignadosFinales();
        p.add(UIFactory.tableScroll(asigFinalTable),BorderLayout.CENTER);
        return p;
    }

    /** Stop any active editors on all tables and clear selections. */
    public void stopEditingAndClearSelection() {
        try {
            JTable[] arr = new JTable[] { pendTable, asigDocTable, asigFinalTable };
            for (JTable t: arr) {
                if (t == null) continue;
                if (t.isEditing()) {
                    try { t.getCellEditor().stopCellEditing(); } catch (Exception ignored) { try { t.getCellEditor().cancelCellEditing(); } catch (Exception ignored2) {} }
                }
                t.clearSelection(); t.repaint();
            }
        } catch (Exception ignored) {}
    }

    void refreshAsignadosFinales(){
        if(asigFinalModel==null) return;
        asigFinalModel.setRowCount(0);
        int count = 0;
        for(Asignado s:asignados){
            boolean faltaDoc = s.nota!=null && s.nota.toLowerCase().contains("docente") || (s.nota!=null && s.nota.toLowerCase().contains("doc"));
            boolean faltaAse = s.nota!=null && s.nota.toLowerCase().contains("asesor") || (s.nota!=null && s.nota.toLowerCase().contains("ase"));
            // Show only fully assigned (institution + docente + asesor)
            if ((s.institucion!=null && !s.institucion.isEmpty()) && !faltaDoc && !faltaAse){
                asigFinalModel.addRow(new Object[]{ s.nombre+"\nC.C: "+s.cedula, s.programa, "🏢 "+s.institucion, s.estado, s });
                count++;
            }
        }
        if(tabs!=null) tabs.setTitleAt(2,"🏁  En Práctica/Finalizada ("+count+")");
    }

    class EstadoPracticaRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int row,int col){
            // For director's assignments view we always show "En práctica" for assigned students
            JLabel badge = UIFactory.badgeEnCurso();
            JPanel p=UIFactory.transparent(new FlowLayout(FlowLayout.CENTER,0,9));
            p.setBackground(s?Theme.PRIMARY_LIGHT:(row%2==0?Color.WHITE:new Color(0xfafafa)));
            p.add(badge); return p;
        }
    }
    class AsigAccionesRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int row,int col){
            Asignado a = (Asignado) v;
            return buildAsigAcciones(a,row,false);
        }
    }
    class AsigAccionesEditor extends AbstractCellEditor implements TableCellEditor {
        AsigAccionesEditor(JTable t){}
        @Override public Object getCellEditorValue(){ return null; }
        @Override public Component getTableCellEditorComponent(JTable t,Object v,boolean s,int row,int col){
            Asignado a = (Asignado) v;
            return buildAsigAcciones(a,row,true);
        }
    }

    private JPanel buildAsigAcciones(Asignado a, int row, boolean live){
        if (a == null) {
            JPanel empty = new JPanel(); empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
            empty.setBackground(row%2==0?Color.WHITE:new Color(0xfafafa));
            empty.add(new JLabel("-"));
            System.err.println("[WARN] buildAsigAcciones received null Asignado at row " + row);
            return empty;
        }
        JPanel p=new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(row%2==0?Color.WHITE:new Color(0xfafafa));
        JPanel top=new JPanel(new FlowLayout(FlowLayout.CENTER,6,6)); top.setOpaque(false);
        JButton ver=UIFactory.outlineBtn("👤 Ver Perfil"); if(live) ver.addActionListener(e->{
            try {
                var u = DB.buscarUsuario(a.cedula);
                if (onVerPerfil != null) { onVerPerfil.accept(u); }
                else { if (u!=null) JOptionPane.showMessageDialog(PanelAsignaciones.this, "Perfil: " + u.nombreCompleto()); }
            } catch (Exception ignored) {}
        });
        top.add(ver);
        JButton del = UIFactory.dangerBtn("Eliminar");
        if (live) {
            del.addActionListener(e -> {
                int resp = JOptionPane.showConfirmDialog(this, "¿Eliminar la asignación de '"+a.nombre+"'?\nEsto devolverá el cupo y permitirá que el estudiante vuelva a elegir.", "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
                if (resp != JOptionPane.YES_OPTION) return;
                // Stop table editor if called from within an editor (do on EDT)
                Component src = (Component) e.getSource();
                JTable table = (JTable) SwingUtilities.getAncestorOfClass(JTable.class, src);
                if (table != null && table.isEditing()) {
                    try { table.getCellEditor().stopCellEditing(); } catch (Exception ex) { try { table.getCellEditor().cancelCellEditing(); } catch (Exception ignored) {} }
                }
                del.setEnabled(false);
                final String idToDelete = a.idAsignacion;
                new Thread(() -> {
                    boolean ok = false;
                    try { ok = DB.eliminarAsignacionPorId(idToDelete); }
                    catch (Exception ex) { ok = false; }
                    final boolean res = ok;
                    SwingUtilities.invokeLater(() -> {
                        del.setEnabled(true);
                        if (!res) {
                            JOptionPane.showMessageDialog(this, "La asignación no se encontró en la base de datos. Se refrescará la vista.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(this, "Asignación eliminada y cupo restaurado.");
                        }
                        // Refrescar la vista en cualquier caso para sincronizar con la BD
                        refreshFromDB();
                    });
                }).start();
            });
        }
        // Si falta docente o asesor, mostrar botón para asignarlos (siempre visible en renderer; listener solo en editor)
        boolean falta = a.nota!=null && (!a.nota.isEmpty());
        if (falta) {
            JButton assignDocBtn = UIFactory.primaryBtn("Asignar Docente/Asesor");
            if (live) {
                assignDocBtn.addActionListener(e -> {
                    // Stop editor if inside a table
                    Component src = (Component) e.getSource();
                    JTable table = (JTable) SwingUtilities.getAncestorOfClass(JTable.class, src);
                    if (table != null && table.isEditing()) {
                        try { table.getCellEditor().stopCellEditing(); } catch (Exception ex) { try { table.getCellEditor().cancelCellEditing(); } catch (Exception ignored) {} }
                    }
                    showDocAsesorModal(a);
                });
            }
            top.add(assignDocBtn);
        }
        top.add(del);
        p.add(top);
        if(a.nota!=null && !a.nota.isEmpty()){
            JLabel note = UIFactory.small(a.nota);
            note.setForeground(Theme.RED_TEXT);
            JPanel noteP = UIFactory.transparent(new FlowLayout(FlowLayout.CENTER,0,4)); noteP.add(note);
            p.add(noteP);
        }
        return p;
    }

    // ── Modal de Asignación ───────────────────────────────────────────────────

    // Muestra un diálogo modal para asignar plaza/docente/asesor al estudiante.
    // Si `plazaOnly` es true, solo permite seleccionar la institución (sin docente/asesor).
    private void showAsignarModal(Pendiente est, boolean plazaOnly){
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
        // Debug button to inspect DB status
        JButton dbBtn = UIFactory.outlineBtn("DB");
        dbBtn.setToolTipText("Mostrar estado de la BD (usuario, conteos)");
        dbBtn.addActionListener(ev -> JOptionPane.showMessageDialog(this, DB.debugStatus(), "Estado BD", JOptionPane.INFORMATION_MESSAGE));
        header.add(dbBtn, BorderLayout.EAST);
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

        // Selects de asignación: limitar instituciones a las dos preferencias del estudiante
        String selId1 = null, selId2 = null, selName1 = null, selName2 = null;
        try { var sel = DB.seleccionDeEstudiante(est.cedula); if (sel != null) { selId1 = sel.idInstOp1; selId2 = sel.idInstOp2; selName1 = sel.nombreOp1; selName2 = sel.nombreOp2; } } catch (Exception ignored) {}
        java.util.List<String> instNames = new java.util.ArrayList<>();
        java.util.List<String> instIds = new java.util.ArrayList<>();
        if (selId1 != null) { instNames.add((selName1==null?selId1:selName1)); instIds.add(selId1); }
        if (selId2 != null && !selId2.equals(selId1)) { instNames.add((selName2==null?selId2:selName2)); instIds.add(selId2); }

        // Detectar si ya existe una asignación (provisional) para este estudiante
        final DB.Asignacion[] existingAsg = new DB.Asignacion[1];
        try { var listA = DB.asignacionesPorEstudiante(est.cedula); if (!listA.isEmpty()) existingAsg[0] = listA.get(0); } catch (Exception ignored) {}
        // Si existe y la institución no está en las opciones (p. ej. asignada previamente), agregarla para poder editar
        if (existingAsg[0] != null && existingAsg[0].idInstitucion != null && !instIds.contains(existingAsg[0].idInstitucion)) {
            String name = existingAsg[0].nombreInstitucion == null ? existingAsg[0].idInstitucion : existingAsg[0].nombreInstitucion;
            instNames.add(name); instIds.add(existingAsg[0].idInstitucion);
        }

        // Reconstruir etiquetas incluyendo cupos actuales por institución
        instNames.clear();
        for (String iid : instIds) {
            String name = iid; int cupos = -1;
            try { var inst = DB.buscarInstitucion(iid); if (inst != null) { name = inst.nombre; cupos = inst.cuposDisp; } } catch (Exception ignored) {}
            String label = name + (cupos>=0 ? " — " + cupos + (cupos==1?" cupo":" cupos") : "");
            instNames.add(label);
        }

        // Asegurar que las listas de docentes y asesores estén actualizadas justo antes de construir los combos
        docentes.clear(); asesores.clear();
        try { var docs = DB.listarPorRol("Docente"); for (var du : docs) docentes.add(new Persona(du.cedula, du.nombre + (du.apellido==null?"":" "+du.apellido))); } catch (Exception ignored) {}
        try { var ases = DB.listarPorRol("Asesor"); for (var au : ases) asesores.add(new Persona(au.cedula, au.nombre + (au.apellido==null?"":" "+au.apellido))); } catch (Exception ignored) {}

        // Debug: imprimir conteos recuperados desde DAO
        System.err.println("[DEBUG] Docentes recuperados: " + docentes.size() + ", Asesores recuperados: " + asesores.size());
        if (docentes.isEmpty() && asesores.isEmpty()) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(dlg, "Advertencia: no se recuperaron docentes ni asesores desde la base de datos.\nRevise que existan usuarios con rol 'Docente' y 'Asesor'.", "Datos vacíos", JOptionPane.WARNING_MESSAGE));
        }

        // Mostrar detalle de cada docente/asesor en logs
        for (int i=0;i<docentes.size();i++) System.err.println("[DEBUG] Docente["+i+"]: ced="+docentes.get(i).cedula+" nombre='"+docentes.get(i).nombre+"'");
        for (int i=0;i<asesores.size();i++) System.err.println("[DEBUG] Asesor["+i+"]: ced="+asesores.get(i).cedula+" nombre='"+asesores.get(i).nombre+"'");

        String[] instOpts = new String[instNames.size()+1]; instOpts[0] = "Seleccione una institución...";
        for (int i=0;i<instNames.size();i++) instOpts[i+1]=instNames.get(i);
        JComboBox<String> instBox=UIFactory.comboBox(instOpts);

        // Construir combos directamente con UsuarioDAO.Usuario (evita wrapper Persona)
        java.util.List<UsuarioDAO.Usuario> docsList = java.util.Collections.emptyList();
        java.util.List<UsuarioDAO.Usuario> asesList = java.util.Collections.emptyList();
        try { docsList = DB.listarPorRol("Docente"); } catch (Exception ignored) {}
        try { asesList = DB.listarPorRol("Asesor"); } catch (Exception ignored) {}
        UsuarioDAO.Usuario[] docItems = new UsuarioDAO.Usuario[docsList.size()+1]; docItems[0] = null;
        for (int i=0;i<docsList.size();i++) docItems[i+1] = docsList.get(i);
        UsuarioDAO.Usuario[] aseItems = new UsuarioDAO.Usuario[asesList.size()+1]; aseItems[0] = null;
        for (int i=0;i<asesList.size();i++) aseItems[i+1] = asesList.get(i);
        JComboBox<UsuarioDAO.Usuario> docBox = new JComboBox<>(docItems);
        JComboBox<UsuarioDAO.Usuario> aseBox = new JComboBox<>(aseItems);
        UIFactory.styleComboObject(docBox);
        UIFactory.styleComboObject(aseBox);
        // Renderer: mostrar nombre + apellido, con placeholder cuando el item es null
        docBox.setRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> l,Object v,int i,boolean s,boolean f){
                String txt = "Seleccione un docente...";
                if (v instanceof UsuarioDAO.Usuario){ UsuarioDAO.Usuario u = (UsuarioDAO.Usuario)v; txt = (u.nombre==null?"":u.nombre) + (u.apellido==null?"":" "+u.apellido); }
                JLabel lbl = (JLabel) super.getListCellRendererComponent(l, txt, i, s, f);
                lbl.setBorder(new EmptyBorder(5,10,5,10)); lbl.setFont(Theme.FONT_BODY);
                if (s){ lbl.setBackground(Theme.PRIMARY_LIGHT); lbl.setForeground(Theme.PRIMARY); } else { lbl.setBackground(Color.WHITE); lbl.setForeground(Theme.TEXT_PRIMARY); }
                return lbl;
            }
        });
        aseBox.setRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> l,Object v,int i,boolean s,boolean f){
                String txt = "Seleccione un asesor...";
                if (v instanceof UsuarioDAO.Usuario){ UsuarioDAO.Usuario u = (UsuarioDAO.Usuario)v; txt = (u.nombre==null?"":u.nombre) + (u.apellido==null?"":" "+u.apellido); }
                JLabel lbl = (JLabel) super.getListCellRendererComponent(l, txt, i, s, f);
                lbl.setBorder(new EmptyBorder(5,10,5,10)); lbl.setFont(Theme.FONT_BODY);
                if (s){ lbl.setBackground(Theme.PRIMARY_LIGHT); lbl.setForeground(Theme.PRIMARY); } else { lbl.setBackground(Color.WHITE); lbl.setForeground(Theme.TEXT_PRIMARY); }
                return lbl;
            }
        });
        

        // Añadir listener para actualizar cupos en tiempo real mientras el dialog está abierto
        DB.ChangeListener _instListener = topic -> {
            if (!"instituciones".equals(topic)) return;
            SwingUtilities.invokeLater(() -> {
                // reconstruir items conservando selección
                int sel = instBox.getSelectedIndex();
                for (int i=0;i<instIds.size();i++){
                    String iid = instIds.get(i);
                    String name = iid; int cupos=-1;
                    try { var inst = DB.buscarInstitucion(iid); if (inst!=null){ name = inst.nombre; cupos = inst.cuposDisp; } } catch (Exception ignored) {}
                    String label = name + (cupos>=0 ? " — " + cupos + (cupos==1?" cupo":" cupos") : "");
                    // update model item (index i+1)
                    try {
                        instBox.insertItemAt(label, i+1);
                        instBox.removeItemAt(i+2);
                    } catch (Exception ignored) {}
                }
                try { instBox.setSelectedIndex(sel); } catch (Exception ignored) {}
            });
        };
        DB.addChangeListener(_instListener);
        // remove listener when dialog closes
        dlg.addWindowListener(new java.awt.event.WindowAdapter(){ public void windowClosed(java.awt.event.WindowEvent e){ DB.removeChangeListener(_instListener); } public void windowClosing(java.awt.event.WindowEvent e){ DB.removeChangeListener(_instListener); } });

        // Si ya existe una asignación, pre-seleccionar valores en los combos
        if (existingAsg[0] != null) {
            // seleccionar institución
            int idxInst = instIds.indexOf(existingAsg[0].idInstitucion);
            if (idxInst >= 0) instBox.setSelectedIndex(idxInst + 1);
            // seleccionar docente
            if (existingAsg[0].cedulaDocente != null) {
                for (int i=0;i<docentes.size();i++) if (docentes.get(i).cedula.equals(existingAsg[0].cedulaDocente)) { docBox.setSelectedIndex(i+1); break; }
            }
            // seleccionar asesor
            if (existingAsg[0].cedulaAsesor != null) {
                for (int i=0;i<asesores.size();i++) if (asesores.get(i).cedula.equals(existingAsg[0].cedulaAsesor)) { aseBox.setSelectedIndex(i+1); break; }
            }
        }

        JPanel selGrid=new JPanel(new GridLayout(plazaOnly?1:3,1,0,10));
        selGrid.setOpaque(false);
        selGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE,plazaOnly?48:120));
        selGrid.add(UIFactory.labeledField("Institución Receptora Disponible",instBox));
        if (!plazaOnly) {
            selGrid.add(UIFactory.labeledField("Docente Encargado",docBox));
            selGrid.add(UIFactory.labeledField("Asesor Asignado",aseBox));
        }
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
            if(instBox.getSelectedIndex()==0) {
                JOptionPane.showMessageDialog(dlg, "Seleccione la plaza (obligatorio).", "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int instIdx = instBox.getSelectedIndex() - 1;
            String chosenInstId = instIds.get(instIdx);
            String chosenInstName = instNames.get(instIdx);

            // Re-check cupos actuales justo antes de confirmar
            try { var instNow = DB.buscarInstitucion(chosenInstId); if (instNow != null && instNow.cuposDisp <= 0) { SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(dlg, "La institución seleccionada ya no tiene cupos disponibles.", "Error", JOptionPane.ERROR_MESSAGE)); confirmBtn.setEnabled(true); return; } } catch (Exception ignored) {}

            String chosenDocCed = null; if(docBox.getSelectedIndex() > 0) { int didx = docBox.getSelectedIndex() - 1; chosenDocCed = docentes.get(didx).cedula; }
            String chosenAseCed = null; if(aseBox.getSelectedIndex() > 0) { int aidx = aseBox.getSelectedIndex() - 1; chosenAseCed = asesores.get(aidx).cedula; }
            final String fChosenInstId = chosenInstId;
            final String fChosenInstName = chosenInstName;
            final String fChosenDocCed = chosenDocCed;
            final String fChosenAseCed = chosenAseCed;

            confirmBtn.setEnabled(false);
            new Thread(() -> {
            try {
                // Si ya existe una asignación para el estudiante, actualizamos en lugar de insertar
                if (existingAsg[0] != null) {
                    boolean faltaDoc = (fChosenDocCed == null || fChosenDocCed.isEmpty());
                    boolean faltaAse = (fChosenAseCed == null || fChosenAseCed.isEmpty());
                    String desiredState = (!faltaDoc && !faltaAse) ? "En Curso" : "Pendiente";
                    String mappedState;
                    try {
                        java.util.List<String> allowed = DB.allowedValues("Asignacion_Practica", "Estado_Asignacion");
                        if (allowed != null && !allowed.isEmpty()) {
                            if (allowed.contains(desiredState)) mappedState = desiredState;
                            else {
                                String pick = null;
                                // Prefer a literal whose mapped habilitado integer matches expected semantics
                                for (String v : allowed) {
                                    if (v == null) continue;
                                    int h = sigep.db.StateUtils.habilitadoFromEstado(v);
                                    if ((faltaDoc || faltaAse) && h == 0) { pick = v; break; }
                                    if (!faltaDoc && !faltaAse && h >= 1) { pick = v; break; }
                                }
                                // Fallback to substring heuristics if no obvious candidate
                                if (pick == null) {
                                    for (String v : allowed) {
                                        String lv = v == null ? "" : v.toLowerCase();
                                        if (!faltaDoc && !faltaAse && (lv.contains("act") || lv.contains("curso") || lv.contains("activ"))) { pick = v; break; }
                                        if ((faltaDoc || faltaAse) && (lv.contains("pend") || lv.contains("esper") || lv.contains("por") || lv.contains("wait"))) { pick = v; break; }
                                    }
                                }
                                if (pick == null) pick = allowed.get(0);
                                mappedState = pick;
                            }
                        } else mappedState = desiredState;
                    } catch (Exception ex) { mappedState = desiredState; }

                    // Construir objeto Asignacion con los nuevos valores y delegar en el método transaccional
                    DB.Asignacion asgUpd = new DB.Asignacion();
                    asgUpd.cedulaEstudiante = est.cedula;
                    asgUpd.idInstitucion = fChosenInstId;
                    asgUpd.cedulaDocente = fChosenDocCed;
                    asgUpd.cedulaAsesor = fChosenAseCed;
                    asgUpd.estadoAsignacion = mappedState;
                    String dbErr = DB.crearAsignacionSafe(asgUpd);
                    if (dbErr != null) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(dlg, "No fue posible actualizar la asignación:\n" + dbErr, "Error", JOptionPane.ERROR_MESSAGE));
                        return;
                    }
                    SwingUtilities.invokeLater(() -> {
                        if (!faltaDoc && !faltaAse) JOptionPane.showMessageDialog(dlg, "Asignación completada.", "OK", JOptionPane.INFORMATION_MESSAGE);
                        else JOptionPane.showMessageDialog(dlg, "Asignación provisional actualizada. Falta docente y/o asesor.", "Información", JOptionPane.INFORMATION_MESSAGE);
                        refreshFromDB(); dlg.dispose();
                    });
                } else {
                    DB.Asignacion asg = new DB.Asignacion();
                    asg.cedulaEstudiante = est.cedula;
                    asg.idInstitucion = chosenInstId;
                    asg.cedulaDocente = fChosenDocCed;
                    asg.cedulaAsesor = fChosenAseCed;
                    asg.periodoAcademico = "2026";
                    boolean faltaDoc = (asg.cedulaDocente == null || asg.cedulaDocente.isEmpty());
                    boolean faltaAse = (asg.cedulaAsesor == null || asg.cedulaAsesor.isEmpty());
                    String desiredState = (!faltaDoc && !faltaAse) ? "En Curso" : "Pendiente";
                    try {
                        java.util.List<String> allowed = DB.allowedValues("Asignacion_Practica", "Estado_Asignacion");
                        if (allowed != null && !allowed.isEmpty()) {
                            if (allowed.contains(desiredState)) asg.estadoAsignacion = desiredState;
                            else {
                                String pick = null;
                                for (String v : allowed) {
                                    if (v == null) continue;
                                    int h = sigep.db.StateUtils.habilitadoFromEstado(v);
                                    if ((faltaDoc || faltaAse) && h == 0) { pick = v; break; }
                                    if (!faltaDoc && !faltaAse && h >= 1) { pick = v; break; }
                                }
                                if (pick == null) {
                                    for (String v : allowed) {
                                        String lv = v == null ? "" : v.toLowerCase();
                                        if (!faltaDoc && !faltaAse && (lv.contains("act") || lv.contains("curso") || lv.contains("activ"))) { pick = v; break; }
                                        if ((faltaDoc || faltaAse) && (lv.contains("pend") || lv.contains("esper") || lv.contains("por") || lv.contains("wait"))) { pick = v; break; }
                                    }
                                }
                                if (pick == null) pick = allowed.get(0);
                                asg.estadoAsignacion = pick;
                            }
                        } else {
                            asg.estadoAsignacion = desiredState;
                        }
                    } catch (Exception ex) {
                        asg.estadoAsignacion = desiredState;
                    }
                    asg.nombreEstudiante = est.nombreCompleto();
                    asg.nombreInstitucion = chosenInstName;
                    asg.programaEstudiante = est.programa;

                    String dbErr = DB.crearAsignacionSafe(asg);
                    if (dbErr != null) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(dlg, "No fue posible guardar la asignación:\n" + dbErr, "Error", JOptionPane.ERROR_MESSAGE));
                        return;
                    }
                    SwingUtilities.invokeLater(() -> {
                        if (!faltaDoc && !faltaAse) JOptionPane.showMessageDialog(dlg, "Asignación guardada y completa.", "OK", JOptionPane.INFORMATION_MESSAGE);
                        else JOptionPane.showMessageDialog(dlg, "Asignación provisional guardada. Falta asignar docente y/o asesor.", "Información", JOptionPane.INFORMATION_MESSAGE);
                        refreshFromDB(); dlg.dispose();
                    });
                }
            } catch (Exception ex) {
                String msg = "Error al crear/actualizar asignación: " + ex.getMessage();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(dlg, msg, "Error", JOptionPane.ERROR_MESSAGE));
            } finally {
                SwingUtilities.invokeLater(() -> confirmBtn.setEnabled(true));
            }
            }).start();

        });

        footer.add(cancelBtn); footer.add(confirmBtn);
        root.add(footer,BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // Modal específico para asignar solo Docente y Asesor sobre una asignación ya existente
    private void showDocAsesorModal(Asignado asign){
        if (asign==null) return;
        // Buscar la asignación completa desde DB
        DB.Asignacion existing = null;
        try { var list = DB.asignacionesPorEstudiante(asign.cedula); if (!list.isEmpty()) existing = list.get(0); } catch (Exception ignored) {}
        if (existing==null) { JOptionPane.showMessageDialog(this, "No se encontró la asignación en la base de datos.", "Error", JOptionPane.ERROR_MESSAGE); return; }
        final DB.Asignacion existingF = existing;

        JDialog dlg=new JDialog((Frame)SwingUtilities.getWindowAncestor(this),"Asignar Docente y Asesor",true);
        dlg.setSize(480,360); dlg.setLocationRelativeTo(this); dlg.setResizable(false);
        JPanel root=new JPanel(new BorderLayout()); dlg.setContentPane(root);
        JPanel header=new JPanel(new BorderLayout()); header.setBackground(Theme.PRIMARY); header.setBorder(new EmptyBorder(12,14,12,14));
        JLabel h = new JLabel("Asignar Docente y Asesor"); h.setFont(Theme.FONT_H3); h.setForeground(Color.WHITE); header.add(h,BorderLayout.WEST);
        root.add(header,BorderLayout.NORTH);

        JPanel body=new JPanel(); body.setLayout(new BoxLayout(body,BoxLayout.Y_AXIS)); body.setBorder(new EmptyBorder(12,14,12,14)); body.setBackground(Color.WHITE);
        body.add(UIFactory.sectionLabel("Estudiante")); body.add(UIFactory.body(asign.nombre+" • C.C: "+asign.cedula)); body.add(UIFactory.gap(10));
        body.add(UIFactory.sectionLabel("Institución asignada")); body.add(UIFactory.body(asign.institucion)); body.add(UIFactory.gap(10));

        // Construir combos directamente con UsuarioDAO.Usuario
        java.util.List<UsuarioDAO.Usuario> docsList = java.util.Collections.emptyList();
        java.util.List<UsuarioDAO.Usuario> asesList = java.util.Collections.emptyList();
        try { docsList = DB.listarPorRol("Docente"); } catch (Exception ignored) {}
        try { asesList = DB.listarPorRol("Asesor"); } catch (Exception ignored) {}
        UsuarioDAO.Usuario[] docItems = new UsuarioDAO.Usuario[docsList.size()+1]; docItems[0] = null;
        for (int i=0;i<docsList.size();i++) docItems[i+1] = docsList.get(i);
        UsuarioDAO.Usuario[] aseItems = new UsuarioDAO.Usuario[asesList.size()+1]; aseItems[0] = null;
        for (int i=0;i<asesList.size();i++) aseItems[i+1] = asesList.get(i);
        JComboBox<UsuarioDAO.Usuario> docBox = new JComboBox<>(docItems);
        JComboBox<UsuarioDAO.Usuario> aseBox = new JComboBox<>(aseItems);
        UIFactory.styleComboObject(docBox);
        UIFactory.styleComboObject(aseBox);
        docBox.setRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> l,Object v,int i,boolean s,boolean f){
                String txt = "Seleccione un docente...";
                if (v instanceof UsuarioDAO.Usuario){ UsuarioDAO.Usuario u = (UsuarioDAO.Usuario)v; txt = (u.nombre==null?"":u.nombre) + (u.apellido==null?"":" "+u.apellido); }
                JLabel lbl = (JLabel) super.getListCellRendererComponent(l, txt, i, s, f);
                lbl.setBorder(new EmptyBorder(5,10,5,10)); lbl.setFont(Theme.FONT_BODY);
                if (s){ lbl.setBackground(Theme.PRIMARY_LIGHT); lbl.setForeground(Theme.PRIMARY); } else { lbl.setBackground(Color.WHITE); lbl.setForeground(Theme.TEXT_PRIMARY); }
                return lbl;
            }
        });
        aseBox.setRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> l,Object v,int i,boolean s,boolean f){
                String txt = "Seleccione un asesor...";
                if (v instanceof UsuarioDAO.Usuario){ UsuarioDAO.Usuario u = (UsuarioDAO.Usuario)v; txt = (u.nombre==null?"":u.nombre) + (u.apellido==null?"":" "+u.apellido); }
                JLabel lbl = (JLabel) super.getListCellRendererComponent(l, txt, i, s, f);
                lbl.setBorder(new EmptyBorder(5,10,5,10)); lbl.setFont(Theme.FONT_BODY);
                if (s){ lbl.setBackground(Theme.PRIMARY_LIGHT); lbl.setForeground(Theme.PRIMARY); } else { lbl.setBackground(Color.WHITE); lbl.setForeground(Theme.TEXT_PRIMARY); }
                return lbl;
            }
        });
        // preselect if exists (match by cedula)
        if (existing.cedulaDocente != null) {
            for (int i=1;i<docBox.getItemCount();i++){ UsuarioDAO.Usuario u = docBox.getItemAt(i); if (u!=null && existing.cedulaDocente.equals(u.cedula)){ docBox.setSelectedIndex(i); break; } }
        }
        if (existing.cedulaAsesor != null) {
            for (int i=1;i<aseBox.getItemCount();i++){ UsuarioDAO.Usuario u = aseBox.getItemAt(i); if (u!=null && existing.cedulaAsesor.equals(u.cedula)){ aseBox.setSelectedIndex(i); break; } }
        }

        body.add(UIFactory.labeledField("Docente Encargado", docBox)); body.add(UIFactory.labeledField("Asesor Asignado", aseBox));
        root.add(body,BorderLayout.CENTER);

        JPanel footer=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,8)); footer.setBackground(new Color(0xf9fafb)); footer.setBorder(BorderFactory.createMatteBorder(1,0,0,0,Theme.BORDER));
        JButton cancel=UIFactory.outlineBtn("Cancelar"); cancel.addActionListener(e->dlg.dispose()); footer.add(cancel);
        JButton save=UIFactory.primaryBtn("Guardar"); save.addActionListener(e->{
            String chosenDoc=null; if (docBox.getSelectedIndex()>0) { UsuarioDAO.Usuario u = (UsuarioDAO.Usuario)docBox.getSelectedItem(); if (u!=null) chosenDoc = u.cedula; }
            String chosenAse=null; if (aseBox.getSelectedIndex()>0) { UsuarioDAO.Usuario u = (UsuarioDAO.Usuario)aseBox.getSelectedItem(); if (u!=null) chosenAse = u.cedula; }
            final String fChosenDoc = chosenDoc;
            final String fChosenAse = chosenAse;
            save.setEnabled(false);
            new Thread(() -> {
                try {
                    DB.Asignacion upd = new DB.Asignacion(); upd.idAsignacion = existingF.idAsignacion; upd.cedulaDocente = fChosenDoc; upd.cedulaAsesor = fChosenAse;
                    // map state
                    boolean faltaDoc = (fChosenDoc==null || fChosenDoc.isEmpty()); boolean faltaAse = (fChosenAse==null || fChosenAse.isEmpty());
                    String desiredState = (!faltaDoc && !faltaAse) ? "En Curso" : "Pendiente";
                    try { var allowed = DB.allowedValues("Asignacion_Practica","Estado_Asignacion"); if (allowed!=null && !allowed.isEmpty()){ if (allowed.contains(desiredState)) upd.estadoAsignacion = desiredState; else upd.estadoAsignacion = allowed.get(0);} else upd.estadoAsignacion = desiredState; } catch (Exception ex){ upd.estadoAsignacion = desiredState; }
                    String err = DB.actualizarAsignacionSafe(upd);
                    if (err!=null) { SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(dlg, "Error: "+err, "Error", JOptionPane.ERROR_MESSAGE)); return; }
                    SwingUtilities.invokeLater(() -> { refreshFromDB(); dlg.dispose(); JOptionPane.showMessageDialog(this, "Asignación actualizada."); });
                } catch (Exception ex){ SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(dlg, "Error: "+ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)); }
                finally { SwingUtilities.invokeLater(() -> save.setEnabled(true)); }
            }).start();
        });
        footer.add(save);
        root.add(footer,BorderLayout.SOUTH);
        dlg.setVisible(true);
    }
}
