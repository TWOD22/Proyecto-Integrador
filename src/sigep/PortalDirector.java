package sigep;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import sigep.db.DB;

/**
 * Ventana portal del Director.
 * Sidebar → Usuarios | Instituciones | Asignaciones
 * CardLayout gestiona el contenido.
 * Al hacer clic en "Ver Perfil" en PanelUsuarios → navega a PanelPerfilDirector.
 */
// Uso: ventana principal para el Director. Orquesta sidebar y paneles CRUD.
public class PortalDirector extends JFrame {

    private static final String KEY_USERS  = "Usuarios";
    private static final String KEY_INST   = "Instituciones Receptoras";
    private static final String KEY_ASIG   = "Asignaciones";
    private static final String KEY_INFO   = "Informes";
    private static final String KEY_PERFIL = "Perfil Estudiante";

    private final Sidebar sidebar;
    private final CardLayout cardLayout=new CardLayout();
    private final JPanel contentWrap;
    private final JLabel headerTitle;
    private Runnable onLogout;

    private final PanelUsuarios panelUsuarios;
    private final PanelInformes panelInformes;

    public PortalDirector(){
        // Constructor: configura ventana del director, sidebar y paneles CRUD.
        setTitle("SGP – Portal Director");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200,730);
        setMinimumSize(new Dimension(920,580));
        setLocationRelativeTo(null);

