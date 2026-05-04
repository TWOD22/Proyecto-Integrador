package sigep;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import sigep.db.DB;

/**
 * Portal Docente Evaluador — replica AssessorView.tsx
 * Layout: lista de estudiantes (izq) + formulario de calificación (der)
 */
// Uso: interfaz para que docentes/asesores califiquen estudiantes asignados.
public class PanelEvaluador extends JPanel {

    static class EstudianteEval {
        int id; String nombre,cedula,institucion,estado; String nota;
        EstudianteEval(int i,String n,String c,String ins,String e,String nota){
            id=i;nombre=n;cedula=c;institucion=ins;estado=e;this.nota=nota;
        }
    }

    private final List<EstudianteEval> estudiantes=new ArrayList<>();
    private EstudianteEval selected;

    // Form
    private JTextField gradeField;
    private JTextArea  obsArea;
    private JPanel     detailPanel;
    private JLabel     headerName,headerSub;
    private JButton    submitBtn;

    public PanelEvaluador(){
        // Constructor: carga asignaciones relevantes para el evaluador y construye la UI.
        try {
            var asigns = DB.asignacionesActivas();
            String myCedula = LoginWindow.usuarioActual != null ? LoginWindow.usuarioActual.cedula : null;
            for (var a : asigns) {
                if (myCedula != null && (myCedula.equals(a.cedulaDocente) || myCedula.equals(a.cedulaAsesor))) {
                    estudiantes.add(new EstudianteEval(0, a.nombreEstudiante == null ? "Estudiante" : a.nombreEstudiante,
                        a.cedulaEstudiante, a.nombreInstitucion == null ? a.idInstitucion : a.nombreInstitucion,
                        a.estadoAsignacion, null));
                }
            }
        } catch (Exception e) {
            // fallback: mantener la lista vacía
        }
        if (!estudiantes.isEmpty()) selected = estudiantes.get(0);
        setOpaque(false);
        setLayout(new BorderLayout(0,0));
        setBorder(new EmptyBorder(Theme.PADDING,Theme.PADDING,Theme.PADDING,Theme.PADDING));
        build();
    }

    

    private void build(){
        // Construye la estructura principal: header + lista de estudiantes + detalle.
        JPanel header=UIFactory.transparent(new BorderLayout());
        header.setBorder(new EmptyBorder(0,0,14,0));
        JPanel hLeft=UIFactory.transparent(new FlowLayout(FlowLayout.LEFT,8,0));
        hLeft.add(new JLabel("✅"){{setFont(new Font("Segoe UI Emoji",Font.PLAIN,20));}}); 
        JPanel hCol=UIFactory.transparent(new GridLayout(2,1,0,2));
        hCol.add(UIFactory.h2("Calificaciones y Evaluaciones"));
        hCol.add(UIFactory.muted("Gestione las calificaciones finales de sus estudiantes asignados."));
        hLeft.add(hCol);
        header.add(hLeft,BorderLayout.WEST);
        JPanel topBlock=UIFactory.transparent(new BorderLayout());
        topBlock.add(header,BorderLayout.NORTH);
        topBlock.add(UIFactory.hSep(),BorderLayout.SOUTH);
        add(topBlock,BorderLayout.NORTH);

        // Main split: lista izq + detalle der
        JPanel main=new JPanel(new BorderLayout(16,0));
        main.setOpaque(false);
        main.setBorder(new EmptyBorder(16,0,0,0));

        main.add(buildStudentList(), BorderLayout.WEST);
        detailPanel=buildDetailPanel();
        main.add(detailPanel,BorderLayout.CENTER);

        add(main,BorderLayout.CENTER);
    }

    // ── Lista de estudiantes ──────────────────────────────────────────────────

