package sigep;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import sigep.db.DB;

/**
 * Dashboard del Estudiante — replica StudentDashboard.tsx
 */
// Uso: muestra información personal del estudiante, documentos, selección
// de plazas y historial de prácticas.
public class PanelEstudiante extends JPanel {

    static class Plaza { @SuppressWarnings("unused") int id; String nombre; int cupos;
        Plaza(int i,String n,int c){ id=i;nombre=n;cupos=c; }
    }
    static class Practica { String titulo,estado,institucion,periodo; int horasHechas,horasTotal; String nota;
        Practica(String t,String e,String i,String p,int h,int ht,String n){
            titulo=t;estado=e;institucion=i;periodo=p;horasHechas=h;horasTotal=ht;nota=n;
        }
        int pct(){ return Math.round(horasHechas*100f/horasTotal); }
    }

    private String nombre="", apellido="", cedula="",
        correo="", programa="", semestre="";
    private boolean listoParaAsignacion=false;
    private String estadoAsignacion="Pendiente";

    private final List<Plaza>     plazas    = new ArrayList<>();
    private final java.util.List<DB.Documento> documentos = new ArrayList<>();
    private final List<Practica>  historial = new ArrayList<>();

    private boolean solicitudEnviada=false;
    private JComboBox<String> opcion1Box, opcion2Box;
    private JButton enviarBtn;
    private DefaultTableModel docModel;

    public PanelEstudiante(){
        // Constructor: inicializa datos desde la sesión del usuario y la BD
        plazas.clear(); historial.clear();
        if (LoginWindow.usuarioActual != null) {
            var u = LoginWindow.usuarioActual;
            nombre = u.nombre == null ? "" : u.nombre;
            apellido = u.apellido == null ? "" : u.apellido;
            correo = u.correo == null ? "" : u.correo;
            cedula = u.cedula == null ? "" : u.cedula;
            programa = DB.nombreProgramaPorId(u.idPrograma);
            semestre = u.semestre == null ? "" : ("S" + u.semestre);
        }
        try {
            var insts = DB.listarInstitucionesVigentes();
            for (var i : insts) plazas.add(new Plaza(0, i.toString(), i.cuposDisponibles()));
        } catch (Exception ignored) {}
        try { loadDocuments(); } catch (Exception ignored) {}
        try {
            if (cedula != null && !cedula.isEmpty()) {
                var asigs = DB.asignacionesPorEstudiante(cedula);
                for (var a : asigs) {
                    historial.add(new Practica(a.periodoAcademico, a.estadoAsignacion, a.nombreInstitucion == null ? a.idInstitucion : a.nombreInstitucion, a.periodoAcademico, 0, 160, null));
                }
                estadoAsignacion = historial.stream().anyMatch(h -> "Asignado".equals(h.estado)) ? "Asignado" : "Pendiente";
            }
        } catch (Exception ignored) {}

        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(Theme.PADDING,Theme.PADDING,Theme.PADDING,Theme.PADDING));