        JPanel root=new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_PAGE);
        setContentPane(root);

        // ── Sidebar ──────────────────────────────────────────────────────────
        sidebar=new Sidebar("Director");
        sidebar.addNavItem("👥", KEY_USERS);
        sidebar.addNavItem("🏢", KEY_INST);
        sidebar.addNavItem("📋", KEY_ASIG);
        sidebar.addNavItem("📊", KEY_INFO);
        root.add(sidebar, BorderLayout.WEST);

        // ── Right ─────────────────────────────────────────────────────────────
        JPanel right=new JPanel(new BorderLayout());
        right.setOpaque(false);

        JPanel header=buildHeader();
        headerTitle=findTitle(header);
        right.add(header,BorderLayout.NORTH);

        contentWrap=new JPanel(cardLayout);
        contentWrap.setBackground(Theme.BG_PAGE);

        // Paneles
        panelUsuarios=new PanelUsuarios();
        panelUsuarios.onVerPerfil=(u)->{
            if (u == null) {
                JOptionPane.showMessageDialog(this, "No se encontró el usuario seleccionado.", "Perfil no disponible", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                headerTitle.setText("Perfil de "+u.nombreCompleto());
                sidebar.selectByLabel(KEY_USERS);
                PanelPerfilDirector perfil=new PanelPerfilDirector(u, ()->{
                    // Ensure any table editors are stopped and UI is refreshed when returning
                    try { panelUsuarios.stopEditingAndClearSelection(); } catch (Exception ignored) {}
                    cardLayout.show(contentWrap,KEY_USERS);
                    headerTitle.setText(KEY_USERS);
                });
                contentWrap.add(wrap(perfil), KEY_PERFIL);
                cardLayout.show(contentWrap,KEY_PERFIL);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al abrir el perfil:\n" + ex, "Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        PanelInstituciones panelInst = new PanelInstituciones();
        PanelAsignaciones panelAsig = new PanelAsignaciones();
        panelInformes = new PanelInformes();

        panelAsig.onVerPerfil = (u) -> {
            if (u == null) {
                JOptionPane.showMessageDialog(this, "No se encontró el usuario seleccionado.", "Perfil no disponible", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                headerTitle.setText("Perfil de "+u.nombreCompleto());
                sidebar.selectByLabel(KEY_ASIG);
                PanelPerfilDirector perfil=new PanelPerfilDirector(u, ()->{
                    try { panelAsig.stopEditingAndClearSelection(); } catch (Exception ignored) {}
                    cardLayout.show(contentWrap,KEY_ASIG);
                    headerTitle.setText(KEY_ASIG);
                });
                contentWrap.add(wrap(perfil), KEY_PERFIL);
                cardLayout.show(contentWrap,KEY_PERFIL);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al abrir el perfil:\n" + ex, "Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        contentWrap.add(wrap(panelUsuarios),         KEY_USERS);
        contentWrap.add(wrap(panelInst),KEY_INST);
        contentWrap.add(wrap(panelAsig), KEY_ASIG);
        contentWrap.add(wrap(panelInformes), KEY_INFO);

        // Registrar listeners para refrescar paneles cuando la BD cambia
        DB.addChangeListener(topic -> {
            if ("instituciones".equals(topic)) {
                try { panelInst.refreshTable(); } catch (Exception ignored) {}
                try { panelAsig.refreshFromDB(); } catch (Exception ignored) {}
                try { panelInformes.refreshAll(); } catch (Exception ignored) {}
            }
            if ("usuarios".equals(topic)) {
                try { panelUsuarios.refreshTable(); } catch (Exception ignored) {}
                try { panelAsig.refreshFromDB(); } catch (Exception ignored) {}
                try { panelInformes.refreshAll(); } catch (Exception ignored) {}
            }
        });

        right.add(contentWrap,BorderLayout.CENTER);
        root.add(right,BorderLayout.CENTER);

        // Navegación
        sidebar.setNavListener(section->{
            if(section.equals("Cerrar Sesión")){ if(onLogout!=null) onLogout.run(); return; }
            if(!section.equals(KEY_PERFIL)){
                // Stop any active editors in panels before switching view
                try {
                    if (section.equals(KEY_USERS)) panelUsuarios.stopEditingAndClearSelection();
                    if (section.equals(KEY_INST)) panelInst.stopEditingAndClearSelection();
                    if (section.equals(KEY_ASIG)) panelAsig.stopEditingAndClearSelection();
                } catch (Exception ignored) {}
                cardLayout.show(contentWrap,section);
                if(headerTitle!=null) headerTitle.setText(section);
            }
        });

        cardLayout.show(contentWrap,KEY_USERS);
    }

    public void setOnLogout(Runnable r){ this.onLogout=r; }

    // ── Header ────────────────────────────────────────────────────────────────

    private JPanel buildHeader(){
        // Construye el header superior con título y chip de usuario.
        JPanel h=new JPanel(new BorderLayout());
        h.setBackground(Color.WHITE);
        h.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,Theme.BORDER),
            new EmptyBorder(0,Theme.PADDING,0,Theme.PADDING)));
        h.setPreferredSize(new Dimension(0,Theme.HEADER_H));

        JLabel title=new JLabel(KEY_USERS);
        title.setFont(Theme.FONT_H3); title.setForeground(Theme.TEXT_PRIMARY);
        JPanel left=UIFactory.transparent(new FlowLayout(FlowLayout.LEFT,0,0));
        left.add(title); h.add(left,BorderLayout.WEST);

        // User chip (use logged-in user if available)
        JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));
        right.setOpaque(false);
        String displayName = "Director Admin";
        String roleLabel = "Director de Prácticas";
        if (LoginWindow.usuarioActual != null) {
            var u = LoginWindow.usuarioActual;
            displayName = (u.nombre == null ? "" : u.nombre) + (u.apellido == null ? "" : " " + u.apellido);
            roleLabel = u.rol == null ? roleLabel : u.rol;
        }
        String initials = "DA";
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
        JPanel nameCol=UIFactory.transparent(new GridLayout(2,1));
        JLabel name=new JLabel(displayName); name.setFont(Theme.FONT_LABEL); name.setForeground(Theme.TEXT_SECONDARY);
        JLabel role=new JLabel(roleLabel); role.setFont(Theme.FONT_SMALL); role.setForeground(Theme.TEXT_MUTED);
        nameCol.add(name); nameCol.add(role);
        right.add(av); right.add(nameCol);
        h.add(right,BorderLayout.EAST);
        return h;
    }

    private JLabel findTitle(JPanel header){
        // Busca y devuelve la etiqueta que contiene el título dentro del header.
        for(Component c:header.getComponents()){
            if(c instanceof JPanel jp){
                for(Component cc:jp.getComponents()){
                    if(cc instanceof JLabel jl) return jl;
                }
            }
        }
        return null;
    }

    private JScrollPane wrap(JPanel p){
        // Envuelve un panel en un JScrollPane con estilos del tema.
        JScrollPane sp=new JScrollPane(p);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Theme.BG_PAGE);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return sp;
    }
}
