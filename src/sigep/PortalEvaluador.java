package sigep;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Ventana del portal del Docente Evaluador.
 */
// Uso: contenedor para docentes/asesores que muestra `PanelEvaluador`.
public class PortalEvaluador extends JFrame {

    private Runnable onLogout;

    public PortalEvaluador(){ this("Docente Evaluador"); }

    public PortalEvaluador(String roleLabel){
        // Constructor: ventana para docentes/asesores; monta sidebar y panel de evaluación.
        setTitle("SGP – Portal " + roleLabel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1150,710);
        setMinimumSize(new Dimension(900,580));
        setLocationRelativeTo(null);

        JPanel root=new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_PAGE);
        setContentPane(root);
        Sidebar sidebar=new Sidebar(roleLabel);
        sidebar.addNavItem("✅","Calificaciones");
        sidebar.addNavItem("👥","Estudiantes");
        root.add(sidebar,BorderLayout.WEST);

        sidebar.setNavListener(s->{ if(s.equals("Cerrar Sesión")&&onLogout!=null) onLogout.run(); });

        JPanel right=new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(buildHeader(),BorderLayout.NORTH);

        JPanel content=new PanelEvaluador();
        content.setBorder(new EmptyBorder(Theme.PADDING,Theme.PADDING,Theme.PADDING,Theme.PADDING));
        right.add(content,BorderLayout.CENTER);

        root.add(right,BorderLayout.CENTER);
    }

    public void setOnLogout(Runnable r){ this.onLogout=r; }

    private JPanel buildHeader(){
        // Construye el header principal con título y avatar del usuario.
        JPanel h=new JPanel(new BorderLayout());
        h.setBackground(Color.WHITE);
        h.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,Theme.BORDER),
            new EmptyBorder(0,Theme.PADDING,0,Theme.PADDING)));
        h.setPreferredSize(new Dimension(0,Theme.HEADER_H));

        JLabel title=new JLabel("Calificaciones");
        title.setFont(Theme.FONT_H3); title.setForeground(Theme.TEXT_PRIMARY);
        JPanel left=UIFactory.transparent(new FlowLayout(FlowLayout.LEFT,0,0));
        left.add(title); h.add(left,BorderLayout.WEST);

        JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));
        right.setOpaque(false);
        String displayName = "Juan Suárez";
        String roleLabel = roleLabel = roleLabel = roleLabel = "Docente Evaluador";
        if (LoginWindow.usuarioActual != null) {
            var u = LoginWindow.usuarioActual;
            displayName = (u.nombre == null ? "" : u.nombre) + (u.apellido == null ? "" : " " + u.apellido);
            roleLabel = u.rol == null ? roleLabel : u.rol;
        }
        String initials = "JS";
            try {
                String[] parts = displayName.trim().split("\\s+");
                if (parts.length == 0 || parts[0].isBlank()) initials = "";
                else if (parts.length == 1) initials = parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
                else {
                    String a = parts[0].isEmpty() ? "" : String.valueOf(parts[0].charAt(0));
                    String b = parts[parts.length-1].isEmpty() ? "" : String.valueOf(parts[parts.length-1].charAt(0));
                    initials = (a + b).toUpperCase();
                }
            } catch (Exception ignore) {}
        JLabel av=UIFactory.avatar(initials,34,Theme.PRIMARY_LIGHT,Theme.PRIMARY);
        JPanel nc=UIFactory.transparent(new GridLayout(2,1));
        JLabel n=new JLabel(displayName); n.setFont(Theme.FONT_LABEL); n.setForeground(Theme.TEXT_SECONDARY);
        JLabel r=new JLabel(roleLabel); r.setFont(Theme.FONT_SMALL); r.setForeground(Theme.TEXT_MUTED);
        nc.add(n); nc.add(r);
        right.add(av); right.add(nc);
        h.add(right,BorderLayout.EAST);
        return h;
    }
}
