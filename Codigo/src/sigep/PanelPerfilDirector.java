package sigep;

import java.awt.*;
import java.awt.event.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import sigep.db.DB;
import sigep.db.UsuarioDAO;

/**
 * Vista de Perfil de Estudiante para el Director — replica DirectorStudentView.tsx
 * Incluye:
 *   1. Información Personal (editable)
 *   2. Asignación Académica (editable)
 *   3. Verificación de Documentos (validar/rechazar)
 *   4. Historial de Prácticas con barra de progreso
 */
    // Uso: muestra y permite editar información del director/administrador.
public class PanelPerfilDirector extends JPanel {

    // ── Modelo ────────────────────────────────────────────────────────────────
    static class Practica {
        String titulo,estado,institucion,periodo;
        int horasHechas,horasTotal;
        String notaFinal;
        Practica(String t,String e,String i,String p,int h,int ht,String n){
            titulo=t;estado=e;institucion=i;periodo=p;horasHechas=h;horasTotal=ht;notaFinal=n;
        }
        int porcentaje(){ return Math.round(horasHechas*100f/horasTotal); }
    }
    // Usamos el modelo de Documentos persistente

    // Estado del estudiante (proveniente de la BD)
    private String nombre,apellido,correo,cedula,programa,semestre;
    private String materia="Práctica Profesional I";
    private int horasRequeridas=160;
    private boolean listoParaAsignacion=false;
    private final List<DB.Documento> documentos=new ArrayList<>();
    private final List<Practica>  historial =new ArrayList<>();

    // Labels que se actualizan
    private JLabel headerNameLbl,headerSubLbl,statusBadge;
    private DefaultTableModel docModel;

    // Paneles de info (para reemplazar)
    private JPanel infoPersonalView,infoPersonalEdit;
    private JPanel infoAcadView,infoAcadEdit;
    private JPanel infoPersonalCard,infoAcadCard;

    // Edit-personal fields
    private JTextField eNombre,eApellido,eCorreo;
    private JComboBox<String> eSemestre;
    // Edit-acad fields
    private JTextField eMateria,eHoras;

    private JButton habilitarBtn;
    private final Runnable onBack;
    private final UsuarioDAO.Usuario usuario;

    public PanelPerfilDirector(UsuarioDAO.Usuario usuario, Runnable onBack){
        // Constructor: inicializa datos del usuario, carga documentos/historial y monta la UI.
        this.onBack=onBack; this.usuario = usuario;
        // inicializar datos desde usuario
        if (usuario != null) {
            this.nombre = usuario.nombre; this.apellido = usuario.apellido; this.correo = usuario.correo;
            this.cedula = usuario.cedula; this.programa = DB.nombreProgramaPorId(usuario.idPrograma); this.semestre = usuario.semestre == null ? "" : "S" + usuario.semestre;
        }
        documentos.clear(); historial.clear();
        try {
            if (this.cedula != null) {
                documentos.addAll(DB.listarDocumentos(this.cedula));
                var asigs = DB.asignacionesPorEstudiante(this.cedula);
                for (var a : asigs) {
                    historial.add(new Practica(a.periodoAcademico, a.estadoAsignacion, a.nombreInstitucion == null ? a.idInstitucion : a.nombreInstitucion, a.periodoAcademico, 0, 160, null));
                }
                listoParaAsignacion = DB.todosDocumentosValidos(this.cedula);
            }
        } catch (Exception e) {
            System.err.println("No se pudieron cargar datos del perfil: " + e.getMessage());
        }
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(Theme.PADDING,Theme.PADDING,Theme.PADDING,Theme.PADDING));