        JPanel content=new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));

        content.add(buildHeader());          content.add(UIFactory.gap(14));
        content.add(buildInfoPersonal());    content.add(UIFactory.gap(14));
        if(listoParaAsignacion) { content.add(buildSeleccionPlazas()); content.add(UIFactory.gap(14)); }
        content.add(buildDocumentos());      content.add(UIFactory.gap(14));
        content.add(buildHistorial());       content.add(UIFactory.gap(20));

        JScrollPane sp=new JScrollPane(content);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Theme.BG_PAGE);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(sp,BorderLayout.CENTER);
    }

    private JPanel buildHeader(){
        // Construye el header con avatar, nombre y badge de estado.
        JPanel p=UIFactory.transparent(new BorderLayout(12,0));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,Theme.BORDER),
            new EmptyBorder(0,0,14,0)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE,70));

        JLabel av=UIFactory.avatar("AP",48,Theme.PRIMARY_LIGHT,Theme.PRIMARY);
        JPanel nameCol=UIFactory.transparent(new GridLayout(2,1,0,3));
        JLabel nameLbl=new JLabel(nombre+" "+apellido);
        nameLbl.setFont(Theme.FONT_H1); nameLbl.setForeground(Theme.TEXT_PRIMARY);
        JLabel subLbl=UIFactory.muted("🎓  C.C: "+cedula+"  •  "+programa);
        nameCol.add(nameLbl); nameCol.add(subLbl);

        JPanel left=UIFactory.transparent(new FlowLayout(FlowLayout.LEFT,12,0));
        left.add(av); left.add(nameCol);

        // Mostrar estado según bandera habilitadoAsig del usuario (0 = pendiente, 1 = listo)
        JLabel statusBadge;
        if (LoginWindow.usuarioActual != null) {
            if (LoginWindow.usuarioActual.habilitadoAsig == 1) {
                statusBadge = UIFactory.badge("✓ Listo para Asignación", Theme.GREEN_BG, Theme.GREEN_TEXT, Theme.GREEN_BORDER);
            } else {
                statusBadge = UIFactory.badge("⏳ Pendiente por asignacion", Theme.YELLOW_BG, Theme.YELLOW_TEXT, Theme.YELLOW_BORDER);
            }
        } else {
            statusBadge = estadoAsignacion.equals("Asignado")
                ? UIFactory.badge("✓ Asignado", Theme.GREEN_BG, Theme.GREEN_TEXT, Theme.GREEN_BORDER)
                : UIFactory.badge("⏳ Pendiente por asignación", Theme.YELLOW_BG, Theme.YELLOW_TEXT, Theme.YELLOW_BORDER);
        }

        p.add(left,BorderLayout.WEST);
        p.add(statusBadge,BorderLayout.EAST);
        return p;
    }

    private JPanel buildInfoPersonal(){
        // Crea y devuelve la card con la información personal del estudiante.
        JPanel card=UIFactory.card();
        card.setLayout(new BorderLayout(0,12));
        card.setBorder(BorderFactory.createCompoundBorder(card.getBorder(),new EmptyBorder(16,18,16,18)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE,110));

        JPanel titleRow=UIFactory.transparent(new FlowLayout(FlowLayout.LEFT,8,0));
        titleRow.add(new JLabel("👤"){ { setFont(new Font("Segoe UI Emoji",Font.PLAIN,13)); } });
        JLabel tLbl=UIFactory.sectionLabel("Información Personal");
        titleRow.add(tLbl);
        JPanel top=UIFactory.transparent(new BorderLayout());
        top.add(titleRow,BorderLayout.WEST);
        top.add(UIFactory.hSep(),BorderLayout.SOUTH);
        card.add(top,BorderLayout.NORTH);

        JPanel grid=new JPanel(new GridLayout(1,3,20,0));
        grid.setOpaque(false);
        grid.add(field("Nombre Completo",nombre+" "+apellido));
        grid.add(field("Correo Electrónico",correo));
        grid.add(fieldBadge("Semestre Actual",semestre));
        card.add(grid,BorderLayout.CENTER);
        return card;
    }

    private JPanel buildSeleccionPlazas(){
        // Crea la sección para seleccionar plazas: opciones y botón de envío.
        JPanel card=UIFactory.blueSection();
        card.setLayout(new BorderLayout(0,12));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE,220));

        JPanel titleRow=UIFactory.transparent(new FlowLayout(FlowLayout.LEFT,8,0));
        titleRow.add(new JLabel("📍"){ { setFont(new Font("Segoe UI Emoji",Font.PLAIN,16)); } });
        titleRow.add(UIFactory.h3("Selección de Plazas de Práctica"));
        JLabel desc=UIFactory.small("Tus documentos han sido validados. Elige dos opciones de instituciones para tu asignación. Revisa los cupos disponibles.");
        desc.setForeground(Theme.TEXT_SECONDARY);

        JPanel top=new JPanel(new GridLayout(2,1,0,4));
        top.setOpaque(false);
        top.add(titleRow); top.add(desc);
        card.add(top,BorderLayout.NORTH);

        if(solicitudEnviada){
            JPanel ok=new JPanel(new FlowLayout(FlowLayout.LEFT,12,10));
            ok.setBackground(Theme.GREEN_BG);
            ok.setBorder(BorderFactory.createLineBorder(Theme.GREEN_BORDER));
            JLabel icon=new JLabel("✅"); icon.setFont(new Font("Segoe UI Emoji",Font.PLAIN,16));
            JPanel msg=new JPanel(new GridLayout(2,1,0,2));
            msg.setOpaque(false);
            JLabel h=new JLabel("¡Solicitud enviada con éxito!");
            h.setFont(Theme.FONT_LABEL); h.setForeground(Theme.GREEN_TEXT);
            JLabel b=UIFactory.small("Tus preferencias han sido registradas. El director revisará tu solicitud para la asignación final.");
            b.setForeground(Theme.GREEN_TEXT);
            msg.add(h); msg.add(b);
            ok.add(icon); ok.add(msg);
            card.add(ok,BorderLayout.CENTER);
        } else {
            String[] plazaOpts=plazas.stream()
                .map(pl->pl.nombre+" - "+pl.cupos+(pl.cupos==1?" cupo":" cupos")+" disponibles")
                .toArray(String[]::new);
            String[] opts=new String[plazaOpts.length+1];
            opts[0]="Selecciona una institución...";
            System.arraycopy(plazaOpts,0,opts,1,plazaOpts.length);

            opcion1Box=UIFactory.comboBox(opts);
            opcion2Box=UIFactory.comboBox(opts);
            DefaultListCellRenderer dRenderer=new DefaultListCellRenderer(){
                @Override public Component getListCellRendererComponent(JList<?> l,Object v,int i,boolean s,boolean f){
                    Component c=super.getListCellRendererComponent(l,v,i,s,f);
                    if(i>0 && plazas.get(i-1).cupos==0){ setEnabled(false); setForeground(Theme.TEXT_MUTED); }
                    return c;
                }
            };
            opcion1Box.setRenderer(dRenderer);
            opcion2Box.setRenderer(dRenderer);

            JPanel selGrid=new JPanel(new GridLayout(1,2,16,0));
            selGrid.setOpaque(false);
            selGrid.add(UIFactory.labeledField("Opción 1 (Prioridad Alta)",opcion1Box));
            selGrid.add(UIFactory.labeledField("Opción 2 (Alternativa)",opcion2Box));

            JPanel footer=UIFactory.transparent(new FlowLayout(FlowLayout.RIGHT));
            enviarBtn=UIFactory.primaryBtn("📨  Enviar Preferencias");
            enviarBtn.addActionListener(e->enviarPreferencias());
            footer.add(enviarBtn);

            JPanel body=new JPanel(new BorderLayout(0,10));
            body.setOpaque(false);
            body.add(selGrid,BorderLayout.CENTER);
            body.add(footer,BorderLayout.SOUTH);
            card.add(body,BorderLayout.CENTER);
        }
        return card;
    }

    private void enviarPreferencias(){
        // Valida selecciones y confirma envío de preferencias al usuario.
        int i1=opcion1Box.getSelectedIndex(), i2=opcion2Box.getSelectedIndex();
        if(i1==0||i2==0){
            JOptionPane.showMessageDialog(this,"Seleccione las dos opciones.","Atención",JOptionPane.WARNING_MESSAGE); return;
        }
        if(i1==i2){
            JOptionPane.showMessageDialog(this,"Las dos opciones deben ser diferentes.","Atención",JOptionPane.WARNING_MESSAGE); return;
        }
        solicitudEnviada=true;
        JOptionPane.showMessageDialog(this,
            "✅ Solicitud enviada:\n• Opción 1: "+opcion1Box.getSelectedItem()+"\n• Opción 2: "+opcion2Box.getSelectedItem(),
            "Solicitud Enviada",JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel buildDocumentos(){
        // Construye la card que muestra los documentos del estudiante y acciones.
        JPanel card=UIFactory.card();
        card.setLayout(new BorderLayout());
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE,320));

        JPanel bar=new JPanel(new BorderLayout());
        bar.setBackground(new Color(0xf9fafb));
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,Theme.BORDER),
            new EmptyBorder(10,16,10,16)));
        JPanel bLeft=UIFactory.transparent(new FlowLayout(FlowLayout.LEFT,8,0));
        bLeft.add(new JLabel("📄"){ { setFont(new Font("Segoe UI Emoji",Font.PLAIN,15)); } });
        bLeft.add(UIFactory.h3("Mis Documentos"));
        bar.add(bLeft,BorderLayout.WEST);
        JButton subirBtn=UIFactory.primaryBtn("＋  Subir Documento");
        subirBtn.addActionListener(e->showUploadModal());
        bar.add(subirBtn,BorderLayout.EAST);
        card.add(bar,BorderLayout.NORTH);

        JPanel strip=new JPanel(new BorderLayout());
        strip.setBackground(new Color(0xeff6ff));
        strip.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,new Color(0xbfdbfe)),
            new EmptyBorder(8,16,8,16)));
        JLabel infoLbl=UIFactory.small("Carga y gestiona tus documentos de habilitación. Debes tener todos en estado Aprobado para recibir una asignación.");
        infoLbl.setForeground(Theme.TEXT_SECONDARY);
        strip.add(infoLbl,BorderLayout.CENTER);

        JPanel center=new JPanel(new BorderLayout());
        center.add(strip,BorderLayout.NORTH);

        String[] cols={"Documento","Estado Actual","Acción"};
        docModel=new DefaultTableModel(cols,0){
            @Override public boolean isCellEditable(int r,int c){ return c==2; }
        };
        JTable t=new JTable(docModel);
        UIFactory.styleTable(t);
        t.getColumnModel().getColumn(1).setMaxWidth(160);
        t.getColumnModel().getColumn(1).setCellRenderer(new DocEstadoRenderer());
        t.getColumnModel().getColumn(2).setPreferredWidth(160);
        t.getColumnModel().getColumn(2).setCellRenderer(new DocAccionesRenderer());
        t.getColumnModel().getColumn(2).setCellEditor(new DocAccionesEditor(t));
        refreshDocTable();
        center.add(UIFactory.tableScroll(t),BorderLayout.CENTER);
        card.add(center,BorderLayout.CENTER);
        return card;
    }

    private void refreshDocTable(){
        // Rellena el modelo de la tabla de documentos desde la lista `documentos`.
        if(docModel==null) return;
        docModel.setRowCount(0);
        for(DB.Documento d:documentos)
            docModel.addRow(new Object[]{(d.nombreArchivo==null?d.tipoDocumento:d.nombreArchivo)+"\nCargado: "+(d.fechaCarga==null?"—":d.fechaCarga.toString()), d.estadoDoc, d});
    }

    private void showUploadModal(){
        // Abre un diálogo modal para seleccionar y subir un nuevo documento.
        JDialog dlg=new JDialog((Frame)SwingUtilities.getWindowAncestor(this),"Subir Nuevo Documento",true);
        dlg.setSize(460,340);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel root=new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        dlg.setContentPane(root);

        JPanel body=new JPanel();
        body.setBackground(Color.WHITE);
        body.setLayout(new BoxLayout(body,BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(20,24,10,24));

        JLabel title=UIFactory.h3("⬆  Subir Nuevo Documento");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(title); body.add(UIFactory.gap(16));

        JLabel tipoLbl=UIFactory.sectionLabel("Tipo de Documento"); tipoLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        JComboBox<String> tipoBox=UIFactory.comboBox(
            "Selecciona un tipo...",
            "Cédula de Ciudadanía","Certificado EPS",
            "ARL (Afiliación a Riesgos Laborales)","Póliza Estudiantil",
            "Carta de Presentación","Hoja de Vida","Otro");
        tipoBox.setMaximumSize(new Dimension(Integer.MAX_VALUE,36));
        tipoBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField tipoCustom=UIFactory.textField("Ej. Certificado Médico");
        tipoCustom.setMaximumSize(new Dimension(Integer.MAX_VALUE,36));
        tipoCustom.setAlignmentX(Component.LEFT_ALIGNMENT);
        tipoCustom.setVisible(false);
        JLabel tipoCustomLbl=UIFactory.sectionLabel("Especifique el tipo");
        tipoCustomLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        tipoCustomLbl.setVisible(false);

        tipoBox.addActionListener(e->{
            boolean otro="Otro".equals(tipoBox.getSelectedItem());
            tipoCustomLbl.setVisible(otro); tipoCustom.setVisible(otro);
            dlg.revalidate(); dlg.repaint();
        });

        JLabel archivoLbl=UIFactory.sectionLabel("Archivo"); archivoLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel archivoRow=UIFactory.transparent(new FlowLayout(FlowLayout.LEFT,8,0));
        archivoRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel archivoName = UIFactory.muted("Ningún archivo seleccionado");
        JButton selectFile = UIFactory.outlineBtn("Seleccionar Archivo...");
        final File[] selectedFile = new File[1];
        selectFile.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(dlg) == JFileChooser.APPROVE_OPTION) {
                selectedFile[0] = fc.getSelectedFile();
                archivoName.setText(selectedFile[0].getName());
            }
        });
        archivoRow.add(selectFile); archivoRow.add(archivoName);

        body.add(tipoLbl); body.add(UIFactory.gap(4)); body.add(tipoBox);
        body.add(UIFactory.gap(10));
        body.add(tipoCustomLbl); body.add(UIFactory.gap(4)); body.add(tipoCustom);
        body.add(UIFactory.gap(10));
        body.add(archivoLbl); body.add(UIFactory.gap(4)); body.add(archivoRow);

        root.add(body,BorderLayout.CENTER);

        JPanel footer=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,10));
        footer.setBackground(new Color(0xf9fafb));
        footer.setBorder(BorderFactory.createMatteBorder(1,0,0,0,Theme.BORDER));

        JButton cancelBtn=UIFactory.outlineBtn("Cancelar");
        cancelBtn.addActionListener(e->dlg.dispose());

        JButton uploadBtn=UIFactory.primaryBtn("⬆  Subir Documento");
        uploadBtn.addActionListener(e->{
            String tipo=tipoBox.getSelectedIndex()==0?"":
                tipoBox.getSelectedItem().equals("Otro")?tipoCustom.getText().trim():tipoBox.getSelectedItem().toString();
            // Validaciones: tipo, archivo y usuario/cedula
            if (tipo.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Seleccione el tipo de documento.", "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (selectedFile[0] == null) {
                JOptionPane.showMessageDialog(dlg, "Seleccione un archivo para subir.", "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (LoginWindow.usuarioActual == null || LoginWindow.usuarioActual.cedula == null || LoginWindow.usuarioActual.cedula.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "No se ha detectado la cédula del estudiante. Por favor inicie sesión correctamente.", "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            File file = selectedFile[0];
            try {
                byte[] content = Files.readAllBytes(file.toPath());
                DB.Documento dObj = new DB.Documento();
                dObj.cedulaEstudiante = LoginWindow.usuarioActual != null ? LoginWindow.usuarioActual.cedula : null;
                dObj.tipoDocumento = tipo;
                dObj.nombreArchivo = file.getName();
                dObj.contenidoBlob = content;
                String err = DB.subirDocumentoConBlobSafe(dObj);
                if (err == null) {
                    // Añadir el documento subido al listado local para que aparezca de inmediato
                    if (dObj.cedulaEstudiante == null && LoginWindow.usuarioActual != null) dObj.cedulaEstudiante = LoginWindow.usuarioActual.cedula;
                    if (dObj.fechaCarga == null) dObj.fechaCarga = new java.sql.Timestamp(System.currentTimeMillis());
                    if (dObj.estadoDoc == null) dObj.estadoDoc = "Pendiente";
                    documentos.add(0, dObj);
                    JOptionPane.showMessageDialog(dlg, "✅ Documento subido correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    System.err.println("[PanelEstudiante] documentos after upload: " + documentos.size());
                    for (DB.Documento dd : documentos) System.err.println("  - doc id=" + dd.idDocumento + " nombre=" + dd.nombreArchivo + " cedula=" + dd.cedulaEstudiante);
                    refreshDocTable(); dlg.dispose();
                } else {
                    // Si el mensaje contiene instrucciones DDL sugeridas, mostrar diálogo enriquecido
                    if (err.contains("ALTER TABLE Documentos ADD" ) || err.toLowerCase().contains("nombre_archivo") || err.toLowerCase().contains("contenido_archivo")) {
                        dlg.dispose();
                        showSchemaErrorDialog(err);
                    } else {
                        JOptionPane.showMessageDialog(dlg, "❌ Error al subir el documento:\n" + err, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(dlg, "No se pudo leer el archivo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        footer.add(cancelBtn); footer.add(uploadBtn);
        root.add(footer,BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    /** Muestra un diálogo amigable con instrucciones (DDL) y permite copiar el texto. */
    private void showSchemaErrorDialog(String message) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Error al subir el documento:", true);
        dlg.setSize(700, 380);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(8,8));

        JPanel top = new JPanel(new BorderLayout()); top.setBorder(new EmptyBorder(10,10,0,10));
        JLabel title = new JLabel("No fue posible almacenar el archivo en la base de datos");
        title.setFont(Theme.FONT_H3); top.add(title, BorderLayout.NORTH);
        JLabel hint = UIFactory.small("Revise el mensaje técnico y comparta las sentencias con el administrador de BD.");
        top.add(hint, BorderLayout.SOUTH);
        dlg.add(top, BorderLayout.NORTH);

        JTextArea ta = new JTextArea(message);
        ta.setEditable(false);
        ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        ta.setLineWrap(false);
        JScrollPane sp = new JScrollPane(ta, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setBorder(new EmptyBorder(8,10,8,10));
        dlg.add(sp, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        footer.setBackground(new Color(0xf9fafb));
        JButton copy = UIFactory.primaryBtn("Copiar al portapapeles");
        copy.addActionListener(ev -> {
            try {
                StringSelection sel = new StringSelection(ta.getText());
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
                JOptionPane.showMessageDialog(dlg, "Texto copiado al portapapeles.", "Copiado", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) { JOptionPane.showMessageDialog(dlg, "No fue posible copiar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
        });
        JButton saveFile = UIFactory.outlineBtn("Guardar .sql");
        saveFile.addActionListener(ev -> {
            try {
                // Extraer bloque DDL (líneas que empiezan con ALTER TABLE)
                String text = ta.getText();
                int idx = text.indexOf("ALTER TABLE");
                String toSave = idx >= 0 ? text.substring(idx) : text;
                String outPath = System.getProperty("user.dir") + File.separator + "fix_documentos_ddls.sql";
                try (java.io.FileWriter fw = new java.io.FileWriter(outPath, false)) { fw.write(toSave); }
                // Abrir el explorador en la carpeta del archivo
                java.awt.Desktop.getDesktop().open(new java.io.File(System.getProperty("user.dir")));
                JOptionPane.showMessageDialog(dlg, "Archivo guardado: " + outPath, "Guardado", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "No fue posible guardar el archivo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        JButton close = UIFactory.outlineBtn("Cerrar");
        close.addActionListener(ev -> dlg.dispose());
        footer.add(copy); footer.add(saveFile); footer.add(close);
        dlg.add(footer, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    class DocEstadoRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int row,int col){
            String val=v==null?"":v.toString();
            JLabel badge;
            if(val.equals("Aprobado"))        badge=UIFactory.badgeValido();
            else if(val.equals("Rechazado"))badge=UIFactory.badgeModificar();
            else                            badge=UIFactory.badgeRevision();
            JPanel p=UIFactory.transparent(new FlowLayout(FlowLayout.LEFT,14,9));
            p.setBackground(s?Theme.PRIMARY_LIGHT:(row%2==0?Color.WHITE:new Color(0xfafafa)));
            p.add(badge); return p;
        }
    }
    class DocAccionesRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int row,int col){
            return buildDocAcc((DB.Documento)v,row,false);
        }
    }
    class DocAccionesEditor extends AbstractCellEditor implements TableCellEditor {
        DocAccionesEditor(JTable t){}
        @Override public Object getCellEditorValue(){ return null; }
        @Override public Component getTableCellEditorComponent(JTable t,Object v,boolean s,int row,int col){
            return buildDocAcc((DB.Documento)v,row,true);
        }
    }
    private JPanel buildDocAcc(DB.Documento doc, int row, boolean live){
        JPanel p=new JPanel(new FlowLayout(FlowLayout.RIGHT,6,7));
        p.setBackground(row%2==0?Color.WHITE:new Color(0xfafafa));

        JButton verBtn=UIFactory.outlineBtn("📄 Ver");
        verBtn.addActionListener(e->{
            try {
                byte[] data = DB.descargarDocumento(doc.idDocumento);
                if (data == null) {
                    JOptionPane.showMessageDialog(this, "No hay contenido disponible para este documento.", "Ver Documento", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                String fileName = doc.nombreArchivo != null ? doc.nombreArchivo : (doc.tipoDocumento + ".bin");
                Path tmp = Files.createTempFile("doc_", "_" + fileName.replaceAll("[^a-zA-Z0-9.\\-_]",""));
                Files.write(tmp, data);
                java.awt.Desktop.getDesktop().open(tmp.toFile());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al abrir el documento: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        String estado = doc.estadoDoc == null ? "Pendiente" : doc.estadoDoc;
        // Siempre mostrar Ver; si no está "Aprobado" permitir eliminar
        JButton deleteBtn = UIFactory.dangerBtn("Eliminar");
        if (live) deleteBtn.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this, "¿Eliminar este documento? Esta acción no se puede deshacer.", "Confirmar eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok == JOptionPane.YES_OPTION) {
                if (doc.idDocumento != null && DB.eliminarDocumento(doc.idDocumento)) {
                    // Forzar recarga desde BD para asegurar consistencia y que la fila desaparezca
                    SwingUtilities.invokeLater(() -> {
                        loadDocuments();
                        refreshDocTable();
                    });
                } else {
                    JOptionPane.showMessageDialog(this, "No fue posible eliminar el documento en la base de datos.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        p.add(verBtn);
        if (!"Aprobado".equals(estado)) p.add(deleteBtn);
        return p;
    }

    private void loadDocuments(){
        documentos.clear();
        try {
            if (LoginWindow.usuarioActual != null && LoginWindow.usuarioActual.cedula != null) {
                documentos.addAll(DB.listarDocumentos(LoginWindow.usuarioActual.cedula));
            }
        } catch (Exception e) {
            System.err.println("No se pudieron cargar los documentos: " + e.getMessage());
        }
    }

    private JPanel buildHistorial(){
        JPanel section=UIFactory.transparent(new BorderLayout(0,12));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));

        JPanel hRow=UIFactory.transparent(new FlowLayout(FlowLayout.LEFT,8,0));
        hRow.add(new JLabel("📋"){ { setFont(new Font("Segoe UI Emoji",Font.PLAIN,20)); } });
        hRow.add(UIFactory.h2("Historial de Prácticas"));
        section.add(hRow,BorderLayout.NORTH);

        JPanel list=new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list,BoxLayout.Y_AXIS));
        for(Practica pr:historial){ list.add(buildPracticaCard(pr)); list.add(UIFactory.gap(10)); }
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
                g2.fillRoundRect(0,0,getWidth(),getHeight(),Theme.RADIUS,Theme.RADIUS);
                g2.setColor(Theme.BORDER); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,Theme.RADIUS,Theme.RADIUS);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(16,18,16,18));

        JLabel title=UIFactory.h3(pr.titulo);
        title.setForeground(Theme.TEXT_PRIMARY);
        card.add(title,BorderLayout.NORTH);

        JPanel center=new JPanel(new BorderLayout());
        center.setOpaque(false);
        card.add(center,BorderLayout.CENTER);

        JPanel info=new JPanel(new GridLayout(1,3,8,0));
        info.setOpaque(false);
        info.add(UIFactory.small("Institución: "+pr.institucion));
        info.add(UIFactory.small("Periodo: "+pr.periodo));
        info.add(UIFactory.small("Estado: "+pr.estado));
        center.add(info,BorderLayout.CENTER);

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