    private JPanel buildStudentList(){
        // Construye y devuelve la lista lateral con los estudiantes a evaluar.
        JPanel card=UIFactory.card();
        card.setLayout(new BorderLayout());
        card.setPreferredSize(new Dimension(260,0));

        // Header de la lista
        JPanel bar=new JPanel(new BorderLayout());
        bar.setBackground(new Color(0xf9fafb));
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,Theme.BORDER),
            new EmptyBorder(10,14,10,14)));
        JPanel bLeft=UIFactory.transparent(new FlowLayout(FlowLayout.LEFT,8,0));
        bLeft.add(new JLabel("👥"){{setFont(new Font("Segoe UI Emoji",Font.PLAIN,14));}}); 
        bLeft.add(UIFactory.h3("Estudiantes a Cargo"));
        bar.add(bLeft,BorderLayout.CENTER);
        card.add(bar,BorderLayout.NORTH);

        // Items
        JPanel listPanel=new JPanel();
        listPanel.setOpaque(false);
        listPanel.setLayout(new BoxLayout(listPanel,BoxLayout.Y_AXIS));
        listPanel.setBorder(new EmptyBorder(8,8,8,8));

        for(EstudianteEval est:estudiantes){
            listPanel.add(buildStudentItem(est));
            listPanel.add(UIFactory.gap(4));
        }

        JScrollPane sp=new JScrollPane(listPanel);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Color.WHITE);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        card.add(sp,BorderLayout.CENTER);
        return card;
    }

    private JPanel buildStudentItem(EstudianteEval est){
        // Crea un item visual para cada estudiante en la lista lateral.
        boolean isSelected=(selected!=null&&selected.id==est.id);
        JPanel item=new JPanel(new BorderLayout(8,0)){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isSelected?Theme.PRIMARY_LIGHT:Color.WHITE);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),Theme.RADIUS,Theme.RADIUS);
                // border-left accent
                if(isSelected){ g2.setColor(Theme.PRIMARY); g2.fillRect(0,0,4,getHeight()); }
                else { g2.setColor(Theme.BORDER); g2.fillRect(0,0,1,getHeight()); }
                g2.dispose();
            }
        };
        item.setOpaque(false);
        item.setBorder(new EmptyBorder(10,12,10,10));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE,70));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Name + status
        JPanel nameRow=new JPanel(new BorderLayout(4,0));
        nameRow.setOpaque(false);
        JLabel nameLbl=new JLabel(est.nombre);
        nameLbl.setFont(Theme.FONT_LABEL); nameLbl.setForeground(Theme.TEXT_PRIMARY);
        nameRow.add(nameLbl,BorderLayout.WEST);

        JLabel statBadge;
        if(est.estado.equals("Evaluado")){
            statBadge=UIFactory.badge("✓ "+est.nota,Theme.GREEN_BG,Theme.GREEN_TEXT,Theme.GREEN_BORDER);
        } else if(est.estado.equals("En Revisión")){
            statBadge=UIFactory.badge("Rev",Theme.YELLOW_BG,Theme.YELLOW_TEXT,Theme.YELLOW_BORDER);
        } else {
            statBadge=UIFactory.badge("Pend",Theme.BG_MUTED,Theme.TEXT_MUTED,Theme.BORDER);
        }
        nameRow.add(statBadge,BorderLayout.EAST);

        JLabel ccLbl=UIFactory.small("CC: "+est.cedula);
        JLabel instLbl=UIFactory.small(est.institucion);

        JPanel col=UIFactory.transparent(new GridLayout(3,1,0,2));
        col.add(nameRow); col.add(ccLbl); col.add(instLbl);
        item.add(col,BorderLayout.CENTER);

        item.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e){
                selected=est;
                gradeField.setText(est.nota!=null?est.nota:"");
                obsArea.setText("");
                updateDetailHeader();
                // rebuild list to update selection
                Container parent=item.getParent();
                parent.removeAll();
                for(EstudianteEval s:estudiantes){
                    parent.add(buildStudentItem(s));
                    parent.add(Box.createRigidArea(new Dimension(0,4)));
                }
                parent.revalidate(); parent.repaint();
            }
        });
        return item;
    }

    // ── Panel de detalle / calificación ──────────────────────────────────────

    private JPanel buildDetailPanel(){
        // Construye el panel de detalle donde se captura la calificación y observaciones.
        JPanel card=UIFactory.card();
        card.setLayout(new BorderLayout());

        // Header azul (como en el componente original)
        JPanel header=new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY);
        header.setBorder(new EmptyBorder(14,18,14,18));

        JPanel hLeft=UIFactory.transparent(new GridLayout(2,1,0,3));
        headerName=new JLabel(selected!=null?selected.nombre:"");
        headerName.setFont(Theme.FONT_H3); headerName.setForeground(Color.WHITE);
        headerSub=new JLabel(selected!=null?"CC: "+selected.cedula+" | "+selected.institucion:"");
        headerSub.setFont(Theme.FONT_SMALL); headerSub.setForeground(new Color(0xbfdbfe));
        hLeft.add(headerName); hLeft.add(headerSub);
        header.add(hLeft,BorderLayout.WEST);

        if(selected!=null && selected.estado.equals("Evaluado")){
            JPanel evalBadge=new JPanel(new FlowLayout(FlowLayout.RIGHT,6,0));
            evalBadge.setBackground(new Color(0x1a3355));
            evalBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x2d4f7c)),
                new EmptyBorder(4,10,4,10)));
            JLabel evLbl=new JLabel("✓ Evaluación Completada");
            evLbl.setFont(Theme.FONT_SMALL); evLbl.setForeground(new Color(0x86efac));
            evalBadge.add(evLbl);
            header.add(evalBadge,BorderLayout.EAST);
        }
        card.add(header,BorderLayout.NORTH);

        // Body scroll
        JPanel body=new JPanel();
        body.setBackground(Color.WHITE);
        body.setLayout(new BoxLayout(body,BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(24,28,24,28));

        // Aviso
        JPanel aviso=new JPanel(new BorderLayout(12,0));
        aviso.setBackground(new Color(0xeff6ff));
        aviso.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xbfdbfe)),
            new EmptyBorder(12,14,12,14)));
        aviso.setMaximumSize(new Dimension(Integer.MAX_VALUE,80));
        JLabel avisoIcon=new JLabel("⚠"); avisoIcon.setFont(new Font("Segoe UI Emoji",Font.PLAIN,18));
        avisoIcon.setForeground(Theme.PRIMARY);
        JPanel avisoText=UIFactory.transparent(new GridLayout(2,1,0,2));
        JLabel avisoTitle=UIFactory.h3("Criterios de Evaluación");
        avisoTitle.setForeground(Theme.PRIMARY);
        JLabel avisoBody=UIFactory.small("La calificación final debe ser un valor decimal entre 0.00 y 5.00. Las observaciones son obligatorias para notas inferiores a 3.00 o superiores a 4.50.");
        avisoBody.setForeground(Theme.TEXT_SECONDARY);
        avisoText.add(avisoTitle); avisoText.add(avisoBody);
        aviso.add(avisoIcon,BorderLayout.WEST); aviso.add(avisoText,BorderLayout.CENTER);
        aviso.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(aviso); body.add(UIFactory.gap(20));

        // Campo nota
        JLabel notaTitle=UIFactory.fieldLabel("Calificación Final (0.00 - 5.00)");
        notaTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(notaTitle); body.add(UIFactory.gap(6));

        gradeField=new JTextField(){
            @Override protected void paintComponent(Graphics g){
                super.paintComponent(g);
                if(getText().isEmpty()&&!isFocusOwner()){
                    Graphics2D g2=(Graphics2D)g.create();
                    g2.setColor(Theme.TEXT_MUTED); g2.setFont(getFont().deriveFont(Font.PLAIN,12f));
                    g2.drawString("Ej: 4.50",getInsets().left,getInsets().top+g2.getFontMetrics().getAscent());
                    g2.dispose();
                }
            }
        };
        gradeField.setFont(new Font("SansSerif",Font.BOLD,20));
        gradeField.setForeground(Theme.TEXT_PRIMARY);
        gradeField.setBackground(new Color(0xf9fafb));
        gradeField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER,1),
            new EmptyBorder(8,12,8,12)));
        gradeField.setMaximumSize(new Dimension(160,50));
        gradeField.setAlignmentX(Component.LEFT_ALIGNMENT);
        if(selected!=null&&selected.nota!=null) gradeField.setText(selected.nota);
        if(selected!=null&&selected.estado.equals("Evaluado")) gradeField.setEditable(false);

        // Solo permitir números y punto
        gradeField.addKeyListener(new KeyAdapter(){
            @Override public void keyTyped(KeyEvent e){
                char c=e.getKeyChar();
                if(!Character.isDigit(c)&&c!='.'&&c!=KeyEvent.VK_BACK_SPACE) e.consume();
            }
        });
        body.add(gradeField); body.add(UIFactory.gap(20));

        // Observaciones
        JPanel obsTitle=UIFactory.transparent(new FlowLayout(FlowLayout.LEFT,6,0));
        obsTitle.add(new JLabel("📄"){{setFont(new Font("Segoe UI Emoji",Font.PLAIN,13));}}); 
        obsTitle.add(UIFactory.fieldLabel("Observaciones Cualitativas"));
        obsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(obsTitle); body.add(UIFactory.gap(6));

        obsArea=UIFactory.textArea("Ingrese las observaciones sobre el desempeño del estudiante...",6);
        obsArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        obsArea.setMaximumSize(new Dimension(Integer.MAX_VALUE,120));
        if(selected!=null&&selected.estado.equals("Evaluado")) obsArea.setEditable(false);
        JScrollPane obsSp=new JScrollPane(obsArea);
        obsSp.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        obsSp.setMaximumSize(new Dimension(Integer.MAX_VALUE,120));
        obsSp.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(obsSp); body.add(UIFactory.gap(20));

        // Botón enviar
        JPanel btnRow=UIFactory.transparent(new FlowLayout(FlowLayout.RIGHT));
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        submitBtn=UIFactory.primaryBtn("📨  Registrar Calificación");
        if(selected!=null&&selected.estado.equals("Evaluado")) submitBtn.setEnabled(false);
        submitBtn.addActionListener(e->submitGrade());
        btnRow.add(submitBtn);
        body.add(btnRow);

        JScrollPane bodyScroll=new JScrollPane(body);
        bodyScroll.setBorder(BorderFactory.createEmptyBorder());
        card.add(bodyScroll,BorderLayout.CENTER);
        return card;
    }

    private void updateDetailHeader(){
        if(selected==null) return;
        headerName.setText(selected.nombre);
        headerSub.setText("CC: "+selected.cedula+" | "+selected.institucion);
        submitBtn.setEnabled(!selected.estado.equals("Evaluado"));
        gradeField.setEditable(!selected.estado.equals("Evaluado"));
        obsArea.setEditable(!selected.estado.equals("Evaluado"));
    }

    private void submitGrade(){
        String grade=gradeField.getText().trim();
        if(grade.isEmpty()){ JOptionPane.showMessageDialog(this,"Ingrese la calificación.","Error",JOptionPane.WARNING_MESSAGE); return; }
        try{
            double val=Double.parseDouble(grade);
            if(val<0||val>5){ JOptionPane.showMessageDialog(this,"La nota debe estar entre 0.00 y 5.00.","Error",JOptionPane.WARNING_MESSAGE); return; }
            if(selected!=null){
                selected.nota=String.format("%.2f",val);
                selected.estado="Evaluado";
                gradeField.setEditable(false);
                obsArea.setEditable(false);
                submitBtn.setEnabled(false);
                JOptionPane.showMessageDialog(this,"✅ Calificación registrada exitosamente.\n"+selected.nombre+": "+selected.nota,"Éxito",JOptionPane.INFORMATION_MESSAGE);
            }
        }catch(NumberFormatException ex){
            JOptionPane.showMessageDialog(this,"Ingrese un número válido (ej: 4.50).","Error",JOptionPane.WARNING_MESSAGE);
        }
    }
}
