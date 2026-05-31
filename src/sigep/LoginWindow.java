package sigep;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import sigep.db.DB;
import sigep.db.UsuarioDAO;

/**
 * Pantalla de inicio de sesión — replica Login.tsx
 * Fondo gris-100 · Card blanca sin bordes redondeados · azul-900
 */
// Uso: ventana principal de login. Muestra campos de cédula/contraseña y
// controla la navegación inicial hacia los diferentes portales según el rol.
public class LoginWindow extends JFrame {

    // Campo para ingresar la cédula del usuario
    private JTextField     cedulaField;
    // Campo para ingresar la contraseña (oculta)
    private JPasswordField passField;
    // Runables que se ejecutan para abrir los diferentes portales según rol
    Runnable onDirector, onStudent, onDocente, onAsesor;

    public LoginWindow(){
        // Constructor: configura la ventana de login (tamaño, posición, contenido)
        setTitle("SGP – Sistema de Gestión de Prácticas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(460, 560);
        setMinimumSize(new Dimension(400,500));
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel bg=new JPanel(new GridBagLayout());
        bg.setBackground(Theme.BG_PAGE);
        setContentPane(bg);
        bg.add(buildCard());
    }

    private JPanel buildCard(){
        // Construye y devuelve la card central que contiene logo, campos y botones.
        // Uso: llamada desde el constructor para montar el contenido principal.
        JPanel card=new JPanel(){
            @Override protected void paintComponent(Graphics g){ g.setColor(Color.WHITE); g.fillRect(0,0,getWidth(),getHeight()); }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER),
            new EmptyBorder(36,40,36,40)));
        card.setPreferredSize(new Dimension(380,480));

