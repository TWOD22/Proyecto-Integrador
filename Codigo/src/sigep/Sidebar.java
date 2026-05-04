package sigep;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Barra lateral persistente — azul-900 con nav items.
 * Replica el Layout.tsx aside de SGPE.
 */
public class Sidebar extends JPanel {

    public interface NavListener { void onNav(String section); }

    private final List<NavItem> items = new ArrayList<>();
    private NavItem activeItem;
    private NavListener listener;

    public Sidebar(String role) {
        // Constructor: crea la barra lateral para el `role` dado y configura layout.
        setPreferredSize(new Dimension(Theme.SIDEBAR_W, 0));
        setBackground(Theme.BG_SIDEBAR);
        setLayout(new BorderLayout());
        add(buildHeader(role), BorderLayout.NORTH);
        add(buildNavArea(),    BorderLayout.CENTER);
        add(buildFooter(),     BorderLayout.SOUTH);
    }

    public void setNavListener(NavListener l){ this.listener=l; }

    public void addNavItem(String icon, String label){
        // Agrega un item de navegación con icono y etiqueta; seleccionable.
        NavItem item=new NavItem(icon, label);
        item.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e){ selectItem(item); }
        });
        items.add(item);
        navPanel.add(item);
        navPanel.add(Box.createRigidArea(new Dimension(0,2)));
        if(activeItem==null) selectItem(item);
    }

    public void selectByLabel(String label){
        // Selecciona un item por su etiqueta (útil para navegación programática).
        items.stream().filter(i->i.label.equals(label)).findFirst().ifPresent(this::selectItem);
    }

    private JPanel navPanel;

    private JPanel buildHeader(String role){
        // Construye la sección superior de la barra lateral (logo + role).
        JPanel p=new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(0,0,0,0));

        // Logo row
        JPanel logoRow=new JPanel(new FlowLayout(FlowLayout.LEFT,14,20));
        logoRow.setOpaque(false);
        logoRow.setBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(0x2d4f7c)));

        JLabel icon=new JLabel("\uD83C\uDF93");
        icon.setFont(new Font("Segoe UI Emoji",Font.PLAIN,22));
        icon.setForeground(Color.WHITE);

        JPanel titleCol=UIFactory.transparent(new GridLayout(2,1,0,1));
        JLabel title=new JLabel("SGP");
        title.setFont(new Font("SansSerif",Font.BOLD,17));
        title.setForeground(Color.WHITE);
        JLabel sub=new JLabel("Sistema de Prácticas");
        sub.setFont(Theme.FONT_SMALL);
        sub.setForeground(Theme.TEXT_SIDEBAR);
        titleCol.add(title); titleCol.add(sub);

        logoRow.add(icon); logoRow.add(titleCol);
        p.add(logoRow);

        // Panel de role
        JPanel rolePanel=new JPanel(new FlowLayout(FlowLayout.LEFT,20,12));
        rolePanel.setOpaque(false);
        JLabel roleLabel=new JLabel("Panel de " + role.toUpperCase());
        roleLabel.setFont(new Font("SansSerif",Font.BOLD,9));
        roleLabel.setForeground(new Color(0x5d8ab4));
        rolePanel.add(roleLabel);
        p.add(rolePanel);

        return p;
    }

    private JPanel buildNavArea(){
        // Construye el área navegable que contendrá los `NavItem`.
        navPanel=new JPanel();
        navPanel.setOpaque(false);
        navPanel.setLayout(new BoxLayout(navPanel,BoxLayout.Y_AXIS));
        navPanel.setBorder(new EmptyBorder(4,10,4,10));

        JScrollPane sp=new JScrollPane(navPanel);
        sp.setOpaque(false); sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel wrapper=new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(sp, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildFooter(){
        // Construye el footer con separador y botón de Cerrar Sesión.
        JPanel p=new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0,10,12,10));

        JSeparator sep=new JSeparator();
        sep.setForeground(new Color(0x2d4f7c));
        p.add(sep, BorderLayout.NORTH);

        NavItem logout=new NavItem("⏻","Cerrar Sesión");
        logout.setBorder(new EmptyBorder(6,0,0,0));
        logout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logout.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e){
                if(listener!=null) listener.onNav("Cerrar Sesión");
            }
        });
        p.add(logout, BorderLayout.CENTER);
        return p;
    }

    private void selectItem(NavItem item){
        // Marca `item` como activo, actualiza visual y notifica al listener.
        if(activeItem!=null) activeItem.setActive(false);
        activeItem=item; item.setActive(true);
        if(listener!=null) listener.onNav(item.label);
    }

    // ── Inner NavItem ────────────────────────────────────────────────────────

    static class NavItem extends JPanel {
        final String label;
        private boolean active=false, hover=false;

        // Constructor: crea el elemento visual de navegación con icono y texto.
        NavItem(String icon, String label){
            this.label=label;
            setOpaque(false);
            setLayout(new FlowLayout(FlowLayout.LEFT,10,7));
            setMaximumSize(new Dimension(Integer.MAX_VALUE,40));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel iconLbl=new JLabel(icon);
            iconLbl.setFont(new Font("Segoe UI Emoji",Font.PLAIN,14));
            iconLbl.setForeground(Theme.TEXT_SIDEBAR);

            JLabel textLbl=new JLabel(label);
            textLbl.setFont(Theme.FONT_NAV);
            textLbl.setForeground(Theme.TEXT_SIDEBAR);

            add(iconLbl); add(textLbl);

            addMouseListener(new MouseAdapter(){
                @Override public void mouseEntered(MouseEvent e){ hover=true; repaint(); }
                @Override public void mouseExited (MouseEvent e){ hover=false;repaint(); }
            });
        }

        // Activa/desactiva el item visualmente.
        void setActive(boolean a){ active=a; repaint(); }

        // Dibuja el fondo del item cuando está activo o en hover.
        @Override protected void paintComponent(Graphics g){
            if(active||hover){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(active?Theme.BG_SIDEBAR_ACTIVE:Theme.BG_SIDEBAR_HOVER);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),Theme.RADIUS,Theme.RADIUS);
                if(active){
                    g2.setColor(Theme.PRIMARY_MEDIUM);
                    g2.fillRect(0,5,3,getHeight()-10);
                }
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