        JPanel content=new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));

        content.add(buildHeaderCard());
        content.add(UIFactory.gap(14));
        content.add(buildInfoPersonalCard());
        content.add(UIFactory.gap(14));
        content.add(buildInfoAcadCard());
        content.add(UIFactory.gap(14));
        content.add(buildDocumentosCard());
        content.add(UIFactory.gap(14));
        content.add(buildHistorialSection());
        content.add(UIFactory.gap(20));

        JScrollPane sp=new JScrollPane(content);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Theme.BG_PAGE);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(sp,BorderLayout.CENTER);
    }

    

    // ── Header ────────────────────────────────────────────────────────────────

    private JPanel buildHeaderCard(){
        // Construye la tarjeta superior con avatar, nombre y estado.
        JPanel p=UIFactory.transparent(new BorderLayout(12,0));
        p.setBorder(new EmptyBorder(0,0,4,0));

        // Back button
        JButton back=new JButton("← Volver");
        back.setFont(Theme.FONT_BODY); back.setForeground(Theme.TEXT_MUTED);
        back.setOpaque(false); back.setContentAreaFilled(false); back.setBorderPainted(false); back.setFocusPainted(false);
        back.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        back.addActionListener(e->{ if(onBack!=null) onBack.run(); });
        back.addMouseListener(new MouseAdapter(){
            @Override public void mouseEntered(MouseEvent e){ back.setForeground(Theme.PRIMARY); }
            @Override public void mouseExited(MouseEvent e){ back.setForeground(Theme.TEXT_MUTED); }
        });

        // Avatar + name
        JLabel av=UIFactory.avatar("AP",48,Theme.PRIMARY_LIGHT,Theme.PRIMARY);
        JPanel nameCol=UIFactory.transparent(new GridLayout(2,1,0,3));
        headerNameLbl=new JLabel(nombre+" "+apellido);
        headerNameLbl.setFont(Theme.FONT_H1); headerNameLbl.setForeground(Theme.TEXT_PRIMARY);
        headerSubLbl=UIFactory.muted("🎓  C.C: "+cedula+"  •  "+programa);
        nameCol.add(headerNameLbl); nameCol.add(headerSubLbl);

        JPanel leftRow=UIFactory.transparent(new FlowLayout(FlowLayout.LEFT,12,0));
        leftRow.add(back); leftRow.add(av); leftRow.add(nameCol);

        // Mostrar estado solo para estudiantes
        if (usuario != null && "Estudiante".equals(usuario.rol)) {
            // Preferir la bandera habilitadoAsig (0 = pendiente, 1 = listo)
            if (usuario.habilitadoAsig == 1) {
                statusBadge = UIFactory.badge("✓ Listo para Asignación", Theme.GREEN_BG, Theme.GREEN_TEXT, Theme.GREEN_BORDER);
            } else {
                statusBadge = UIFactory.badge("⏳ Pendiente por asignacion", Theme.YELLOW_BG, Theme.YELLOW_TEXT, Theme.YELLOW_BORDER);
            }
        } else {
            statusBadge = new JLabel("");
        }

        JPanel row=UIFactory.transparent(new BorderLayout());
        row.add(leftRow,BorderLayout.WEST);
        row.add(statusBadge,BorderLayout.EAST);

        JPanel wrapper=UIFactory.transparent(new BorderLayout());
        wrapper.add(row,BorderLayout.CENTER);
        wrapper.add(UIFactory.hSep(),BorderLayout.SOUTH);
        wrapper.setBorder(new EmptyBorder(0,0,10,0));
        return wrapper;
    }

    // ── Información Personal ──────────────────────────────────────────────────

    private JPanel buildInfoPersonalCard(){
        // Construye la tarjeta de Información Personal (vista + modo edición oculto).
        infoPersonalCard=UIFactory.card();
        infoPersonalCard.setLayout(new BorderLayout(0,10));
        infoPersonalCard.setBorder(BorderFactory.createCompoundBorder(infoPersonalCard.getBorder(),new EmptyBorder(16,18,16,18)));
        infoPersonalCard.setMaximumSize(new Dimension(Integer.MAX_VALUE,130));

        // Title row
        JPanel titleRow=new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setBorder(new EmptyBorder(0,0,8,0));
        JPanel left=UIFactory.transparent(new FlowLayout(FlowLayout.LEFT,6,0));
        left.add(new JLabel("👤"){{setFont(new Font("Segoe UI Emoji",Font.PLAIN,13));}}); 
        left.add(UIFactory.sectionLabel("Información Personal"));
        titleRow.add(left,BorderLayout.WEST);
        JButton editBtn=UIFactory.linkBtn("✏ Editar Perfil",Theme.PRIMARY);
        editBtn.addActionListener(e->toggleInfoPersonal(true,editBtn));
        titleRow.add(editBtn,BorderLayout.EAST);
        infoPersonalCard.add(titleRow,BorderLayout.NORTH);

        // View mode
        infoPersonalView=buildPersonalView();
        infoPersonalCard.add(infoPersonalView,BorderLayout.CENTER);

        // Edit mode (hidden)
        infoPersonalEdit=buildPersonalEdit(editBtn);
        infoPersonalEdit.setVisible(false);
        infoPersonalCard.add(infoPersonalEdit,BorderLayout.SOUTH);

        return infoPersonalCard;
    }

    private JPanel buildPersonalView(){
        // Construye la vista read-only de los campos personales.
        JPanel g=new JPanel(new GridLayout(1,3,20,0));
        g.setOpaque(false);
        JPanel c1=field("Nombre Completo",nombre+" "+apellido);
        JPanel c2=field("Correo Electrónico",correo);
        JPanel c3=fieldBadge("Semestre Actual",semestre);
        g.add(c1); g.add(c2); g.add(c3);
        return g;
    }

    @SuppressWarnings("unused")
    private JPanel buildPersonalEdit(JButton _editBtn){
        // Construye el formulario de edición de la información personal.
        JPanel g=new JPanel(new GridLayout(1,4,12,0));
        g.setOpaque(false);
        eNombre=UIFactory.textField("Nombre"); eNombre.setText(nombre);
        eApellido=UIFactory.textField("Apellido"); eApellido.setText(apellido);
        eCorreo=UIFactory.textField("Correo"); eCorreo.setText(correo);
        eSemestre=UIFactory.comboBox("Semestre VII","Semestre VIII","Semestre IX","Semestre X","Egresado");
        eSemestre.setSelectedItem(semestre);

        g.add(UIFactory.labeledField("Nombre(s)",eNombre));
        g.add(UIFactory.labeledField("Apellido(s)",eApellido));
        g.add(UIFactory.labeledField("Correo",eCorreo));
        g.add(UIFactory.labeledField("Semestre",eSemestre));
        return g;
    }

    private void toggleInfoPersonal(boolean editing, JButton editBtn){
        // Alterna entre vista y edición personal; al guardar actualiza BD.
        infoPersonalView.setVisible(!editing);
        infoPersonalEdit.setVisible(editing);
        if(editing){
            editBtn.setText("✓ Guardar");
            editBtn.removeActionListener(editBtn.getActionListeners()[0]);
            editBtn.addActionListener(e->{
                nombre = eNombre.getText().trim();
                apellido = eApellido.getText().trim();
                correo = eCorreo.getText().trim();
                String semSel = eSemestre.getSelectedItem() == null ? null : eSemestre.getSelectedItem().toString();
                Integer semVal = null;
                if (semSel != null) {
                    // Try parse trailing number, else keep existing usuario.semestre
                    try {
                        // extract digits
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(semSel);
                        if (m.find()) semVal = Integer.parseInt(m.group(1));
                    } catch (Exception ex) { semVal = null; }
                }
                // Prepare update object
                UsuarioDAO.Usuario upd = new UsuarioDAO.Usuario();
                upd.cedula = cedula;
                upd.nombre = nombre;
                upd.apellido = apellido;
                upd.correo = correo;
                upd.idPrograma = usuario.idPrograma; // keep existing
                upd.estadoUsuario = usuario.estadoUsuario; // keep existing
                upd.semestre = semVal != null ? semVal : usuario.semestre;

                boolean ok = DB.actualizarUsuario(upd);
                if (ok) {
                    // reflect changes locally
                    headerNameLbl.setText(nombre + " " + apellido);
                    headerSubLbl.setText("🎓  C.C: " + cedula + "  •  " + programa);
                    usuario.nombre = nombre; usuario.apellido = apellido; usuario.correo = correo;
                    usuario.semestre = upd.semestre;
                    // Rebuild personal view
                    infoPersonalCard.remove(infoPersonalView);
                    infoPersonalView = buildPersonalView();
                    infoPersonalCard.add(infoPersonalView, BorderLayout.CENTER);
                    JOptionPane.showMessageDialog(this, "✅ Perfil actualizado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "❌ Error al actualizar el perfil en la base de datos.", "Error", JOptionPane.ERROR_MESSAGE);
                }
                editBtn.setText("✏ Editar Perfil");
                editBtn.removeActionListener(editBtn.getActionListeners()[0]);
                editBtn.addActionListener(ev->toggleInfoPersonal(true,editBtn));
                toggleInfoPersonal(false,editBtn);
            });
        } else {
            editBtn.setText("✏ Editar Perfil");
        }
        revalidate(); repaint();
    }

    // ── Asignación Académica ──────────────────────────────────────────────────

    private JPanel buildInfoAcadCard(){
        infoAcadCard=UIFactory.card();
        infoAcadCard.setLayout(new BorderLayout(0,10));
        infoAcadCard.setBorder(BorderFactory.createCompoundBorder(infoAcadCard.getBorder(),new EmptyBorder(16,18,16,18)));
        infoAcadCard.setMaximumSize(new Dimension(Integer.MAX_VALUE,120));

        JPanel titleRow=new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setBorder(new EmptyBorder(0,0,8,0));
        JPanel left=UIFactory.transparent(new FlowLayout(FlowLayout.LEFT,6,0));
        left.add(new JLabel("🎓"){{setFont(new Font("Segoe UI Emoji",Font.PLAIN,13));}}); 
        left.add(UIFactory.sectionLabel("Asignación Académica"));
        titleRow.add(left,BorderLayout.WEST);
        JButton editBtn=UIFactory.linkBtn("✏ Editar Asignación",Theme.PRIMARY);
        editBtn.addActionListener(e->toggleInfoAcad(true,editBtn));
        titleRow.add(editBtn,BorderLayout.EAST);
        infoAcadCard.add(titleRow,BorderLayout.NORTH);

        infoAcadView=buildAcadView();
        infoAcadCard.add(infoAcadView,BorderLayout.CENTER);

        infoAcadEdit=buildAcadEdit(editBtn);
        infoAcadEdit.setVisible(false);
        infoAcadCard.add(infoAcadEdit,BorderLayout.SOUTH);

        return infoAcadCard;
    }

    private JPanel buildAcadView(){
        JPanel g=new JPanel(new GridLayout(1,2,20,0));
        g.setOpaque(false);
        g.add(field("Materia de Práctica",materia));
        g.add(field("Horas Requeridas",horasRequeridas+" horas"));
        return g;
    }
    @SuppressWarnings("unused")
    private JPanel buildAcadEdit(JButton _editBtn){
        JPanel g=new JPanel(new GridLayout(1,2,12,0));
        g.setOpaque(false);
        eMateria=UIFactory.textField("Materia"); eMateria.setText(materia);
        eHoras=UIFactory.textField("160"); eHoras.setText(String.valueOf(horasRequeridas));
        g.add(UIFactory.labeledField("Materia de Práctica",eMateria));
        g.add(UIFactory.labeledField("Horas Requeridas",eHoras));
        return g;
    }
    private void toggleInfoAcad(boolean editing, JButton editBtn){
        infoAcadView.setVisible(!editing);
        infoAcadEdit.setVisible(editing);
        if(editing){
            editBtn.setText("✓ Guardar");
            editBtn.removeActionListener(editBtn.getActionListeners()[0]);
            editBtn.addActionListener(e->{
                materia=eMateria.getText().trim();
                try{ horasRequeridas=Integer.parseInt(eHoras.getText().trim()); }catch(NumberFormatException ex){}
                infoAcadCard.remove(infoAcadView);
                infoAcadView=buildAcadView();
                infoAcadCard.add(infoAcadView,BorderLayout.CENTER);
                editBtn.setText("✏ Editar Asignación");
                editBtn.removeActionListener(editBtn.getActionListeners()[0]);
                editBtn.addActionListener(ev->toggleInfoAcad(true,editBtn));
                toggleInfoAcad(false,editBtn);
            });
        }
        revalidate(); repaint();
    }

    // ── Documentos ────────────────────────────────────────────────────────────

    private JPanel buildDocumentosCard(){
        // Construye la tarjeta para ver y gestionar documentos del estudiante.
        JPanel card=UIFactory.card();
        card.setLayout(new BorderLayout());
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE,400));

        // Header bar
        JPanel bar=new JPanel(new BorderLayout());
        bar.setBackground(new Color(0xf9fafb));
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,Theme.BORDER),
            new EmptyBorder(10,16,10,16)));
        JPanel barLeft=UIFactory.transparent(new FlowLayout(FlowLayout.LEFT,8,0));
        barLeft.add(new JLabel("📄"){{setFont(new Font("Segoe UI Emoji",Font.PLAIN,15));}}); 
        barLeft.add(UIFactory.h3("Verificación de Documentos"));
        bar.add(barLeft,BorderLayout.WEST);
        card.add(bar,BorderLayout.NORTH);

        // Info strip
        JPanel infoStrip=new JPanel(new BorderLayout());
        infoStrip.setBackground(new Color(0xeff6ff));
        infoStrip.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,new Color(0xbfdbfe)),
            new EmptyBorder(8,16,8,16)));
        JLabel infoLbl=UIFactory.small("Revise los documentos cargados. Una vez que todos sean Aprobados, podrá habilitar al estudiante para asignación.");
        infoLbl.setForeground(Theme.TEXT_SECONDARY);
        infoStrip.add(infoLbl,BorderLayout.CENTER);
        card.add(infoStrip,BorderLayout.NORTH);

        JPanel center=new JPanel(new BorderLayout());
        center.add(infoStrip,BorderLayout.NORTH);

        // Table
        String[] cols={"Documento","Estado Actual","Acción del Director"};
        docModel=new DefaultTableModel(cols,0){
            @Override public boolean isCellEditable(int r,int c){ return c==2; }
        };
        JTable t=new JTable(docModel);
        UIFactory.styleTable(t);
        t.getColumnModel().getColumn(1).setMaxWidth(160);
        t.getColumnModel().getColumn(1).setCellRenderer(new DocEstadoRenderer());
        t.getColumnModel().getColumn(2).setPreferredWidth(180);
        t.getColumnModel().getColumn(2).setCellRenderer(new DocAccionesRenderer());
        t.getColumnModel().getColumn(2).setCellEditor(new DocAccionesEditor(t));
        refreshDocTable();
        center.add(UIFactory.tableScroll(t),BorderLayout.CENTER);

        // Footer habilitación
        center.add(buildHabilitarFooter(),BorderLayout.SOUTH);
        card.add(center,BorderLayout.CENTER);
        return card;
    }

    private void refreshDocTable(){
        if(docModel==null) return;
        docModel.setRowCount(0);
        for(DB.Documento d:documentos)
            docModel.addRow(new Object[]{ (d.nombreArchivo==null?d.tipoDocumento:d.nombreArchivo)+"\nCargado: "+(d.fechaCarga==null?"—":d.fechaCarga.toString()), d.estadoDoc, d });
    }

    private JPanel buildHabilitarFooter(){
        JPanel p=new JPanel(new BorderLayout());
        p.setBackground(new Color(0xf9fafb));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1,0,0,0,Theme.BORDER),
            new EmptyBorder(14,16,14,16)));

        JPanel center=new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center,BoxLayout.Y_AXIS));

        JLabel hTitle=UIFactory.h3("Decisión de Asignación a Práctica");
        hTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(hTitle); center.add(UIFactory.gap(6));

        boolean allValid=documentos.stream().allMatch(d->"Aprobado".equals(d.estadoDoc));

        JLabel hMsg;
        if(allValid){
            hMsg=UIFactory.small("Todos los documentos han sido verificados. Puede habilitar al estudiante para asignación.");
        } else {
            hMsg=UIFactory.small("Debe verificar y marcar todos los documentos como \"Aprobados\" antes de habilitar al estudiante.");
        }
        hMsg.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(hMsg); center.add(UIFactory.gap(10));

        habilitarBtn=listoParaAsignacion
            ? UIFactory.outlineBtn("⏳ Revertir a Pendiente")
            : UIFactory.primaryBtn("✓ Marcar como Listo para Asignación");
        habilitarBtn.setEnabled(allValid);
        habilitarBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        habilitarBtn.addActionListener(e->toggleHabilitar());
        center.add(habilitarBtn);

        p.add(center,BorderLayout.CENTER);
        return p;
    }

    private void toggleHabilitar(){
        listoParaAsignacion=!listoParaAsignacion;
        statusBadge.setText(listoParaAsignacion?"✓ Listo para Asignación":"⏳ Asignación Pendiente");
        // recolor badge
        JOptionPane.showMessageDialog(this,
            listoParaAsignacion
                ?"✅ Estudiante marcado como LISTO para asignación."
                :"⚠ Estudiante revertido a estado PENDIENTE.",
            "Estado actualizado",JOptionPane.INFORMATION_MESSAGE);
    }

    class DocEstadoRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int row,int col){
            String val=v==null?"":v.toString();
            JLabel badge;
            if(val.equals("Aprobado"))  badge=UIFactory.badgeValido();
                else if(val.equals("Rechazado")) badge=UIFactory.badgeModificar();
            else badge=UIFactory.badgeRevision();
            JPanel p=UIFactory.transparent(new FlowLayout(FlowLayout.LEFT,14,9));
            p.setBackground(s?Theme.PRIMARY_LIGHT:(row%2==0?Color.WHITE:new Color(0xfafafa)));
            p.add(badge); return p;
        }
    }

    class DocAccionesRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int row,int col){
            return buildDocAcciones((DB.Documento)v,row,false);
        }
    }
    class DocAccionesEditor extends AbstractCellEditor implements TableCellEditor {
        DocAccionesEditor(JTable t){}
        @Override public Object getCellEditorValue(){ return null; }
        @Override public Component getTableCellEditorComponent(JTable t,Object v,boolean s,int row,int col){
            return buildDocAcciones((DB.Documento)v,row,true);
        }
    }
    private JPanel buildDocAcciones(DB.Documento doc, int row, boolean live){
        JPanel p=new JPanel(new FlowLayout(FlowLayout.RIGHT,6,7));
        p.setBackground(row%2==0?Color.WHITE:new Color(0xfafafa));

        JButton verBtn=UIFactory.outlineBtn("📄 Ver");
        JButton validarBtn=UIFactory.blueOutlineBtn("✓ Aprobar");
        JButton rechazarBtn=UIFactory.dangerBtn("✗ Rechazar");

        // Resaltar el estado activo
        String estado = doc.estadoDoc == null ? "Pendiente" : doc.estadoDoc;
        if("Válido".equals(estado)){ validarBtn.setForeground(Theme.GREEN_TEXT); }
        if("Modificar".equals(estado)){ rechazarBtn.setForeground(Theme.RED_TEXT); }

        if(live){
            verBtn.addActionListener(e->{
                try {
                    byte[] data = DB.descargarDocumento(doc.idDocumento);
                    if (data == null) { JOptionPane.showMessageDialog(this, "No hay contenido para este documento.", "Ver Documento", JOptionPane.INFORMATION_MESSAGE); return; }
                    var tmp = Files.createTempFile("doc_", "_" + (doc.nombreArchivo != null ? doc.nombreArchivo.replaceAll("[^a-zA-Z0-9.\\-_]","") : "file.bin"));
                    Files.write(tmp, data);
                    java.awt.Desktop.getDesktop().open(tmp.toFile());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error al abrir el documento: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            validarBtn.addActionListener(e->{
                if (DB.cambiarEstadoDocumento(doc.idDocumento, "Aprobado")) { loadProfileDocs(); refreshDocTable(); }
            });
            rechazarBtn.addActionListener(e->{
                if (DB.cambiarEstadoDocumento(doc.idDocumento, "Rechazado")) { loadProfileDocs(); refreshDocTable(); }
            });
        }
        p.add(verBtn); p.add(validarBtn); p.add(rechazarBtn);
        return p;
    }

    private void loadProfileDocs(){
        try {
            documentos.clear();
            if (this.cedula != null) documentos.addAll(DB.listarDocumentos(this.cedula));
            listoParaAsignacion = DB.todosDocumentosValidos(this.cedula);
        } catch (Exception e) { System.err.println("Error cargando documentos: " + e.getMessage()); }
    }

    // ── Historial de Prácticas ────────────────────────────────────────────────

    private JPanel buildHistorialSection(){
        JPanel section=UIFactory.transparent(new BorderLayout(0,12));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));

        JPanel hRow=UIFactory.transparent(new FlowLayout(FlowLayout.LEFT,8,0));
        hRow.add(new JLabel("📋"){{setFont(new Font("Segoe UI Emoji",Font.PLAIN,18));}}); 
        hRow.add(UIFactory.h2("Historial de Prácticas"));
        section.add(hRow,BorderLayout.NORTH);

        JPanel list=new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list,BoxLayout.Y_AXIS));

        for(Practica pr:historial){
            list.add(buildPracticaCard(pr));
            list.add(UIFactory.gap(10));
        }
        section.add(list,BorderLayout.CENTER);
        return section;
    }

    private JPanel buildPracticaCard(Practica pr){
        boolean enCurso=pr.estado.equals("En Curso");
        JPanel card=new JPanel(new BorderLayout(16,0)){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0,0,getWidth()-1,getHeight()-1,Theme.RADIUS,Theme.RADIUS);
                g2.setColor(enCurso?Theme.PRIMARY:Theme.BORDER);
                g2.drawRoundRect(0,0,getWidth()-2,getHeight()-2,Theme.RADIUS,Theme.RADIUS);
                if(enCurso){ g2.setStroke(new BasicStroke(2)); g2.drawRoundRect(1,1,getWidth()-3,getHeight()-3,Theme.RADIUS,Theme.RADIUS); }
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(16,18,16,18));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE,140));

        // Left content
        JPanel left=new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left,BoxLayout.Y_AXIS));

        // Title + badge
        JPanel titleRow=UIFactory.transparent(new FlowLayout(FlowLayout.LEFT,10,0));
        JLabel titleLbl=new JLabel(pr.titulo);
        titleLbl.setFont(Theme.FONT_H2); titleLbl.setForeground(Theme.TEXT_PRIMARY);
        JLabel stateBadge=enCurso?UIFactory.badgeEnCurso():UIFactory.badgeFinalizada();
        titleRow.add(titleLbl); titleRow.add(stateBadge);
        left.add(titleRow); left.add(UIFactory.gap(6));

        // Institution + period
        JPanel detRow=UIFactory.transparent(new FlowLayout(FlowLayout.LEFT,20,0));
        detRow.add(UIFactory.body("🏢 "+pr.institucion));
        JLabel pLabel=UIFactory.body("Período: "); pLabel.setForeground(Theme.TEXT_MUTED);
        detRow.add(pLabel); detRow.add(UIFactory.body(pr.periodo));
        left.add(detRow); left.add(UIFactory.gap(8));

        // Progress bar
        JPanel progPanel=UIFactory.transparent(new BorderLayout(0,4));
        progPanel.setMaximumSize(new Dimension(500,30));
        JPanel progLabels=UIFactory.transparent(new BorderLayout());
        progLabels.add(UIFactory.small("Horas: "+pr.horasHechas+"/"+pr.horasTotal),BorderLayout.WEST);
        progLabels.add(UIFactory.small(pr.porcentaje()+"%"),BorderLayout.EAST);
        JProgressBar pb=UIFactory.progressBar(pr.horasHechas,pr.horasTotal,enCurso);
        progPanel.add(progLabels,BorderLayout.NORTH);
        progPanel.add(pb,BorderLayout.SOUTH);
        left.add(progPanel);

        card.add(left,BorderLayout.CENTER);

        // Right: nota + button
        JPanel right=new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right,BoxLayout.Y_AXIS));
        right.setPreferredSize(new Dimension(140,0));

        if(pr.notaFinal!=null){
            JPanel notaBox=UIFactory.transparent(new FlowLayout(FlowLayout.RIGHT,0,0));
            JPanel notaCol=new JPanel(new GridLayout(2,1,0,0));
            notaCol.setOpaque(false);
            JLabel notaLbl=UIFactory.small("NOTA FINAL");
            notaLbl.setHorizontalAlignment(SwingConstants.RIGHT);
            JLabel notaVal=new JLabel(pr.notaFinal,SwingConstants.RIGHT);
            notaVal.setFont(new Font("SansSerif",Font.BOLD,26));
            notaVal.setForeground(Theme.TEXT_PRIMARY);
            notaCol.add(notaLbl); notaCol.add(notaVal);
            notaBox.add(notaCol);
            right.add(notaBox);
            right.add(UIFactory.gap(8));
        }

        JButton verBtn=enCurso?UIFactory.primaryBtn("Ver Detalles →"):UIFactory.outlineBtn("Ver Detalles →");
        verBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);
        verBtn.addActionListener(e->JOptionPane.showMessageDialog(this,"Detalle de práctica: "+pr.titulo,"Detalle",JOptionPane.INFORMATION_MESSAGE));
        right.add(verBtn);

        card.add(right,BorderLayout.EAST);
        return card;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private JPanel field(String label, String val){
        JPanel p=new JPanel(new GridLayout(2,1,0,3)); p.setOpaque(false);
        p.add(UIFactory.sectionLabel(label));
        JLabel v=new JLabel(val); v.setFont(Theme.FONT_LABEL); v.setForeground(Theme.TEXT_PRIMARY);
        p.add(v); return p;
    }
    private JPanel fieldBadge(String label, String val){
        JPanel p=new JPanel(new GridLayout(2,1,0,3)); p.setOpaque(false);
        p.add(UIFactory.sectionLabel(label));
        JLabel badge=UIFactory.badge(val,Theme.BLUE_BG,Theme.BLUE_TEXT,Theme.BLUE_BORDER);
        p.add(badge); return p;
    }
}