        // ── Logo ──────────────────────────────────────────────────────────
        JPanel logoRow=UIFactory.transparent(new FlowLayout(FlowLayout.CENTER,0,0));
        JLabel logoIcon=new JLabel(){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.PRIMARY); g2.fillOval(0,0,getWidth(),getHeight());
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI Emoji",Font.PLAIN,28));
                FontMetrics fm=g2.getFontMetrics();
                String t="\uD83C\uDF93";
                g2.drawString(t,(getWidth()-fm.stringWidth(t))/2,fm.getAscent()+(getHeight()-fm.getHeight())/2);
                g2.dispose();
            }
        };
        logoIcon.setPreferredSize(new Dimension(64,64));
        logoIcon.setOpaque(false);
        logoRow.add(logoIcon);
        logoRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLbl=new JLabel("Sistema de Prácticas",SwingConstants.CENTER);
        titleLbl.setFont(new Font("SansSerif",Font.BOLD,20));
        titleLbl.setForeground(Theme.PRIMARY);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLbl=new JLabel("Portal Universitario",SwingConstants.CENTER);
        subLbl.setFont(Theme.FONT_BODY);
        subLbl.setForeground(Theme.TEXT_MUTED);
        subLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── Campos ────────────────────────────────────────────────────────
        JPanel fields=new JPanel();
        fields.setOpaque(false);
        fields.setLayout(new BoxLayout(fields,BoxLayout.Y_AXIS));
        fields.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel cLbl=UIFactory.fieldLabel("Cédula"); cLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        cedulaField=UIFactory.textField("Ingrese su número de documento");
        cedulaField.setAlignmentX(Component.LEFT_ALIGNMENT);
        cedulaField.setMaximumSize(new Dimension(Integer.MAX_VALUE,36));

        JLabel pLbl=UIFactory.fieldLabel("Contraseña"); pLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        passField=UIFactory.passwordField("••••••••");
        passField.setAlignmentX(Component.LEFT_ALIGNMENT);
        passField.setMaximumSize(new Dimension(Integer.MAX_VALUE,36));

        fields.add(cLbl); fields.add(UIFactory.gap(4)); fields.add(cedulaField);
        fields.add(UIFactory.gap(14));
        fields.add(pLbl); fields.add(UIFactory.gap(4)); fields.add(passField);

        // ── Botón principal ───────────────────────────────────────────────
        JButton loginBtn=UIFactory.primaryBtn("Ingresar al Sistema");
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.addActionListener(e->login());
        passField.addKeyListener(new KeyAdapter(){
            @Override public void keyPressed(KeyEvent e){ if(e.getKeyCode()==KeyEvent.VK_ENTER) login(); }
        });

        // ── Links rápidos ─────────────────────────────────────────────────
        JButton dirLink=quickLink("Vista Director / Administrador");
        dirLink.addActionListener(e->{ if(onDirector!=null) onDirector.run(); });
        JButton docLink=quickLink("Vista Evaluación / Docente");
        docLink.addActionListener(e->{ if(onDocente!=null) onDocente.run(); });

        // ── Ensamble ──────────────────────────────────────────────────────
        card.add(logoRow);      card.add(UIFactory.gap(12));
        card.add(titleLbl);     card.add(UIFactory.gap(4));
        card.add(subLbl);       card.add(UIFactory.gap(28));
        card.add(fields);       card.add(UIFactory.gap(20));
        card.add(loginBtn);     card.add(UIFactory.gap(14));
        card.add(divider());    card.add(UIFactory.gap(10));
        card.add(dirLink);      card.add(UIFactory.gap(4));
        card.add(docLink);
        return card;
    }

    private JButton quickLink(String text){
        // Crea un botón de enlace estilizado (sin estilo de botón normal).
        // Uso: enlace rápido debajo del login para vistas alternativas.
        JButton b=new JButton(text);
        b.setFont(Theme.FONT_SMALL); b.setForeground(Theme.TEXT_MUTED);
        b.setOpaque(false); b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.addMouseListener(new MouseAdapter(){
            @Override public void mouseEntered(MouseEvent e){ b.setForeground(Theme.PRIMARY); }
            @Override public void mouseExited (MouseEvent e){ b.setForeground(Theme.TEXT_MUTED); }
        });
        return b;
    }

    private JSeparator divider(){
        // Devuelve un separador horizontal estilizado para la UI.
        JSeparator s=new JSeparator();
        s.setForeground(Theme.BORDER); s.setMaximumSize(new Dimension(Integer.MAX_VALUE,1));
        s.setAlignmentX(Component.CENTER_ALIGNMENT);
        return s;
    }

    /** Usuario autenticado — disponible para los portales */
    public static UsuarioDAO.Usuario usuarioActual = null;

    private void login(){
        // Maneja la acción de autenticación: valida campos, llama a DB.login
        // y dispara el Runnable correspondiente según el rol autenticado.
        String cedula = cedulaField.getText().trim();
        String pass   = new String(passField.getPassword()).trim();
        if(cedula.isEmpty() || pass.isEmpty()){
            JOptionPane.showMessageDialog(this,"Complete todos los campos.","Campo requerido",JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Intentar autenticación contra Oracle
        UsuarioDAO.Usuario u = DB.login(cedula, pass);

        if(u == null){
            JOptionPane.showMessageDialog(this,
                "Cédula o contraseña incorrectos, o usuario inactivo.",
                "Acceso denegado", JOptionPane.ERROR_MESSAGE);
            return;
        }

        usuarioActual = u;   // guardar usuario en sesión

        switch(u.rol){
            case "Director" -> { if(onDirector!=null) onDirector.run(); }
            case "Docente"  -> { if(onDocente!=null) onDocente.run(); }
            case "Asesor"   -> { if(onAsesor!=null) onAsesor.run(); }
            case "Estudiante"->{ if(onStudent!=null)  onStudent.run();  }
            default -> JOptionPane.showMessageDialog(this,"Rol desconocido: "+u.rol,"Error",JOptionPane.ERROR_MESSAGE);
        }
    }
}
